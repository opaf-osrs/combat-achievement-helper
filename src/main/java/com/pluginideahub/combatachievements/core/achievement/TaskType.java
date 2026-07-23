package com.pluginideahub.combatachievements.core.achievement;

/**
 * The six wiki-defined Combat Achievement task types. The {@code baseEffort} weight feeds the
 * low-hanging-fruit ranker (see {@code core.ranking.EffortModel} and docs/DESIGN.md §6b.1); it is a
 * heuristic ordering of "how fiddly is this kind of task", not a wiki fact.
 */
public enum TaskType
{
	KILL_COUNT("Kill Count", 1.0),
	STAMINA("Stamina", 1.5),
	MECHANICAL("Mechanical", 2.0),
	RESTRICTION("Restriction", 2.5),
	SPEED("Speed", 3.0),
	PERFECTION("Perfection", 4.0);

	private final String displayName;
	private final double baseEffort;

	TaskType(String displayName, double baseEffort)
	{
		this.displayName = displayName;
		this.baseEffort = baseEffort;
	}

	public String displayName()
	{
		return displayName;
	}

	public double baseEffort()
	{
		return baseEffort;
	}

	/** Resolves a type from the wiki's display string (case-insensitive); null if unrecognized. */
	public static TaskType fromDisplayName(String name)
	{
		if (name == null)
		{
			return null;
		}
		String trimmed = name.trim();
		for (TaskType type : values())
		{
			if (type.displayName.equalsIgnoreCase(trimmed))
			{
				return type;
			}
		}
		return null;
	}
}
