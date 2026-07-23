package com.pluginideahub.combatachievements.core.guide;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GuideLibraryTest
{
	@Test
	public void bundledGuidesLoadWithFlagshipRoute()
	{
		GuideLibrary lib = GuideLibrary.loadBundled();
		assertTrue("expected at least the 0->Medium and GM guides", lib.count() >= 2);

		Guide zeroToMedium = lib.all().stream()
			.filter(g -> g.id().equals("zero-to-medium")).findFirst().orElse(null);
		assertEquals(AchievementTier.MEDIUM, zeroToMedium.targetTier());
		assertFalse(zeroToMedium.title().isEmpty());
		assertFalse(zeroToMedium.steps().isEmpty());
	}

	@Test
	public void parsesFieldsStepsAndTags()
	{
		String json = "{\"guides\":[{"
			+ "\"id\":\"g1\",\"title\":\"T\",\"author\":\"A\",\"summary\":\"S\","
			+ "\"videoUrl\":\"https://youtu.be/x\",\"targetTier\":\"Hard\","
			+ "\"tags\":[\"route\",\"hard\"],"
			+ "\"steps\":[{\"note\":\"do this\",\"taskId\":5},\"plain tip\"]}]}";
		GuideLibrary lib = GuideLibrary.load(stream(json));
		Guide g = lib.all().get(0);
		assertEquals("g1", g.id());
		assertTrue(g.hasVideo());
		assertEquals(AchievementTier.HARD, g.targetTier());
		assertEquals(2, g.tags().size());

		List<GuideStep> steps = g.steps();
		assertEquals(2, steps.size());
		assertTrue(steps.get(0).hasTask());
		assertEquals(5, steps.get(0).taskId());
		assertFalse(steps.get(1).hasTask());
		assertEquals("plain tip", steps.get(1).note());
	}

	@Test
	public void unknownTargetTierIsNullNotAnError()
	{
		GuideLibrary lib = GuideLibrary.load(stream(
			"{\"guides\":[{\"id\":\"g\",\"title\":\"T\",\"targetTier\":\"\"}]}"));
		assertNull(lib.all().get(0).targetTier());
	}

	@Test
	public void malformedFileYieldsEmptyLibrary()
	{
		assertEquals(0, GuideLibrary.load(stream("not json")).count());
		assertEquals(0, GuideLibrary.load(stream("{\"nope\":1}")).count());
	}

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
