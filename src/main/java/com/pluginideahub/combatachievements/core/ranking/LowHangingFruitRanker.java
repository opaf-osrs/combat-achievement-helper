package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.effort.TaskTimeModel;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficulty;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ranks incomplete tasks by points-per-effort (highest first) so the genuinely cheapest progress is
 * surfaced. Pure: all live account facts arrive through {@link SignalsProvider}. Ordering is
 * deterministic (score desc, then task id asc) so it is test-pinnable. See docs/DESIGN.md §6b.
 */
public final class LowHangingFruitRanker
{
	/** Neutral difficulty: the UNKNOWN fallback (3) scores ×1, so no-data behaviour is unchanged. */
	private static final double NEUTRAL_DIFFICULTY = 3.0;

	private final EffortDataLibrary effortLib;
	private final EffortModel model;
	private final TaskDifficultyLibrary difficultyLib;
	/** Exponent on points in the score (1.0 = linear); &gt;1 favours high-point tasks, &lt;1 favours cheap ones. */
	private final double pointsWeight;
	/** How strongly pure-skill difficulty inflates effort (1.0 = the ÷3 pivot; 0 = ignore difficulty). */
	private final double difficultyWeight;

	public LowHangingFruitRanker(EffortDataLibrary effortLib, EffortModel model)
	{
		this(effortLib, model, TaskDifficultyLibrary.empty());
	}

	public LowHangingFruitRanker(EffortDataLibrary effortLib, EffortModel model,
		TaskDifficultyLibrary difficultyLib)
	{
		this(effortLib, model, difficultyLib, 1.0, 1.0);
	}

	public LowHangingFruitRanker(EffortDataLibrary effortLib, EffortModel model,
		TaskDifficultyLibrary difficultyLib, double pointsWeight, double difficultyWeight)
	{
		this.effortLib = effortLib == null ? EffortDataLibrary.empty() : effortLib;
		this.model = model == null ? EffortModel.standard() : model;
		this.difficultyLib = difficultyLib == null ? TaskDifficultyLibrary.empty() : difficultyLib;
		this.pointsWeight = Math.max(0.0, pointsWeight);
		this.difficultyWeight = Math.max(0.0, difficultyWeight);
	}

	/**
	 * Effort multiplier from a task's pure-skill {@link TaskDifficulty} (1–10): easy tasks cost less,
	 * hard ones much more. Centred so the neutral fallback (difficulty 3, {@link TaskDifficulty#UNKNOWN})
	 * is ×1 — behaviour is identical when no difficulty data is loaded. Difficulty 1 → ×0.33 (a strong
	 * boost), 10 → ×3.33 (heavy sink). This is the knob that puts pure-skill difficulty — not player
	 * completion % — in charge of the "easy wins" ordering, so a popular-but-hard boss like Jad no
	 * longer reads as easy.
	 */
	static double difficultyFactor(TaskDifficulty difficulty)
	{
		return difficultyFactor(difficulty, 1.0);
	}

	/**
	 * Effort multiplier from difficulty, dialled by {@code weight}: 1.0 = the ÷3 pivot (difficulty/3), 0 =
	 * ignore difficulty (×1), higher amplifies the spread. Clamped to stay positive.
	 */
	static double difficultyFactor(TaskDifficulty difficulty, double weight)
	{
		int d = difficulty == null ? TaskDifficulty.UNKNOWN.difficulty() : difficulty.difficulty();
		double base = d / NEUTRAL_DIFFICULTY;
		return Math.max(0.05, 1.0 + (base - 1.0) * weight);
	}

	/**
	 * @param candidates    tasks to consider (typically the whole dataset)
	 * @param completedIds  ids already done, excluded from the result
	 * @param signals       live per-task account facts
	 * @param doableNowOnly when true, drop tasks whose access/levels are not yet satisfied
	 */
	public List<RankedTask> rank(List<CombatAchievement> candidates, Set<Integer> completedIds,
		SignalsProvider signals, boolean doableNowOnly)
	{
		SignalsProvider resolved = signals == null ? SignalsProvider.defaults() : signals;
		List<RankedTask> ranked = new ArrayList<>();

		for (CombatAchievement task : candidates)
		{
			if (completedIds.contains(task.id()))
			{
				continue;
			}
			TaskEffortData effort = effortLib.effortFor(task.id());
			TaskLiveSignals sig = resolved.signalsFor(task.id());
			boolean doableNow = sig.doableNow();
			if (doableNowOnly && !doableNow)
			{
				continue;
			}
			TaskDifficulty difficulty = difficultyLib.difficultyFor(task.id());
			double effortValue = model.effortFor(task, effort, sig)
				* difficultyFactor(difficulty, difficultyWeight)
				* repetitionFactor(task)
				* RankedTask.recStatsSinkFactor(sig.recStatsShortfall());
			double score = effortValue <= 0 ? 0 : Math.pow(task.points(), pointsWeight) / effortValue;
			ranked.add(new RankedTask(task, effortValue, score, rationale(task, effort, sig),
				lockReason(effort, sig), doableNow, effort.curated(), difficulty, sig.recStatsShortfall()));
		}

		// An "entry" kill (kill the boss once) is completed for free by ANY other task at that boss — you
		// cannot do "kill Nex without her healing" or "kill Nex 25 times" without first killing Nex once. So
		// it can never really cost more than the cheapest task at its boss: clamp its effort down to that
		// minimum and rescore. Without this a harder, higher-point task at the same boss outranked the very
		// task it completes on the way (Nex Master, "kill 25 times", above Nex Veteran, "kill once").
		reprice(ranked);

		ranked.sort(Comparator
			.comparingDouble(RankedTask::score).reversed()
			.thenComparing(Comparator.comparing(RankedTask::isEntryKill).reversed())
			.thenComparingInt(rt -> rt.achievement().id()));
		return ranked;
	}

	/**
	 * Lifts each entry kill to at least the best score among its boss's tasks. Any of them completes the
	 * single kill for free, so the entry kill is worth no less than the best of them — including a
	 * higher-point grind like "kill 25 times", which the abstract effort model otherwise scores above a
	 * single kill purely on points. Paired with the entry-kill tie-break, this makes "kill it once" lead
	 * its boss.
	 */
	/**
	 * How much a task's required repetitions multiply its cost. The effort model scores gear, RNG, supplies
	 * and learning — all real things, but none of them notice that a task says "150 times". Without this,
	 * "Complete the Chambers of Xeric 150 times" carried exactly the same effort as "25 times" AND scored
	 * as cheaper than doing a single raid under a restriction, so points-per-effort put the longest grind
	 * in the game at the top of its own boss.
	 *
	 * <p>Square-root rather than linear, and deliberately gentle. This is a count, not a cost: 20 Abyssal
	 * Sire kills and 20 Chambers raids repeat the same number of times but are an hour and a day apart, so
	 * a steep curve punishes quick-boss grinds it has no business punishing (it flipped "kill the Sire 20
	 * times" below a one-attempt Master perfection task, which is plainly wrong). sqrt is enough to sink a
	 * long raid grind beneath every single-attempt task at its own boss without over-taxing short ones.
	 *
	 * <p>The honest fix is to cost tasks by their estimated MINUTES, which already multiply kills by
	 * time-to-kill; the ranker has no timing library, so this approximates it.</p>
	 */
	private static double repetitionFactor(CombatAchievement task)
	{
		int kills = TaskTimeModel.requiredKills(task.description());
		return kills <= 1 ? 1.0 : Math.sqrt(kills);
	}

	private void reprice(List<RankedTask> ranked)
	{
		Map<String, Double> bestByBoss = new HashMap<>();
		for (RankedTask rt : ranked)
		{
			String boss = rt.achievement().monster();
			if (boss == null || boss.isEmpty())
			{
				continue;
			}
			bestByBoss.merge(boss, rt.score(), Math::max);
		}
		for (int i = 0; i < ranked.size(); i++)
		{
			RankedTask rt = ranked.get(i);
			if (!rt.isEntryKill())
			{
				continue;
			}
			double best = bestByBoss.getOrDefault(rt.achievement().monster(), rt.score());
			if (best > rt.score())
			{
				ranked.set(i, rt.withScore(best));
			}
		}
	}

	private static String rationale(CombatAchievement task, TaskEffortData effort, TaskLiveSignals sig)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(task.type() == null ? "Task" : task.type().displayName());
		if (task.hasMonster())
		{
			sb.append(" · ").append(task.monster());
		}
		sb.append(" · ").append(effort.soloable() ? "soloable" : "group");
		if (sig.gearOwned())
		{
			sb.append(" · gear owned");
		}
		if (!sig.bossEngaged())
		{
			sb.append(" · new boss");
		}

		// Name the specific gate so "locked" is actionable rather than mysterious (one source of
		// truth: the same string surfaced as RankedTask.lockReason()).
		String lock = lockReason(effort, sig);
		if (!lock.isEmpty())
		{
			sb.append(" · ").append(lock);
		}
		return sb.toString();
	}

	/**
	 * A short, user-facing reason a task is not doable now, or "" when it is doable. Quest gates take
	 * precedence (most actionable), then unmet level requirements.
	 */
	private static String lockReason(TaskEffortData effort, TaskLiveSignals sig)
	{
		if (sig.doableNow())
		{
			return "";
		}
		if (!sig.accessMet() && effort.hasQuestGate())
		{
			return "needs " + questGateText(effort);
		}
		if (!sig.levelsMet())
		{
			return "level locked";
		}
		return "locked";
	}

	/** Comma-joined quest names for an unmet quest gate, e.g. "Dragon Slayer II, Regicide". */
	private static String questGateText(TaskEffortData effort)
	{
		StringBuilder sb = new StringBuilder();
		for (QuestRequirement req : effort.questReqs())
		{
			if (req == null || req.quest().isEmpty())
			{
				continue;
			}
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(req.quest());
		}
		return sb.toString();
	}
}
