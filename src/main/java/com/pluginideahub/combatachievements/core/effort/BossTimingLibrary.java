package com.pluginideahub.combatachievements.core.effort;

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
 * Loads the bundled {@code boss_timing.json} (monster name → {@link BossTiming}). Sparse and safe: a
 * missing/malformed file yields an empty library where every lookup returns {@link BossTiming#UNKNOWN},
 * so timing-data problems never break the ranker. Pure Java (Gson only). Mirrors
 * {@code EffortDataLibrary}.
 */
public final class BossTimingLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/boss_timing.json";

	private final String version;
	private final Map<String, BossTiming> byMonster;

	private BossTimingLibrary(String version, Map<String, BossTiming> byMonster)
	{
		this.version = version;
		this.byMonster = byMonster;
	}

	public static BossTimingLibrary empty()
	{
		return new BossTimingLibrary("none", new LinkedHashMap<>());
	}

	public static BossTimingLibrary loadBundled()
	{
		try (InputStream in = BossTimingLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static BossTimingLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonObject root = parsed.getAsJsonObject();
			String version = root.has("version") && !root.get("version").isJsonNull()
				? root.get("version").getAsString() : "unknown";

			Map<String, BossTiming> map = new LinkedHashMap<>();
			JsonElement el = root.get("monsters");
			if (el != null && el.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
				{
					if (!e.getValue().isJsonObject())
					{
						continue;
					}
					JsonObject o = e.getValue().getAsJsonObject();
					map.put(e.getKey().trim().toLowerCase(Locale.ROOT), new BossTiming(
						optInt(o, "ttkSeconds"), optInt(o, "respawnSeconds"), optInt(o, "killsPerHour"),
						optString(o, "note"), optDouble(o, "attemptsPerKill", 1.0)));
				}
			}
			return new BossTimingLibrary(version, map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static int optInt(JsonObject o, String key)
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

	private static double optDouble(JsonObject o, String key, double fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsDouble();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	private static String optString(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return "";
		}
		try
		{
			return el.getAsString();
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	public String version()
	{
		return version;
	}

	public int count()
	{
		return byMonster.size();
	}

	/** Timing for a monster/activity (case-insensitive), or {@link BossTiming#UNKNOWN} when absent. */
	public BossTiming timingFor(String monster)
	{
		if (monster == null)
		{
			return BossTiming.UNKNOWN;
		}
		return byMonster.getOrDefault(monster.trim().toLowerCase(Locale.ROOT), BossTiming.UNKNOWN);
	}
}
