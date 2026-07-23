package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the bundled {@code task_difficulty.json} (task id → {@link TaskDifficulty}), the curated
 * pure-skill Difficulty compiled from the boss ratings + keyword bumps. Sparse and safe: a
 * missing/malformed file yields an empty library where every lookup returns {@link
 * TaskDifficulty#UNKNOWN}, so a data problem never breaks the ranker. Pure Java (Gson only). Mirrors
 * {@code CompletionLibrary}. See CONTEXT.md "Difficulty".
 */
public final class TaskDifficultyLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/task_difficulty.json";

	private final Map<Integer, TaskDifficulty> byId;

	private TaskDifficultyLibrary(Map<Integer, TaskDifficulty> byId)
	{
		this.byId = Collections.unmodifiableMap(byId);
	}

	public static TaskDifficultyLibrary empty()
	{
		return new TaskDifficultyLibrary(new LinkedHashMap<>());
	}

	public static TaskDifficultyLibrary loadBundled()
	{
		try (InputStream in = TaskDifficultyLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static TaskDifficultyLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonElement tasksEl = parsed.getAsJsonObject().get("tasks");
			if (tasksEl == null || !tasksEl.isJsonObject())
			{
				return empty();
			}
			Map<Integer, TaskDifficulty> map = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : tasksEl.getAsJsonObject().entrySet())
			{
				if (entry.getValue() == null || !entry.getValue().isJsonObject())
				{
					continue;
				}
				try
				{
					int id = Integer.parseInt(entry.getKey().trim());
					JsonObject o = entry.getValue().getAsJsonObject();
					int difficulty = optInt(o, "difficulty", 3);
					int boss = optInt(o, "boss", 0);
					double bump = optDouble(o, "bump");
					String reason = optString(o, "reason");
					double attempts = optDouble(o, "attempts");
					map.put(id, new TaskDifficulty(difficulty, boss, bump, reason, attempts));
				}
				catch (RuntimeException ignored)
				{
					// skip malformed entry
				}
			}
			return new TaskDifficultyLibrary(map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static int optInt(JsonObject o, String key, int fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsInt();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	private static double optDouble(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return 0.0;
		}
		try
		{
			return el.getAsDouble();
		}
		catch (RuntimeException ex)
		{
			return 0.0;
		}
	}

	private static String optString(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return "";
		}
		try
		{
			return el.getAsString();
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	public int count()
	{
		return byId.size();
	}

	/** Difficulty for a task id, or {@link TaskDifficulty#UNKNOWN} (difficulty 3) when absent. */
	public TaskDifficulty difficultyFor(int taskId)
	{
		return byId.getOrDefault(taskId, TaskDifficulty.UNKNOWN);
	}
}
