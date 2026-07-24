package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** Renders the Bosses mode: the ranked boss directory, one row per boss with incomplete CAs. */
public class BossesPage
{
	private final CombatAchievementsPanel panel;
	private final JPanel content;

	public BossesPage(CombatAchievementsPanel panel)
	{
		this.panel = panel;
		this.content = panel.content();
	}

	public void buildBosses()
	{
		String q = panel.searchText();
		List<SidePanelViewModel.BossRow> all = panel.model().bosses();
		if (all == null || all.isEmpty())
		{
			content.add(panel.messageLabel("No bosses with incomplete CAs."));
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
			content.add(panel.messageLabel("No bosses match your search."));
			return;
		}
		bosses.sort(bossComparator(panel.sort(), panel.tripOverheadMinutes(), panel.bossTimeWeight()));
		for (SidePanelViewModel.BossRow b : bosses)
		{
			content.add(bossRowCard(b));
			content.add(CardKit.spacer());
		}
	}

	/**
	 * The boss-list ordering for a given {@link CombatAchievementsPanel.Sort}. Locked bosses (no doable
	 * CAs) always sink to the bottom. The metrics are derived from each boss's doable CAs:
	 * <ul>
	 *   <li>{@code RECOMMENDED} — points per hour with the per-trip overhead amortised in
	 *       ({@link #bossPointsPerHour}); because a CA's estimated minutes already scale with its
	 *       Difficulty and the fixed {@code tripOverheadMinutes} is spread across the whole visit, this is
	 *       the "points / time / ease" value blend that also rewards clustering (staying at one boss).</li>
	 *   <li>{@code MOST_POINTS} — projected (doable-now) points, highest first.</li>
	 *   <li>{@code EASIEST} — lowest average doable-CA Difficulty, tie-broken by quickest then most points.</li>
	 * </ul>
	 */
	public static Comparator<SidePanelViewModel.BossRow> bossComparator(CombatAchievementsPanel.Sort sort,
		int tripOverheadMinutes)
	{
		return bossComparator(sort, tripOverheadMinutes, 1.0);
	}

	public static Comparator<SidePanelViewModel.BossRow> bossComparator(CombatAchievementsPanel.Sort sort,
		int tripOverheadMinutes, double timeWeight)
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
					.thenComparing(Comparator.comparingDouble(BossesPage::bossAvgDifficulty))
					.thenComparing(Comparator.comparingInt(BossesPage::bossDoableMinutes))
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
			.append("'><b>").append(CardKit.escape(b.monster)).append("</b></span>");
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
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>(locked)</span>");
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.NEGATIVE))
				.append("'>").append(CardKit.escape(reason)).append("</span>")
				.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(b.lockedCount)
				.append(b.lockedCount == 1 ? " CA</span>" : " CAs</span>");
		}
		else
		{
			sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
				.append("'>").append(b.projectedPoints).append(" pts available</span>")
				.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(b.doableCount)
				.append(b.doableCount == 1 ? " CA" : " CAs");
			if (b.lockedCount > 0)
			{
				sb.append(" (+").append(b.lockedCount).append(" locked)");
			}
			sb.append("</span>");
		}
		sb.append("</body></html>");
		card.add(new JLabel(sb.toString()), BorderLayout.CENTER);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withBoss(b.monster));
			panel.rebuild();
		});
		return CardKit.fullWidth(card);
	}
}
