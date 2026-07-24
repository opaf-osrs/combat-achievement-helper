package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Bosses page shows how far through a boss you are. Completed CAs never reach the ranker — it only
 * ranks what is left — so they are gathered separately, and this guards that they actually arrive.
 */
public class BossCompletedCasTest
{
	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();

	private static PlayerProfile maxed()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Hitpoints", "Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking",
			"Fishing", "Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, 99);
		}
		return PlayerProfile.of(levels);
	}

	private SidePanelViewModel model(Set<Integer> completed)
	{
		PlayerProfile profile = maxed();
		EffortDataLibrary effort = EffortDataLibrary.loadBundled();
		RecStatsLibrary rec = RecStatsLibrary.loadBundled();
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(rec)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(completed, 500, 500, null, 1L),
				new ProfileSignalsProvider(effort, rec, profile), null);
	}

	@Test
	public void aBossCarriesTheCAsAlreadyCompletedThere()
	{
		// Take a boss with several CAs and mark one of them done.
		Map<String, List<CombatAchievement>> byBoss = new HashMap<>();
		for (CombatAchievement t : lib.all())
		{
			if (t.hasMonster())
			{
				byBoss.computeIfAbsent(t.monster(), k -> new ArrayList<>()).add(t);
			}
		}
		Map.Entry<String, List<CombatAchievement>> pick = byBoss.entrySet().stream()
			.filter(e -> e.getValue().size() >= 3)
			.findFirst().orElseThrow(() -> new AssertionError("expected a boss with 3+ CAs"));
		String boss = pick.getKey();
		CombatAchievement done = pick.getValue().get(0);

		SidePanelViewModel before = model(new HashSet<>());
		SidePanelViewModel after = model(new HashSet<>(java.util.Collections.singletonList(done.id())));

		SidePanelViewModel.BossRow rowBefore = row(before, boss);
		SidePanelViewModel.BossRow rowAfter = row(after, boss);

		assertTrue("nothing completed to begin with", rowBefore.completedCas.isEmpty());
		assertEquals("the completed CA is listed against its boss", 1, rowAfter.completedCas.size());
		assertEquals(done.id(), rowAfter.completedCas.get(0).id);

		// It moved from outstanding to completed, so the boss's total is unchanged - that total is what
		// the "N of M done" figure divides by, and it must not drift as tasks are completed.
		assertEquals("total CAs at the boss is stable", rowBefore.totalCas(), rowAfter.totalCas());
		assertFalse("and it is no longer listed as something still to do",
			rowAfter.doable.stream().anyMatch(d -> d.id == done.id()));
	}

	private static SidePanelViewModel.BossRow row(SidePanelViewModel vm, String boss)
	{
		return vm.bosses().stream().filter(b -> boss.equals(b.monster)).findFirst()
			.orElseThrow(() -> new AssertionError("boss missing from the directory: " + boss));
	}
}
