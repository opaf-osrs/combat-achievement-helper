package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.ui.Palette;

/** Selectable side-panel colour themes (config dropdown). Each maps to an immutable {@link Palette}. */
public enum PanelTheme
{
	CLASSIC("Classic", Palette.CLASSIC),
	MOLTEN_GAUNTLET("Molten Gauntlet (refined)", Palette.MOLTEN_GAUNTLET),
	RUNELITE_CITIZEN("RuneLite Citizen (native)", Palette.RUNELITE_CITIZEN),
	MODERN_DARK("Modern Dark (minimal)", Palette.MODERN_DARK),
	OSRS_WARM("OSRS Warm (authentic)", Palette.OSRS_WARM),
	HIGH_CONTRAST("High Contrast (accessible)", Palette.HIGH_CONTRAST);

	private final String label;
	private final Palette palette;

	PanelTheme(String label, Palette palette)
	{
		this.label = label;
		this.palette = palette;
	}

	public Palette palette()
	{
		return palette;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
