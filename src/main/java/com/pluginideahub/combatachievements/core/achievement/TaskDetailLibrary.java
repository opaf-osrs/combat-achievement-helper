package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(TaskDetailLibrary.class, BUNDLED_RESOURCE));
	}

	public static TaskDetailLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static TaskDetailLibrary fromRoot(JsonObject root)
	{
		return new TaskDetailLibrary(BundledJson.idMap(root, "tasks", TaskDetailLibrary::parseDetail));
	}

	private static TaskDetail parseDetail(JsonObject o)
	{
		return new TaskDetail(BundledJson.optString(o, "stats", ""), BundledJson.optString(o, "setup", ""),
			BundledJson.optString(o, "strategy", ""), BundledJson.optString(o, "items", ""));
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
