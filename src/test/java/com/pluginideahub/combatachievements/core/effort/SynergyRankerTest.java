package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SynergyRankerTest
{
	// Distinct execution tasks (each its own attempts — additive within a boss, unlike nested KC).
	private static CombatAchievement task(int id, String monster, int points)
	{
		return new CombatAchievement(id, "T" + id, AchievementTier.HARD, monster,
			TaskType.RESTRICTION, points, "Restriction challenge " + id + ".", "", "");
	}

	private static CombatAchievement kc(int id, String monster, int killThreshold)
	{
		return new CombatAchievement(id, "KC" + id, AchievementTier.HARD, monster,
			TaskType.KILL_COUNT, 3, "Kill " + monster + " " + killThreshold + " times.", "", "");
	}

	// Both bosses kill in 60s; a CA = 1 kill = ~1 min each.
	private static BossTimingLibrary timing()
	{
		return BossTimingLibrary.load(new ByteArrayInputStream((
			"{\"monsters\":{"
				+ "\"Dense Boss\":{\"ttkSeconds\":54,\"respawnSeconds\":6,\"killsPerHour\":60},"
				+ "\"Lone Boss\":{\"ttkSeconds\":54,\"respawnSeconds\":6,\"killsPerHour\":60}}}")
			.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void denseBossBundleOutranksScatteredSingle()
	{
		// Dense Boss has 4 doable CAs (4 pts total); Lone Boss has 1 CA (2 pts). With a shared trip
		// overhead, the 4-CA boss is the better next session despite each CA being individually small.
		List<CombatAchievement> tasks = new ArrayList<>();
		for (int i = 0; i < 4; i++)
		{
			tasks.add(task(i, "Dense Boss", 1));
		}
		tasks.add(task(99, "Lone Boss", 2));

		List<BossSession> sessions = SynergyRanker.standard().rank(tasks, timing(), CombatExperience.empty(), com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary.empty());
		assertEquals("Dense Boss", sessions.get(0).monster());
		assertEquals(4, sessions.get(0).taskCount());
		assertEquals(4, sessions.get(0).totalPoints());
		assertTrue(sessions.get(0).score() > sessions.get(1).score());
	}

	@Test
	public void pointsPerHourIsScorePerMinuteScaledToAnHour()
	{
		// pts/hr is just the per-minute score × 60 — the same ranking, expressed in the units the
		// "Next session" tab and the curation spreadsheet show.
		List<CombatAchievement> tasks = Arrays.asList(kc(1, "Dense Boss", 60));
		BossSession s = new SynergyRanker(TaskTimeModel.standard(), 0)
			.rank(tasks, timing(), CombatExperience.empty(), TaskDifficultyLibrary.empty()).get(0);

		assertEquals(s.score() * 60.0, s.pointsPerHour(), 1e-9);
		// 3 pts over a 60-min grind (60s/kill × 60 kills, 0 overhead) ⇒ 3 pts/hr.
		assertEquals(3.0, s.pointsPerHour(), 1e-9);
	}

	@Test
	public void stackedKillCountChargesIncrementalMilestones()
	{
		// Three nested Kill Count CAs at one boss (25 / 75 / 150), zero KC, 60s/kill. Reaching 150 also
		// clears 25 and 75, so the rungs cost the INCREMENTAL kills (25, +50, +75 = 150 min total) — not
		// 25 + 75 + 150 = 250 — yet the lowest rung keeps a real, short time ("just do 25").
		List<CombatAchievement> tasks = Arrays.asList(
			kc(1, "Dense Boss", 25), kc(2, "Dense Boss", 75), kc(3, "Dense Boss", 150));
		TaskDifficultyLibrary none = TaskDifficultyLibrary.empty();

		BossSession s = new SynergyRanker(TaskTimeModel.standard(), 0)
			.rank(tasks, timing(), CombatExperience.empty(), none).get(0);

		assertEquals("collapsed to one 150-kill grind, not three", 150, s.totalMinutes());

		int sum = 0;
		int lowestRungMinutes = -1;
		int lowestThreshold = Integer.MAX_VALUE;
		for (BossSession.Item it : s.items())
		{
			sum += it.sessionMinutes();
			if (it.estimate().requiredKills() < lowestThreshold)
			{
				lowestThreshold = it.estimate().requiredKills();
				lowestRungMinutes = it.sessionMinutes();
			}
		}
		assertEquals("rungs sum to the single grind", 150, sum);
		assertEquals("the 25-kill rung is a real short option (25 min), not zero", 25, lowestRungMinutes);
	}

	@Test
	public void overheadDialControlsClustering()
	{
		// Dense Boss: 3 CAs (3 pts total, 3 min); Lone Boss: 1 CA (2 pts, 1 min, better points/min).
		List<CombatAchievement> tasks = new ArrayList<>();
		tasks.add(task(1, "Dense Boss", 1));
		tasks.add(task(2, "Dense Boss", 1));
		tasks.add(task(4, "Dense Boss", 1));
		tasks.add(task(3, "Lone Boss", 2));

		// With near-zero overhead, the single 2-pt CA (better points/min) leads.
		List<BossSession> lowO = new SynergyRanker(TaskTimeModel.standard(), 0)
			.rank(tasks, timing(), CombatExperience.empty(), com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary.empty());
		assertEquals("Lone Boss", lowO.get(0).monster());

		// With a big overhead, bundling the two Dense CAs wins.
		List<BossSession> highO = new SynergyRanker(TaskTimeModel.standard(), 30)
			.rank(tasks, timing(), CombatExperience.empty(), com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary.empty());
		assertEquals("Dense Boss", highO.get(0).monster());
	}

	@Test
	public void higherDifficultyExecutionTaskCostsMoreSessionMinutes()
	{
		// Two execution CAs at one boss; the higher-Difficulty one needs more attempts, so its session
		// contribution (minutes) is larger — proving Difficulty flows through the ranker into the estimate.
		TaskDifficultyLibrary diffs = TaskDifficultyLibrary.load(new ByteArrayInputStream(
			"{\"tasks\":{\"1\":{\"difficulty\":10},\"2\":{\"difficulty\":2}}}".getBytes(StandardCharsets.UTF_8)));

		BossSession s = new SynergyRanker(TaskTimeModel.standard(), 0)
			.rank(Arrays.asList(task(1, "Dense Boss", 3), task(2, "Dense Boss", 3)),
				timing(), CombatExperience.empty(), diffs).get(0);

		int hardMin = itemMinutes(s, 1);
		int easyMin = itemMinutes(s, 2);
		assertTrue("harder task costs more session minutes (" + hardMin + " vs " + easyMin + ")",
			hardMin > easyMin);
	}

	private static int itemMinutes(BossSession s, int taskId)
	{
		for (BossSession.Item it : s.items())
		{
			if (it.task().id() == taskId)
			{
				return it.sessionMinutes();
			}
		}
		return -1;
	}
}
