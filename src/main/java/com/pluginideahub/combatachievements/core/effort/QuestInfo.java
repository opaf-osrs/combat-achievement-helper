package com.pluginideahub.combatachievements.core.effort;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Effort metadata for one quest in the CA-relevant universe: difficulty, length, an estimated
 * completion time, quest-point reward, skill requirements and the direct prerequisite quests (the
 * dependency edges). Pure value object sourced from the bundled {@code quests.json}.
 */
public final class QuestInfo
{
	public static final QuestInfo UNKNOWN = new QuestInfo(
		"", "", "", 0, 0, 0, false, Collections.emptyMap(), Collections.emptyList());

	private final String name;
	private final String difficulty;
	private final String length;
	private final int estMinutes;
	private final int effortScore;
	private final int questPoints;
	private final boolean members;
	private final Map<String, Integer> skillRequirements;
	private final List<String> directPrerequisiteQuests;

	public QuestInfo(String name, String difficulty, String length, int estMinutes, int effortScore,
		int questPoints, boolean members, Map<String, Integer> skillRequirements,
		List<String> directPrerequisiteQuests)
	{
		this.name = name == null ? "" : name;
		this.difficulty = difficulty == null ? "" : difficulty;
		this.length = length == null ? "" : length;
		this.estMinutes = Math.max(0, estMinutes);
		this.effortScore = Math.max(0, effortScore);
		this.questPoints = Math.max(0, questPoints);
		this.members = members;
		this.skillRequirements = Collections.unmodifiableMap(
			skillRequirements == null ? Collections.emptyMap() : new java.util.LinkedHashMap<>(skillRequirements));
		this.directPrerequisiteQuests = Collections.unmodifiableList(
			directPrerequisiteQuests == null ? Collections.emptyList()
				: new java.util.ArrayList<>(directPrerequisiteQuests));
	}

	public String name()
	{
		return name;
	}

	public String difficulty()
	{
		return difficulty;
	}

	public String length()
	{
		return length;
	}

	public int estMinutes()
	{
		return estMinutes;
	}

	public int effortScore()
	{
		return effortScore;
	}

	public int questPoints()
	{
		return questPoints;
	}

	public boolean members()
	{
		return members;
	}

	public Map<String, Integer> skillRequirements()
	{
		return skillRequirements;
	}

	public List<String> directPrerequisiteQuests()
	{
		return directPrerequisiteQuests;
	}

	public boolean isKnown()
	{
		return !name.isEmpty();
	}
}
