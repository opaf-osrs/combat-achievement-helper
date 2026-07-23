package com.pluginideahub.combatachievements.core.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads bundled authored route guides ({@code guides.json}). Pure Java (Gson only). A missing or
 * malformed file yields an empty library rather than an error, so guide problems never break the
 * panel. See docs/DESIGN.md (Guide section).
 */
public final class GuideLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/guides.json";

	private final List<Guide> guides;

	private GuideLibrary(List<Guide> guides)
	{
		this.guides = Collections.unmodifiableList(guides);
	}

	public static GuideLibrary empty()
	{
		return new GuideLibrary(new ArrayList<>());
	}

	public static GuideLibrary loadBundled()
	{
		try (InputStream in = GuideLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
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

	public static GuideLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonElement guidesEl = parsed.getAsJsonObject().get("guides");
			if (guidesEl == null || !guidesEl.isJsonArray())
			{
				return empty();
			}
			List<Guide> guides = new ArrayList<>();
			for (JsonElement el : guidesEl.getAsJsonArray())
			{
				if (el.isJsonObject())
				{
					guides.add(parseGuide(el.getAsJsonObject()));
				}
			}
			return new GuideLibrary(guides);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	private static Guide parseGuide(JsonObject obj)
	{
		String id = optString(obj, "id");
		String title = optString(obj, "title");
		String author = optString(obj, "author");
		String summary = optString(obj, "summary");
		String videoUrl = optString(obj, "videoUrl");
		AchievementTier targetTier = AchievementTier.fromDisplayName(optString(obj, "targetTier"));

		List<String> tags = new ArrayList<>();
		JsonElement tagsEl = obj.get("tags");
		if (tagsEl != null && tagsEl.isJsonArray())
		{
			for (JsonElement t : tagsEl.getAsJsonArray())
			{
				tags.add(t.getAsString());
			}
		}

		List<GuideStep> steps = new ArrayList<>();
		JsonElement stepsEl = obj.get("steps");
		if (stepsEl != null && stepsEl.isJsonArray())
		{
			for (JsonElement s : stepsEl.getAsJsonArray())
			{
				if (s.isJsonObject())
				{
					JsonObject so = s.getAsJsonObject();
					steps.add(new GuideStep(optString(so, "note"), optInt(so, "taskId", -1)));
				}
				else if (s.isJsonPrimitive())
				{
					steps.add(new GuideStep(s.getAsString(), -1));
				}
			}
		}

		return new Guide(id, title, author, summary, videoUrl, targetTier, tags, steps);
	}

	private static String optString(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
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

	private static int optInt(JsonObject obj, String key, int fallback)
	{
		JsonElement el = obj.get(key);
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

	public List<Guide> all()
	{
		return guides;
	}

	public int count()
	{
		return guides.size();
	}
}
