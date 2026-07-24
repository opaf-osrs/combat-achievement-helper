package com.pluginideahub.combatachievements.core.achievement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CombatAchievementLibraryTest
{
	@Test
	public void bundledDatasetLoadsWithExpectedShape()
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		assertEquals("expected 646 tasks (July 2026 snapshot + Maggot King)", 646, lib.taskCount());
		assertEquals(646, lib.all().size());

		// Every task has a recognized tier and type, and derived points equal the tier rank.
		for (CombatAchievement task : lib.all())
		{
			assertNotNull("tier for " + task.id(), task.tier());
			assertNotNull("type for " + task.id(), task.type());
			assertEquals("points derive from tier for " + task.id(),
				task.tier().pointsPerTask(), task.points());
			assertEquals(Integer.valueOf(task.points()), lib.pointsById().get(task.id()));
		}
	}

	@Test
	public void perTierCountsMatchVerifiedNumbers()
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		assertEquals(41, lib.byTier(AchievementTier.EASY).size());
		assertEquals(60, lib.byTier(AchievementTier.MEDIUM).size());
		assertEquals(86, lib.byTier(AchievementTier.HARD).size());
		assertEquals(164, lib.byTier(AchievementTier.ELITE).size());
		assertEquals(174, lib.byTier(AchievementTier.MASTER).size());
		assertEquals(121, lib.byTier(AchievementTier.GRANDMASTER).size());
	}

	@Test
	public void emptyTaskArrayIsRejected()
	{
		try
		{
			CombatAchievementLibrary.load(stream("{\"version\":\"x\",\"tasks\":[]}"));
			fail("expected CombatAchievementDataException for empty tasks");
		}
		catch (CombatAchievementDataException expected)
		{
			assertTrue(expected.getMessage().toLowerCase().contains("empty"));
		}
	}

	@Test(expected = CombatAchievementDataException.class)
	public void malformedJsonIsRejected()
	{
		CombatAchievementLibrary.load(stream("{ this is not valid json "));
	}

	@Test(expected = CombatAchievementDataException.class)
	public void unknownTierIsRejected()
	{
		CombatAchievementLibrary.load(stream(
			"{\"tasks\":[{\"id\":1,\"name\":\"x\",\"tier\":\"Legendary\",\"type\":\"Kill Count\"}]}"));
	}

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
