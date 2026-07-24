package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Renders a boss page: its headline, then its CAs split into doable / train-first / locked / done. */
public class BossDetailPage
{
	private final CombatAchievementsPanel panel;
	/** The CA-detail page owns the shared "Recommended stats" prose renderer. */
	private final CaDetailPage caDetail;
	private final JPanel content;

	public BossDetailPage(CombatAchievementsPanel panel, CaDetailPage caDetail)
	{
		this.panel = panel;
		this.caDetail = caDetail;
		this.content = panel.content();
	}

	public void buildBossDetail(String monster)
	{
		content.add(panel.backButton("← All bosses", () -> {
			panel.setRoute(panel.route().clearBoss());
			panel.rebuild();
		}));
		content.add(CardKit.spacer());

		SidePanelViewModel.BossRow boss = null;
		for (SidePanelViewModel.BossRow b : panel.model().bosses())
		{
			if (monster.equals(b.monster))
			{
				boss = b;
				break;
			}
		}
		if (boss == null)
		{
			content.add(panel.messageLabel("Boss not found."));
			return;
		}

		StringBuilder head = new StringBuilder("<span style='color:")
			.append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NAME))
			.append("'><b style='font-size:11px'>").append(CardKit.escape(boss.monster)).append("</b></span>");
		if (boss.locked)
		{
			head.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>Locked — no doable CAs yet.</span>");
		}
		else
		{
			head.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
				.append("'>").append(boss.projectedPoints).append(" pts available</span>")
				.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(boss.doableCount)
				.append(boss.doableCount == 1 ? " CA</span>" : " CAs</span>");
		}
		if (!boss.completedCas.isEmpty())
		{
			head.append("<br><span style='color:" + CardKit.metaHex() + "'>")
				.append(boss.completedCas.size()).append(" of ").append(boss.totalCas())
				.append(" done</span>");
		}
		content.add(CardKit.fullWidth(CardKit.wrappedHtmlLabel(head.toString(), CombatAchievementsPanel.CARD_TEXT_WIDTH)));
		content.add(CardKit.spacer());

		if (!boss.recommendedStats.isEmpty())
		{
			caDetail.addDetailText("Recommended stats", boss.recommendedStats);
		}

		// "Doable" only means no hard gate blocks you, so a level-3 saw seven Barrows CAs it was 49-84
		// levels short of, all reading as available. Split on the ready line: what you could go and do
		// now, and below it what is ungated but out of reach. Nothing is hidden either way.
		// Headings: "Doable now" / "Train first" / "Locked". The middle group was called "Not yet", which
		// read as "not unlocked yet" — i.e. the Locked group below, the exact opposite of what it holds.
		// The lesson: a heading describing the CONTENT ("not yet", "out of reach") always blurs into
		// "locked", because from the task's side both mean "you can't have it". These two groups differ in
		// what they are ABOUT — Locked is a gate on the content, this is your stats — so the heading names
		// the fix instead, matching the Route's "Train next".
		List<SidePanelViewModel.CaDetail> reachable = new ArrayList<>();
		List<SidePanelViewModel.CaDetail> notYet = new ArrayList<>();
		for (SidePanelViewModel.CaDetail d : boss.doable)
		{
			(d.withinReach ? reachable : notYet).add(d);
		}
		if (!reachable.isEmpty())
		{
			content.add(panel.collapseHeader("Doable now (" + reachable.size() + ")", panel.doableCollapsed(),
				() -> { panel.setDoableCollapsed(!panel.doableCollapsed()); panel.rebuild(); }));
			if (!panel.doableCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.CaDetail d : reachable)
				{
					content.add(caCard(d));
					content.add(CardKit.spacer());
				}
			}
		}
		if (!notYet.isEmpty())
		{
			content.add(panel.collapseHeader("Train first (" + notYet.size() + ")", panel.trainFirstCollapsed(),
				() -> { panel.setTrainFirstCollapsed(!panel.trainFirstCollapsed()); panel.rebuild(); }));
			if (!panel.trainFirstCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.CaDetail d : notYet)
				{
					content.add(caCard(d));
					content.add(CardKit.spacer());
				}
			}
		}
		if (!boss.lockedCas.isEmpty())
		{
			content.add(panel.collapseHeader("Locked (" + boss.lockedCas.size() + ")", panel.lockedCollapsed(),
				() -> { panel.setLockedCollapsed(!panel.lockedCollapsed()); panel.rebuild(); }));
			if (!panel.lockedCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.CaDetail d : boss.lockedCas)
				{
					content.add(caCard(d));
					content.add(CardKit.spacer());
				}
			}
		}

		// What you have already done here, collapsed by default so the boss page stays forward-looking.
		if (!boss.completedCas.isEmpty())
		{
			content.add(CardKit.spacer());
			content.add(panel.collapseHeader("Completed (" + boss.completedCas.size() + ")", panel.completedCollapsed(),
				() -> { panel.setCompletedCollapsed(!panel.completedCollapsed()); panel.rebuild(); }));
			if (!panel.completedCollapsed())
			{
				content.add(CardKit.spacer());
				for (SidePanelViewModel.CaDetail d : boss.completedCas)
				{
					content.add(completedCard(d));
					content.add(CardKit.spacer());
				}
			}
		}
	}

	/**
	 * A completed CA at a boss: same shape as the other cards but greyed with a tick, so a glance down the
	 * boss page separates "done" from "to do" without reading the text.
	 */
	private JPanel completedCard(SidePanelViewModel.CaDetail d)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, CombatAchievementsTheme.POSITIVE),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder("<html><body style='width:166px'>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POSITIVE))
			.append("'>&#10003; </span>");
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
			.append("'><b>").append(CardKit.escape(d.name)).append("</b></span>");
		sb.append("<br><span style='color:" + CardKit.metaHex() + "'>").append(d.points)
			.append(d.points == 1 ? " pt" : " pts").append(" · ").append(CardKit.escape(d.tierName))
			.append("</span>");
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(d));
			panel.rebuild();
		});
		return CardKit.fullWidth(card);
	}

	/** A clickable CA row from a CaDetail — orange (doable) or red (locked) — opening the CA detail. */
	/** A CA card for the boss detail — same layout as the CAs-list task card for a consistent look. */
	private JPanel caCard(SidePanelViewModel.CaDetail d)
	{
		boolean toggle = d.doableNow && (panel.onAddToRoute() != null || panel.onRemoveFromRoute() != null);
		final int textWidth = toggle ? 166 - CombatAchievementsPanel.BAR_BUTTON_WIDTH : 166;
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = d.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.LOCKED;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		StringBuilder sb = new StringBuilder();
		sb.append("<span style='color:").append(CombatAchievementsTheme.hex(accent))
			.append("'><b>").append(CardKit.escape(d.name)).append("</b></span>");
		if (!d.doableNow)
		{
			String lock = d.lockReason == null || d.lockReason.isEmpty() ? "locked" : d.lockReason;
			sb.append(" <span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
				.append("'>(").append(CardKit.escape(lock)).append(")</span>");
		}
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
			.append("'>").append(CardKit.escape(d.description)).append("</span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(d.points).append(" pts</span>")
			.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(CardKit.escape(d.tierName)).append("</span>");
		if (d.difficulty > 0)
		{
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(CardKit.difficultyColor(d.difficulty)))
				.append("'>difficulty ").append(d.difficulty).append("</span>");
		}
		JLabel label = CardKit.wrappedHtmlLabel(sb.toString(), textWidth);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		// The border sits inside the pinned size, so grow the pin or it eats the text's height.
		Dimension pinned = label.getPreferredSize();
		Dimension withBorder = new Dimension(pinned.width, pinned.height + 3);
		label.setPreferredSize(withBorder);
		label.setMaximumSize(withBorder);
		card.add(label, BorderLayout.CENTER);

		card.add(panel.linkRow(d.wikiUrl, d.guideUrl, d.curatedVideo), BorderLayout.SOUTH);
		// "-" when this CA is already in the route, "+" when it is not, so a boss page doubles as a way to
		// steer the route: take more of this boss, or drop the ones you would rather skip.
		if (toggle)
		{
			card.add(routeToggle(d), BorderLayout.EAST);
		}
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(d));
			panel.rebuild();
		});
		CardKit.fullWidth(card);
		card.setMaximumSize(new Dimension(CombatAchievementsPanel.ROUTE_CARD_MAX_WIDTH, card.getPreferredSize().height));
		return card;
	}

	/** True when this CA is one of the Route's current steps. */
	private boolean inRoute(int id)
	{
		if (panel.model().path() == null)
		{
			return false;
		}
		for (SidePanelViewModel.PathRow r : panel.model().path().steps)
		{
			if (r.id == id)
			{
				return true;
			}
		}
		return false;
	}

	/** The boss page's route control: "-" to drop a routed CA, "+" to pull one in. */
	private JButton routeToggle(SidePanelViewModel.CaDetail d)
	{
		boolean routed = inRoute(d.id);
		JButton b = new JButton(routed ? "-" : "+");
		b.setFont(FontManager.getRunescapeBoldFont());
		b.setToolTipText(routed
			? "Remove " + d.name + " from the route"
			: "Add " + d.name + " to the route");
		b.setFocusPainted(false);
		b.setBorderPainted(false);
		b.setContentAreaFilled(false);
		b.setOpaque(false);
		Color idle = routed
			? CombatAchievementsPanel.blend(CombatAchievementsTheme.NEUTRAL_META, CombatAchievementsTheme.NEGATIVE, 0.72)
			: CombatAchievementsPanel.blend(CombatAchievementsTheme.NEUTRAL_META, CombatAchievementsTheme.POSITIVE, 0.72);
		b.setForeground(idle);
		CardKit.addForegroundHover(b, idle,
			routed ? CombatAchievementsTheme.NEGATIVE : CombatAchievementsTheme.POSITIVE);
		b.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		b.setPreferredSize(new Dimension(CombatAchievementsPanel.BAR_BUTTON_WIDTH, 18));
		b.setCursor(new Cursor(Cursor.HAND_CURSOR));
		b.addActionListener(e -> {
			Consumer<Integer> handler = routed ? panel.onRemoveFromRoute() : panel.onAddToRoute();
			if (handler != null)
			{
				handler.accept(d.id);
			}
		});
		return b;
	}
}
