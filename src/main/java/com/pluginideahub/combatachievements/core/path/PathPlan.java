package com.pluginideahub.combatachievements.core.path;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.util.Collections;
import java.util.List;

/**
 * The result of {@link OptimalPathSolver}: the ordered set of tasks that closes the points gap to a
 * target tier with minimum total effort. Pure value object.
 */
public final class PathPlan
{
	private final AchievementTier targetTier;
	private final int pointsGap;
	private final List<PathStep> steps;
	private final int totalPoints;
	private final double totalEffort;
	private final boolean reachable;
	private final boolean alreadyUnlocked;

	public PathPlan(AchievementTier targetTier, int pointsGap, List<PathStep> steps,
		int totalPoints, double totalEffort, boolean reachable, boolean alreadyUnlocked)
	{
		this.targetTier = targetTier;
		this.pointsGap = pointsGap;
		this.steps = Collections.unmodifiableList(steps);
		this.totalPoints = totalPoints;
		this.totalEffort = totalEffort;
		this.reachable = reachable;
		this.alreadyUnlocked = alreadyUnlocked;
	}

	/** The "nothing to do — already unlocked" plan. */
	public static PathPlan alreadyUnlocked(AchievementTier targetTier)
	{
		return new PathPlan(targetTier, 0, Collections.emptyList(), 0, 0.0, true, true);
	}

	public AchievementTier targetTier()
	{
		return targetTier;
	}

	public int pointsGap()
	{
		return pointsGap;
	}

	public List<PathStep> steps()
	{
		return steps;
	}

	public int totalPoints()
	{
		return totalPoints;
	}

	public double totalEffort()
	{
		return totalEffort;
	}

	/** True when the chosen tasks actually reach the gap; false when there aren't enough doable tasks. */
	public boolean reachable()
	{
		return reachable;
	}

	public boolean alreadyUnlocked()
	{
		return alreadyUnlocked;
	}
}
