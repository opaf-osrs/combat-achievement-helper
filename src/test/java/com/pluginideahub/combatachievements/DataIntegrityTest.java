package com.pluginideahub.combatachievements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.CompletionLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.combat.MonsterStatsLibrary;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Cross-file data invariants. These guard the "id-shift catastrophe": every id-keyed dataset must
 * stay aligned with the canonical task id space (0 .. taskCount-1). A dataset regen that drops
 * entries or shifts ids fails here instead of silently mislabelling the player's progress.
 */
public class DataIntegrityTest
{
	private static final String PKG = "/com/pluginideahub/combatachievements/";

	@Test
	public void taskIdsAreContiguousFromZero()
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		int n = lib.taskCount();
		assertTrue("expected a populated dataset", n > 0);
		for (int id = 0; id < n; id++)
		{
			assertNotNull("task ids must be contiguous 0.." + (n - 1) + "; missing " + id, lib.byId(id));
		}
	}

	@Test
	public void completionCoversEveryTaskWithKeysInRange()
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		CompletionLibrary comp = CompletionLibrary.loadBundled();
		assertEquals("completion% must cover every task", lib.taskCount(), comp.count());
		for (CombatAchievement t : lib.all())
		{
			assertTrue("no completion% for task " + t.id(), comp.completionFor(t.id()) >= 0);
		}
		for (int key : taskKeys(PKG + "task_completion.json"))
		{
			assertInRange(key, lib.taskCount());
		}
	}

	@Test
	public void difficultyCoversEveryTaskWithKeysInRangeAndOnScale()
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		TaskDifficultyLibrary diff = TaskDifficultyLibrary.loadBundled();
		assertEquals("difficulty must cover every task", lib.taskCount(), diff.count());
		for (CombatAchievement t : lib.all())
		{
			int d = diff.difficultyFor(t.id()).difficulty();
			assertTrue("difficulty for task " + t.id() + " must be on the 1-10 scale, was " + d,
				d >= 1 && d <= 10);
		}
		for (int key : taskKeys(PKG + "task_difficulty.json"))
		{
			assertInRange(key, lib.taskCount());
		}
	}

	@Test
	public void effortKeysAreValidTaskIds()
	{
		int n = CombatAchievementLibrary.loadBundled().taskCount();
		for (int key : taskKeys(PKG + "task_effort.json"))
		{
			assertInRange(key, n);
		}
	}

	@Test
	public void monsterStatsArePresent()
	{
		assertTrue("expected the auto-sourced monster stats", MonsterStatsLibrary.loadBundled().count() >= 60);
	}

	@Test
	public void tierRewardsCoverEveryTier()
	{
		TierRewardLibrary rewards = TierRewardLibrary.loadBundled();
		for (AchievementTier tier : AchievementTier.values())
		{
			assertTrue("missing tier reward for " + tier, rewards.forTier(tier).isPresent());
		}
	}

	private static void assertInRange(int key, int taskCount)
	{
		assertTrue("dataset key " + key + " is outside the task id range [0," + (taskCount - 1) + "]",
			key >= 0 && key < taskCount);
	}

	/** Numeric keys of the {@code tasks} object in a bundled JSON resource. */
	private static List<Integer> taskKeys(String resource)
	{
		List<Integer> keys = new ArrayList<>();
		try (InputStream in = DataIntegrityTest.class.getResourceAsStream(resource))
		{
			assertNotNull("missing bundled resource " + resource, in);
			JsonObject root = JsonParser.parseReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonElement tasks = root.get("tasks");
			assertNotNull("no 'tasks' object in " + resource, tasks);
			for (String k : tasks.getAsJsonObject().keySet())
			{
				keys.add(Integer.parseInt(k.trim()));
			}
		}
		catch (Exception ex)
		{
			throw new AssertionError("failed reading " + resource + ": " + ex, ex);
		}
		return keys;
	}
}
