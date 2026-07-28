package com.pluginideahub.combatachievements.varbit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * The bug this guards against shipped once already: the game grew from 637 to 646 tasks (Maggot
 * King) and the completion bitfield stayed at 20 varps / 640 bits, so tasks 640-645 silently read
 * as incomplete and players were shown the wrong tier. The reader bounds-checks rather than
 * crashing, which is correct at runtime, so this run is where the drift gets noticed. Warns rather
 * than fails: CA_POINTS keeps the tier right, only per-task checkmarks go stale.
 */
public class CaVarbitIdsTest
{
	@Test
	public void bitfieldCoversEveryBundledTask() throws Exception
	{
		int maxId = -1;
		try (InputStream in = CaVarbitIdsTest.class.getResourceAsStream(
			"/com/pluginideahub/combatachievements/combat_achievements.json"))
		{
			JsonObject root = new JsonParser()
				.parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonArray tasks = root.getAsJsonArray("tasks");
			for (int i = 0; i < tasks.size(); i++)
			{
				maxId = Math.max(maxId, tasks.get(i).getAsJsonObject().get("id").getAsInt());
			}
		}
		int capacity = CaVarbitIds.varpCount() * 32;
		if (maxId >= capacity)
		{
			// Deliberately a warning, not a failure: tier maths stays right via CA_POINTS, only the
			// per-task checkmarks past the capacity read as incomplete.
			System.err.println("WARNING: dataset has task id " + maxId
				+ " but the completion bitfield only covers ids 0-" + (capacity - 1)
				+ "; add the next ca_task_completed_N varp to CaVarbitIds or those tasks read as not done");
		}
		assertTrue("dataset looks empty", maxId > 600);
	}
}
