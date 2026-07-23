package com.pluginideahub.combatachievements.core.combat;

/**
 * Immutable magic attack setup. Base max hit is spell-defined (e.g. Ice Barrage 30); magic-damage %
 * and elemental weakness add on top. Built via {@link Builder}. See docs/SYSTEMS-DESIGN.md §5.1.
 */
public final class MagicAttack
{
	private final int baseMaxHit;
	private final double magicDamageBonusPct;
	private final double elementalWeaknessPct;
	private final int magicLevel;
	private final double prayerMult;
	private final int styleBonus;
	private final int equipMagicAttackBonus;
	private final boolean voidSet;
	private final double targetMult;
	private final int weaponSpeedTicks;

	private MagicAttack(Builder b)
	{
		this.baseMaxHit = b.baseMaxHit;
		this.magicDamageBonusPct = b.magicDamageBonusPct;
		this.elementalWeaknessPct = b.elementalWeaknessPct;
		this.magicLevel = b.magicLevel;
		this.prayerMult = b.prayerMult;
		this.styleBonus = b.styleBonus;
		this.equipMagicAttackBonus = b.equipMagicAttackBonus;
		this.voidSet = b.voidSet;
		this.targetMult = b.targetMult;
		this.weaponSpeedTicks = b.weaponSpeedTicks;
	}

	public int baseMaxHit()
	{
		return baseMaxHit;
	}

	public double magicDamageBonusPct()
	{
		return magicDamageBonusPct;
	}

	public double elementalWeaknessPct()
	{
		return elementalWeaknessPct;
	}

	public int magicLevel()
	{
		return magicLevel;
	}

	public double prayerMult()
	{
		return prayerMult;
	}

	public int styleBonus()
	{
		return styleBonus;
	}

	public int equipMagicAttackBonus()
	{
		return equipMagicAttackBonus;
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
		private int baseMaxHit = 0;
		private double magicDamageBonusPct = 0.0;
		private double elementalWeaknessPct = 0.0;
		private int magicLevel = 1;
		private double prayerMult = 1.0;
		private int styleBonus = 0;
		private int equipMagicAttackBonus = 0;
		private boolean voidSet = false;
		private double targetMult = 1.0;
		private int weaponSpeedTicks = 5;

		public Builder baseMaxHit(int v)
		{
			this.baseMaxHit = v;
			return this;
		}

		public Builder magicDamageBonusPct(double v)
		{
			this.magicDamageBonusPct = v;
			return this;
		}

		public Builder elementalWeaknessPct(double v)
		{
			this.elementalWeaknessPct = v;
			return this;
		}

		public Builder magicLevel(int v)
		{
			this.magicLevel = v;
			return this;
		}

		public Builder prayerMult(double v)
		{
			this.prayerMult = v;
			return this;
		}

		public Builder styleBonus(int v)
		{
			this.styleBonus = v;
			return this;
		}

		public Builder equipMagicAttackBonus(int v)
		{
			this.equipMagicAttackBonus = v;
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

		public MagicAttack build()
		{
			return new MagicAttack(this);
		}
	}
}
