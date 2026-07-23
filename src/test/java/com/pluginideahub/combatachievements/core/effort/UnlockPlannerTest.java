package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnlockPlannerTest
{
	private static ByteArrayInputStream s(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	private static CombatAchievement task(int id, int points)
	{
		return new CombatAchievement(id, "T" + id, AchievementTier.ELITE, "Boss",
			TaskType.KILL_COUNT, points, "Kill Boss.", "", "");
	}

	// Tasks 1 & 2 gated by Dragon Slayer II; task 3 gated by Regicide.
	private static EffortDataLibrary effort()
	{
		return EffortDataLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"questReqs\":[\"Dragon Slayer II\"]},"
			+ "\"2\":{\"questReqs\":[\"Dragon Slayer II\"]},"
			+ "\"3\":{\"questReqs\":[\"Regicide\"]}}}"));
	}

	private static QuestEffortLibrary quests()
	{
		return QuestEffortLibrary.load(s("{\"quests\":{"
			+ "\"Dragon Slayer II\":{\"name\":\"Dragon Slayer II\",\"difficulty\":\"Grandmaster\","
			+ "\"estMinutes\":75,\"skillRequirements\":{\"Magic\":75},\"directPrerequisiteQuests\":[\"Legends' Quest\"]},"
			+ "\"Legends' Quest\":{\"name\":\"Legends' Quest\",\"estMinutes\":60,\"directPrerequisiteQuests\":[]},"
			+ "\"Regicide\":{\"name\":\"Regicide\",\"difficulty\":\"Experienced\",\"estMinutes\":40,\"directPrerequisiteQuests\":[]}"
			+ "}}"));
	}

	private static SkillXpLibrary skills()
	{
		return SkillXpLibrary.load(s("{\"skills\":{\"Magic\":{\"skill\":\"Magic\",\"brackets\":["
			+ "{\"fromLevel\":1,\"toLevel\":99,\"method\":\"x\",\"xpPerHour\":250000,\"members\":true}]}}}"));
	}

	private static PlayerProfile player(int magic, Set<String> doneQuests)
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Magic", magic);
		return PlayerProfile.of(levels, doneQuests, doneQuests);
	}

	@Test
	public void ranksQuestsByUnlockedPointsPerEffort()
	{
		List<CombatAchievement> tasks = Arrays.asList(task(1, 4), task(2, 4), task(3, 4));
		UnlockPlanner planner = new UnlockPlanner(quests(), skills());
		List<UnlockSuggestion> out = planner.plan(tasks, Collections.emptySet(), effort(),
			player(99, Collections.emptySet()));

		assertEquals(2, out.size());
		UnlockSuggestion ds2 = out.stream().filter(u -> u.questName().equals("Dragon Slayer II")).findFirst().get();
		UnlockSuggestion reg = out.stream().filter(u -> u.questName().equals("Regicide")).findFirst().get();

		assertEquals("DS2 unlocks both gated tasks", 2, ds2.unlockedTaskCount());
		assertEquals(8, ds2.unlockedPoints());
		assertTrue("DS2 chain includes its prerequisite", ds2.remainingPrerequisites().contains("Legends' Quest"));
		assertEquals(1, reg.unlockedTaskCount());

		// Regicide (40 min, no training, 4 pts) beats DS2 (75+60 min chain, 4pts/each but heavier) per minute.
		assertTrue("cheaper-per-point quest ranks first", out.get(0).score() >= out.get(1).score());
	}

	@Test
	public void difficultyWeightingRanksEasyUnlockAboveEqualCostHardUnlock()
	{
		// Two quests with identical time cost (40 min, no prereqs/training), each unlocking one 4-pt task:
		// one easy (difficulty 1), one brutal (difficulty 10). Difficulty weighting must rank the quest
		// that opens easy points first, while the raw unlockedPoints prize stays unchanged.
		EffortDataLibrary el = EffortDataLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"questReqs\":[\"Easy Unlock\"]},"
			+ "\"2\":{\"questReqs\":[\"Hard Unlock\"]}}}"));
		QuestEffortLibrary q = QuestEffortLibrary.load(s("{\"quests\":{"
			+ "\"Easy Unlock\":{\"name\":\"Easy Unlock\",\"estMinutes\":40,\"directPrerequisiteQuests\":[]},"
			+ "\"Hard Unlock\":{\"name\":\"Hard Unlock\",\"estMinutes\":40,\"directPrerequisiteQuests\":[]}}}"));
		TaskDifficultyLibrary diffs = TaskDifficultyLibrary.load(
			s("{\"tasks\":{\"1\":{\"difficulty\":1},\"2\":{\"difficulty\":10}}}"));

		List<UnlockSuggestion> out = new UnlockPlanner(q, skills()).plan(
			Arrays.asList(task(1, 4), task(2, 4)), Collections.emptySet(), el,
			player(99, Collections.emptySet()), diffs);

		assertEquals(2, out.size());
		assertEquals("unlocking easy points ranks first", "Easy Unlock", out.get(0).questName());
		UnlockSuggestion easy = out.get(0);
		assertEquals("raw prize is unchanged for display", 4, easy.unlockedPoints());
		assertEquals("easy points count at near-full weight", 4, easy.achievablePoints());
		UnlockSuggestion hard = out.stream().filter(u -> u.questName().equals("Hard Unlock")).findFirst().get();
		assertTrue("hard points are discounted", hard.achievablePoints() < hard.unlockedPoints());
	}

	@Test
	public void chargesSkillTrainingWhenLevelShort()
	{
		List<CombatAchievement> tasks = Arrays.asList(task(1, 4));
		UnlockPlanner planner = new UnlockPlanner(quests(), skills());
		// Player has Magic 60, DS2 needs 75 -> training time is added and the skill is flagged.
		UnlockSuggestion ds2 = planner.plan(tasks, Collections.emptySet(), effort(),
			player(60, Collections.emptySet())).get(0);
		assertTrue("training time charged", ds2.trainingMinutes() > 0);
		assertTrue(ds2.unmetSkills().stream().anyMatch(x -> x.startsWith("Magic")));
	}

	@Test
	public void startedSufficesQuestAlreadyStartedIsNotAnUnlock()
	{
		// Task 1 gated by a "started suffices" quest the player has STARTED (not completed): the CA is
		// already doable, so completing the quest unlocks nothing — it must not be a suggestion (else it
		// would render as a red "needs Regicide" locked Route card for an already-doable CA).
		EffortDataLibrary el = EffortDataLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"questReqs\":[{\"name\":\"Regicide\",\"startedSuffices\":true}]}}}"));
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Magic", 99);
		PlayerProfile p = PlayerProfile.of(levels, Collections.emptySet(),
			new HashSet<>(Collections.singletonList("Regicide")));

		List<UnlockSuggestion> out = new UnlockPlanner(quests(), skills())
			.plan(Arrays.asList(task(1, 4)), Collections.emptySet(), el, p);

		assertTrue("a merely-started started-suffices quest is not a pending unlock", out.isEmpty());
	}

	@Test
	public void completedQuestProducesNoSuggestion()
	{
		List<CombatAchievement> tasks = Arrays.asList(task(3, 4));
		Set<String> done = new HashSet<>(Collections.singletonList("Regicide"));
		List<UnlockSuggestion> out = new UnlockPlanner(quests(), skills())
			.plan(tasks, Collections.emptySet(), effort(), player(99, done));
		assertTrue("a finished quest is not an unlock suggestion", out.isEmpty());
	}

	@Test
	public void taskWithUnmetSkillsIsNotCountedAsPureUnlock()
	{
		// Task 1 also needs a skill the player lacks -> completing the quest alone wouldn't unlock it.
		EffortDataLibrary el = EffortDataLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"questReqs\":[\"Dragon Slayer II\"],\"levelReqs\":{\"Slayer\":95}}}}"));
		List<UnlockSuggestion> out = new UnlockPlanner(quests(), skills())
			.plan(Arrays.asList(task(1, 4)), Collections.emptySet(), el, player(99, Collections.emptySet()));
		assertTrue(out.isEmpty());
	}
}
