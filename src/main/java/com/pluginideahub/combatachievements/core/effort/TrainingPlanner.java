package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.StatRequirement;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.TaskLiveSignals;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Answers "what should I train to open up Combat Achievements?" — the counterpart to {@link UnlockPlanner},
 * which answers the same question for quests.
 *
 * <p>A task counts as VIABLE when it is both doable (gates satisfied) and not hopelessly under-levelled
 * (its recommended-stat distance is within reach). Training can flip a task to viable two different ways —
 * by clearing a hard gate (Firemaking 50 → Wintertodt) or by pulling the readiness distance down (combat) —
 * and counting newly-viable tasks captures both without needing to special-case them.</p>
 *
 * <p>Self-suppressing: an account that already meets everything gains nothing from any goal, so every
 * candidate scores zero and the result is empty. Only players who are actually held back see suggestions.</p>
 */
public final class TrainingPlanner
{
	/**
	 * How many levels short you may be on the WORST single stat and still count as able to attempt a task.
	 * Deliberately the worst gap rather than the summed one: summing let a level-1 who trained only Prayer
	 * to 43 count Bryophyta as viable, because its 49-level combat gap still summed under the old limit.
	 *
	 * <p>Shared with the Route, which builds its path from the same "ready" set — so what the Route sends you
	 * to do and what "Train next" counts as opened up are one definition, not two that can drift.</p>
	 */
	public static final int VIABLE_WORST_GAP = 15;

	/** Combat recommendations almost always want the whole kit, so raise these together. */
	private static final String[] COMBAT_KIT = {
		"Attack", "Strength", "Defence", "Ranged", "Magic", "Hitpoints"
	};
	private static final int[] COMBAT_MILESTONES = {20, 30, 40, 50, 60, 70, 80, 90};

	private final SkillXpLibrary skillXp;

	public TrainingPlanner(SkillXpLibrary skillXp)
	{
		this.skillXp = skillXp == null ? SkillXpLibrary.empty() : skillXp;
	}

	/**
	 * Ranks training goals by the CA points they open per hour of training, best first.
	 *
	 * @param all          every task in the dataset
	 * @param completedIds tasks already done (never counted as newly unlocked)
	 * @param effortLib    curated per-task gates
	 * @param recStatsLib  curated recommended stats (drives the readiness distance)
	 * @param profile      the player's live levels + quests
	 * @param limit        how many suggestions to return
	 */
	public List<TrainingSuggestion> plan(List<CombatAchievement> all, Set<Integer> completedIds,
		EffortDataLibrary effortLib, RecStatsLibrary recStatsLib, PlayerProfile profile, int limit)
	{
		if (all == null || all.isEmpty() || profile == null || profile.isEmpty())
		{
			return new ArrayList<>();
		}
		EffortDataLibrary effort = effortLib == null ? EffortDataLibrary.empty() : effortLib;
		RecStatsLibrary rec = recStatsLib == null ? RecStatsLibrary.empty() : recStatsLib;
		Set<Integer> done = completedIds == null ? new HashSet<>() : completedIds;

		Set<Integer> baseViable = viable(all, done, effort, rec, profile);

		// Candidate goals: every hard-gate threshold above the player's current level (these are the levels
		// that actually open something), plus combat milestones for the soft readiness side.
		Map<String, Set<Integer>> thresholds = new LinkedHashMap<>();
		for (CombatAchievement a : all)
		{
			if (done.contains(a.id()))
			{
				continue;
			}
			for (Map.Entry<String, Integer> req : effort.effortFor(a.id()).levelReqs().entrySet())
			{
				if (req.getValue() != null && req.getValue() > profile.levelOf(req.getKey()))
				{
					thresholds.computeIfAbsent(req.getKey(), k -> new TreeSet<>()).add(req.getValue());
				}
			}
			// RECOMMENDED levels are candidates too, not just hard gates. Without them the planner could
			// only aim at levels some quest happens to demand, and would land one short of what the content
			// actually wants: it suggested "Prayer 42" (a Vorkath gate) for a row about Bryophyta, whose
			// CAs ask for 43. The goal should be the number the player is actually trying to reach.
			for (StatRequirement req : rec.softFor(a.id()))
			{
				for (String skill : req.skills())
				{
					if (req.level() > profile.levelOf(skill))
					{
						thresholds.computeIfAbsent(skill, k -> new TreeSet<>()).add(req.level());
					}
				}
			}
		}

		List<TrainingSuggestion> out = new ArrayList<>();
		for (Map.Entry<String, Set<Integer>> e : thresholds.entrySet())
		{
			for (int target : e.getValue())
			{
				if (target > 99)
				{
					continue;
				}
				Map<String, Integer> raise = new HashMap<>();
				raise.put(e.getKey(), target);
				add(out, e.getKey() + " " + target, raise, all, done, effort, rec, profile, baseViable);
			}
		}
		for (int target : COMBAT_MILESTONES)
		{
			Map<String, Integer> raise = new HashMap<>();
			for (String c : COMBAT_KIT)
			{
				if (profile.levelOf(c) < target)
				{
					raise.put(c, target);
				}
			}
			if (!raise.isEmpty())
			{
				add(out, "All combat " + target, raise, all, done, effort, rec, profile, baseViable);
			}
		}

		// One goal per skill — the best rate — so "Ranged 25 / 30 / 40" collapses to a single suggestion.
		Map<String, TrainingSuggestion> best = new LinkedHashMap<>();
		for (TrainingSuggestion s : out)
		{
			String key = s.skills().size() > 1 && s.label().startsWith("All combat")
				? "All combat" : String.join("/", s.skills());
			TrainingSuggestion cur = best.get(key);
			if (cur == null || s.score() > cur.score())
			{
				best.put(key, s);
			}
		}
		List<TrainingSuggestion> ranked = new ArrayList<>(best.values());
		ranked.sort(Comparator.comparingDouble(TrainingSuggestion::score).reversed()
			.thenComparing(TrainingSuggestion::label));
		return ranked.size() > limit ? new ArrayList<>(ranked.subList(0, limit)) : ranked;
	}

	private void add(List<TrainingSuggestion> out, String label, Map<String, Integer> raise,
		List<CombatAchievement> all, Set<Integer> done, EffortDataLibrary effort, RecStatsLibrary rec,
		PlayerProfile profile, Set<Integer> baseViable)
	{
		Set<Integer> after = viable(all, done, effort, rec, profile.withLevels(raise));
		int count = 0;
		int points = 0;
		Map<String, Integer> byMonster = new HashMap<>();
		for (CombatAchievement a : all)
		{
			if (after.contains(a.id()) && !baseViable.contains(a.id()))
			{
				count++;
				points += a.points();
				if (a.hasMonster())
				{
					byMonster.merge(a.monster(), 1, Integer::sum);
				}
			}
		}
		if (count == 0)
		{
			return; // trains nothing open — not worth suggesting
		}
		double hours = 0;
		boolean calendarTime = false;
		for (Map.Entry<String, Integer> e : raise.entrySet())
		{
			int have = profile.levelOf(e.getKey());
			hours += Math.max(0, skillXp.hoursToTrain(e.getKey(), have, e.getValue()));
			// Farming is bounded by patch timers, so its estimate is elapsed time and reads in days. A goal
			// you simply have to sit and play stays in hours however long it runs.
			calendarTime |= skillXp.isDailyGated(e.getKey(), have, e.getValue());
		}
		String hint = byMonster.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey)
			.orElse("");
		int target = raise.values().stream().max(Integer::compareTo).orElse(0);
		out.add(new TrainingSuggestion(label, new ArrayList<>(raise.keySet()), target, count, points,
			(int) Math.round(hours * 60), hint, calendarTime));
	}

	/** Tasks that are both doable and within reach — the set a training goal is trying to grow. */
	private Set<Integer> viable(List<CombatAchievement> all, Set<Integer> done, EffortDataLibrary effort,
		RecStatsLibrary rec, PlayerProfile profile)
	{
		ProfileSignalsProvider signals = new ProfileSignalsProvider(effort, rec, profile);
		Set<Integer> out = new HashSet<>();
		for (CombatAchievement a : all)
		{
			if (done.contains(a.id()))
			{
				continue;
			}
			TaskLiveSignals sig = signals.signalsFor(a.id());
			// A training GOAL aims at the level the content actually asks for, not the cheapest level that
			// brings it inside the tolerance window. Scored on the window, the planner undershot on purpose:
			// it suggested "Prayer 37" for a row about Bryophyta, whose CAs want 43, because 37 was enough
			// to land within 15 of it. Someone who trains to the number they were given should arrive ready.
			if (sig.doableNow() && profile.worstShortfall(rec.softFor(a.id())) == 0)
			{
				out.add(a.id());
			}
		}
		return out;
	}
}
