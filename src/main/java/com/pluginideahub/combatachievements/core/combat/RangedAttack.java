package com.pluginideahub.combatachievements.core.combat;

/** Immutable ranged attack setup. Built via {@link Builder}. See docs/SYSTEMS-DESIGN.md §5.1. */
public final class RangedAttack
{
	private final int rangedLevel;
	private final double prayerStrMult;
	private final double prayerAtkMult;
	private final int styleBonus;
	private final int equipRangedStrBonus;
	private final int equipRangedAttackBonus;
	private final boolean voidSet;
	private final double voidStrMult;
	private final double gearBonus;
	private final int weaponSpeedTicks;

	private RangedAttack(Builder b)
	{
		this.rangedLevel = b.rangedLevel;
		this.prayerStrMult = b.prayerStrMult;
		this.prayerAtkMult = b.prayerAtkMult;
		this.styleBonus = b.styleBonus;
		this.equipRangedStrBonus = b.equipRangedStrBonus;
		this.equipRangedAttackBonus = b.equipRangedAttackBonus;
		this.voidSet = b.voidSet;
		this.voidStrMult = b.voidStrMult;
		this.gearBonus = b.gearBonus;
		this.weaponSpeedTicks = b.weaponSpeedTicks;
	}

	public int rangedLevel()
	{
		return rangedLevel;
	}

	public double prayerStrMult()
	{
		return prayerStrMult;
	}

	public double prayerAtkMult()
	{
		return prayerAtkMult;
	}

	public int styleBonus()
	{
		return styleBonus;
	}

	public int equipRangedStrBonus()
	{
		return equipRangedStrBonus;
	}

	public int equipRangedAttackBonus()
	{
		return equipRangedAttackBonus;
	}

	public boolean voidSet()
	{
		return voidSet;
	}

	public double voidStrMult()
	{
		return voidStrMult;
	}

	public double gearBonus()
	{
		return gearBonus;
	}

	public int weaponSpeedTicks()
	{
		return weaponSpeedTicks;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private int rangedLevel = 1;
		private double prayerStrMult = 1.0;
		private double prayerAtkMult = 1.0;
		private int styleBonus = 0;
		private int equipRangedStrBonus = 0;
		private int equipRangedAttackBonus = 0;
		private boolean voidSet = false;
		private double voidStrMult = 1.1;
		private double gearBonus = 1.0;
		private int weaponSpeedTicks = 4;

		public Builder rangedLevel(int v)
		{
			this.rangedLevel = v;
			return this;
		}

		public Builder prayer(double strMult, double atkMult)
		{
			this.prayerStrMult = strMult;
			this.prayerAtkMult = atkMult;
			return this;
		}

		public Builder styleBonus(int v)
		{
			this.styleBonus = v;
			return this;
		}

		public Builder equipRangedStrBonus(int v)
		{
			this.equipRangedStrBonus = v;
			return this;
		}

		public Builder equipRangedAttackBonus(int v)
		{
			this.equipRangedAttackBonus = v;
			return this;
		}

		public Builder voidSet(boolean v, double strMult)
		{
			this.voidSet = v;
			this.voidStrMult = strMult;
			return this;
		}

		public Builder gearBonus(double v)
		{
			this.gearBonus = v;
			return this;
		}

		public Builder weaponSpeedTicks(int v)
		{
			this.weaponSpeedTicks = v;
			return this;
		}

		public RangedAttack build()
		{
			return new RangedAttack(this);
		}
	}
}
