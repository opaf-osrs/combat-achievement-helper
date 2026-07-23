package com.pluginideahub.combatachievements.core.video;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YouTubeSearchTest
{
	@Test
	public void spacesBecomePlus()
	{
		assertEquals(
			"https://www.youtube.com/results?search_query=OSRS+Peach+Conjurer+combat+achievement+guide",
			YouTubeSearch.urlFor("Peach Conjurer"));
	}

	@Test
	public void apostropheIsPercentEncoded()
	{
		assertEquals(
			"https://www.youtube.com/results?search_query=OSRS+You%27re+a+wizard+combat+achievement+guide",
			YouTubeSearch.urlFor("You're a wizard"));
	}

	@Test
	public void emptyNameStillProducesValidQuery()
	{
		assertEquals(
			"https://www.youtube.com/results?search_query=OSRS++combat+achievement+guide",
			YouTubeSearch.urlFor(""));
	}
}
