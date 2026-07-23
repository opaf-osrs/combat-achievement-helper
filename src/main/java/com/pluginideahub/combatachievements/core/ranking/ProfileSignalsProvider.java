package com.pluginideahub.combatachievements.core.ranking;

import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;

/**
 * Account-aware {@link SignalsProvider}: derives each task's live signals from the player's
 * {@link PlayerProfile} and the curated effort data. Pure — the bridge supplies the profile read from
 * the client. Computes {@code levelsMet} (real skills vs the task's level requirements) and
 * {@code accessMet} (the task's quest gates vs the player's quest-completion state). Note that
 * {@code accessMet} verifies skill + quest gates only — area/diary unlocks (the free-form
 * {@code access} hint) are advisory and not blocked, so "doable now" never hides a task on an
 * unverifiable gate. Gear ownership and boss engagement are left permissive until those readers exist.
 */
public final class ProfileSignalsProvider implements SignalsProvider
{
	private final EffortDataLibrary effortLib;
	private final RecStatsLibrary recStatsLib;
	private final PlayerProfile profile;

	public ProfileSignalsProvider(EffortDataLibrary effortLib, PlayerProfile profile)
	{
		this(effortLib, RecStatsLibrary.empty(), profile);
	}

	public ProfileSignalsProvider(EffortDataLibrary effortLib, RecStatsLibrary recStatsLib,
		PlayerProfile profile)
	{
		this.effortLib = effortLib == null ? EffortDataLibrary.empty() : effortLib;
		this.recStatsLib = recStatsLib == null ? RecStatsLibrary.empty() : recStatsLib;
		this.profile = profile == null ? PlayerProfile.empty() : profile;
	}

	@Override
	public TaskLiveSignals signalsFor(int taskId)
	{
		TaskEffortData effort = effortLib.effortFor(taskId);

		// Level gate = curated levelReqs AND the HARD (required) recommended-stat gates. The latter backfills
		// stat gates the raw levelReqs miss (e.g. a "(required)" Slayer level not captured by reldo).
		boolean levelsMet = profile.meets(effort.levelReqs())
			&& profile.meetsAll(recStatsLib.hardFor(taskId));

		// Quest gates are machine-checkable against the player's live quest-completion state. Tasks
		// with no quest gate stay permissive (hasQuestAccess([]) == true), so nothing is hidden on an
		// unverifiable area/diary unlock.
		boolean accessMet = profile.hasQuestAccess(effort.questReqs());

		// SOFT recommended stats aren't a gate — the shortfall (levels below) sinks the task instead, so a
		// fresh account stops being told to do ToB Grandmaster while still being able to attempt it.
		int recStatsShortfall = profile.shortfall(recStatsLib.softFor(taskId));

		// TODO: gear ownership (ItemManager/equipment scan) and boss engagement (KC) once implemented.
		boolean gearOwned = false;
		boolean bossEngaged = false;

		return new TaskLiveSignals(accessMet, levelsMet, gearOwned, bossEngaged, recStatsShortfall);
	}
}
