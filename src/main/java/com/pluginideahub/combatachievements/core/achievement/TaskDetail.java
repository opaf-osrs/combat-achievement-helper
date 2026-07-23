package com.pluginideahub.combatachievements.core.achievement;

/**
 * Curated "how to do it" text for a CA — recommended stats, gear setup, strategy and required items —
 * surfaced in the CA-detail view. Immutable value; curated in {@code data/curation/tasks_master.csv}
 * and bundled as {@code task_detail.json}. Any field may be empty.
 */
public final class TaskDetail
{
	public static final TaskDetail EMPTY = new TaskDetail("", "", "", "");

	private final String stats;
	private final String setup;
	private final String strategy;
	private final String items;

	public TaskDetail(String stats, String setup, String strategy, String items)
	{
		this.stats = stats == null ? "" : stats;
		this.setup = setup == null ? "" : setup;
		this.strategy = strategy == null ? "" : strategy;
		this.items = items == null ? "" : items;
	}

	/** Recommended stats, e.g. "80+ Ranged, 43+ Prayer, 75 Magic". */
	public String stats()
	{
		return stats;
	}

	/** Recommended gear/inventory setup. */
	public String setup()
	{
		return setup;
	}

	/** Short strategy note. */
	public String strategy()
	{
		return strategy;
	}

	/** Required/recommended items, semicolon-separated. */
	public String items()
	{
		return items;
	}

	public boolean isEmpty()
	{
		return stats.isEmpty() && setup.isEmpty() && strategy.isEmpty() && items.isEmpty();
	}
}
