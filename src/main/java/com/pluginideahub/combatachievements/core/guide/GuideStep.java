package com.pluginideahub.combatachievements.core.guide;

/**
 * One authored step/tip within a {@link Guide}. A step is free-text advice; it may optionally
 * reference a specific task id so the UI can link straight to that task's wiki/video.
 */
public final class GuideStep
{
	private final String note;
	private final int taskId;

	public GuideStep(String note, int taskId)
	{
		this.note = note == null ? "" : note;
		this.taskId = taskId;
	}

	public String note()
	{
		return note;
	}

	/** The referenced task id, or a negative value when the step is general advice. */
	public int taskId()
	{
		return taskId;
	}

	public boolean hasTask()
	{
		return taskId >= 0;
	}
}
