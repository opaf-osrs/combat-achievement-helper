package com.pluginideahub.combatachievements.core.combat;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MonsterStatsLibraryTest
{
	@Test
	public void bundledStatsResolveKnownMonsters()
	{
		MonsterStatsLibrary lib = MonsterStatsLibrary.loadBundled();
		assertTrue("expected the auto-sourced monster set", lib.count() >= 60);

		MonsterStats sire = lib.statsFor("Abyssal Sire");
		assertTrue(sire.isKnown());
		assertEquals(425, sire.hitpoints());
		assertEquals(250, sire.defenceLevel());
		assertEquals(60, sire.defSlash());

		// case-insensitive
		assertTrue(lib.statsFor("abyssal sire").isKnown());
	}

	@Test
	public void unknownMonsterIsUnknown()
	{
		MonsterStatsLibrary lib = MonsterStatsLibrary.loadBundled();
		MonsterStats raid = lib.statsFor("Theatre of Blood"); // activity, no single-monster row
		assertFalse(raid.isKnown());
		assertEquals(MonsterStats.UNKNOWN, lib.statsFor(null));
	}

	@Test
	public void mostCaTasksWithAMonsterResolveStats()
	{
		MonsterStatsLibrary stats = MonsterStatsLibrary.loadBundled();
		CombatAchievementLibrary tasks = CombatAchievementLibrary.loadBundled();
		int withMonster = 0;
		int resolved = 0;
		for (CombatAchievement t : tasks.all())
		{
			if (t.hasMonster())
			{
				withMonster++;
				if (stats.statsFor(t.monster()).isKnown())
				{
					resolved++;
				}
			}
		}
		// Single-monster tasks resolve; raids/activities don't (curated aliases come later).
		assertTrue("expected majority of monster tasks to resolve stats, got " + resolved + "/" + withMonster,
			resolved >= withMonster / 2);
	}
}
