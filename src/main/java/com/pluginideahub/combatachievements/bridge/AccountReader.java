package com.pluginideahub.combatachievements.bridge;

import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

/**
 * Reads the logged-in account's real skill levels and quest-completion state from the client into a
 * pure {@link PlayerProfile}. The only piece that touches {@link Client} for account state; must run
 * on the client thread. Skills are keyed by {@link Skill#getName()} and quests by
 * {@link Quest#getName()} so they match the dataset's level- and quest-requirement keys.
 */
public final class AccountReader
{
	private AccountReader()
	{
	}

	public static PlayerProfile readProfile(Client client)
	{
		if (client == null)
		{
			return PlayerProfile.empty();
		}
		Map<String, Integer> levels = new HashMap<>();
		for (Skill skill : Skill.values())
		{
			try
			{
				levels.put(skill.getName(), client.getRealSkillLevel(skill));
			}
			catch (RuntimeException ignored)
			{
				// skip any skill the client can't report right now
			}
		}

		Set<String> completed = new HashSet<>();
		Set<String> started = new HashSet<>();
		for (Quest quest : Quest.values())
		{
			try
			{
				QuestState state = quest.getState(client);
				if (state == QuestState.FINISHED)
				{
					completed.add(quest.getName());
					started.add(quest.getName());
				}
				else if (state == QuestState.IN_PROGRESS)
				{
					started.add(quest.getName());
				}
			}
			catch (RuntimeException ignored)
			{
				// skip any quest whose state can't be resolved right now
			}
		}

		return PlayerProfile.of(levels, completed, started);
	}
}
