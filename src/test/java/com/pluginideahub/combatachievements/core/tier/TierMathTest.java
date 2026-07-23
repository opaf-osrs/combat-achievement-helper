package com.pluginideahub.combatachievements.core.tier;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TierMathTest
{
	private static final List<CombatAchievement> ALL = CombatAchievementLibrary.loadBundled().all();

	@Test
	public void totalPointsMatchVerifiedNumber()
	{
		assertEquals(2630, TierMath.totalPointsAvailable(ALL));
	}

	@Test
	public void cumulativeThresholdsMatchVerifiedNumbers()
	{
		assertEquals(41, TierMath.thresholdFor(AchievementTier.EASY, ALL));
		assertEquals(161, TierMath.thresholdFor(AchievementTier.MEDIUM, ALL));
		assertEquals(416, TierMath.thresholdFor(AchievementTier.HARD, ALL));
		assertEquals(1064, TierMath.thresholdFor(AchievementTier.ELITE, ALL));
		assertEquals(1904, TierMath.thresholdFor(AchievementTier.MASTER, ALL));
		assertEquals(2630, TierMath.thresholdFor(AchievementTier.GRANDMASTER, ALL));
	}

	@Test
	public void currentTierTracksThresholds()
	{
		assertNull(TierMath.currentTierFor(0, ALL));
		assertNull(TierMath.currentTierFor(40, ALL));
		assertEquals(AchievementTier.EASY, TierMath.currentTierFor(41, ALL));
		assertEquals(AchievementTier.EASY, TierMath.currentTierFor(160, ALL));
		assertEquals(AchievementTier.MEDIUM, TierMath.currentTierFor(161, ALL));
		assertEquals(AchievementTier.MASTER, TierMath.currentTierFor(1904, ALL));
		assertEquals(AchievementTier.GRANDMASTER, TierMath.currentTierFor(2630, ALL));
		assertEquals(AchievementTier.GRANDMASTER, TierMath.currentTierFor(9999, ALL));
	}

	@Test
	public void gapToNextTierReportsShortfall()
	{
		TierMath.TierGap gap = TierMath.gapToNextTier(0, ALL);
		assertEquals(AchievementTier.EASY, gap.nextTier());
		assertEquals(41, gap.pointsNeeded());

		TierMath.TierGap gap2 = TierMath.gapToNextTier(160, ALL);
		assertEquals(AchievementTier.MEDIUM, gap2.nextTier());
		assertEquals(1, gap2.pointsNeeded());

		assertNull("fully unlocked has no next tier", TierMath.gapToNextTier(2630, ALL));
	}

	@Test
	public void progressByTierCountsCompletion()
	{
		// Complete every Easy task; nothing else.
		Set<Integer> completed = new HashSet<>();
		int easyPoints = 0;
		for (CombatAchievement task : ALL)
		{
			if (task.tier() == AchievementTier.EASY)
			{
				completed.add(task.id());
				easyPoints += task.points();
			}
		}

		List<TierProgress> rows = TierMath.progressByTier(completed, ALL, easyPoints);
		TierProgress easy = rows.get(0);
		assertEquals(AchievementTier.EASY, easy.tier());
		assertEquals(41, easy.completedCount());
		assertEquals(41, easy.totalCount());
		assertEquals(true, easy.unlocked());
		assertEquals(true, easy.fullyComplete());

		TierProgress medium = rows.get(1);
		assertEquals(0, medium.completedCount());
		assertEquals(false, medium.unlocked());
		assertEquals(161 - easyPoints, medium.pointsRemainingToUnlock());
	}

	@Test
	public void emptyProgressUnlocksNothing()
	{
		List<TierProgress> rows = TierMath.progressByTier(Collections.emptySet(), ALL, 0);
		for (TierProgress row : rows)
		{
			assertEquals(false, row.unlocked());
			assertEquals(0, row.completedCount());
		}
	}
}
