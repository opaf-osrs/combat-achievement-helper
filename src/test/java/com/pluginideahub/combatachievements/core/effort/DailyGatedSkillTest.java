package com.pluginideahub.combatachievements.core.effort;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Farming is bounded by the calendar, not by time at the keyboard: its patches grow on multi-hour
 * timers, so its bundled rate is an xpPerDay allowance the engine spreads over 24 elapsed hours.
 * Before this, the wiki's "per actively spent hour" figures (up to 2.5m xp/hr) were read literally and
 * the model believed 70-90 took about 27 minutes.
 */
public class DailyGatedSkillTest
{
	private final SkillXpLibrary lib = SkillXpLibrary.loadBundled();

	private static double days(double hours)
	{
		return hours / 24.0;
	}

	@Test
	public void farmingIsCostedInCalendarDays()
	{
		// 20,000/day to 50 and 50,000/day after: level 50 is 101,333 xp, level 65 is 449,428.
		double toFifty = lib.hoursToTrain("Farming", 1, 50);
		assertEquals("1-50 at 20k a day", 5.1, days(toFifty), 0.2);

		double toHespori = lib.hoursToTrain("Farming", 1, 65);
		assertEquals("1-65 for Hespori, a fortnight of tree runs", 12.0, days(toHespori), 0.3);
	}

	@Test
	public void theOldPerActiveHourFigureNoLongerMakesHighLevelFarmingFree()
	{
		// The bug this replaces: at 2.4m xp/hr, 70-90 came out at roughly 27 minutes, so any quest gated
		// on high Farming looked free to the unlock planner.
		double hours = lib.hoursToTrain("Farming", 70, 90);
		assertTrue("70-90 must not read as an afternoon (got " + hours + " hr)", hours > 100);
		// 70-90 is 4.6m xp — three months of daily runs, not 27 minutes.
		assertEquals("about 92 days at 50k a day", 92.2, days(hours), 1.0);
	}

	@Test
	public void onlyFarmingIsFlaggedAsCalendarTime()
	{
		// Drives whether the panel reads an estimate in days. Slayer is long but you have to sit and play
		// it, so it must never be flagged — showing its 83 hours as "3.5 days" implied it passes offline.
		assertTrue("Farming waits on patch timers", lib.isDailyGated("Farming", 1, 65));
		assertFalse("Slayer is time at the keyboard", lib.isDailyGated("Slayer", 1, 92));
		assertFalse(lib.isDailyGated("Firemaking", 1, 50));
		assertFalse("an unknown skill is not calendar-gated", lib.isDailyGated("Nonsense", 1, 50));
	}

	@Test
	public void slayerRatesAreCappedAtFortyThousand()
	{
		// Published Slayer rates assume barrage-stacking ideal tasks with a full setup; ordinary play with
		// travel, bad tasks and skips does not sustain them.
		double hours = lib.hoursToTrain("Slayer", 1, 92);
		assertTrue("1-92 Slayer is a real grind, not a weekend (got " + hours + " hr)",
			hours > 140 && hours < 200);
	}

	@Test
	public void anOrdinarySkillIsStillCostedInHoursPlayed()
	{
		// Only daily-gated brackets carry xpPerDay; everything else keeps the hours-played convention.
		double firemaking = lib.hoursToTrain("Firemaking", 1, 50);
		assertTrue("Firemaking 1-50 is an evening, not a fortnight (got " + firemaking + " hr)",
			firemaking > 0.5 && firemaking < 12);
	}

	@Test
	public void farmingOutranksNothingItShouldNot()
	{
		// The point of the change: per hour of effort, Farming must now cost far more than a skill you can
		// simply sit and train to the same level.
		assertTrue("Farming to 65 must cost more than Firemaking to 65",
			lib.hoursToTrain("Farming", 1, 65) > 10 * lib.hoursToTrain("Firemaking", 1, 65));
	}
}
