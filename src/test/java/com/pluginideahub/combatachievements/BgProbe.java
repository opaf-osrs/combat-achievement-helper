package com.pluginideahub.combatachievements;

import net.runelite.client.ui.ColorScheme;

public final class BgProbe
{
	private BgProbe()
	{
	}

	public static void main(String[] args)
	{
		System.out.printf("DARK_GRAY   #%06X%n", ColorScheme.DARK_GRAY_COLOR.getRGB() & 0xFFFFFF);
		System.out.printf("DARKER_GRAY #%06X%n", ColorScheme.DARKER_GRAY_COLOR.getRGB() & 0xFFFFFF);
		System.out.printf("HOVER       #%06X%n", ColorScheme.DARK_GRAY_HOVER_COLOR.getRGB() & 0xFFFFFF);
	}
}
