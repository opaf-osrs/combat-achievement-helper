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
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The quest page's boss groups start closed, and clicking a group's arrow opens it. Simulated with a
 * real mouse event rather than eyeballed: AWT does not bubble clicks, so a toggle that renders
 * perfectly can still be dead if the handler sits on the wrong component.
 */
public class UnlockBossGroupToggleTest
{
	/** An account with quests left to do, so the Route has "Unlock next" cards to drill into. */
	private static SidePanelViewModel model()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Hitpoints", "Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking",
			"Fishing", "Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, 80);
		}
		PlayerProfile profile = PlayerProfile.of(levels);
		EffortDataLibrary effort = EffortDataLibrary.loadBundled();
		RecStatsLibrary rec = RecStatsLibrary.loadBundled();
		return new SidePanelViewModelBuilder(CombatAchievementLibrary.loadBundled(), effort,
			VideoGuideLibrary.loadBundled(), GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(),
			EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(rec)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, rec, profile), null);
	}

	private static List<JLabel> labels(Container c, List<JLabel> out)
	{
		for (Component child : c.getComponents())
		{
			if (child instanceof JLabel)
			{
				out.add((JLabel) child);
			}
			if (child instanceof Container)
			{
				labels((Container) child, out);
			}
		}
		return out;
	}

	/** The quest page's group arrows are the only bare "▸ "/"▾ " labels on show. */
	private static List<JLabel> arrows(Container panel)
	{
		List<JLabel> out = new ArrayList<>();
		for (JLabel l : labels(panel, new ArrayList<>()))
		{
			if ("▸ ".equals(l.getText()) || "▾ ".equals(l.getText()))
			{
				out.add(l);
			}
		}
		return out;
	}

	@Test
	public void bossGroupsStartClosedAndOpenOnAClick() throws Exception
	{
		SidePanelViewModel vm = model();
		assertFalse("fixture must produce unlock suggestions", vm.unlocks().isEmpty());
		final int groupCas = vm.unlocks().get(0).unlockedCas.size();
		assertTrue("fixture's first quest must unlock something", groupCas > 0);

		SwingUtilities.invokeAndWait(() ->
		{
			CombatAchievementsPanel panel = new CombatAchievementsPanel(action -> { });
			panel.render(vm);
			panel.openFirstUnlockDetail();

			List<JLabel> closed = arrows(panel);
			assertTrue("the quest page shows at least one boss group", !closed.isEmpty());
			for (JLabel a : closed)
			{
				assertTrue("every group starts closed", "▸ ".equals(a.getText()));
			}
			int labelsBefore = labels(panel, new ArrayList<>()).size();

			JLabel first = closed.get(0);
			first.dispatchEvent(new MouseEvent(first, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 1, 1, 1, false));

			assertTrue("clicking the arrow opens the group",
				arrows(panel).stream().anyMatch(a -> "▾ ".equals(a.getText())));
			assertTrue("the group's CA cards are now on show",
				labels(panel, new ArrayList<>()).size() > labelsBefore);
		});
	}
}
