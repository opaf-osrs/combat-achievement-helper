package com.pluginideahub.combatachievements.core.combat;

/**
 * Immutable melee attack setup (effective/boosted levels, prayer multipliers, style bonuses, gear
 * bonuses, void, on-task/salve target multiplier, weapon speed). Built via {@link Builder}; sensible
 * neutral defaults so tests/callers only set what matters.
 */
public final class MeleeAttack
{
	private final int strengthLevel;
	private final int attackLevel;
	private final double prayerStrMult;
	private final double prayerAtkMult;
	private final int styleStrBonus;
	private final int styleAtkBonus;
	private final int equipStrBonus;
	private final int equipAttackBonus;
	private final MeleeAttackType attackType;
	private final boolean voidSet;
	private final double targetMult;
	private final int weaponSpeedTicks;

	private MeleeAttack(Builder b)
	{
		this.strengthLevel = b.strengthLevel;
		this.attackLevel = b.attackLevel;
		this.prayerStrMult = b.prayerStrMult;
		this.prayerAtkMult = b.prayerAtkMult;
		this.styleStrBonus = b.styleStrBonus;
		this.styleAtkBonus = b.styleAtkBonus;
		this.equipStrBonus = b.equipStrBonus;
		this.equipAttackBonus = b.equipAttackBonus;
		this.attackType = b.attackType;
		this.voidSet = b.voidSet;
		this.targetMult = b.targetMult;
		this.weaponSpeedTicks = b.weaponSpeedTicks;
	}

	public int strengthLevel()
	{
		return strengthLevel;
	}

	public int attackLevel()
	{
		return attackLevel;
	}

	public double prayerStrMult()
	{
		return prayerStrMult;
	}

	public double prayerAtkMult()
	{
		return prayerAtkMult;
	}

	public int styleStrBonus()
	{
		return styleStrBonus;
	}

	public int styleAtkBonus()
	{
		return styleAtkBonus;
	}

	public int equipStrBonus()
	{
		return equipStrBonus;
	}

	public int equipAttackBonus()
	{
		return equipAttackBonus;
	}

	public MeleeAttackType attackType()
	{
		return attackType;
	}

	public boolean voidSet()
	{
		return voidSet;
	}

	public double targetMult()
	{
		return targetMult;
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
		private int strengthLevel = 1;
		private int attackLevel = 1;
		private double prayerStrMult = 1.0;
		private double prayerAtkMult = 1.0;
		private int styleStrBonus = 0;
		private int styleAtkBonus = 0;
		private int equipStrBonus = 0;
		private int equipAttackBonus = 0;
		private MeleeAttackType attackType = MeleeAttackType.SLASH;
		private boolean voidSet = false;
		private double targetMult = 1.0;
		private int weaponSpeedTicks = 4;

		public Builder strengthLevel(int v)
		{
			this.strengthLevel = v;
			return this;
		}

		public Builder attackLevel(int v)
		{
			this.attackLevel = v;
			return this;
		}

		public Builder prayer(double strMult, double atkMult)
		{
			this.prayerStrMult = strMult;
			this.prayerAtkMult = atkMult;
			return this;
		}

		public Builder styleBonus(int str, int atk)
		{
			this.styleStrBonus = str;
			this.styleAtkBonus = atk;
			return this;
		}

		public Builder equipStrBonus(int v)
		{
			this.equipStrBonus = v;
			return this;
		}

		public Builder equipAttackBonus(int v)
		{
			this.equipAttackBonus = v;
			return this;
		}

		public Builder attackType(MeleeAttackType v)
		{
			this.attackType = v;
			return this;
		}

		public Builder voidSet(boolean v)
		{
			this.voidSet = v;
			return this;
		}

		public Builder targetMult(double v)
		{
			this.targetMult = v;
			return this;
		}

		public Builder weaponSpeedTicks(int v)
		{
			this.weaponSpeedTicks = v;
			return this;
		}

		public MeleeAttack build()
		{
			return new MeleeAttack(this);
		}
	}
}
