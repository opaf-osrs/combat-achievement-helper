package com.pluginideahub.combatachievements.core.effort;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BossDifficultyLibraryTest
{
	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void looksUpCaseAndWhitespaceInsensitively()
	{
		BossDifficultyLibrary lib = BossDifficultyLibrary.load(stream(
			"{\"bosses\":{\"TzKal-Zuk\":10,\"Aberrant Spectre\":1}}"));
		assertEquals(10, lib.difficultyFor("tzkal-zuk"));
		assertEquals(10, lib.difficultyFor("  TzKal-Zuk  "));
		assertEquals(1, lib.difficultyFor("Aberrant Spectre"));
	}

	@Test
	public void unknownBossYieldsZero()
	{
		BossDifficultyLibrary lib = BossDifficultyLibrary.load(stream("{\"bosses\":{\"Zulrah\":4}}"));
		assertEquals(0, lib.difficultyFor("Nonexistent"));
		assertEquals(0, lib.difficultyFor(null));
	}

	@Test
	public void difficultyIsClampedToTenAndZeroFloor()
	{
		BossDifficultyLibrary lib = BossDifficultyLibrary.load(stream(
			"{\"bosses\":{\"A\":99,\"B\":-5}}"));
		assertEquals(10, lib.difficultyFor("A"));
		assertEquals(0, lib.difficultyFor("B"));
	}

	@Test
	public void emptyOnBrokenJson()
	{
		assertEquals(0, BossDifficultyLibrary.load(stream("not json")).count());
		assertEquals(0, BossDifficultyLibrary.empty().count());
	}

	@Test
	public void bundledResourceCarriesAllRatedBosses()
	{
		BossDifficultyLibrary lib = BossDifficultyLibrary.loadBundled();
		assertTrue("bundled boss_difficulty.json should be populated", lib.count() >= 80);
		// Anchor points from the curated scale (see data/curation/boss_difficulty.csv).
		assertEquals(10, lib.difficultyFor("TzKal-Zuk"));
		assertEquals(6, lib.difficultyFor("TzTok-Jad"));
		assertEquals(1, lib.difficultyFor("Aberrant Spectre"));
	}
}
