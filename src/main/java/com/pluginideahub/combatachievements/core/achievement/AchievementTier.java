package com.pluginideahub.combatachievements.core.achievement;

/**
 * The six Combat Achievement tiers. A completed task awards points equal to its tier rank
 * (Easy=1 .. Grandmaster=6); this is a structural OSRS constant and is the one value safe to encode
 * here. Task <em>counts</em> and therefore tier-unlock <em>thresholds</em> are NOT hard-coded — they
 * are derived from the loaded dataset at runtime (see {@code core.tier.TierMath}) so they self-update
 * when Jagex adds tasks. See docs/DESIGN.md §4.2.
 */
public enum AchievementTier
{
	EASY("Easy", 1),
	MEDIUM("Medium", 2),
	HARD("Hard", 3),
	ELITE("Elite", 4),
	MASTER("Master", 5),
	GRANDMASTER("Grandmaster", 6);

	private final String displayName;
	private final int pointsPerTask;

	AchievementTier(String displayName, int pointsPerTask)
	{
		this.displayName = displayName;
		this.pointsPerTask = pointsPerTask;
	}

	public String displayName()
	{
		return displayName;
	}

	public int pointsPerTask()
	{
		return pointsPerTask;
	}

	/** Resolves a tier from the wiki's display string (case-insensitive); null if unrecognized. */
	public static AchievementTier fromDisplayName(String name)
	{
		if (name == null)
		{
			return null;
		}
		String trimmed = name.trim();
		for (AchievementTier tier : values())
		{
			if (tier.displayName.equalsIgnoreCase(trimmed))
			{
				return tier;
			}
		}
		return null;
	}
}
