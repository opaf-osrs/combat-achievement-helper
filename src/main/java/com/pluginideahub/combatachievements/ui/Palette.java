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

	/** Deeper amber names against brighter gold headers, with a teal-violet-coral difficulty ramp so a
	 *  difficulty never reads as the points green or the header gold. */
	public static final Palette MOLTEN_GAUNTLET = builder()
		.name("#E28F30").points("#8BD450").headerAccent("#F2C34E").desc("#BDB49A")
		.positive("#5CB85C").negative("#E4756A").locked("#8F8F8F").neutralMeta("#969696")
		.accent("#DC8A00").modeSelected("#DC8A00").modeSelectedText("#1E1E1E")
		.diffEasy("#5BC0C0").diffMid("#C9A0E0").diffHard("#E4756A")
		.tierEasy("#8BD48B").tierMedium("#78B4DC").tierHard("#C8AA5A")
		.tierElite("#C88FD2").tierMaster("#DC8A78").tierGrandmaster("#E6C85A").build();

	/**
	 * Neutral and cool: near-white names on pale blue headers and grey meta, with only the difficulty and
	 * tier ramps keeping any colour — muted, so they stay readable without breaking the calm.
	 */
	public static final Palette LITE = builder()
		.name("#E7EDF3").points("#8FC7EA").headerAccent("#B3D6F2").desc("#98A2AE")
		.positive("#87C7DE").negative("#D9908F").locked("#78818D").neutralMeta("#88919D")
		.accent("#6BA6D6").modeSelected("#4E86B8").modeSelectedText("#FFFFFF")
		.diffEasy("#8AC7B4").diffMid("#D3C08F").diffHard("#D9908F")
		.tierEasy("#8FC7AE").tierMedium("#8FC0E4").tierHard("#D3C08F")
		.tierElite("#B6A8DC").tierMaster("#D9A492").tierGrandmaster("#D8CA96").build();

	/** One warm accent and near-white headers, so headings separate by weight rather than a third colour. */
	public static final Palette MODERN_DARK = builder()
		.name("#E8935C").points("#6FB749").headerAccent("#F0F0F0").desc("#B0A99A")
		.positive("#6FB749").negative("#E8795F").locked("#969696").neutralMeta("#969696")
		.accent("#DC8A00").modeSelected("#DC8A00").modeSelectedText("#1E1E1E")
		.diffEasy("#6FB749").diffMid("#D9A441").diffHard("#E8795F")
		.tierEasy("#6FB749").tierMedium("#5AA9E0").tierHard("#D9A441")
		.tierElite("#BB84DA").tierMaster("#E8795F").tierGrandmaster("#E6C85A").build();

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
