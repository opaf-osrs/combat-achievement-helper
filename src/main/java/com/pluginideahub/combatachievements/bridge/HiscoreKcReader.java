package com.pluginideahub.combatachievements.bridge;

import com.pluginideahub.combatachievements.core.effort.MonsterNames;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.hiscore.Skill;

/**
 * Backfills historical per-boss kill count from the OSRS HiScores (via RuneLite's {@code HiscoreClient}).
 * The chat tracker only sees kills made while the plugin runs; the hiscores carry the player's whole
 * ranked history, so seeding from them gives competence/partial-progress from kill 1. Maps each
 * {@code HiscoreSkill} of type BOSS to the dataset monster key via {@link MonsterNames}. Unranked
 * bosses report a level of -1 and are skipped.
 */
public final class HiscoreKcReader
{
	private HiscoreKcReader()
	{
	}

	/** Extracts {dataset monster key → kill count} from a hiscore lookup result. */
	public static Map<String, Integer> read(HiscoreResult result)
	{
		Map<String, Integer> out = new HashMap<>();
		if (result == null)
		{
			return out;
		}
		for (HiscoreSkill hs : HiscoreSkill.values())
		{
			if (hs.getType() != HiscoreSkillType.BOSS)
			{
				continue;
			}
			Skill skill = result.getSkill(hs);
			if (skill == null)
			{
				continue;
			}
			int kc = skill.getLevel();
			if (kc <= 0)
			{
				continue; // unranked (-1) or zero
			}
			String key = MonsterNames.toDatasetKey(hs.getName());
			if (!key.isEmpty())
			{
				out.merge(key, kc, Math::max);
			}
		}
		return out;
	}
}
