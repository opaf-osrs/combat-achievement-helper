package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficulty;

/**
 * A task scored by the low-hanging-fruit ranker: its computed effort, its points-per-effort score,
 * a short human rationale, its pure-skill {@link TaskDifficulty}, and whether it is doable right now.
 * Pure value object.
 */
public final class RankedTask
{
	/**
	 * Soft "below recommended stats" sink. The penalty grows with the SQUARE of how far below a task's
	 * recommended combat stats you are, so distance outruns points: a linear penalty let a 4-point task
	 * 4× further away still come out cheaper per point (Death to the Archer King, distance 311, only cost
	 * ×7.2 vs an Easy task's ×2.6 at distance 78 — so a level-3's route picked Dagannoth Kings). Squared,
	 * being 4× further costs ~16×, so it can never be "good value" to send an under-levelled account at
	 * harder content. At the scale below, 40 summed levels short = ×2, 80 = ×5, 160 = ×17.
	 */
	public static final double REC_STATS_SINK_SCALE = 40.0;
	public static final double REC_STATS_SINK_MAX = 100.0;

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
		this.achievement = achievement;
		this.effort = effort;
		this.score = score;
		this.rationale = rationale;
		this.lockReason = lockReason == null ? "" : lockReason;
		this.doableNow = doableNow;
		this.curated = curated;
		this.difficulty = difficulty == null ? TaskDifficulty.UNKNOWN : difficulty;
		this.recStatsShortfall = Math.max(0, recStatsShortfall);
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
