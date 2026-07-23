package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;

/**
 * Pure, tunable effort heuristic. Effort is dimensionless — only relative ordering matters. The
 * concrete default weights are documented in docs/DESIGN.md §6b.1. A higher {@code sensitivity}
 * amplifies the penalty terms (0 = ignore penalties → rank on type+points only).
 *
 * <pre>
 *   penaltySum = gearGap + questGap + groupPenalty + rngPenalty + learningPenalty + supplyPenalty
 *   effort     = typeBaseWeight * (1 + sensitivity * penaltySum)
 *   score      = points / effort        // higher = better low-hanging fruit
 * </pre>
 */
public final class EffortModel
{
	/** Effort assigned to a blocked task so it sinks below everything doable. */
	public static final double BLOCKING_PENALTY = 100.0;

	private final double sensitivity;
	private final double gearWeight;
	private final double rngWeight;
	private final double supplyWeight;
	private final double groupWeight;
	private final double learningWeight;

	public EffortModel(double sensitivity)
	{
		this(sensitivity, 1.0, 1.0, 1.0, 1.0, 1.0);
	}

	/**
	 * Full constructor: {@code sensitivity} scales all penalties together; the five per-category weights
	 * (each 1.0 = neutral) let gear / RNG / supply / group / learning penalties be dialled independently.
	 */
	public EffortModel(double sensitivity, double gearWeight, double rngWeight, double supplyWeight,
		double groupWeight, double learningWeight)
	{
		this.sensitivity = Math.max(0.0, sensitivity);
		this.gearWeight = Math.max(0.0, gearWeight);
		this.rngWeight = Math.max(0.0, rngWeight);
		this.supplyWeight = Math.max(0.0, supplyWeight);
		this.groupWeight = Math.max(0.0, groupWeight);
		this.learningWeight = Math.max(0.0, learningWeight);
	}

	/** Neutral sensitivity (×1.0), matching the config slider's default of 50. */
	public static EffortModel standard()
	{
		return new EffortModel(1.0);
	}

	/** Maps the config slider (0..100, 50 = neutral) to a sensitivity multiplier. */
	public static EffortModel fromConfig(int effortSensitivity)
	{
		int clamped = Math.max(0, Math.min(100, effortSensitivity));
		return new EffortModel(clamped / 50.0);
	}

	/** Full config mapping: the sensitivity slider plus the five per-category weights (each 1.0 = neutral). */
	public static EffortModel fromConfig(int effortSensitivity, double gearWeight, double rngWeight,
		double supplyWeight, double groupWeight, double learningWeight)
	{
		int clamped = Math.max(0, Math.min(100, effortSensitivity));
		return new EffortModel(clamped / 50.0, gearWeight, rngWeight, supplyWeight, groupWeight, learningWeight);
	}

	public double sensitivity()
	{
		return sensitivity;
	}

	public double gearGap(TaskEffortData.GearTier tier, boolean gearOwned)
	{
		if (gearOwned)
		{
			return 0.0;
		}
		switch (tier)
		{
			case LOW: return 0.0;
			case MID: return 0.3;
			case HIGH: return 0.7;
			case BIS: return 1.2;
			default: return 0.3;
		}
	}

	public double rngPenalty(TaskEffortData.Intensity rng)
	{
		switch (rng)
		{
			case NONE: return 0.0;
			case LOW: return 0.15;
			case MED: return 0.4;
			case HIGH: return 0.9;
			default: return 0.15;
		}
	}

	public double supplyPenalty(TaskEffortData.Intensity supply)
	{
		switch (supply)
		{
			case NONE: return 0.0;
			case LOW: return 0.1;
			case MED: return 0.3;
			case HIGH: return 0.6;
			default: return 0.1;
		}
	}

	public double groupPenalty(TaskEffortData effort)
	{
		if (effort.soloable())
		{
			return 0.0;
		}
		// Team-only raid content (ToB/CoX CM, etc.) is the heaviest; other group content is lighter.
		return effort.minigameOrRaid().isEmpty() ? 0.4 : 1.0;
	}

	public double learningPenalty(boolean bossEngaged)
	{
		return bossEngaged ? 0.0 : 0.5;
	}

	/** Penalty for an unmet access gate or level requirement — dominates so blocked tasks sink. */
	public double questGap(TaskLiveSignals signals)
	{
		double penalty = 0.0;
		if (!signals.accessMet())
		{
			penalty += BLOCKING_PENALTY;
		}
		if (!signals.levelsMet())
		{
			penalty += BLOCKING_PENALTY;
		}
		return penalty;
	}

	/** Total effort for a task given its curated data and live signals. Always &gt; 0. */
	public double effortFor(CombatAchievement task, TaskEffortData effort, TaskLiveSignals signals)
	{
		double penaltySum =
			gearWeight * gearGap(effort.gearTier(), signals.gearOwned())
				+ questGap(signals) // the doable-now gate — never weighted down
				+ groupWeight * groupPenalty(effort)
				+ rngWeight * rngPenalty(effort.rng())
				+ supplyWeight * supplyPenalty(effort.supply())
				+ learningWeight * learningPenalty(signals.bossEngaged());

		double base = task.type() == null ? 1.0 : task.type().baseEffort();
		return base * (1.0 + sensitivity * penaltySum);
	}
}
