package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.FontManager;

/** Renders the CA-detail drill-in: one achievement's crumb, requirements, difficulty and how-to. */
public class CaDetailPage
{
	/** Wrap width for the CA-detail how-to prose, kept clear of the scrollbar the tall view brings in. */
	private static final int DETAIL_TEXT_WIDTH = 170;

	private final CombatAchievementsPanel panel;
	private final JPanel content;

	public CaDetailPage(CombatAchievementsPanel panel)
	{
		this.panel = panel;
		this.content = panel.content();
	}

	public void renderCaDetail(SidePanelViewModel.CaDetail d)
	{
		content.add(panel.backButton("← Back", () -> {
			panel.setRoute(panel.route().clearCa());
			panel.rebuild();
		}));
		content.add(CardKit.spacer());

		StringBuilder crumb = new StringBuilder();
		if (!d.monster.isEmpty())
		{
			crumb.append(CardKit.escape(d.monster)).append(" · ");
		}
		crumb.append(CardKit.escape(d.tierName)).append(" · ").append(d.points).append(" pts");
		if (!d.type.isEmpty())
		{
			crumb.append(" · ").append(CardKit.escape(d.type));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NAME))
			.append("'><b style='font-size:11px'>").append(CardKit.escape(d.name)).append("</b></span>");
		sb.append("<br><span style='color:" + CardKit.metaHex() + "'>").append(crumb).append("</span>");
		if (!d.description.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
				.append("'>").append(CardKit.escape(d.description)).append("</span>");
		}
		if (!d.doableNow && !d.lockReason.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(CardKit.escape(d.lockReason)).append("</span>");
		}
		content.add(CardKit.fullWidth(CardKit.wrappedHtmlLabel(sb.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH)));
		content.add(CardKit.spacer());

		// Jump to the boss this CA is at, to see everything else you could knock out on the same trip.
		// Only offered when that boss actually has a page in the directory, so it can never be a dead end.
		if (!d.monster.isEmpty() && panel.bossExists(d.monster))
		{
			String label = d.monster.length() > 18 ? d.monster.substring(0, 17) + "…" : d.monster;
			content.add(panel.backButton("View " + label + " →", () -> {
				panel.setRoute(panel.route().toBoss(d.monster));
				panel.buildModeBar();
				panel.rebuild();
			}));
			content.add(CardKit.spacer());
		}

		if (!d.requirements.isEmpty())
		{
			content.add(panel.collapseHeader("Requirements", !panel.reqsExpanded(),
				() -> { panel.setReqsExpanded(!panel.reqsExpanded()); panel.rebuild(); }));
			if (panel.reqsExpanded())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.CaReq r : d.requirements)
				{
					content.add(reqRow(r));
				}
			}
			content.add(CardKit.spacer());
		}

		content.add(panel.sectionHeader("Difficulty"));
		double rating = displayedDifficulty(d);
		StringBuilder diff = new StringBuilder();
		diff.append("<span style='color:").append(CombatAchievementsTheme.hex(CardKit.difficultyColor(d.difficulty)))
			.append("'>").append(String.format(Locale.ROOT, "%.1f", rating)).append(" / 10</span>");
		if (d.bossDifficulty > 0)
		{
			diff.append("<br><span style='color:" + CardKit.metaHex() + "'>").append(CardKit.escape(d.monster)).append(" (boss ")
				.append(d.bossDifficulty).append(")");
			if (d.bump != 0 && !d.difficultyReason.isEmpty())
			{
				diff.append(" + ").append(CardKit.escape(d.difficultyReason))
					.append(String.format(Locale.ROOT, " (+%.1f)", d.bump));
			}
			diff.append("</span>");
		}
		content.add(CardKit.fullWidth(CardKit.wrappedHtmlLabel(diff.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH)));
		content.add(CardKit.spacer());


		// Curated how-to (stats/setup/items/strategy) behind a collapsible arrow so the detail stays lean;
		// the config option seeds whether it starts expanded.
		boolean hasHowTo = !d.stats.isEmpty() || !d.setup.isEmpty() || !d.items.isEmpty()
			|| !d.strategy.isEmpty();
		if (hasHowTo)
		{
			content.add(panel.collapseHeader("How to do it", !panel.howToExpanded(),
				() -> { panel.setHowToExpanded(!panel.howToExpanded()); panel.rebuild(); }));
			if (panel.howToExpanded())
			{
				content.add(CardKit.spacer());
				addDetailText("Recommended stats", d.stats);
				addDetailText("Recommended setup", d.setup);
				addDetailText("Items", d.items);
				addDetailText("Strategy", d.strategy);
			}
			content.add(CardKit.spacer());
		}

		// "Suggest fix" only belongs on a task's own page, so it is appended here rather than baked into
		// the shared link row the Bosses and Recommended cards use.
		JPanel links = panel.linkRow(d.wikiUrl, d.guideUrl, d.curatedVideo);
		links.add(CardKit.linkButton("Suggest fix", CombatAchievementsPanel.SUGGEST_FIX_URL));
		content.add(CardKit.fullWidth(links));
	}

	void addDetailText(String header, String text)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		content.add(panel.sectionHeader(header));
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
		content.add(CardKit.spacer());
	}

	private JLabel reqRow(SidePanelViewModel.CaReq r)
	{
		Color colour = r.met ? CombatAchievementsTheme.POSITIVE : CombatAchievementsTheme.NEGATIVE;
		String mark = r.met ? "&#10003;" : "&#10007;"; // check / cross
		StringBuilder sb = new StringBuilder("<span style='color:")
			.append(CombatAchievementsTheme.hex(colour)).append("'>").append(mark).append(" ")
			.append(CardKit.escape(r.label)).append("</span>");
		if (!r.note.isEmpty())
		{
			sb.append(" <span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>— ").append(CardKit.escape(r.note)).append("</span>");
		}
		return CardKit.fullWidth(CardKit.wrappedHtmlLabel(sb.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH));
	}

	/** The Difficulty rating the panel shows: the boss rating plus this task's bump, else the raw value. */
	private static double displayedDifficulty(SidePanelViewModel.CaDetail d)
	{
		return d.bossDifficulty > 0 ? d.bossDifficulty + d.bump : d.difficulty;
	}
}
