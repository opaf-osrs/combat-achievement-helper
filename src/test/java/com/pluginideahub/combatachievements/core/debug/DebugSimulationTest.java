package com.pluginideahub.combatachievements.core.debug;

import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DebugSimulationTest
{
	private static PlayerProfile maxed()
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : DebugSimulation.SKILLS)
		{
			levels.put(s, 99);
		}
		Set<String> quests = new HashSet<>(Arrays.asList("Dragon Slayer II", "Priest in Peril"));
		return PlayerProfile.of(levels, quests, quests);
	}

	private static ProgressSnapshot realProgress()
	{
		return new ProgressSnapshot(new HashSet<>(Arrays.asList(1, 2, 3)), 900, 900, null, 4242L);
	}

	@Test
	public void noneIsTheIdentity()
	{
		DebugSimulation none = DebugSimulation.none();
		PlayerProfile profile = maxed();
		ProgressSnapshot progress = realProgress();

		assertFalse(none.isActive());
		assertSame("the real profile is passed straight through", profile, none.apply(profile));
		assertSame("as is the real progress", progress, none.apply(progress));
	}

	@Test
	public void anEmptyOverrideCollapsesToNone()
	{
		assertFalse(DebugSimulation.of(Collections.emptyMap(), false).isActive());
		assertFalse(DebugSimulation.of(null, false).isActive());
	}

	@Test
	public void simulatedLevelsTurnAMaxedAccountIntoALevelThree()
	{
		DebugSimulation sim = DebugSimulation.of(DebugSimulation.allSkillsAt(1), false);
		PlayerProfile simulated = sim.apply(maxed());

		assertEquals("all-1s with the 10 Hitpoints floor is combat 3", 3, simulated.combatLevel());
		assertEquals(1, simulated.levelOf("Attack"));
		assertEquals("Hitpoints cannot go below 10", 10, simulated.levelOf("Hitpoints"));
	}

	@Test
	public void simulatingLevelsLeavesQuestsAlone()
	{
		// Levels alone must not touch the quest log — that is a separate switch.
		PlayerProfile simulated = DebugSimulation.of(DebugSimulation.allSkillsAt(1), false).apply(maxed());
		assertTrue("real quest completions survive", simulated.hasCompleted("Dragon Slayer II"));
	}

	@Test
	public void zeroQuestsBlanksTheQuestLogButKeepsTheLevels()
	{
		// Needed to reproduce a beginner: with the real quest log intact, quest-gated content stays
		// unlocked and "Unlock next" stays empty, because a quest already done is not an unlock.
		DebugSimulation sim = DebugSimulation.of(Collections.emptyMap(), false, true);
		PlayerProfile simulated = sim.apply(maxed());

		assertTrue("blanking quests alone is a simulation", sim.isActive());
		assertTrue(sim.zeroQuests());
		assertFalse("no quest reads as completed", simulated.hasCompleted("Dragon Slayer II"));
		assertFalse("nor as started", simulated.hasStarted("Dragon Slayer II"));
		assertEquals("levels are untouched", 99, simulated.levelOf("Attack"));
	}

	@Test
	public void levelsAndQuestsCanBeSimulatedTogether()
	{
		PlayerProfile simulated = DebugSimulation
			.of(DebugSimulation.allSkillsAt(1), true, true)
			.apply(maxed());

		assertEquals("a full fresh-account simulation", 3, simulated.combatLevel());
		assertFalse(simulated.hasCompleted("Priest in Peril"));
	}

	@Test
	public void everyOsrsSkillIsCovered()
	{
		assertEquals("all 23 skills, so a preset leaves nothing at its real level",
			23, DebugSimulation.SKILLS.size());
		assertEquals(23, DebugSimulation.allSkillsAt(50).size());
	}

	@Test
	public void levelsAreClampedToTheGamesRange()
	{
		Map<String, Integer> silly = new HashMap<>();
		silly.put("Attack", 200);
		silly.put("Strength", -5);
		silly.put("Hitpoints", 1);
		DebugSimulation sim = DebugSimulation.of(silly, false);

		assertEquals(99, sim.levelOf("Attack", 0));
		assertEquals(1, sim.levelOf("Strength", 0));
		assertEquals(10, sim.levelOf("Hitpoints", 0));
	}

	@Test
	public void zeroCompletionClearsProgressButKeepsTheAccountSynced()
	{
		DebugSimulation sim = DebugSimulation.of(Collections.emptyMap(), true);
		ProgressSnapshot simulated = sim.apply(realProgress());

		assertTrue("still a present, real-account snapshot — not the logged-out state",
			simulated.isPresent());
		assertEquals(0, simulated.completedCount());
		assertEquals(0, simulated.gamePoints());
		assertEquals("the account is still identified", 4242L, simulated.accountHash());
		assertFalse("zeroing both point counts must not read as a stale dataset",
			simulated.datasetLooksStale());
	}

	@Test
	public void zeroCompletionLeavesTheLoggedOutStateAlone()
	{
		ProgressSnapshot absent = ProgressSnapshot.absent();
		assertSame(absent, DebugSimulation.of(Collections.emptyMap(), true).apply(absent));
	}

	@Test
	public void levelOfFallsBackToTheRealLevelForUnsimulatedSkills()
	{
		Map<String, Integer> onlyAttack = new HashMap<>();
		onlyAttack.put("Attack", 42);
		DebugSimulation sim = DebugSimulation.of(onlyAttack, false);

		assertEquals(42, sim.levelOf("Attack", 99));
		assertEquals("case-insensitive, matching how the profile keys are normalised",
			42, sim.levelOf("attack", 99));
		assertEquals("untouched skills report the real level", 99, sim.levelOf("Slayer", 99));
	}
}
