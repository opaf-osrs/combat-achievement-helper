package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estimates the real time to finish a single CA, in minutes. The core is
 * {@code requiredKills × attemptsPerCompletion × (TTK + respawn)}, where:
 * <ul>
 *   <li><b>requiredKills</b> subtracts live KC for Kill Count tasks (partial progress);</li>
 *   <li><b>attemptsPerCompletion</b> folds in the boss's <em>deadliness</em> — a curated per-CA attempts
 *       override, else the boss's attempts-per-kill (so a single kill of a brutal boss like an Awakened
 *       DT2 boss is no longer treated as "one quick kill") — and, for execution tasks, the type's base
 *       attempts × the difficulty-derived ability factor, all discounted by the player's competence.</li>
 * </ul>
 * Deadliness of 1 (every non-deadly boss) reproduces the previous behaviour exactly. All attempt
 * multipliers come from the curated {@link ScalingLibrary} (bundled {@code scaling.json}) — the single
 * place to tune them. Pure and deterministic.
 */
public final class TaskTimeModel
{
	private static final Pattern COUNT_TIMES =
		Pattern.compile("(\\d+)\\s*(?:times|kills?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern KILL_N =
		Pattern.compile("kill\\s+(\\d+)\\b", Pattern.CASE_INSENSITIVE);

	/** Fallback seconds-per-kill when a boss has no timing data. */
	private final int fallbackSecondsPerKill;
	private final ScalingLibrary scaling;

	public TaskTimeModel(int fallbackSecondsPerKill)
	{
		this(fallbackSecondsPerKill, ScalingLibrary.defaults());
	}

	public TaskTimeModel(int fallbackSecondsPerKill, ScalingLibrary scaling)
	{
		this.fallbackSecondsPerKill = Math.max(1, fallbackSecondsPerKill);
		this.scaling = scaling == null ? ScalingLibrary.defaults() : scaling;
	}

	/** Uses the built-in default scaling constants (for tests / when the bundled table is unavailable). */
	public static TaskTimeModel standard()
	{
		return new TaskTimeModel(120);
	}

	/** Uses the curated {@link ScalingLibrary} so sheet edits to the attempt multipliers take effect. */
	public static TaskTimeModel standard(ScalingLibrary scaling)
	{
		return new TaskTimeModel(120, scaling);
	}

	/** Minutes to do {@code kills} kills at a boss (used for incremental Kill Count milestones). */
	public int minutesForKills(int kills, BossTiming timing)
	{
		return minutesForKills(kills, timing, 1.0);
	}

	/**
	 * Minutes for {@code kills} kills, scaled by an attempt factor (the boss's competence-discounted
	 * deadliness) so incremental Kill Count milestones at a deadly boss also reflect its death rate.
	 */
	public int minutesForKills(int kills, BossTiming timing, double attemptFactor)
	{
		int spk = timing != null && timing.isKnown() ? timing.secondsPerKill() : fallbackSecondsPerKill;
		double factor = Math.max(1.0, attemptFactor);
		return (int) Math.ceil(Math.max(0, kills) * factor * spk / 60.0);
	}

	/** The competence-discount multiplier for a given kill count, via the curated thresholds/factors. */
	public double competenceFactorForKc(int kc)
	{
		return scaling.competenceFactor(scaling.competenceForKc(kc));
	}

	/** The completion/kill count stated in a task's wording (the grind-length signal); 1 if none. */
	public static int requiredKills(String description)
	{
		if (description == null)
		{
			return 1;
		}
		Matcher m = COUNT_TIMES.matcher(description);
		if (m.find())
		{
			return parseOr1(m.group(1));
		}
		m = KILL_N.matcher(description);
		if (m.find())
		{
			return parseOr1(m.group(1));
		}
		return 1;
	}

	private static int parseOr1(String s)
	{
		try
		{
			return Math.max(1, Integer.parseInt(s));
		}
		catch (RuntimeException ex)
		{
			return 1; // overflow / malformed → treat as a single completion
		}
	}

	/** The result of estimating one task's effort. */
	public static final class Estimate
	{
		private final int minutes;
		private final int requiredKills;
		private final int currentKc;
		private final int remainingKills;
		private final CombatExperience.Competence competence;
		private final int abilityRating;
		private final double killAttemptFactor;

		Estimate(int minutes, int requiredKills, int currentKc, int remainingKills,
			CombatExperience.Competence competence, int abilityRating, double killAttemptFactor)
		{
			this.minutes = minutes;
			this.requiredKills = requiredKills;
			this.currentKc = currentKc;
			this.remainingKills = remainingKills;
			this.competence = competence;
			this.abilityRating = abilityRating;
			this.killAttemptFactor = killAttemptFactor;
		}

		/** Mechanical execution difficulty 1 (trivial) .. 5 (very fiddly); 1 for pure Kill Count grinds. */
		public int abilityRating()
		{
			return abilityRating;
		}

		/** Attempts per kill applied (competence-discounted deadliness); ≥ 1. Used for incremental KC time. */
		public double killAttemptFactor()
		{
			return killAttemptFactor;
		}

		public int minutes()
		{
			return minutes;
		}

		public int requiredKills()
		{
			return requiredKills;
		}

		public int currentKc()
		{
			return currentKc;
		}

		public int remainingKills()
		{
			return remainingKills;
		}

		public CombatExperience.Competence competence()
		{
			return competence;
		}

		/** True when this is a multi-kill task the player has already made progress on. */
		public boolean hasPartialProgress()
		{
			return requiredKills > 1 && currentKc > 0 && remainingKills < requiredKills;
		}
	}

	/**
	 * Mechanical execution difficulty 1..5 for an execution task, derived from the curated pure-skill
	 * Difficulty (1..10) — which already folds in the task's keyword bumps. Two difficulty points per
	 * rung: mid difficulty (5–6) maps to the neutral rating 3.
	 */
	static int abilityRating(int difficulty)
	{
		int d = Math.max(1, Math.min(10, difficulty));
		return Math.max(1, Math.min(5, (int) Math.round(d / 2.0)));
	}

	public Estimate estimate(CombatAchievement task, BossTiming timing, CombatExperience experience)
	{
		return estimate(task, timing, experience, 3, 0.0);
	}

	public Estimate estimate(CombatAchievement task, BossTiming timing, CombatExperience experience,
		int difficulty)
	{
		return estimate(task, timing, experience, difficulty, 0.0);
	}

	/**
	 * @param difficulty       the task's curated pure-skill Difficulty (1..10); the execution-attempt seed
	 * @param attemptsOverride curated attempts-to-complete for this CA (0 ⇒ use the boss's attempts/kill)
	 */
	public Estimate estimate(CombatAchievement task, BossTiming timing, CombatExperience experience,
		int difficulty, double attemptsOverride)
	{
		CombatExperience exp = experience == null ? CombatExperience.empty() : experience;
		BossTiming t = timing == null ? BossTiming.UNKNOWN : timing;
		int spk = t.isKnown() ? t.secondsPerKill() : fallbackSecondsPerKill;

		int required = requiredKills(task.description());
		int kc = task.hasMonster() ? exp.kcFor(task.monster()) : 0;
		CombatExperience.Competence comp = scaling.competenceForKc(kc);
		double cf = scaling.competenceFactor(comp);

		// Deaths/fails per successful kill: a curated per-CA override wins, else the boss's deadliness.
		// Only the attempts BEYOND the guaranteed first are discounted by competence, so deadliness 1
		// leaves the estimate exactly as it was before this signal existed.
		double deadliness = attemptsOverride > 0 ? attemptsOverride : t.attemptsPerKill();
		double deadlinessFactor = 1.0 + Math.max(0.0, deadliness - 1.0) * cf;

		double effectiveKills;
		int remaining;
		int rating;
		if (task.type() == TaskType.KILL_COUNT)
		{
			// Pure grind: live KC is genuine progress and is subtracted; then scaled by the death rate.
			remaining = Math.max(0, required - kc);
			rating = 1;
			effectiveKills = remaining * deadlinessFactor;
		}
		else
		{
			remaining = required;
			rating = abilityRating(difficulty);
			if (attemptsOverride > 0)
			{
				// A curated/research per-CA override is the TOTAL attempts-to-complete (survival + skill
				// already folded in), so it is authoritative — use it directly rather than multiplying it
				// by the type × ability estimate (which would double-count the skill component).
				effectiveKills = required * deadlinessFactor;
			}
			else
			{
				// No override: each success may take several attempts (type × ability × competence), all
				// further scaled by the boss's deadliness (you must survive to even attempt the constraint).
				double execution = Math.max(1.0,
					scaling.baseAttempts(task.type()) * scaling.abilityFactor(rating) * cf);
				effectiveKills = required * execution * deadlinessFactor;
			}
		}

		int minutes = (int) Math.ceil(effectiveKills * spk / 60.0);
		return new Estimate(minutes, required, kc, remaining, comp, rating, deadlinessFactor);
	}
}
