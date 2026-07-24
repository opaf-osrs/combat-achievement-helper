package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(TaskDifficultyLibrary.class, BUNDLED_RESOURCE));
	}

	public static TaskDifficultyLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static TaskDifficultyLibrary fromRoot(JsonObject root)
	{
		return new TaskDifficultyLibrary(
			BundledJson.idMap(root, "tasks", TaskDifficultyLibrary::parseEntry));
	}

	private static TaskDifficulty parseEntry(JsonObject o)
	{
		return new TaskDifficulty(BundledJson.optInt(o, "difficulty", 3), BundledJson.optInt(o, "boss", 0),
			BundledJson.optDouble(o, "bump", 0.0), BundledJson.optString(o, "reason", ""),
			BundledJson.optDouble(o, "attempts", 0.0));
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
