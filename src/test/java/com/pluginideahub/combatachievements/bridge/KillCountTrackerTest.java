package com.pluginideahub.combatachievements.bridge;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KillCountTrackerTest
{
	@Test
	public void parsesStandardKillCountLine()
	{
		KillCountTracker.Update u = KillCountTracker.parse("Your Vorkath kill count is: 1,234.").get();
		assertEquals("Vorkath", u.boss());
		assertEquals(1234, u.count());
	}

	@Test
	public void parsesRaidCompletionLines()
	{
		assertEquals("Chambers of Xeric",
			KillCountTracker.parse("Your completed Chambers of Xeric count is: 50").get().boss());
		assertEquals(12, KillCountTracker.parse(
			"Your Theatre of Blood total completion count is: 12").get().count());
	}

	@Test
	public void ignoresNonKillCountMessages()
	{
		assertFalse(KillCountTracker.parse("You feel something weird sneaking into your backpack.").isPresent());
		assertFalse(KillCountTracker.parse(null).isPresent());
		// A bare "... count is:" (e.g. a non-boss tally) must NOT register as a fake boss.
		assertFalse(new KillCountTracker().onMessage("Your slayer task count is: 5"));
	}

	@Test
	public void canonicalisesBossNamesToTheDatasetNamespace()
	{
		KillCountTracker tracker = new KillCountTracker();
		tracker.onMessage("Your Nightmare kill count is: 30.");
		// Stored under the dataset spelling "The Nightmare", not the raw chat name.
		assertEquals(30, tracker.snapshot().kcFor("The Nightmare"));
		assertEquals(0, tracker.snapshot().kcFor("Nightmare"));
	}

	@Test
	public void accumulatesAndExposesExperience()
	{
		KillCountTracker tracker = new KillCountTracker();
		assertTrue(tracker.onMessage("Your Vorkath kill count is: 40."));
		assertTrue(tracker.onMessage("Your Vorkath kill count is: 41."));
		assertFalse(tracker.onMessage("just a normal message"));
		assertEquals(41, tracker.snapshot().kcFor("Vorkath"));
		// Counts only go up — a stale lower value never overwrites.
		tracker.onMessage("Your Vorkath kill count is: 5.");
		assertEquals(41, tracker.snapshot().kcFor("vorkath"));
	}
}
