package com.pluginideahub.combatachievements.core.effort;

import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(BossTimingLibrary.class, BUNDLED_RESOURCE));
	}

	public static BossTimingLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static BossTimingLibrary fromRoot(JsonObject root)
	{
		if (root == null)
		{
			return empty();
		}
		return new BossTimingLibrary(BundledJson.optString(root, "version", "unknown"),
			BundledJson.nameMap(root, "monsters", (name, o) -> new BossTiming(
				BundledJson.optInt(o, "ttkSeconds", 0), BundledJson.optInt(o, "respawnSeconds", 0),
				BundledJson.optInt(o, "killsPerHour", 0), BundledJson.optString(o, "note", ""),
				BundledJson.optDouble(o, "attemptsPerKill", 1.0))));
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
