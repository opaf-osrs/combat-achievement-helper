package com.pluginideahub.combatachievements.core.video;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds the deterministic YouTube-search fallback URL used when a task has no curated video. Pure.
 * {@link URLEncoder} emits {@code +} for spaces (application/x-www-form-urlencoded), matching
 * YouTube's {@code search_query} format exactly. See docs/DESIGN.md §6d.
 */
public final class YouTubeSearch
{
	private static final String BASE = "https://www.youtube.com/results?search_query=";

	private YouTubeSearch()
	{
	}

	/** e.g. {@code "Abyssal Adept"} → {@code ...search_query=OSRS+Abyssal+Adept+combat+achievement+guide}. */
	public static String urlFor(String taskName)
	{
		String query = "OSRS " + (taskName == null ? "" : taskName) + " combat achievement guide";
		return BASE + URLEncoder.encode(query, StandardCharsets.UTF_8);
	}
}
