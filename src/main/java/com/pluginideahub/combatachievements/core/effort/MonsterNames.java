package com.pluginideahub.combatachievements.core.effort;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Canonicalises an external boss/activity name (from a kill-count chat line or the hiscores) to the
 * key the dataset uses for that monster, lower-cased. This is the one place the two namespaces meet:
 * the in-game / hiscore spelling ("Nightmare", "The Gauntlet", "Barrows Chests", "Chambers of Xeric
 * Challenge Mode") rarely matches the dataset's {@code monster} field ("The Nightmare", "Crystalline
 * Hunllef", "Barrows", "Chambers of Xeric: Challenge Mode"). Apply this at ingestion so kill counts
 * land in the same namespace the effort engine queries with {@code CombatAchievement.monster()}.
 */
public final class MonsterNames
{
	private static final Map<String, String> ALIASES = build();

	private MonsterNames()
	{
	}

	/** The dataset monster key (lower-cased) for an external boss/activity name. */
	public static String toDatasetKey(String externalName)
	{
		if (externalName == null)
		{
			return "";
		}
		String n = externalName.trim().toLowerCase(Locale.ROOT);
		return ALIASES.getOrDefault(n, n);
	}

	private static Map<String, String> build()
	{
		Map<String, String> m = new HashMap<>();
		// Articled / pluralised names.
		m.put("nightmare", "the nightmare");
		m.put("mimic", "the mimic");
		m.put("barrows chests", "barrows");
		m.put("the leviathan", "leviathan");
		m.put("the whisperer", "whisperer");
		m.put("the royal titans", "royal titans");
		// The Gauntlet's boss is the (Corrupted) Hunllef in the dataset; chat + hiscore use the activity.
		m.put("gauntlet", "crystalline hunllef");
		m.put("the gauntlet", "crystalline hunllef");
		m.put("corrupted gauntlet", "corrupted hunllef");
		m.put("the corrupted gauntlet", "corrupted hunllef");
		// Activities the dataset labels by their reward/area.
		m.put("lunar chests", "moons of peril");
		m.put("sol heredit", "fortis colosseum");
		// Raid mode variants: the completion chat line drops the colon the dataset keeps.
		m.put("chambers of xeric challenge mode", "chambers of xeric: challenge mode");
		m.put("theatre of blood hard mode", "theatre of blood: hard mode");
		m.put("theatre of blood entry mode", "theatre of blood: entry mode");
		m.put("theatre of blood: entry mode", "theatre of blood: entry mode");
		m.put("tombs of amascut expert mode", "tombs of amascut: expert mode");
		m.put("tombs of amascut expert", "tombs of amascut: expert mode");
		m.put("tombs of amascut entry mode", "tombs of amascut: entry mode");
		m.put("tombs of amascut: entry mode", "tombs of amascut: entry mode");
		return m;
	}
}
