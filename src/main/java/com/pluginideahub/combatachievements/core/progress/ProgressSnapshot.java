package com.pluginideahub.combatachievements.core.progress;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable snapshot of an account's Combat Achievement progress, produced by the varbit reader on
 * the client thread and consumed by the pure core. Crossing the client→EDT boundary as an immutable
 * value means no shared mutable state. See docs/DESIGN.md §3.2/§5.2.
 *
 * <p>{@code gamePoints} (the client's own points varp) is the ground truth for what rewards are
 * unlocked; {@code computedPoints} (summed from the bundled dataset) is used for the per-task
 * breakdown and as a staleness signal when the two diverge.</p>
 */
public final class ProgressSnapshot
{
	private final Set<Integer> completedIds;
	private final int computedPoints;
	private final int gamePoints;
	private final AchievementTier currentTier;
	private final long accountHash;
	private final boolean present;
	private final boolean sample;

	public ProgressSnapshot(Set<Integer> completedIds, int computedPoints, int gamePoints,
		AchievementTier currentTier, long accountHash)
	{
		this(completedIds, computedPoints, gamePoints, currentTier, accountHash, false);
	}

	public ProgressSnapshot(Set<Integer> completedIds, int computedPoints, int gamePoints,
		AchievementTier currentTier, long accountHash, boolean sample)
	{
		this.completedIds = Collections.unmodifiableSet(new HashSet<>(
			completedIds == null ? Collections.emptySet() : completedIds));
		this.computedPoints = computedPoints;
		this.gamePoints = gamePoints;
		this.currentTier = currentTier;
		this.accountHash = accountHash;
		this.present = true;
		this.sample = sample;
	}

	private ProgressSnapshot()
	{
		this.completedIds = Collections.emptySet();
		this.computedPoints = 0;
		this.gamePoints = 0;
		this.currentTier = null;
		this.accountHash = -1L;
		this.present = false;
		this.sample = false;
	}

	/** True when this is placeholder/sample data, not a real account read. */
	public boolean isSample()
	{
		return sample;
	}

	/** The "no account synced yet" state (logged out / indeterminate account). */
	public static ProgressSnapshot absent()
	{
		return new ProgressSnapshot();
	}

	/** False for {@link #absent()}; true once a real account read has populated this snapshot. */
	public boolean isPresent()
	{
		return present;
	}

	public Set<Integer> completedIds()
	{
		return completedIds;
	}

	public boolean isCompleted(int taskId)
	{
		return completedIds.contains(taskId);
	}

	public int completedCount()
	{
		return completedIds.size();
	}

	public int computedPoints()
	{
		return computedPoints;
	}

	public int gamePoints()
	{
		return gamePoints;
	}

	/** True when the game's own points exceed what the bundled dataset can account for (stale bundle). */
	public boolean datasetLooksStale()
	{
		return gamePoints > computedPoints;
	}

	public AchievementTier currentTier()
	{
		return currentTier;
	}

	public long accountHash()
	{
		return accountHash;
	}
}
