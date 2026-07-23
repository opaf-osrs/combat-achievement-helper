package com.pluginideahub.combatachievements.core.combat;

/**
 * Pure OSRS DPS calculator for the three combat styles. Implements the standard max-hit / accuracy
 * (attack roll vs defence roll) / DPS formulas verified against the OSRS Wiki (see
 * docs/SYSTEMS-DESIGN.md §5.1). No RuneLite types — the bridge supplies the equipment-derived inputs.
 *
 * <p>Average damage per attack uses the discrete-uniform mean {@code maxHit/2}; DPS =
 * {@code hitChance * maxHit/2 / (weaponTicks * 0.6s)}. Single-target; special attacks and phased
 * mechanics are out of scope here (see the feasibility model for multi-stage handling).</p>
 */
public final class DpsCalculator
{
	private static final double TICK_SECONDS = 0.6;

	private DpsCalculator()
	{
	}

	public static DpsResult melee(MeleeAttack a, MonsterStats m)
	{
		int effStr = (int) Math.floor(Math.floor(a.strengthLevel() * a.prayerStrMult()) + a.styleStrBonus() + 8);
		if (a.voidSet())
		{
			effStr = (int) Math.floor(effStr * 1.1);
		}
		int maxHit = (int) Math.floor(0.5 + effStr * (a.equipStrBonus() + 64) / 640.0);
		maxHit = (int) Math.floor(maxHit * a.targetMult());

		int effAtk = (int) Math.floor(Math.floor(a.attackLevel() * a.prayerAtkMult()) + a.styleAtkBonus() + 8);
		if (a.voidSet())
		{
			effAtk = (int) Math.floor(effAtk * 1.1);
		}
		long atkRoll = (long) Math.floor((double) effAtk * (a.equipAttackBonus() + 64) * a.targetMult());
		long defRoll = (long) (m.defenceLevel() + 9) * (m.meleeDefenceBonus(a.attackType()) + 64);

		return finish(maxHit, atkRoll, defRoll, a.weaponSpeedTicks());
	}

	public static DpsResult ranged(RangedAttack a, MonsterStats m)
	{
		int effStr = (int) Math.floor(Math.floor(a.rangedLevel() * a.prayerStrMult()) + a.styleBonus() + 8);
		if (a.voidSet())
		{
			effStr = (int) Math.floor(effStr * a.voidStrMult());
		}
		int maxHit = (int) Math.floor(Math.floor(0.5 + effStr * (a.equipRangedStrBonus() + 64) / 640.0) * a.gearBonus());

		int effAtk = (int) Math.floor(Math.floor(a.rangedLevel() * a.prayerAtkMult()) + a.styleBonus() + 8);
		if (a.voidSet())
		{
			effAtk = (int) Math.floor(effAtk * 1.1);
		}
		long atkRoll = (long) Math.floor((double) effAtk * (a.equipRangedAttackBonus() + 64) * a.gearBonus());
		long defRoll = (long) (m.defenceLevel() + 9) * (m.defRange() + 64);

		return finish(maxHit, atkRoll, defRoll, a.weaponSpeedTicks());
	}

	public static DpsResult magic(MagicAttack a, MonsterStats m)
	{
		int maxHit = (int) Math.floor(a.baseMaxHit() * (1.0 + a.magicDamageBonusPct()));
		maxHit += (int) Math.floor(a.baseMaxHit() * a.elementalWeaknessPct());

		int base = (int) Math.floor(a.magicLevel() * a.prayerMult());
		if (a.voidSet())
		{
			base = (int) Math.floor(base * 1.45);
		}
		int effMagic = base + a.styleBonus() + 9;
		long atkRoll = (long) Math.floor((double) effMagic * (a.equipMagicAttackBonus() + 64) * a.targetMult());
		long defRoll = (long) (9 + m.magicLevel()) * (m.defMagic() + 64);

		return finish(maxHit, atkRoll, defRoll, a.weaponSpeedTicks());
	}

	/** Best (highest) DPS across whichever styles are supplied; null inputs are skipped. */
	public static double bestDps(MonsterStats m, MeleeAttack melee, RangedAttack ranged, MagicAttack magic)
	{
		double best = 0.0;
		if (melee != null)
		{
			best = Math.max(best, melee(melee, m).dps());
		}
		if (ranged != null)
		{
			best = Math.max(best, ranged(ranged, m).dps());
		}
		if (magic != null)
		{
			best = Math.max(best, magic(magic, m).dps());
		}
		return best;
	}

	private static DpsResult finish(int maxHit, long atkRoll, long defRoll, int weaponSpeedTicks)
	{
		double hitChance;
		if (atkRoll > defRoll)
		{
			hitChance = 1.0 - (defRoll + 2.0) / (2.0 * (atkRoll + 1.0));
		}
		else
		{
			double base = atkRoll / (2.0 * (defRoll + 1.0));
			hitChance = base * base;
		}
		double avgDamage = hitChance * (maxHit / 2.0);
		double speedSeconds = Math.max(1, weaponSpeedTicks) * TICK_SECONDS;
		return new DpsResult(maxHit, hitChance, avgDamage / speedSeconds);
	}
}
