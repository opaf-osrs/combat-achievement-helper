package com.pluginideahub.combatachievements.core.ranking;

/**
 * The hand-picked dials behind the recommendations, gathered in one place so adjusting them means
 * editing one file. None of these values has been validated against real player data yet — they are
 * judgement calls that produced sensible orderings on the accounts tried so far.
 */
public final class Tuning
{
	/** Effort assigned to a blocked task so it sinks below everything doable. */
	public static final double BLOCKING_PENALTY = 100.0;

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

	/** Neutral difficulty: the UNKNOWN fallback (3) scores ×1, so no-data behaviour is unchanged. */
	public static final double NEUTRAL_DIFFICULTY = 3.0;

	/** Nominal minutes for a "normal" task; only sets the scale of the effort numbers. */
	public static final double TIME_BASELINE_MINUTES = 10.0;

	// Speed-tier cost multipliers. Applied directly rather than through the difficulty term: curated
	// difficulty is already inside the minutes estimate, which is why that term is square-rooted, but the
	// gear/RNG gate on a speed CA is a separate signal that is in neither — halving it made it near
	// invisible (only 4 of 21 speed CAs moved at all).
	public static final double SPEED_TRIALIST_COST = 1.15;
	public static final double SPEED_CHASER_COST = 1.40;
	public static final double SPEED_RUNNER_COST = 1.80;

	/**
	 * What a CA counts for when the player is not yet within reach of it. Small but non-zero: a quest is
	 * permanent progress, so opening content you cannot use today is still worth something. Zero made
	 * every quest tie at nothing for a brand-new account, and the ordering fell through to alphabetical —
	 * which put a 21-hour grandmaster questline above an 8-minute novice one.
	 */
	public static final double OUT_OF_REACH_WEIGHT = 0.15;

	/**
	 * How many levels short you may be on the WORST single stat and still count as able to attempt a task.
	 * Deliberately the worst gap rather than the summed one: summing let a level-1 who trained only Prayer
	 * to 43 count Bryophyta as viable, because its 49-level combat gap still summed under the old limit.
	 *
	 * <p>Shared with the Route, which builds its path from the same "ready" set — so what the Route sends you
	 * to do and what "Train next" counts as opened up are one definition, not two that can drift.</p>
	 */
	public static final int VIABLE_WORST_GAP = 15;

	/** Rate assumed for a skill with no bracket covering the level being trained. */
	public static final int FALLBACK_XP_PER_HOUR = 30000;

	/**
	 * Combat level at which the beginner rule stops applying regardless of CA points. An established player
	 * who has simply never touched Combat Achievements starts at 0 points, and must not be treated as new.
	 */
	public static final int BEGINNER_COMBAT_LEVEL = 70;

	private Tuning()
	{
	}
}
