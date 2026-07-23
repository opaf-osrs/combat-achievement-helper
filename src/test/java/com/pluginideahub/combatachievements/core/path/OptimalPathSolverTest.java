package com.pluginideahub.combatachievements.core.path;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficulty;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.LowHangingFruitRanker;
import com.pluginideahub.combatachievements.core.ranking.RankedTask;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.TaskLiveSignals;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OptimalPathSolverTest
{
	private final OptimalPathSolver solver = new OptimalPathSolver();

	private static RankedTask ranked(int id, int points, double effort)
	{
		CombatAchievement a = new CombatAchievement(id, "Task " + id, AchievementTier.HARD, "Boss",
			TaskType.KILL_COUNT, points, "desc", "General", "url");
		return new RankedTask(a, effort, points / effort, "r", "", true, true, TaskDifficulty.UNKNOWN);
	}

	private static final List<RankedTask> CANDIDATES = Arrays.asList(
		ranked(1, 1, 1.0),
		ranked(2, 2, 1.5),
		ranked(3, 3, 5.0),
		ranked(4, 2, 1.2),
		ranked(5, 4, 3.0),
		ranked(6, 1, 0.5));

	@Test
	public void dpMatchesBruteForceOptimumForEveryGap()
	{
		int totalPoints = CANDIDATES.stream().mapToInt(rt -> rt.achievement().points()).sum();
		for (int gap = 1; gap <= totalPoints; gap++)
		{
			PathPlan plan = solver.solve(AchievementTier.ELITE, gap, CANDIDATES);
			double brute = bruteMinEffort(CANDIDATES, gap);
			assertTrue("reachable for gap " + gap, plan.reachable());
			assertEquals("min effort for gap " + gap, brute, plan.totalEffort(), 1e-9);
			assertTrue("points cover gap " + gap, plan.totalPoints() >= gap);
		}
	}

	@Test
	public void solveMinimisesTheSuppliedCostNotEffort()
	{
		// Two tasks that each cover a 2-point gap: task 1 is cheap on effort, task 2 is expensive.
		List<RankedTask> two = Arrays.asList(ranked(1, 2, 1.0), ranked(2, 2, 9.0));

		// Default cost is effort → the solver takes the low-effort task 1.
		PathPlan byEffort = solver.solve(AchievementTier.HARD, 2, two);
		assertEquals(1, byEffort.steps().get(0).achievement().id());

		// A custom cost that inverts effort (task 2 cheap) → the solver must instead take task 2,
		// proving the Route's quickest-path cost genuinely drives selection.
		PathPlan byCost = solver.solve(AchievementTier.HARD, 2, two,
			rt -> rt.achievement().id() == 2 ? 1.0 : 9.0);
		assertEquals(2, byCost.steps().get(0).achievement().id());
	}

	@Test
	public void planIsDeterministic()
	{
		PathPlan a = solver.solve(AchievementTier.ELITE, 5, CANDIDATES);
		PathPlan b = solver.solve(AchievementTier.ELITE, 5, CANDIDATES);
		assertEquals(stepIds(a), stepIds(b));
	}

	@Test
	public void zeroGapIsAlreadyUnlocked()
	{
		PathPlan plan = solver.solve(AchievementTier.ELITE, 0, CANDIDATES);
		assertTrue(plan.alreadyUnlocked());
		assertTrue(plan.steps().isEmpty());
	}

	@Test
	public void unreachableGapReturnsAllFlaggedNotReachable()
	{
		PathPlan plan = solver.solve(AchievementTier.ELITE, 1000, CANDIDATES);
		assertFalse(plan.reachable());
		assertEquals(CANDIDATES.size(), plan.steps().size());
	}

	@Test
	public void grandmasterBranchReturnsEveryTask()
	{
		PathPlan plan = solver.solveCompleteAll(AchievementTier.GRANDMASTER, CANDIDATES);
		assertEquals(CANDIDATES.size(), plan.steps().size());
		int sum = CANDIDATES.stream().mapToInt(rt -> rt.achievement().points()).sum();
		assertEquals(sum, plan.totalPoints());
		// steps are ordered easiest-first
		for (int i = 1; i < plan.steps().size(); i++)
		{
			assertTrue(plan.steps().get(i - 1).effort() <= plan.steps().get(i).effort() + 1e-9);
		}
	}

	@Test
	public void routePrefersLowerDifficultyTaskToCloseTheGapViaEffort()
	{
		// Two tasks that each alone close a 6-pt gap: one easy (difficulty 2), one hard (difficulty 9).
		// The Route feeds the difficulty-aware LowHangingFruitRanker effort into the solver, so the
		// min-effort path must pick the EASIER task — proving Difficulty reaches the Route (not just the
		// quick-wins / synergy rankers).
		//
		// The easier task deliberately gets the HIGHER id (11). The solver's effort tie-break is
		// (effort asc, id asc), so if difficulty were disconnected both efforts would tie and the LOWER
		// id (10, the hard task) would win — failing this assertion. It only passes because difficulty
		// gives the easy task strictly lower effort, so the id coincidence can't fake a green.
		CombatAchievement hard = new CombatAchievement(10, "Hard", AchievementTier.ELITE, "Boss",
			TaskType.KILL_COUNT, 6, "", "", "");
		CombatAchievement easy = new CombatAchievement(11, "Easy", AchievementTier.ELITE, "Boss",
			TaskType.KILL_COUNT, 6, "", "", "");
		EffortDataLibrary el = EffortDataLibrary.load(stream("{\"tasks\":{}}"));
		TaskDifficultyLibrary diff = TaskDifficultyLibrary.load(
			stream("{\"tasks\":{\"11\":{\"difficulty\":2},\"10\":{\"difficulty\":9}}}"));
		SignalsProvider allDoable = id -> new TaskLiveSignals(true, true, false, true);

		List<RankedTask> ranked = new LowHangingFruitRanker(el, EffortModel.standard(), diff)
			.rank(Arrays.asList(hard, easy), java.util.Collections.<Integer>emptySet(), allDoable, false);
		PathPlan plan = solver.solve(AchievementTier.ELITE, 6, ranked);

		assertEquals("route closes the 6-pt gap with one task", 1, plan.steps().size());
		assertEquals("route picks the easier task despite its higher id", 11,
			plan.steps().get(0).achievement().id());
	}

	// ---- clustering ---------------------------------------------------------------------------------

	private static RankedTask atBoss(int id, int points, double effort, String boss)
	{
		CombatAchievement a = new CombatAchievement(id, "Task " + id, AchievementTier.HARD, boss,
			TaskType.KILL_COUNT, points, "desc", "General", "url");
		return new RankedTask(a, effort, points / effort, "r", "", true, true, TaskDifficulty.UNKNOWN);
	}

	/** The clustered cost of a plan: the per-task cost plus one trip overhead for each distinct boss. */
	private static double clusteredCost(PathPlan plan, double overhead)
	{
		double cost = 0.0;
		java.util.Set<String> bosses = new java.util.HashSet<>();
		for (PathStep s : plan.steps())
		{
			cost += s.effort();
			bosses.add(s.achievement().monster());
		}
		return cost + overhead * bosses.size();
	}

	@Test
	public void clusteringKeepsYouAtOneBossWhenItCanCloseTheGap()
	{
		// Boss A alone can cover a gap of 3 (three 1-pt CAs). Boss B has a single cheaper 1-pt CA. With a
		// real trip overhead, doing all three at A beats two at A + a trip to B.
		List<RankedTask> tasks = Arrays.asList(
			atBoss(1, 1, 2.0, "A"), atBoss(2, 1, 2.0, "A"), atBoss(3, 1, 2.0, "A"),
			atBoss(4, 1, 0.5, "B"));

		PathPlan plan = solver.solveClustered(AchievementTier.ELITE, 3, tasks, RankedTask::effort, 10.0);

		assertTrue("reaches the gap", plan.totalPoints() >= 3);
		long bosses = plan.steps().stream().map(s -> s.achievement().monster()).distinct().count();
		assertEquals("stays at a single boss rather than making a second trip", 1, bosses);
	}

	@Test
	public void clusteringIsNeverWorseThanScatteringToCloseTheGap()
	{
		// Two bosses, three CAs each, mixed effort. For every gap and a range of overheads, the clustered
		// solver's true cost (with per-boss trips) must not exceed what the boss-blind solver would cost.
		List<RankedTask> tasks = Arrays.asList(
			atBoss(1, 2, 3.0, "A"), atBoss(2, 1, 1.0, "A"), atBoss(3, 3, 4.0, "A"),
			atBoss(4, 2, 2.0, "B"), atBoss(5, 1, 1.5, "B"), atBoss(6, 4, 5.0, "B"));
		int total = tasks.stream().mapToInt(rt -> rt.achievement().points()).sum();

		for (double overhead : new double[]{0.0, 2.0, 8.0})
		{
			for (int gap = 1; gap <= total; gap++)
			{
				PathPlan clustered = solver.solveClustered(AchievementTier.ELITE, gap, tasks,
					RankedTask::effort, overhead);
				PathPlan scattered = solver.solve(AchievementTier.ELITE, gap, tasks, RankedTask::effort);

				assertTrue("clustered reaches gap " + gap, clustered.totalPoints() >= gap);
				assertTrue("clustered (overhead " + overhead + ", gap " + gap + ") is no costlier than "
						+ "scattering once trips are counted",
					clusteredCost(clustered, overhead) <= clusteredCost(scattered, overhead) + EPS);
			}
		}
	}

	@Test
	public void withNoTripOverheadClusteringReachesTheSameCostAsThePlainSolver()
	{
		// Overhead 0 removes the only reason to cluster, so the two solvers must tie on total cost.
		int total = CANDIDATES.stream().mapToInt(rt -> rt.achievement().points()).sum();
		for (int gap = 1; gap <= total; gap++)
		{
			PathPlan clustered = solver.solveClustered(AchievementTier.ELITE, gap, CANDIDATES,
				RankedTask::effort, 0.0);
			double brute = bruteMinEffort(CANDIDATES, gap);
			assertEquals("clustered with no overhead is optimal at gap " + gap,
				brute, clusteredCost(clustered, 0.0), 1e-9);
		}
	}

	private static final double EPS = 1e-9;

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	private static List<Integer> stepIds(PathPlan plan)
	{
		List<Integer> ids = new ArrayList<>();
		plan.steps().forEach(s -> ids.add(s.achievement().id()));
		return ids;
	}

	/** Exhaustive 2^N reference: minimum total effort of any subset whose points reach the gap. */
	private static double bruteMinEffort(List<RankedTask> items, int gap)
	{
		int n = items.size();
		double best = Double.POSITIVE_INFINITY;
		for (int mask = 0; mask < (1 << n); mask++)
		{
			int points = 0;
			double effort = 0.0;
			for (int i = 0; i < n; i++)
			{
				if ((mask & (1 << i)) != 0)
				{
					points += items.get(i).achievement().points();
					effort += items.get(i).effort();
				}
			}
			if (points >= gap && effort < best)
			{
				best = effort;
			}
		}
		return best;
	}
}
