package com.pluginideahub.combatachievements.core.combat;

/**
 * The subset of an OSRS monster's combat stats the DPS calculator needs: hit points (for time-to-kill)
 * plus the defensive values a player's attack rolls against. Pure value object — populated from the
 * wiki's {@code infobox_monster} data carried in the bundled metadata. See docs/SYSTEMS-DESIGN.md §5.
 */
public final class MonsterStats
{
	public static final MonsterStats UNKNOWN = new MonsterStats("", 0, 1, 1, 0, 0, 0, 0, 0);

	private final String name;
	private final int hitpoints;
	private final int defenceLevel;
	private final int magicLevel;
	private final int defStab;
	private final int defSlash;
	private final int defCrush;
	private final int defRange;
	private final int defMagic;

	public MonsterStats(String name, int hitpoints, int defenceLevel, int magicLevel,
		int defStab, int defSlash, int defCrush, int defRange, int defMagic)
	{
		this.name = name == null ? "" : name;
		this.hitpoints = hitpoints;
		this.defenceLevel = defenceLevel;
		this.magicLevel = magicLevel;
		this.defStab = defStab;
		this.defSlash = defSlash;
		this.defCrush = defCrush;
		this.defRange = defRange;
		this.defMagic = defMagic;
	}

	public String name()
	{
		return name;
	}

	public int hitpoints()
	{
		return hitpoints;
	}

	public int defenceLevel()
	{
		return defenceLevel;
	}

	public int magicLevel()
	{
		return magicLevel;
	}

	public int defStab()
	{
		return defStab;
	}

	public int defSlash()
	{
		return defSlash;
	}

	public int defCrush()
	{
		return defCrush;
	}

	public int defRange()
	{
		return defRange;
	}

	public int defMagic()
	{
		return defMagic;
	}

	public boolean isKnown()
	{
		return hitpoints > 0;
	}

	/** The defensive bonus a melee attack of the given type rolls against. */
	public int meleeDefenceBonus(MeleeAttackType type)
	{
		switch (type)
		{
			case STAB: return defStab;
			case SLASH: return defSlash;
			case CRUSH: return defCrush;
			default: return defSlash;
		}
	}
}
