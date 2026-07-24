package com.pluginideahub.combatachievements.core.combat;

import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the bundled per-monster combat stats ({@code monster_stats.json}, auto-sourced from the wiki
 * {@code infobox_monster} bucket) and exposes them as {@link MonsterStats} keyed by monster name
 * (case-insensitive). Pure Java (Gson only). Tasks join via their {@code monster} field; unknown
 * monsters (raids/activities pending curated aliases) return {@link MonsterStats#UNKNOWN}.
 */
public final class MonsterStatsLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/monster_stats.json";

	private final Map<String, MonsterStats> byName;

	private MonsterStatsLibrary(Map<String, MonsterStats> byName)
	{
		this.byName = byName;
	}

	public static MonsterStatsLibrary empty()
	{
		return new MonsterStatsLibrary(new LinkedHashMap<>());
	}

	public static MonsterStatsLibrary loadBundled()
	{
		return fromRoot(BundledJson.readBundled(MonsterStatsLibrary.class, BUNDLED_RESOURCE));
	}

	public static MonsterStatsLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static MonsterStatsLibrary fromRoot(JsonObject root)
	{
		return new MonsterStatsLibrary(
			BundledJson.nameMap(root, "monsters", MonsterStatsLibrary::parseStats));
	}

	/** {@code name} is the raw dataset key — MonsterStats keeps the display form. */
	private static MonsterStats parseStats(String name, JsonObject o)
	{
		return new MonsterStats(
			name,
			BundledJson.optInt(o, "hitpoints", 0),
			BundledJson.optInt(o, "defenceLevel", 0),
			BundledJson.optInt(o, "magicLevel", 0),
			BundledJson.optInt(o, "defStab", 0),
			BundledJson.optInt(o, "defSlash", 0),
			BundledJson.optInt(o, "defCrush", 0),
			BundledJson.optInt(o, "defRange", 0),
			BundledJson.optInt(o, "defMagic", 0));
	}

	public int count()
	{
		return byName.size();
	}

	/** Stats for a monster by name (case-insensitive); {@link MonsterStats#UNKNOWN} if not found. */
	public MonsterStats statsFor(String monsterName)
	{
		if (monsterName == null)
		{
			return MonsterStats.UNKNOWN;
		}
		return byName.getOrDefault(monsterName.trim().toLowerCase(Locale.ROOT), MonsterStats.UNKNOWN);
	}
}
