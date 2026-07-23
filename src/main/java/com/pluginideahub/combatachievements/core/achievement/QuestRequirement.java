package com.pluginideahub.combatachievements.core.achievement;

import java.util.Locale;
import java.util.Objects;

/**
 * A quest a player must have done to access (and therefore attempt) a task's content. Pure value
 * object — the live "is this quest finished?" check is resolved by the bridge against the client's
 * quest state, keyed by {@link #quest()} (matched to {@code net.runelite.api.Quest.getName()}).
 *
 * <p>{@link #startedSuffices()} captures the handful of cases where merely <em>starting</em> the
 * quest grants access (e.g. Regicide for Zulrah's Zul-Andra) rather than completing it — so a player
 * mid-quest is not wrongly told the task is locked.</p>
 */
public final class QuestRequirement
{
	private final String quest;
	private final boolean startedSuffices;

	public QuestRequirement(String quest, boolean startedSuffices)
	{
		this.quest = quest == null ? "" : quest.trim();
		this.startedSuffices = startedSuffices;
	}

	/** The exact in-game quest name (matches {@code Quest.getName()}). */
	public String quest()
	{
		return quest;
	}

	/** True when starting the quest is enough; false when it must be completed. */
	public boolean startedSuffices()
	{
		return startedSuffices;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof QuestRequirement))
		{
			return false;
		}
		QuestRequirement that = (QuestRequirement) o;
		return startedSuffices == that.startedSuffices && quest.equalsIgnoreCase(that.quest);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(quest.toLowerCase(Locale.ROOT), startedSuffices);
	}

	@Override
	public String toString()
	{
		return "QuestRequirement{" + quest + (startedSuffices ? " (started)" : "") + '}';
	}
}
