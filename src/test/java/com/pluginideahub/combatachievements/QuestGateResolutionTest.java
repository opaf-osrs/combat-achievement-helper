package com.pluginideahub.combatachievements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Guards the quest-gate join: every {@code questReqs} quest name authored in the bundled effort data
 * must resolve to a real {@code net.runelite.api.Quest} (matched the same way the live
 * {@code AccountReader} matches: trim + lower-case). A typo or a renamed quest would otherwise make a
 * gate silently never apply — exactly the kind of quiet wrongness the data-integrity tests exist to
 * catch. This is the one test that ties the pure dataset to the live RuneLite quest enum.
 */
public class QuestGateResolutionTest
{
	private static final String EFFORT = "/com/pluginideahub/combatachievements/task_effort.json";

	private static String norm(String s)
	{
		return s == null ? "" : s.trim().toLowerCase();
	}

	private static Set<String> runeliteQuestNames()
	{
		Set<String> names = new HashSet<>();
		for (Quest q : Quest.values())
		{
			names.add(norm(q.getName()));
		}
		return names;
	}

	@Test
	public void everyAuthoredQuestNameResolvesToARuneliteQuest()
	{
		Set<String> known = runeliteQuestNames();
		assertTrue("expected the RuneLite Quest enum to be populated", known.size() > 100);

		EffortDataLibrary lib = EffortDataLibrary.loadBundled();
		int taskCount = CombatAchievementLibrary.loadBundled().taskCount();

		int gatedTasks = 0;
		Set<String> distinctQuests = new HashSet<>();
		for (int id = 0; id < taskCount; id++)
		{
			TaskEffortData effort = lib.effortFor(id);
			if (!effort.hasQuestGate())
			{
				continue;
			}
			gatedTasks++;
			for (QuestRequirement req : effort.questReqs())
			{
				distinctQuests.add(req.quest());
				if (!known.contains(norm(req.quest())))
				{
					fail("task " + id + " requires quest \"" + req.quest()
						+ "\" which does not match any net.runelite.api.Quest.getName()");
				}
			}
		}

		// Sanity: the gate data is actually present (so this test can't silently pass on an empty file).
		assertTrue("expected many quest-gated tasks; got " + gatedTasks, gatedTasks >= 300);
		assertTrue("expected a healthy spread of distinct gating quests; got " + distinctQuests.size(),
			distinctQuests.size() >= 15);
	}

	/** The curation source (data/quest-gates.json) keys must all match a real dataset monster value. */
	@Test
	public void questGateMonstersExistInDataset()
	{
		Set<String> monsters = datasetMonsters();
		JsonObject gates = readResourceOrFile();
		if (gates == null)
		{
			return; // data/ file not on the test classpath in some build layouts; the bundled check above covers correctness
		}
		for (Map.Entry<String, JsonElement> e : gates.entrySet())
		{
			assertTrue("quest-gates.json names monster \"" + e.getKey()
				+ "\" which is not in combat_achievements.json", monsters.contains(e.getKey()));
		}
	}

	private static Set<String> datasetMonsters()
	{
		Set<String> monsters = new HashSet<>();
		try (InputStream in = QuestGateResolutionTest.class.getResourceAsStream(
			"/com/pluginideahub/combatachievements/combat_achievements.json"))
		{
			assertNotNull(in);
			JsonObject root = JsonParser.parseReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray tasks = root.getAsJsonArray("tasks");
			for (JsonElement t : tasks)
			{
				JsonElement m = t.getAsJsonObject().get("monster");
				if (m != null && !m.isJsonNull())
				{
					monsters.add(m.getAsString());
				}
			}
		}
		catch (Exception ex)
		{
			throw new AssertionError("failed reading combat_achievements.json: " + ex, ex);
		}
		return monsters;
	}

	/** Reads data/quest-gates.json from the working dir (repo root) if available; null otherwise. */
	private static JsonObject readResourceOrFile()
	{
		java.io.File f = new java.io.File("data/quest-gates.json");
		if (!f.isFile())
		{
			return null;
		}
		try (java.io.Reader r = new InputStreamReader(
			new java.io.FileInputStream(f), StandardCharsets.UTF_8))
		{
			return JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("gates");
		}
		catch (Exception ex)
		{
			throw new AssertionError("failed reading data/quest-gates.json: " + ex, ex);
		}
	}
}
