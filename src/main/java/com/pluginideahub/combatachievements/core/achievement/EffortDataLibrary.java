package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
		try (InputStream in = EffortDataLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			if (in == null)
			{
				return empty();
			}
			return load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	/** Loads from an arbitrary stream; returns {@link #empty()} on any malformed input. */
	public static EffortDataLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonObject root = parsed.getAsJsonObject();
			String version = root.has("version") && !root.get("version").isJsonNull()
				? root.get("version").getAsString() : "unknown";

			Map<Integer, TaskEffortData> map = new LinkedHashMap<>();
			JsonElement tasksEl = root.get("tasks");
			if (tasksEl != null && tasksEl.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> entry : tasksEl.getAsJsonObject().entrySet())
				{
					Integer id = tryParseId(entry.getKey());
					if (id == null || !entry.getValue().isJsonObject())
					{
						continue;
					}
					map.put(id, parseEntry(entry.getValue().getAsJsonObject()));
				}
			}
			return new EffortDataLibrary(version, map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static Integer tryParseId(String key)
	{
		try
		{
			return Integer.parseInt(key.trim());
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static TaskEffortData parseEntry(JsonObject obj)
	{
		String access = optString(obj, "access", "none");
		TaskEffortData.GearTier gearTier =
			TaskEffortData.GearTier.fromString(optString(obj, "gearTier", "mid"), TaskEffortData.GearTier.MID);
		TaskEffortData.Intensity rng =
			TaskEffortData.Intensity.fromString(optString(obj, "rng", "low"), TaskEffortData.Intensity.LOW);
		TaskEffortData.Intensity supply =
			TaskEffortData.Intensity.fromString(optString(obj, "supply", "low"), TaskEffortData.Intensity.LOW);
		boolean soloable = !obj.has("soloable") || obj.get("soloable").isJsonNull()
			|| obj.get("soloable").getAsBoolean();
		String minigame = optString(obj, "minigameOrRaid", "");

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
					String name = optString(q, "name", "");
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

	private static String optString(JsonObject obj, String key, String fallback)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsString();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
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
