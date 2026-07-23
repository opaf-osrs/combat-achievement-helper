package com.pluginideahub.combatachievements.core.ranking;

/**
 * Live, per-task account facts the effort model blends with the static curated data: whether the
 * access gate and level requirements are met, whether the player already owns comparable gear, and
 * whether they have engaged the relevant boss (KC &gt; 0). The bridge resolves these from the client;
 * the pure ranker just consumes them. See docs/DESIGN.md §4.6/§6b.
 */
public final class TaskLiveSignals
{
	private final boolean accessMet;
	private final boolean levelsMet;
	private final boolean gearOwned;
	private final boolean bossEngaged;
	/** Levels short of this task's SOFT recommended stats (0 = meets them); scales a "below rec stats" sink. */
	private final int recStatsShortfall;

	public TaskLiveSignals(boolean accessMet, boolean levelsMet, boolean gearOwned, boolean bossEngaged)
	{
		this(accessMet, levelsMet, gearOwned, bossEngaged, 0);
	}

	public TaskLiveSignals(boolean accessMet, boolean levelsMet, boolean gearOwned, boolean bossEngaged,
		int recStatsShortfall)
	{
		this.accessMet = accessMet;
		this.levelsMet = levelsMet;
		this.gearOwned = gearOwned;
		this.bossEngaged = bossEngaged;
		this.recStatsShortfall = Math.max(0, recStatsShortfall);
	}

	/**
	 * Conservative default when no live data is available: nothing is treated as blocked (so tasks
	 * are not hidden), but gear/learning penalties still apply.
	 */
	public static TaskLiveSignals defaults()
	{
		return new TaskLiveSignals(true, true, false, false);
	}

	public boolean accessMet()
	{
		return accessMet;
	}

	public boolean levelsMet()
	{
		return levelsMet;
	}

	public boolean gearOwned()
	{
		return gearOwned;
	}

	public boolean bossEngaged()
	{
		return bossEngaged;
	}

	/** Levels short of the task's soft recommended stats; 0 when the player meets them. */
	public int recStatsShortfall()
	{
		return recStatsShortfall;
	}

	/** True when the player is below the task's soft recommended stats (a sink, not a gate). */
	public boolean belowRecStats()
	{
		return recStatsShortfall > 0;
	}

	/** A task is doable now when both its access gate and level requirements are satisfied. */
	public boolean doableNow()
	{
		return accessMet && levelsMet;
	}
}
