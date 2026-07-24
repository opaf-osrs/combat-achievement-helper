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
	private static final String FORM_ID = "1FAIpQLSfcuYJVGfV7QKeoI72h_TTyTuTTESueSro5T5PTJJndB9Bw5g";

	/** entry.NNNNN for the "Task id" field — the only thing the player should not have to type. */
	private static final String ENTRY_TASK_ID = "entry.602421074";

	/**
	 * A second, general form for feedback about the plugin itself (rather than one task's rating), linked
	 * from the panel footer. Nothing is pre-filled: it is opened from the footer, not from a task, so there
	 * is no context to carry. Empty = the footer link is hidden.
	 */
	private static final String GENERAL_FORM_ID = "1FAIpQLSf_9QToV1ASjX6kETsMGdamM1zUvSPuC7K8B3bJDK-nkjWO7w";

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

	/** True once the general feedback form is configured; the footer hides its link when false. */
	public static boolean hasGeneralForm()
	{
		return !GENERAL_FORM_ID.isEmpty();
	}

	/** The general plugin-feedback form, or "" when none is configured. Carries no pre-filled data. */
	public static String generalUrl()
	{
		return buildUrl(GENERAL_FORM_ID, new LinkedHashMap<>());
	}

	/**
	 * A feedback URL for one task, or "" when no form is configured. Only the task id is carried: the
	 * player is already looking at the achievement, so the form should ask them for the two things the
	 * plugin cannot know — the difficulty they'd give it, and why — and nothing else. Everything else
	 * (name, listed rating, how that rating was derived) is recoverable from the id.
	 *
	 * @param taskId the dataset task id — the key the curation CSV is sorted by
	 */
	public static String taskUrl(int taskId)
	{
		Map<String, String> prefill = new LinkedHashMap<>();
		put(prefill, ENTRY_TASK_ID, String.valueOf(taskId));
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
