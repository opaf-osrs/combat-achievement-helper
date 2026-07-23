package com.pluginideahub.combatachievements;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CombatAchievementsPluginTestClient
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CombatAchievementsPlugin.class);
		RuneLite.main(args);
	}
}
