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
import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Clicking a boss name in the Route must open that boss.
 *
 * <p>This is tested rather than eyeballed because the obvious implementation silently does nothing:
 * AWT does not bubble mouse events the way the DOM does. A component that has any listener of its own
 * consumes them, so once the header label carries a hover tint, a click handler registered only on its
 * parent row never fires. The bug is invisible in a screenshot — the header looks right and simply does
 * not respond — so the click is simulated here.</p>
 */
public class RouteBossHeaderClickTest
{
	/** A maxed account partway to Elite: gives a route with several boss groups in it. */
	private static SidePanelViewModel model()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Hitpoints", "Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking",
			"Fishing", "Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, 99);
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
			.build(new ProgressSnapshot(Collections.emptySet(), 1000, 1000, null, 1L),
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

	/** True once the boss-detail screen is on show — it is the only view with this back button. */
	private static boolean showingBossDetail(Container panel)
	{
		for (Component child : panel.getComponents())
		{
			if (child instanceof javax.swing.AbstractButton
				&& "← All bosses".equals(((javax.swing.AbstractButton) child).getText()))
			{
				return true;
			}
			if (child instanceof Container && showingBossDetail((Container) child))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void clickingARouteBossHeaderOpensThatBoss() throws Exception
	{
		final boolean[] opened = {false};
		final String[] clicked = {null};

		SwingUtilities.invokeAndWait(() ->
		{
			CombatAchievementsPanel panel = new CombatAchievementsPanel(action -> { });
			panel.render(model());
			panel.showMode(PanelMode.ROUTE);

			assertFalse("precondition: not already on a boss page", showingBossDetail(panel));

			// The route's group headers are the bold labels carrying a bare boss name; the first one that
			// responds to a click is the one under test.
			for (JLabel l : labels(panel, new ArrayList<>()))
			{
				String text = l.getText();
				if (text == null || text.isEmpty() || text.startsWith("<html")
					|| l.getMouseListeners().length == 0)
				{
					continue;
				}
				clicked[0] = text;
				l.dispatchEvent(new MouseEvent(l, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
					0, 2, 2, 1, false));
				break;
			}
			opened[0] = showingBossDetail(panel);
		});

		assertTrue("expected to find a clickable boss header in the route", clicked[0] != null);
		assertTrue("clicking the boss header \"" + clicked[0] + "\" should open that boss", opened[0]);
	}
}
