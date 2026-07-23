package com.pluginideahub.combatachievements.core.effort;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MonsterNamesTest
{
	@Test
	public void mapsArticledAndStructuralAliasesToDatasetKeys()
	{
		assertEquals("the nightmare", MonsterNames.toDatasetKey("Nightmare"));
		assertEquals("the mimic", MonsterNames.toDatasetKey("Mimic"));
		assertEquals("barrows", MonsterNames.toDatasetKey("Barrows Chests"));
		assertEquals("crystalline hunllef", MonsterNames.toDatasetKey("The Gauntlet"));
		assertEquals("crystalline hunllef", MonsterNames.toDatasetKey("Gauntlet"));
		assertEquals("corrupted hunllef", MonsterNames.toDatasetKey("The Corrupted Gauntlet"));
		assertEquals("leviathan", MonsterNames.toDatasetKey("The Leviathan"));
		assertEquals("moons of peril", MonsterNames.toDatasetKey("Lunar Chests"));
		assertEquals("fortis colosseum", MonsterNames.toDatasetKey("Sol Heredit"));
		// Raid mode variants: re-insert the colon the chat line drops.
		assertEquals("chambers of xeric: challenge mode",
			MonsterNames.toDatasetKey("Chambers of Xeric Challenge Mode"));
		assertEquals("theatre of blood: hard mode",
			MonsterNames.toDatasetKey("Theatre of Blood Hard Mode"));
	}

	@Test
	public void passesThroughAlreadyMatchingNames()
	{
		assertEquals("vorkath", MonsterNames.toDatasetKey("Vorkath"));
		assertEquals("abyssal sire", MonsterNames.toDatasetKey("Abyssal Sire"));
		// Hiscore CoX-CM already has the colon — must stay stable.
		assertEquals("chambers of xeric: challenge mode",
			MonsterNames.toDatasetKey("Chambers of Xeric: Challenge Mode"));
		assertEquals("", MonsterNames.toDatasetKey(null));
	}
}
