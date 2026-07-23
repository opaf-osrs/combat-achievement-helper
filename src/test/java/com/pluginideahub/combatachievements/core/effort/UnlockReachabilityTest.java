package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * "Unlock next" is ranked on the part of the prize you could actually collect. A quest opening 15 CAs
 * you are 40 levels short of has not opened 15 CAs — before this it was ranked and displayed on the raw
 * total, so a level-3 was offered The Final Dawn (a 25-hour questline) for content it could never do.
 */
public class UnlockReachabilityTest
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

	/** Task 1 gated by a short quest, task 2 by a long one. */
	private static EffortDataLibrary effort()
	{
		return EffortDataLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"questReqs\":[\"Short Quest\"]},"
			+ "\"2\":{\"questReqs\":[\"Long Quest\"]}}}"));
	}

	private static QuestEffortLibrary quests()
	{
		return QuestEffortLibrary.load(s("{\"quests\":{"
			+ "\"Short Quest\":{\"name\":\"Short Quest\",\"difficulty\":\"Novice\",\"estMinutes\":10,"
			+ "\"directPrerequisiteQuests\":[]},"
			+ "\"Long Quest\":{\"name\":\"Long Quest\",\"difficulty\":\"Master\",\"estMinutes\":600,"
			+ "\"directPrerequisiteQuests\":[]},"
			+ "\"Training Quest\":{\"name\":\"Training Quest\",\"difficulty\":\"Novice\",\"estMinutes\":10,"
			+ "\"skillRequirements\":{\"Attack\":80},\"directPrerequisiteQuests\":[]}"
			+ "}}"));
	}

	private static SkillXpLibrary skills()
	{
		return SkillXpLibrary.load(s("{\"skills\":{\"Attack\":{\"skill\":\"Attack\",\"brackets\":["
			+ "{\"fromLevel\":1,\"toLevel\":99,\"method\":\"x\",\"xpPerHour\":250000,\"members\":true}]}}}"));
	}

	/** Both tasks softly want 80 Attack. */
	private static RecStatsLibrary recStats()
	{
		return RecStatsLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"soft\":[{\"skills\":[\"Attack\"],\"level\":80}]},"
			+ "\"2\":{\"soft\":[{\"skills\":[\"Attack\"],\"level\":80}]}}}"));
	}

	private static PlayerProfile player(int attack)
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", attack);
		return PlayerProfile.of(levels, Collections.emptySet(), Collections.emptySet());
	}

	private static List<UnlockSuggestion> plan(PlayerProfile profile, RecStatsLibrary rec,
		EffortDataLibrary effortLib)
	{
		return new UnlockPlanner(quests(), skills()).plan(
			Arrays.asList(task(1, 4), task(2, 40)), Collections.emptySet(), effortLib, profile,
			TaskDifficultyLibrary.empty(), 1.0, 1.0, rec);
	}

	@Test
	public void questsOpeningNothingReachableAreStillRankedSensiblyAmongThemselves()
	{
		// Attack 10 against tasks wanting 80 — a 70-level gap, far past the ready line. The suggestions are
		// kept, because a quest is permanent progress worth doing before you can use what it opens. They
		// must still order by value, though: scoring them all at a flat zero made the sort fall through to
		// alphabetical, which put the 600-minute quest above the 10-minute one.
		List<UnlockSuggestion> out = plan(player(10), recStats(), effort());
		assertEquals(2, out.size());
		for (UnlockSuggestion u : out)
		{
			assertEquals("nothing reachable at " + u.questName(), 0, u.reachableTaskCount());
			assertEquals(0, u.reachablePoints());
		}
		assertEquals("the 10-minute quest leads the 600-minute one", "Short Quest", out.get(0).questName());
	}

	@Test
	public void aReadyAccountStillGetsBothSuggestions()
	{
		List<UnlockSuggestion> out = plan(player(80), recStats(), effort());
		assertEquals(2, out.size());
		assertEquals("the big prize ranks on reachable points, and it is all reachable",
			"Long Quest", find(out, "Long Quest").questName());
		assertEquals(40, find(out, "Long Quest").reachablePoints());
	}

	@Test
	public void readinessIsJudgedAfterTheQuestsOwnTraining()
	{
		// A quest that requires Attack 80 leaves you AT 80 — so the CAs it opens, which want 80, are
		// reachable. Judging at today's levels would wrongly drop it.
		EffortDataLibrary gated = EffortDataLibrary.load(s(
			"{\"tasks\":{\"1\":{\"questReqs\":[\"Training Quest\"]}}}"));
		List<UnlockSuggestion> out = new UnlockPlanner(quests(), skills()).plan(
			Arrays.asList(task(1, 4)), Collections.emptySet(), gated, player(1),
			TaskDifficultyLibrary.empty(), 1.0, 1.0, recStats());

		assertEquals("the quest trains you into its own reward", 1, out.size());
		assertEquals(1, out.get(0).reachableTaskCount());
		assertTrue("and the training is still charged", out.get(0).trainingMinutes() > 0);
	}

	@Test
	public void theRawPrizeIsKeptSeparateFromTheReachableOne()
	{
		// Only task 2 is out of reach: 80 Attack wanted, player has 80 for task 1's boss but the rec stats
		// here are shared, so use a profile between the two — task 1 (4 pts) reachable, task 2 (40) not.
		RecStatsLibrary split = RecStatsLibrary.load(s("{\"tasks\":{"
			+ "\"1\":{\"soft\":[{\"skills\":[\"Attack\"],\"level\":40}]},"
			+ "\"2\":{\"soft\":[{\"skills\":[\"Attack\"],\"level\":90}]}}}"));
		List<UnlockSuggestion> out = plan(player(40), split, effort());

		UnlockSuggestion reachable = find(out, "Short Quest");
		assertEquals("raw total is preserved for reference", 4, reachable.unlockedPoints());
		assertEquals(4, reachable.reachablePoints());

		UnlockSuggestion unreachable = find(out, "Long Quest");
		assertEquals("the 40-point prize is out of reach and counts for nothing",
			0, unreachable.reachablePoints());
		assertEquals("even though it is worth far more on paper", 40, unreachable.unlockedPoints());
		assertEquals("so the small reachable quest ranks first", "Short Quest", out.get(0).questName());
	}

	@Test
	public void withoutRecommendedStatsNothingIsFiltered()
	{
		// Back-compat: callers that never supplied rec stats behave exactly as before.
		List<UnlockSuggestion> out = plan(player(1), RecStatsLibrary.empty(), effort());
		assertEquals(2, out.size());
		for (UnlockSuggestion u : out)
		{
			assertEquals("every unlocked CA counts as reachable",
				u.unlockedTaskCount(), u.reachableTaskCount());
		}
	}

	// ---- against the real bundled data --------------------------------------------------------------

	private static PlayerProfile account(int everyStat)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking", "Fishing",
			"Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, everyStat);
		}
		levels.put("Hitpoints", Math.max(10, everyStat));
		Set<String> done = new HashSet<>(Arrays.asList(
			"Priest in Peril", "The Restless Ghost", "Children of the Sun"));
		return PlayerProfile.of(levels, done, done);
	}

	private static List<UnlockSuggestion> realPlan(PlayerProfile profile)
	{
		return new UnlockPlanner(QuestEffortLibrary.loadBundled(), SkillXpLibrary.loadBundled()).plan(
			CombatAchievementLibrary.loadBundled().all(), Collections.emptySet(),
			EffortDataLibrary.loadBundled(), profile, TaskDifficultyLibrary.loadBundled(), 1.0, 1.0,
			RecStatsLibrary.loadBundled());
	}

	@Test
	public void nothingIsReachableForALevelThree()
	{
		// The reported bug: a level-3 was offered The Final Dawn (~25 hrs) and A Kingdom Divided (~21 hrs),
		// ranked on prizes of 79 and 55 points it could never collect. Every one now counts zero, so the
		// panel's "Unlock next" section has nothing to show (asserted end-to-end in RouteReadinessTest).
		List<UnlockSuggestion> out = realPlan(account(1));
		assertFalse("precondition: the quests are still found", out.isEmpty());
		for (UnlockSuggestion u : out)
		{
			assertEquals("a level-3 can collect nothing from " + u.questName(),
				0, u.reachableTaskCount());
		}
	}

	@Test
	public void anEstablishedAccountKeepsItsBestUnlock()
	{
		// The other direction: this must not gut the section for an account that can use it.
		List<UnlockSuggestion> out = realPlan(account(80));
		assertFalse("an all-80s account still gets suggestions", out.isEmpty());
		assertEquals("and the biggest quick prize still leads", "Beneath Cursed Sands",
			out.get(0).questName());
		assertTrue("counted on what it can actually do", out.get(0).reachablePoints() > 200);
	}

	private static UnlockSuggestion find(List<UnlockSuggestion> out, String quest)
	{
		return out.stream().filter(u -> u.questName().equals(quest)).findFirst().orElseThrow(
			() -> new AssertionError("no suggestion for " + quest));
	}
}
