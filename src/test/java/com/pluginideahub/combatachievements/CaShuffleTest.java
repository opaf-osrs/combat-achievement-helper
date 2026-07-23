package com.pluginideahub.combatachievements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Guards the CAs-list reshuffle jitter ({@link CombatAchievementsPanel#jitteredKey}). */
public class CaShuffleTest
{
	@Test
	public void jitterIsDeterministicAndBounded()
	{
		// Same (rank, id, seed) always yields the same key, so per-tick rebuilds don't re-scramble.
		assertEquals(CombatAchievementsPanel.jitteredKey(5, 42, 7L),
			CombatAchievementsPanel.jitteredKey(5, 42, 7L), 0.0);

		// A task never moves more than the jitter window (SHUFFLE_JITTER = 12) from its rank.
		for (int rank = 0; rank < 40; rank++)
		{
			double key = CombatAchievementsPanel.jitteredKey(rank, rank * 3 + 1, 9L);
			assertTrue("bounded jitter at rank " + rank, Math.abs(key - rank) <= 12.0 + 1e-9);
		}
	}

	@Test
	public void differentSeedsReshuffleButSameSeedIsStable()
	{
		List<Integer> underSeed1 = orderUnder(1L);
		assertNotEquals("a new seed reshuffles the list", underSeed1, orderUnder(2L));
		assertEquals("same seed is deterministic", underSeed1, orderUnder(1L));
	}

	private static List<Integer> orderUnder(long seed)
	{
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < 25; i++)
		{
			ids.add(i);
		}
		ids.sort(Comparator.comparingDouble(id -> CombatAchievementsPanel.jitteredKey(id, id, seed)));
		return ids;
	}
}
