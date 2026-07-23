package com.pluginideahub.combatachievements.core.path;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.ranking.RankedTask;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Finds the minimum-total-effort set of incomplete tasks whose points close the gap to a target
 * tier's cumulative threshold — an exact min-cost covering knapsack solved by DP over the integer
 * points axis (effort, a real number, is the minimized cost). O(N·gap) time, O(N·gap) space; with
 * N ≤ 637 and gap ≤ 2630 this is instant. Deterministic: candidates are processed in (effort asc, id
 * asc) order and ties resolve to "skip", so the same input always yields the same plan. See
 * docs/DESIGN.md §6c.
 */
public final class OptimalPathSolver
{
	/** Float-equality tolerance for effort comparisons. */
	public static final double EPS = 1e-9;

	/**
	 * Min-effort path to cross {@code pointsGap} points using {@code candidates} (incomplete,
	 * typically doable-now tasks, each carrying its effort). Steps are returned easiest-first.
	 */
	public PathPlan solve(AchievementTier targetTier, int pointsGap, List<RankedTask> candidates)
	{
		return solve(targetTier, pointsGap, candidates, RankedTask::effort);
	}

	/**
	 * As {@link #solve(AchievementTier, int, List)} but minimising an arbitrary per-task {@code cost}
	 * (e.g. estimated minutes for the quickest path) instead of the default abstract effort. Steps are
	 * returned cheapest-first by the same cost.
	 */
	public PathPlan solve(AchievementTier targetTier, int pointsGap, List<RankedTask> candidates,
		ToDoubleFunction<RankedTask> cost)
	{
		if (pointsGap <= 0)
		{
			return PathPlan.alreadyUnlocked(targetTier);
		}

		List<RankedTask> items = sortedByCost(candidates, cost);
		int n = items.size();

		int sumPoints = 0;
		for (RankedTask rt : items)
		{
			sumPoints += rt.achievement().points();
		}
		if (sumPoints < pointsGap)
		{
			// Not enough doable tasks to reach the gap: surface them all, flagged unreachable.
			return buildPlan(targetTier, pointsGap, items, false);
		}

		int gap = pointsGap;
		double[][] dp = new double[n + 1][gap + 1];
		boolean[][] took = new boolean[n + 1][gap + 1];
		for (int p = 1; p <= gap; p++)
		{
			dp[0][p] = Double.POSITIVE_INFINITY;
		}
		for (int i = 1; i <= n; i++)
		{
			int value = items.get(i - 1).achievement().points();
			double c = cost.applyAsDouble(items.get(i - 1));
			for (int p = 0; p <= gap; p++)
			{
				double skip = dp[i - 1][p];
				int prev = Math.max(0, p - value);
				double take = dp[i - 1][prev] + c;
				if (take < skip - EPS)
				{
					dp[i][p] = take;
					took[i][p] = true;
				}
				else
				{
					dp[i][p] = skip;
				}
			}
		}

		List<RankedTask> chosen = new ArrayList<>();
		int p = gap;
		for (int i = n; i >= 1; i--)
		{
			if (took[i][p])
			{
				chosen.add(items.get(i - 1));
				p = Math.max(0, p - items.get(i - 1).achievement().points());
			}
		}
		return buildPlan(targetTier, pointsGap, sortedByCost(chosen, cost), true);
	}

	/**
	 * Grandmaster branch: GM additionally requires completing <em>every</em> remaining task, so the
	 * "path" is simply all incomplete candidates, cheapest-first. See docs/DESIGN.md §4.2/§6c.
	 */
	public PathPlan solveCompleteAll(AchievementTier targetTier, List<RankedTask> candidates)
	{
		return solveCompleteAll(targetTier, candidates, RankedTask::effort);
	}

	public PathPlan solveCompleteAll(AchievementTier targetTier, List<RankedTask> candidates,
		ToDoubleFunction<RankedTask> cost)
	{
		if (candidates == null || candidates.isEmpty())
		{
			return PathPlan.alreadyUnlocked(targetTier);
		}
		List<RankedTask> ordered = sortedByCost(candidates, cost);
		int gap = 0;
		for (RankedTask rt : ordered)
		{
			gap += rt.achievement().points();
		}
		return buildPlan(targetTier, gap, ordered, true);
	}

	private static List<RankedTask> sortedByCost(List<RankedTask> candidates,
		ToDoubleFunction<RankedTask> cost)
	{
		List<RankedTask> items = new ArrayList<>(candidates);
		items.sort(Comparator
			.comparingDouble(cost)
			.thenComparingInt(rt -> rt.achievement().id()));
		return items;
	}

	private static PathPlan buildPlan(AchievementTier targetTier, int pointsGap,
		List<RankedTask> ordered, boolean reachable)
	{
		List<PathStep> steps = new ArrayList<>();
		int cumulative = 0;
		double totalEffort = 0.0;
		for (RankedTask rt : ordered)
		{
			cumulative += rt.achievement().points();
			totalEffort += rt.effort();
			steps.add(new PathStep(rt.achievement(), rt.effort(), cumulative));
		}
		return new PathPlan(targetTier, pointsGap, steps, cumulative, totalEffort, reachable, false);
	}
}
