package com.pluginideahub.combatachievements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
