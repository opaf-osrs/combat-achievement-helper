package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.ui.Palette;

/** Selectable side-panel colour themes (config dropdown). Each maps to an immutable {@link Palette}. */
public enum PanelTheme
{
	// Labels only — the constant NAMES are what config persists, so renaming those would reset every
	// user's chosen theme.
	CLASSIC("Classic", Palette.CLASSIC),
	MOLTEN_GAUNTLET("Vivid", Palette.MOLTEN_GAUNTLET),
	RUNELITE_CITIZEN("Slate", Palette.RUNELITE_CITIZEN),
	MODERN_DARK("Plain", Palette.MODERN_DARK),
	HIGH_CONTRAST("High contrast", Palette.HIGH_CONTRAST);

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
