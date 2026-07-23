package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.debug.DebugSimulation;
import com.pluginideahub.combatachievements.core.feedback.FeedbackLink;
import com.pluginideahub.combatachievements.core.ui.PanelAction;
import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import com.pluginideahub.combatachievements.ui.Palette;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The Combat Achievements side panel — a Quest-Helper-style single-column flow with three top-level
 * modes ({@link PanelMode}): Recommended, Bosses and Route (see docs/adr/0001). Pure renderer of
 * {@link SidePanelViewModel}: it never touches the game client and only emits {@link PanelAction}s
 * (and opens links) in response to clicks. Mode and sort are pure view state over the built model.
 */
public class CombatAchievementsPanel extends PluginPanel
{
	/** List orderings offered in the CAs and Bosses modes (one shared Order control). */
	enum Sort
	{
		RECOMMENDED("Recommended"),
		MOST_POINTS("Most points"),
		EASIEST("Easiest");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private static final String SEARCH_HINT = "Search";

	/** Opened by the footer link. Plain browser navigation — nothing is sent from the client. */
	private static final String PATREON_URL = "https://www.patreon.com/c/CAHelper";
	/** Cap on cards rendered at once in the (now full) CAs list; refine the search to see more. */
	private static final int RENDER_CAP = 60;
	/** Max rank positions a CA can move when the CAs list is shuffled — big enough to surface a different
	 *  set, small enough to keep good CAs near the top. */
	private static final double SHUFFLE_JITTER = 12.0;
	/** Default per-visit overhead (minutes) amortised into the Bosses "Recommended" sort; matches the
	 *  {@code tripOverheadMinutes} config default so the two agree until the plugin seeds the live value. */
	private static final int DEFAULT_TRIP_OVERHEAD = 6;
	/** Pinned size of a developer-mode level spinner — see {@link #devSkillCell(String)}. */
	private static final int SPINNER_WIDTH = 46;
	private static final int SPINNER_HEIGHT = 18;
	/** Wrap width for the CA-detail how-to prose, kept clear of the scrollbar the tall view brings in. */
	private static final int DETAIL_TEXT_WIDTH = 170;

	private final transient Consumer<PanelAction> onAction;

	private transient SidePanelViewModel model = SidePanelViewModel.loggedOut();
	private PanelMode currentMode = PanelMode.RECOMMENDED;
	private Sort sort = Sort.RECOMMENDED;
	/** Shuffle seed for the CAs list; 0 = natural (model) order, each reshuffle bumps it. */
	private long shuffleSeed;
	private boolean unlocksCollapsed;
	private boolean trainingsCollapsed;
	/** Per-visit overhead (min) amortised into the Bosses "Recommended" sort — the clustering dial: the
	 *  higher it is, the more the sort favours bosses with several doable CAs (less boss-swapping). */
	private int tripOverheadMinutes = DEFAULT_TRIP_OVERHEAD;
	/** Weight on total time in the Bosses "Recommended" pts/hr metric (1.0 = neutral; 0 = ignore time,
	 *  ranking purely on points). */
	private double bossTimeWeight = 1.0;
	/** Whether the CA-detail "How to do it" section is expanded; seeded from config, toggled per-CA. */
	private boolean howToExpanded;
	/** Whether the CA-detail "Requirements" section is expanded (default yes, hideable). */
	private boolean reqsExpanded = true;
	/** When non-null, the CA-detail drill-in is shown (over whatever mode is active). */
	private transient SidePanelViewModel.CaDetail selectedCa;
	/** When non-null (Bosses mode), the boss-detail drill-in is shown. */
	private String selectedBoss;

	/**
	 * Developer-mode account simulation. Volatile because it is written on the EDT (by the controls) and
	 * read from the client thread (by the plugin, on every refresh); {@link DebugSimulation} is immutable,
	 * so publishing a whole new instance is the entire synchronisation.
	 */
	private volatile transient DebugSimulation debugSimulation = DebugSimulation.none();
	/** Gate for the whole dev section. While false the simulation is not applied and cannot be seen. */
	private volatile boolean developerMode;
	private boolean devCollapsed = true;
	/** True once a level has been simulated; until then the spinners just mirror the real account. */
	private boolean devLevelsActive;
	/** Suppresses the per-spinner listener while a preset writes all 23 at once. */
	private transient boolean devBulkUpdate;

	private final JLabel title = new JLabel("Combat Achievement Helper");
	private final JPanel devSection = new JPanel(new BorderLayout());
	private final JPanel devBody = new JPanel();
	private final JLabel devHeader = new JLabel();
	private final JCheckBox devZeroCas = new JCheckBox("No CAs completed");
	private final JCheckBox devZeroQuests = new JCheckBox("No quests done");
	private final transient Map<String, JSpinner> devSpinners = new LinkedHashMap<>();
	private final JTextField searchField = new JTextField();
	private final JComboBox<Sort> sortBox = new JComboBox<>(Sort.values());
	private final JButton shuffleButton = new JButton("⟳");
	private final JLabel orderLabel = new JLabel("Order ");
	private final JPanel modeBar = new JPanel(new GridBagLayout());
	private final JPanel controlBar = new JPanel(new BorderLayout(0, 6));
	private final JPanel orderRow = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel();

	public CombatAchievementsPanel(Consumer<PanelAction> onAction)
	{
		super(false);
		this.onAction = onAction;

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		titleRow.add(title, BorderLayout.WEST);
		header.add(titleRow, BorderLayout.NORTH);

		modeBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		modeBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		buildModeBar();

		// The Order dropdown lives in the persistent header (never in the rebuilt content), so selecting
		// an order can't destroy the combobox mid-click — the reason it was unusable before.
		sortBox.setFont(FontManager.getRunescapeSmallFont());
		sortBox.setSelectedItem(sort);
		sortBox.setFocusable(false);
		sortBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sortBox.setForeground(CombatAchievementsTheme.NAME);
		sortBox.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		// Themed rendering for both the closed box and the dropdown items (default L&F is a bright box).
		sortBox.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setFont(FontManager.getRunescapeSmallFont());
				setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
				setForeground(isSelected ? Color.WHITE : CombatAchievementsTheme.NAME);
				setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
				return this;
			}
		});
		sortBox.addActionListener(e -> {
			Object sel = sortBox.getSelectedItem();
			if (sel instanceof Sort)
			{
				sort = (Sort) sel;
			}
			rebuild();
		});

		// Search box — also persistent in the header. Its DocumentListener re-renders only the content
		// list, so typing never rebuilds the field itself. A simple focus-driven "Search" placeholder.
		searchField.setFont(FontManager.getRunescapeSmallFont());
		searchField.setBackground(ColorScheme.DARK_GRAY_COLOR);
		searchField.setCaretColor(CombatAchievementsTheme.NAME);
		searchField.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_HOVER_COLOR),
			BorderFactory.createEmptyBorder(3, 5, 3, 5)));
		showSearchHint();
		searchField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				if (SEARCH_HINT.equals(searchField.getText()))
				{
					searchField.setText("");
					searchField.setForeground(CombatAchievementsTheme.NAME);
				}
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				if (searchField.getText().isEmpty())
				{
					showSearchHint();
				}
			}
		});
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
			}
		});
		buildControlBar();

		JPanel nav = new JPanel(new BorderLayout());
		nav.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nav.add(modeBar, BorderLayout.NORTH);
		nav.add(controlBar, BorderLayout.SOUTH);
		header.add(nav, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JScrollPane scroll = new JScrollPane(content,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		buildDevSection();

		// The dev section is deliberately OUTSIDE the scrolled `content`: rebuild() clears that container on
		// every model push, and detaching a focused spinner mid-keystroke makes Swing commit the half-typed
		// number and switch the override on by itself. Pinned here it is never detached — and the controls
		// stay put while the list scrolls underneath, which is what you want while dialling levels in.
		JPanel centre = new JPanel(new BorderLayout());
		centre.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		centre.add(devSection, BorderLayout.NORTH);
		centre.add(scroll, BorderLayout.CENTER);
		add(centre, BorderLayout.CENTER);
		add(supportFooter(), BorderLayout.SOUTH);

		rebuild();
	}

	/**
	 * The developer-only "pretend this is a different account" controls: level presets, a per-skill grid,
	 * and a switch that hides your completed Combat Achievements. Built ONCE here and merely re-parented by
	 * {@link #rebuild()}, because rebuild runs on every model push (potentially every game tick) and every
	 * search keystroke — a spinner created inside it would be destroyed and re-created out from under the
	 * click that was setting it.
	 */
	private void buildDevSection()
	{
		devSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		devSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		devSection.setVisible(false); // developer mode off until the plugin says otherwise

		devHeader.setFont(FontManager.getRunescapeBoldFont());
		devHeader.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setOpaque(false);
		headerRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		headerRow.add(devHeader, BorderLayout.WEST);
		onClick(headerRow, () -> setDevCollapsed(!devCollapsed));
		devSection.add(headerRow, BorderLayout.NORTH);

		devBody.setLayout(new BoxLayout(devBody, BoxLayout.Y_AXIS));
		devBody.setOpaque(false);
		devBody.setVisible(!devCollapsed);

		// Presets: every skill at once. "Real" drops the override so the panel goes back to your account.
		// Three per row — six across one row pushes the last button off the 225px panel.
		JPanel presets = new JPanel(new GridLayout(0, 3, 2, 2));
		presets.setOpaque(false);
		for (int level : new int[]{1, 40, 60, 80, 99})
		{
			presets.add(pillButton(String.valueOf(level), e -> applyLevelPreset(level)));
		}
		presets.add(pillButton("Real", e -> clearLevelSimulation()));
		devBody.add(fullWidth(presets));
		devBody.add(spacer());

		styleDevCheckBox(devZeroCas,
			"Hide your completed CAs, so the panel treats you as having 0 points");
		// Without this a simulated beginner keeps your real quest log, so quest-gated content stays
		// unlocked and "Unlock next" comes back empty — a quest you have done is not an unlock.
		styleDevCheckBox(devZeroQuests,
			"Pretend no quest is done, so quest-gated content locks like a new account's");
		JPanel switches = new JPanel(new GridLayout(0, 1, 0, 1));
		switches.setOpaque(false);
		switches.add(devZeroCas);
		switches.add(devZeroQuests);
		devBody.add(fullWidth(switches));
		devBody.add(spacer());

		// Two columns: at three, the label plus the spinner's arrows exceed the cell and the last column's
		// spinner is clipped off the panel.
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 2));
		grid.setOpaque(false);
		for (String skill : DebugSimulation.SKILLS)
		{
			grid.add(devSkillCell(skill));
		}
		devBody.add(fullWidth(grid));

		devSection.add(devBody, BorderLayout.CENTER);
		refreshDevHeader();
	}

	private void styleDevCheckBox(JCheckBox box, String tooltip)
	{
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setOpaque(false);
		box.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		box.setFocusPainted(false);
		box.setToolTipText(tooltip);
		box.addActionListener(e -> publishSimulation());
	}

	/** One "Att [ 1]" cell of the per-skill grid. */
	private JPanel devSkillCell(String skill)
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(
			skill.equalsIgnoreCase("Hitpoints") ? DebugSimulation.MIN_HITPOINTS : 1, 1, 99, 1));
		JComponent editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor)
		{
			JTextField field = ((JSpinner.DefaultEditor) editor).getTextField();
			field.setColumns(2);
			field.setFont(FontManager.getRunescapeSmallFont());
			field.setBackground(ColorScheme.DARK_GRAY_COLOR);
			field.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			field.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
		}
		spinner.setBorder(BorderFactory.createEmptyBorder());
		spinner.setToolTipText(skill);
		// A JSpinner reports its arrows-plus-editor width as its MINIMUM, and BoxLayout will not shrink a
		// child below that — two unpinned spinners per row size the whole section wider than the 225px
		// panel and everything past the first column is clipped away. Pin all three sizes.
		Dimension size = new Dimension(SPINNER_WIDTH, SPINNER_HEIGHT);
		spinner.setPreferredSize(size);
		spinner.setMinimumSize(size);
		spinner.setMaximumSize(size);
		spinner.addChangeListener(e ->
		{
			if (!devBulkUpdate)
			{
				// Touching any spinner switches the levels on — otherwise the first nudge would be ignored.
				devLevelsActive = true;
				publishSimulation();
			}
		});
		devSpinners.put(skill, spinner);

		JLabel label = new JLabel(abbreviate(skill));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel cell = new JPanel(new BorderLayout(2, 0));
		cell.setOpaque(false);
		cell.add(label, BorderLayout.WEST);
		cell.add(spinner, BorderLayout.CENTER);
		return cell;
	}

	/** Three characters is all the 225px panel affords for 3 columns of skills. */
	private static String abbreviate(String skill)
	{
		if (skill.equalsIgnoreCase("Hitpoints"))
		{
			return "HP";
		}
		return skill.length() <= 3 ? skill : skill.substring(0, 3);
	}

	void applyLevelPreset(int level)
	{
		devBulkUpdate = true;
		try
		{
			for (Map.Entry<String, JSpinner> e : devSpinners.entrySet())
			{
				e.getValue().setValue(e.getKey().equalsIgnoreCase("Hitpoints")
					? Math.max(DebugSimulation.MIN_HITPOINTS, level) : level);
			}
		}
		finally
		{
			devBulkUpdate = false;
		}
		devLevelsActive = true;
		publishSimulation();
	}

	/** Back to the real account's levels; the spinners re-seed from it on the next refresh. */
	void clearLevelSimulation()
	{
		devLevelsActive = false;
		publishSimulation();
	}

	/** Folds the dev section away; collapsed by default so developer mode costs one line until wanted. */
	void setDevCollapsed(boolean collapsed)
	{
		devCollapsed = collapsed;
		devBody.setVisible(!collapsed);
		refreshDevHeader();
		// The section lives outside `content`, so this never needs a full rebuild.
		devSection.revalidate();
		devSection.repaint();
	}

	/** Test hook: flips the "No CAs completed" switch as a click would. */
	void setZeroCompletion(boolean on)
	{
		devZeroCas.setSelected(on);
		publishSimulation();
	}

	/** Test hook: flips the "No quests done" switch as a click would. */
	void setZeroQuests(boolean on)
	{
		devZeroQuests.setSelected(on);
		publishSimulation();
	}

	/** Test hook: the level currently shown for one skill. */
	int devSpinnerValue(String skill)
	{
		JSpinner spinner = devSpinners.get(skill);
		return spinner == null ? 0 : ((Number) spinner.getValue()).intValue();
	}

	/**
	 * Seeds the spinners from the account's real levels so the grid starts at the truth and "Real" means
	 * something. Ignored while a simulation is running, or the next refresh would clobber what was typed.
	 */
	public void setRealLevels(Map<String, Integer> levels)
	{
		if (levels == null || levels.isEmpty())
		{
			return;
		}
		Runnable apply = () ->
		{
			if (devLevelsActive)
			{
				return;
			}
			devBulkUpdate = true;
			try
			{
				for (Map.Entry<String, JSpinner> e : devSpinners.entrySet())
				{
					Integer real = levels.get(e.getKey());
					if (real != null)
					{
						e.getValue().setValue(Math.max(1, Math.min(99, real)));
					}
				}
			}
			finally
			{
				devBulkUpdate = false;
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			apply.run();
		}
		else
		{
			SwingUtilities.invokeLater(apply);
		}
	}

	/** Rebuilds the immutable simulation from the controls and asks the plugin for a fresh model. */
	private void publishSimulation()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		if (devLevelsActive)
		{
			for (Map.Entry<String, JSpinner> e : devSpinners.entrySet())
			{
				Object value = e.getValue().getValue();
				if (value instanceof Number)
				{
					levels.put(e.getKey(), ((Number) value).intValue());
				}
			}
		}
		debugSimulation = DebugSimulation.of(levels, devZeroCas.isSelected(),
			devZeroQuests.isSelected());
		refreshDevHeader();
		emit(PanelAction.REFRESH);
	}

	/** Keeps the collapsed header honest about whether a simulation is running underneath it. */
	private void refreshDevHeader()
	{
		String state = debugSimulation.isActive() ? " · on" : "";
		devHeader.setText((devCollapsed ? "▸ " : "▾ ") + "Simulate" + state);
		devHeader.setForeground(debugSimulation.isActive()
			? CombatAchievementsTheme.ACCENT : CombatAchievementsTheme.HEADER_GOLD);
	}

	/**
	 * Turns the dev section on or off. While off the simulation is inert — {@link #debugSimulation()}
	 * reports none, so a simulation left running cannot survive switching developer mode back off and
	 * silently misreport a real account. Safe to call off the EDT.
	 */
	public void setDeveloperMode(boolean on)
	{
		Runnable apply = () ->
		{
			developerMode = on;
			devSection.setVisible(on);
			devSection.revalidate();
			devSection.repaint();
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			apply.run();
		}
		else
		{
			SwingUtilities.invokeLater(apply);
		}
	}

	/**
	 * The simulation the plugin should apply this refresh — always {@link DebugSimulation#none()} unless
	 * developer mode is on. Called from the client thread.
	 */
	public DebugSimulation debugSimulation()
	{
		return developerMode ? debugSimulation : DebugSimulation.none();
	}

	/**
	 * A single muted link pinned below the scroll area. Deliberately the quietest thing in the panel:
	 * meta-grey (not the gold used for real actions), warming only on hover, and never a popup, chat
	 * message or recurring prompt — it should read as chrome you can ignore, not an advert.
	 */
	private JPanel supportFooter()
	{
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

		JButton support = new JButton("Support on Patreon");
		support.setFont(FontManager.getRunescapeSmallFont());
		support.setFocusPainted(false);
		support.setBorderPainted(false);
		support.setContentAreaFilled(false);
		support.setOpaque(false);
		support.setForeground(CombatAchievementsTheme.NEUTRAL_META);
		support.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		support.setCursor(new Cursor(Cursor.HAND_CURSOR));
		support.setToolTipText("Opens patreon.com in your browser");
		support.addActionListener(e -> LinkBrowser.browse(PATREON_URL));
		addForegroundHover(support, CombatAchievementsTheme.NEUTRAL_META,
			CombatAchievementsTheme.HEADER_GOLD);
		footer.add(support, BorderLayout.WEST);
		return footer;
	}

	private void emit(PanelAction action)
	{
		if (onAction != null)
		{
			onAction.accept(action);
		}
	}

	private void buildModeBar()
	{
		modeBar.removeAll();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.gridy = 0;
		PanelMode[] modes = PanelMode.values();
		for (int i = 0; i < modes.length; i++)
		{
			PanelMode mode = modes[i];
			boolean sel = mode == currentMode;
			JButton button = new JButton(mode.label());
			button.setFont(FontManager.getRunescapeSmallFont());
			button.setFocusPainted(false);
			button.setBorderPainted(false);
			button.setOpaque(true);
			button.setBackground(sel ? CombatAchievementsTheme.MODE_SELECTED : ColorScheme.DARK_GRAY_COLOR);
			button.setForeground(sel ? CombatAchievementsTheme.MODE_SELECTED_TEXT : ColorScheme.LIGHT_GRAY_COLOR);
			button.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
			button.setMargin(new Insets(0, 0, 0, 0));
			button.addActionListener(e -> {
				currentMode = mode;
				selectedCa = null;
				selectedBoss = null;
				buildModeBar();
				rebuild();
			});
			if (!sel)
			{
				addHover(button, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
			}
			// "Recommended" is the widest label — give it more of the row so it doesn't clip.
			gc.gridx = i;
			gc.weightx = mode == PanelMode.RECOMMENDED ? 1.5 : 1.0;
			gc.insets = new Insets(0, 0, 0, i == modes.length - 1 ? 0 : 4);
			modeBar.add(button, gc);
		}
		modeBar.revalidate();
		modeBar.repaint();
	}

	private JButton pillButton(String text, java.awt.event.ActionListener onClick)
	{
		JButton b = new JButton(text);
		b.setFont(FontManager.getRunescapeSmallFont());
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setOpaque(true);
		b.setBackground(ColorScheme.DARK_GRAY_COLOR);
		b.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		b.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		b.addActionListener(onClick);
		addHover(b, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return b;
	}

	/** Styles a button as the small gold ⟳ refresh icon (a system font — the RuneScape font lacks the glyph). */
	private void styleAsRefresh(JButton b, String tooltip)
	{
		b.setToolTipText(tooltip);
		b.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setOpaque(true);
		b.setBackground(ColorScheme.DARK_GRAY_COLOR);
		b.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		b.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
		b.setMargin(new Insets(0, 0, 0, 0));
		addHover(b, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
	}

	/** Hover that brightens the TEXT (for transparent link-style buttons), mirroring {@link #addHover}. */
	private static void addForegroundHover(JComponent c, Color base, Color hover)
	{
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				c.setForeground(hover);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				c.setForeground(base);
			}
		});
	}

	private static void addHover(JComponent c, Color base, Color hover)
	{
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				c.setBackground(hover);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				c.setBackground(base);
			}
		});
	}

	/** Switches the active mode and re-renders (used by the headless preview harness and tests). */
	public void showMode(PanelMode mode)
	{
		this.currentMode = mode;
		this.selectedCa = null;
		this.selectedBoss = null;
		buildModeBar();
		rebuild();
	}

	/** Preview/test hook: opens the CA-detail view for the first doable CA. */
	public void openFirstCaDetail()
	{
		for (SidePanelViewModel.TaskRow r : model.quickWins())
		{
			if (r.doableNow && r.detail != null)
			{
				selectedCa = r.detail;
				rebuild();
				return;
			}
		}
	}

	/** Preview/test hook: opens the boss-detail view for the first boss. */
	public void openFirstBossDetail()
	{
		if (model.sessions() != null && !model.sessions().isEmpty())
		{
			currentMode = PanelMode.BOSSES;
			selectedCa = null;
			selectedBoss = model.sessions().get(0).monster;
			buildModeBar();
			rebuild();
		}
	}

	/** Preview/test hook: opens the detail for the first LOCKED route CA (shows red unmet requirements). */
	public void openFirstLockedCaDetail()
	{
		if (model.path() != null && !model.path().lockedCas.isEmpty())
		{
			currentMode = PanelMode.ROUTE;
			selectedBoss = null;
			selectedCa = model.path().lockedCas.get(0);
			buildModeBar();
			rebuild();
		}
	}

	/** Sets the default expanded state of the CA-detail "How to do it" section (from the config option). */
	public void setHowToDefault(boolean expanded)
	{
		this.howToExpanded = expanded;
		if (SwingUtilities.isEventDispatchThread())
		{
			rebuild();
		}
	}

	/**
	 * Sets the per-trip overhead (minutes) that weights the Bosses "Recommended" sort — the config's
	 * "Session clustering" dial. Higher values amortise more fixed cost across each boss's doable CAs,
	 * so bosses where you can knock out several CAs in one visit rise above trivial single-CA bosses.
	 */
	public void setTripOverheadMinutes(int minutes)
	{
		this.tripOverheadMinutes = Math.max(0, minutes);
		if (SwingUtilities.isEventDispatchThread())
		{
			rebuild();
		}
	}

	/** Sets the Bosses "Recommended" time weight (1.0 = neutral; 0 = ignore time and rank on points). */
	public void setBossTimeWeight(double weight)
	{
		this.bossTimeWeight = Math.max(0.0, weight);
		if (SwingUtilities.isEventDispatchThread())
		{
			rebuild();
		}
	}

	/**
	 * Swaps the active colour palette (from the config theme) and re-themes the persistent header
	 * components + the mode bar, then re-renders. Safe to call off the EDT.
	 */
	public void applyTheme(Palette palette)
	{
		Runnable apply = () ->
		{
			CombatAchievementsTheme.apply(palette);
			title.setForeground(CombatAchievementsTheme.HEADER_GOLD);
			refreshDevHeader(); // the dev section persists across rebuilds, so re-tint it explicitly
			sortBox.setForeground(CombatAchievementsTheme.NAME);
			searchField.setCaretColor(CombatAchievementsTheme.NAME);
			if (!SEARCH_HINT.equals(searchField.getText()))
			{
				searchField.setForeground(CombatAchievementsTheme.NAME); // re-tint an active query
			}
			buildModeBar();
			rebuild();
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			apply.run();
		}
		else
		{
			SwingUtilities.invokeLater(apply);
		}
	}

	/** Pushes a new model and re-renders on the EDT. Safe to call from the client thread. */
	public void render(SidePanelViewModel viewModel)
	{
		this.model = viewModel == null ? SidePanelViewModel.loggedOut() : viewModel;
		if (SwingUtilities.isEventDispatchThread())
		{
			rebuild();
		}
		else
		{
			SwingUtilities.invokeLater(this::rebuild);
		}
	}

	private void rebuild()
	{
		boolean ready = model.state() == SidePanelViewModel.State.READY;
		boolean detailOpen = selectedCa != null
			|| (selectedBoss != null && currentMode == PanelMode.BOSSES);
		boolean caMode = currentMode == PanelMode.RECOMMENDED;
		boolean routeMode = ready && !detailOpen && currentMode == PanelMode.ROUTE;
		boolean searchable = ready && !detailOpen && (caMode || currentMode == PanelMode.BOSSES);
		searchField.setVisible(searchable);
		orderLabel.setVisible(searchable);
		sortBox.setVisible(searchable);
		orderRow.setVisible(searchable || routeMode); // Order controls (CAs/Bosses) and/or the refresh button
		shuffleButton.setVisible(caMode || routeMode); // CAs reshuffle + Route re-solve; not Bosses
		shuffleButton.setToolTipText(routeMode ? "Suggest a different route" : "Shuffle — show a different set of CAs");
		controlBar.setVisible(searchable || routeMode);
		content.removeAll();

		if (selectedCa != null)
		{
			renderCaDetail(selectedCa); // the drill-in overrides whatever mode is active
		}
		else if (!ready)
		{
			content.add(messageLabel(model.message()));
		}
		else
		{
			switch (currentMode)
			{
				case RECOMMENDED:
					buildRecommended();
					break;
				case BOSSES:
					if (selectedBoss != null)
					{
						buildBossDetail(selectedBoss);
					}
					else
					{
						buildBosses();
					}
					break;
				case ROUTE:
					buildRoute();
					break;
				default:
					break;
			}
		}

		content.revalidate();
		content.repaint();
	}

	/** Adds a click handler + hand cursor to a card, without disturbing its inner link buttons. */
	private static void onClick(JComponent c, Runnable action)
	{
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				action.run();
			}
		});
	}

	// ---- Recommended -------------------------------------------------------------------------------

	private void buildControlBar()
	{
		controlBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		controlBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		orderRow.setOpaque(false);
		orderLabel.setFont(FontManager.getRunescapeSmallFont());
		orderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		orderRow.add(orderLabel, BorderLayout.WEST);
		orderRow.add(sortBox, BorderLayout.CENTER);

		// A little refresh button: in CAs it reshuffles the list to a different set; in Route it re-solves
		// for a different quick route. (Shares the header control bar, which lays out reliably.)
		styleAsRefresh(shuffleButton, "Shuffle — show a different set");
		shuffleButton.addActionListener(e -> {
			if (currentMode == PanelMode.ROUTE)
			{
				emit(PanelAction.RESHUFFLE_ROUTE);
			}
			else
			{
				shuffleSeed++;
				rebuild();
			}
		});
		orderRow.add(shuffleButton, BorderLayout.EAST);

		controlBar.add(searchField, BorderLayout.NORTH);
		controlBar.add(orderRow, BorderLayout.CENTER);
	}

	private void showSearchHint()
	{
		searchField.setText(SEARCH_HINT);
		searchField.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
	}

	/** The active search query (lower-cased), or "" when the box is empty / showing its placeholder. */
	private String searchText()
	{
		String t = searchField.getText();
		return t == null || SEARCH_HINT.equals(t) ? "" : t.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean matches(SidePanelViewModel.TaskRow r, String q)
	{
		return q.isEmpty()
			|| r.name.toLowerCase(Locale.ROOT).contains(q)
			|| r.monster.toLowerCase(Locale.ROOT).contains(q);
	}

	private void buildRecommended()
	{
		String q = searchText();
		List<SidePanelViewModel.TaskRow> rows = new ArrayList<>();
		for (SidePanelViewModel.TaskRow r : model.quickWins())
		{
			if (r.doableNow && matches(r, q)) // Recommended is strictly doable-now
			{
				rows.add(r);
			}
		}
		sortRows(rows);
		if (shuffleSeed != 0)
		{
			applyShuffle(rows);
		}

		if (rows.isEmpty())
		{
			content.add(messageLabel(q.isEmpty()
				? "No doable Combat Achievements right now."
				: "No CAs match your search."));
			return;
		}
		int shown = 0;
		for (SidePanelViewModel.TaskRow r : rows)
		{
			if (shown >= RENDER_CAP)
			{
				content.add(messageLabel("+" + (rows.size() - shown) + " more — refine your search."));
				break;
			}
			content.add(taskCard(r));
			content.add(spacer());
			shown++;
		}
	}

	private void sortRows(List<SidePanelViewModel.TaskRow> rows)
	{
		switch (sort)
		{
			case MOST_POINTS:
				rows.sort(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.points).reversed()
					.thenComparingInt(r -> r.id));
				break;
			case EASIEST:
				rows.sort(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.difficulty)
					.thenComparing(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.points).reversed())
					.thenComparingInt(r -> r.id));
				break;
			default: // RECOMMENDED: keep the model's ranked order
				break;
		}
	}

	/**
	 * Reorders the doable CAs with a bounded random rank jitter so each reshuffle surfaces a different but
	 * still-sensible set. Deterministic per {@link #shuffleSeed}, so per-tick refreshes don't re-scramble
	 * the list — only pressing the shuffle button (which bumps the seed) changes it.
	 */
	private void applyShuffle(List<SidePanelViewModel.TaskRow> rows)
	{
		Map<Integer, Double> key = new HashMap<>();
		for (int i = 0; i < rows.size(); i++)
		{
			SidePanelViewModel.TaskRow r = rows.get(i);
			key.put(r.id, jitteredKey(i, r.id, shuffleSeed));
		}
		rows.sort(Comparator.comparingDouble(r -> key.get(r.id)));
	}

	/**
	 * A task's jittered sort key: its rank plus a bounded, deterministic offset. Uses a splitmix64 hash of
	 * (seed, id) rather than {@link java.util.Random} — Random's first output is correlated across nearby
	 * seeds, which would make consecutive reshuffles produce almost the same order.
	 */
	static double jitteredKey(int rank, int id, long seed)
	{
		long h = seed * 0x9E3779B97F4A7C15L + (id + 1L) * 0xD1B54A32D192ED03L;
		h ^= h >>> 30;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 27;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 31;
		double u = (h >>> 11) * 0x1.0p-53; // uniform in [0, 1)
		return rank + (u * 2.0 - 1.0) * SHUFFLE_JITTER;
	}

	private JPanel taskCard(SidePanelViewModel.TaskRow row)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = row.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.LOCKED;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		String name = CombatAchievementsTheme.hex(row.doableNow
			? CombatAchievementsTheme.NAME : CombatAchievementsTheme.LOCKED);
		StringBuilder sb = new StringBuilder("<html><body style='width:166px'>");
		sb.append("<span style='color:").append(name).append("'><b>").append(escape(row.name)).append("</b></span>");
		if (!row.doableNow)
		{
			String lock = (row.lockReason == null || row.lockReason.isEmpty()) ? "locked" : row.lockReason;
			sb.append(" <span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
				.append("'>(").append(escape(lock)).append(")</span>");
		}
		if (!row.curated)
		{
			sb.append(" <span style='color:#6f6f6f'>&#9679;</span>");
		}
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
			.append("'>").append(escape(row.description)).append("</span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(row.points).append(" pts</span>")
			.append(" <span style='color:" + metaHex() + "'>· ").append(escape(row.tierName)).append("</span>");
		if (row.difficulty > 0)
		{
			sb.append(" <span style='color:" + metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(difficultyColor(row.difficulty)))
				.append("'>diff ").append(row.difficulty).append("</span>");
		}
		sb.append("</body></html>");
		JLabel label = new JLabel(sb.toString());
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		card.add(label, BorderLayout.CENTER);

		card.add(linkRow(row.wikiUrl, row.guideUrl, row.curatedVideo), BorderLayout.SOUTH);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		onClick(card, () -> {
			selectedCa = row.detail;
			rebuild();
		});
		return fullWidth(card);
	}

	/** Easy → mid → hard on the 1–10 Difficulty scale, using the theme's dedicated difficulty ramp so a
	 *  "diff N" never reads as the green points or a gold header sharing its line. */
	private static Color difficultyColor(int d)
	{
		if (d <= 0)
		{
			return CombatAchievementsTheme.DESC;
		}
		if (d <= 3)
		{
			return CombatAchievementsTheme.DIFF_EASY;
		}
		if (d <= 6)
		{
			return CombatAchievementsTheme.DIFF_MID;
		}
		return CombatAchievementsTheme.DIFF_HARD;
	}

	/** Themed grey for the muted "·" separators / secondary meta, as an HTML hex (follows the palette). */
	private static String metaHex()
	{
		return CombatAchievementsTheme.hex(CombatAchievementsTheme.NEUTRAL_META);
	}

	// ---- Bosses ------------------------------------------------------------------------------------

	private void buildBosses()
	{
		String q = searchText();
		List<SidePanelViewModel.BossRow> all = model.bosses();
		if (all == null || all.isEmpty())
		{
			content.add(messageLabel("No bosses with incomplete CAs."));
			return;
		}
		List<SidePanelViewModel.BossRow> bosses = new ArrayList<>();
		for (SidePanelViewModel.BossRow b : all)
		{
			if (q.isEmpty() || b.monster.toLowerCase(Locale.ROOT).contains(q))
			{
				bosses.add(b);
			}
		}
		if (bosses.isEmpty())
		{
			content.add(messageLabel("No bosses match your search."));
			return;
		}
		bosses.sort(bossComparator(sort, tripOverheadMinutes, bossTimeWeight));
		for (SidePanelViewModel.BossRow b : bosses)
		{
			content.add(bossRowCard(b));
			content.add(spacer());
		}
	}

	/**
	 * The boss-list ordering for a given {@link Sort}. Locked bosses (no doable CAs) always sink to the
	 * bottom. The metrics are derived from each boss's doable CAs:
	 * <ul>
	 *   <li>{@code RECOMMENDED} — points per hour with the per-trip overhead amortised in
	 *       ({@link #bossPointsPerHour}); because a CA's estimated minutes already scale with its
	 *       Difficulty and the fixed {@code tripOverheadMinutes} is spread across the whole visit, this is
	 *       the "points / time / ease" value blend that also rewards clustering (staying at one boss).</li>
	 *   <li>{@code MOST_POINTS} — projected (doable-now) points, highest first.</li>
	 *   <li>{@code EASIEST} — lowest average doable-CA Difficulty, tie-broken by quickest then most points.</li>
	 * </ul>
	 */
	static Comparator<SidePanelViewModel.BossRow> bossComparator(Sort sort, int tripOverheadMinutes)
	{
		return bossComparator(sort, tripOverheadMinutes, 1.0);
	}

	static Comparator<SidePanelViewModel.BossRow> bossComparator(Sort sort, int tripOverheadMinutes,
		double timeWeight)
	{
		Comparator<SidePanelViewModel.BossRow> lockedLast =
			Comparator.comparing((SidePanelViewModel.BossRow b) -> b.locked); // false (doable) first
		switch (sort)
		{
			case MOST_POINTS:
				return lockedLast
					.thenComparing(Comparator.comparingInt((SidePanelViewModel.BossRow b) -> b.projectedPoints).reversed())
					.thenComparing(b -> b.monster);
			case EASIEST:
				return lockedLast
					.thenComparing(Comparator.comparingDouble(CombatAchievementsPanel::bossAvgDifficulty))
					.thenComparing(Comparator.comparingInt(CombatAchievementsPanel::bossDoableMinutes))
					.thenComparing(Comparator.comparingInt((SidePanelViewModel.BossRow b) -> b.projectedPoints).reversed())
					.thenComparing(b -> b.monster);
			default: // RECOMMENDED
				return lockedLast
					.thenComparing(Comparator.comparingDouble(
						(SidePanelViewModel.BossRow b) -> bossPointsPerHour(b, tripOverheadMinutes, timeWeight)).reversed())
					.thenComparing(b -> b.monster);
		}
	}

	/** Total estimated minutes across a boss's doable CAs. */
	private static int bossDoableMinutes(SidePanelViewModel.BossRow b)
	{
		int minutes = 0;
		for (SidePanelViewModel.CaDetail d : b.doable)
		{
			minutes += d.estMinutes;
		}
		return minutes;
	}

	/**
	 * Points per hour for a boss's doable CAs, with the fixed per-trip overhead amortised into the time.
	 * Spreading {@code tripOverheadMinutes} across the whole visit rewards bosses with several doable CAs
	 * (clustering) over trivial single-CA hops. Nothing-to-gain bosses score 0; a free (zero-time,
	 * zero-overhead) prize ranks top.
	 */
	private static double bossPointsPerHour(SidePanelViewModel.BossRow b, int tripOverheadMinutes,
		double timeWeight)
	{
		if (b.projectedPoints <= 0)
		{
			return 0.0;
		}
		// Readiness rides on the row: a boss whose doable CAs sit far above the player's recommended stats
		// is worth less per hour to THEM, however good its points-per-hour looks in the abstract. Without
		// this the list was account-blind — a level-3 and a combat-89 account both led with Dagannoth Kings.
		double sink = Math.max(1.0, b.readinessSink);
		double denominator = sink
			* (Math.max(0, tripOverheadMinutes) + Math.max(0.0, timeWeight) * bossDoableMinutes(b));
		// No time cost at all (both dials off, or a zero-minute boss) → rank on points: a large sentinel keeps
		// such bosses above any real points-per-hour figure, and adding the points breaks their ties by points
		// (not alphabetically), so "Time vs points = 0" genuinely ranks on available points.
		return denominator <= 0 ? 1.0e12 + b.projectedPoints : b.projectedPoints * 60.0 / denominator;
	}

	/** Average Difficulty of a boss's doable CAs; MAX when none are rated (sinks below rated bosses). */
	private static double bossAvgDifficulty(SidePanelViewModel.BossRow b)
	{
		int sum = 0;
		int rated = 0;
		for (SidePanelViewModel.CaDetail d : b.doable)
		{
			if (d.difficulty > 0)
			{
				sum += d.difficulty;
				rated++;
			}
		}
		return rated == 0 ? Double.MAX_VALUE : (double) sum / rated;
	}

	private JPanel bossRowCard(SidePanelViewModel.BossRow b)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = b.locked ? CombatAchievementsTheme.LOCKED : CombatAchievementsTheme.NAME;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(6, 7, 6, 7)));

		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(accent))
			.append("'><b>").append(escape(b.monster)).append("</b></span>");
		if (b.locked)
		{
			String reason = "needs access";
			for (SidePanelViewModel.CaDetail lc : b.lockedCas)
			{
				if (!lc.lockReason.isEmpty())
				{
					reason = lc.lockReason; // usually "needs <quest>" — name the specific gate
					break;
				}
			}
			sb.append(" <span style='color:" + metaHex() + "'>(locked)</span>");
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(escape(reason)).append("</span>")
				.append(" <span style='color:" + metaHex() + "'>· ").append(b.lockedCount)
				.append(b.lockedCount == 1 ? " CA</span>" : " CAs</span>");
		}
		else
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
				.append("'>").append(b.projectedPoints).append(" pts available</span>")
				.append(" <span style='color:" + metaHex() + "'>· ").append(b.doableCount)
				.append(b.doableCount == 1 ? " CA" : " CAs");
			if (b.lockedCount > 0)
			{
				sb.append(" (+").append(b.lockedCount).append(" locked)");
			}
			sb.append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		onClick(card, () -> {
			selectedBoss = b.monster;
			rebuild();
		});
		return fullWidth(card);
	}

	// ---- Route -------------------------------------------------------------------------------------

	/** The route to the next tier: unlock shortcuts first (a quest can open points faster than grinding),
	 *  then the CA steps. The unlock section is collapsible. */
	private void buildRoute()
	{
		SidePanelViewModel.PathView path = model.path();
		List<SidePanelViewModel.UnlockView> unlocks = model.unlocks();
		boolean haveUnlocks = unlocks != null && !unlocks.isEmpty();

		if (path == null && !haveUnlocks)
		{
			content.add(messageLabel("Log in to see your route."));
			return;
		}

		// Unlocks on top: doing a quest can open a chunk of points faster than grinding CAs. Hideable.
		if (haveUnlocks)
		{
			content.add(collapseHeader("Unlock next", unlocksCollapsed,
				() -> { unlocksCollapsed = !unlocksCollapsed; rebuild(); }));
			if (!unlocksCollapsed)
			{
				content.add(spacer());
				for (SidePanelViewModel.UnlockView u : unlocks)
				{
					content.add(unlockCard(u));
					content.add(spacer());
				}
			}
			content.add(spacer());
		}

		// "Train next": only present when the account is actually held back by levels, so it quietly
		// disappears once you can attempt things — no empty section for an established player.
		List<SidePanelViewModel.TrainingView> trainings = model.trainings();
		if (trainings != null && !trainings.isEmpty())
		{
			content.add(collapseHeader("Train next", trainingsCollapsed,
				() -> { trainingsCollapsed = !trainingsCollapsed; rebuild(); }));
			if (!trainingsCollapsed)
			{
				content.add(spacer());
				for (SidePanelViewModel.TrainingView t : trainings)
				{
					content.add(trainingCard(t));
					content.add(spacer());
				}
			}
			content.add(spacer());
		}

		if (path != null)
		{
			StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
			sb.append("Goal: <b>").append(escape(path.targetTierName)).append("</b>");
			if (path.alreadyUnlocked)
			{
				sb.append("<br><span style='color:")
					.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
					.append("'>Already unlocked.</span>");
			}
			else
			{
				sb.append("<br>").append(path.pointsGap).append(" pts to go");
				if (path.trainFirst)
				{
					// The route stopped at what the account is ready for rather than padding it out with
					// content 40+ levels away. "Train next" sits directly above with the way forward.
					// Kept to two short lines: a long line clips mid-word at this panel width rather than
					// wrapping (a JLabel sizes itself from its own preferred width, so the body width style
					// does not rescue it).
					int within = path.steps.size();
					sb.append("<br><span style='color:")
						.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.ACCENT))
						.append("'>").append(within).append(within == 1 ? " CA" : " CAs")
						.append(" within reach.<br>Train for the rest.</span>");
				}
				else if (!path.reachable)
				{
					sb.append("<br><span style='color:")
						.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.ACCENT))
						.append("'>Not enough doable tasks yet —<br>shows the closest set.</span>");
				}
			}
			sb.append("</body></html>");
			content.add(fullWidth(new JLabel(sb.toString())));
			content.add(spacer());

			// Only CAs the player can go and do right now, grouped by boss so one trip clears several.
			// Nothing out of reach and nothing behind a quest: the Route is a plan to follow, and listing
			// content that cannot be attempted made the whole thing read as if it were all available. The
			// quest that would open more is recommended above, in Unlock next, where it can be acted on.
			List<SidePanelViewModel.CaDetail> route = new ArrayList<>();
			for (SidePanelViewModel.PathRow step : path.steps)
			{
				if (step.detail != null)
				{
					route.add(step.detail);
				}
			}
			renderRouteGroups(route);
		}
	}

	/**
	 * Renders the route clustered by boss so the same boss's tasks sit together (one trip), with the groups
	 * ordered by their quickest task. A boss with two or more tasks gets a header.
	 */
	private void renderRouteGroups(List<SidePanelViewModel.CaDetail> route)
	{
		LinkedHashMap<String, List<SidePanelViewModel.CaDetail>> byBoss = new LinkedHashMap<>();
		Set<Integer> seen = new HashSet<>();
		for (SidePanelViewModel.CaDetail c : route)
		{
			// A Grandmaster (complete-all) route can list a CA as both a step and a locked CA — dedupe
			// by id (keep the first, the doable step) so it is not rendered twice or double-counted.
			if (!seen.add(c.id))
			{
				continue;
			}
			// No-boss tasks stay solo (unique key); real bosses cluster (case-insensitive).
			String key = c.monster.isEmpty() ? ("solo:" + c.id) : c.monster.toLowerCase(Locale.ROOT);
			byBoss.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
		}
		// Groups keep the order the solver put their first task in, which already accounts for how ready the
		// player is. Sorting groups by raw time instead let one quick outlier drag a whole boss forward —
		// Bryophyta led the route off the back of "Quick Cutter" (kill a growthling with an axe) while its
		// other six tasks want 50 combat.
		Map<String, Integer> firstSeen = new LinkedHashMap<>();
		int order = 0;
		for (SidePanelViewModel.CaDetail c : route)
		{
			String key = c.monster.isEmpty() ? ("solo:" + c.id) : c.monster.toLowerCase(Locale.ROOT);
			firstSeen.putIfAbsent(key, order++);
		}
		List<Map.Entry<String, List<SidePanelViewModel.CaDetail>>> entries =
			new ArrayList<>(byBoss.entrySet());
		for (Map.Entry<String, List<SidePanelViewModel.CaDetail>> e : entries)
		{
			e.getValue().sort(Comparator.comparingInt(SidePanelViewModel.CaDetail::totalMinutes)
				.thenComparingInt(c -> -c.points));
		}
		entries.sort(Comparator.comparingInt(e -> firstSeen.getOrDefault(e.getKey(), Integer.MAX_VALUE)));
		List<List<SidePanelViewModel.CaDetail>> groups = new ArrayList<>();
		for (Map.Entry<String, List<SidePanelViewModel.CaDetail>> e : entries)
		{
			groups.add(e.getValue());
		}
		for (List<SidePanelViewModel.CaDetail> g : groups)
		{
			SidePanelViewModel.CaDetail first = g.get(0);
			// Every real boss gets a header, even a single-task one: a lone card sandwiched between two
			// headed clusters used to read as if it belonged to the cluster above it.
			boolean grouped = !first.monster.isEmpty();
			if (grouped)
			{
				content.add(routeGroupHeader(first.monster, g.size()));
			}
			for (SidePanelViewModel.CaDetail c : g)
			{
				content.add(routeCaCard(c, grouped));
				content.add(spacer());
			}
		}
	}

	/** A small boss header for a route cluster: "General Graardor · 3 tasks". */
	private JPanel routeGroupHeader(String boss, int count)
	{
		// Just the boss name: the cards beneath it already show how many there are, and keeping it short
		// avoids the clipping a longer label hit ("Deranged Archaeologist · 2 tasks" cut off mid-word —
		// Swing sizes an HTML label from its own preferred width, so a body-width wrap doesn't rescue it).
		JLabel label = new JLabel(escape(boss));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);

		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
		row.add(label, BorderLayout.CENTER);
		return fullWidth(row);
	}

	private JPanel routeCaCard(SidePanelViewModel.CaDetail c, boolean grouped)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = c.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.NEGATIVE;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(accent))
			.append("'><b>").append(escape(c.name)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(c.points).append(" pts</span>");
		if (c.difficulty > 0)
		{
			sb.append(" <span style='color:" + metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(difficultyColor(c.difficulty)))
				.append("'>diff ").append(c.difficulty).append("</span>");
		}
		// Time estimates are intentionally not shown — the engine still uses them for ordering.
		// When not under a boss header, name the boss so a solo route step still tells you where to go.
		if (!grouped && !c.monster.isEmpty())
		{
			sb.append("<br><span style='color:" + metaHex() + "'>").append(escape(c.monster)).append("</span>");
		}
		if (!c.doableNow && !c.lockReason.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(escape(c.lockReason)).append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		onClick(card, () -> {
			selectedCa = c;
			rebuild();
		});
		return fullWidth(card);
	}

	/** A bold gold section header that toggles a collapsed section when clicked (▸ collapsed / ▾ open). */
	private JPanel collapseHeader(String text, boolean collapsed, Runnable onToggle)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		JLabel label = new JLabel((collapsed ? "▸ " : "▾ ") + escape(text));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		row.add(label, BorderLayout.WEST);
		row.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				onToggle.run();
			}
		});
		return fullWidth(row);
	}

	/** A "train X to N" goal: what it opens and roughly how long, styled as a quieter sibling of unlockCard. */
	private JPanel trainingCard(SidePanelViewModel.TrainingView t)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.ACCENT),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.HEADER_GOLD))
			.append("'><b>").append(escape(t.label)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
			.append("'>opens ").append(t.unlockedTaskCount).append(" CAs (").append(t.unlockedPoints)
			.append(" pts)</span>");
		if (t.unlocksHint != null && !t.unlocksHint.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
				.append("'>mostly ").append(escape(t.unlocksHint)).append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return fullWidth(card);
	}

	private JPanel unlockCard(SidePanelViewModel.UnlockView u)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.HEADER_GOLD),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.HEADER_GOLD))
			.append("'><b>").append(escape(u.questName)).append("</b></span>");
		if (u.difficulty != null && !u.difficulty.isEmpty())
		{
			sb.append(" <span style='color:" + metaHex() + "'>· ").append(escape(u.difficulty)).append("</span>");
		}
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
			.append("'>unlocks ").append(u.unlockedTaskCount).append(" CAs (").append(u.unlockedPoints)
			.append(" pts)</span>");
		if (u.prerequisites != null && !u.prerequisites.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
				.append("'>first: ").append(escape(u.prerequisites)).append("</span>");
		}
		if (u.unmetSkills != null && !u.unmetSkills.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
				.append("'>train: ").append(escape(u.unmetSkills)).append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return fullWidth(card);
	}

	// ---- CA detail & Boss detail -------------------------------------------------------------------

	private void renderCaDetail(SidePanelViewModel.CaDetail d)
	{
		content.add(backButton("← Back", () -> {
			selectedCa = null;
			rebuild();
		}));
		content.add(spacer());

		StringBuilder crumb = new StringBuilder();
		if (!d.monster.isEmpty())
		{
			crumb.append(escape(d.monster)).append(" · ");
		}
		crumb.append(escape(d.tierName)).append(" · ").append(d.points).append(" pts");
		if (!d.type.isEmpty())
		{
			crumb.append(" · ").append(escape(d.type));
		}

		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NAME))
			.append("'><b style='font-size:11px'>").append(escape(d.name)).append("</b></span>");
		sb.append("<br><span style='color:" + metaHex() + "'>").append(crumb).append("</span>");
		if (!d.description.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
				.append("'>").append(escape(d.description)).append("</span>");
		}
		if (!d.doableNow && !d.lockReason.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(escape(d.lockReason)).append("</span>");
		}
		sb.append("</body></html>");
		content.add(fullWidth(new JLabel(sb.toString())));
		content.add(spacer());

		if (!d.requirements.isEmpty())
		{
			content.add(collapseHeader("Requirements", !reqsExpanded,
				() -> { reqsExpanded = !reqsExpanded; rebuild(); }));
			if (reqsExpanded)
			{
				content.add(spacer());
				for (SidePanelViewModel.CaReq r : d.requirements)
				{
					content.add(reqRow(r));
				}
			}
			content.add(spacer());
		}

		content.add(sectionHeader("Difficulty"));
		double rating = displayedDifficulty(d);
		StringBuilder diff = new StringBuilder("<html><body style='width:182px'>");
		diff.append("<span style='color:").append(CombatAchievementsTheme.hex(difficultyColor(d.difficulty)))
			.append("'>").append(String.format(Locale.ROOT, "%.1f", rating)).append(" / 10</span>");
		if (d.bossDifficulty > 0)
		{
			diff.append("<br><span style='color:" + metaHex() + "'>").append(escape(d.monster)).append(" (boss ")
				.append(d.bossDifficulty).append(")");
			if (d.bump != 0 && !d.difficultyReason.isEmpty())
			{
				diff.append(" + ").append(escape(d.difficultyReason))
					.append(String.format(Locale.ROOT, " (+%.1f)", d.bump));
			}
			diff.append("</span>");
		}
		diff.append("</body></html>");
		content.add(fullWidth(new JLabel(diff.toString())));
		content.add(spacer());


		// Curated how-to (stats/setup/items/strategy) behind a collapsible arrow so the detail stays lean;
		// the config option seeds whether it starts expanded.
		boolean hasHowTo = !d.stats.isEmpty() || !d.setup.isEmpty() || !d.items.isEmpty()
			|| !d.strategy.isEmpty();
		if (hasHowTo)
		{
			content.add(collapseHeader("How to do it", !howToExpanded,
				() -> { howToExpanded = !howToExpanded; rebuild(); }));
			if (howToExpanded)
			{
				content.add(spacer());
				addDetailText("Recommended stats", d.stats);
				addDetailText("Recommended setup", d.setup);
				addDetailText("Items", d.items);
				addDetailText("Strategy", d.strategy);
			}
			content.add(spacer());
		}

		content.add(fullWidth(linkRow(d.wikiUrl, d.guideUrl, d.curatedVideo, feedbackUrl(d))));
	}

	private void addDetailText(String header, String text)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		content.add(sectionHeader(header));
		// A JTextArea rather than an HTML JLabel: the label's body-width style is unreliable here — a long
		// word ("thrownhammer") keeps the whole line intact and it clips at the panel edge. A word-wrapping
		// text area reflows to its actual width every time, which is the whole point.
		JTextArea body = new JTextArea(text);
		body.setLineWrap(true);
		body.setWrapStyleWord(true);
		body.setEditable(false);
		body.setFocusable(false);
		body.setOpaque(false);
		body.setBorder(null);
		body.setFont(FontManager.getRunescapeSmallFont());
		body.setForeground(CombatAchievementsTheme.DESC);
		body.setAlignmentX(Component.LEFT_ALIGNMENT);
		// A word-wrapping JTextArea reports a one-line preferred height until it knows its width, so under
		// BoxLayout it would collapse to a single line. Fix the width first, then the reported height is the
		// wrapped height; pin both so the layout gives it exactly that.
		body.setSize(DETAIL_TEXT_WIDTH, Short.MAX_VALUE);
		int wrappedHeight = body.getPreferredSize().height;
		body.setPreferredSize(new Dimension(DETAIL_TEXT_WIDTH, wrappedHeight));
		body.setMaximumSize(new Dimension(DETAIL_TEXT_WIDTH, wrappedHeight));
		content.add(body);
		content.add(spacer());
	}

	private JLabel reqRow(SidePanelViewModel.CaReq r)
	{
		Color colour = r.met ? CombatAchievementsTheme.POSITIVE : CombatAchievementsTheme.NEGATIVE;
		String mark = r.met ? "&#10003;" : "&#10007;"; // check / cross
		StringBuilder sb = new StringBuilder("<html><body style='width:182px'><span style='color:")
			.append(CombatAchievementsTheme.hex(colour)).append("'>").append(mark).append(" ")
			.append(escape(r.label)).append("</span>");
		if (!r.note.isEmpty())
		{
			sb.append(" <span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>— ").append(escape(r.note)).append("</span>");
		}
		sb.append("</body></html>");
		return fullWidth(new JLabel(sb.toString()));
	}

	private void buildBossDetail(String monster)
	{
		content.add(backButton("← All bosses", () -> {
			selectedBoss = null;
			rebuild();
		}));
		content.add(spacer());

		SidePanelViewModel.BossRow boss = null;
		for (SidePanelViewModel.BossRow b : model.bosses())
		{
			if (monster.equals(b.monster))
			{
				boss = b;
				break;
			}
		}
		if (boss == null)
		{
			content.add(messageLabel("Boss not found."));
			return;
		}

		StringBuilder head = new StringBuilder("<html><body style='width:182px'><span style='color:")
			.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NAME))
			.append("'><b style='font-size:11px'>").append(escape(boss.monster)).append("</b></span>");
		if (boss.locked)
		{
			head.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>Locked — no doable CAs yet.</span>");
		}
		else
		{
			head.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
				.append("'>").append(boss.projectedPoints).append(" pts available</span>")
				.append(" <span style='color:" + metaHex() + "'>· ").append(boss.doableCount)
				.append(boss.doableCount == 1 ? " CA</span>" : " CAs</span>");
		}
		head.append("</body></html>");
		content.add(fullWidth(new JLabel(head.toString())));
		content.add(spacer());

		if (!boss.recommendedStats.isEmpty())
		{
			addDetailText("Recommended stats", boss.recommendedStats);
		}

		// "Doable" only means no hard gate blocks you, so a level-3 saw seven Barrows CAs it was 49-84
		// levels short of, all reading as available. Split on the ready line: what you could go and do
		// now, and below it what is ungated but out of reach. Nothing is hidden either way.
		List<SidePanelViewModel.CaDetail> reachable = new ArrayList<>();
		List<SidePanelViewModel.CaDetail> notYet = new ArrayList<>();
		for (SidePanelViewModel.CaDetail d : boss.doable)
		{
			(d.withinReach ? reachable : notYet).add(d);
		}
		if (!reachable.isEmpty())
		{
			content.add(sectionHeader(notYet.isEmpty() ? "Doable CAs" : "Within reach"));
			content.add(spacer());
			for (SidePanelViewModel.CaDetail d : reachable)
			{
				content.add(caCard(d));
				content.add(spacer());
			}
		}
		if (!notYet.isEmpty())
		{
			content.add(sectionHeader("Not yet (" + notYet.size() + ")"));
			content.add(spacer());
			for (SidePanelViewModel.CaDetail d : notYet)
			{
				content.add(caCard(d));
				content.add(spacer());
			}
		}
		if (!boss.lockedCas.isEmpty())
		{
			content.add(sectionHeader("Locked (+" + boss.lockedCas.size() + ")"));
			content.add(spacer());
			for (SidePanelViewModel.CaDetail d : boss.lockedCas)
			{
				content.add(caCard(d));
				content.add(spacer());
			}
		}
	}

	/** A clickable CA row from a CaDetail — orange (doable) or red (locked) — opening the CA detail. */
	private JPanel caCard(SidePanelViewModel.CaDetail d)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = d.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.NEGATIVE;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		StringBuilder sb = new StringBuilder("<html><body style='width:182px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(accent))
			.append("'><b>").append(escape(d.name)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(d.points).append(" pts</span> <span style='color:" + metaHex() + "'>· ")
			.append(escape(d.tierName)).append("</span>");
		if (d.difficulty > 0)
		{
			sb.append(" <span style='color:" + metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(difficultyColor(d.difficulty)))
				.append("'>diff ").append(d.difficulty).append("</span>");
		}
		if (!d.doableNow && !d.lockReason.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(escape(d.lockReason)).append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		onClick(card, () -> {
			selectedCa = d;
			rebuild();
		});
		return fullWidth(card);
	}

	private JButton backButton(String text, Runnable action)
	{
		JButton back = new JButton(text);
		back.setMargin(new Insets(1, 6, 1, 6));
		back.setFocusPainted(false);
		back.setFont(FontManager.getRunescapeSmallFont());
		back.addActionListener(e -> action.run());
		return back;
	}

	// ---- Shared helpers ----------------------------------------------------------------------------

	private JLabel sectionHeader(String text)
	{
		JLabel label = new JLabel(escape(text));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		return fullWidth(label);
	}

	private JPanel linkRow(String wikiUrl, String guideUrl, boolean curatedVideo)
	{
		return linkRow(wikiUrl, guideUrl, curatedVideo, "");
	}

	private JPanel linkRow(String wikiUrl, String guideUrl, boolean curatedVideo, String feedbackUrl)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
		row.setOpaque(false);
		if (wikiUrl != null && !wikiUrl.isEmpty())
		{
			row.add(linkButton("Wiki", wikiUrl));
		}
		if (guideUrl != null && !guideUrl.isEmpty())
		{
			row.add(linkButton(curatedVideo ? "Watch guide" : "Search guide", guideUrl));
		}
		if (feedbackUrl != null && !feedbackUrl.isEmpty())
		{
			row.add(linkButton("Suggest fix", feedbackUrl));
		}
		return row;
	}

	/** The Difficulty rating the panel shows: the boss rating plus this task's bump, else the raw value. */
	private static double displayedDifficulty(SidePanelViewModel.CaDetail d)
	{
		return d.bossDifficulty > 0 ? d.bossDifficulty + d.bump : d.difficulty;
	}

	/**
	 * The "Suggest fix" link for a task, or "" when no feedback form is configured (button hidden). Only
	 * the task id is sent — the player is already looking at the achievement, so the form asks them for
	 * the difficulty they'd give it and nothing they shouldn't have to type.
	 */
	private static String feedbackUrl(SidePanelViewModel.CaDetail d)
	{
		return FeedbackLink.isConfigured() ? FeedbackLink.taskUrl(d.id) : "";
	}

	private JButton linkButton(String text, String url)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		button.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		button.addActionListener(e -> LinkBrowser.browse(url));
		addHover(button, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return button;
	}

	private JLabel messageLabel(String text)
	{
		JLabel label = new JLabel("<html><body style='width:182px'>" + escape(text) + "</body></html>");
		label.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		return fullWidth(label);
	}

	private static JPanel spacer()
	{
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setPreferredSize(new Dimension(1, 6));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		return p;
	}

	private static <T extends JPanel> T fullWidth(T panel)
	{
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private static JLabel fullWidth(JLabel label)
	{
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static String escape(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
