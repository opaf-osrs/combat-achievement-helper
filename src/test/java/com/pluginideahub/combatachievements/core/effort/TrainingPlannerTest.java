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
	public void rankedByPointsOpenedPerHourOfTraining()
	{
		List<TrainingSuggestion> out = plan(account(1));
		for (int i = 1; i < out.size(); i++)
		{
			assertTrue("suggestions must be ordered best-rate first",
				out.get(i - 1).score() >= out.get(i).score() - 1e-9);
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
		for (TrainingSuggestion s : plan(PlayerProfile.of(skiller, quests, quests)))
		{
			assertFalse("already has Firemaking 70 — must not be told to train it",
				s.label().startsWith("Firemaking"));
			assertFalse("already has Fishing 70 — must not be told to train it",
				s.label().startsWith("Fishing"));
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
