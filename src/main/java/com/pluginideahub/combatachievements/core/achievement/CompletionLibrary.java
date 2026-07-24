package com.pluginideahub.combatachievements.core.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the wiki's player-completion percentage per task. Not read at runtime right now — the
 * curated 1-10 Difficulty replaced completion% as the difficulty signal (popular-but-hard milestones
 * like Fight Caves skewed it) — but the data is kept current for a planned rarity display
 * ("only 2.5% of players have this"). Pure Java (Gson only).
 */
public final class CompletionLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/task_completion.json";

	private final Map<Integer, Double> percentById;

	private CompletionLibrary(Map<Integer, Double> percentById)
	{
		this.percentById = Collections.unmodifiableMap(percentById);
	}

	public static CompletionLibrary empty()
	{
		return new CompletionLibrary(new LinkedHashMap<>());
	}

	public static CompletionLibrary loadBundled()
	{
		return fromRoot(BundledJson.readBundled(CompletionLibrary.class, BUNDLED_RESOURCE));
	}

	public static CompletionLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static CompletionLibrary fromRoot(JsonObject root)
	{
		return new CompletionLibrary(BundledJson.idMapValues(root, "tasks", JsonElement::getAsDouble));
	}

	public int count()
	{
		return percentById.size();
	}

	/** Completion percentage for a task (0–100), or a negative value when unknown. */
	public double completionFor(int taskId)
	{
		Double pct = percentById.get(taskId);
		return pct == null ? -1.0 : pct;
	}
}
