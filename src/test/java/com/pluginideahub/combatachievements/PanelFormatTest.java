package com.pluginideahub.combatachievements;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Training estimates read in hours however long they run, because the figure is time spent playing.
 * The exception is a daily-gated skill (Farming), where the estimate is elapsed time waiting on patch
 * timers — showing Slayer's 83 hours as "3.5 days" wrongly implied it passes while you are offline.
 */
public class PanelFormatTest
{
	@Test
	public void ordinaryEstimatesStayInHoursNoMatterHowLarge()
	{
		assertEquals("45 min", CombatAchievementsPanel.formatMinutes(45, false));
		assertEquals("1.5 hr", CombatAchievementsPanel.formatMinutes(90, false));
		assertEquals("83 hr", CombatAchievementsPanel.formatMinutes(83 * 60, false));
		assertEquals("a Slayer grind is hours, never days", "164 hr",
			CombatAchievementsPanel.formatMinutes(164 * 60, false));
	}

	@Test
	public void onlyCalendarGatedEstimatesReadInDays()
	{
		assertEquals("12 days", CombatAchievementsPanel.formatMinutes(12 * 24 * 60, true));
		assertEquals("5.1 days", CombatAchievementsPanel.formatMinutes((int) (5.1 * 24 * 60), true));
		// Below two days even a calendar estimate stays in hours — "1.5 days" is harder to read than "36 hr".
		assertEquals("36 hr", CombatAchievementsPanel.formatMinutes(36 * 60, true));
	}

	@Test
	public void theDefaultIsHours()
	{
		assertEquals("the no-flag overload must never produce days", "83 hr",
			CombatAchievementsPanel.formatMinutes(83 * 60));
	}
}
