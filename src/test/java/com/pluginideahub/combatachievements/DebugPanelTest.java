package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.debug.DebugSimulation;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ui.PanelAction;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The developer-mode simulation controls. The property that matters most is the gate: a simulation left
 * running must become inert the moment developer mode goes off, or the panel would keep reporting fake
 * figures for a real account with no visible sign of it.
 */
public class DebugPanelTest
{
	private final List<PanelAction> actions = new CopyOnWriteArrayList<>();

	private CombatAchievementsPanel panel()
	{
		return new CombatAchievementsPanel(actions::add);
	}

	/** Swing state must be touched on the EDT, and the panel's setters defer when called off it. */
	private static void onEdt(Runnable body) throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(body);
	}

	private static PlayerProfile maxed()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : DebugSimulation.SKILLS)
		{
			levels.put(s, 99);
		}
		return PlayerProfile.of(levels);
	}

	@Test
	public void aSimulationIsInertUntilDeveloperModeIsOn() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(false);
			panel.applyLevelPreset(1);

			assertFalse("developer mode off means the plugin is handed no simulation",
				panel.debugSimulation().isActive());

			panel.setDeveloperMode(true);
			assertTrue("and gets it once developer mode is on", panel.debugSimulation().isActive());

			// The guard that matters: turning developer mode back off must not leave a live simulation
			// silently rewriting a real account's panel.
			panel.setDeveloperMode(false);
			assertFalse("switching developer mode off retires the simulation",
				panel.debugSimulation().isActive());
		});
	}

	@Test
	public void aPresetSimulatesEverySkillAtOnce() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);
			panel.applyLevelPreset(1);

			DebugSimulation sim = panel.debugSimulation();
			assertEquals("all 23 skills, so nothing is left at the real level",
				23, sim.levelOverrides().size());
			assertEquals("a maxed account now reads as a brand-new one",
				3, sim.apply(maxed()).combatLevel());
			assertEquals("Hitpoints keeps its floor of 10", 10, panel.devSpinnerValue("Hitpoints"));
		});
	}

	@Test
	public void realGivesTheAccountItsOwnLevelsBack() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);
			panel.applyLevelPreset(40);
			assertTrue(panel.debugSimulation().isActive());

			panel.clearLevelSimulation();
			assertFalse("no override left, so the real profile passes through",
				panel.debugSimulation().isActive());
			assertEquals(99, panel.debugSimulation().apply(maxed()).levelOf("Attack"));
		});
	}

	@Test
	public void zeroCompletionIsIndependentOfTheLevels() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);
			panel.setZeroCompletion(true);

			DebugSimulation sim = panel.debugSimulation();
			assertTrue("hiding completions alone is a simulation", sim.isActive());
			assertTrue(sim.zeroCompletion());
			assertTrue("and it did not invent level overrides", sim.levelOverrides().isEmpty());
		});
	}

	@Test
	public void theRealAccountSeedsTheSpinnersButNeverClobbersASimulation() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);

			Map<String, Integer> real = new HashMap<>();
			real.put("Attack", 70);
			panel.setRealLevels(real);
			assertEquals("the grid starts from the truth", 70, panel.devSpinnerValue("Attack"));

			panel.applyLevelPreset(1);
			assertEquals(1, panel.devSpinnerValue("Attack"));

			// Every refresh pushes the real levels again — while simulating, that must be ignored, or the
			// next game tick would overwrite what was just dialled in.
			panel.setRealLevels(real);
			assertEquals("a refresh mid-simulation leaves the dialled-in value alone",
				1, panel.devSpinnerValue("Attack"));

			panel.clearLevelSimulation();
			panel.setRealLevels(real);
			assertEquals("and the real level returns once the simulation is cleared",
				70, panel.devSpinnerValue("Attack"));
		});
	}

	@Test
	public void hidingCompletionsDoesNotFreezeTheLevelGrid() throws Exception
	{
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);

			Map<String, Integer> real = new HashMap<>();
			real.put("Attack", 70);
			panel.setRealLevels(real);

			// "No CAs completed" is about progress, not levels. It leaves a simulation active, so gating the
			// re-seed on that alone used to freeze the grid: "Real" would restore the account's levels in the
			// panel while the spinners still showed the old preset, and one nudge re-applied them.
			panel.setZeroCompletion(true);
			panel.applyLevelPreset(1);
			assertEquals(1, panel.devSpinnerValue("Attack"));

			panel.clearLevelSimulation();
			panel.setRealLevels(real);
			assertEquals("the grid follows the real account again even while completions stay hidden",
				70, panel.devSpinnerValue("Attack"));
			assertTrue("and hiding completions is still in force",
				panel.debugSimulation().zeroCompletion());
		});
	}

	@Test
	public void everyControlAsksThePluginForAFreshModel() throws Exception
	{
		actions.clear();
		onEdt(() ->
		{
			CombatAchievementsPanel panel = panel();
			panel.setDeveloperMode(true);
			panel.applyLevelPreset(60);
			panel.setZeroCompletion(true);
			panel.clearLevelSimulation();
		});
		assertEquals("a simulation is useless if the panel does not re-render",
			3, actions.stream().filter(a -> a == PanelAction.REFRESH).count());
	}
}
