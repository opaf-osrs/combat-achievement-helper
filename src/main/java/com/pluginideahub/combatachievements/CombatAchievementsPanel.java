package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.debug.DebugSimulation;
import com.pluginideahub.combatachievements.core.feedback.FeedbackLink;
import com.pluginideahub.combatachievements.core.ui.PanelAction;
import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import com.pluginideahub.combatachievements.ui.Palette;
import com.pluginideahub.combatachievements.ui.PanelRoute;
import com.pluginideahub.combatachievements.ui.pages.BossDetailPage;
import com.pluginideahub.combatachievements.ui.pages.BossesPage;
import com.pluginideahub.combatachievements.ui.pages.CaDetailPage;
import com.pluginideahub.combatachievements.ui.pages.RecommendedPage;
import com.pluginideahub.combatachievements.ui.pages.RoutePage;
import com.pluginideahub.combatachievements.ui.pages.UnlockDetailPage;
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
import java.awt.image.BufferedImage;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
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
	public enum Sort
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
	/** Discord invite for the footer icon. Empty = the icon is hidden, so a blank never ships a dead link. */
	private static final String DISCORD_URL = "https://discord.gg/N7HUvV92a";
	/** Default per-visit overhead (minutes) amortised into the Bosses "Recommended" sort; matches the
	 *  {@code tripOverheadMinutes} config default so the two agree until the plugin seeds the live value. */
	private static final int DEFAULT_TRIP_OVERHEAD = 6;
	/** Pinned size of a developer-mode level spinner — see {@link #devSkillCell(String)}. */
	private static final int SPINNER_WIDTH = 46;
	private static final int SPINNER_HEIGHT = 18;
	/** Wrap width for card text: the panel minus the card's left bar and padding. */
	public static final int CARD_TEXT_WIDTH = 182;
	/** Route card text width, and the slice the per-CA "-" (bar) control takes on the right. */
	public static final int ROUTE_TEXT_WIDTH = 182;
	public static final int BAR_BUTTON_WIDTH = 20;
	/**
	 * Hard cap on a route card's width. The content column sizes itself to its widest child, and a
	 * long unlock card can push that past the ~225px panel; a full-width route card would then put
	 * its right-hand "-" outside the visible area. Capping keeps the control on screen.
	 */
	public static final int ROUTE_CARD_MAX_WIDTH = 211;

	private final transient Consumer<PanelAction> onAction;
	/** Bar a task from the Route (the "−" on a route card); the plugin persists it and re-solves. */
	private transient Consumer<Integer> onBarTask;
	/** Put one barred task back in the running. */
	private transient Consumer<Integer> onUnbarTask;
	/** Add a CA to the route (pin it) from a boss page. */
	private transient Consumer<Integer> onAddToRoute;
	/** Remove a CA from the route from a boss page (unpin if pinned, otherwise bar). */
	private transient Consumer<Integer> onRemoveFromRoute;
	/** Clear every barred task, putting them all back in the running. */
	private transient Runnable onClearBarred;

	private transient SidePanelViewModel model = SidePanelViewModel.loggedOut();
	/** Where the panel is: the active mode plus any drill-in selections layered over it. */
	private transient PanelRoute route = PanelRoute.of(PanelMode.RECOMMENDED);
	private Sort sort = Sort.RECOMMENDED;
	/** Shuffle seed for the CAs list; 0 = natural (model) order, each reshuffle bumps it. */
	private long shuffleSeed;
	private boolean unlocksCollapsed;
	private boolean trainingsCollapsed;
	/** Whether the Route's barred ("Not doing these") section is collapsed. */
	private boolean barredCollapsed = true;
	/** Whether a boss page's "Completed" section is collapsed. */
	private boolean completedCollapsed = true;
	// The boss page's three outstanding groups. All start open, so the page reads as it always has;
	// collapsing is there for when one of them is long and in the way.
	private boolean doableCollapsed;
	private boolean trainFirstCollapsed;
	private boolean lockedCollapsed;
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
	/** Boss groups the user has opened on the quest page; all start closed, reset per quest. */
	private final Set<String> expandedUnlockBosses = new HashSet<>();
	/** Whether the quest page's "Quests first" chain is unfolded; starts closed, reset per quest. */
	private boolean unlockPrereqsExpanded;
	/** Whether the quest page's "Stats first" list is unfolded; starts closed, reset per quest. */
	private boolean unlockStatsExpanded;

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
	/** Header Discord button, kept so a theme change can re-tint its icon. */
	private transient JButton discordButton;
	private final JPanel devSection = new JPanel(new BorderLayout());
	private final JPanel devBody = new JPanel();
	private final JLabel devHeader = new JLabel();
	private final JCheckBox devZeroCas = new JCheckBox("No CAs completed");
	private final JCheckBox devZeroQuests = new JCheckBox("No quests done");
	private final transient Map<String, JSpinner> devSpinners = new LinkedHashMap<>();
	private final JTextField searchField = new JTextField();
	private final JComboBox<Sort> sortBox = new JComboBox<>(Sort.values());
	private final JButton shuffleButton = new JButton("⟳");
	/** Clears every pinned/barred CA, putting the Route back to the solver's own answer. */
	/** True when anything is pinned or barred; the reset control only exists when there is something to reset. */
	private boolean routeCustomised;
	private transient Runnable onResetCustom;
	private final JLabel orderLabel = new JLabel("Order ");
	private final JPanel modeBar = new JPanel(new GridBagLayout());
	private final JPanel controlBar = new JPanel(new BorderLayout(0, 6));
	private final JPanel orderRow = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel();

	// One renderer per page, all drawing into `content`; rebuild() dispatches to whichever the route says.
	private final transient RecommendedPage recommendedPage;
	private final transient BossesPage bossesPage;
	private final transient BossDetailPage bossDetailPage;
	private final transient RoutePage routePage;
	private final transient UnlockDetailPage unlockDetailPage;
	private final transient CaDetailPage caDetailPage;

	public CombatAchievementsPanel(Consumer<PanelAction> onAction)
	{
		super(false);
		this.onAction = onAction;
		caDetailPage = new CaDetailPage(this);
		recommendedPage = new RecommendedPage(this);
		bossesPage = new BossesPage(this);
		bossDetailPage = new BossDetailPage(this, caDetailPage);
		routePage = new RoutePage(this);
		unlockDetailPage = new UnlockDetailPage(this, routePage);

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
		if (!DISCORD_URL.isEmpty())
		{
			discordButton = discordLink();
			titleRow.add(discordButton, BorderLayout.EAST);
		}
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
		CardKit.onClick(headerRow, () -> setDevCollapsed(!devCollapsed));
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
		devBody.add(CardKit.fullWidth(presets));
		devBody.add(CardKit.spacer());

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
		devBody.add(CardKit.fullWidth(switches));
		devBody.add(CardKit.spacer());

		// Two columns: at three, the label plus the spinner's arrows exceed the cell and the last column's
		// spinner is clipped off the panel.
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 2));
		grid.setOpaque(false);
		for (String skill : DebugSimulation.SKILLS)
		{
			grid.add(devSkillCell(skill));
		}
		devBody.add(CardKit.fullWidth(grid));

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

		footer.add(footerLink("Support on Patreon", PATREON_URL, "Opens patreon.com in your browser"),
			BorderLayout.WEST);

		// Right-hand side: the quiet "tell us something" links. Same muted styling as the Patreon link,
		// so the whole footer reads as chrome rather than as calls to action.
		JPanel links = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		links.setOpaque(false);
		if (FeedbackLink.hasGeneralForm())
		{
			links.add(footerLink("Feedback", FeedbackLink.generalUrl(),
				"Opens the feedback form in your browser"));
		}
		if (links.getComponentCount() > 0)
		{
			footer.add(links, BorderLayout.EAST);
		}
		return footer;
	}

	/**
	 * The Discord mark as a footer button. The bundled PNG is a white silhouette with alpha, tinted here
	 * to the footer's meta-grey and to gold for the hover state, so the icon tracks the active theme
	 * instead of needing a recoloured asset per palette.
	 */
	private JButton discordLink()
	{
		BufferedImage mark = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "discord.png");
		JButton link = new JButton(new ImageIcon(tint(mark, CombatAchievementsTheme.NEUTRAL_META)));
		link.setRolloverIcon(new ImageIcon(tint(mark, CombatAchievementsTheme.HEADER_GOLD)));
		link.setRolloverEnabled(true);
		link.setFocusPainted(false);
		link.setBorderPainted(false);
		link.setContentAreaFilled(false);
		link.setOpaque(false);
		link.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		link.setMargin(new Insets(0, 0, 0, 0));
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setToolTipText("Opens the Discord invite in your browser");
		link.addActionListener(e -> LinkBrowser.browse(DISCORD_URL));
		return link;
	}

	/** Re-applies the current theme's colours to the header Discord icon (it outlives a rebuild). */
	private void retintDiscord()
	{
		if (discordButton == null)
		{
			return;
		}
		BufferedImage mark = ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "discord.png");
		discordButton.setIcon(new ImageIcon(tint(mark, CombatAchievementsTheme.NEUTRAL_META)));
		discordButton.setRolloverIcon(new ImageIcon(tint(mark, CombatAchievementsTheme.HEADER_GOLD)));
	}

	/** Mixes two colours, {@code t} of the way from {@code a} to {@code b}. */
	public static Color blend(Color a, Color b, double t)
	{
		double k = Math.max(0.0, Math.min(1.0, t));
		return new Color(
			(int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
			(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
			(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k));
	}

	/** Recolours a white-with-alpha silhouette, keeping its alpha so the edges stay smooth. */
	private static BufferedImage tint(BufferedImage src, Color colour)
	{
		BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		int rgb = colour.getRGB() & 0x00FFFFFF;
		for (int y = 0; y < src.getHeight(); y++)
		{
			for (int x = 0; x < src.getWidth(); x++)
			{
				out.setRGB(x, y, (src.getRGB(x, y) & 0xFF000000) | rgb);
			}
		}
		return out;
	}

	/** One muted footer link: plain text, meta-grey, warming to gold on hover. */
	private JButton footerLink(String text, String url, String tooltip)
	{
		JButton link = new JButton(text);
		link.setFont(FontManager.getRunescapeSmallFont());
		link.setFocusPainted(false);
		link.setBorderPainted(false);
		link.setContentAreaFilled(false);
		link.setOpaque(false);
		link.setForeground(CombatAchievementsTheme.NEUTRAL_META);
		link.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		link.setMargin(new Insets(0, 0, 0, 0));
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.setToolTipText(tooltip);
		link.addActionListener(e -> LinkBrowser.browse(url));
		CardKit.addForegroundHover(link, CombatAchievementsTheme.NEUTRAL_META,
			CombatAchievementsTheme.HEADER_GOLD);
		return link;
	}

	private void emit(PanelAction action)
	{
		if (onAction != null)
		{
			onAction.accept(action);
		}
	}

	public void buildModeBar()
	{
		modeBar.removeAll();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.gridy = 0;
		PanelMode[] modes = PanelMode.values();
		for (int i = 0; i < modes.length; i++)
		{
			PanelMode mode = modes[i];
			boolean sel = mode == route.mode();
			JButton button = new JButton(mode.label());
			button.setFont(FontManager.getRunescapeSmallFont());
			button.setFocusPainted(false);
			button.setBorderPainted(false);
			button.setOpaque(true);
			button.setBackground(sel ? CombatAchievementsTheme.MODE_SELECTED : ColorScheme.DARK_GRAY_COLOR);
			button.setForeground(sel ? CombatAchievementsTheme.MODE_SELECTED_TEXT : ColorScheme.LIGHT_GRAY_COLOR);
			button.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
			button.setMargin(new Insets(0, 0, 0, 0));
			button.addActionListener(e -> switchMode(mode));
			if (!sel)
			{
				CardKit.addHover(button, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
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
		CardKit.addHover(b, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
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
		CardKit.addHover(b, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
	}

	/** Selects a mode fresh — every drill-in closed — and re-renders the bar and the content. */
	private void switchMode(PanelMode mode)
	{
		route = PanelRoute.of(mode);
		buildModeBar();
		rebuild();
	}

	/** Switches the active mode and re-renders (used by the headless preview harness and tests). */
	public void showMode(PanelMode mode)
	{
		switchMode(mode);
	}

	/** Preview/test hook: opens the CA-detail view for the first doable CA. */
	public void openFirstCaDetail()
	{
		for (SidePanelViewModel.TaskRow r : model.quickWins())
		{
			if (r.doableNow && r.detail != null)
			{
				route = route.withCa(r.detail);
				rebuild();
				return;
			}
		}
	}

	/** Preview/test hook: opens the boss-detail view for the first boss. */
	/** Opens the first boss that has something completed — used by the preview to show that section. */
	public void openFirstBossWithCompletions()
	{
		for (SidePanelViewModel.BossRow b : model.bosses())
		{
			if (!b.completedCas.isEmpty())
			{
				route = route.toBoss(b.monster);
				buildModeBar();
				rebuild();
				return;
			}
		}
	}

	public void openFirstBossDetail()
	{
		if (model.sessions() != null && !model.sessions().isEmpty())
		{
			route = route.toBoss(model.sessions().get(0).monster);
			buildModeBar();
			rebuild();
		}
	}

	/** Preview/test hook: opens the quest-unlock drill-in for the Route's first "Unlock next" card. */
	public void openFirstUnlockDetail()
	{
		if (model.unlocks() != null && !model.unlocks().isEmpty())
		{
			route = PanelRoute.of(PanelMode.ROUTE).withUnlock(model.unlocks().get(0));
			expandedUnlockBosses.clear();
			unlockPrereqsExpanded = false;
			unlockStatsExpanded = false;
			buildModeBar();
			rebuild();
		}
	}

	/** Preview/test hook: opens the first boss group on the quest-unlock page (they start closed). */
	public void expandFirstUnlockBoss()
	{
		if (route.unlock() != null && !route.unlock().unlockedCas.isEmpty())
		{
			expandedUnlockBosses.add(route.unlock().unlockedCas.get(0).monster);
			rebuild();
		}
	}

	/** Preview/test hook: opens the detail for the first LOCKED route CA (shows red unmet requirements). */
	public void openFirstLockedCaDetail()
	{
		if (model.path() != null && !model.path().lockedCas.isEmpty())
		{
			route = PanelRoute.of(PanelMode.ROUTE).withCa(model.path().lockedCas.get(0));
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

	/** Wires the Route's bar/restore controls to the plugin, which owns and persists the barred set. */
	public void setBarHandlers(Consumer<Integer> barTask, Runnable clearBarred)
	{
		setBarHandlers(barTask, null, clearBarred);
	}

	/** Wires the boss page's add-to / remove-from route controls. */
	public void setRouteHandlers(Consumer<Integer> addToRoute, Consumer<Integer> removeFromRoute)
	{
		this.onAddToRoute = addToRoute;
		this.onRemoveFromRoute = removeFromRoute;
	}

	/** Wires the Route's "reset my customisation" control. */
	public void setResetCustomHandler(Runnable reset)
	{
		this.onResetCustom = reset;
	}

	/** Tells the panel whether anything is pinned or barred, so the reset control can hide itself. */
	public void setRouteCustomised(boolean customised)
	{
		this.routeCustomised = customised;
	}

	/** Wires bar / un-bar / restore-all to the plugin, which owns and persists the barred set. */
	public void setBarHandlers(Consumer<Integer> barTask, Consumer<Integer> unbarTask,
		Runnable clearBarred)
	{
		this.onBarTask = barTask;
		this.onUnbarTask = unbarTask;
		this.onClearBarred = clearBarred;
	}

	/** Expands or collapses the Route's barred section (used by the preview renderer). */
	/** Expands or collapses a boss page's "Completed" section (used by the preview renderer). */
	public void setCompletedCollapsed(boolean collapsed)
	{
		this.completedCollapsed = collapsed;
	}

	public void setBarredCollapsed(boolean collapsed)
	{
		this.barredCollapsed = collapsed;
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
			retintDiscord();
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

	public void rebuild()
	{
		boolean ready = model.state() == SidePanelViewModel.State.READY;
		boolean detailOpen = route.detailOpen();
		boolean caMode = route.mode() == PanelMode.RECOMMENDED;
		boolean routeMode = ready && !detailOpen && route.mode() == PanelMode.ROUTE;
		boolean searchable = ready && !detailOpen && (caMode || route.mode() == PanelMode.BOSSES);
		searchField.setVisible(searchable);
		orderLabel.setVisible(searchable);
		sortBox.setVisible(searchable);
		orderRow.setVisible(searchable || routeMode); // Order controls (CAs/Bosses) and/or the refresh button
		shuffleButton.setVisible(caMode || routeMode); // CAs reshuffle + Route re-solve; not Bosses
		shuffleButton.setToolTipText(routeMode ? "Suggest a different route" : "Shuffle — show a different set of CAs");
		controlBar.setVisible(searchable || routeMode);
		content.removeAll();

		if (route.ca() != null)
		{
			caDetailPage.renderCaDetail(route.ca()); // the drill-in overrides whatever mode is active
		}
		else if (!ready)
		{
			content.add(messageLabel(model.message()));
		}
		else
		{
			switch (route.mode())
			{
				case RECOMMENDED:
					recommendedPage.buildRecommended();
					break;
				case BOSSES:
					if (route.boss() != null)
					{
						bossDetailPage.buildBossDetail(route.boss());
					}
					else
					{
						bossesPage.buildBosses();
					}
					break;
				case ROUTE:
					if (route.unlock() != null)
					{
						unlockDetailPage.renderUnlockDetail(route.unlock());
					}
					else
					{
						routePage.buildRoute();
					}
					break;
				default:
					break;
			}
		}

		content.revalidate();
		content.repaint();
	}

	// ---- Search & order ----------------------------------------------------------------------------

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
			if (route.mode() == PanelMode.ROUTE)
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
	public String searchText()
	{
		String t = searchField.getText();
		return t == null || SEARCH_HINT.equals(t) ? "" : t.trim().toLowerCase(Locale.ROOT);
	}

	// ---- Page context ------------------------------------------------------------------------------

	/** The boss-list ordering; {@link BossesPage#bossComparator} is the real thing, this is the tests' seam. */
	public static Comparator<SidePanelViewModel.BossRow> bossComparator(Sort sort, int tripOverheadMinutes)
	{
		return BossesPage.bossComparator(sort, tripOverheadMinutes);
	}

	public static Comparator<SidePanelViewModel.BossRow> bossComparator(Sort sort, int tripOverheadMinutes,
		double timeWeight)
	{
		return BossesPage.bossComparator(sort, tripOverheadMinutes, timeWeight);
	}

	/** A task's jittered shuffle key; {@link RecommendedPage#jitteredKey} is the real thing, this is the tests' seam. */
	public static double jitteredKey(int rank, int id, long seed)
	{
		return RecommendedPage.jitteredKey(rank, id, seed);
	}

	/** The scrolled column the pages render into. */
	public JPanel content()
	{
		return content;
	}

	/** The model currently on screen. */
	public SidePanelViewModel model()
	{
		return model;
	}

	public PanelRoute route()
	{
		return route;
	}

	public void setRoute(PanelRoute route)
	{
		this.route = route;
	}

	public Sort sort()
	{
		return sort;
	}

	public long shuffleSeed()
	{
		return shuffleSeed;
	}

	public int tripOverheadMinutes()
	{
		return tripOverheadMinutes;
	}

	public double bossTimeWeight()
	{
		return bossTimeWeight;
	}

	public boolean routeCustomised()
	{
		return routeCustomised;
	}

	// The collapse/expand flags stay owned here (they must survive a page being torn down and re-rendered
	// every rebuild); the pages read and write them through these.

	public boolean unlocksCollapsed()
	{
		return unlocksCollapsed;
	}

	public void setUnlocksCollapsed(boolean collapsed)
	{
		this.unlocksCollapsed = collapsed;
	}

	public boolean trainingsCollapsed()
	{
		return trainingsCollapsed;
	}

	public void setTrainingsCollapsed(boolean collapsed)
	{
		this.trainingsCollapsed = collapsed;
	}

	public boolean barredCollapsed()
	{
		return barredCollapsed;
	}

	public boolean completedCollapsed()
	{
		return completedCollapsed;
	}

	public boolean doableCollapsed()
	{
		return doableCollapsed;
	}

	public void setDoableCollapsed(boolean collapsed)
	{
		this.doableCollapsed = collapsed;
	}

	public boolean trainFirstCollapsed()
	{
		return trainFirstCollapsed;
	}

	public void setTrainFirstCollapsed(boolean collapsed)
	{
		this.trainFirstCollapsed = collapsed;
	}

	public boolean lockedCollapsed()
	{
		return lockedCollapsed;
	}

	public void setLockedCollapsed(boolean collapsed)
	{
		this.lockedCollapsed = collapsed;
	}

	public boolean howToExpanded()
	{
		return howToExpanded;
	}

	public void setHowToExpanded(boolean expanded)
	{
		this.howToExpanded = expanded;
	}

	public boolean reqsExpanded()
	{
		return reqsExpanded;
	}

	public void setReqsExpanded(boolean expanded)
	{
		this.reqsExpanded = expanded;
	}

	public boolean unlockPrereqsExpanded()
	{
		return unlockPrereqsExpanded;
	}

	public void setUnlockPrereqsExpanded(boolean expanded)
	{
		this.unlockPrereqsExpanded = expanded;
	}

	public boolean unlockStatsExpanded()
	{
		return unlockStatsExpanded;
	}

	public void setUnlockStatsExpanded(boolean expanded)
	{
		this.unlockStatsExpanded = expanded;
	}

	/** The boss groups currently open on the quest page (mutable — the pages toggle membership). */
	public Set<String> expandedUnlockBosses()
	{
		return expandedUnlockBosses;
	}

	// The click handlers the plugin wired in; null when a control should not exist.

	public Consumer<Integer> onBarTask()
	{
		return onBarTask;
	}

	public Consumer<Integer> onUnbarTask()
	{
		return onUnbarTask;
	}

	public Consumer<Integer> onAddToRoute()
	{
		return onAddToRoute;
	}

	public Consumer<Integer> onRemoveFromRoute()
	{
		return onRemoveFromRoute;
	}

	public Runnable onClearBarred()
	{
		return onClearBarred;
	}

	public Runnable onResetCustom()
	{
		return onResetCustom;
	}

	/** A bold gold section header that toggles a collapsed section when clicked (▸ collapsed / ▾ open). */
	public JPanel collapseHeader(String text, boolean collapsed, Runnable onToggle)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		JLabel label = new JLabel((collapsed ? "▸ " : "▾ ") + CardKit.escape(text));
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
		return CardKit.fullWidth(row);
	}

	/** True when the boss directory has a page for this monster (so a "View boss" jump can't dead-end). */
	public boolean bossExists(String monster)
	{
		for (SidePanelViewModel.BossRow b : model.bosses())
		{
			if (monster.equals(b.monster))
			{
				return true;
			}
		}
		return false;
	}

	public JButton backButton(String text, Runnable action)
	{
		JButton back = new JButton(text);
		back.setMargin(new Insets(1, 6, 1, 6));
		back.setFocusPainted(false);
		back.setFont(FontManager.getRunescapeSmallFont());
		back.addActionListener(e -> action.run());
		return back;
	}

	// ---- Shared helpers ----------------------------------------------------------------------------

	public JLabel sectionHeader(String text)
	{
		JLabel label = new JLabel(CardKit.escape(text));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		label.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
		return CardKit.fullWidth(label);
	}

	public JPanel linkRow(String wikiUrl, String guideUrl, boolean curatedVideo)
	{
		return linkRow(wikiUrl, guideUrl, curatedVideo, "");
	}

	public JPanel linkRow(String wikiUrl, String guideUrl, boolean curatedVideo, String feedbackUrl)
	{
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
		row.setOpaque(false);
		if (wikiUrl != null && !wikiUrl.isEmpty())
		{
			row.add(CardKit.linkButton("Wiki", wikiUrl));
		}
		if (guideUrl != null && !guideUrl.isEmpty())
		{
			row.add(CardKit.linkButton(curatedVideo ? "Watch guide" : "Search guide", guideUrl));
		}
		if (feedbackUrl != null && !feedbackUrl.isEmpty())
		{
			row.add(CardKit.linkButton("Suggest fix", feedbackUrl));
		}
		return row;
	}

	public JLabel messageLabel(String text)
	{
		JLabel label = CardKit.wrappedHtmlLabel(CardKit.escape(text), CARD_TEXT_WIDTH);
		label.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		// The border sits inside the pinned size, so grow the pin or it eats the text's height.
		Dimension pinned = label.getPreferredSize();
		Dimension withBorder = new Dimension(pinned.width, pinned.height + 16);
		label.setPreferredSize(withBorder);
		label.setMaximumSize(withBorder);
		return CardKit.fullWidth(label);
	}
}
