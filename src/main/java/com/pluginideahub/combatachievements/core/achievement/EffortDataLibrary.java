package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the curated {@code task_effort.json} (see docs/DESIGN.md §4.6). Sparse with a safe default:
 * any task id without an entry resolves to {@link TaskEffortData#NEUTRAL}, so the ranker degrades
 * gracefully and never silently drops a task. Pure Java (Gson only); unit-testable without a client.
 *
 * <p>The dataset is optional — a missing or malformed effort file yields an empty library where
 * every lookup returns {@code NEUTRAL}, never an exception, so effort data problems never break the
 * panel.</p>
 */
public final class EffortDataLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/task_effort.json";

	private final String version;
	private final Map<Integer, TaskEffortData> byId;

	private EffortDataLibrary(String version, Map<Integer, TaskEffortData> byId)
	{
		this.version = version;
		this.byId = byId;
	}

	/** An empty library: every {@link #effortFor(int)} returns {@link TaskEffortData#NEUTRAL}. */
	public static EffortDataLibrary empty()
	{
		return new EffortDataLibrary("none", new LinkedHashMap<>());
	}

	/** Loads the bundled curated effort dataset; returns {@link #empty()} if absent or unreadable. */
	public static EffortDataLibrary loadBundled()
	{
		return fromRoot(BundledJson.readBundled(EffortDataLibrary.class, BUNDLED_RESOURCE));
	}

	/** Loads from an arbitrary stream; returns {@link #empty()} on any malformed input. */
	public static EffortDataLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static EffortDataLibrary fromRoot(JsonObject root)
	{
		if (root == null)
		{
			return empty();
		}
		return new EffortDataLibrary(BundledJson.optString(root, "version", "unknown"),
			BundledJson.idMap(root, "tasks", EffortDataLibrary::parseEntry));
	}

	private static TaskEffortData parseEntry(JsonObject obj)
	{
		String access = BundledJson.optString(obj, "access", "none");
		TaskEffortData.GearTier gearTier = TaskEffortData.GearTier.fromString(
			BundledJson.optString(obj, "gearTier", "mid"), TaskEffortData.GearTier.MID);
		TaskEffortData.Intensity rng = TaskEffortData.Intensity.fromString(
			BundledJson.optString(obj, "rng", "low"), TaskEffortData.Intensity.LOW);
		TaskEffortData.Intensity supply = TaskEffortData.Intensity.fromString(
			BundledJson.optString(obj, "supply", "low"), TaskEffortData.Intensity.LOW);
		boolean soloable = BundledJson.optBoolean(obj, "soloable", true);
		String minigame = BundledJson.optString(obj, "minigameOrRaid", "");

		Map<String, Integer> levelReqs = new LinkedHashMap<>();
		JsonElement reqsEl = obj.get("levelReqs");
		if (reqsEl != null && reqsEl.isJsonObject())
		{
			for (Map.Entry<String, JsonElement> e : reqsEl.getAsJsonObject().entrySet())
			{
				try
				{
					levelReqs.put(e.getKey(), e.getValue().getAsInt());
				}
				catch (RuntimeException ignored)
				{
					// skip malformed level requirement
				}
			}
		}

		List<QuestRequirement> questReqs = parseQuestReqs(obj.get("questReqs"));

		// Only flag "curated" when a real effort signal was authored — a levelReqs/questReqs-only entry
		// (the auto-imported reldo/wiki gates) carries neutral placeholder difficulty and must not pose
		// as curated.
		boolean curated = obj.has("gearTier") || obj.has("rng") || obj.has("supply")
			|| obj.has("access") || obj.has("soloable") || obj.has("minigameOrRaid");
		return new TaskEffortData(access, levelReqs, questReqs, gearTier, rng, supply, soloable,
			minigame, curated);
	}

	/**
	 * Parses the {@code questReqs} array. Each element is either a bare quest-name string (must be
	 * completed) or an object {@code {"name": "...", "startedSuffices": true}} for the few gates where
	 * merely starting the quest grants access. Malformed elements are skipped, never thrown.
	 */
	private static List<QuestRequirement> parseQuestReqs(JsonElement el)
	{
		List<QuestRequirement> out = new ArrayList<>();
		if (el == null || !el.isJsonArray())
		{
			return out;
		}
		JsonArray arr = el.getAsJsonArray();
		for (JsonElement item : arr)
		{
			try
			{
				if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString())
				{
					String name = item.getAsString();
					if (name != null && !name.trim().isEmpty())
					{
						out.add(new QuestRequirement(name, false));
					}
				}
				else if (item.isJsonObject())
				{
					JsonObject q = item.getAsJsonObject();
					String name = BundledJson.optString(q, "name", "");
					if (!name.trim().isEmpty())
					{
						boolean started = q.has("startedSuffices") && !q.get("startedSuffices").isJsonNull()
							&& q.get("startedSuffices").getAsBoolean();
						out.add(new QuestRequirement(name, started));
					}
				}
			}
			catch (RuntimeException ignored)
			{
				// skip malformed quest requirement
			}
		}
		return out;
	}

	public String version()
	{
		return version;
	}

	public int curatedCount()
	{
		return byId.size();
	}

	/** Curated effort for a task id, or {@link TaskEffortData#NEUTRAL} when none is present. */
	public TaskEffortData effortFor(int taskId)
	{
		return byId.getOrDefault(taskId, TaskEffortData.NEUTRAL);
	}
}
