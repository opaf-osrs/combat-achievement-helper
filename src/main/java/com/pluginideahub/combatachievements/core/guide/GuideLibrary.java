package com.pluginideahub.combatachievements.core.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
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
		return fromRoot(BundledJson.readBundled(GuideLibrary.class, BUNDLED_RESOURCE));
	}

	public static GuideLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static GuideLibrary fromRoot(JsonObject root)
	{
		JsonElement guidesEl = root == null ? null : root.get("guides");
		if (guidesEl == null || !guidesEl.isJsonArray())
		{
			return empty();
		}
		// Guides are an ordered, authored list — one broken guide means a broken file, so the whole
		// library is discarded rather than a guide silently dropped.
		try
		{
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
		catch (RuntimeException ex)
		{
			return empty();
		}
	}

	private static Guide parseGuide(JsonObject obj)
	{
		String id = BundledJson.optString(obj, "id", "");
		String title = BundledJson.optString(obj, "title", "");
		String author = BundledJson.optString(obj, "author", "");
		String summary = BundledJson.optString(obj, "summary", "");
		String videoUrl = BundledJson.optString(obj, "videoUrl", "");
		AchievementTier targetTier = AchievementTier.fromDisplayName(
			BundledJson.optString(obj, "targetTier", ""));

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
					steps.add(new GuideStep(BundledJson.optString(so, "note", ""),
						BundledJson.optInt(so, "taskId", -1)));
				}
				else if (s.isJsonPrimitive())
				{
					steps.add(new GuideStep(s.getAsString(), -1));
				}
			}
		}

		return new Guide(id, title, author, summary, videoUrl, targetTier, tags, steps);
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
