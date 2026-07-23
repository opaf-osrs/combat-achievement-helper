package com.pluginideahub.combatachievements.core.achievement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EffortDataLibraryTest
{
	@Test
	public void curatedEntryParses()
	{
		EffortDataLibrary lib = EffortDataLibrary.load(stream(
			"{\"tasks\":{\"7\":{\"access\":\"none\",\"gearTier\":\"high\",\"rng\":\"med\","
				+ "\"supply\":\"high\",\"soloable\":false,\"minigameOrRaid\":\"ToB\","
				+ "\"levelReqs\":{\"Ranged\":90}}}}"));

		TaskEffortData data = lib.effortFor(7);
		assertTrue(data.curated());
		assertEquals(TaskEffortData.GearTier.HIGH, data.gearTier());
		assertEquals(TaskEffortData.Intensity.MED, data.rng());
		assertEquals(TaskEffortData.Intensity.HIGH, data.supply());
		assertFalse(data.soloable());
		assertEquals("ToB", data.minigameOrRaid());
		assertEquals(Integer.valueOf(90), data.levelReqs().get("Ranged"));
	}

	@Test
	public void questReqsParseFromStringAndObjectForms()
	{
		EffortDataLibrary lib = EffortDataLibrary.load(stream(
			"{\"tasks\":{"
				+ "\"3\":{\"questReqs\":[\"Dragon Slayer II\"]},"
				+ "\"4\":{\"questReqs\":[{\"name\":\"Regicide\",\"startedSuffices\":true}]}"
				+ "}}"));

		TaskEffortData byString = lib.effortFor(3);
		assertTrue(byString.hasQuestGate());
		assertEquals(1, byString.questReqs().size());
		assertEquals("Dragon Slayer II", byString.questReqs().get(0).quest());
		assertFalse("bare string => must be completed", byString.questReqs().get(0).startedSuffices());

		TaskEffortData byObject = lib.effortFor(4);
		assertEquals("Regicide", byObject.questReqs().get(0).quest());
		assertTrue(byObject.questReqs().get(0).startedSuffices());
	}

	@Test
	public void questReqsOnlyEntryIsNotFlaggedCurated()
	{
		// questReqs are auto-derived (like levelReqs); a questReqs-only entry must not pose as curated.
		EffortDataLibrary lib = EffortDataLibrary.load(stream(
			"{\"tasks\":{\"3\":{\"questReqs\":[\"Dragon Slayer II\"]}}}"));
		assertFalse(lib.effortFor(3).curated());
		assertTrue(lib.effortFor(3).hasQuestGate());
	}

	@Test
	public void malformedQuestReqsAreSkippedNotThrown()
	{
		EffortDataLibrary lib = EffortDataLibrary.load(stream(
			"{\"tasks\":{\"3\":{\"questReqs\":[\"\",{\"noName\":true},42,\"Regicide\"]}}}"));
		TaskEffortData d = lib.effortFor(3);
		assertEquals(1, d.questReqs().size());
		assertEquals("Regicide", d.questReqs().get(0).quest());
	}

	@Test
	public void missingTaskFallsBackToNeutral()
	{
		EffortDataLibrary lib = EffortDataLibrary.load(stream("{\"tasks\":{\"7\":{\"gearTier\":\"bis\"}}}"));
		assertSame(TaskEffortData.NEUTRAL, lib.effortFor(999));
		assertFalse(lib.effortFor(999).curated());
	}

	@Test
	public void bundledEffortDataCarriesLevelRequirements()
	{
		EffortDataLibrary lib = EffortDataLibrary.loadBundled();
		// The reldo-sourced skill gates were merged in: hundreds of tasks now have level reqs.
		assertTrue("expected the populated effort dataset", lib.curatedCount() >= 300);
		// Task 0 (Noxious Foe / Aberrant Spectre) requires Slayer 60.
		assertEquals(Integer.valueOf(60), lib.effortFor(0).levelReqs().get("Slayer"));
	}

	@Test
	public void malformedFileYieldsEmptyLibrary()
	{
		EffortDataLibrary lib = EffortDataLibrary.load(stream("not json"));
		assertEquals(0, lib.curatedCount());
		assertSame(TaskEffortData.NEUTRAL, lib.effortFor(1));
	}

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
