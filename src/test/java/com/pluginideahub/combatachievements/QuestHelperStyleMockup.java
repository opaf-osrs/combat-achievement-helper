package com.pluginideahub.combatachievements;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * THROWAWAY visual mockup of a Quest-Helper-style layout for the Combat Achievements panel
 * (mode picker -&gt; list -&gt; boss detail -&gt; CA detail). Not wired to the real engine — hand-fed
 * sample data so we can eyeball the structure/theme. Colours match RuneLite/Quest-Helper exactly:
 * #282828 panel, #1E1E1E rows, BRAND_ORANGE accent w/ black text, OSRS green/red, gold in-progress.
 * Renders headless PNGs to build/ui-preview/mock-*.png. Run: ./gradlew mockupUi
 */
public final class QuestHelperStyleMockup
{
	private static final int W = 225;
	private static final Color BG = ColorScheme.DARK_GRAY_COLOR;        // #282828
	private static final Color ROW = ColorScheme.DARKER_GRAY_COLOR;     // #1E1E1E
	private static final Color ORANGE = ColorScheme.BRAND_ORANGE;       // #DC8A00
	private static final Color GOLD = new Color(0xC8, 0xA0, 0x32);
	private static final Color MET = new Color(0x4C, 0xD1, 0x37);       // OSRS/QH green
	private static final Color UNMET = new Color(0xE8, 0x4A, 0x4A);     // OSRS/QH red
	private static final Color SUB = new Color(0x9A, 0x9A, 0x9A);
	private static final Color BODY = new Color(0xD0, 0xD0, 0xD0);
	private static final Color INACTIVE = new Color(0xBD, 0xBD, 0xBD);
	private static final Color INPROGRESS = new Color(240, 207, 123);   // exact QH in-progress
	private static final Color POINTS = MET;

	public static void main(String[] args) throws Exception
	{
		System.setProperty("java.awt.headless", "true");
		File out = new File("build/ui-preview");
		out.mkdirs();
		SwingUtilities.invokeAndWait(() ->
		{
			render(recommendedView(), new File(out, "mock-1-recommended.png"));
			render(bossesView(), new File(out, "mock-2-bosses.png"));
			render(detailView(), new File(out, "mock-3-boss-detail.png"));
			render(caDetailView(), new File(out, "mock-4-ca-detail.png"));
		});
		System.out.println("Wrote QH-style mockups to " + out.getAbsolutePath());
	}

	// ---- shared header: title + mode toggle + search + order ----
	private static void addHeader(JPanel col, boolean recommended)
	{
		JLabel title = new JLabel("Combat Achievements");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(GOLD);
		title.setBorder(new EmptyBorder(8, 8, 6, 8));
		add(col, title, 26);

		JPanel toggle = new JPanel(new GridLayout(1, 2, 4, 0));
		toggle.setBackground(BG);
		toggle.setBorder(new EmptyBorder(0, 8, 6, 8));
		toggle.add(modeCell("Recommended", recommended));
		toggle.add(modeCell("Bosses", !recommended));
		add(col, toggle, 30);

		JPanel search = new JPanel(new BorderLayout());
		search.setBackground(ROW);
		search.setBorder(new EmptyBorder(6, 8, 6, 8));
		JLabel s = new JLabel("Search");
		s.setFont(FontManager.getRunescapeSmallFont());
		s.setForeground(SUB);
		search.add(s, BorderLayout.CENTER);
		add(col, pad(search, 8), 34);

		JLabel order = new JLabel("Order:  " + (recommended ? "Recommended" : "Most points") + "  v");
		order.setFont(FontManager.getRunescapeSmallFont());
		order.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		order.setHorizontalAlignment(SwingConstants.RIGHT);
		order.setBorder(new EmptyBorder(2, 8, 6, 10));
		add(col, order, 20);
	}

	private static JPanel modeCell(String text, boolean selected)
	{
		JPanel cell = new JPanel(new GridBagLayout());
		cell.setBackground(selected ? ORANGE : ROW);
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(selected ? Color.BLACK : ColorScheme.LIGHT_GRAY_COLOR);
		cell.add(l);
		return cell;
	}

	private static void addSectionLabel(JPanel col, String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(SUB);
		l.setBorder(new EmptyBorder(4, 10, 2, 8));
		add(col, l, 16);
	}

	// ---- list row: name (status colour) + subtitle + right value ----
	private static void addListRow(JPanel col, String name, Color nameColor, String sub, String right, Color rightColor)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ROW);
		row.setBorder(new EmptyBorder(5, 8, 5, 8));

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setBackground(ROW);
		JLabel nm = new JLabel(name);
		nm.setFont(FontManager.getRunescapeFont());
		nm.setForeground(nameColor);
		nm.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.add(nm);
		if (sub != null)
		{
			JLabel sb = new JLabel(sub);
			sb.setFont(FontManager.getRunescapeSmallFont());
			sb.setForeground(SUB);
			sb.setAlignmentX(Component.LEFT_ALIGNMENT);
			text.add(sb);
		}
		row.add(text, BorderLayout.CENTER);

		if (right != null)
		{
			JLabel r = new JLabel(right);
			r.setFont(FontManager.getRunescapeFont());
			r.setForeground(rightColor);
			row.add(r, BorderLayout.LINE_END);
		}
		add(col, pad(row, 6), sub == null ? 26 : 34);
	}

	// ---- views ----
	private static JComponent recommendedView()
	{
		JPanel col = column();
		addHeader(col, true);
		addSectionLabel(col, "Best next · doable now · most points");
		addListRow(col, "Wintertodt Novice", Color.WHITE, "Wintertodt · easy · 0/8 done", "+1", POINTS);
		addListRow(col, "A Smashing Time", Color.WHITE, "Gargoyle · medium · doable now", "+2", POINTS);
		addListRow(col, "Noxious Foe", Color.WHITE, "Aberrant Spectre · easy · doable now", "+1", POINTS);
		addListRow(col, "Scurrius Champion", Color.WHITE, "Scurrius · medium · doable now", "+2", POINTS);
		addListRow(col, "Barrows Novice", Color.WHITE, "Barrows · easy · doable now", "+1", POINTS);
		addListRow(col, "Bryophyta Champion", Color.WHITE, "Bryophyta · medium · doable now", "+2", POINTS);
		addListRow(col, "Big, Black and Fiery", Color.WHITE, "Black Dragon · easy · doable now", "+1", POINTS);
		addListRow(col, "Kill It with Fire", INPROGRESS, "Tempoross · medium · in progress", "+2", POINTS);
		return col;
	}

	private static JComponent bossesView()
	{
		JPanel col = column();
		addHeader(col, false);
		addSectionLabel(col, "Pick a boss · points + tasks inside");
		addListRow(col, "Wintertodt", Color.WHITE, "easy minigame · doable now", "8 pts", GOLD);
		addListRow(col, "Scurrius", Color.WHITE, "difficulty 2 · doable now", "5 pts", GOLD);
		addListRow(col, "Barrows", Color.WHITE, "difficulty 2 · doable now", "6 pts", GOLD);
		addListRow(col, "Vorkath", Color.WHITE, "difficulty 4 · doable now", "32 pts", GOLD);
		addListRow(col, "Zulrah", Color.WHITE, "difficulty 4 · doable now", "28 pts", GOLD);
		addListRow(col, "Tombs of Amascut", Color.WHITE, "difficulty 6 · doable now", "40 pts", GOLD);
		addListRow(col, "Scorpia", MET, "difficulty 3 · all done", "✓", MET);
		addListRow(col, "TzKal-Zuk", new Color(0x7D, 0x7D, 0x7D), "difficulty 10 · needs Inferno access", "locked", new Color(0x7D, 0x7D, 0x7D));
		return col;
	}

	private static JComponent detailView()
	{
		JPanel col = column();
		addTitleBar(col, "Vorkath");

		addSectionLabel(col, "Requirements");
		addReqRow(col, "Dragon Slayer II", true);
		addReqRow(col, "Combat level 100+", true);
		addMuted(col, "Recommended: 94 Magic / 90 Ranged", 16);

		addOrangeHeader(col, "Recommended tasks", "32 pts");
		addTaskRow(col, "Vorkath Veteran — kill 50", ORANGE, "+4", "easiest · ~84 min");
		addTaskRow(col, "Vorkath Master — kill 100", INACTIVE, "+5", "difficulty 4");
		addTaskRow(col, "Just the Tip — perfect kill", INACTIVE, "+4", "difficulty 5.8 · no damage");
		addTaskRow(col, "Quick Kill — under 30s", INACTIVE, "+5", "difficulty 5.2 · speed");
		return col;
	}

	// ---- what a single CA looks like when clicked ----
	private static JComponent caDetailView()
	{
		JPanel col = column();
		addTitleBar(col, "Just the Tip");
		addMuted(col, "Vorkath · Elite · 4 pts · Perfection", 15);
		JLabel desc = new JLabel("<html><body style='width:208px'>Kill Vorkath without taking any avoidable damage.</body></html>");
		desc.setFont(FontManager.getRunescapeFont());
		desc.setForeground(BODY);
		desc.setBorder(new EmptyBorder(2, 12, 4, 10));
		add(col, desc, 26);

		addSectionLabel(col, "Requirements");
		addReqRow(col, "Dragon Slayer II", true);
		addReqRow(col, "Combat level 100+", true);
		addReqRow(col, "92 Magic — you have 88", false);

		addSectionLabel(col, "Difficulty");
		addKeyVal(col, "Rating", "5.8 / 10", ORANGE);
		addMuted(col, "Vorkath (boss 4) + no damage (+1.8)", 15);

		addSectionLabel(col, "Effort");
		addKeyVal(col, "Estimate", "~12 min  ·  20 pts/hr", POINTS);

		addSectionLabel(col, "Recommended setup");
		addMuted(col, "Tank gear · Protect from Magic · Crumble Undead", 16);
		addSectionLabel(col, "Strategy");
		JLabel strat = new JLabel("<html><body style='width:208px'>Walk through the acid trail to dodge the fireball; re-pray each phase.</body></html>");
		strat.setFont(FontManager.getRunescapeSmallFont());
		strat.setForeground(INACTIVE);
		strat.setBorder(new EmptyBorder(1, 12, 4, 10));
		add(col, strat, 28);

		JPanel links = new JPanel(new GridLayout(1, 2, 6, 0));
		links.setBackground(BG);
		links.setBorder(new EmptyBorder(4, 10, 4, 10));
		links.add(pill("Wiki"));
		links.add(pill("Video guide"));
		add(col, links, 28);
		return col;
	}

	// ---- shared detail widgets ----
	private static void addTitleBar(JPanel col, String name)
	{
		JPanel bar = new JPanel(new BorderLayout());
		bar.setBackground(ROW);
		bar.setBorder(new EmptyBorder(7, 8, 7, 8));
		JLabel nm = new JLabel(name);
		nm.setFont(FontManager.getRunescapeBoldFont());
		nm.setForeground(Color.WHITE);
		bar.add(nm, BorderLayout.CENTER);
		JLabel x = new JLabel("✕");
		x.setForeground(SUB);
		bar.add(x, BorderLayout.LINE_END);
		add(col, pad(bar, 6), 30);
	}

	private static void addOrangeHeader(JPanel col, String text, String right)
	{
		JPanel hdr = new JPanel(new BorderLayout());
		hdr.setBackground(ORANGE);
		hdr.setBorder(new EmptyBorder(6, 8, 6, 8));
		JLabel h = new JLabel(text);
		h.setFont(FontManager.getRunescapeBoldFont());
		h.setForeground(Color.BLACK);
		hdr.add(h, BorderLayout.CENTER);
		JLabel r = new JLabel(right);
		r.setFont(FontManager.getRunescapeFont());
		r.setForeground(Color.BLACK);
		hdr.add(r, BorderLayout.LINE_END);
		add(col, pad(hdr, 6), 28);
	}

	private static void addReqRow(JPanel col, String text, boolean met)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(BG);
		row.setBorder(new EmptyBorder(2, 12, 2, 8));
		JLabel l = new JLabel((met ? "✓  " : "✗  ") + text);
		l.setFont(FontManager.getRunescapeFont());
		l.setForeground(met ? MET : UNMET);
		row.add(l, BorderLayout.CENTER);
		add(col, row, 18);
	}

	private static void addKeyVal(JPanel col, String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(BG);
		row.setBorder(new EmptyBorder(2, 12, 2, 10));
		JLabel l = new JLabel(label);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(SUB);
		JLabel v = new JLabel(value);
		v.setFont(FontManager.getRunescapeFont());
		v.setForeground(valueColor);
		row.add(l, BorderLayout.WEST);
		row.add(v, BorderLayout.EAST);
		add(col, row, 18);
	}

	private static void addMuted(JPanel col, String text, int h)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(SUB);
		l.setBorder(new EmptyBorder(1, 12, 4, 8));
		add(col, l, h);
	}

	private static JPanel pill(String text)
	{
		JPanel p = new JPanel(new GridBagLayout());
		p.setBackground(ROW);
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(GOLD);
		p.add(l);
		return p;
	}

	private static void addTaskRow(JPanel col, String name, Color nameColor, String right, String sub)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ROW);
		row.setBorder(new MatteBorder(1, 0, 0, 0, BG.brighter()));
		JPanel inner = new JPanel(new BorderLayout(4, 0));
		inner.setBackground(ROW);
		inner.setBorder(new EmptyBorder(5, 8, 5, 8));
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setBackground(ROW);
		JLabel nm = new JLabel(name);
		nm.setFont(FontManager.getRunescapeFont());
		nm.setForeground(nameColor);
		nm.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.add(nm);
		JLabel sb = new JLabel(sub);
		sb.setFont(FontManager.getRunescapeSmallFont());
		sb.setForeground(SUB);
		sb.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.add(sb);
		inner.add(text, BorderLayout.CENTER);
		JLabel r = new JLabel(right);
		r.setFont(FontManager.getRunescapeFont());
		r.setForeground(POINTS);
		inner.add(r, BorderLayout.LINE_END);
		row.add(inner, BorderLayout.CENTER);
		add(col, pad(row, 6), 34);
	}

	// ---- plumbing ----
	private static JPanel column()
	{
		JPanel col = new JPanel();
		col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
		col.setBackground(BG);
		col.setBorder(new EmptyBorder(0, 0, 8, 0));
		return col;
	}

	private static JPanel pad(JComponent c, int side)
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(BG);
		p.setBorder(new EmptyBorder(0, side, 0, side));
		p.add(c, BorderLayout.CENTER);
		return p;
	}

	private static void add(JPanel col, JComponent c, int h)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.setMaximumSize(new Dimension(W, h));
		c.setPreferredSize(new Dimension(W, h));
		col.add(c);
	}

	private static void render(JComponent view, File file)
	{
		try
		{
			view.setSize(W, 100);
			int h = Math.max(80, view.getPreferredSize().height);
			view.setSize(W, h);
			layout(view);
			BufferedImage img = new BufferedImage(W, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = img.createGraphics();
			g.setColor(BG);
			g.fillRect(0, 0, W, h);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			view.printAll(g);
			g.dispose();
			ImageIO.write(img, "png", file);
			System.out.println("  " + file.getName() + "  " + W + "x" + h);
		}
		catch (Exception ex)
		{
			System.out.println("  FAILED " + file.getName() + ": " + ex);
		}
	}

	private static void layout(Component c)
	{
		if (c instanceof Container)
		{
			Container k = (Container) c;
			k.doLayout();
			for (Component child : k.getComponents())
			{
				layout(child);
			}
		}
	}

	private QuestHelperStyleMockup()
	{
	}
}
