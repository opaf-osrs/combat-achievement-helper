package com.pluginideahub.combatachievements.core.achievement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TaskDifficultyLibraryTest
{
	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void readsDifficultyBossBumpAndReason()
	{
		TaskDifficultyLibrary lib = TaskDifficultyLibrary.load(stream(
			"{\"tasks\":{\"155\":{\"difficulty\":7,\"boss\":6,\"bump\":1.2,\"reason\":\"speed\"}}}"));
		TaskDifficulty d = lib.difficultyFor(155);
		assertEquals(7, d.difficulty());
		assertEquals(6, d.bossDifficulty());
		assertEquals(1.2, d.bump(), 1e-9);
		assertEquals("speed", d.reason());
	}

	@Test
	public void missingTaskYieldsUnknownFallbackOfThree()
	{
		TaskDifficultyLibrary lib = TaskDifficultyLibrary.load(stream("{\"tasks\":{}}"));
		assertSame(TaskDifficulty.UNKNOWN, lib.difficultyFor(42));
		assertEquals(3, lib.difficultyFor(42).difficulty());
	}

	@Test
	public void difficultyIsClampedToTheOneToTenScale()
	{
		TaskDifficultyLibrary lib = TaskDifficultyLibrary.load(stream(
			"{\"tasks\":{\"1\":{\"difficulty\":99},\"2\":{\"difficulty\":0}}}"));
		assertEquals(10, lib.difficultyFor(1).difficulty());
		assertEquals(1, lib.difficultyFor(2).difficulty());
	}

	@Test
	public void malformedEntriesAreSkippedNotFatal()
	{
		TaskDifficultyLibrary lib = TaskDifficultyLibrary.load(stream(
			"{\"tasks\":{\"1\":{\"difficulty\":5},\"bad\":{\"difficulty\":4},\"3\":\"nope\"}}"));
		assertEquals(1, lib.count());
		assertEquals(5, lib.difficultyFor(1).difficulty());
		assertEquals(3, lib.difficultyFor(3).difficulty()); // fell through to UNKNOWN
	}

	@Test
	public void emptyOnMissingOrBrokenJson()
	{
		assertEquals(0, TaskDifficultyLibrary.load(stream("not json")).count());
		assertEquals(0, TaskDifficultyLibrary.load(stream("{\"nope\":1}")).count());
		assertEquals(0, TaskDifficultyLibrary.empty().count());
	}

	@Test
	public void bundledResourceLoadsEveryTask()
	{
		assertTrue("bundled task_difficulty.json should be populated",
			TaskDifficultyLibrary.loadBundled().count() >= 600);
	}

	@Test
	public void bundledDifficultyRatesJadHardAndSlayerEasy()
	{
		TaskDifficultyLibrary lib = TaskDifficultyLibrary.loadBundled();
		// Noxious Foe (id 0) — an afk slayer kill-count — is trivially easy.
		assertTrue("slayer KC should be easy", lib.difficultyFor(0).difficulty() <= 2);
		// Fight Caves Master (id 148, TzTok-Jad) is hard despite being a popular milestone — the old
		// completion%-as-difficulty heuristic used to rate it as one of the *easiest* tasks.
		assertTrue("Jad should be hard, not easy", lib.difficultyFor(148).difficulty() >= 5);
	}
}
