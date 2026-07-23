package com.pluginideahub.combatachievements.core.achievement;

import java.util.Objects;

/**
 * A task's pure-skill <b>Difficulty</b> on the 1–10 scale = curated per-boss difficulty + task-level
 * keyword bumps (no-supplies, no-damage, speed, …). Measures how hard the task is to pull off,
 * independent of grind length (that is the effort/time model's job). Immutable value. Curated in
 * {@code data/curation} and bundled as {@code task_difficulty.json}; see CONTEXT.md "Difficulty".
 */
public final class TaskDifficulty
{
	/** Fallback when a task has no curated difficulty — the curation sheet's VLOOKUP default (3). */
	public static final TaskDifficulty UNKNOWN = new TaskDifficulty(3, 3, 0.0, "");

	private final int difficulty;
	private final int bossDifficulty;
	private final double bump;
	private final String reason;
	private final double attemptsOverride;

	public TaskDifficulty(int difficulty, int bossDifficulty, double bump, String reason)
	{
		this(difficulty, bossDifficulty, bump, reason, 0.0);
	}

	public TaskDifficulty(int difficulty, int bossDifficulty, double bump, String reason,
		double attemptsOverride)
	{
		this.difficulty = Math.max(1, Math.min(10, difficulty));
		this.bossDifficulty = Math.max(0, Math.min(10, bossDifficulty));
		this.bump = bump;
		this.reason = reason == null ? "" : reason;
		this.attemptsOverride = Math.max(0.0, attemptsOverride);
	}

	/** Pure-skill difficulty, 1 (trivial) … 10 (Inferno / ToB Hard Mode). Always in [1, 10]. */
	public int difficulty()
	{
		return difficulty;
	}

	/** The per-boss base rating this was built on, or 0 when the boss was unknown. */
	public int bossDifficulty()
	{
		return bossDifficulty;
	}

	/** Task-level keyword adjustment applied on top of the boss rating (0 for plain kill-count tasks). */
	public double bump()
	{
		return bump;
	}

	/** Short human tag describing the bump (e.g. "no-damage", "speed", "kill count"). */
	public String reason()
	{
		return reason;
	}

	/** Curated attempts-to-complete for this specific CA (0 = none; use the boss's attempts/kill). */
	public double attemptsOverride()
	{
		return attemptsOverride;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof TaskDifficulty))
		{
			return false;
		}
		TaskDifficulty that = (TaskDifficulty) o;
		return difficulty == that.difficulty
			&& bossDifficulty == that.bossDifficulty
			&& Double.compare(bump, that.bump) == 0
			&& Double.compare(attemptsOverride, that.attemptsOverride) == 0
			&& reason.equals(that.reason);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(difficulty, bossDifficulty, bump, reason, attemptsOverride);
	}

	@Override
	public String toString()
	{
		return "TaskDifficulty{difficulty=" + difficulty + ", boss=" + bossDifficulty
			+ ", bump=" + bump + ", reason='" + reason + "', attempts=" + attemptsOverride + "}";
	}
}
