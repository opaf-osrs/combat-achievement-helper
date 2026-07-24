package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(TierRewardLibrary.class, BUNDLED_RESOURCE));
	}

	public static TierRewardLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static TierRewardLibrary fromRoot(JsonObject root)
	{
		if (root == null)
		{
			return empty();
		}
		return new TierRewardLibrary(BundledJson.optString(root, "version", "unknown"),
			BundledJson.nameMap(root, "tiers", (name, obj) -> parseTier(obj)));
	}

	private static TierReward parseTier(JsonObject obj)
	{
		String headline = BundledJson.optString(obj, "headline", "");
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
		return new TierReward(headline, rewards);
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
