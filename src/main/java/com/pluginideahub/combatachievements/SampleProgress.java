package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.tier.TierMath;
import java.util.HashSet;
import java.util.Set;

/**
 * Builds a deterministic, plausible progress snapshot so the full UI is reviewable while the live
 * varbit reader is gated (see {@code varbit.CaVarbitIds}). Dev-only; never used once
 * {@code CaVarbitIds.VERIFIED} is true.
 */
final class SampleProgress
{
	private SampleProgress()
	{
	}

	static ProgressSnapshot build(CombatAchievementLibrary lib)
	{
		Set<Integer> completed = new HashSet<>();
		for (CombatAchievement task : lib.all())
		{
			if (isSampleComplete(task))
			{
				completed.add(task.id());
			}
		}
		int computed = 0;
		for (Integer id : completed)
		{
			computed += lib.pointsById().getOrDefault(id, 0);
		}
		AchievementTier current = TierMath.currentTierFor(computed, lib.all());
		return new ProgressSnapshot(completed, computed, computed, current, 0L, true);
	}

	private static boolean isSampleComplete(CombatAchievement task)
	{
		switch (task.tier())
		{
			case EASY:
				return true;
			case MEDIUM:
				return task.id() % 2 == 0;
			case HARD:
				return task.id() % 5 == 0;
			default:
				return false;
		}
	}
}
