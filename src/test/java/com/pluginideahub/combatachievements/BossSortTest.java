package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Guards the Bosses-mode Order control ({@link CombatAchievementsPanel#bossComparator}). The bosses are
 * engineered so the three sorts each produce a DISTINCT order (proving each uses its own metric), the
 * difficulty tie-break is exercised (Alpha vs Echo tie on avg difficulty, split by time), and the locked
 * boss sinks to the bottom in every sort. Figures below are at the default overhead (6 min):
 * <pre>
 *   Alpha   : 10 pts, CAs diff 2/10min + diff 4/10min  ->  20 min, 23.1 pts/hr, avg diff 3.0
 *   Bravo   : 20 pts, CA   diff 8/60min                ->  60 min, 18.2 pts/hr, avg diff 8.0
 *   Charlie :  6 pts, CA   diff 1/5min                 ->   5 min, 32.7 pts/hr, avg diff 1.0
 *   Echo    : 30 pts, CA   diff 3/120min               -> 120 min, 14.3 pts/hr, avg diff 3.0 (easy but slow)
 *   Delta   :  0 pts, no doable CAs                     -> locked (always last)
 * </pre>
 * {@link #clusteringWeightPromotesMultiCaBossOverTrivialHop} separately proves the overhead does its job:
 * the amortised trip cost pulls a meaty multi-CA boss above a trivial single-CA hop.
 */
public class BossSortTest
{
	private static final int DEFAULT_OVERHEAD = 6;

	private static SidePanelViewModel.CaDetail ca(int difficulty, int estMinutes)
	{
		return new SidePanelViewModel.CaDetail(0, "ca", "", "", "", 0, "", true, "",
			difficulty, 0, 0.0, "", estMinutes, 0, Collections.emptyList(),
			"", "", "", "", "", "", false, 0);
	}

	private static SidePanelViewModel.BossRow boss(String name, int projectedPoints,
		SidePanelViewModel.CaDetail... doable)
	{
		List<SidePanelViewModel.CaDetail> list = Arrays.asList(doable);
		return new SidePanelViewModel.BossRow(name, projectedPoints, list.size(), 0,
			list.isEmpty(), "", list, Collections.emptyList());
	}

	private static List<String> order(List<SidePanelViewModel.BossRow> rows,
		CombatAchievementsPanel.Sort sort, int tripOverhead)
	{
		List<SidePanelViewModel.BossRow> copy = new ArrayList<>(rows);
		copy.sort(CombatAchievementsPanel.bossComparator(sort, tripOverhead));
		List<String> names = new ArrayList<>();
		for (SidePanelViewModel.BossRow b : copy)
		{
			names.add(b.monster);
		}
		return names;
	}

	// Deliberately not pre-sorted, so the comparator has real work to do.
	private static List<SidePanelViewModel.BossRow> bosses()
	{
		return new ArrayList<>(Arrays.asList(
			boss("Bravo", 20, ca(8, 60)),
			boss("Delta", 0),
			boss("Echo", 30, ca(3, 120)),
			boss("Charlie", 6, ca(1, 5)),
			boss("Alpha", 10, ca(2, 10), ca(4, 10))));
	}

	@Test
	public void recommendedSortsByAmortisedPointsPerHour()
	{
		assertEquals(Arrays.asList("Charlie", "Alpha", "Bravo", "Echo", "Delta"),
			order(bosses(), CombatAchievementsPanel.Sort.RECOMMENDED, DEFAULT_OVERHEAD));
	}

	@Test
	public void mostPointsSortsByProjectedPoints()
	{
		assertEquals(Arrays.asList("Echo", "Bravo", "Alpha", "Charlie", "Delta"),
			order(bosses(), CombatAchievementsPanel.Sort.MOST_POINTS, DEFAULT_OVERHEAD));
	}

	@Test
	public void easiestSortsByAverageDifficultyThenQuickest()
	{
		// Alpha and Echo tie at avg difficulty 3.0; the quicker one (Alpha, 20 min) comes first.
		assertEquals(Arrays.asList("Charlie", "Alpha", "Echo", "Bravo", "Delta"),
			order(bosses(), CombatAchievementsPanel.Sort.EASIEST, DEFAULT_OVERHEAD));
	}

	@Test
	public void lockedBossAlwaysSinksToBottom()
	{
		for (CombatAchievementsPanel.Sort sort : CombatAchievementsPanel.Sort.values())
		{
			List<String> names = order(bosses(), sort, DEFAULT_OVERHEAD);
			assertEquals("locked boss must be last for " + sort, "Delta", names.get(names.size() - 1));
		}
	}

	@Test
	public void recommendedWithTimeOffRanksByPointsNotAlphabetically()
	{
		// Both Bosses dials off (overhead 0, time weight 0): every boss's pts/hr denominator vanishes, so the
		// sort must fall back to available points — NOT collapse to alphabetical (the fixed edge case).
		List<SidePanelViewModel.BossRow> copy = new ArrayList<>(bosses());
		copy.sort(CombatAchievementsPanel.bossComparator(CombatAchievementsPanel.Sort.RECOMMENDED, 0, 0.0));
		List<String> names = new ArrayList<>();
		for (SidePanelViewModel.BossRow b : copy)
		{
			names.add(b.monster);
		}
		assertEquals(Arrays.asList("Echo", "Bravo", "Alpha", "Charlie", "Delta"), names);
	}

	@Test
	public void clusteringWeightPromotesMultiCaBossOverTrivialHop()
	{
		List<SidePanelViewModel.BossRow> rows = new ArrayList<>(Arrays.asList(
			boss("Trivial", 3, ca(1, 3)),               // one quick 3-pt CA
			boss("Meaty", 40, ca(3, 50), ca(4, 50))));  // 40 pts across a longer, multi-CA visit

		// No overhead: raw points-per-time favours the trivial single-CA hop (60 vs 24 pts/hr).
		assertEquals(Arrays.asList("Trivial", "Meaty"),
			order(rows, CombatAchievementsPanel.Sort.RECOMMENDED, 0));

		// With the clustering overhead the fixed trip cost amortises across the meaty boss's CAs, pulling
		// it above the trivial hop (22.6 vs 20 pts/hr) — the whole point of the weighting.
		assertEquals(Arrays.asList("Meaty", "Trivial"),
			order(rows, CombatAchievementsPanel.Sort.RECOMMENDED, DEFAULT_OVERHEAD));
	}
}
