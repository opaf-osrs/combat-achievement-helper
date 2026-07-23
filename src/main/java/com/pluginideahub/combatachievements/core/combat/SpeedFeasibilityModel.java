package com.pluginideahub.combatachievements.core.combat;

/**
 * Judges whether a Speed/time-trial CA is achievable: estimated clear time vs the task's time
 * threshold. For single-boss tasks the estimate is {@code hitpoints / bestDps × (1 + downtimeFactor)};
 * for multi-stage encounters (Gauntlet, Inferno, raids, Colosseum) a curated
 * {@code referenceClearSeconds} is supplied instead, since they have no single HP bar. See
 * docs/SYSTEMS-DESIGN.md §5.3.
 */
public final class SpeedFeasibilityModel
{
	/** Margin at/above which a feasible task is considered comfortable rather than tight. */
	private static final double COMFORTABLE_MARGIN = 1.15;

	public enum Verdict
	{
		FEASIBLE, TIGHT, INFEASIBLE, INSUFFICIENT_DATA
	}

	public static final class Result
	{
		private final Verdict verdict;
		private final double estimatedClearSeconds;
		private final double thresholdSeconds;
		private final double marginRatio;

		Result(Verdict verdict, double estimatedClearSeconds, double thresholdSeconds, double marginRatio)
		{
			this.verdict = verdict;
			this.estimatedClearSeconds = estimatedClearSeconds;
			this.thresholdSeconds = thresholdSeconds;
			this.marginRatio = marginRatio;
		}

		public Verdict verdict()
		{
			return verdict;
		}

		public double estimatedClearSeconds()
		{
			return estimatedClearSeconds;
		}

		public double thresholdSeconds()
		{
			return thresholdSeconds;
		}

		/** thresholdSeconds / estimatedClearSeconds — >1 = feasible, the higher the more comfortable. */
		public double marginRatio()
		{
			return marginRatio;
		}
	}

	private SpeedFeasibilityModel()
	{
	}

	public static final Result INSUFFICIENT = new Result(Verdict.INSUFFICIENT_DATA, 0, 0, 0);

	/**
	 * @param hitpoints            boss HP (single-target); ignored when {@code referenceClearSeconds > 0}
	 * @param bestDps              best DPS across styles (single-target path)
	 * @param thresholdSeconds     the CA's time limit
	 * @param downtimeFactor       per-boss mechanics/downtime fudge (e.g. 0.1 tank-and-spank, 0.6 mechanic-heavy)
	 * @param referenceClearSeconds curated clear time for multi-stage encounters; ≤0 = use HP/DPS
	 */
	public static Result evaluate(int hitpoints, double bestDps, double thresholdSeconds,
		double downtimeFactor, double referenceClearSeconds)
	{
		if (thresholdSeconds <= 0)
		{
			return INSUFFICIENT;
		}

		double estimatedClear;
		if (referenceClearSeconds > 0)
		{
			estimatedClear = referenceClearSeconds;
		}
		else if (hitpoints > 0 && bestDps > 0)
		{
			double ttk = hitpoints / bestDps;
			estimatedClear = ttk * (1.0 + Math.max(0.0, downtimeFactor));
		}
		else
		{
			return INSUFFICIENT;
		}

		double margin = thresholdSeconds / estimatedClear;
		Verdict verdict;
		if (margin >= COMFORTABLE_MARGIN)
		{
			verdict = Verdict.FEASIBLE;
		}
		else if (margin >= 1.0)
		{
			verdict = Verdict.TIGHT;
		}
		else
		{
			verdict = Verdict.INFEASIBLE;
		}
		return new Result(verdict, estimatedClear, thresholdSeconds, margin);
	}
}
