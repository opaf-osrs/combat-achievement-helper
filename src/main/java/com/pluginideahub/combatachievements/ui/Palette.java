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

	/**
	 * The panel's default OSRS look, kept as-is in character: orange names, gold headers, green points.
	 * A warm analogous core (OKLCH hue 62 orange to 88 gold) with green for value and red for negatives.
	 * Its green/red difficulty ramp is the pretty one, not the accessible one — under red-green colour
	 * blindness easy and hard collapse together, which is exactly what High contrast exists for.
	 */
	public static final Palette CLASSIC = builder()
		.name("#FFA752").points("#82D67A").headerAccent("#F2D48E").desc("#AEA798")
		.positive("#7CCF73").negative("#E66F62").locked("#7E7A72").neutralMeta("#938F85")
		.accent("#EF912D").modeSelected("#EF912D").modeSelectedText("#1E1E1E")
		.diffEasy("#7CCF73").diffMid("#F4CC64").diffHard("#E66F62")
		.tierEasy("#90D189").tierMedium("#71C9FA").tierHard("#E8C773")
		.tierElite("#C49BF3").tierMaster("#EF7F72").tierGrandmaster("#F0D777").build();

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
		.name("#D0D9E1").points("#84DCE0").headerAccent("#A4D8FE").desc("#95A0AA")
		.positive("#7ED6D5").negative("#E1938C").locked("#798189").neutralMeta("#89949D")
		.accent("#68B2EF").modeSelected("#3E80B4").modeSelectedText("#FFFFFF")
		.diffEasy("#8CD6BA").diffMid("#E0C88E").diffHard("#E3928B")
		.tierEasy("#92D5B7").tierMedium("#9BD2FA").tierHard("#E0C88E")
		.tierElite("#C6B1F0").tierMaster("#ECA19A").tierGrandmaster("#E1D195").build();

	/**
	 * Near-achromatic. The text roles are pure neutral greys; only the semantic ones (points, difficulty,
	 * tiers) carry chroma — muted, but enough to read as colour rather than as grey. A restrained steel
	 * accent carries the interactive bits.
	 */
	public static final Palette MODERN_DARK = builder()
		.name("#DEDEDE").points("#93C69D").headerAccent("#BEBEBE").desc("#929292")
		.positive("#93C69D").negative("#CA827C").locked("#777777").neutralMeta("#8C8C8C")
		.accent("#89B0D2").modeSelected("#4E6D87").modeSelectedText("#FFFFFF")
		.diffEasy("#8DC096").diffMid("#CEB577").diffHard("#CA827C")
		.tierEasy("#8FBF98").tierMedium("#85B8DC").tierHard("#CDB57B")
		.tierElite("#B2A0D6").tierMaster("#D5908A").tierGrandmaster("#CEBE82").build();

	/**
	 * Built for legibility rather than looks. Everything sits high on the lightness scale — every text
	 * role except the deliberately dim "locked" clears WCAG AAA (7:1) on the card — and the hues stay on
	 * the blue/amber axis that survives red-green colour blindness: value is cyan, never green, and the
	 * difficulty ramp separates by LIGHTNESS as well as hue so it still reads in greyscale. Checked with
	 * Vienot-Brettel protanope and deuteranope simulation: every ramp pair keeps clear separation.
	 */
	public static final Palette HIGH_CONTRAST = builder()
		.name("#FFCD94").points("#73EEF4").headerAccent("#FCE39F").desc("#C8C4B9")
		.positive("#73EEF4").negative("#FFA191").locked("#AEAAA4").neutralMeta("#BBB7AF")
		.accent("#FEB354").modeSelected("#FEB354").modeSelectedText("#1E1E1E")
		.diffEasy("#9BD1FF").diffMid("#FFDA7D").diffHard("#FFA191")
		.tierEasy("#A9D7FF").tierMedium("#73EEF4").tierHard("#FFDA7D")
		.tierElite("#D5B1FF").tierMaster("#FFA191").tierGrandmaster("#FDE7A2").build();
}
