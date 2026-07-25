package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import com.pluginideahub.combatachievements.ui.PanelRoute;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Renders the quest-unlock drill-in: the quest's headline card, then the CAs it opens, by boss. */
public class UnlockDetailPage
{
	private final CombatAchievementsPanel panel;
	/** The Route page owns the unlock card html this page's header shares. */
	private final RoutePage routePage;
	private final JPanel content;

	public UnlockDetailPage(CombatAchievementsPanel panel, RoutePage routePage)
	{
		this.panel = panel;
		this.routePage = routePage;
		this.content = panel.content();
	}

	/** The quest-unlock drill-in: the quest's headline, then the CAs it opens, each clickable. */
	public void renderUnlockDetail(SidePanelViewModel.UnlockView u)
	{
		content.add(panel.backButton("← Back", () -> {
			panel.setRoute(panel.route().clearUnlock());
			panel.rebuild();
		}));
		content.add(CardKit.spacer());

		// The same card the Route shows, minus the click: an html JLabel added bare to the BoxLayout
		// clips long lines at the panel edge (its CSS body width is unreliable), while the card's
		// BorderLayout demonstrably wraps this exact content in the Route list.
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.HEADER_GOLD),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		header.add(CardKit.wrappedHtmlLabel(routePage.unlockCardHtml(u), CombatAchievementsPanel.CARD_TEXT_WIDTH),
			BorderLayout.CENTER);
		content.add(CardKit.fullWidth(header));
		content.add(CardKit.spacer());

		if (!u.questWikiUrl.isEmpty())
		{
			JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
			links.setOpaque(false);
			links.add(CardKit.linkButton("Wiki", u.questWikiUrl));
			content.add(CardKit.fullWidth(links));
			content.add(CardKit.spacer());
		}

		// The prerequisite chain as its own foldable section, one quest per line in chain order —
		// prose on the card turned into a wall for the long questlines.
		if (!u.prerequisiteList.isEmpty())
		{
			content.add(panel.collapseHeader("Quests first · " + u.prerequisiteList.size(),
				!panel.unlockPrereqsExpanded(),
				() -> { panel.setUnlockPrereqsExpanded(!panel.unlockPrereqsExpanded()); panel.rebuild(); }));
			if (panel.unlockPrereqsExpanded())
			{
				content.add(CardKit.spacer());
				for (String quest : u.prerequisiteList)
				{
					JLabel row = new JLabel(CardKit.escape(quest));
					row.setFont(FontManager.getRunescapeSmallFont());
					row.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					row.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 0));
					content.add(CardKit.fullWidth(row));
				}
			}
			content.add(CardKit.spacer());
		}

		// The chain's unmet stats, same shape as the quest list: a count on the card, the detail here.
		if (!u.unmetSkillList.isEmpty())
		{
			content.add(panel.collapseHeader("Stats first · " + u.unmetSkillList.size(),
				!panel.unlockStatsExpanded(),
				() -> { panel.setUnlockStatsExpanded(!panel.unlockStatsExpanded()); panel.rebuild(); }));
			if (panel.unlockStatsExpanded())
			{
				content.add(CardKit.spacer());
				for (String stat : u.unmetSkillList)
				{
					JLabel row = new JLabel(CardKit.escape(stat.replace("→", " → ")));
					row.setFont(FontManager.getRunescapeSmallFont());
					row.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					row.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 0));
					content.add(CardKit.fullWidth(row));
				}
			}
			content.add(CardKit.spacer());
		}

		// Grouped by boss, same as the Route: each boss header is a way in to its page, with the
		// prize at that boss beside it. Group order follows the quickest-first CA order.
		content.add(panel.sectionHeader("Unlocks"));
		content.add(CardKit.spacer());
		LinkedHashMap<String, List<SidePanelViewModel.CaDetail>> byBoss = new LinkedHashMap<>();
		for (SidePanelViewModel.CaDetail c : u.unlockedCas)
		{
			byBoss.computeIfAbsent(c.monster.isEmpty() ? "Other" : c.monster,
				k -> new ArrayList<>()).add(c);
		}
		for (Map.Entry<String, List<SidePanelViewModel.CaDetail>> e : byBoss.entrySet())
		{
			int pts = 0;
			for (SidePanelViewModel.CaDetail c : e.getValue())
			{
				pts += c.points;
			}
			boolean expanded = panel.expandedUnlockBosses().contains(e.getKey());
			content.add(unlockBossHeader(e.getKey(), e.getValue().size(), pts, expanded));
			content.add(CardKit.spacer());
			if (expanded)
			{
				for (SidePanelViewModel.CaDetail c : e.getValue())
				{
					content.add(unlockCaCard(c));
					content.add(CardKit.spacer());
				}
			}
		}
	}

	/**
	 * A boss group header on the quest page: an arrow that shows/hides the group's CAs (all groups
	 * start closed), the boss name — clickable through to the boss page when it has one — and the
	 * group's share of the prize.
	 */
	private JPanel unlockBossHeader(String boss, int count, int points, boolean expanded)
	{
		JLabel arrow = new JLabel(expanded ? "▾ " : "▸ ");
		arrow.setFont(FontManager.getRunescapeBoldFont());
		arrow.setForeground(CombatAchievementsTheme.HEADER_GOLD);

		JLabel label = new JLabel(CardKit.escape(boss));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);

		JLabel meta = new JLabel(count + (count == 1 ? " CA · " : " CAs · ") + points + " pts");
		meta.setFont(FontManager.getRunescapeSmallFont());
		meta.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		// The prize goes UNDER the name, not beside it: three "Tombs of Am…" rows squeezed against
		// an inline count were indistinguishable, and the boss name is the part that must survive.
		JPanel textCol = new JPanel();
		textCol.setOpaque(false);
		textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		meta.setAlignmentX(Component.LEFT_ALIGNMENT);
		textCol.add(label);
		textCol.add(meta);

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
		row.add(arrow, BorderLayout.WEST);
		row.add(textCol, BorderLayout.CENTER);
		// Cap the row's preferred width or a long name widens the whole content column past the panel
		// (the widest-child trap; same cap as the route cards). The name still ellipsizes past this.
		row.setPreferredSize(new Dimension(CombatAchievementsPanel.ROUTE_TEXT_WIDTH, row.getPreferredSize().height));

		Runnable toggle = () -> {
			if (!panel.expandedUnlockBosses().remove(boss))
			{
				panel.expandedUnlockBosses().add(boss);
			}
			panel.rebuild();
		};
		// The row (and the arrow) toggle the group; the boss NAME jumps to the boss page instead,
		// when there is one. AWT does not bubble mouse events, so each part needs its own handler —
		// the name's hover listener alone would swallow clicks meant for the row.
		CardKit.onClick(arrow, toggle);
		CardKit.onClick(row, toggle);
		if (panel.bossExists(boss))
		{
			Runnable openBoss = () -> {
				panel.setRoute(PanelRoute.of(PanelMode.BOSSES).withBoss(boss));
				panel.buildModeBar();
				panel.rebuild();
			};
			CardKit.addForegroundHover(label, CombatAchievementsTheme.HEADER_GOLD, CombatAchievementsTheme.NAME);
			CardKit.onClick(label, openBoss);
		}
		else
		{
			CardKit.onClick(label, toggle);
		}
		return CardKit.fullWidth(row);
	}

	/** A CA the quest would open, on the quest's own page — grouped under its boss header, so no
	 *  per-card monster or "needs <quest>" line. */
	private JPanel unlockCaCard(SidePanelViewModel.CaDetail c)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.NAME),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder();
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NAME))
			.append("'><b>").append(CardKit.escape(c.name)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(c.points).append(" pts</span>");
		if (c.difficulty > 0)
		{
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(CardKit.difficultyColor(c.difficulty)))
				.append("'>difficulty ").append(c.difficulty).append("</span>");
		}
		card.add(CardKit.wrappedHtmlLabel(sb.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH), BorderLayout.CENTER);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(c));
			panel.rebuild();
		});
		return CardKit.fullWidth(card);
	}
}
