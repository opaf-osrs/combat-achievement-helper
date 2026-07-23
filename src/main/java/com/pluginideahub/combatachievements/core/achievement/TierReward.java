package com.pluginideahub.combatachievements.core.achievement;

import java.util.Collections;
import java.util.List;

/**
 * The unlocks granted for completing every task in a tier (e.g. Ghommal's hilt upgrades). Machine
 * readable so the panel can answer "what does my next tier unlock?" rather than burying rewards in
 * guide prose. Reward names are point-in-time wiki data and may drift with game updates.
 */
public final class TierReward
{
	public static final TierReward NONE = new TierReward("", Collections.emptyList());

	private final String headline;
	private final List<String> rewards;

	public TierReward(String headline, List<String> rewards)
	{
		this.headline = headline == null ? "" : headline;
		this.rewards = Collections.unmodifiableList(
			rewards == null ? Collections.emptyList() : new java.util.ArrayList<>(rewards));
	}

	/** The single iconic unlock for this tier (short), or "" when none is recorded. */
	public String headline()
	{
		return headline;
	}

	/** The fuller list of unlocks for tooltips/detail (may be empty). */
	public List<String> rewards()
	{
		return rewards;
	}

	public boolean isPresent()
	{
		return !headline.isEmpty() || !rewards.isEmpty();
	}
}
