package com.pluginideahub.combatachievements.core.effort;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pluginideahub.combatachievements.core.data.BundledJson;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads the bundled {@code quests.json} quest-effort graph (quest name → {@link QuestInfo}). Sparse
 * and safe: a missing/malformed file yields an empty library. Beyond per-quest lookup it walks the
 * {@code directPrerequisiteQuests} edges to answer "what is the full chain behind this quest, and how
 * much of it does a player still have to do?". Pure Java (Gson only).
 */
public final class QuestEffortLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/quests.json";

	private final String version;
	private final Map<String, QuestInfo> byName;

	private QuestEffortLibrary(String version, Map<String, QuestInfo> byName)
	{
		this.version = version;
		this.byName = byName;
	}

	public static QuestEffortLibrary empty()
	{
		return new QuestEffortLibrary("none", new LinkedHashMap<>());
	}

	public static QuestEffortLibrary loadBundled()
	{
		return fromRoot(BundledJson.readBundled(QuestEffortLibrary.class, BUNDLED_RESOURCE));
	}

	public static QuestEffortLibrary load(InputStream in)
	{
		return fromRoot(BundledJson.read(in));
	}

	private static QuestEffortLibrary fromRoot(JsonObject root)
	{
		if (root == null)
		{
			return empty();
		}
		return new QuestEffortLibrary(BundledJson.optString(root, "version", "unknown"),
			BundledJson.nameMap(root, "quests", (name, o) -> parse(o)));
	}

	private static QuestInfo parse(JsonObject o)
	{
		Map<String, Integer> skills = new LinkedHashMap<>();
		JsonElement skEl = o.get("skillRequirements");
		if (skEl != null && skEl.isJsonObject())
		{
			for (Map.Entry<String, JsonElement> s : skEl.getAsJsonObject().entrySet())
			{
				try
				{
					skills.put(s.getKey(), s.getValue().getAsInt());
				}
				catch (RuntimeException ignored)
				{
					// skip malformed skill level
				}
			}
		}
		List<String> prereqs = new ArrayList<>();
		JsonElement pq = o.get("directPrerequisiteQuests");
		if (pq != null && pq.isJsonArray())
		{
			for (JsonElement q : pq.getAsJsonArray())
			{
				try
				{
					if (q.isJsonPrimitive() && q.getAsJsonPrimitive().isString())
					{
						prereqs.add(q.getAsString());
					}
				}
				catch (RuntimeException ignored)
				{
					// skip malformed prerequisite
				}
			}
		}
		return new QuestInfo(BundledJson.optString(o, "name", ""),
			BundledJson.optString(o, "difficulty", ""), BundledJson.optString(o, "length", ""),
			BundledJson.optInt(o, "estMinutes", 0), BundledJson.optInt(o, "effortScore", 0),
			BundledJson.optInt(o, "questPoints", 0), BundledJson.optBoolean(o, "members", false),
			skills, prereqs, BundledJson.optString(o, "wikiUrl", ""));
	}

	public String version()
	{
		return version;
	}

	public int count()
	{
		return byName.size();
	}

	/** Quest metadata (case-insensitive), or {@link QuestInfo#UNKNOWN} when absent. */
	public QuestInfo questFor(String name)
	{
		if (name == null)
		{
			return QuestInfo.UNKNOWN;
		}
		return byName.getOrDefault(name.trim().toLowerCase(Locale.ROOT), QuestInfo.UNKNOWN);
	}

	/**
	 * The full recursive set of prerequisite quests behind {@code questName} (not including the quest
	 * itself), resolved by walking the dependency edges. Cycles and unknown quests are handled safely.
	 */
	public Set<String> fullPrerequisites(String questName)
	{
		Set<String> out = new LinkedHashSet<>();
		collectPrereqs(questName, out, new LinkedHashSet<>());
		return out;
	}

	private void collectPrereqs(String questName, Set<String> out, Set<String> visiting)
	{
		String key = questName == null ? "" : questName.trim().toLowerCase(Locale.ROOT);
		if (key.isEmpty() || visiting.contains(key))
		{
			return;
		}
		visiting.add(key);
		for (String pq : questFor(questName).directPrerequisiteQuests())
		{
			out.add(pq);
			collectPrereqs(pq, out, visiting);
		}
	}

	/**
	 * Estimated minutes of questing a player still has to do to complete {@code questName}: the quest
	 * plus every prerequisite they have not yet finished. Skill-training time is handled separately
	 * (see {@code SkillXpLibrary}). Quests with no data contribute 0.
	 */
	public int remainingQuestMinutes(String questName, Set<String> completedQuestsLower)
	{
		Set<String> needed = new LinkedHashSet<>(fullPrerequisites(questName));
		needed.add(questName);
		int total = 0;
		for (String q : needed)
		{
			String key = q.trim().toLowerCase(Locale.ROOT);
			if (completedQuestsLower != null && completedQuestsLower.contains(key))
			{
				continue;
			}
			total += questFor(q).estMinutes();
		}
		return total;
	}
}
