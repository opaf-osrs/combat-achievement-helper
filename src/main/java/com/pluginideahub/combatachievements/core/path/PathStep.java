package com.pluginideahub.combatachievements.core.path;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;

/** One step of an optimal path: a task to complete and the running point total after it. */
public final class PathStep
{
	private final CombatAchievement achievement;
	private final double effort;
	private final int cumulativePoints;

	public PathStep(CombatAchievement achievement, double effort, int cumulativePoints)
	{
		this.achievement = achievement;
		this.effort = effort;
		this.cumulativePoints = cumulativePoints;
	}

	public CombatAchievement achievement()
	{
		return achievement;
	}

	public double effort()
	{
		return effort;
	}

	public int cumulativePoints()
	{
		return cumulativePoints;
	}
}
