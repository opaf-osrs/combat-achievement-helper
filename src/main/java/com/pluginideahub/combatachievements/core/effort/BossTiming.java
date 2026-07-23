package com.pluginideahub.combatachievements.core.effort;

/**
 * Rough timing for one boss/activity: time-to-kill, between-kill downtime, and effective kills/hour.
 * Estimates (wiki + judgement) for a competently-geared player; for raids/minigames {@code ttkSeconds}
 * is one full run. Pure value object. Used with a task's kill count to estimate real grind time.
 */
public final class BossTiming
{
	/** The neutral fallback when a boss has no timing data. */
	public static final BossTiming UNKNOWN = new BossTiming(0, 0, 0, "");

	private final int ttkSeconds;
	private final int respawnSeconds;
	private final int killsPerHour;
	private final String note;
	private final double attemptsPerKill;

	public BossTiming(int ttkSeconds, int respawnSeconds, int killsPerHour, String note)
	{
		this(ttkSeconds, respawnSeconds, killsPerHour, note, 1.0);
	}

	public BossTiming(int ttkSeconds, int respawnSeconds, int killsPerHour, String note,
		double attemptsPerKill)
	{
		this.ttkSeconds = Math.max(0, ttkSeconds);
		this.respawnSeconds = Math.max(0, respawnSeconds);
		this.killsPerHour = Math.max(0, killsPerHour);
		this.note = note == null ? "" : note;
		this.attemptsPerKill = Math.max(1.0, attemptsPerKill);
	}

	public int ttkSeconds()
	{
		return ttkSeconds;
	}

	public int respawnSeconds()
	{
		return respawnSeconds;
	}

	public int killsPerHour()
	{
		return killsPerHour;
	}

	public String note()
	{
		return note;
	}

	/** Novice attempts (deaths/fails) per successful kill; ≥ 1. Multiplies a task's estimated time. */
	public double attemptsPerKill()
	{
		return attemptsPerKill;
	}

	public boolean isKnown()
	{
		return ttkSeconds > 0;
	}

	/** Seconds for a single completion including the downtime that precedes the next one. */
	public int secondsPerKill()
	{
		return ttkSeconds + respawnSeconds;
	}
}
