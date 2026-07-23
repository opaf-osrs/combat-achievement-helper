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
 * Loads the wiki's player-completion percentage per task (the rarity/difficulty backbone). A lower
 * percentage means fewer players have done it — a strong, holistic difficulty signal that captures
 * gear/RNG/skill the synthetic effort weights can't. Pure Java (Gson only). See
 * docs/REQUIREMENTS-RESEARCH.md.
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
		try (InputStream in = CompletionLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
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

	public static CompletionLibrary load(InputStream in)
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
			Map<Integer, Double> map = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : tasksEl.getAsJsonObject().entrySet())
			{
				try
				{
					map.put(Integer.parseInt(entry.getKey().trim()), entry.getValue().getAsDouble());
				}
				catch (RuntimeException ignored)
				{
					// skip malformed entry
				}
			}
			return new CompletionLibrary(map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
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
