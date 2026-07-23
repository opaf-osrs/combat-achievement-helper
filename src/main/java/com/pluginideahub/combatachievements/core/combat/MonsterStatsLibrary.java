package com.pluginideahub.combatachievements.core.combat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
		try (InputStream in = MonsterStatsLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			if (in == null)
			{
				return empty();
			}
			return load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static MonsterStatsLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonElement monstersEl = parsed.getAsJsonObject().get("monsters");
			if (monstersEl == null || !monstersEl.isJsonObject())
			{
				return empty();
			}
			Map<String, MonsterStats> map = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : monstersEl.getAsJsonObject().entrySet())
			{
				if (!entry.getValue().isJsonObject())
				{
					continue;
				}
				JsonObject o = entry.getValue().getAsJsonObject();
				MonsterStats stats = new MonsterStats(
					entry.getKey(),
					intOf(o, "hitpoints"),
					intOf(o, "defenceLevel"),
					intOf(o, "magicLevel"),
					intOf(o, "defStab"),
					intOf(o, "defSlash"),
					intOf(o, "defCrush"),
					intOf(o, "defRange"),
					intOf(o, "defMagic"));
				map.put(entry.getKey().trim().toLowerCase(Locale.ROOT), stats);
			}
			return new MonsterStatsLibrary(map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static int intOf(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return 0;
		}
		try
		{
			return el.getAsInt();
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
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
