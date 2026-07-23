package com.pluginideahub.combatachievements.bridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pluginideahub.combatachievements.core.effort.MonsterNames;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Diagnostic + guard for hiscore KC coverage: which dataset monsters get a hiscore source via the
 * {@code MonsterNames} canonicaliser, which hiscore bosses map to a key no task uses (orphans), and
 * which dataset monsters have no hiscore source at all (expected: slayer/quest/sub-mode monsters).
 * Writes a human-readable report to {@code build/hiscore-coverage.txt}.
 */
public class HiscoreCoverageTest
{
	@Test
	public void reportHiscoreCoverage() throws Exception
	{
		Set<String> datasetMonsters = datasetMonsterKeys();

		TreeMap<String, String> matched = new TreeMap<>();   // hiscore boss -> dataset key
		TreeSet<String> orphanHiscore = new TreeSet<>();      // hiscore boss -> key not in dataset
		for (HiscoreSkill hs : HiscoreSkill.values())
		{
			if (hs.getType() != HiscoreSkillType.BOSS)
			{
				continue;
			}
			String key = MonsterNames.toDatasetKey(hs.getName());
			if (datasetMonsters.contains(key))
			{
				matched.put(hs.getName(), key);
			}
			else
			{
				orphanHiscore.add(hs.getName() + "  ->  " + key);
			}
		}

		TreeSet<String> uncovered = new TreeSet<>(datasetMonsters);
		uncovered.removeAll(matched.values());

		StringBuilder sb = new StringBuilder();
		sb.append("HISCORE KC COVERAGE\n===================\n\n");
		sb.append("Dataset monsters WITH a hiscore source: ").append(matched.size()).append("\n");
		for (String b : matched.keySet())
		{
			sb.append("  ").append(b).append("  ->  ").append(matched.get(b)).append('\n');
		}
		sb.append("\nHiscore bosses with NO matching dataset monster (orphans — harmless): ")
			.append(orphanHiscore.size()).append('\n');
		for (String o : orphanHiscore)
		{
			sb.append("  ").append(o).append('\n');
		}
		sb.append("\nDataset monsters with NO hiscore source (chat-only; expected slayer/quest/sub-mode): ")
			.append(uncovered.size()).append('\n');
		for (String u : uncovered)
		{
			sb.append("  ").append(u).append('\n');
		}

		Files.createDirectories(Paths.get("build"));
		Files.write(Paths.get("build/hiscore-coverage.txt"), sb.toString().getBytes(StandardCharsets.UTF_8));

		// Guard: the bulk of CA bosses must resolve to a hiscore source, and the alias-mapped ones in
		// particular must stay connected (a rename in the dataset or the HiscoreSkill enum fails here).
		assertTrue("expected most CA bosses to have a hiscore source; got " + matched.size(),
			matched.size() >= 60);
		for (String mustMap : new String[]{
			"vorkath", "zulrah", "barrows", "the nightmare", "crystalline hunllef", "corrupted hunllef",
			"moons of peril", "fortis colosseum", "royal titans", "leviathan", "whisperer",
			"tombs of amascut", "chambers of xeric", "theatre of blood"})
		{
			assertTrue("CA boss '" + mustMap + "' lost its hiscore source",
				matched.values().contains(mustMap));
		}
	}

	private static Set<String> datasetMonsterKeys()
	{
		Set<String> monsters = new TreeSet<>();
		try (InputStream in = HiscoreCoverageTest.class.getResourceAsStream(
			"/com/pluginideahub/combatachievements/combat_achievements.json"))
		{
			assertNotNull(in);
			JsonObject root = JsonParser.parseReader(
				new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement t : root.getAsJsonArray("tasks"))
			{
				JsonElement m = t.getAsJsonObject().get("monster");
				if (m != null && !m.isJsonNull())
				{
					String name = m.getAsString().trim();
					if (!name.isEmpty() && !"None".equalsIgnoreCase(name))
					{
						monsters.add(name.toLowerCase(Locale.ROOT));
					}
				}
			}
		}
		catch (Exception ex)
		{
			throw new AssertionError(ex);
		}
		return monsters;
	}
}
