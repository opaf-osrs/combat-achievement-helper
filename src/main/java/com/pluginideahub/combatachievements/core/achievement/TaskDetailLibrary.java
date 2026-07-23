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
 * Loads the bundled {@code task_detail.json} (task id → {@link TaskDetail}) — the curated stats/setup/
 * strategy/items text for the CA-detail view. Sparse and safe: a missing/malformed file yields an
 * empty library where every lookup returns {@link TaskDetail#EMPTY}. Pure Java (Gson only). Mirrors
 * {@code TaskDifficultyLibrary}.
 */
public final class TaskDetailLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/task_detail.json";

	private final Map<Integer, TaskDetail> byId;

	private TaskDetailLibrary(Map<Integer, TaskDetail> byId)
	{
		this.byId = Collections.unmodifiableMap(byId);
	}

	public static TaskDetailLibrary empty()
	{
		return new TaskDetailLibrary(new LinkedHashMap<>());
	}

	public static TaskDetailLibrary loadBundled()
	{
		try (InputStream in = TaskDetailLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static TaskDetailLibrary load(InputStream in)
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
			Map<Integer, TaskDetail> map = new LinkedHashMap<>();
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
					map.put(id, new TaskDetail(opt(o, "stats"), opt(o, "setup"),
						opt(o, "strategy"), opt(o, "items")));
				}
				catch (RuntimeException ignored)
				{
					// skip malformed entry
				}
			}
			return new TaskDetailLibrary(map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static String opt(JsonObject o, String key)
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

	/** Curated detail for a task id, or {@link TaskDetail#EMPTY} when absent. */
	public TaskDetail detailFor(int taskId)
	{
		return byId.getOrDefault(taskId, TaskDetail.EMPTY);
	}
}
