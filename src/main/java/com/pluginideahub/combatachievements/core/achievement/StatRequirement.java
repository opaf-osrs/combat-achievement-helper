package com.pluginideahub.combatachievements.core.achievement;

import java.util.Collections;
import java.util.List;

/**
 * One parsed "recommended stats" requirement, e.g. 90 in every one of Attack/Strength/Defence, or 80 in
 * the best of Magic/Ranged, or 75 in your primary attack style. Parsed from the curated text into
 * {@code rec_stats.json}; see data/curation/build-rec-stats-json.py.
 */
public final class StatRequirement
{
	public enum Mode
	{
		/** Need {@code level} in EVERY listed skill. */
		ALL,
		/** Need {@code level} in ANY one listed skill. */
		ANY,
		/** Need {@code level} in your best attack style (max of the listed skills). */
		PRIMARY
	}

	private final List<String> skills;
	private final int level;
	private final Mode mode;

	public StatRequirement(List<String> skills, int level, Mode mode)
	{
		this.skills = Collections.unmodifiableList(skills == null ? Collections.emptyList() : skills);
		this.level = level;
		this.mode = mode == null ? Mode.ALL : mode;
	}

	public List<String> skills()
	{
		return skills;
	}

	public int level()
	{
		return level;
	}

	public Mode mode()
	{
		return mode;
	}
}
