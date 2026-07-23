package com.pluginideahub.combatachievements.core.tier;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, dataset-driven tier arithmetic. Counts and thresholds are computed from the loaded task list
 * — never hard-coded — so they self-update when the dataset gains tasks. A tier's cumulative unlock
 * threshold is the running sum of points up to and including that tier. See docs/DESIGN.md §4.2/§9.
 */
public final class TierMath
{
	private TierMath()
	{
	}

	/** A shortfall to the next reward tier. */
	public static final class TierGap
	{
		private final AchievementTier nextTier;
		private final int pointsNeeded;

		public TierGap(AchievementTier nextTier, int pointsNeeded)
		{
			this.nextTier = nextTier;
			this.pointsNeeded = pointsNeeded;
		}

		public AchievementTier nextTier()
		{
			return nextTier;
		}

		public int pointsNeeded()
		{
			return pointsNeeded;
		}
	}

	/** Total points available across every task in the dataset. */
	public static int totalPointsAvailable(List<CombatAchievement> all)
	{
		int sum = 0;
		for (CombatAchievement task : all)
		{
			sum += task.points();
		}
		return sum;
	}

	/** Points available within a single tier (task count × tier rank). */
	public static int pointsInTier(AchievementTier tier, List<CombatAchievement> all)
	{
		int sum = 0;
		for (CombatAchievement task : all)
		{
			if (task.tier() == tier)
			{
				sum += task.points();
			}
		}
		return sum;
	}

	/** Cumulative points needed to unlock a tier's rewards: running sum of points up to and incl. it. */
	public static int thresholdFor(AchievementTier tier, List<CombatAchievement> all)
	{
		int sum = 0;
		for (CombatAchievement task : all)
		{
			if (task.tier().ordinal() <= tier.ordinal())
			{
				sum += task.points();
			}
		}
		return sum;
	}

	/** Highest tier whose cumulative threshold has been reached with {@code earnedPoints}; null if none. */
	public static AchievementTier currentTierFor(int earnedPoints, List<CombatAchievement> all)
	{
		AchievementTier current = null;
		for (AchievementTier tier : AchievementTier.values())
		{
			if (earnedPoints >= thresholdFor(tier, all))
			{
				current = tier;
			}
			else
			{
				break;
			}
		}
		return current;
	}

	/** The next tier still to unlock and the points still needed; null when everything is unlocked. */
	public static TierGap gapToNextTier(int earnedPoints, List<CombatAchievement> all)
	{
		for (AchievementTier tier : AchievementTier.values())
		{
			int threshold = thresholdFor(tier, all);
			if (earnedPoints < threshold)
			{
				return new TierGap(tier, threshold - earnedPoints);
			}
		}
		return null;
	}

	/**
	 * Per-tier progress. Completed counts/points come from {@code completedIds}; the unlock decision
	 * uses {@code unlockPoints} (pass the game's own points varp, or the derived earned total).
	 */
	public static List<TierProgress> progressByTier(Set<Integer> completedIds,
		List<CombatAchievement> all, int unlockPoints)
	{
		Map<AchievementTier, int[]> stats = new EnumMap<>(AchievementTier.class);
		for (AchievementTier tier : AchievementTier.values())
		{
			// [completedCount, totalCount, earnedPoints, totalPoints]
			stats.put(tier, new int[4]);
		}
		for (CombatAchievement task : all)
		{
			int[] s = stats.get(task.tier());
			s[1] += 1;
			s[3] += task.points();
			if (completedIds.contains(task.id()))
			{
				s[0] += 1;
				s[2] += task.points();
			}
		}

		List<TierProgress> result = new ArrayList<>();
		for (AchievementTier tier : AchievementTier.values())
		{
			int[] s = stats.get(tier);
			int threshold = thresholdFor(tier, all);
			boolean unlocked = unlockPoints >= threshold;
			int remaining = Math.max(0, threshold - unlockPoints);
			result.add(new TierProgress(tier, s[0], s[1], s[2], s[3], threshold, unlocked, remaining));
		}
		return Collections.unmodifiableList(result);
	}

	/** Convenience: derive earned points from the completed set and use it as the unlock points. */
	public static List<TierProgress> progressByTier(Set<Integer> completedIds,
		List<CombatAchievement> all)
	{
		int earned = 0;
		for (CombatAchievement task : all)
		{
			if (completedIds.contains(task.id()))
			{
				earned += task.points();
			}
		}
		return progressByTier(completedIds, all, earned);
	}
}
