package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerProfileTest
{
	private static Set<String> set(String... names)
	{
		return new HashSet<>(Arrays.asList(names));
	}

	private static PlayerProfile withQuests(Set<String> completed, Set<String> started)
	{
		return PlayerProfile.of(new HashMap<>(), completed, started);
	}

	@Test
	public void completedQuestSatisfiesACompletionGate()
	{
		PlayerProfile p = withQuests(set("Dragon Slayer II"), set("Dragon Slayer II"));
		assertTrue(p.hasCompleted("Dragon Slayer II"));
		assertTrue(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Dragon Slayer II", false))));
	}

	@Test
	public void missingQuestFailsACompletionGate()
	{
		PlayerProfile p = withQuests(set("Regicide"), set("Regicide"));
		assertFalse(p.hasCompleted("Dragon Slayer II"));
		assertFalse(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Dragon Slayer II", false))));
	}

	@Test
	public void startedSufficesGateAcceptsAnInProgressQuest()
	{
		// Zulrah needs Regicide STARTED, not finished. A player mid-Regicide must not be locked out.
		PlayerProfile p = withQuests(Collections.emptySet(), set("Regicide"));
		assertFalse("not finished", p.hasCompleted("Regicide"));
		assertTrue("but started", p.hasStarted("Regicide"));
		assertTrue(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Regicide", true))));
		// The same in-progress quest does NOT satisfy a completion gate.
		assertFalse(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Regicide", false))));
	}

	@Test
	public void finishedImpliesStartedEvenWhenCallerForgets()
	{
		// Caller passes a finished quest but an empty "started" set; the profile back-fills it.
		PlayerProfile p = PlayerProfile.of(new HashMap<>(), set("Sins of the Father"), Collections.emptySet());
		assertTrue(p.hasStarted("Sins of the Father"));
		assertTrue(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Sins of the Father", true))));
	}

	@Test
	public void questNamesAreCaseInsensitive()
	{
		PlayerProfile p = withQuests(set("song of the elves"), set("song of the elves"));
		assertTrue(p.hasCompleted("Song of the Elves"));
		assertTrue(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("SONG OF THE ELVES", false))));
	}

	@Test
	public void allGatesMustPass()
	{
		PlayerProfile p = withQuests(set("Monkey Madness II"), set("Monkey Madness II"));
		assertTrue(p.hasQuestAccess(Collections.singletonList(new QuestRequirement("Monkey Madness II", false))));
		assertFalse("second quest missing", p.hasQuestAccess(Arrays.asList(
			new QuestRequirement("Monkey Madness II", false),
			new QuestRequirement("Dragon Slayer II", false))));
	}

	@Test
	public void emptyOrNullGateIsTriviallyMet()
	{
		PlayerProfile p = PlayerProfile.empty();
		assertTrue(p.hasQuestAccess(Collections.emptyList()));
		assertTrue(p.hasQuestAccess(null));
	}

	@Test
	public void skillLevelsStillWorkAlongsideQuests()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Slayer", 85);
		PlayerProfile p = PlayerProfile.of(levels, set("Priest in Peril"), set("Priest in Peril"));
		assertTrue(p.levelOf("slayer") == 85);
		Map<String, Integer> need = new HashMap<>();
		need.put("Slayer", 85);
		assertTrue(p.meets(need));
	}
}
