package com.pluginideahub.combatachievements.core.ranking;

/**
 * Supplies {@link TaskLiveSignals} for a given task id. Implemented by the RuneLite bridge (reading
 * skills/items/KC); the pure ranker depends only on this interface so it stays client-free.
 */
@FunctionalInterface
public interface SignalsProvider
{
	TaskLiveSignals signalsFor(int taskId);

	/** A provider that returns {@link TaskLiveSignals#defaults()} for every task. */
	static SignalsProvider defaults()
	{
		return taskId -> TaskLiveSignals.defaults();
	}
}
