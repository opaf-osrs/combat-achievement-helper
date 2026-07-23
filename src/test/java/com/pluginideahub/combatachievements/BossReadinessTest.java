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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Bosses tab used to be account-blind: its "Recommended" order was points-per-hour of the content
 * alone, so a level-3 and a combat-89 account both led with Dagannoth Kings, and the list was built from
 * the ungated ranking so a brand-new account saw Chambers of Xeric and Theatre of Blood too.
 */
public class BossReadinessTest
{
	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final RecStatsLibrary recStats = RecStatsLibrary.loadBundled();

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

	/** The boss list in the order the panel's default "Recommended" sort puts it. */
	private List<SidePanelViewModel.BossRow> bossesFor(PlayerProfile profile)
	{
		SidePanelViewModel vm = new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null);

		List<SidePanelViewModel.BossRow> rows = new ArrayList<>(vm.bosses());
		rows.sort(CombatAchievementsPanel.bossComparator(
			CombatAchievementsPanel.Sort.RECOMMENDED, 6, 1.0));
		return rows;
	}

	private static List<String> topMonsters(List<SidePanelViewModel.BossRow> rows, int n)
	{
		List<String> names = new ArrayList<>();
		for (int i = 0; i < Math.min(n, rows.size()); i++)
		{
			names.add(rows.get(i).monster);
		}
		return names;
	}

	@Test
	public void theRecommendedOrderRespondsToTheAccount()
	{
		// The bug: identical top-6 from a level-3 to a combat-89 account.
		assertNotEquals("a level-3 and a maxed-ish account must not get the same boss list",
			topMonsters(bossesFor(account(1)), 6), topMonsters(bossesFor(account(90)), 6));
	}

	@Test
	public void aLevelThreeIsNotSentToDagannothKings()
	{
		List<String> top = topMonsters(bossesFor(account(1)), 3);
		for (String monster : top)
		{
			assertFalse("a level-3's top bosses must not be Dagannoth Kings (" + top + ")",
				monster.startsWith("Dagannoth"));
		}
		assertTrue("its starter bosses lead instead (" + top + ")",
			top.contains("Bryophyta") || top.contains("Obor") || top.contains("Greater Demon"));
	}

	@Test
	public void anEstablishedAccountStillGetsTheEfficientBosses()
	{
		// The other direction: readiness must not flatten the list for someone who is ready for everything.
		List<String> top = topMonsters(bossesFor(account(90)), 6);
		assertTrue("a combat-114 account still leads with the efficient bosses (" + top + ")",
			top.stream().anyMatch(m -> m.startsWith("Dagannoth")));
	}

	@Test
	public void theDirectoryIsBeginnerGatedLikeEveryOtherSurface()
	{
		List<String> beginner = new ArrayList<>();
		for (SidePanelViewModel.BossRow b : bossesFor(account(1)))
		{
			beginner.add(b.monster);
		}
		assertFalse("no raids in a brand-new account's boss directory",
			beginner.contains("Chambers of Xeric"));
		assertFalse(beginner.contains("Theatre of Blood"));

		List<String> established = new ArrayList<>();
		for (SidePanelViewModel.BossRow b : bossesFor(account(70)))
		{
			established.add(b.monster);
		}
		assertTrue("but a combat-89 account browses the whole game",
			established.contains("Chambers of Xeric"));
	}

	@Test
	public void readinessIsCarriedOnTheRowAndIsNeutralByDefault()
	{
		for (SidePanelViewModel.BossRow b : bossesFor(account(90)))
		{
			assertTrue("a maxed account is ready for everything it can see (" + b.monster + ")",
				b.readinessSink < 3.0);
		}
		// Back-compat: the old constructor still yields a neutral row, so the raw directory is unaffected.
		assertEquals(1.0, new SidePanelViewModel.BossRow("X", 1, 1, 0, false, "",
			Collections.emptyList(), Collections.emptyList()).readinessSink, 1e-9);
	}
}
