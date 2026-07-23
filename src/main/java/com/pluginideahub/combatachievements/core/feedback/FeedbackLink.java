package com.pluginideahub.combatachievements.core.feedback;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the "Suggest fix" URL: a Google Form pre-filled with the task's curation context, opened in the
 * player's browser. Nothing is sent from the client and no account data is collected — the plugin only
 * hands the browser a link, exactly like the Wiki / guide buttons.
 *
 * <p><b>Setup.</b> Paste the form id and the per-field entry ids below; see docs/FEEDBACK-FORM.md for how
 * to read them off a Google Forms pre-fill link. Until {@link #FORM_ID} is filled in, {@link #isConfigured()}
 * is false and the panel simply hides the button — so a half-configured build never ships a broken link.
 * Every entry id is independently optional: wire up the task field first and add the rest later.</p>
 */
public final class FeedbackLink
{
	// ---------------------------------------------------------------------------------------------
	// Paste your Google Form details here. See docs/FEEDBACK-FORM.md.
	// ---------------------------------------------------------------------------------------------

	/** The long id in the form's URL: .../forms/d/e/<THIS>/viewform. Empty = feedback button hidden. */
	private static final String FORM_ID = "";

	/** entry.NNNNN for the "Task id" field (the sortable key — wire this one up first). */
	private static final String ENTRY_TASK_ID = "";
	/** entry.NNNNN for the "Task name" field. */
	private static final String ENTRY_TASK_NAME = "";
	/** entry.NNNNN for the "Listed difficulty" field (what the plugin currently shows, e.g. "4.5"). */
	private static final String ENTRY_LISTED_DIFFICULTY = "";
	/** entry.NNNNN for the "How it was derived" field (boss rating + task bump + reason). */
	private static final String ENTRY_BASIS = "";

	// ---------------------------------------------------------------------------------------------

	private static final String FORM_URL = "https://docs.google.com/forms/d/e/%s/viewform";

	private FeedbackLink()
	{
	}

	/** True once a form id is configured; the panel hides the button when false. */
	public static boolean isConfigured()
	{
		return !FORM_ID.isEmpty();
	}

	/**
	 * A pre-filled feedback URL for one task, or "" when no form is configured.
	 *
	 * @param taskId           the dataset task id (the key the curation CSV is sorted by)
	 * @param taskName         the task's display name
	 * @param listedDifficulty the rating the panel showed, e.g. "4.5"
	 * @param basis            how that rating was derived, e.g. "boss 4 +0.5 (no supplies)" — tells the
	 *                         curator whether to fix the boss row or just this task's bump
	 */
	public static String difficultyUrl(int taskId, String taskName, String listedDifficulty, String basis)
	{
		Map<String, String> prefill = new LinkedHashMap<>();
		put(prefill, ENTRY_TASK_ID, String.valueOf(taskId));
		put(prefill, ENTRY_TASK_NAME, taskName);
		put(prefill, ENTRY_LISTED_DIFFICULTY, listedDifficulty);
		put(prefill, ENTRY_BASIS, basis);
		return buildUrl(FORM_ID, prefill);
	}

	/**
	 * Assembles the pre-fill URL. Package-private seam so the construction (encoding, separators, the
	 * empty-form-id contract) is testable without depending on the hard-coded constants above.
	 *
	 * @return the URL, or "" when {@code formId} is absent
	 */
	static String buildUrl(String formId, Map<String, String> prefill)
	{
		if (formId == null || formId.isEmpty())
		{
			return "";
		}
		StringBuilder url = new StringBuilder(String.format(FORM_URL, formId));
		url.append("?usp=pp_url");
		for (Map.Entry<String, String> e : prefill.entrySet())
		{
			url.append('&').append(e.getKey()).append('=').append(encode(e.getValue()));
		}
		return url.toString();
	}

	/** Adds a pre-fill param only when both its entry id and its value are present. */
	private static void put(Map<String, String> params, String entryId, String value)
	{
		if (entryId != null && !entryId.isEmpty() && value != null && !value.isEmpty())
		{
			params.put(entryId, value);
		}
	}

	private static String encode(String value)
	{
		try
		{
			return URLEncoder.encode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException ex)
		{
			return ""; // UTF-8 is always available; drop the value rather than break the link
		}
	}
}
