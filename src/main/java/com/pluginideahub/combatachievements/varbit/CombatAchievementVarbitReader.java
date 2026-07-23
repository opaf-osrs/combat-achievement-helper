package com.pluginideahub.combatachievements.varbit;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.tier.TierMath;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;

/**
 * Reads the live account's Combat Achievement completion into a pure {@link ProgressSnapshot}. The
 * only place that touches {@link Client} for progress. Must run on the client thread (varbit/varp
 * reads are only valid there). See docs/DESIGN.md §5.2.
 *
 * <p>The game's own points varp is treated as ground truth for tier unlocks; the dataset-summed
 * {@code computedPoints} is kept for the per-task breakdown and as a staleness signal.</p>
 */
public final class CombatAchievementVarbitReader
{
	/** True once {@link CaVarbitIds} holds confirmed IDs; gates whether the reader can be trusted. */
	public boolean isVerified()
	{
		return CaVarbitIds.VERIFIED;
	}

	/** Full read of the logged-in account. Caller guarantees the client thread. */
	public ProgressSnapshot read(Client client, CombatAchievementLibrary lib)
	{
		if (client == null || lib == null)
		{
			return ProgressSnapshot.absent();
		}

		Map<Integer, Integer> pointsById = lib.pointsById();
		Set<Integer> completed = new HashSet<>();
		for (CombatAchievement task : lib.all())
		{
			if (CaVarbitIds.isComplete(client, task.id()))
			{
				completed.add(task.id());
			}
		}

		int computedPoints = 0;
		for (Integer id : completed)
		{
			computedPoints += pointsById.getOrDefault(id, 0);
		}

		int gamePoints = CaVarbitIds.readTotalPoints(client);
		if (gamePoints <= 0)
		{
			// No dedicated points varp in this scheme — the (now real) completed set is authoritative.
			gamePoints = computedPoints;
		}
		long accountHash = client.getAccountHash();

		return new ProgressSnapshot(
			completed,
			computedPoints,
			gamePoints,
			TierMath.currentTierFor(gamePoints, lib.all()),
			accountHash);
	}
}
