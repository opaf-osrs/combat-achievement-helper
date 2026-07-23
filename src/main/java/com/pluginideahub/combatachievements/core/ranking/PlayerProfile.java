package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import com.pluginideahub.combatachievements.core.achievement.StatRequirement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A pure snapshot of the player's account state used to decide which tasks they meet the
 * requirements for: skill levels plus quest-completion state. Skill and quest names are compared
 * case-insensitively so the client's {@code Skill.getName()} / {@code Quest.getName()} line up with
 * the dataset's level- and quest-requirement keys. No RuneLite types — the bridge fills this from the
 * live client; the core just reads it.
 */
public final class PlayerProfile
{
	private final Map<String, Integer> levels;
	private final Set<String> completedQuests;
	private final Set<String> startedQuests;

	private PlayerProfile(Map<String, Integer> levels, Set<String> completedQuests,
		Set<String> startedQuests)
	{
		this.levels = Collections.unmodifiableMap(levels);
		this.completedQuests = Collections.unmodifiableSet(completedQuests);
		this.startedQuests = Collections.unmodifiableSet(startedQuests);
	}

	public static PlayerProfile empty()
	{
		return new PlayerProfile(new HashMap<>(), new HashSet<>(), new HashSet<>());
	}

	/** Builds a profile from skill levels only (no quest data — every quest gate reads as unmet). */
	public static PlayerProfile of(Map<String, Integer> rawLevels)
	{
		return of(rawLevels, Collections.emptySet(), Collections.emptySet());
	}

	/**
	 * Builds a full profile.
	 *
	 * @param rawLevels       skill name → level (names normalised to lower-case)
	 * @param completedQuests quest names the player has FINISHED
	 * @param startedQuests   quest names the player has started (FINISHED or IN_PROGRESS); should be a
	 *                        superset of {@code completedQuests}
	 */
	public static PlayerProfile of(Map<String, Integer> rawLevels, Set<String> completedQuests,
		Set<String> startedQuests)
	{
		Map<String, Integer> normalised = new HashMap<>();
		if (rawLevels != null)
		{
			for (Map.Entry<String, Integer> entry : rawLevels.entrySet())
			{
				if (entry.getKey() != null && entry.getValue() != null)
				{
					normalised.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
				}
			}
		}
		Set<String> done = normaliseNames(completedQuests);
		Set<String> started = normaliseNames(startedQuests);
		// A finished quest is, by definition, also started — guard against a caller that forgets this.
		started.addAll(done);
		return new PlayerProfile(normalised, done, started);
	}

	private static Set<String> normaliseNames(Set<String> names)
	{
		Set<String> out = new HashSet<>();
		if (names != null)
		{
			for (String n : names)
			{
				if (n != null && !n.trim().isEmpty())
				{
					out.add(n.trim().toLowerCase(Locale.ROOT));
				}
			}
		}
		return out;
	}

	/**
	 * A copy of this profile with the given skills raised (everything else untouched, quests preserved).
	 * Lets the training planner ask "how many CAs would open up if I trained this?" without a live account.
	 */
	public PlayerProfile withLevels(Map<String, Integer> raised)
	{
		if (raised == null || raised.isEmpty())
		{
			return this;
		}
		Map<String, Integer> merged = new HashMap<>(levels);
		for (Map.Entry<String, Integer> e : raised.entrySet())
		{
			if (e.getKey() != null && e.getValue() != null)
			{
				merged.put(e.getKey().trim().toLowerCase(Locale.ROOT), e.getValue());
			}
		}
		return new PlayerProfile(merged, new HashSet<>(completedQuests), new HashSet<>(startedQuests));
	}

	/**
	 * The account's OSRS combat level, by the game's own formula. Used to tell a genuinely new account apart
	 * from an established player who simply hasn't started Combat Achievements yet — the latter has 0 CA
	 * points but should never be treated as a beginner.
	 */
	public int combatLevel()
	{
		double base = 0.25 * (levelOf("Defence") + levelOf("Hitpoints") + Math.floor(levelOf("Prayer") / 2.0));
		double melee = 0.325 * (levelOf("Attack") + levelOf("Strength"));
		double ranged = 0.325 * Math.floor(levelOf("Ranged") * 3 / 2.0);
		double magic = 0.325 * Math.floor(levelOf("Magic") * 3 / 2.0);
		return (int) Math.floor(base + Math.max(melee, Math.max(ranged, magic)));
	}

	public int levelOf(String skill)
	{
		if (skill == null)
		{
			return 0;
		}
		return levels.getOrDefault(skill.trim().toLowerCase(Locale.ROOT), 0);
	}

	/** True when every required skill level is satisfied (an empty/absent requirement is trivially met). */
	public boolean meets(Map<String, Integer> requirements)
	{
		if (requirements == null || requirements.isEmpty())
		{
			return true;
		}
		for (Map.Entry<String, Integer> req : requirements.entrySet())
		{
			if (req.getValue() != null && levelOf(req.getKey()) < req.getValue())
			{
				return false;
			}
		}
		return true;
	}

	/** True when the player has FINISHED this exact quest. */
	public boolean hasCompleted(String quest)
	{
		return quest != null && completedQuests.contains(quest.trim().toLowerCase(Locale.ROOT));
	}

	/** The set of finished quest names, normalised to lower-case (for chain-cost math). */
	public Set<String> completedQuestsLower()
	{
		return completedQuests;
	}

	/** True when the player has at least STARTED this exact quest (finished counts). */
	public boolean hasStarted(String quest)
	{
		return quest != null && startedQuests.contains(quest.trim().toLowerCase(Locale.ROOT));
	}

	/**
	 * True when the player satisfies every quest gate: each requirement must be finished, or merely
	 * started when {@link QuestRequirement#startedSuffices()}. An empty/absent requirement list is
	 * trivially met, so un-gated tasks are never blocked.
	 */
	public boolean hasQuestAccess(List<QuestRequirement> requirements)
	{
		if (requirements == null || requirements.isEmpty())
		{
			return true;
		}
		for (QuestRequirement req : requirements)
		{
			if (req == null || req.quest().isEmpty())
			{
				continue;
			}
			boolean ok = req.startedSuffices() ? hasStarted(req.quest()) : hasCompleted(req.quest());
			if (!ok)
			{
				return false;
			}
		}
		return true;
	}

	/** True when the player satisfies a single recommended-stat requirement (ALL / ANY / PRIMARY). */
	public boolean meets(StatRequirement req)
	{
		if (req == null || req.skills().isEmpty())
		{
			return true;
		}
		if (req.mode() == StatRequirement.Mode.ANY || req.mode() == StatRequirement.Mode.PRIMARY)
		{
			for (String s : req.skills())
			{
				if (levelOf(s) >= req.level())
				{
					return true;
				}
			}
			return false;
		}
		for (String s : req.skills()) // ALL
		{
			if (levelOf(s) < req.level())
			{
				return false;
			}
		}
		return true;
	}

	/** True when the player satisfies every requirement (empty/null ⇒ trivially met). */
	public boolean meetsAll(List<StatRequirement> reqs)
	{
		if (reqs == null)
		{
			return true;
		}
		for (StatRequirement r : reqs)
		{
			if (!meets(r))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * How many levels short of the requirements the player is (0 = meets all). ALL sums each skill's
	 * shortfall; ANY/PRIMARY count only the best listed skill. Used to scale a soft "below rec stats" sink.
	 */
	public int shortfall(List<StatRequirement> reqs)
	{
		if (reqs == null)
		{
			return 0;
		}
		int total = 0;
		for (StatRequirement r : reqs)
		{
			if (r == null || r.skills().isEmpty())
			{
				continue;
			}
			if (r.mode() == StatRequirement.Mode.ALL)
			{
				for (String s : r.skills())
				{
					total += Math.max(0, r.level() - levelOf(s));
				}
			}
			else
			{
				int best = 0;
				for (String s : r.skills())
				{
					best = Math.max(best, levelOf(s));
				}
				total += Math.max(0, r.level() - best);
			}
		}
		return total;
	}

	/**
	 * The biggest gap on any single stat (0 = meets everything). Where {@link #shortfall} SUMS every gap —
	 * right for weighting, since lots of small gaps do add up — this reports the worst one, which is what
	 * "could I actually attempt this?" hinges on. Being 10 short in six stats is fine; being 49 short in one
	 * is not, yet both sum to about the same. ANY/PRIMARY requirements count only the player's best skill.
	 */
	public int worstShortfall(List<StatRequirement> reqs)
	{
		if (reqs == null)
		{
			return 0;
		}
		int worst = 0;
		for (StatRequirement r : reqs)
		{
			if (r == null || r.skills().isEmpty())
			{
				continue;
			}
			if (r.mode() == StatRequirement.Mode.ALL)
			{
				for (String s : r.skills())
				{
					worst = Math.max(worst, r.level() - levelOf(s));
				}
			}
			else
			{
				int best = 0;
				for (String s : r.skills())
				{
					best = Math.max(best, levelOf(s));
				}
				worst = Math.max(worst, r.level() - best);
			}
		}
		return Math.max(0, worst);
	}

	public boolean isEmpty()
	{
		return levels.isEmpty();
	}
}
