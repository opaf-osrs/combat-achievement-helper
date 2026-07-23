package com.pluginideahub.combatachievements.ui;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import java.awt.Color;

/**
 * The panel's ACTIVE colours. These are read (by name) across the Swing panel and the pop-out window; the
 * plugin swaps the whole set at runtime via {@link #apply(Palette)} when the user picks a theme in config.
 * Backgrounds stay on RuneLite's ColorScheme greys — only these text/accent roles are themed. Mutated only
 * on the Swing EDT, so plain (non-volatile) statics are fine for this single-threaded UI.
 */
public final class CombatAchievementsTheme
{
	private CombatAchievementsTheme()
	{
	}

	public static Color POSITIVE;
	public static Color LOCKED;
	public static Color ACCENT;

	/** Brand identity (shared with the in-game window): names, points, headers, descriptions. */
	public static Color NAME;
	public static Color POINTS;
	public static Color HEADER_GOLD;
	public static Color DESC;

	/** Unmet / hard / locked text (was the panel's HARD_RED). */
	public static Color NEGATIVE;
	/** Muted "·" separators and secondary meta. */
	public static Color NEUTRAL_META;

	/** Mode-bar selected fill + its text colour (kept legible per-palette). */
	public static Color MODE_SELECTED;
	public static Color MODE_SELECTED_TEXT;

	/** Difficulty ramp — its own colours so a "diff N" never reads as the points or a header. */
	public static Color DIFF_EASY;
	public static Color DIFF_MID;
	public static Color DIFF_HARD;

	public static Color TIER_EASY;
	public static Color TIER_MEDIUM;
	public static Color TIER_HARD;
	public static Color TIER_ELITE;
	public static Color TIER_MASTER;
	public static Color TIER_GRANDMASTER;

	static
	{
		apply(Palette.CLASSIC);
	}

	/** Swaps the whole active colour set. Call on the EDT, then re-render. */
	public static void apply(Palette p)
	{
		NAME = p.name;
		POINTS = p.points;
		HEADER_GOLD = p.headerAccent;
		DESC = p.desc;
		POSITIVE = p.positive;
		NEGATIVE = p.negative;
		LOCKED = p.locked;
		NEUTRAL_META = p.neutralMeta;
		ACCENT = p.accent;
		MODE_SELECTED = p.modeSelected;
		MODE_SELECTED_TEXT = p.modeSelectedText;
		DIFF_EASY = p.diffEasy;
		DIFF_MID = p.diffMid;
		DIFF_HARD = p.diffHard;
		TIER_EASY = p.tierEasy;
		TIER_MEDIUM = p.tierMedium;
		TIER_HARD = p.tierHard;
		TIER_ELITE = p.tierElite;
		TIER_MASTER = p.tierMaster;
		TIER_GRANDMASTER = p.tierGrandmaster;
	}

	public static Color forTier(AchievementTier tier)
	{
		if (tier == null)
		{
			return LOCKED;
		}
		switch (tier)
		{
			case EASY: return TIER_EASY;
			case MEDIUM: return TIER_MEDIUM;
			case HARD: return TIER_HARD;
			case ELITE: return TIER_ELITE;
			case MASTER: return TIER_MASTER;
			case GRANDMASTER: return TIER_GRANDMASTER;
			default: return LOCKED;
		}
	}

	/** Hex string (e.g. {@code #4caf50}) for embedding a colour in a JLabel's HTML. */
	public static String hex(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}
}
