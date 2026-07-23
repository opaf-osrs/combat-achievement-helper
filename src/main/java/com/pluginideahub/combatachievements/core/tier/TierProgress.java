package com.pluginideahub.combatachievements.core.tier;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;

/**
 * Per-tier completion snapshot: how many of a tier's tasks are done, the points that represents, and
 * whether the tier's cumulative reward threshold has been unlocked. Pure value object.
 */
public final class TierProgress
{
	private final AchievementTier tier;
	private final int completedCount;
	private final int totalCount;
	private final int earnedPointsInTier;
	private final int totalPointsInTier;
	private final int cumulativeThreshold;
	private final boolean unlocked;
	private final int pointsRemainingToUnlock;

	public TierProgress(AchievementTier tier, int completedCount, int totalCount,
		int earnedPointsInTier, int totalPointsInTier, int cumulativeThreshold,
		boolean unlocked, int pointsRemainingToUnlock)
	{
		this.tier = tier;
		this.completedCount = completedCount;
		this.totalCount = totalCount;
		this.earnedPointsInTier = earnedPointsInTier;
		this.totalPointsInTier = totalPointsInTier;
		this.cumulativeThreshold = cumulativeThreshold;
		this.unlocked = unlocked;
		this.pointsRemainingToUnlock = pointsRemainingToUnlock;
	}

	public AchievementTier tier()
	{
		return tier;
	}

	public int completedCount()
	{
		return completedCount;
	}

	public int totalCount()
	{
		return totalCount;
	}

	public int earnedPointsInTier()
	{
		return earnedPointsInTier;
	}

	public int totalPointsInTier()
	{
		return totalPointsInTier;
	}

	public int cumulativeThreshold()
	{
		return cumulativeThreshold;
	}

	public boolean unlocked()
	{
		return unlocked;
	}

	public int pointsRemainingToUnlock()
	{
		return pointsRemainingToUnlock;
	}

	/** True when every task in this tier is complete. */
	public boolean fullyComplete()
	{
		return totalCount > 0 && completedCount == totalCount;
	}
}
