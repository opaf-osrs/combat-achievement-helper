package com.pluginideahub.combatachievements.core.combat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DpsCalculatorTest
{
	// Max hit does not depend on the monster; use a dummy for those checks.
	private static final MonsterStats DUMMY = new MonsterStats("dummy", 100, 1, 1, 0, 0, 0, 0, 0);

	@Test
	public void meleeMaxHitMatchesKnownOsrsValues()
	{
		// 99 Strength, aggressive (+3), no prayer, no strength bonus → 11 (the well-known unarmed-ish max).
		MeleeAttack bare = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).build();
		assertEquals(11, DpsCalculator.melee(bare, DUMMY).maxHit());

		// Add an Abyssal whip (strength bonus 82) → 25.
		MeleeAttack whip = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).equipStrBonus(82).build();
		assertEquals(25, DpsCalculator.melee(whip, DUMMY).maxHit());

		// Whip + Piety (strength prayer ×1.23) → 30.
		MeleeAttack whipPiety = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).equipStrBonus(82).prayer(1.23, 1.20).build();
		assertEquals(30, DpsCalculator.melee(whipPiety, DUMMY).maxHit());
	}

	@Test
	public void rangedAndMagicMaxHits()
	{
		// 99 Ranged, accurate (+3), no ranged-strength bonus → 11.
		RangedAttack bareRange = RangedAttack.builder().rangedLevel(99).styleBonus(3).build();
		assertEquals(11, DpsCalculator.ranged(bareRange, DUMMY).maxHit());

		// + ranged strength bonus 80 → 25.
		RangedAttack strongRange = RangedAttack.builder().rangedLevel(99).styleBonus(3)
			.equipRangedStrBonus(80).build();
		assertEquals(25, DpsCalculator.ranged(strongRange, DUMMY).maxHit());

		// Ice Barrage base 30, no damage bonus → 30; with Occult (+10%) → 33.
		MagicAttack barrage = MagicAttack.builder().baseMaxHit(30).magicLevel(94)
			.equipMagicAttackBonus(120).build();
		assertEquals(30, DpsCalculator.magic(barrage, DUMMY).maxHit());
		MagicAttack occult = MagicAttack.builder().baseMaxHit(30).magicLevel(94)
			.equipMagicAttackBonus(120).magicDamageBonusPct(0.10).build();
		assertEquals(33, DpsCalculator.magic(occult, DUMMY).maxHit());
	}

	@Test
	public void hitChanceAndDpsAgainstADefendedMonster()
	{
		// Whip + Piety vs a defence-100 / slash-defence-50 monster.
		MeleeAttack whip = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).equipStrBonus(82).equipAttackBonus(82)
			.attackType(MeleeAttackType.SLASH).prayer(1.23, 1.20).weaponSpeedTicks(4).build();
		MonsterStats mon = new MonsterStats("m", 1000, 100, 1, 50, 50, 50, 50, 50);

		DpsResult r = DpsCalculator.melee(whip, mon);
		assertEquals(30, r.maxHit());
		assertEquals(0.6701, r.hitChance(), 0.01);   // exact per the accuracy formula
		assertTrue(r.dps() > 3.5 && r.dps() < 5.0);   // ~4.2 dps at 2.4s/attack
	}

	@Test
	public void higherDefenceLowersHitChanceAndDps()
	{
		MeleeAttack whip = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).equipStrBonus(82).equipAttackBonus(82).prayer(1.23, 1.20).build();
		MonsterStats soft = new MonsterStats("soft", 1000, 1, 1, 0, 0, 0, 0, 0);
		MonsterStats tanky = new MonsterStats("tanky", 1000, 300, 1, 250, 250, 250, 250, 250);

		assertTrue(DpsCalculator.melee(whip, soft).hitChance() > 0.95);
		assertTrue(DpsCalculator.melee(whip, soft).dps() > DpsCalculator.melee(whip, tanky).dps());
	}

	@Test
	public void bestDpsPicksTheStrongestStyle()
	{
		MonsterStats mon = new MonsterStats("m", 1000, 100, 1, 200, 200, 200, 5, 200); // weak to ranged
		MeleeAttack melee = MeleeAttack.builder().strengthLevel(99).attackLevel(99)
			.styleBonus(3, 3).equipStrBonus(82).equipAttackBonus(82).build();
		RangedAttack ranged = RangedAttack.builder().rangedLevel(99).styleBonus(3)
			.equipRangedStrBonus(80).equipRangedAttackBonus(120).weaponSpeedTicks(3).build();
		double best = DpsCalculator.bestDps(mon, melee, ranged, null);
		assertEquals(DpsCalculator.ranged(ranged, mon).dps(), best, 1e-9);
	}
}
