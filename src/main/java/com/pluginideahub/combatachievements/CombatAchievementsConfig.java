package com.pluginideahub.combatachievements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("pluginideahub-combatachievements")
public interface CombatAchievementsConfig extends Config
{
	@ConfigItem(
		keyName = "panelTheme",
		name = "Panel theme",
		description = "The panel's colour scheme.",
		position = 0
	)
	default PanelTheme panelTheme()
	{
		return PanelTheme.CLASSIC;
	}

	@ConfigItem(
		keyName = "showHowTo",
		name = "Show setup & strategy",
		description = "Show recommended gear, stats and strategy on each task by default.",
		position = 1
	)
	default boolean showHowTo()
	{
		return false;
	}

	// ===================== CAs ranking =====================
	@ConfigSection(
		name = "CAs ranking",
		description = "Tune what the CAs list puts first.",
		position = 10,
		closedByDefault = true
	)
	String CAS_SECTION = "casSection";

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caPointsWeight",
		name = "Points weight",
		description = "Higher = prefer tasks worth more points. Lower = prefer the easiest tasks.",
		section = CAS_SECTION,
		position = 1
	)
	default int caPointsWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caDifficultyWeight",
		name = "Difficulty weight",
		description = "Higher = push harder tasks further down the list. Lower = care less about difficulty.",
		section = CAS_SECTION,
		position = 2
	)
	default int caDifficultyWeight()
	{
		return 100;
	}

	// ===== CAs effort penalties — currently inert (need per-CA effort data), collapsed by default =====
	@ConfigSection(
		name = "CAs penalties (inactive)",
		description = "These barely do anything yet — each task needs extra data first. Tucked away for now.",
		position = 15,
		closedByDefault = true
	)
	String CA_PENALTIES_SECTION = "caPenaltiesSection";

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "effortSensitivity",
		name = "Effort sensitivity",
		description = "Overall strength of the penalties below.",
		section = CA_PENALTIES_SECTION,
		position = 1
	)
	default int effortSensitivity()
	{
		return 50;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caGearWeight",
		name = "Gear penalty",
		description = "Higher = avoid tasks you don't have the gear for.",
		section = CA_PENALTIES_SECTION,
		position = 2
	)
	default int caGearWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caRngWeight",
		name = "RNG penalty",
		description = "Higher = avoid tasks that rely on luck.",
		section = CA_PENALTIES_SECTION,
		position = 3
	)
	default int caRngWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caSupplyWeight",
		name = "Supply penalty",
		description = "Higher = avoid tasks that use lots of supplies.",
		section = CA_PENALTIES_SECTION,
		position = 4
	)
	default int caSupplyWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caGroupWeight",
		name = "Group/raid penalty",
		description = "Higher = avoid tasks that need a team.",
		section = CA_PENALTIES_SECTION,
		position = 5
	)
	default int caGroupWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "caLearningWeight",
		name = "New-boss penalty",
		description = "Higher = avoid bosses you've never killed.",
		section = CA_PENALTIES_SECTION,
		position = 6
	)
	default int caLearningWeight()
	{
		return 100;
	}

	// ===================== Bosses ranking =====================
	@ConfigSection(
		name = "Bosses ranking",
		description = "Tune what the Bosses list puts first.",
		position = 20,
		closedByDefault = true
	)
	String BOSSES_SECTION = "bossesSection";

	@Range(min = 0, max = 30)
	@ConfigItem(
		keyName = "tripOverheadMinutes",
		name = "Session clustering",
		description = "Higher = prefer bosses where you can knock out several tasks in one trip.",
		section = BOSSES_SECTION,
		position = 1
	)
	default int tripOverheadMinutes()
	{
		return 6;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "bossTimeWeight",
		name = "Time vs points",
		description = "Higher = care more about speed. Lower = care more about total points.",
		section = BOSSES_SECTION,
		position = 2
	)
	default int bossTimeWeight()
	{
		return 100;
	}

	// ===================== Route ranking =====================
	@ConfigSection(
		name = "Route ranking",
		description = "Tune the fastest route to your next tier.",
		position = 30,
		closedByDefault = true
	)
	String ROUTE_SECTION = "routeSection";

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "routePathPointsWeight",
		name = "Path: points weight",
		description = "Higher = prefer bigger tasks so you reach the tier in fewer steps.",
		section = ROUTE_SECTION,
		position = 1
	)
	default int routePathPointsWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "routePathDifficultyWeight",
		name = "Path: difficulty weight",
		description = "Higher = avoid harder tasks even when they're quick.",
		section = ROUTE_SECTION,
		position = 2
	)
	default int routePathDifficultyWeight()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "routeUnlockBias",
		name = "Unlock bias",
		description = "Higher = prefer quests that unlock lots of tasks. Lower = prefer quick quests.",
		section = ROUTE_SECTION,
		position = 3
	)
	default int routeUnlockBias()
	{
		return 100;
	}

	@Range(min = 0, max = 200)
	@ConfigItem(
		keyName = "routeUnlockDifficultyWeight",
		name = "Unlock difficulty weight",
		description = "Higher = value a quest less when the tasks it unlocks are hard.",
		section = ROUTE_SECTION,
		position = 4
	)
	default int routeUnlockDifficultyWeight()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "developerMode",
		name = "Developer mode",
		description = "Extra debug info for development. Leave this off.",
		position = 40
	)
	default boolean developerMode()
	{
		return false;
	}
}
