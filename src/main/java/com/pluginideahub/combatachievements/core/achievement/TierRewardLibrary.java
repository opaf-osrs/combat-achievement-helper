package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the bundled {@code tier_rewards.json} (tier display name → {@link TierReward}). Sparse and
 * safe: a missing/malformed file yields an empty library where every lookup returns
 * {@link TierReward#NONE}, so reward data problems never break the panel. Pure Java (Gson only).
 */
public final class TierRewardLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/tier_rewards.json";

	private final String version;
	private final Map<String, TierReward> byTier;

	private TierRewardLibrary(String version, Map<String, TierReward> byTier)
	{
		this.version = version;
		this.byTier = byTier;
	}

	public static TierRewardLibrary empty()
	{
		return new TierRewardLibrary("none", new LinkedHashMap<>());
	}

	public static TierRewardLibrary loadBundled()
	{
		try (InputStream in = TierRewardLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static TierRewardLibrary load(InputStream in)
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

			Map<String, TierReward> map = new LinkedHashMap<>();
			JsonElement tiersEl = root.get("tiers");
			if (tiersEl != null && tiersEl.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : tiersEl.getAsJsonObject().entrySet())
				{
					if (!e.getValue().isJsonObject())
					{
						continue;
					}
					JsonObject obj = e.getValue().getAsJsonObject();
					String headline = obj.has("headline") && !obj.get("headline").isJsonNull()
						? obj.get("headline").getAsString() : "";
					List<String> rewards = new ArrayList<>();
					JsonElement rewEl = obj.get("rewards");
					if (rewEl != null && rewEl.isJsonArray())
					{
						for (JsonElement r : rewEl.getAsJsonArray())
						{
							try
							{
								if (r.isJsonPrimitive() && r.getAsJsonPrimitive().isString())
								{
									rewards.add(r.getAsString());
								}
							}
							catch (RuntimeException ignored)
							{
								// skip malformed reward line
							}
						}
					}
					map.put(e.getKey().trim().toLowerCase(Locale.ROOT), new TierReward(headline, rewards));
				}
			}
			return new TierRewardLibrary(version, map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	public String version()
	{
		return version;
	}

	public int count()
	{
		return byTier.size();
	}

	/** Reward for a tier display name (case-insensitive), or {@link TierReward#NONE} when absent. */
	public TierReward forTier(String tierName)
	{
		if (tierName == null)
		{
			return TierReward.NONE;
		}
		return byTier.getOrDefault(tierName.trim().toLowerCase(Locale.ROOT), TierReward.NONE);
	}

	/** Convenience overload keyed by {@link AchievementTier}. */
	public TierReward forTier(AchievementTier tier)
	{
		return tier == null ? TierReward.NONE : forTier(tier.displayName());
	}
}
