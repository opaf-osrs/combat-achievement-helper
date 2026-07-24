/**
 * Combat math for a planned gear/DPS feature — not wired into the panel yet.
 *
 * <p>Max hit and accuracy follow the wiki formulas (melee/ranged/magic), monster defence comes from
 * {@link com.pluginideahub.combatachievements.core.combat.MonsterStatsLibrary} (bundled
 * {@code monster_stats.json}), and {@link com.pluginideahub.combatachievements.core.combat.SpeedFeasibilityModel}
 * judges whether a speed task is realistic for a given DPS. The plan is to read the player's bank/gear
 * and use this to mark speed tasks doable or not for their actual setup, and to recommend upgrades.
 * The formulas are pinned by tests against known in-game values (whip max hits, barrage, etc.), so the
 * hard part stays correct while it waits.</p>
 */
package com.pluginideahub.combatachievements.core.combat;
