package com.pluginideahub.combatachievements.core.video;

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
import java.util.Map;

/**
 * Loads curated per-task guide videos ({@code video_guides.json}, keyed by task id). Sparse and
 * decoupled: any task without a curated entry falls through to the {@link YouTubeSearch} fallback, so
 * the panel always offers a one-click guide. Pure Java (Gson only). See docs/DESIGN.md §6d.
 */
public final class VideoGuideLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/video_guides.json";

	private final Map<Integer, List<String>> byId;

	private VideoGuideLibrary(Map<Integer, List<String>> byId)
	{
		this.byId = byId;
	}

	public static VideoGuideLibrary empty()
	{
		return new VideoGuideLibrary(new LinkedHashMap<>());
	}

	/** Loads the bundled curated guides; returns {@link #empty()} if absent or unreadable. */
	public static VideoGuideLibrary loadBundled()
	{
		try (InputStream in = VideoGuideLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
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

	/** Loads from a stream; returns {@link #empty()} on malformed input. */
	public static VideoGuideLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			Map<Integer, List<String>> map = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet())
			{
				Integer id = tryParseId(entry.getKey());
				if (id == null)
				{
					continue;
				}
				List<String> urls = new ArrayList<>();
				JsonElement value = entry.getValue();
				if (value.isJsonArray())
				{
					for (JsonElement url : value.getAsJsonArray())
					{
						addUrl(urls, url);
					}
				}
				else
				{
					addUrl(urls, value);
				}
				if (!urls.isEmpty())
				{
					map.put(id, Collections.unmodifiableList(urls));
				}
			}
			return new VideoGuideLibrary(map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static void addUrl(List<String> urls, JsonElement el)
	{
		if (el == null || el.isJsonNull())
		{
			return;
		}
		try
		{
			String s = el.getAsString();
			if (s != null && !s.trim().isEmpty())
			{
				urls.add(s.trim());
			}
		}
		catch (RuntimeException ignored)
		{
			// skip malformed url entry
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

	public int curatedCount()
	{
		return byId.size();
	}

	public boolean hasCuratedGuide(int taskId)
	{
		List<String> urls = byId.get(taskId);
		return urls != null && !urls.isEmpty();
	}

	/** Curated URLs for a task (empty if none). */
	public List<String> guidesFor(int taskId)
	{
		return byId.getOrDefault(taskId, Collections.emptyList());
	}

	/**
	 * The best guide URL for a task: the first curated link if present, otherwise the YouTube-search
	 * fallback built from the task name.
	 */
	public String bestGuideUrl(int taskId, String taskName)
	{
		List<String> urls = byId.get(taskId);
		if (urls != null && !urls.isEmpty())
		{
			return urls.get(0);
		}
		return YouTubeSearch.urlFor(taskName);
	}
}
