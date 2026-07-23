package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import java.util.Collections;
import java.util.List;

/**
 * A recommended one-trip bundle of CAs at a single boss/activity. The whole point of synergy ranking:
 * doing several CAs in one visit amortises the fixed trip overhead, so a boss with many doable CAs is
 * more efficient per point than scattering. Pure value object.
 */
public final class BossSession
{
	/** A task within a session, paired with its time estimate and its real contribution to the trip. */
	public static final class Item
	{
		private final CombatAchievement task;
		private final TaskTimeModel.Estimate estimate;
		private final int sessionMinutes;

		public Item(CombatAchievement task, TaskTimeModel.Estimate estimate, int sessionMinutes)
		{
			this.task = task;
			this.estimate = estimate;
			this.sessionMinutes = sessionMinutes;
		}

		public CombatAchievement task()
		{
			return task;
		}

		public TaskTimeModel.Estimate estimate()
		{
			return estimate;
		}

		/**
		 * Incremental minutes this task adds to the trip. For stacked Kill Count tasks this is only the
		 * kills needed to reach this threshold from the one below it — so a lower rung (e.g. "kill 25")
		 * stays a viable short task on its own, while the rungs still sum to a single grind, not N grinds.
		 */
		public int sessionMinutes()
		{
			return sessionMinutes;
		}
	}

	private final String monster;
	private final List<Item> items;
	private final int totalPoints;
	private final int totalMinutes;
	private final double score;

	public BossSession(String monster, List<Item> items, int totalPoints, int totalMinutes, double score)
	{
		this.monster = monster == null ? "" : monster;
		this.items = Collections.unmodifiableList(items);
		this.totalPoints = totalPoints;
		this.totalMinutes = totalMinutes;
		this.score = score;
	}

	public String monster()
	{
		return monster;
	}

	public List<Item> items()
	{
		return items;
	}

	public int taskCount()
	{
		return items.size();
	}

	public int totalPoints()
	{
		return totalPoints;
	}

	public int totalMinutes()
	{
		return totalMinutes;
	}

	/** Points per amortised minute (incl. trip overhead) — higher is a better next session. */
	public double score()
	{
		return score;
	}

	/**
	 * Points per hour for this session — {@link #score()} expressed per hour rather than per minute,
	 * so it reads as the same headline efficiency number the curation spreadsheet uses.
	 */
	public double pointsPerHour()
	{
		// Computed from totals (not score * 60) so the degenerate zero-minute case yields 0 rather than
		// an inflated points × 60; in every real session (minutes ≥ 1) this equals score × 60 exactly.
		return totalMinutes <= 0 ? 0.0 : totalPoints * 60.0 / totalMinutes;
	}
}
