package com.pluginideahub.combatachievements.core.combat;

/** Output of a DPS calculation for one combat style: max hit, per-attack hit chance, and DPS. */
public final class DpsResult
{
	public static final DpsResult ZERO = new DpsResult(0, 0.0, 0.0);

	private final int maxHit;
	private final double hitChance;
	private final double dps;

	public DpsResult(int maxHit, double hitChance, double dps)
	{
		this.maxHit = maxHit;
		this.hitChance = hitChance;
		this.dps = dps;
	}

	public int maxHit()
	{
		return maxHit;
	}

	public double hitChance()
	{
		return hitChance;
	}

	public double dps()
	{
		return dps;
	}
}
