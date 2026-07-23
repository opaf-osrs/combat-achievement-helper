package com.pluginideahub.combatachievements.core.effort;

import java.util.Collections;
import java.util.List;

/**
 * "Train this to open up CAs": one suggested training goal, with what it would unlock and what it costs.
 * Produced by {@link TrainingPlanner}. Pure value object.
 */
public final class TrainingSuggestion
{
	private final String label;
	private final List<String> skills;
	private final int targetLevel;
	private final int unlockedTaskCount;
	private final int unlockedPoints;
	private final int trainingMinutes;
	private final String unlocksHint;

	public TrainingSuggestion(String label, List<String> skills, int targetLevel, int unlockedTaskCount,
		int unlockedPoints, int trainingMinutes, String unlocksHint)
	{
		this.label = label == null ? "" : label;
		this.skills = Collections.unmodifiableList(
			skills == null ? Collections.emptyList() : skills);
		this.targetLevel = targetLevel;
		this.unlockedTaskCount = Math.max(0, unlockedTaskCount);
		this.unlockedPoints = Math.max(0, unlockedPoints);
		this.trainingMinutes = Math.max(0, trainingMinutes);
		this.unlocksHint = unlocksHint == null ? "" : unlocksHint;
	}

	/** Display label, e.g. "Fishing 35" or "All combat 20". */
	public String label()
	{
		return label;
	}

	/** The skill(s) this goal raises. */
	public List<String> skills()
	{
		return skills;
	}

	public int targetLevel()
	{
		return targetLevel;
	}

	/** How many CAs become genuinely attemptable once trained. */
	public int unlockedTaskCount()
	{
		return unlockedTaskCount;
	}

	public int unlockedPoints()
	{
		return unlockedPoints;
	}

	public int trainingMinutes()
	{
		return trainingMinutes;
	}

	/** The most-unlocked boss/activity, e.g. "Tempoross" — the reason this goal is worth it. */
	public String unlocksHint()
	{
		return unlocksHint;
	}

	/** Points opened per hour of training — the ranking metric, comparable with the quest unlocks. */
	public double score()
	{
		return trainingMinutes <= 0 ? unlockedPoints : unlockedPoints * 60.0 / trainingMinutes;
	}
}
