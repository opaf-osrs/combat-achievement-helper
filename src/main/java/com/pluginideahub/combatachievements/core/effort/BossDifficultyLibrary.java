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
 * Loads the bundled {@code boss_difficulty.json} (boss/activity name → pure-skill difficulty 1–10),
 * the hand-rated per-boss backbone the per-task {@code TaskDifficulty} is built on. Sparse and safe:
 * a missing/malformed file yields an empty library where every lookup returns 0 (unknown). Case- and
 * whitespace-insensitive keys. Pure Java (Gson only). Mirrors {@link BossTimingLibrary}. Feeds the
 * future Bosses browse mode and the By-boss sort. See CONTEXT.md "Difficulty".
 */
public final class BossDifficultyLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/boss_difficulty.json";

	private final String version;
	private final Map<String, Integer> byBoss;
	private final java.util.Set<String> endgameAccess;

	private BossDifficultyLibrary(String version, Map<String, Integer> byBoss,
		java.util.Set<String> endgameAccess)
	{
		this.version = version;
		this.byBoss = byBoss;
		this.endgameAccess = endgameAccess;
	}

	public static BossDifficultyLibrary empty()
	{
		return new BossDifficultyLibrary("none", new LinkedHashMap<>(), new java.util.HashSet<>());
	}

	public static BossDifficultyLibrary loadBundled()
	{
		try (InputStream in = BossDifficultyLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static BossDifficultyLibrary load(InputStream in)
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

			Map<String, Integer> map = new LinkedHashMap<>();
			JsonElement el = root.get("bosses");
			if (el != null && el.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
				{
					try
					{
						int diff = e.getValue().getAsInt();
						map.put(e.getKey().trim().toLowerCase(Locale.ROOT),
							Math.max(0, Math.min(10, diff)));
					}
					catch (RuntimeException ignored)
					{
						// skip malformed entry
					}
				}
			}
			java.util.Set<String> gated = new java.util.HashSet<>();
			JsonElement ga = root.get("endgameAccess");
			if (ga != null && ga.isJsonArray())
			{
				for (JsonElement e : ga.getAsJsonArray())
				{
					try
					{
						gated.add(e.getAsString().trim().toLowerCase(Locale.ROOT));
					}
					catch (RuntimeException ignored)
					{
						// skip malformed entry
					}
				}
			}
			return new BossDifficultyLibrary(version, map, gated);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	/**
	 * True for content you cannot simply walk up to at a low level — raids, instanced gauntlets and the
	 * coordinated group bosses. Deliberately NOT a difficulty claim: some tasks inside these are genuinely
	 * easy (Chambers of Xeric's "Playing with Lasers" is a puzzle room), which is precisely why it is
	 * curated per-activity in boss_difficulty.csv rather than inferred from task stats.
	 */
	public boolean isEndgameAccess(String boss)
	{
		return boss != null && endgameAccess.contains(boss.trim().toLowerCase(Locale.ROOT));
	}

	public String version()
	{
		return version;
	}

	public int count()
	{
		return byBoss.size();
	}

	/** Difficulty (1–10) for a boss/activity (case-insensitive), or 0 when unknown. */
	public int difficultyFor(String boss)
	{
		if (boss == null)
		{
			return 0;
		}
		return byBoss.getOrDefault(boss.trim().toLowerCase(Locale.ROOT), 0);
	}
}
