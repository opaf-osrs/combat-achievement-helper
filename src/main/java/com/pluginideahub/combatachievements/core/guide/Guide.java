package com.pluginideahub.combatachievements.core.guide;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.util.Collections;
import java.util.List;

/**
 * An authored route guide (e.g. "0 → Medium: The Fastest Path"). Carries a walkthrough video, a
 * summary, authored tips, and an optional {@code targetTier} — when set, the UI also renders the
 * player's live optimal route to that tier from {@code OptimalPathSolver}, so an authored guide and
 * a personalised route live in one place. Pure value object.
 */
public final class Guide
{
	private final String id;
	private final String title;
	private final String author;
	private final String summary;
	private final String videoUrl;
	private final AchievementTier targetTier;
	private final List<String> tags;
	private final List<GuideStep> steps;

	public Guide(String id, String title, String author, String summary, String videoUrl,
		AchievementTier targetTier, List<String> tags, List<GuideStep> steps)
	{
		this.id = id == null ? "" : id;
		this.title = title == null ? "" : title;
		this.author = author == null ? "" : author;
		this.summary = summary == null ? "" : summary;
		this.videoUrl = videoUrl == null ? "" : videoUrl;
		this.targetTier = targetTier;
		this.tags = Collections.unmodifiableList(tags);
		this.steps = Collections.unmodifiableList(steps);
	}

	public String id()
	{
		return id;
	}

	public String title()
	{
		return title;
	}

	public String author()
	{
		return author;
	}

	public String summary()
	{
		return summary;
	}

	public String videoUrl()
	{
		return videoUrl;
	}

	public boolean hasVideo()
	{
		return !videoUrl.isEmpty();
	}

	/** The tier this guide routes toward, or null for a non-route guide. */
	public AchievementTier targetTier()
	{
		return targetTier;
	}

	public List<String> tags()
	{
		return tags;
	}

	public List<GuideStep> steps()
	{
		return steps;
	}
}
