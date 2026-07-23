package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the "Unlock next" suggestions: for each quest the player has not finished, how many incomplete
 * CAs (and points) completing it would open up, weighed against the remaining effort to do that quest's
 * chain plus train its unmet skills. Kept deliberately separate from the doable-now CA ranking (a
 * decided design point) so it surfaces the "a few short quests beats an Elite CA" tradeoff without
 * muddying the main list. Pure and deterministic.
 */
public final class UnlockPlanner
{
	/**
	 * What a CA counts for when the player is not yet within reach of it. Small but non-zero: a quest is
	 * permanent progress, so opening content you cannot use today is still worth something. Zero made
	 * every quest tie at nothing for a brand-new account, and the ordering fell through to alphabetical —
	 * which put a 21-hour grandmaster questline above an 8-minute novice one.
	 */
	private static final double OUT_OF_REACH_WEIGHT = 0.15;

	private final QuestEffortLibrary questLib;
	private final SkillXpLibrary skillLib;

	public UnlockPlanner(QuestEffortLibrary questLib, SkillXpLibrary skillLib)
	{
		this.questLib = questLib == null ? QuestEffortLibrary.empty() : questLib;
		this.skillLib = skillLib == null ? SkillXpLibrary.empty() : skillLib;
	}

	public List<UnlockSuggestion> plan(List<CombatAchievement> allTasks, Set<Integer> completedTaskIds,
		EffortDataLibrary effortLib, PlayerProfile profile)
	{
		return plan(allTasks, completedTaskIds, effortLib, profile, TaskDifficultyLibrary.empty());
	}

	/**
	 * @param difficultyLib per-task pure-skill Difficulty, used to weight the unlocked points so a quest
	 *                      that opens easy CAs out-ranks one opening equally many hard CAs at the same
	 *                      time cost (empty ⇒ neutral, every unlock counts its full points).
	 */
	public List<UnlockSuggestion> plan(List<CombatAchievement> allTasks, Set<Integer> completedTaskIds,
		EffortDataLibrary effortLib, PlayerProfile profile, TaskDifficultyLibrary difficultyLib)
	{
		return plan(allTasks, completedTaskIds, effortLib, profile, difficultyLib, 1.0, 1.0);
	}

	/**
	 * @param unlockBias             exponent on a quest's achievable points in its score (1.0 = neutral;
	 *                               &gt;1 favours big-unlock quests, &lt;1 favours quick ones)
	 * @param unlockDifficultyWeight how strongly a hard CA's points are discounted (1.0 = the 3/difficulty
	 *                               curve; 0 = count every unlocked CA's points in full)
	 */
	public List<UnlockSuggestion> plan(List<CombatAchievement> allTasks, Set<Integer> completedTaskIds,
		EffortDataLibrary effortLib, PlayerProfile profile, TaskDifficultyLibrary difficultyLib,
		double unlockBias, double unlockDifficultyWeight)
	{
		return plan(allTasks, completedTaskIds, effortLib, profile, difficultyLib, unlockBias,
			unlockDifficultyWeight, RecStatsLibrary.empty());
	}

	/**
	 * @param recStatsLib curated recommended stats, used to judge which of a quest's unlocked CAs the
	 *                    player could actually do afterwards. A quest that opens nothing they are ready
	 *                    for is not a suggestion and is dropped. Empty ⇒ every unlocked CA counts, i.e.
	 *                    exactly the behaviour before readiness was considered.
	 */
	public List<UnlockSuggestion> plan(List<CombatAchievement> allTasks, Set<Integer> completedTaskIds,
		EffortDataLibrary effortLib, PlayerProfile profile, TaskDifficultyLibrary difficultyLib,
		double unlockBias, double unlockDifficultyWeight, RecStatsLibrary recStatsLib)
	{
		if (allTasks == null)
		{
			return new ArrayList<>();
		}
		Set<Integer> doneTaskIds = completedTaskIds == null ? java.util.Collections.emptySet() : completedTaskIds;
		EffortDataLibrary el = effortLib == null ? EffortDataLibrary.empty() : effortLib;
		PlayerProfile p = profile == null ? PlayerProfile.empty() : profile;
		TaskDifficultyLibrary diffs = difficultyLib == null ? TaskDifficultyLibrary.empty() : difficultyLib;
		RecStatsLibrary rec = recStatsLib == null ? RecStatsLibrary.empty() : recStatsLib;

		// quest name -> incomplete tasks it would unlock (skills + other gates already met)
		Map<String, List<CombatAchievement>> unlockedByQuest = new LinkedHashMap<>();
		for (CombatAchievement task : allTasks)
		{
			if (doneTaskIds.contains(task.id()))
			{
				continue;
			}
			TaskEffortData effort = el.effortFor(task.id());
			if (!effort.hasQuestGate() || !p.meets(effort.levelReqs()))
			{
				continue; // not a pure quest-unlock (no quest gate, or skills not yet met)
			}
			for (QuestRequirement req : effort.questReqs())
			{
				String quest = req.quest();
				// Honour startedSuffices: a "started is enough" gate the player has already STARTED is
				// satisfied, so this quest unlocks nothing here (else the CA is already doable and would
				// wrongly show as a locked "needs <quest>" card). Mirrors otherGatesMet / hasQuestAccess.
				boolean gateSatisfied = req.startedSuffices() ? p.hasStarted(quest) : p.hasCompleted(quest);
				if (quest.isEmpty() || gateSatisfied)
				{
					continue; // gate already satisfied — not an unlock
				}
				// Only attribute when every OTHER quest gate on this task is already satisfied.
				if (otherGatesMet(effort, p, quest))
				{
					unlockedByQuest.computeIfAbsent(quest, k -> new ArrayList<>()).add(task);
				}
			}
		}

		List<UnlockSuggestion> out = new ArrayList<>();
		for (Map.Entry<String, List<CombatAchievement>> e : unlockedByQuest.entrySet())
		{
			String quest = e.getKey();
			List<CombatAchievement> tasks = e.getValue();
			int points = tasks.stream().mapToInt(CombatAchievement::points).sum();

			Set<String> completed = p.completedQuestsLower();
			List<String> remainingPrereqs = new ArrayList<>();
			for (String pq : questLib.fullPrerequisites(quest))
			{
				if (!completed.contains(pq.trim().toLowerCase(Locale.ROOT)))
				{
					remainingPrereqs.add(pq);
				}
			}
			int questMinutes = questLib.remainingQuestMinutes(quest, completed);

			// Skill training cost: union the skill reqs of the quest + every remaining prereq, then
			// charge for the levels the player is short.
			Map<String, Integer> needed = new LinkedHashMap<>();
			Set<String> chain = new LinkedHashSet<>(remainingPrereqs);
			chain.add(quest);
			for (String q : chain)
			{
				for (Map.Entry<String, Integer> s : questLib.questFor(q).skillRequirements().entrySet())
				{
					needed.merge(s.getKey(), s.getValue(), Math::max);
				}
			}
			double trainingHours = 0.0;
			List<String> unmet = new ArrayList<>();
			Map<String, Integer> raised = new LinkedHashMap<>();
			for (Map.Entry<String, Integer> s : needed.entrySet())
			{
				int have = p.levelOf(s.getKey());
				if (have < s.getValue())
				{
					trainingHours += skillLib.hoursToTrain(s.getKey(), have, s.getValue());
					unmet.add(s.getKey() + " " + have + "→" + s.getValue());
					raised.put(s.getKey(), s.getValue()); // only ever upward — never lower a met skill
				}
			}

			// Judge readiness at the levels the player would HAVE once the chain's requirements were met,
			// not at today's: a quest that trains you on the way would otherwise look like it opens nothing.
			PlayerProfile after = raised.isEmpty() ? p : p.withLevels(raised);

			// The reachable part of the prize, and its difficulty-weighted value: an unlock opening easy CAs
			// is worth more per quest-minute than one opening equally many brutal CAs. CAs the player still
			// could not attempt afterwards count for nothing — a quest that opens 15 CAs you are 40 levels
			// short of has not opened 15 CAs.
			int reachableCount = 0;
			int reachablePoints = 0;
			double achievable = 0.0;
			for (CombatAchievement task : tasks)
			{
				boolean inReach = after.worstShortfall(rec.softFor(task.id()))
					<= TrainingPlanner.VIABLE_WORST_GAP;
				if (inReach)
				{
					reachableCount++;
					reachablePoints += task.points();
				}
				achievable += task.points()
					* achievableWeight(diffs.difficultyFor(task.id()).difficulty(), unlockDifficultyWeight)
					* (inReach ? 1.0 : OUT_OF_REACH_WEIGHT);
			}
			// A quest opening nothing reachable keeps a zero score and sinks to the bottom; it is not dropped
			// here because the Route's locked "needs <quest>" pile is built from this same list and is
			// deliberately aspirational. The "Unlock next" section is what filters on reachableTaskCount.
			achievable = Math.pow(Math.max(0.0, achievable), Math.max(0.0, unlockBias));

			List<Integer> taskIds = new ArrayList<>();
			for (CombatAchievement t : tasks)
			{
				taskIds.add(t.id());
			}
			out.add(new UnlockSuggestion(quest, questLib.questFor(quest).difficulty(), tasks.size(),
				points, reachableCount, reachablePoints, (int) Math.round(achievable), questMinutes,
				(int) Math.round(trainingHours * 60), remainingPrereqs, unmet, taskIds));
		}

		out.sort(Comparator.comparingDouble(UnlockSuggestion::score).reversed()
			.thenComparing(UnlockSuggestion::questName));
		return out;
	}

	/**
	 * Ease weight for a CA's pure-skill Difficulty (1–10): easy points count near full, hard points are
	 * discounted. Bounded to [0.3, 1.0] and never inflates above the raw points — neutral difficulty (3)
	 * and below map to 1.0. Mirrors the inverse of the quick-wins difficulty factor (difficulty / 3).
	 */
	private static double achievableWeight(int difficulty, double weight)
	{
		int d = Math.max(1, Math.min(10, difficulty));
		double base = 3.0 / d;                    // weight 1.0 → the current 3/difficulty discount
		double w = 1.0 - (1.0 - base) * weight;   // weight 0 → 1.0 (difficulty ignored, full points)
		return Math.max(0.3, Math.min(1.0, w));
	}

	private static boolean otherGatesMet(TaskEffortData effort, PlayerProfile p, String thisQuest)
	{
		for (QuestRequirement req : effort.questReqs())
		{
			if (req.quest().equalsIgnoreCase(thisQuest))
			{
				continue;
			}
			boolean ok = req.startedSuffices() ? p.hasStarted(req.quest()) : p.hasCompleted(req.quest());
			if (!ok)
			{
				return false;
			}
		}
		return true;
	}
}
