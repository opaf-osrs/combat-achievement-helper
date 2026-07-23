package com.pluginideahub.combatachievements.bridge;

import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.MonsterNames;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accumulates per-boss kill count from the in-game "kill count is: N" chat messages (the same source
 * the loot/boss trackers use — no special API needed), so the effort engine gets partial progress on
 * Kill Count CAs and a competence signal for execution CAs. The parser is pure and unit-tested; the
 * accumulated map is the live state. Keyed by the boss name as the dataset spells it.
 *
 * <p>Captures kills observed while the plugin runs; the plugin persists the map per account so it
 * survives restarts. (A future enhancement can backfill historical KC from the collection log.)</p>
 */
public final class KillCountTracker
{
	// Boss lines carry an explicit "kill count" / "completion count" / "chest count" phrase, e.g.
	// "Your Vorkath kill count is: 1,234." / "Your Theatre of Blood total completion count is: 12" /
	// "Your Barrows chest count is: 40".
	private static final Pattern KC_PHRASE = Pattern.compile(
		"your (.+?) (?:kill count|total completion count|completion count|chest count) is: ?([\\d,]+)",
		Pattern.CASE_INSENSITIVE);
	// Raid completions use a bare "count" but always behind "completed", e.g.
	// "Your completed Chambers of Xeric count is: 50" — anchoring on "completed" avoids matching
	// unrelated "... count is:" tally lines (slayer tasks, etc.).
	private static final Pattern KC_COMPLETED = Pattern.compile(
		"your completed (.+?) count is: ?([\\d,]+)", Pattern.CASE_INSENSITIVE);

	private final Map<String, Integer> kcByMonster = new HashMap<>();

	/** A parsed kill-count update from a chat message. */
	public static final class Update
	{
		private final String boss;
		private final int count;

		Update(String boss, int count)
		{
			this.boss = boss;
			this.count = count;
		}

		public String boss()
		{
			return boss;
		}

		public int count()
		{
			return count;
		}
	}

	/** Parses a kill-count chat message, or empty when the message is not one. Pure. */
	public static Optional<Update> parse(String message)
	{
		if (message == null)
		{
			return Optional.empty();
		}
		String text = message.trim();
		Matcher m = KC_PHRASE.matcher(text);
		if (!m.find())
		{
			m = KC_COMPLETED.matcher(text);
			if (!m.find())
			{
				return Optional.empty();
			}
		}
		String boss = m.group(1).trim();
		try
		{
			int count = Integer.parseInt(m.group(2).replace(",", ""));
			return boss.isEmpty() ? Optional.empty() : Optional.of(new Update(boss, count));
		}
		catch (NumberFormatException ex)
		{
			return Optional.empty();
		}
	}

	/** Feeds a chat message; updates the tracked KC if it is a kill-count line. Returns true if matched. */
	public synchronized boolean onMessage(String message)
	{
		Optional<Update> update = parse(message);
		if (!update.isPresent())
		{
			return false;
		}
		// Canonicalise the chat boss name to the dataset's monster key, then keep the highest seen.
		String key = MonsterNames.toDatasetKey(update.get().boss());
		if (key.isEmpty())
		{
			return false;
		}
		kcByMonster.merge(key, update.get().count(), Math::max);
		return true;
	}

	/** Replaces the tracked state (e.g. from persisted config). */
	public synchronized void load(Map<String, Integer> persisted)
	{
		kcByMonster.clear();
		mergeMax(persisted);
	}

	/** Merges in KC (taking the higher value), e.g. a hiscore backfill alongside live chat tracking. */
	public synchronized void seed(Map<String, Integer> kc)
	{
		mergeMax(kc);
	}

	private void mergeMax(Map<String, Integer> kc)
	{
		if (kc == null)
		{
			return;
		}
		for (Map.Entry<String, Integer> e : kc.entrySet())
		{
			if (e.getKey() != null && e.getValue() != null && e.getValue() >= 0)
			{
				kcByMonster.merge(e.getKey().trim().toLowerCase(Locale.ROOT), e.getValue(), Math::max);
			}
		}
	}

	public synchronized Map<String, Integer> asMap()
	{
		return new HashMap<>(kcByMonster);
	}

	public synchronized void clear()
	{
		kcByMonster.clear();
	}

	/** A pure snapshot for the effort engine. */
	public synchronized CombatExperience snapshot()
	{
		return CombatExperience.of(kcByMonster);
	}
}
