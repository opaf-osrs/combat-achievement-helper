package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bundled Combat Achievements task dataset and exposes it as immutable
 * {@link CombatAchievement}s. Pure Java: depends only on Gson (a parser, not a RuneLite type), so it
 * is unit-testable with no game client. Mirrors the {@code BossLibrary.all()} access shape; the JSON
 * loading itself is new (roguescape's libraries are hardcoded) — see docs/DESIGN.md §2.1.
 *
 * <p>Points are <em>derived</em> from each task's tier on load, never trusted from the file.</p>
 */
public final class CombatAchievementLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/combat_achievements.json";

	private final String version;
	private final List<CombatAchievement> tasks;
	private final Map<Integer, CombatAchievement> byId;
	private final Map<Integer, Integer> pointsById;

	private CombatAchievementLibrary(String version, List<CombatAchievement> tasks)
	{
		this.version = version;
		this.tasks = Collections.unmodifiableList(tasks);
		Map<Integer, CombatAchievement> idMap = new LinkedHashMap<>();
		Map<Integer, Integer> pts = new LinkedHashMap<>();
		for (CombatAchievement task : tasks)
		{
			idMap.put(task.id(), task);
			pts.put(task.id(), task.points());
		}
		this.byId = Collections.unmodifiableMap(idMap);
		this.pointsById = Collections.unmodifiableMap(pts);
	}

	/** Loads the dataset bundled in the plugin jar. Throws {@link CombatAchievementDataException} on failure. */
	public static CombatAchievementLibrary loadBundled()
	{
		try (InputStream in = CombatAchievementLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			if (in == null)
			{
				throw new CombatAchievementDataException("Bundled dataset not found: " + BUNDLED_RESOURCE);
			}
			return load(in);
		}
		catch (IOException ex)
		{
			throw new CombatAchievementDataException("Failed to read bundled dataset", ex);
		}
	}

	/** Loads the dataset from an arbitrary stream (used by tests with fixtures). */
	public static CombatAchievementLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				throw new CombatAchievementDataException("Dataset root is not a JSON object");
			}
			JsonObject root = parsed.getAsJsonObject();
			String version = root.has("version") && !root.get("version").isJsonNull()
				? root.get("version").getAsString() : "unknown";

			JsonElement tasksElement = root.get("tasks");
			if (tasksElement == null || !tasksElement.isJsonArray())
			{
				throw new CombatAchievementDataException("Dataset has no 'tasks' array");
			}
			JsonArray taskArray = tasksElement.getAsJsonArray();
			if (taskArray.size() == 0)
			{
				throw new CombatAchievementDataException("Dataset 'tasks' array is empty");
			}

			List<CombatAchievement> tasks = new ArrayList<>(taskArray.size());
			for (JsonElement element : taskArray)
			{
				tasks.add(parseTask(element));
			}
			return new CombatAchievementLibrary(version, tasks);
		}
		catch (JsonSyntaxException | IllegalStateException ex)
		{
			throw new CombatAchievementDataException("Malformed dataset JSON", ex);
		}
		catch (IOException ex)
		{
			throw new CombatAchievementDataException("Failed to read dataset stream", ex);
		}
	}

	private static CombatAchievement parseTask(JsonElement element)
	{
		if (element == null || !element.isJsonObject())
		{
			throw new CombatAchievementDataException("Task entry is not a JSON object");
		}
		JsonObject obj = element.getAsJsonObject();
		int id = requireInt(obj, "id");
		String name = optString(obj, "name");

		AchievementTier tier = AchievementTier.fromDisplayName(optString(obj, "tier"));
		if (tier == null)
		{
			throw new CombatAchievementDataException(
				"Task " + id + " has unknown tier: " + optString(obj, "tier"));
		}
		TaskType type = TaskType.fromDisplayName(optString(obj, "type"));
		if (type == null)
		{
			throw new CombatAchievementDataException(
				"Task " + id + " has unknown type: " + optString(obj, "type"));
		}

		String monster = optString(obj, "monster");
		String description = optString(obj, "description");
		String leagueRegion = optString(obj, "leagueRegion");
		String wikiUrl = optString(obj, "wikiUrl");

		// Points are always derived from the tier, never trusted from the file.
		return new CombatAchievement(id, name, tier, monster, type, tier.pointsPerTask(),
			description, leagueRegion, wikiUrl);
	}

	private static int requireInt(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			throw new CombatAchievementDataException("Task missing required field: " + key);
		}
		try
		{
			return el.getAsInt();
		}
		catch (NumberFormatException | IllegalStateException ex)
		{
			throw new CombatAchievementDataException("Task field '" + key + "' is not an integer", ex);
		}
	}

	private static String optString(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return "";
		}
		if (el.isJsonArray())
		{
			JsonArray arr = el.getAsJsonArray();
			return arr.size() == 0 ? "" : arr.get(0).getAsString();
		}
		return el.getAsString();
	}

	public String version()
	{
		return version;
	}

	/** All tasks, in dataset order (sorted by id at build time). Unmodifiable. */
	public List<CombatAchievement> all()
	{
		return tasks;
	}

	public int taskCount()
	{
		return tasks.size();
	}

	public CombatAchievement byId(int id)
	{
		return byId.get(id);
	}

	/** Task id -> derived points. Single owner of this map; consumed by the varbit reader. */
	public Map<Integer, Integer> pointsById()
	{
		return pointsById;
	}

	public List<CombatAchievement> byTier(AchievementTier tier)
	{
		List<CombatAchievement> result = new ArrayList<>();
		for (CombatAchievement task : tasks)
		{
			if (task.tier() == tier)
			{
				result.add(task);
			}
		}
		return Collections.unmodifiableList(result);
	}
}
