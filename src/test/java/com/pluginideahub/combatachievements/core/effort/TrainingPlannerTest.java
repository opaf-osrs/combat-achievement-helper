package com.pluginideahub.combatachievements.core.effort;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The "Train next" planner: it must help a held-back account and stay silent for an established one. */
public class TrainingPlannerTest
{
	private static final String[] ALL = {
		"Attack", "Strength", "Defence", "Ranged", "Magic", "Hitpoints", "Prayer", "Slayer",
		"Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking", "Fishing",
		"Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"
	};

	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final RecStatsLibrary recStats = RecStatsLibrary.loadBundled();
	private final TrainingPlanner planner = new TrainingPlanner(SkillXpLibrary.loadBundled());

	private static PlayerProfile account(int everySkill)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : ALL)
		{
			levels.put(s, everySkill);
		}
		Set<String> quests = new HashSet<>(Arrays.asList("Priest in Peril", "The Restless Ghost"));
		return PlayerProfile.of(levels, quests, quests);
	}

	private List<TrainingSuggestion> plan(PlayerProfile p)
	{
		return planner.plan(lib.all(), Collections.emptySet(), effort, recStats, p, 6);
	}

	@Test
	public void suggestsTrainingForAnAccountHeldBackByLevels()
	{
		List<TrainingSuggestion> out = plan(account(1));
		assertFalse("a level-1 account is held back and should get advice", out.isEmpty());
		for (TrainingSuggestion s : out)
		{
			assertTrue("every suggestion must open at least one CA", s.unlockedTaskCount() > 0);
			assertTrue("and name what to train", !s.label().isEmpty());
		}
	}

	@Test
	public void staysSilentForAnAccountThatIsNotHeldBack()
	{
		// Self-suppressing: a maxed account gains nothing from any goal, so the section disappears rather
		// than inventing busywork.
		assertTrue("a maxed account needs no training advice", plan(account(99)).isEmpty());
	}

	@Test
	public void rankedByRateFirstThenTheBeginnerLadderAscending()
	{
		// The scored goals (skilling, and for graduated accounts the combat milestones) lead in
		// best-rate order; a beginner's combat rungs ride after them, lowest level first.
		List<TrainingSuggestion> out = plan(account(1));
		int firstRung = out.size();
		for (int i = 0; i < out.size(); i++)
		{
			if (out.get(i).isToward())
			{
				firstRung = i;
				break;
			}
		}
		for (int i = 1; i < firstRung; i++)
		{
			assertTrue("scored suggestions must be ordered best-rate first",
				out.get(i - 1).score() >= out.get(i).score() - 1e-9);
		}
		for (int i = firstRung; i < out.size(); i++)
		{
			assertTrue("everything after the first rung is a rung", out.get(i).isToward());
			assertTrue("each rung raises one skill", out.get(i).skills().size() == 1);
			if (i > firstRung)
			{
				assertTrue("rungs climb, lowest first",
					out.get(i - 1).targetLevel() <= out.get(i).targetLevel());
			}
		}
	}

	@Test
	public void aSkillerIsNotToldToTrainWhatTheyAlreadyHave()
	{
		// Wintertodt (Firemaking 50) and Tempoross (Fishing 35) are the headline skilling gates: a level-1
		// account should be pointed at them, a 70-skilling account should not.
		Map<String, Integer> skiller = new HashMap<>();
		for (String s : ALL)
		{
			skiller.put(s, 1);
		}
		for (String s : new String[]{"Firemaking", "Fishing", "Woodcutting", "Fletching", "Mining",
			"Farming", "Herblore", "Crafting", "Smithing", "Agility", "Thieving", "Hunter", "Runecraft",
			"Construction", "Cooking"})
		{
			skiller.put(s, 70);
		}
		Set<String> quests = new HashSet<>(Arrays.asList("Priest in Peril", "The Restless Ghost"));
		PlayerProfile p = PlayerProfile.of(skiller, quests, quests);
		for (TrainingSuggestion s : plan(p))
		{
			// The real invariant: a goal must aim ABOVE what the player has. "Fishing 76" for a
			// 70-fishing skiller is a legitimate goal (a Tempoross CA recommends it); "Fishing 35" is not.
			for (String skill : s.skills())
			{
				assertTrue("goal " + s.label() + " targets a level already reached",
					s.targetLevel() > p.levelOf(skill) || s.skills().size() > 1);
			}
		}
	}

	@Test
	public void hasNoSuggestionsWithoutAProfile()
	{
		assertTrue(planner.plan(lib.all(), Collections.emptySet(), effort, recStats,
			PlayerProfile.empty(), 6).isEmpty());
	}

	@Test
	public void everySuggestionCarriesTheTasksItOpens()
	{
		for (TrainingSuggestion s : plan(account(1)))
		{
			assertTrue("points opened should be positive", s.unlockedPoints() > 0);
			assertTrue("target level should be a real level", s.targetLevel() > 0 && s.targetLevel() <= 99);
		}
	}
}
