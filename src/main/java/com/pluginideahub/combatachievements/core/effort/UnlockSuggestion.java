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
	private final int worstSkillShortfall;

	public UnlockSuggestion(String questName, String difficulty, int unlockedTaskCount,
		int unlockedPoints, int reachableTaskCount, int reachablePoints, int achievablePoints,
		int questMinutes, int trainingMinutes,
		List<String> remainingPrerequisites, List<String> unmetSkills, List<Integer> unlockedTaskIds)
	{
		this(questName, difficulty, unlockedTaskCount, unlockedPoints, reachableTaskCount,
			reachablePoints, achievablePoints, questMinutes, trainingMinutes, remainingPrerequisites,
			unmetSkills, unlockedTaskIds, 0);
	}

	public UnlockSuggestion(String questName, String difficulty, int unlockedTaskCount,
		int unlockedPoints, int reachableTaskCount, int reachablePoints, int achievablePoints,
		int questMinutes, int trainingMinutes,
		List<String> remainingPrerequisites, List<String> unmetSkills, List<Integer> unlockedTaskIds,
		int worstSkillShortfall)
	{
		this(builder()
			.questName(questName)
			.difficulty(difficulty)
			.unlockedTaskCount(unlockedTaskCount)
			.unlockedPoints(unlockedPoints)
			.reachableTaskCount(reachableTaskCount)
			.reachablePoints(reachablePoints)
			.achievablePoints(achievablePoints)
			.questMinutes(questMinutes)
			.trainingMinutes(trainingMinutes)
			.remainingPrerequisites(remainingPrerequisites)
			.unmetSkills(unmetSkills)
			.unlockedTaskIds(unlockedTaskIds)
			.worstSkillShortfall(worstSkillShortfall));
	}

	private UnlockSuggestion(Builder builder)
	{
		this.worstSkillShortfall = Math.max(0, builder.worstSkillShortfall);
		this.questName = builder.questName == null ? "" : builder.questName;
		this.difficulty = builder.difficulty == null ? "" : builder.difficulty;
		this.unlockedTaskCount = builder.unlockedTaskCount;
		this.unlockedPoints = builder.unlockedPoints;
		this.reachableTaskCount = builder.reachableTaskCount;
		this.reachablePoints = builder.reachablePoints;
		this.achievablePoints = Math.max(0, builder.achievablePoints);
		this.questMinutes = Math.max(0, builder.questMinutes);
		this.trainingMinutes = Math.max(0, builder.trainingMinutes);
		this.remainingPrerequisites = Collections.unmodifiableList(
			builder.remainingPrerequisites == null ? Collections.emptyList() : builder.remainingPrerequisites);
		this.unmetSkills = Collections.unmodifiableList(
			builder.unmetSkills == null ? Collections.emptyList() : builder.unmetSkills);
		this.unlockedTaskIds = Collections.unmodifiableList(
			builder.unlockedTaskIds == null ? Collections.emptyList() : builder.unlockedTaskIds);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	/** Names each of the thirteen fields at the call site; unset fields keep their neutral defaults. */
	public static final class Builder
	{
		private String questName = "";
		private String difficulty = "";
		private int unlockedTaskCount;
		private int unlockedPoints;
		private int reachableTaskCount;
		private int reachablePoints;
		private int achievablePoints;
		private int questMinutes;
		private int trainingMinutes;
		private List<String> remainingPrerequisites;
		private List<String> unmetSkills;
		private List<Integer> unlockedTaskIds;
		private int worstSkillShortfall;

		public Builder questName(String questName)
		{
			this.questName = questName;
			return this;
		}

		public Builder difficulty(String difficulty)
		{
			this.difficulty = difficulty;
			return this;
		}

		public Builder unlockedTaskCount(int unlockedTaskCount)
		{
			this.unlockedTaskCount = unlockedTaskCount;
			return this;
		}

		public Builder unlockedPoints(int unlockedPoints)
		{
			this.unlockedPoints = unlockedPoints;
			return this;
		}

		public Builder reachableTaskCount(int reachableTaskCount)
		{
			this.reachableTaskCount = reachableTaskCount;
			return this;
		}

		public Builder reachablePoints(int reachablePoints)
		{
			this.reachablePoints = reachablePoints;
			return this;
		}

		public Builder achievablePoints(int achievablePoints)
		{
			this.achievablePoints = achievablePoints;
			return this;
		}

		public Builder questMinutes(int questMinutes)
		{
			this.questMinutes = questMinutes;
			return this;
		}

		public Builder trainingMinutes(int trainingMinutes)
		{
			this.trainingMinutes = trainingMinutes;
			return this;
		}

		public Builder remainingPrerequisites(List<String> remainingPrerequisites)
		{
			this.remainingPrerequisites = remainingPrerequisites;
			return this;
		}

		public Builder unmetSkills(List<String> unmetSkills)
		{
			this.unmetSkills = unmetSkills;
			return this;
		}

		public Builder unlockedTaskIds(List<Integer> unlockedTaskIds)
		{
			this.unlockedTaskIds = unlockedTaskIds;
			return this;
		}

		public Builder worstSkillShortfall(int worstSkillShortfall)
		{
			this.worstSkillShortfall = worstSkillShortfall;
			return this;
		}

		public UnlockSuggestion build()
		{
			return new UnlockSuggestion(this);
		}
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
	 * The single worst level gap across the quest chain's own skill requirements — how far the player is
	 * from being ABLE to do this quest, in levels. 0 when every requirement is met. This is the "is this
	 * quest anywhere near me?" number: a level-3 shown a Master questline needing 70s has a shortfall in
	 * the 60s, however many points it would open.
	 */
	public int worstSkillShortfall()
	{
		return worstSkillShortfall;
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
