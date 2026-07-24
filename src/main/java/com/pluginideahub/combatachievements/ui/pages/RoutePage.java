package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Renders the Route mode: unlock and training suggestions, then the boss-grouped path to the next tier. */
public class RoutePage
{
	private final CombatAchievementsPanel panel;
	private final JPanel content;

	public RoutePage(CombatAchievementsPanel panel)
	{
		this.panel = panel;
		this.content = panel.content();
	}

	/** The route to the next tier: unlock shortcuts first (a quest can open points faster than grinding),
	 *  then the CA steps. The unlock section is collapsible. */
	public void buildRoute()
	{
		SidePanelViewModel.PathView path = panel.model().path();
		List<SidePanelViewModel.UnlockView> unlocks = panel.model().unlocks();
		boolean haveUnlocks = unlocks != null && !unlocks.isEmpty();

		if (path == null && !haveUnlocks)
		{
			content.add(panel.messageLabel("Log in to see your route."));
			return;
		}

		// Unlocks on top: doing a quest can open a chunk of points faster than grinding CAs. Hideable.
		if (haveUnlocks)
		{
			content.add(panel.collapseHeader("Unlock next", panel.unlocksCollapsed(),
				() -> { panel.setUnlocksCollapsed(!panel.unlocksCollapsed()); panel.rebuild(); }));
			if (!panel.unlocksCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.UnlockView u : unlocks)
				{
					content.add(unlockCard(u));
					content.add(CardKit.spacer());
				}
			}
			content.add(CardKit.spacer());
		}

		// "Train next": only present when the account is actually held back by levels, so it quietly
		// disappears once you can attempt things — no empty section for an established player.
		List<SidePanelViewModel.TrainingView> trainings = panel.model().trainings();
		if (trainings != null && !trainings.isEmpty())
		{
			content.add(panel.collapseHeader("Train next", panel.trainingsCollapsed(),
				() -> { panel.setTrainingsCollapsed(!panel.trainingsCollapsed()); panel.rebuild(); }));
			if (!panel.trainingsCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.TrainingView t : trainings)
				{
					content.add(trainingCard(t));
					content.add(CardKit.spacer());
				}
			}
			content.add(CardKit.spacer());
		}

		if (path != null)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Goal: <b>").append(CardKit.escape(path.targetTierName)).append("</b>");
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
			content.add(CardKit.fullWidth(CardKit.wrappedHtmlLabel(sb.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH)));
			content.add(CardKit.spacer());

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

			// What the whole visible list is worth, so the total is readable without adding the cards up.
			if (!route.isEmpty())
			{
				StringBuilder tot = new StringBuilder();
				tot.append("<span style='color:" + CardKit.metaHex() + "'>").append(route.size())
					.append(route.size() == 1 ? " CA · " : " CAs · ")
					.append("</span><span style='color:")
					.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
					.append("'>").append(path.shownPoints()).append(" pts</span>");
				content.add(CardKit.fullWidth(CardKit.wrappedHtmlLabel(tot.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH)));
				content.add(CardKit.spacer());
			}
			// Sits with the route it affects rather than in the control bar, and only exists once something
			// is actually pinned or barred.
			if (panel.routeCustomised() && panel.onResetCustom() != null)
			{
				content.add(panel.backButton("Reset custom CAs", () -> {
					if (panel.onResetCustom() != null)
					{
						panel.onResetCustom().run();
					}
				}));
				content.add(CardKit.spacer());
			}
			renderRouteGroups(route);

			// "Not doing these": the CAs barred out of the route, collapsible like Unlock/Train next, each
			// restorable on its own. Absent entirely when nothing is barred, so it costs nothing normally.
			List<SidePanelViewModel.CaDetail> barred = path.barredCas;
			if (barred != null && !barred.isEmpty())
			{
				content.add(CardKit.spacer());
				content.add(panel.collapseHeader("Not doing these", panel.barredCollapsed(),
					() -> { panel.setBarredCollapsed(!panel.barredCollapsed()); panel.rebuild(); }));
				if (!panel.barredCollapsed())
				{
					content.add(CardKit.spacer());
					for (SidePanelViewModel.CaDetail c : barred)
					{
						content.add(barredCard(c));
						content.add(CardKit.spacer());
					}
					if (barred.size() > 1 && panel.onClearBarred() != null)
					{
						content.add(panel.backButton("Restore all " + barred.size(), () -> {
							if (panel.onClearBarred() != null)
							{
								panel.onClearBarred().run();
							}
						}));
					}
				}
				content.add(CardKit.spacer());
			}
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
				content.add(CardKit.spacer());
			}
		}
	}

	/** A small boss header for a route cluster: "General Graardor · 3 tasks". */
	private JPanel routeGroupHeader(String boss, int count)
	{
		// Just the boss name: the cards beneath it already show how many there are, and keeping it short
		// avoids the clipping a longer label hit ("Deranged Archaeologist · 2 tasks" cut off mid-word —
		// Swing sizes an HTML label from its own preferred width, so a body-width wrap doesn't rescue it).
		JLabel label = new JLabel(CardKit.escape(boss));
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(CombatAchievementsTheme.HEADER_GOLD);

		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
		row.add(label, BorderLayout.CENTER);

		// Clicking the boss name opens that boss, so a route group is a way in to everything else there.
		// Formatting is untouched — only a hand cursor and a hover tint mark it as clickable — and it is
		// only wired up when the boss actually has a page, so it can never be a dead click.
		if (panel.bossExists(boss))
		{
			Runnable openBoss = () -> {
				panel.setRoute(panel.route().toBoss(boss));
				panel.buildModeBar();
				panel.rebuild();
			};
			CardKit.addForegroundHover(label, CombatAchievementsTheme.HEADER_GOLD, CombatAchievementsTheme.NAME);
			// The listener has to go on the LABEL as well as the row. AWT does not bubble mouse events the
			// way the DOM does: a component with any listener of its own consumes them, and the hover tint
			// above gives the label one — so a click on the name never reached a row-only handler.
			CardKit.onClick(label, openBoss);
			CardKit.onClick(row, openBoss);
		}
		return CardKit.fullWidth(row);
	}

	private JPanel routeCaCard(SidePanelViewModel.CaDetail c, boolean grouped)
	{
		final int textWidth = panel.onBarTask() != null
			? CombatAchievementsPanel.ROUTE_TEXT_WIDTH - CombatAchievementsPanel.BAR_BUTTON_WIDTH
			: CombatAchievementsPanel.ROUTE_TEXT_WIDTH;
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = c.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.NEGATIVE;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		StringBuilder sb = new StringBuilder("<html><body style='width:" + textWidth + "px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(accent))
			.append("'><b>").append(CardKit.escape(c.name)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(c.points).append(" pts</span>");
		if (c.difficulty > 0)
		{
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(CardKit.difficultyColor(c.difficulty)))
				.append("'>difficulty ").append(c.difficulty).append("</span>");
		}
		// Time estimates are intentionally not shown — the engine still uses them for ordering.
		// When not under a boss header, name the boss so a solo route step still tells you where to go.
		if (!grouped && !c.monster.isEmpty())
		{
			sb.append("<br><span style='color:" + CardKit.metaHex() + "'>").append(CardKit.escape(c.monster)).append("</span>");
		}
		if (!c.doableNow && !c.lockReason.isEmpty())
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(CardKit.escape(c.lockReason)).append("</span>");
		}
		sb.append("</body></html>");
		JLabel routeLabel = new JLabel(sb.toString());
		// The HTML body width only drives WRAPPING; the label still reports a wider preferred size,
		// which pushed the card past the panel and clipped the "-" clean off the right edge.
		Dimension routePref = routeLabel.getPreferredSize();
		Dimension capped = new Dimension(Math.min(routePref.width, textWidth), routePref.height);
		routeLabel.setPreferredSize(capped);
		routeLabel.setMaximumSize(capped);
		card.add(routeLabel, BorderLayout.CENTER);
		// "Not doing that one" — bars the task so the solver closes the gap with the next best instead.
		if (panel.onBarTask() != null)
		{
			card.add(barButton(c), BorderLayout.EAST);
		}
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(c));
			panel.rebuild();
		});
		CardKit.fullWidth(card);
		card.setMaximumSize(new Dimension(CombatAchievementsPanel.ROUTE_CARD_MAX_WIDTH, card.getPreferredSize().height));
		return card;
	}

	/**
	 * A barred CA, listed under "Not doing these". Same card shape as the route, with a "+" that puts this
	 * one back in the running instead of the "-" that took it out.
	 */
	private JPanel barredCard(SidePanelViewModel.CaDetail c)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.LOCKED),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		int textWidth = CombatAchievementsPanel.ROUTE_TEXT_WIDTH - CombatAchievementsPanel.BAR_BUTTON_WIDTH;
		StringBuilder sb = new StringBuilder("<html><body style='width:" + textWidth + "px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
			.append("'><b>").append(CardKit.escape(c.name)).append("</b></span>");
		sb.append("<br><span style='color:" + CardKit.metaHex() + "'>").append(c.points)
			.append(c.points == 1 ? " pt" : " pts");
		if (!c.monster.isEmpty())
		{
			sb.append(" · ").append(CardKit.escape(c.monster));
		}
		sb.append("</span></body></html>");
		JLabel label = new JLabel(sb.toString());
		Dimension pref = label.getPreferredSize();
		Dimension capped = new Dimension(Math.min(pref.width, textWidth), pref.height);
		label.setPreferredSize(capped);
		label.setMaximumSize(capped);
		card.add(label, BorderLayout.CENTER);

		if (panel.onUnbarTask() != null)
		{
			JButton restore = new JButton("+");
			// Matches the route card's "-" in weight and size: they are a pair, and a small "+" against a
			// bold "-" reads as two unrelated controls.
			restore.setFont(FontManager.getRunescapeBoldFont());
			restore.setToolTipText("Put " + c.name + " back in the route");
			restore.setFocusPainted(false);
			restore.setBorderPainted(false);
			restore.setContentAreaFilled(false);
			restore.setOpaque(false);
			restore.setForeground(CombatAchievementsTheme.POSITIVE);
			CardKit.addForegroundHover(restore, CombatAchievementsTheme.POSITIVE,
				CombatAchievementsTheme.NAME);
			restore.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
			restore.setPreferredSize(new Dimension(CombatAchievementsPanel.BAR_BUTTON_WIDTH, 18));
			restore.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			restore.addActionListener(e -> {
				if (panel.onUnbarTask() != null)
				{
					panel.onUnbarTask().accept(c.id);
				}
			});
			card.add(restore, BorderLayout.EAST);
		}
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(c));
			panel.rebuild();
		});
		CardKit.fullWidth(card);
		card.setMaximumSize(new Dimension(CombatAchievementsPanel.ROUTE_CARD_MAX_WIDTH, card.getPreferredSize().height));
		return card;
	}

	/**
	 * The small "−" on a route card. Kept visually quiet so it never competes with the task name, and it
	 * consumes its own click so barring a task cannot also open its detail.
	 */
	private JButton barButton(SidePanelViewModel.CaDetail c)
	{
		// Plain ASCII "-": the RuneScape font does not carry U+2212, which rendered as nothing.
		JButton bar = new JButton("-");
		// Bigger than the card text and tinted toward the theme's negative, so it reads as a remove
		// control at a glance. Muted rather than full red at rest - it is an option, not a warning - and
		// it comes up to the full negative on hover.
		bar.setFont(FontManager.getRunescapeBoldFont());
		bar.setToolTipText("Don't show " + c.name + " in the route");
		bar.setFocusPainted(false);
		bar.setBorderPainted(false);
		bar.setContentAreaFilled(false);
		bar.setOpaque(false);
		Color barIdle = CombatAchievementsPanel.blend(CombatAchievementsTheme.NEUTRAL_META,
			CombatAchievementsTheme.NEGATIVE, 0.72);
		bar.setForeground(barIdle);
		CardKit.addForegroundHover(bar, barIdle, CombatAchievementsTheme.NEGATIVE);
		bar.setPreferredSize(new Dimension(CombatAchievementsPanel.BAR_BUTTON_WIDTH, 18));
		bar.setMargin(new Insets(0, 4, 0, 0));
		bar.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		bar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		bar.addActionListener(e -> {
			if (panel.onBarTask() != null)
			{
				panel.onBarTask().accept(c.id);
			}
		});
		return bar;
	}

	/** A "train X to N" goal: what it opens and roughly how long, styled as a quieter sibling of unlockCard. */
	private JPanel trainingCard(SidePanelViewModel.TrainingView t)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.ACCENT),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder();
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.HEADER_GOLD))
			.append("'><b>").append(CardKit.escape(t.label)).append("</b></span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
			.append("'>").append(t.toward ? "toward " : "opens ").append(t.unlockedTaskCount)
			.append(t.unlockedTaskCount == 1 ? " CA (" : " CAs (")
			.append(t.unlockedPoints).append(t.unlockedPoints == 1 ? " pt)</span>" : " pts)</span>");
		// Just the stat and the prize; naming a boss here read oddly when the nearest rec happened
		// to be some niche monster.
		card.add(CardKit.wrappedHtmlLabel(sb.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH), BorderLayout.CENTER);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return CardKit.fullWidth(card);
	}

	/** The unlock card's text (inner html, for {@link CardKit#wrappedHtmlLabel}): quest, difficulty, prize,
	 *  prerequisites, unmet skills. Shared with the quest drill-in header so the two can never drift. */
	String unlockCardHtml(SidePanelViewModel.UnlockView u)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.HEADER_GOLD))
			.append("'><b>").append(CardKit.escape(u.questName)).append("</b></span>");
		if (u.difficulty != null && !u.difficulty.isEmpty())
		{
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(CardKit.escape(u.difficulty)).append("</span>");
		}
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
			.append("'>unlocks ").append(u.unlockedTaskCount).append(" CAs (").append(u.unlockedPoints)
			.append(" pts)</span>");
		// One prerequisite gets named; a chain becomes a count — A Kingdom Divided's seven-quest
		// comma wall made the card unreadable. The drill-in page lists the chain properly.
		if (!u.prerequisiteList.isEmpty())
		{
			String first = u.prerequisiteList.size() == 1
				? "first: " + u.prerequisiteList.get(0)
				: u.prerequisiteList.size() + " quests first";
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
				.append("'>").append(CardKit.escape(first)).append("</span>");
		}
		// Same rule as the quest chain: one stat gets named, several become a count.
		if (!u.unmetSkillList.isEmpty())
		{
			String train = u.unmetSkillList.size() == 1
				? "train: " + trainGoal(u.unmetSkillList.get(0))
				: u.unmetSkillList.size() + " stats to train";
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
				.append("'>").append(CardKit.escape(train)).append("</span>");
		}
		return sb.toString();
	}

	/** "Magic 3→75" → "Magic 75": on a card only the goal matters, the drill-in shows the journey. */
	private static String trainGoal(String raw)
	{
		int arrow = raw.indexOf('→');
		if (arrow < 0)
		{
			return raw;
		}
		String head = raw.substring(0, arrow).trim();
		int lastSpace = head.lastIndexOf(' ');
		String skill = lastSpace > 0 ? head.substring(0, lastSpace) : head;
		return skill + " " + raw.substring(arrow + 1).trim();
	}

	private JPanel unlockCard(SidePanelViewModel.UnlockView u)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.HEADER_GOLD),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));
		card.add(CardKit.wrappedHtmlLabel(unlockCardHtml(u), CombatAchievementsPanel.CARD_TEXT_WIDTH), BorderLayout.CENTER);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		if (!u.unlockedCas.isEmpty())
		{
			CardKit.onClick(card, () -> {
				panel.setRoute(panel.route().withUnlock(u));
				panel.expandedUnlockBosses().clear();
				panel.setUnlockPrereqsExpanded(false);
				panel.setUnlockStatsExpanded(false);
				panel.rebuild();
			});
		}
		return CardKit.fullWidth(card);
	}
}
