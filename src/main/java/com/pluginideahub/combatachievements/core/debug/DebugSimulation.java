package com.pluginideahub.combatachievements.core.debug;

import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A developer-only "what would this account see?" override: pretend the player has different skill
 * levels, and/or has completed no Combat Achievements at all. Pure — it rewrites the
 * {@link PlayerProfile} and {@link ProgressSnapshot} that the rest of the plugin already runs on, so
 * every downstream surface (doable-now, the beginner rule, the Route's readiness stop-short, Train
 * next, session times) reacts exactly as it would for a real account in that state.
 *
 * <p>Both halves are needed to reproduce a beginner: the beginner rule releases on EITHER Easy-tier
 * points OR 70+ combat, so dropping levels alone leaves an established account's CA points holding
 * the gate open.</p>
 *
 * <p>This never writes anywhere. It is applied to the values in flight on each refresh, so nothing
 * simulated can reach the account's persisted data.</p>
 */
public final class DebugSimulation
{
	/**
	 * The OSRS skills, in the client's own display names so they match both the levels read by the
	 * account reader and the keys the requirement data is written against.
	 */
	public static final List<String> SKILLS = Collections.unmodifiableList(Arrays.asList(
		"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Hitpoints",
		"Slayer", "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Mining",
		"Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting", "Runecraft",
		"Construction", "Farming", "Hunter"));

	/** Hitpoints starts at 10 in OSRS, so a "level 1 in everything" account is still 10 HP (combat 3). */
	public static final int MIN_HITPOINTS = 10;

	private static final DebugSimulation NONE =
		new DebugSimulation(Collections.emptyMap(), false, false);

	private final Map<String, Integer> levelOverrides;
	private final boolean zeroCompletion;
	private final boolean zeroQuests;

	private DebugSimulation(Map<String, Integer> levelOverrides, boolean zeroCompletion,
		boolean zeroQuests)
	{
		this.levelOverrides = Collections.unmodifiableMap(new LinkedHashMap<>(levelOverrides));
		this.zeroCompletion = zeroCompletion;
		this.zeroQuests = zeroQuests;
	}

	/** No simulation: every {@code apply} is the identity. */
	public static DebugSimulation none()
	{
		return NONE;
	}

	public static DebugSimulation of(Map<String, Integer> levelOverrides, boolean zeroCompletion)
	{
		return of(levelOverrides, zeroCompletion, false);
	}

	/**
	 * @param levelOverrides skill name → level to pretend; empty leaves the real levels alone
	 * @param zeroCompletion pretend no Combat Achievement has been completed
	 * @param zeroQuests     pretend no quest has been done — without this a simulated beginner keeps the
	 *                       real account's quest log, so quest-gated content stays unlocked and nothing
	 *                       shows under "Unlock next" (a quest already done is not an unlock)
	 */
	public static DebugSimulation of(Map<String, Integer> levelOverrides, boolean zeroCompletion,
		boolean zeroQuests)
	{
		Map<String, Integer> clean = new LinkedHashMap<>();
		if (levelOverrides != null)
		{
			for (Map.Entry<String, Integer> e : levelOverrides.entrySet())
			{
				if (e.getKey() != null && e.getValue() != null && !e.getKey().trim().isEmpty())
				{
					clean.put(e.getKey().trim(), clamp(e.getKey(), e.getValue()));
				}
			}
		}
		if (clean.isEmpty() && !zeroCompletion && !zeroQuests)
		{
			return NONE;
		}
		return new DebugSimulation(clean, zeroCompletion, zeroQuests);
	}

	/** Every skill set to {@code level} — the preset behind the "3 / 40 / 60 / 80 / 99" buttons. */
	public static Map<String, Integer> allSkillsAt(int level)
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (String skill : SKILLS)
		{
			levels.put(skill, clamp(skill, level));
		}
		return levels;
	}

	private static int clamp(String skill, int level)
	{
		int floor = "Hitpoints".equalsIgnoreCase(skill == null ? "" : skill.trim()) ? MIN_HITPOINTS : 1;
		return Math.max(floor, Math.min(99, level));
	}

	/** True when this changes anything; a false value means the panel should show the real account. */
	public boolean isActive()
	{
		return zeroCompletion || zeroQuests || !levelOverrides.isEmpty();
	}

	public boolean zeroCompletion()
	{
		return zeroCompletion;
	}

	public boolean zeroQuests()
	{
		return zeroQuests;
	}

	/** Skill name → simulated level; empty when levels are not being overridden. */
	public Map<String, Integer> levelOverrides()
	{
		return levelOverrides;
	}

	/** The simulated level for one skill, or {@code fallback} when that skill is not overridden. */
	public int levelOf(String skill, int fallback)
	{
		if (skill == null)
		{
			return fallback;
		}
		for (Map.Entry<String, Integer> e : levelOverrides.entrySet())
		{
			if (e.getKey().equalsIgnoreCase(skill.trim()))
			{
				return e.getValue();
			}
		}
		return fallback;
	}

	/** The real profile with the simulated levels swapped in, and the quest log blanked when asked. */
	public PlayerProfile apply(PlayerProfile real)
	{
		PlayerProfile base = real == null ? PlayerProfile.empty() : real;
		if (zeroQuests)
		{
			base = base.withoutQuests();
		}
		if (levelOverrides.isEmpty())
		{
			return base;
		}
		Map<String, Integer> lowered = new HashMap<>();
		for (Map.Entry<String, Integer> e : levelOverrides.entrySet())
		{
			lowered.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
		}
		return base.withLevels(lowered);
	}

	/**
	 * The real progress with every completion cleared when simulating a fresh account. The account hash
	 * is preserved so the snapshot still reads as a present, real-account sync rather than the
	 * logged-out state.
	 */
	public ProgressSnapshot apply(ProgressSnapshot real)
	{
		if (!zeroCompletion || real == null || !real.isPresent())
		{
			return real;
		}
		return new ProgressSnapshot(Collections.emptySet(), 0, 0, null, real.accountHash(),
			real.isSample());
	}
}
