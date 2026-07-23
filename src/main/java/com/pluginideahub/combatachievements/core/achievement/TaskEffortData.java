package com.pluginideahub.combatachievements.core.achievement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hand-curated per-task effort signals that the wiki Bucket API does not provide (gear, level reqs,
 * access gate, RNG/supply variance, group requirement). Drives the low-hanging-fruit ranker and the
 * optimal-path solver. Coarse by design (small enums, not gp/attempt counts) so it is cheap to
 * curate and honest about being a heuristic. See docs/DESIGN.md §4.6.
 *
 * <p>Missing tasks fall back to {@link #NEUTRAL} rather than being excluded.</p>
 */
public final class TaskEffortData
{
	/** Coarse gear requirement bucket. */
	public enum GearTier
	{
		LOW, MID, HIGH, BIS;

		public static GearTier fromString(String s, GearTier fallback)
		{
			if (s == null)
			{
				return fallback;
			}
			switch (s.trim().toLowerCase(Locale.ROOT))
			{
				case "low": return LOW;
				case "mid": return MID;
				case "high": return HIGH;
				case "bis": return BIS;
				default: return fallback;
			}
		}
	}

	/** Coarse variance/intensity bucket, reused for both RNG and supply cost. */
	public enum Intensity
	{
		NONE, LOW, MED, HIGH;

		public static Intensity fromString(String s, Intensity fallback)
		{
			if (s == null)
			{
				return fallback;
			}
			switch (s.trim().toLowerCase(Locale.ROOT))
			{
				case "none": return NONE;
				case "low": return LOW;
				case "med": case "medium": return MED;
				case "high": return HIGH;
				default: return fallback;
			}
		}
	}

	/** The safe default for any task without a curated entry. */
	public static final TaskEffortData NEUTRAL = new TaskEffortData(
		"none", Collections.emptyMap(), Collections.emptyList(), GearTier.MID, Intensity.LOW,
		Intensity.LOW, true, "", false);

	private final String access;
	private final Map<String, Integer> levelReqs;
	private final List<QuestRequirement> questReqs;
	private final GearTier gearTier;
	private final Intensity rng;
	private final Intensity supply;
	private final boolean soloable;
	private final String minigameOrRaid;
	private final boolean curated;

	public TaskEffortData(String access, Map<String, Integer> levelReqs,
		List<QuestRequirement> questReqs, GearTier gearTier, Intensity rng, Intensity supply,
		boolean soloable, String minigameOrRaid, boolean curated)
	{
		this.access = access == null || access.trim().isEmpty() ? "none" : access.trim();
		this.levelReqs = Collections.unmodifiableMap(new LinkedHashMap<>(
			levelReqs == null ? Collections.emptyMap() : levelReqs));
		this.questReqs = Collections.unmodifiableList(new java.util.ArrayList<>(
			questReqs == null ? Collections.emptyList() : questReqs));
		this.gearTier = gearTier == null ? GearTier.MID : gearTier;
		this.rng = rng == null ? Intensity.LOW : rng;
		this.supply = supply == null ? Intensity.LOW : supply;
		this.soloable = soloable;
		this.minigameOrRaid = minigameOrRaid == null ? "" : minigameOrRaid;
		this.curated = curated;
	}

	public String access()
	{
		return access;
	}

	/** True when this task has no access gate the player must first satisfy. */
	public boolean hasAccessGate()
	{
		return !"none".equalsIgnoreCase(access);
	}

	public Map<String, Integer> levelReqs()
	{
		return levelReqs;
	}

	/** Quests that gate access to this task's content (empty when there is no quest gate). */
	public List<QuestRequirement> questReqs()
	{
		return questReqs;
	}

	/** True when this task is gated behind one or more quests. */
	public boolean hasQuestGate()
	{
		return !questReqs.isEmpty();
	}

	public GearTier gearTier()
	{
		return gearTier;
	}

	public Intensity rng()
	{
		return rng;
	}

	public Intensity supply()
	{
		return supply;
	}

	public boolean soloable()
	{
		return soloable;
	}

	public String minigameOrRaid()
	{
		return minigameOrRaid;
	}

	/** False when this is the {@link #NEUTRAL} fallback or otherwise un-curated. */
	public boolean curated()
	{
		return curated;
	}
}
