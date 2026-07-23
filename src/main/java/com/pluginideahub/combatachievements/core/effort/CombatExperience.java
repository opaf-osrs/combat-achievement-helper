package com.pluginideahub.combatachievements.core.effort;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A pure snapshot of the player's kill count per boss/activity. KC is a three-in-one signal: it gives
 * partial progress on Kill Count tasks, a competence proxy for execution-heavy tasks (someone with
 * 1,000 Gauntlet kills will land a no-hit task far more reliably than someone with 5), and an
 * "already engaged here" hint for session synergy. The bridge fills this from the live client; the
 * core just reads it. Keyed case-insensitively by the dataset's monster name.
 */
public final class CombatExperience
{
	/** How well the player knows a boss, derived from kill count. */
	public enum Competence
	{
		NOVICE, EXPERIENCED, VETERAN
	}

	private final Map<String, Integer> kcByMonster;

	private CombatExperience(Map<String, Integer> kcByMonster)
	{
		this.kcByMonster = Collections.unmodifiableMap(kcByMonster);
	}

	public static CombatExperience empty()
	{
		return new CombatExperience(new HashMap<>());
	}

	/** Builds from a {monster name → kill count} map (names normalised to lower-case). */
	public static CombatExperience of(Map<String, Integer> rawKc)
	{
		Map<String, Integer> m = new HashMap<>();
		if (rawKc != null)
		{
			for (Map.Entry<String, Integer> e : rawKc.entrySet())
			{
				if (e.getKey() != null && e.getValue() != null && e.getValue() >= 0)
				{
					m.put(e.getKey().trim().toLowerCase(Locale.ROOT), e.getValue());
				}
			}
		}
		return new CombatExperience(m);
	}

	public int kcFor(String monster)
	{
		if (monster == null)
		{
			return 0;
		}
		return kcByMonster.getOrDefault(monster.trim().toLowerCase(Locale.ROOT), 0);
	}

	/** True once the player has engaged a boss at all (KC &gt; 0). */
	public boolean hasEngaged(String monster)
	{
		return kcFor(monster) > 0;
	}

	public Competence competence(String monster)
	{
		int kc = kcFor(monster);
		if (kc >= 150)
		{
			return Competence.VETERAN;
		}
		if (kc >= 25)
		{
			return Competence.EXPERIENCED;
		}
		return Competence.NOVICE;
	}

	public boolean isEmpty()
	{
		return kcByMonster.isEmpty();
	}
}
