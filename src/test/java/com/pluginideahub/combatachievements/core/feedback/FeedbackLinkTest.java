package com.pluginideahub.combatachievements.core.feedback;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the feedback link's two contracts: it stays SILENT until a form is configured (so a build with
 * no form never shows a dead "Suggest fix" button), and once configured it produces a properly-encoded
 * pre-fill URL. The URL construction is exercised through the {@code buildUrl} seam so these assertions
 * hold regardless of whether the real form constants have been pasted in yet.
 */
public class FeedbackLinkTest
{
	private static Map<String, String> prefill()
	{
		Map<String, String> m = new LinkedHashMap<>();
		m.put("entry.111", "42");
		m.put("entry.222", "A Smashing Time");
		m.put("entry.333", "4.5");
		m.put("entry.444", "Gargoyle (boss 4) + no supplies (+0.5)");
		return m;
	}

	@Test
	public void noFormIdMeansNoLinkSoThePanelHidesTheButton()
	{
		assertEquals("", FeedbackLink.buildUrl("", prefill()));
		assertEquals("", FeedbackLink.buildUrl(null, prefill()));
		// The shipped default is unconfigured, so the button is hidden out of the box.
		if (!FeedbackLink.isConfigured())
		{
			assertEquals("", FeedbackLink.difficultyUrl(42, "A Smashing Time", "4.5", "Gargoyle (boss 4)"));
		}
	}

	@Test
	public void buildsAWellFormedPrefillUrl()
	{
		String url = FeedbackLink.buildUrl("FORM123", prefill());

		assertTrue(url.startsWith("https://docs.google.com/forms/d/e/FORM123/viewform?usp=pp_url&"));
		assertEquals("exactly one query separator", url.indexOf('?'), url.lastIndexOf('?'));
		assertFalse("spaces must be encoded, never raw", url.contains(" "));
		assertTrue("task id is carried as the sortable key", url.contains("entry.111=42"));
		assertTrue("spaces encode as +", url.contains("entry.222=A+Smashing+Time"));
		// Parens/plus in the basis must survive encoding without breaking the query string.
		assertTrue("basis is encoded", url.contains("entry.444=Gargoyle+%28boss+4%29+%2B+no+supplies+%28%2B0.5%29"));
	}

	@Test
	public void omitsFieldsThatHaveNoEntryIdOrNoValue()
	{
		// difficultyUrl only adds a param when BOTH the entry id and the value are present, so a
		// partially-configured form (task field wired, rest blank) still yields a valid link.
		Map<String, String> sparse = new LinkedHashMap<>();
		sparse.put("entry.111", "42");
		String url = FeedbackLink.buildUrl("FORM123", sparse);

		assertTrue(url.contains("entry.111=42"));
		assertFalse(url.contains("entry.222"));
		assertEquals("https://docs.google.com/forms/d/e/FORM123/viewform?usp=pp_url&entry.111=42", url);
	}
}
