package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileSignalsProviderTest
{
	// Task 1 needs Slayer 85; task 2 has no skill gate; task 3 needs Dragon Slayer II (completion);
	// task 4 needs Regicide started.
	private static EffortDataLibrary effortLib()
	{
		String json = "{\"tasks\":{"
			+ "\"1\":{\"levelReqs\":{\"Slayer\":85}},"
			+ "\"2\":{\"gearTier\":\"mid\"},"
			+ "\"3\":{\"questReqs\":[\"Dragon Slayer II\"]},"
			+ "\"4\":{\"questReqs\":[{\"name\":\"Regicide\",\"startedSuffices\":true}]}"
			+ "}}";
		return EffortDataLibrary.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}

	private static PlayerProfile profileWithSlayer(int level)
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Slayer", level);
		return PlayerProfile.of(levels);
	}

	private static Set<String> set(String... names)
	{
		return new HashSet<>(Arrays.asList(names));
	}

	@Test
	public void taskIsDoableWhenLevelsMet()
	{
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), profileWithSlayer(90));
		TaskLiveSignals s = provider.signalsFor(1);
		assertTrue(s.levelsMet());
		assertTrue(s.doableNow());
	}

	@Test
	public void taskIsLockedWhenLevelTooLow()
	{
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), profileWithSlayer(80));
		TaskLiveSignals s = provider.signalsFor(1);
		assertFalse(s.levelsMet());
		assertFalse(s.doableNow());
	}

	@Test
	public void taskWithoutGateIsAlwaysDoable()
	{
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), PlayerProfile.empty());
		TaskLiveSignals s = provider.signalsFor(2);
		assertTrue(s.levelsMet());
		assertTrue(s.doableNow());
	}

	@Test
	public void caseInsensitiveSkillNames()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("SLAYER", 99); // client may report different casing
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), PlayerProfile.of(levels));
		assertTrue(provider.signalsFor(1).levelsMet());
	}

	@Test
	public void questGatedTaskIsLockedWhenQuestNotDone()
	{
		// Player has the skills but has not completed Dragon Slayer II.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(),
			PlayerProfile.of(new HashMap<>(), set("Regicide"), set("Regicide")));
		TaskLiveSignals s = provider.signalsFor(3);
		assertFalse("quest gate not satisfied", s.accessMet());
		assertFalse(s.doableNow());
	}

	@Test
	public void questGatedTaskIsDoableWhenQuestComplete()
	{
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(),
			PlayerProfile.of(new HashMap<>(), set("Dragon Slayer II"), set("Dragon Slayer II")));
		TaskLiveSignals s = provider.signalsFor(3);
		assertTrue(s.accessMet());
		assertTrue(s.doableNow());
	}

	@Test
	public void startedSufficesGateAcceptsAnInProgressQuest()
	{
		// Task 4 only needs Regicide STARTED; the player is mid-quest.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(),
			PlayerProfile.of(new HashMap<>(), new HashSet<>(), set("Regicide")));
		assertTrue(provider.signalsFor(4).accessMet());
	}

	@Test
	public void taskWithoutQuestGateIsAccessibleByDefault()
	{
		// Task 1 has only a skill gate (no questReqs) — access is never blocked on a missing quest.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), profileWithSlayer(99));
		assertTrue(provider.signalsFor(1).accessMet());
	}

	// Task 2 (no effort level gate) carries a HARD 60 Slayer rec gate and a SOFT 90 Strength recommendation.
	private static RecStatsLibrary recStatsLib()
	{
		String json = "{\"tasks\":{\"2\":{"
			+ "\"hard\":[{\"skills\":[\"Slayer\"],\"level\":60,\"mode\":\"all\"}],"
			+ "\"soft\":[{\"skills\":[\"Strength\"],\"level\":90,\"mode\":\"all\"}]"
			+ "}}}";
		return RecStatsLibrary.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}

	private static PlayerProfile profile(int slayer, int strength)
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Slayer", slayer);
		levels.put("Strength", strength);
		return PlayerProfile.of(levels);
	}

	@Test
	public void hardRecStatGateLocksTaskWithNoEffortLevelGate()
	{
		// Task 2 has no curated levelReqs, but its HARD rec gate (60 Slayer) must still lock it.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), recStatsLib(),
			profile(50, 99));
		TaskLiveSignals s = provider.signalsFor(2);
		assertFalse("hard rec gate not met", s.levelsMet());
		assertFalse(s.doableNow());
	}

	@Test
	public void meetingHardAndSoftRecStatsLeavesTaskUnsunk()
	{
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), recStatsLib(),
			profile(70, 95));
		TaskLiveSignals s = provider.signalsFor(2);
		assertTrue(s.doableNow());
		assertFalse(s.belowRecStats());
		assertEquals(0, s.recStatsShortfall());
	}

	@Test
	public void belowSoftRecStatsSinksButDoesNotGate()
	{
		// Hard gate met (Slayer 70), but 10 Strength short of the soft 90 recommendation.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), recStatsLib(),
			profile(70, 80));
		TaskLiveSignals s = provider.signalsFor(2);
		assertTrue("soft rec stats never gate", s.doableNow());
		assertTrue(s.belowRecStats());
		assertEquals(10, s.recStatsShortfall());
	}

	@Test
	public void legacyConstructorIgnoresRecStats()
	{
		// The 2-arg constructor keeps the old behaviour: no rec-stats library, so task 2 is never gated
		// or sunk on recommended stats regardless of the player's Slayer/Strength.
		ProfileSignalsProvider provider = new ProfileSignalsProvider(effortLib(), profile(1, 1));
		TaskLiveSignals s = provider.signalsFor(2);
		assertTrue(s.doableNow());
		assertFalse(s.belowRecStats());
	}

	@Test
	public void profileMeetsHandlesMultiSkillAndEmpty()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Ranged", 70);
		levels.put("Slayer", 50);
		PlayerProfile p = PlayerProfile.of(levels);

		Map<String, Integer> needBoth = new HashMap<>();
		needBoth.put("Ranged", 70);
		needBoth.put("Slayer", 50);
		assertTrue(p.meets(needBoth));

		Map<String, Integer> needMore = new HashMap<>();
		needMore.put("Ranged", 70);
		needMore.put("Slayer", 60);
		assertFalse(p.meets(needMore));

		assertTrue(p.meets(new HashMap<>()));
	}
}
