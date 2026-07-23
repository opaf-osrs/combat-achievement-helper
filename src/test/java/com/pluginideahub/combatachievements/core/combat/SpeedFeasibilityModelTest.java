package com.pluginideahub.combatachievements.core.combat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpeedFeasibilityModelTest
{
	@Test
	public void feasibleWhenDpsClearsComfortablyInsideThreshold()
	{
		// 500 HP at 20 DPS = 25s ttk, ×1.1 downtime = 27.5s, vs a 60s threshold → margin ~2.18.
		SpeedFeasibilityModel.Result r =
			SpeedFeasibilityModel.evaluate(500, 20.0, 60.0, 0.1, 0.0);
		assertEquals(SpeedFeasibilityModel.Verdict.FEASIBLE, r.verdict());
		assertEquals(27.5, r.estimatedClearSeconds(), 1e-9);
	}

	@Test
	public void tightWhenBarelyInside()
	{
		// 500 HP at 8 DPS = 62.5s, no downtime, vs 65s → margin 1.04 → TIGHT.
		SpeedFeasibilityModel.Result r =
			SpeedFeasibilityModel.evaluate(500, 8.0, 65.0, 0.0, 0.0);
		assertEquals(SpeedFeasibilityModel.Verdict.TIGHT, r.verdict());
	}

	@Test
	public void infeasibleWhenTooSlow()
	{
		SpeedFeasibilityModel.Result r =
			SpeedFeasibilityModel.evaluate(500, 5.0, 60.0, 0.0, 0.0);
		assertEquals(SpeedFeasibilityModel.Verdict.INFEASIBLE, r.verdict());
	}

	@Test
	public void referenceClearTimeOverridesHpDpsForMultiStage()
	{
		// Multi-stage: curated 50s reference clear vs 60s threshold → FEASIBLE, ignores hp/dps.
		SpeedFeasibilityModel.Result r =
			SpeedFeasibilityModel.evaluate(0, 0.0, 60.0, 0.0, 50.0);
		assertEquals(SpeedFeasibilityModel.Verdict.FEASIBLE, r.verdict());
		assertEquals(50.0, r.estimatedClearSeconds(), 1e-9);
	}

	@Test
	public void insufficientDataWhenNoStatsOrThreshold()
	{
		assertEquals(SpeedFeasibilityModel.Verdict.INSUFFICIENT_DATA,
			SpeedFeasibilityModel.evaluate(0, 0.0, 60.0, 0.0, 0.0).verdict());
		assertEquals(SpeedFeasibilityModel.Verdict.INSUFFICIENT_DATA,
			SpeedFeasibilityModel.evaluate(500, 20.0, 0.0, 0.0, 0.0).verdict());
	}
}
