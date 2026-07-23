package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.effort.BossDifficultyLibrary;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The beginner rule: raid / coordinated group content ("Endgame access") is held back until the player has
 * unlocked the Easy tier — UNLESS they are already 70+ combat, which means an established player who simply
 * has not started Combat Achievements and must not be treated as new.
 */
public class BeginnerGateTest
{
	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final RecStatsLibrary recStats = RecStatsLibrary.loadBundled();
	private final BossDifficultyLibrary bossDiff = BossDifficultyLibrary.loadBundled();

	private static PlayerProfile account(int everyStat)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking", "Fishing",
			"Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, everyStat);
		}
		levels.put("Hitpoints", Math.max(10, everyStat));
		Set<String> quests = new HashSet<>(Arrays.asList(
			"Priest in Peril", "The Restless Ghost", "Children of the Sun"));
		return PlayerProfile.of(levels, quests, quests);
	}

	/** Endgame-access tasks surfaced in the CAs list, for an account with no CA points yet. */
	private long gatedShown(PlayerProfile profile)
	{
		SidePanelViewModel vm = new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(bossDiff)
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null);
		return vm.quickWins().stream().filter(r -> bossDiff.isEndgameAccess(r.monster)).count();
	}

	@Test
	public void combatLevelMatchesTheGameFormula()
	{
		assertEquals("a brand-new account is combat 3", 3, account(1).combatLevel());
		assertEquals("all 99s is the max combat level", 126, account(99).combatLevel());
	}

	@Test
	public void aBeginnerIsNotShownRaidContent()
	{
		PlayerProfile fresh = account(1);
		assertTrue("precondition: a fresh account is low combat", fresh.combatLevel() < 70);
		assertEquals("no endgame-access content for a brand-new account", 0, gatedShown(fresh));
	}

	@Test
	public void anEstablishedPlayerWithNoCaPointsIsNotTreatedAsNew()
	{
		// The case the combat check exists for: a main who just installed the plugin has 0 CA points, but
		// hiding raids from them would be plainly wrong.
		PlayerProfile main = account(90);
		assertTrue("precondition: this is a high-combat account", main.combatLevel() >= 70);
		assertTrue("an established player sees endgame content immediately", gatedShown(main) > 0);
	}

	@Test
	public void theGateReleasesAtSeventyCombat()
	{
		// 40s across the board is combat 51 — still a beginner. 55s is combat 70 — released.
		assertTrue("combat 51 is still gated", account(40).combatLevel() < 70);
		assertEquals(0, gatedShown(account(40)));

		assertTrue("55s reaches combat 70", account(55).combatLevel() >= 70);
		assertTrue("and the gate releases", gatedShown(account(55)) > 0);
	}

	@Test
	public void unlockingTheEasyTierAlsoReleasesTheGateForALowLevel()
	{
		// The other half of the rule: CA points release it too, independent of combat level.
		PlayerProfile fresh = account(1);
		SidePanelViewModel vm = new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(bossDiff)
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), fresh, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 500, 500, null, 1L),
				new ProfileSignalsProvider(effort, recStats, fresh), null);
		assertTrue("plenty of CA points releases the gate too",
			vm.quickWins().stream().anyMatch(r -> bossDiff.isEndgameAccess(r.monster)));
	}
}
