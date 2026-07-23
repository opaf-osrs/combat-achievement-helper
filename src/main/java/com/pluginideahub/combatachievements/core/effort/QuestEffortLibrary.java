package com.pluginideahub.combatachievements.core.effort;

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
		try (InputStream in = QuestEffortLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static QuestEffortLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonObject root = parsed.getAsJsonObject();
			String version = root.has("version") && !root.get("version").isJsonNull()
				? root.get("version").getAsString() : "unknown";

			Map<String, QuestInfo> map = new LinkedHashMap<>();
			JsonElement el = root.get("quests");
			if (el != null && el.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
				{
					if (!e.getValue().isJsonObject())
					{
						continue;
					}
					QuestInfo info = parse(e.getValue().getAsJsonObject());
					map.put(e.getKey().trim().toLowerCase(Locale.ROOT), info);
				}
			}
			return new QuestEffortLibrary(version, map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
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
		return new QuestInfo(optString(o, "name"), optString(o, "difficulty"), optString(o, "length"),
			optInt(o, "estMinutes"), optInt(o, "effortScore"), optInt(o, "questPoints"),
			optBool(o, "members"), skills, prereqs);
	}

	private static int optInt(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return 0;
		}
		try
		{
			return el.getAsInt();
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
	}

	private static boolean optBool(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		return el != null && !el.isJsonNull() && el.isJsonPrimitive() && tryBool(el);
	}

	private static boolean tryBool(JsonElement el)
	{
		try
		{
			return el.getAsBoolean();
		}
		catch (RuntimeException ex)
		{
			return false;
		}
	}

	private static String optString(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
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
