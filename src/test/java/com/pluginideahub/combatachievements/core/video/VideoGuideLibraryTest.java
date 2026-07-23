package com.pluginideahub.combatachievements.core.video;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VideoGuideLibraryTest
{
	private static VideoGuideLibrary fixture()
	{
		String json = "{\"_comment\":\"ignored\","
			+ "\"1\":[\"https://youtu.be/aaa\"],"
			+ "\"2\":[\"https://youtu.be/bbb\",\"https://youtu.be/ccc\"]}";
		return VideoGuideLibrary.load(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void curatedLinksLoad()
	{
		VideoGuideLibrary lib = fixture();
		assertEquals(2, lib.curatedCount());
		assertTrue(lib.hasCuratedGuide(1));
		assertEquals(1, lib.guidesFor(1).size());
		assertEquals(2, lib.guidesFor(2).size());
		assertEquals("https://youtu.be/aaa", lib.bestGuideUrl(1, "Whatever"));
	}

	@Test
	public void missingTaskFallsBackToSearch()
	{
		VideoGuideLibrary lib = fixture();
		assertFalse(lib.hasCuratedGuide(99));
		assertTrue(lib.guidesFor(99).isEmpty());
		assertEquals(YouTubeSearch.urlFor("Noxious Foe"), lib.bestGuideUrl(99, "Noxious Foe"));
	}

	@Test
	public void nonNumericKeysAreIgnored()
	{
		VideoGuideLibrary lib = fixture();
		// "_comment" must not have been parsed as a guide entry
		assertEquals(2, lib.curatedCount());
	}
}
