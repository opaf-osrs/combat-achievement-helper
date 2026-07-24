package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficulty;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import com.pluginideahub.combatachievements.core.effort.TaskTimeModel;

/**
 * A task scored by the low-hanging-fruit ranker: its computed effort, its points-per-effort score,
 * a short human rationale, its pure-skill {@link TaskDifficulty}, and whether it is doable right now.
 * Pure value object.
 */
public final class RankedTask
{
	/** See {@link Tuning#REC_STATS_SINK_SCALE}. */
	public static final double REC_STATS_SINK_SCALE = Tuning.REC_STATS_SINK_SCALE;
	public static final double REC_STATS_SINK_MAX = Tuning.REC_STATS_SINK_MAX;

	private final CombatAchievement achievement;
	private final double effort;
	private final double score;
	private final String rationale;
	private final String lockReason;
	private final boolean doableNow;
	private final boolean curated;
	private final TaskDifficulty difficulty;
	private final int recStatsShortfall;

	public RankedTask(CombatAchievement achievement, double effort, double score, String rationale,
		String lockReason, boolean doableNow, boolean curated, TaskDifficulty difficulty)
	{
		this(achievement, effort, score, rationale, lockReason, doableNow, curated, difficulty, 0);
	}

	public RankedTask(CombatAchievement achievement, double effort, double score, String rationale,
		String lockReason, boolean doableNow, boolean curated, TaskDifficulty difficulty,
		int recStatsShortfall)
	{
		this(builder()
			.achievement(achievement)
			.effort(effort)
			.score(score)
			.rationale(rationale)
			.lockReason(lockReason)
			.doableNow(doableNow)
			.curated(curated)
			.difficulty(difficulty)
			.recStatsShortfall(recStatsShortfall));
	}

	private RankedTask(Builder builder)
	{
		this.achievement = builder.achievement;
		this.effort = builder.effort;
		this.score = builder.score;
		this.rationale = builder.rationale;
		this.lockReason = builder.lockReason == null ? "" : builder.lockReason;
		this.doableNow = builder.doableNow;
		this.curated = builder.curated;
		this.difficulty = builder.difficulty == null ? TaskDifficulty.UNKNOWN : builder.difficulty;
		this.recStatsShortfall = Math.max(0, builder.recStatsShortfall);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	/** A builder pre-filled with every field of this task, for copies that change just one of them. */
	public Builder copy()
	{
		return builder()
			.achievement(achievement)
			.effort(effort)
			.score(score)
			.rationale(rationale)
			.lockReason(lockReason)
			.doableNow(doableNow)
			.curated(curated)
			.difficulty(difficulty)
			.recStatsShortfall(recStatsShortfall);
	}

	/** Names each field at the call site; unset fields keep their neutral defaults. */
	public static final class Builder
	{
		private CombatAchievement achievement;
		private double effort;
		private double score;
		private String rationale;
		private String lockReason = "";
		private boolean doableNow;
		private boolean curated;
		private TaskDifficulty difficulty = TaskDifficulty.UNKNOWN;
		private int recStatsShortfall;

		public Builder achievement(CombatAchievement achievement)
		{
			this.achievement = achievement;
			return this;
		}

		public Builder effort(double effort)
		{
			this.effort = effort;
			return this;
		}

		public Builder score(double score)
		{
			this.score = score;
			return this;
		}

		public Builder rationale(String rationale)
		{
			this.rationale = rationale;
			return this;
		}

		public Builder lockReason(String lockReason)
		{
			this.lockReason = lockReason;
			return this;
		}

		public Builder doableNow(boolean doableNow)
		{
			this.doableNow = doableNow;
			return this;
		}

		public Builder curated(boolean curated)
		{
			this.curated = curated;
			return this;
		}

		public Builder difficulty(TaskDifficulty difficulty)
		{
			this.difficulty = difficulty;
			return this;
		}

		public Builder recStatsShortfall(int recStatsShortfall)
		{
			this.recStatsShortfall = recStatsShortfall;
			return this;
		}

		public RankedTask build()
		{
			return new RankedTask(this);
		}
	}

	public CombatAchievement achievement()
	{
		return achievement;
	}

	public double effort()
	{
		return effort;
	}

	public double score()
	{
		return score;
	}

	/**
	 * An "entry" task: a Kill Count task needing a single kill of its boss. It is completed for free by
	 * ANY other task at that boss — you cannot do "kill Nex without her healing" without first killing Nex
	 * — so it should never rank behind a harder task at the same boss just because that task is worth an
	 * extra point. The ranker floats these to the front so a boss's "kill it once" always leads.
	 */
	public boolean isEntryKill()
	{
		return achievement.type() == TaskType.KILL_COUNT
			&& TaskTimeModel.requiredKills(achievement.description()) <= 1;
	}

	/**
	 * A copy with the score raised to {@code newScore}. Used to lift an entry kill to the value of the
	 * best task at its boss, which completes it for free. Effort is left as-is (it is the real effort of
	 * the single kill); only the ranking value changes.
	 */
	public RankedTask withScore(double newScore)
	{
		return copy().score(Math.max(score, newScore)).build();
	}

	public String rationale()
	{
		return rationale;
	}

	/**
	 * A short, user-facing reason this task is not doable now (e.g. "needs Dragon Slayer II" or
	 * "level locked"), or "" when the task is doable. Surfaced on locked quick-win cards.
	 */
	public String lockReason()
	{
		return lockReason;
	}

	public boolean doableNow()
	{
		return doableNow;
	}

	/** The task's pure-skill Difficulty (1–10 + breakdown), for card display and the Easiest sort. */
	public TaskDifficulty difficulty()
	{
		return difficulty;
	}

	/** False when the effort estimate came from the {@code NEUTRAL} fallback (no curated entry). */
	public boolean curated()
	{
		return curated;
	}

	/**
	 * Summed levels the player is below this task's SOFT recommended stats (0 = meets them). Used to sink
	 * attemptable-but-underlevelled endgame content in both the CA ranking and the Route cost.
	 */
	public int recStatsShortfall()
	{
		return recStatsShortfall;
	}

	/**
	 * True when the player is below this task's SOFT recommended stats — the task is attemptable but was
	 * sunk in the ranking (a fresh account shouldn't be told to do endgame content it can technically enter).
	 */
	public boolean belowRecStats()
	{
		return recStatsShortfall > 0;
	}

	/** This task's soft rec-stats effort/cost multiplier (1.0 when met). */
	public double recStatsSinkFactor()
	{
		return recStatsSinkFactor(recStatsShortfall);
	}

	/**
	 * The soft rec-stats multiplier for a given shortfall: 1.0 when the player meets the recommended stats,
	 * then {@code 1 + (shortfall/scale)²} — the further below, the disproportionately heavier the weight
	 * against it. The single source of truth for the sink, shared by the CAs ranker, the Route cost, and the
	 * Sessions/unlock ordering, so every recommendation surface weights under-levelled content the same way.
	 */
	public static double recStatsSinkFactor(int shortfall)
	{
		if (shortfall <= 0)
		{
			return 1.0;
		}
		double distance = shortfall / REC_STATS_SINK_SCALE;
		return Math.min(REC_STATS_SINK_MAX, 1.0 + distance * distance);
	}
}
