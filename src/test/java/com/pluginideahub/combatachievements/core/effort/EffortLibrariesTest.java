package com.pluginideahub.combatachievements.core.effort;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EffortLibrariesTest
{
	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void bundledTimingLoads()
	{
		BossTimingLibrary lib = BossTimingLibrary.loadBundled();
		assertTrue("expected the bundled boss timing", lib.count() >= 80);
		assertTrue(lib.timingFor("Vorkath").isKnown());
		assertEquals(BossTiming.UNKNOWN, lib.timingFor("Not A Boss"));
	}

	@Test
	public void timingSecondsPerKillAddsRespawn()
	{
		BossTimingLibrary lib = BossTimingLibrary.load(stream(
			"{\"monsters\":{\"Test Boss\":{\"ttkSeconds\":50,\"respawnSeconds\":10,\"killsPerHour\":60}}}"));
		assertEquals(60, lib.timingFor("test boss").secondsPerKill());
	}

	@Test
	public void malformedTimingIsSafe()
	{
		BossTimingLibrary lib = BossTimingLibrary.load(stream("not json"));
		assertEquals(0, lib.count());
		assertSame(BossTiming.UNKNOWN, lib.timingFor("anything"));
	}

	@Test
	public void questPrerequisiteClosureWalksTheGraph()
	{
		QuestEffortLibrary lib = QuestEffortLibrary.load(stream(
			"{\"quests\":{"
				+ "\"A\":{\"name\":\"A\",\"estMinutes\":10,\"directPrerequisiteQuests\":[\"B\"]},"
				+ "\"B\":{\"name\":\"B\",\"estMinutes\":20,\"directPrerequisiteQuests\":[\"C\"]},"
				+ "\"C\":{\"name\":\"C\",\"estMinutes\":30,\"directPrerequisiteQuests\":[]}"
				+ "}}"));
		Set<String> prereqs = lib.fullPrerequisites("A");
		assertTrue(prereqs.contains("B"));
		assertTrue(prereqs.contains("C"));
		assertEquals(2, prereqs.size());

		// Remaining minutes = A + B + C when nothing done; subtracts completed quests.
		assertEquals(60, lib.remainingQuestMinutes("A", Collections.emptySet()));
		Set<String> doneB = new HashSet<>(Collections.singletonList("b"));
		assertEquals(40, lib.remainingQuestMinutes("A", doneB)); // A(10)+C(30), B skipped
	}

	@Test
	public void questGraphToleratesCycles()
	{
		QuestEffortLibrary lib = QuestEffortLibrary.load(stream(
			"{\"quests\":{"
				+ "\"X\":{\"name\":\"X\",\"directPrerequisiteQuests\":[\"Y\"]},"
				+ "\"Y\":{\"name\":\"Y\",\"directPrerequisiteQuests\":[\"X\"]}}}"));
		// Should not stack-overflow.
		assertTrue(lib.fullPrerequisites("X").contains("Y"));
	}

	@Test
	public void skillXpTableMatchesKnownValues()
	{
		// Canonical OSRS experience milestones.
		assertEquals(0, SkillXpLibrary.xpAtLevel(1));
		assertEquals(83, SkillXpLibrary.xpAtLevel(2));
		assertEquals(273742, SkillXpLibrary.xpAtLevel(60));
		assertEquals(13034431, SkillXpLibrary.xpAtLevel(99));
	}

	@Test
	public void hoursToTrainUsesBracketRates()
	{
		// 100k xp/hr flat across the range; train 60->70 should be (xp(70)-xp(60))/100k hours.
		SkillXpLibrary lib = SkillXpLibrary.load(stream(
			"{\"skills\":{\"Magic\":{\"skill\":\"Magic\",\"brackets\":["
				+ "{\"fromLevel\":1,\"toLevel\":99,\"method\":\"x\",\"xpPerHour\":100000,\"members\":true}]}}}"));
		double expected = (SkillXpLibrary.xpAtLevel(70) - SkillXpLibrary.xpAtLevel(60)) / 100000.0;
		assertEquals(expected, lib.hoursToTrain("magic", 60, 70), 1e-6);
		assertEquals("already there", 0.0, lib.hoursToTrain("Magic", 80, 70), 1e-9);
		assertEquals("no data", 0.0, lib.hoursToTrain("Cooking", 1, 99), 1e-9);
	}
}
