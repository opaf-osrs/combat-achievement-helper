package com.pluginideahub.combatachievements.bridge;

import java.util.HashMap;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HiscoreKcReaderTest
{
	private static Skill kc(int value)
	{
		return new Skill(value <= 0 ? -1 : 1000, value, 0L);
	}

	@Test
	public void mapsHiscoreBossesToDatasetMonsterKeys()
	{
		Map<HiscoreSkill, Skill> skills = new HashMap<>();
		skills.put(HiscoreSkill.VORKATH, kc(250));
		skills.put(HiscoreSkill.THE_GAUNTLET, kc(80));          // -> Crystalline Hunllef
		skills.put(HiscoreSkill.THE_CORRUPTED_GAUNTLET, kc(15)); // -> Corrupted Hunllef
		skills.put(HiscoreSkill.BARROWS_CHESTS, kc(40));         // -> Barrows
		skills.put(HiscoreSkill.NIGHTMARE, kc(12));              // -> The Nightmare
		skills.put(HiscoreSkill.ABYSSAL_SIRE, kc(-1));           // unranked -> skipped

		Map<String, Integer> out = HiscoreKcReader.read(new HiscoreResult("player", skills));

		assertEquals(Integer.valueOf(250), out.get("vorkath"));
		assertEquals(Integer.valueOf(80), out.get("crystalline hunllef"));
		assertEquals(Integer.valueOf(15), out.get("corrupted hunllef"));
		assertEquals(Integer.valueOf(40), out.get("barrows"));
		assertEquals(Integer.valueOf(12), out.get("the nightmare"));
		assertFalse("unranked boss is skipped", out.containsKey("abyssal sire"));
	}

	@Test
	public void nullResultIsEmpty()
	{
		assertEquals(0, HiscoreKcReader.read(null).size());
	}
}
