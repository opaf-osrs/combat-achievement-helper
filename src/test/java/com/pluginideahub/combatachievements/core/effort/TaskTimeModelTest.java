package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskTimeModelTest
{
	private static final BossTiming SIXTY = new BossTiming(50, 10, 60, ""); // 60s per kill
	private static final BossTiming SIXTY_DEADLY = new BossTiming(50, 10, 60, "", 10.0); // 10 attempts/kill

	private static CombatAchievement kc(int id, int points, String desc)
	{
		return new CombatAchievement(id, "T" + id, AchievementTier.HARD, "Vorkath",
			TaskType.KILL_COUNT, points, desc, "", "");
	}

	private static CombatAchievement perfection()
	{
		return new CombatAchievement(9, "Perfect", AchievementTier.MASTER, "Vorkath",
			TaskType.PERFECTION, 5, "Kill Vorkath without taking damage.", "", "");
	}

	private static CombatExperience withKc(int vorkath)
	{
		Map<String, Integer> m = new HashMap<>();
		m.put("Vorkath", vorkath);
		return CombatExperience.of(m);
	}

	@Test
	public void parsesKillCountFromWording()
	{
		assertEquals(100, TaskTimeModel.requiredKills("Kill Vorkath 100 times."));
		assertEquals(6, TaskTimeModel.requiredKills("Kill 6 Giant Rats within the lair."));
		assertEquals(1, TaskTimeModel.requiredKills("Kill an Aberrant Spectre."));
		assertEquals("count at end of string", 6, TaskTimeModel.requiredKills("Kill 6"));
		assertEquals("uppercase still parses (locale-safe)", 50, TaskTimeModel.requiredKills("KILL VORKATH 50 TIMES"));
		assertEquals("overflow falls back to 1, not a crash", 1,
			TaskTimeModel.requiredKills("Kill it 99999999999 times"));
	}

	@Test
	public void nonKillCountTaskDoesNotSubtractKc()
	{
		// A counted Mechanical task ("kill 6 ... in 3s") must NOT treat boss KC as completions.
		CombatAchievement mech = new CombatAchievement(20, "Efficient", AchievementTier.MEDIUM, "Vorkath",
			TaskType.MECHANICAL, 2, "Kill 6 things within 3 seconds.", "", "");
		TaskTimeModel.Estimate est = TaskTimeModel.standard().estimate(mech, SIXTY, withKc(500));
		assertTrue("KC is not subtracted for execution tasks", est.minutes() > 0);
		assertFalse(est.hasPartialProgress());
	}

	@Test
	public void killCountUsesRemainingAfterPartialProgress()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		// "Kill Vorkath 50 times", already 30 KC -> 20 left x 60s = 1200s = 20 min.
		TaskTimeModel.Estimate est = model.estimate(kc(1, 4, "Kill Vorkath 50 times."), SIXTY, withKc(30));
		assertEquals(50, est.requiredKills());
		assertEquals(30, est.currentKc());
		assertEquals(20, est.remainingKills());
		assertEquals(20, est.minutes());
		assertTrue(est.hasPartialProgress());
	}

	@Test
	public void completedGrindCostsNothing()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		TaskTimeModel.Estimate est = model.estimate(kc(1, 4, "Kill Vorkath 50 times."), SIXTY, withKc(60));
		assertEquals(0, est.remainingKills());
		assertEquals(0, est.minutes());
	}

	@Test
	public void hardMechanicCostsMoreAttemptsThanEasyRestriction()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		// Both Restriction, both novice. The high-Difficulty coordination task should cost materially
		// more attempts than a low-Difficulty one — the curated pure-skill Difficulty (which already
		// folds in the "within 3 seconds" bump), not completion %, now drives the ability rating.
		CombatAchievement coord = new CombatAchievement(30, "Three in Three", AchievementTier.HARD, "Vorkath",
			TaskType.RESTRICTION, 5, "Kill three within 3 seconds.", "", "");
		CombatAchievement easy = new CombatAchievement(31, "Stab It", AchievementTier.HARD, "Vorkath",
			TaskType.RESTRICTION, 4, "Kill it with a stab weapon.", "", "");

		int coordMin = model.estimate(coord, SIXTY, withKc(0), 10).minutes();   // difficulty 10 → rating 5
		int easyMin = model.estimate(easy, SIXTY, withKc(0), 3).minutes();      // difficulty 3 → rating 2
		assertTrue("the hard coordination task should estimate longer (" + coordMin + " vs " + easyMin + ")",
			coordMin > easyMin + 2);
		assertEquals(5, model.estimate(coord, SIXTY, withKc(0), 10).abilityRating());
		assertTrue(model.estimate(easy, SIXTY, withKc(0), 3).abilityRating() <= 2);
	}

	@Test
	public void competenceStillDiscountsTheHardTask()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		CombatAchievement coord = new CombatAchievement(30, "Three in Three", AchievementTier.HARD, "Vorkath",
			TaskType.RESTRICTION, 5, "Kill three within 3 seconds.", "", "");
		int novice = model.estimate(coord, SIXTY, withKc(0), 10).minutes();
		int veteran = model.estimate(coord, SIXTY, withKc(500), 10).minutes();
		assertTrue("a veteran still clears the hard task far quicker", veteran < novice);
	}

	@Test
	public void competenceReducesExecutionTaskTime()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		int novice = model.estimate(perfection(), SIXTY, withKc(0)).minutes();      // more attempts
		int veteran = model.estimate(perfection(), SIXTY, withKc(500)).minutes();   // competence discount
		assertTrue("a veteran needs fewer attempts at a perfection task", veteran < novice);
		assertEquals(CombatExperience.Competence.VETERAN,
			model.estimate(perfection(), SIXTY, withKc(500)).competence());
	}

	@Test
	public void abilityRatingMapsDifficultyToFivePointScale()
	{
		// Two difficulty points per rung; mid difficulty (5–6) is the neutral rating 3.
		assertEquals(1, TaskTimeModel.abilityRating(1));
		assertEquals(1, TaskTimeModel.abilityRating(2));
		assertEquals(2, TaskTimeModel.abilityRating(3));
		assertEquals(3, TaskTimeModel.abilityRating(5));
		assertEquals(3, TaskTimeModel.abilityRating(6));
		assertEquals(4, TaskTimeModel.abilityRating(7));
		assertEquals(5, TaskTimeModel.abilityRating(10));
		// Out-of-range is clamped, never throwing.
		assertEquals(1, TaskTimeModel.abilityRating(0));
		assertEquals(5, TaskTimeModel.abilityRating(99));
	}

	@Test
	public void deadlinessScalesSingleKillTimeForNovice()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		CombatAchievement task = kc(1, 4, "Kill Vorkath.");   // one kill, 60s each
		int normal = model.estimate(task, SIXTY, withKc(0)).minutes();          // deadliness 1 -> 1 min
		int deadly = model.estimate(task, SIXTY_DEADLY, withKc(0)).minutes();   // deadliness 10 -> 10 min
		assertEquals("a normal single kill stays one kill", 1, normal);
		assertEquals("a deadly boss charges ~10 attempts for the same kill", 10, deadly);
	}

	@Test
	public void deadlinessOfOneMatchesTheOldEstimate()
	{
		// The whole model is unchanged for every non-deadly boss (attempts/kill defaults to 1).
		TaskTimeModel model = TaskTimeModel.standard();
		assertEquals(20, model.estimate(kc(1, 4, "Kill Vorkath 50 times."), SIXTY, withKc(30)).minutes());
	}

	@Test
	public void perCaOverrideReplacesBossDeadliness()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		CombatAchievement task = kc(1, 4, "Kill Vorkath.");
		// Override 3 wins over the boss's 10 -> 3 attempts -> 3 min.
		int overridden = model.estimate(task, SIXTY_DEADLY, withKc(0), 1, 3.0).minutes();
		assertEquals(3, overridden);
	}

	@Test
	public void competenceDiscountsTheDeadlinessAttempts()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		CombatAchievement task = kc(1, 4, "Kill Vorkath.");
		int novice = model.estimate(task, SIXTY_DEADLY, withKc(0)).minutes();     // 1 + 9*1.0  = 10 min
		int veteran = model.estimate(task, SIXTY_DEADLY, withKc(500)).minutes();  // 1 + 9*0.35 ≈ 5 min
		assertTrue("a veteran dies far less at a deadly boss (" + veteran + " < " + novice + ")",
			veteran < novice);
	}

	@Test
	public void unknownTimingFallsBackNotZero()
	{
		TaskTimeModel model = TaskTimeModel.standard();
		TaskTimeModel.Estimate est = model.estimate(
			kc(1, 4, "Kill Vorkath 10 times."), BossTiming.UNKNOWN, CombatExperience.empty());
		assertTrue("fallback keeps a non-zero estimate", est.minutes() > 0);
	}
}
