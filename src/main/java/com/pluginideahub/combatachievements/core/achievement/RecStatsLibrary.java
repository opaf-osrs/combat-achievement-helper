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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the bundled {@code rec_stats.json} (task id → recommended-stat requirements), parsed from the
 * curated "Recommended stats" text. Each task has HARD requirements (tagged (required)/(hard req) — real
 * gates) and SOFT requirements (recommendations). Sparse and safe: a missing/malformed file yields empty
 * lists everywhere, so a data problem never gates or sinks a task. Pure Java (Gson only).
 */
public final class RecStatsLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/rec_stats.json";

	private final Map<Integer, List<StatRequirement>> hard;
	private final Map<Integer, List<StatRequirement>> soft;

	private RecStatsLibrary(Map<Integer, List<StatRequirement>> hard, Map<Integer, List<StatRequirement>> soft)
	{
		this.hard = Collections.unmodifiableMap(hard);
		this.soft = Collections.unmodifiableMap(soft);
	}

	public static RecStatsLibrary empty()
	{
		return new RecStatsLibrary(new LinkedHashMap<>(), new LinkedHashMap<>());
	}

	public static RecStatsLibrary loadBundled()
	{
		try (InputStream in = RecStatsLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static RecStatsLibrary load(InputStream in)
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
			Map<Integer, List<StatRequirement>> hard = new LinkedHashMap<>();
			Map<Integer, List<StatRequirement>> soft = new LinkedHashMap<>();
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
					List<StatRequirement> h = parseReqs(o.get("hard"));
					List<StatRequirement> s = parseReqs(o.get("soft"));
					if (!h.isEmpty())
					{
						hard.put(id, h);
					}
					if (!s.isEmpty())
					{
						soft.put(id, s);
					}
				}
				catch (RuntimeException ignored)
				{
					// skip malformed entry
				}
			}
			return new RecStatsLibrary(hard, soft);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static List<StatRequirement> parseReqs(JsonElement el)
	{
		List<StatRequirement> out = new ArrayList<>();
		if (el == null || !el.isJsonArray())
		{
			return out;
		}
		JsonArray arr = el.getAsJsonArray();
		for (JsonElement e : arr)
		{
			if (e == null || !e.isJsonObject())
			{
				continue;
			}
			try
			{
				JsonObject o = e.getAsJsonObject();
				List<String> skills = new ArrayList<>();
				JsonElement sk = o.get("skills");
				if (sk != null && sk.isJsonArray())
				{
					for (JsonElement s : sk.getAsJsonArray())
					{
						skills.add(s.getAsString());
					}
				}
				int level = o.get("level").getAsInt();
				String modeStr = o.has("mode") ? o.get("mode").getAsString() : "all";
				StatRequirement.Mode mode;
				switch (modeStr.trim().toLowerCase(Locale.ROOT))
				{
					case "any":
						mode = StatRequirement.Mode.ANY;
						break;
					case "primary":
						mode = StatRequirement.Mode.PRIMARY;
						break;
					default:
						mode = StatRequirement.Mode.ALL;
						break;
				}
				if (!skills.isEmpty())
				{
					out.add(new StatRequirement(skills, level, mode));
				}
			}
			catch (RuntimeException ignored)
			{
				// skip malformed requirement
			}
		}
		return out;
	}

	/** HARD (required) stat gates for a task — real requirements; empty when none. */
	public List<StatRequirement> hardFor(int taskId)
	{
		return hard.getOrDefault(taskId, Collections.emptyList());
	}

	/** SOFT recommended stats for a task — recommendations; empty when none. */
	public List<StatRequirement> softFor(int taskId)
	{
		return soft.getOrDefault(taskId, Collections.emptyList());
	}

	public int count()
	{
		return Math.max(hard.size(), soft.size());
	}
}
