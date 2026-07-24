package com.pluginideahub.combatachievements.core.effort;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(BossDifficultyLibrary.class, BUNDLED_RESOURCE));
	}

	public static BossDifficultyLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static BossDifficultyLibrary fromRoot(JsonObject root)
	{
		if (root == null)
		{
			return empty();
		}
		Map<String, Integer> map = BundledJson.nameMapValues(root, "bosses",
			el -> Math.max(0, Math.min(10, el.getAsInt())));
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
		return new BossDifficultyLibrary(BundledJson.optString(root, "version", "unknown"), map, gated);
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
