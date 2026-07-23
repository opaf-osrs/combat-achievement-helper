package com.pluginideahub.combatachievements.core.effort;

import java.util.Collections;
import java.util.List;

/**
 * A "do this quest to open up CAs" suggestion: how many incomplete CAs (and points) completing a
 * quest would unlock, against the remaining effort to do that quest's chain plus train its unmet
 * skills. Pure value object; ranked by {@link #score()} (difficulty-weighted "achievable" points per
 * effort-minute — a quest that opens easy points is worth more than one opening equally many hard ones).
 */
public final class UnlockSuggestion
{
	private final String questName;
	private final String difficulty;
	private final int unlockedTaskCount;
	private final int unlockedPoints;
	private final int reachableTaskCount;
	private final int reachablePoints;
	private final int achievablePoints;
	private final int questMinutes;
	private final int trainingMinutes;
	private final List<String> remainingPrerequisites;
	private final List<String> unmetSkills;
	private final List<Integer> unlockedTaskIds;

	public UnlockSuggestion(String questName, String difficulty, int unlockedTaskCount,
		int unlockedPoints, int reachableTaskCount, int reachablePoints, int achievablePoints,
		int questMinutes, int trainingMinutes,
		List<String> remainingPrerequisites, List<String> unmetSkills, List<Integer> unlockedTaskIds)
	{
		this.questName = questName == null ? "" : questName;
		this.difficulty = difficulty == null ? "" : difficulty;
		this.unlockedTaskCount = unlockedTaskCount;
		this.unlockedPoints = unlockedPoints;
		this.reachableTaskCount = reachableTaskCount;
		this.reachablePoints = reachablePoints;
		this.achievablePoints = Math.max(0, achievablePoints);
		this.questMinutes = Math.max(0, questMinutes);
		this.trainingMinutes = Math.max(0, trainingMinutes);
		this.remainingPrerequisites = Collections.unmodifiableList(
			remainingPrerequisites == null ? Collections.emptyList() : remainingPrerequisites);
		this.unmetSkills = Collections.unmodifiableList(
			unmetSkills == null ? Collections.emptyList() : unmetSkills);
		this.unlockedTaskIds = Collections.unmodifiableList(
			unlockedTaskIds == null ? Collections.emptyList() : unlockedTaskIds);
	}

	public String questName()
	{
		return questName;
	}

	public String difficulty()
	{
		return difficulty;
	}

	public int unlockedTaskCount()
	{
		return unlockedTaskCount;
	}

	public int unlockedPoints()
	{
		return unlockedPoints;
	}

	/**
	 * How many of the unlocked CAs the player would actually be ready for once this quest chain's own
	 * skill requirements were met. The prize the panel shows: a quest that opens 15 CAs you are 40 levels
	 * short of has not opened 15 CAs.
	 */
	public int reachableTaskCount()
	{
		return reachableTaskCount;
	}

	/** Points from {@link #reachableTaskCount()} — the part of the prize you could go and collect. */
	public int reachablePoints()
	{
		return reachablePoints;
	}

	/**
	 * The REACHABLE points weighted by how easy those CAs are (difficulty-discounted): easy points count
	 * near full, hard points are worth less. Drives {@link #score()}; {@link #unlockedPoints()} stays the
	 * raw total for reference.
	 */
	public int achievablePoints()
	{
		return achievablePoints;
	}

	public int questMinutes()
	{
		return questMinutes;
	}

	public int trainingMinutes()
	{
		return trainingMinutes;
	}

	public int totalMinutes()
	{
		return questMinutes + trainingMinutes;
	}

	public List<String> remainingPrerequisites()
	{
		return remainingPrerequisites;
	}

	public List<String> unmetSkills()
	{
		return unmetSkills;
	}

	/** Ids of the incomplete CAs this quest would unlock (skills already met). */
	public List<Integer> unlockedTaskIds()
	{
		return unlockedTaskIds;
	}

	/**
	 * Difficulty-weighted achievable points per total effort-minute (quest chain + skill training).
	 * Higher is better. Uses {@link #achievablePoints()} so a quest opening easy points out-ranks one
	 * opening equally many hard points at the same time cost.
	 */
	public double score()
	{
		int total = totalMinutes();
		return total <= 0 ? achievablePoints : achievablePoints / (double) total;
	}
}
