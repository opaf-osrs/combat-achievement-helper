package com.pluginideahub.combatachievements.core.achievement;

import java.util.Objects;

/**
 * One Combat Achievement task. Immutable value object (Java 11 — no records); accessor methods are
 * named after the field, mirroring roguescape's {@code RoomDefinition}/{@code CampaignDefinition}.
 * {@code points} is derived from {@code tier} at load time.
 */
public final class CombatAchievement
{
	private final int id;
	private final String name;
	private final AchievementTier tier;
	private final String monster;
	private final TaskType type;
	private final int points;
	private final String description;
	private final String leagueRegion;
	private final String wikiUrl;

	public CombatAchievement(int id, String name, AchievementTier tier, String monster, TaskType type,
		int points, String description, String leagueRegion, String wikiUrl)
	{
		this.id = id;
		this.name = name == null ? "" : name;
		this.tier = tier;
		this.monster = monster == null ? "" : monster;
		this.type = type;
		this.points = points;
		this.description = description == null ? "" : description;
		this.leagueRegion = leagueRegion == null ? "" : leagueRegion;
		this.wikiUrl = wikiUrl == null ? "" : wikiUrl;
	}

	public int id()
	{
		return id;
	}

	public String name()
	{
		return name;
	}

	public AchievementTier tier()
	{
		return tier;
	}

	public String monster()
	{
		return monster;
	}

	public TaskType type()
	{
		return type;
	}

	public int points()
	{
		return points;
	}

	public String description()
	{
		return description;
	}

	public String leagueRegion()
	{
		return leagueRegion;
	}

	public String wikiUrl()
	{
		return wikiUrl;
	}

	/** True when this task is bound to a specific monster (vs a non-boss activity task). */
	public boolean hasMonster()
	{
		return !monster.isEmpty() && !"None".equalsIgnoreCase(monster);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof CombatAchievement))
		{
			return false;
		}
		return id == ((CombatAchievement) o).id;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public String toString()
	{
		return "CombatAchievement{" + id + ", " + name + ", " + tier + '}';
	}
}
