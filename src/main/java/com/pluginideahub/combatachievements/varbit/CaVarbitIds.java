package com.pluginideahub.combatachievements.varbit;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Reads Combat Achievement task completion from the live client.
 *
 * <p>Completion is stored as a <b>packed bitfield across 20 VarPlayers</b>
 * ({@code CA_TASK_COMPLETED_0 .. _19}, 640 bits — enough for the 637 tasks): task {@code id}'s
 * completion is bit {@code id % 32} of varp {@code id / 32}. The task {@code id} here is the in-game
 * CA struct's id param (1306), which we validated equals our dataset id 0..636 (see
 * {@code data/struct-id-bridge.json}).</p>
 *
 * <p>The varps are RuneLite {@code gameval} constants, so they track game updates rather than
 * hard-coded raw ids. Mechanism confirmed against the open-source {@code combat-achievements-tracker}
 * plugin (BSD-2-Clause) and the RuneLite API; see docs/DESIGN.md §5.1.1.</p>
 */
public final class CaVarbitIds
{
	private CaVarbitIds()
	{
	}

	/** The completion-reading mechanism is confirmed and live. */
	public static final boolean VERIFIED = true;

	private static final int[] COMPLETION_VARPS = {
		VarPlayerID.CA_TASK_COMPLETED_0, VarPlayerID.CA_TASK_COMPLETED_1,
		VarPlayerID.CA_TASK_COMPLETED_2, VarPlayerID.CA_TASK_COMPLETED_3,
		VarPlayerID.CA_TASK_COMPLETED_4, VarPlayerID.CA_TASK_COMPLETED_5,
		VarPlayerID.CA_TASK_COMPLETED_6, VarPlayerID.CA_TASK_COMPLETED_7,
		VarPlayerID.CA_TASK_COMPLETED_8, VarPlayerID.CA_TASK_COMPLETED_9,
		VarPlayerID.CA_TASK_COMPLETED_10, VarPlayerID.CA_TASK_COMPLETED_11,
		VarPlayerID.CA_TASK_COMPLETED_12, VarPlayerID.CA_TASK_COMPLETED_13,
		VarPlayerID.CA_TASK_COMPLETED_14, VarPlayerID.CA_TASK_COMPLETED_15,
		VarPlayerID.CA_TASK_COMPLETED_16, VarPlayerID.CA_TASK_COMPLETED_17,
		VarPlayerID.CA_TASK_COMPLETED_18, VarPlayerID.CA_TASK_COMPLETED_19,
	};

	/** True when task {@code taskId} is complete on the logged-in account. Call on the client thread. */
	public static boolean isComplete(Client client, int taskId)
	{
		if (client == null || taskId < 0 || taskId >= COMPLETION_VARPS.length * 32)
		{
			return false;
		}
		int varpValue = client.getVarpValue(COMPLETION_VARPS[taskId / 32]);
		return (varpValue & (1 << (taskId % 32))) != 0;
	}

	/** The number of completion varps backing the bitfield (used to bound id ranges / sanity checks). */
	public static int varpCount()
	{
		return COMPLETION_VARPS.length;
	}

	/**
	 * True when {@code varpId} is one of the 20 CA-completion varps. Lets a VarbitChanged handler ignore
	 * the flood of unrelated varp changes (which is most of them, especially on login) and only refresh
	 * when a completion bit could have changed.
	 */
	public static boolean isCompletionVarp(int varpId)
	{
		for (int v : COMPLETION_VARPS)
		{
			if (v == varpId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Total CA points: there is no separate points varp in this scheme — points are summed from the
	 * (now real) completed task set by the reader. Returns 0 so the reader falls back to that sum.
	 */
	public static int readTotalPoints(Client client)
	{
		return 0;
	}
}
