package com.pluginideahub.combatachievements.ui;

import java.awt.Color;

/**
 * An immutable colour palette for the side panel. The user picks one in the config; the chosen palette is
 * pushed into {@link CombatAchievementsTheme} (which the panel and pop-out window read). The presets were
 * generated and adversarially judged for hierarchy, difficulty/points colour separation, and WCAG AA
 * contrast on the RuneLite dark card background (#282828); see docs. Backgrounds stay on RuneLite's
 * ColorScheme greys — only the text/accent roles vary between palettes.
 */
public final class Palette
{
	public final Color name;
	public final Color points;
	public final Color headerAccent;
	public final Color desc;
	public final Color positive;
	public final Color negative;
	public final Color locked;
	public final Color neutralMeta;
	public final Color accent;
	public final Color modeSelected;
	public final Color modeSelectedText;
	public final Color diffEasy;
	public final Color diffMid;
	public final Color diffHard;
	public final Color tierEasy;
	public final Color tierMedium;
	public final Color tierHard;
	public final Color tierElite;
	public final Color tierMaster;
	public final Color tierGrandmaster;

	private Palette(Builder b)
	{
		this.name = b.name;
		this.points = b.points;
		this.headerAccent = b.headerAccent;
		this.desc = b.desc;
		this.positive = b.positive;
		this.negative = b.negative;
		this.locked = b.locked;
		this.neutralMeta = b.neutralMeta;
		this.accent = b.accent;
		this.modeSelected = b.modeSelected;
		this.modeSelectedText = b.modeSelectedText;
		this.diffEasy = b.diffEasy;
		this.diffMid = b.diffMid;
		this.diffHard = b.diffHard;
		this.tierEasy = b.tierEasy;
		this.tierMedium = b.tierMedium;
		this.tierHard = b.tierHard;
		this.tierElite = b.tierElite;
		this.tierMaster = b.tierMaster;
		this.tierGrandmaster = b.tierGrandmaster;
	}

	private static Color c(String hex)
	{
		return Color.decode(hex);
	}

	private static Builder builder()
	{
		return new Builder();
	}

	private static final class Builder
	{
		private Color name, points, headerAccent, desc, positive, negative, locked, neutralMeta, accent,
			modeSelected, modeSelectedText, diffEasy, diffMid, diffHard,
			tierEasy, tierMedium, tierHard, tierElite, tierMaster, tierGrandmaster;

		Builder name(String h) { this.name = c(h); return this; }
		Builder points(String h) { this.points = c(h); return this; }
		Builder headerAccent(String h) { this.headerAccent = c(h); return this; }
		Builder desc(String h) { this.desc = c(h); return this; }
		Builder positive(String h) { this.positive = c(h); return this; }
		Builder negative(String h) { this.negative = c(h); return this; }
		Builder locked(String h) { this.locked = c(h); return this; }
		Builder neutralMeta(String h) { this.neutralMeta = c(h); return this; }
		Builder accent(String h) { this.accent = c(h); return this; }
		Builder modeSelected(String h) { this.modeSelected = c(h); return this; }
		Builder modeSelectedText(String h) { this.modeSelectedText = c(h); return this; }
		Builder diffEasy(String h) { this.diffEasy = c(h); return this; }
		Builder diffMid(String h) { this.diffMid = c(h); return this; }
		Builder diffHard(String h) { this.diffHard = c(h); return this; }
		Builder tierEasy(String h) { this.tierEasy = c(h); return this; }
		Builder tierMedium(String h) { this.tierMedium = c(h); return this; }
		Builder tierHard(String h) { this.tierHard = c(h); return this; }
		Builder tierElite(String h) { this.tierElite = c(h); return this; }
		Builder tierMaster(String h) { this.tierMaster = c(h); return this; }
		Builder tierGrandmaster(String h) { this.tierGrandmaster = c(h); return this; }

		Palette build() { return new Palette(this); }
	}

	/** Orange names, green points, gold headers. The panel's default look. */
	public static final Palette CLASSIC = builder()
		.name("#FF9D33").points("#7FC24A").headerAccent("#FFD98A").desc("#B6AE96")
		.positive("#4CAF50").negative("#D86A5A").locked("#969696").neutralMeta("#8A8A8A")
		.accent("#DC8A00").modeSelected("#DC8A00").modeSelectedText("#FFFFFF")
		.diffEasy("#4CAF50").diffMid("#FFD98A").diffHard("#D86A5A")
		.tierEasy("#78C878").tierMedium("#78B4DC").tierHard("#C8AA5A")
		.tierElite("#BE78C8").tierMaster("#DC786E").tierGrandmaster("#E6C85A").build();

	/**
	 * Loud on purpose: a magenta / green / cyan triad (OKLCH hues 330, 145, 210) at high chroma. Because
	 * every hue sits in the same lightness and chroma band, a saturated scheme still reads as one
	 * deliberate palette rather than a pile of clashing brights.
	 */
	public static final Palette MOLTEN_GAUNTLET = builder()
		.name("#F695EE").points("#7CE782").headerAccent("#27E3FD").desc("#A19BAF")
		.positive("#7CE782").negative("#EE6570").locked("#7C7885").neutralMeta("#918C9D")
		.accent("#E175D9").modeSelected("#AF4DA9").modeSelectedText("#FFFFFF")
		.diffEasy("#7CE782").diffMid("#F3C935").diffHard("#EF656B")
		.tierEasy("#88E48C").tierMedium("#44D7FF").tierHard("#F0CF4C")
		.tierElite("#C896FF").tierMaster("#FD7277").tierGrandmaster("#F0D947").build();

	/**
	 * Cool and airy: one blue family (OKLCH hue 240–245) with analogous cyan/teal carrying value, and a
	 * single complementary rose for the negatives. Lightness stays high and chroma stays low throughout,
	 * so the colours read as tinted white rather than as colour.
	 */
	public static final Palette LITE = builder()
		.name("#E0E9F1").points("#84DCE0").headerAccent("#A4D8FE").desc("#95A0AA")
		.positive("#7ED6D5").negative("#E1938C").locked("#798189").neutralMeta("#89949D")
		.accent("#68B2EF").modeSelected("#3E80B4").modeSelectedText("#FFFFFF")
		.diffEasy("#8CD6BA").diffMid("#E0C88E").diffHard("#E3928B")
		.tierEasy("#92D5B7").tierMedium("#9BD2FA").tierHard("#E0C88E")
		.tierElite("#C6B1F0").tierMaster("#ECA19A").tierGrandmaster("#E1D195").build();

	/**
	 * Near-achromatic. The text roles are pure neutral greys; only the semantic ones (points, difficulty,
	 * tiers) carry a whisper of chroma — just enough to stay tellable apart. A single muted steel accent
	 * keeps it from being lifeless.
	 */
	public static final Palette MODERN_DARK = builder()
		.name("#DEDEDE").points("#A4C0A8").headerAccent("#BEBEBE").desc("#929292")
		.positive("#A4C0A8").negative("#B88C87").locked("#777777").neutralMeta("#8C8C8C")
		.accent("#99AEC0").modeSelected("#5B6B79").modeSelectedText("#FFFFFF")
		.diffEasy("#9EBAA2").diffMid("#C5B693").diffHard("#B88C87")
		.tierEasy("#A0B9A4").tierMedium("#9BB5C8").tierHard("#C3B697")
		.tierElite("#AFA5C4").tierMaster("#C29995").tierGrandmaster("#C7BE9E").build();

	/** Brighter, and picked to stay legible for colour-blind readers: cyan for value, blue-amber-red for
	 *  difficulty. Nearly every pairing meets AAA contrast. */
	public static final Palette HIGH_CONTRAST = builder()
		.name("#FFB84D").points("#5FD3C6").headerAccent("#FFD98A").desc("#C9C2B0")
		.positive("#5FD3C6").negative("#FF9E85").locked("#9A9A9A").neutralMeta("#9A9A9A")
		.accent("#E8A33D").modeSelected("#E8A33D").modeSelectedText("#1E1E1E")
		.diffEasy("#6FB7FF").diffMid("#FFC24D").diffHard("#FF9E85")
		.tierEasy("#7FBFF2").tierMedium("#5FD3C6").tierHard("#FFC24D")
		.tierElite("#C9A0FF").tierMaster("#FF9E85").tierGrandmaster("#FFD98A").build();
}
