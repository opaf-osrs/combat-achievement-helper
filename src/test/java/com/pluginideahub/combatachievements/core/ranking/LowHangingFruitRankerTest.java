package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LowHangingFruitRankerTest
{
	// Task 1: Hard Kill Count, 3 pts. Task 2: Master Perfection, 5 pts.
	private static final CombatAchievement HARD_KC = new CombatAchievement(
		1, "Abyssal Adept", AchievementTier.HARD, "Abyssal Sire", TaskType.KILL_COUNT, 3,
		"Kill the Abyssal Sire 20 times.", "General", "url");
	private static final CombatAchievement MASTER_PERFECTION = new CombatAchievement(
		2, "Flawless", AchievementTier.MASTER, "Some Boss", TaskType.PERFECTION, 5,
		"Do it perfectly.", "General", "url");

	private static EffortDataLibrary effortLib()
	{
		String json = "{\"tasks\":{"
			+ "\"1\":{\"gearTier\":\"mid\",\"rng\":\"low\",\"supply\":\"none\",\"soloable\":true},"
			+ "\"2\":{\"gearTier\":\"high\",\"rng\":\"high\",\"supply\":\"none\",\"soloable\":true}"
			+ "}}";
		return EffortDataLibrary.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}

	private static SignalsProvider signals()
	{
		return taskId ->
		{
			if (taskId == 1)
			{
				// boss already farmed (KC>0): no learning penalty
				return new TaskLiveSignals(true, true, false, true);
			}
			// never-killed boss: learning penalty applies
			return new TaskLiveSignals(true, true, false, false);
		};
	}

	@Test
	public void hardKillCountOutranksMasterPerfection()
	{
		LowHangingFruitRanker ranker = new LowHangingFruitRanker(effortLib(), EffortModel.standard());
		List<RankedTask> ranked = ranker.rank(
			Arrays.asList(HARD_KC, MASTER_PERFECTION), Collections.emptySet(), signals(), false);

		assertEquals(2, ranked.size());
		assertEquals("Hard KC ranks first", 1, ranked.get(0).achievement().id());
		assertEquals(2, ranked.get(1).achievement().id());

		// Matches the worked example in docs/DESIGN.md §6b.1.
		assertEquals(1.45, ranked.get(0).effort(), 1e-6);
		assertEquals(3.0 / 1.45, ranked.get(0).score(), 1e-6);
		assertEquals(12.4, ranked.get(1).effort(), 1e-6);
		assertEquals(5.0 / 12.4, ranked.get(1).score(), 1e-6);
		assertTrue(ranked.get(0).score() > ranked.get(1).score());
	}

	@Test
	public void completedTasksAreExcluded()
	{
		LowHangingFruitRanker ranker = new LowHangingFruitRanker(effortLib(), EffortModel.standard());
		List<RankedTask> ranked = ranker.rank(
			Arrays.asList(HARD_KC, MASTER_PERFECTION), Collections.singleton(1), signals(), false);
		assertEquals(1, ranked.size());
		assertEquals(2, ranked.get(0).achievement().id());
	}

	@Test
	public void doableNowFilterDropsLockedTasks()
	{
		// Task 2's access is not met → not doable now.
		SignalsProvider gated = taskId -> taskId == 1
			? new TaskLiveSignals(true, true, false, true)
			: new TaskLiveSignals(false, true, false, false);

		LowHangingFruitRanker ranker = new LowHangingFruitRanker(effortLib(), EffortModel.standard());
		List<RankedTask> doable = ranker.rank(
			Arrays.asList(HARD_KC, MASTER_PERFECTION), Collections.emptySet(), gated, true);
		assertEquals(1, doable.size());
		assertEquals(1, doable.get(0).achievement().id());

		// Without the filter the locked task still appears, flagged not-doable.
		List<RankedTask> all = ranker.rank(
			Arrays.asList(HARD_KC, MASTER_PERFECTION), Collections.emptySet(), gated, false);
		assertEquals(2, all.size());
	}

	@Test
	public void questGatedTaskIsExcludedFromRouteUntilQuestDone()
	{
		// Full stack: real ProfileSignalsProvider resolves the quest gate, ranker reflects it.
		EffortDataLibrary gatedLib = EffortDataLibrary.load(
			stream("{\"tasks\":{\"1\":{\"questReqs\":[\"Dragon Slayer II\"]}}}"));
		CombatAchievement vorkath = new CombatAchievement(1, "Vorkath Veteran", AchievementTier.ELITE,
			"Vorkath", TaskType.KILL_COUNT, 4, "Kill Vorkath.", "", "");

		// Without Dragon Slayer II the task is dropped from the doable-now route...
		ProfileSignalsProvider noQuest = new ProfileSignalsProvider(gatedLib, PlayerProfile.empty());
		LowHangingFruitRanker ranker = new LowHangingFruitRanker(gatedLib, EffortModel.standard());
		assertTrue(ranker.rank(Collections.singletonList(vorkath), Collections.emptySet(), noQuest, true)
			.isEmpty());

		// ...and is shown locked with an actionable reason when the filter is off.
		RankedTask locked = ranker.rank(
			Collections.singletonList(vorkath), Collections.emptySet(), noQuest, false).get(0);
		assertFalse(locked.doableNow());
		assertEquals("needs Dragon Slayer II", locked.lockReason());

		// With the quest finished, the task becomes doable.
		Set<String> done = new HashSet<>(Collections.singletonList("Dragon Slayer II"));
		ProfileSignalsProvider withQuest = new ProfileSignalsProvider(gatedLib,
			PlayerProfile.of(new HashMap<>(), done, done));
		List<RankedTask> nowDoable = ranker.rank(
			Collections.singletonList(vorkath), Collections.emptySet(), withQuest, true);
		assertEquals(1, nowDoable.size());
		assertTrue(nowDoable.get(0).doableNow());
		assertEquals("", nowDoable.get(0).lockReason());
	}

	@Test
	public void hardTasksSinkBelowEasyOnesViaDifficulty()
	{
		// An easy 2-pt task vs a hard 6-pt raid task, both equally cheap by the synthetic effort model.
		// Difficulty (pure skill) — not player completion % — must decide the ordering, so the popular-
		// but-hard raid no longer reads as easy.
		CombatAchievement easy = new CombatAchievement(10, "Easy", AchievementTier.MEDIUM, "Boss",
			TaskType.KILL_COUNT, 2, "", "", "");
		CombatAchievement hard = new CombatAchievement(11, "Hard", AchievementTier.GRANDMASTER, "Raid",
			TaskType.KILL_COUNT, 6, "", "", "");
		EffortDataLibrary el = EffortDataLibrary.load(stream("{\"tasks\":{}}"));
		TaskDifficultyLibrary diff = TaskDifficultyLibrary.load(
			stream("{\"tasks\":{\"10\":{\"difficulty\":2},\"11\":{\"difficulty\":9}}}"));
		SignalsProvider sig = id -> new TaskLiveSignals(true, true, false, true);

		// Without difficulty data the 6-pt task wins on raw points/effort...
		List<RankedTask> naive = new LowHangingFruitRanker(el, EffortModel.standard())
			.rank(Arrays.asList(hard, easy), Collections.emptySet(), sig, false);
		assertEquals(11, naive.get(0).achievement().id());

		// ...with the pure-skill difficulty factor, the easy task correctly ranks first.
		List<RankedTask> withDifficulty = new LowHangingFruitRanker(el, EffortModel.standard(), diff)
			.rank(Arrays.asList(hard, easy), Collections.emptySet(), sig, false);
		assertEquals(10, withDifficulty.get(0).achievement().id());
		// Each ranked task now carries its difficulty for card display / the Easiest sort.
		assertEquals(9, withDifficulty.get(1).difficulty().difficulty());
	}

	@Test
	public void belowRecStatsTaskIsSunkBelowAnOtherwiseEqualTask()
	{
		// Two identical tasks (same points, same neutral effort, no difficulty data). The only difference:
		// task 21's player is 40 summed levels below its soft recommended stats, so it must rank lower.
		CombatAchievement metStats = new CombatAchievement(20, "Ready", AchievementTier.HARD, "Boss",
			TaskType.KILL_COUNT, 3, "", "", "");
		CombatAchievement underLevelled = new CombatAchievement(21, "Underlevelled", AchievementTier.HARD,
			"Boss", TaskType.KILL_COUNT, 3, "", "", "");
		EffortDataLibrary el = EffortDataLibrary.load(stream("{\"tasks\":{}}"));
		SignalsProvider sig = id -> id == 20
			? new TaskLiveSignals(true, true, false, true, 0)
			: new TaskLiveSignals(true, true, false, true, 40);

		List<RankedTask> ranked = new LowHangingFruitRanker(el, EffortModel.standard())
			.rank(Arrays.asList(underLevelled, metStats), Collections.emptySet(), sig, false);

		assertEquals("stats-met task ranks first", 20, ranked.get(0).achievement().id());
		assertEquals(21, ranked.get(1).achievement().id());
		assertTrue(ranked.get(1).belowRecStats());
		assertEquals(40, ranked.get(1).recStatsShortfall());
		// The penalty is SQUARED distance: at the 40-level scale, 40 short -> ×(1 + 1²) = ×2.0 effort.
		// Squaring is what stops a high-point task far out of reach from still looking like good value.
		assertEquals(2.0 * ranked.get(0).effort(), ranked.get(1).effort(), 1e-6);
		assertEquals(ranked.get(0).score() / 2.0, ranked.get(1).score(), 1e-6);
	}

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
