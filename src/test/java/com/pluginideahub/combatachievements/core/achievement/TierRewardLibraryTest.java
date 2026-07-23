package com.pluginideahub.combatachievements.core.achievement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TierRewardLibraryTest
{
	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void bundledRewardsCoverAllSixTiers()
	{
		TierRewardLibrary lib = TierRewardLibrary.loadBundled();
		assertEquals(6, lib.count());
		for (AchievementTier tier : AchievementTier.values())
		{
			TierReward reward = lib.forTier(tier);
			assertTrue("missing reward headline for " + tier, reward.isPresent());
			assertFalse("empty headline for " + tier, reward.headline().isEmpty());
		}
		// Spot-check the iconic progression.
		assertTrue(lib.forTier(AchievementTier.EASY).headline().toLowerCase().contains("ghommal"));
	}

	@Test
	public void lookupIsCaseInsensitiveAndParsesRewardList()
	{
		TierRewardLibrary lib = TierRewardLibrary.load(stream(
			"{\"tiers\":{\"Easy\":{\"headline\":\"Ghommal's hilt (1)\","
				+ "\"rewards\":[\"3 GWD teleports\",\"Antique lamp\"]}}}"));
		TierReward r = lib.forTier("easy");
		assertEquals("Ghommal's hilt (1)", r.headline());
		assertEquals(2, r.rewards().size());
		assertEquals("3 GWD teleports", r.rewards().get(0));
	}

	@Test
	public void missingTierReturnsNone()
	{
		TierRewardLibrary lib = TierRewardLibrary.load(stream("{\"tiers\":{\"Easy\":{\"headline\":\"x\"}}}"));
		assertSame(TierReward.NONE, lib.forTier("Grandmaster"));
		assertSame(TierReward.NONE, lib.forTier((AchievementTier) null));
	}

	@Test
	public void malformedFileYieldsEmptyLibrary()
	{
		TierRewardLibrary lib = TierRewardLibrary.load(stream("not json"));
		assertEquals(0, lib.count());
		assertSame(TierReward.NONE, lib.forTier(AchievementTier.MASTER));
	}
}
