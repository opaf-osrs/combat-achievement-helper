package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Headless preview renderer. Paints the REAL {@link CombatAchievementsPanel} (production Swing
 * classes, real view-model data) onto an off-screen {@link BufferedImage} and writes PNGs to
 * {@code build/ui-preview/} — no game client, no display. Run with {@code ./gradlew renderUi}.
 *
 * <p>Same idea as a Graphics2D overlay preview: painting to an image is the same operation as
 * painting to the screen, so the PNG matches what the live panel draws (fonts/metrics aside).</p>
 */
public final class PanelPreview
{
	private static final int WIDTH = 225;

	private static final String[] SKILLS = {
		"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Hitpoints",
		"Crafting", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting",
		"Agility", "Herblore", "Thieving", "Fletching", "Slayer", "Farming", "Construction", "Hunter"
	};

	public static void main(String[] args) throws Exception
	{
		System.setProperty("java.awt.headless", "true");
		File outDir = new File("build/ui-preview");
		outDir.mkdirs();

		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		EffortDataLibrary effort = EffortDataLibrary.loadBundled();
		VideoGuideLibrary video = VideoGuideLibrary.loadBundled();
		GuideLibrary guides = GuideLibrary.loadBundled();
		TierRewardLibrary rewards = TierRewardLibrary.loadBundled();
		BossTimingLibrary timing = BossTimingLibrary.loadBundled();
		QuestEffortLibrary questEffort = QuestEffortLibrary.loadBundled();
		SkillXpLibrary skillXp = SkillXpLibrary.loadBundled();

		// A mid-level account (all skills 80) so the skill-gated "doable now" filter is visible:
		// tasks needing >80 (e.g. Slayer 85 Abyssal Sire) drop out of quick wins / the route.
		Map<String, Integer> levels = new HashMap<>();
		for (String s : SKILLS)
		{
			levels.put(s, 80);
		}
		// A realistic mid-game quest log: the common early/mid quests done, but NOT the hardest
		// grandmaster questlines — so the quest gate is visible both ways (Barrows etc. unlock; DT2 /
		// Song of the Elves bosses still read "needs ...").
		Set<String> quests = new HashSet<>(Arrays.asList(
			"Priest in Peril", "Bone Voyage", "Dragon Slayer II", "Monkey Madness II",
			"Children of the Sun", "Regicide", "While Guthix Sleeps"));
		PlayerProfile profile = PlayerProfile.of(levels, quests, quests);
		SignalsProvider signals = new ProfileSignalsProvider(effort, profile);

		// A sample kill log: partway through some grinds, veteran at one boss, novice elsewhere.
		Map<String, Integer> kc = new HashMap<>();
		kc.put("Vorkath", 18);
		kc.put("Zulrah", 240);
		kc.put("General Graardor", 5);
		kc.put("Cerberus", 60);
		CombatExperience experience = CombatExperience.of(kc);

		ProgressSnapshot snapshot = SampleProgress.build(lib);
		SidePanelViewModel viewModel = new SidePanelViewModelBuilder(lib, effort, video, guides,
			rewards, EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary.loadBundled())
			.detail(com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary.loadBundled())
			.effortEngine(timing, questEffort, skillXp, experience, profile, 6)
			.build(snapshot, signals, null);

		// A couple of CAs barred, so the Route's "Not doing these" section appears in the previews.
		Set<Integer> barredSample = new HashSet<>();
		for (SidePanelViewModel.PathRow r : viewModel.path().steps)
		{
			if (barredSample.size() < 2)
			{
				barredSample.add(r.id);
			}
		}
		SidePanelViewModel barredModel = new SidePanelViewModelBuilder(lib, effort, video, guides,
			rewards, EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary.loadBundled())
			.detail(com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary.loadBundled())
			.barred(barredSample)
			.effortEngine(timing, questEffort, skillXp, experience, profile, 6)
			.build(snapshot, signals, null);

		SwingUtilities.invokeAndWait(() ->
		{
			CombatAchievementsPanel panel = new CombatAchievementsPanel(action -> { });
			// No-op handlers so the Route's per-CA "−" (bar) control renders in the previews.
			panel.setBarHandlers(id -> { }, id -> { }, () -> { });
			panel.setRouteHandlers(id -> { }, id -> { });
			panel.render(viewModel);

			write(panel, PanelMode.RECOMMENDED, new File(outDir, "02-recommended.png"));
			write(panel, PanelMode.BOSSES, new File(outDir, "07-bosses.png"));
			write(panel, PanelMode.ROUTE, new File(outDir, "03-route.png"));
			panel.setRouteCustomised(true); // so the reset control shows in this preview
			panel.render(barredModel);
			panel.setBarredCollapsed(false);
			write(panel, PanelMode.ROUTE, new File(outDir, "18-route-barred.png"));
			panel.setRouteCustomised(false);
			panel.render(viewModel);
			panel.openFirstCaDetail();
			snapshot(panel, new File(outDir, "08-ca-detail.png")); // how-to collapsed (lean default)
			panel.setHowToDefault(true);
			panel.openFirstCaDetail();
			snapshot(panel, new File(outDir, "08b-ca-detail-expanded.png")); // how-to expanded
			panel.setHowToDefault(false);
			panel.openFirstBossDetail();
			snapshot(panel, new File(outDir, "09-boss-detail.png"));
			panel.setCompletedCollapsed(false);
			panel.openFirstBossWithCompletions();
			snapshot(panel, new File(outDir, "19-boss-completed.png"));
			panel.setCompletedCollapsed(true);
			panel.openFirstLockedCaDetail();
			snapshot(panel, new File(outDir, "15-locked-ca-detail.png"));

			// Theme previews: the same CA-detail screen under each selectable palette, side by side, so
			// the colour themes (names, points, headers, difficulty, requirements) can be compared.
			int ti = 1;
			for (PanelTheme theme : PanelTheme.values())
			{
				panel.applyTheme(theme.palette());
				panel.openFirstCaDetail();
				String key = theme.name().toLowerCase(java.util.Locale.ROOT);
				snapshot(panel, new File(outDir, String.format("theme-%d-%s.png", ti++, key)));
			}
			panel.applyTheme(PanelTheme.CLASSIC.palette()); // restore the default theme

			// A brand-new account: the Route stops at what it is ready for and hands over to "Train next",
			// rather than padding the path out to the tier with content 40+ levels away.
			panel.render(beginnerViewModel(lib, effort, video, guides, rewards, timing, questEffort, skillXp));
			write(panel, PanelMode.ROUTE, new File(outDir, "16-route-beginner.png"));
			write(panel, PanelMode.RECOMMENDED, new File(outDir, "16b-recommended-beginner.png"));

			// Developer mode: the account-simulation controls, collapsed (the default) and opened.
			panel.render(viewModel);
			panel.setDeveloperMode(true);
			panel.showMode(PanelMode.RECOMMENDED);
			snapshot(panel, new File(outDir, "17-dev-collapsed.png"));
			panel.setDevCollapsed(false);
			snapshot(panel, new File(outDir, "17b-dev-expanded.png"));
			panel.setDeveloperMode(false);
		});

		System.out.println("Wrote UI previews to " + outDir.getAbsolutePath());
	}


	/** A level-3 account with nothing completed — the case the Route's readiness stop-short exists for. */
	private static SidePanelViewModel beginnerViewModel(CombatAchievementLibrary lib,
		EffortDataLibrary effort, VideoGuideLibrary video, GuideLibrary guides, TierRewardLibrary rewards,
		BossTimingLibrary timing, QuestEffortLibrary questEffort, SkillXpLibrary skillXp)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : SKILLS)
		{
			levels.put(s, 1);
		}
		levels.put("Hitpoints", 10);
		Set<String> quests = new HashSet<>(Arrays.asList(
			"Priest in Peril", "The Restless Ghost", "Children of the Sun"));
		PlayerProfile fresh = PlayerProfile.of(levels, quests, quests);
		com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary recStats =
			com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary.loadBundled();

		return new SidePanelViewModelBuilder(lib, effort, video, guides, rewards, EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(
				com.pluginideahub.combatachievements.core.effort.BossDifficultyLibrary.loadBundled())
			.detail(com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary.loadBundled())
			.effortEngine(timing, questEffort, skillXp, CombatExperience.empty(), fresh, 6)
			.build(new ProgressSnapshot(java.util.Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, recStats, fresh), null);
	}

	private static void write(CombatAchievementsPanel panel, PanelMode mode, File file)
	{
		panel.showMode(mode);
		snapshot(panel, file);
	}

	private static void snapshot(CombatAchievementsPanel panel, File file)
	{
		try
		{
			panel.setSize(WIDTH, 100);
			int height = Math.max(80, panel.getPreferredSize().height);
			panel.setSize(WIDTH, height);
			layoutTree(panel);

			BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = image.createGraphics();
			Color bg = panel.getBackground() != null ? panel.getBackground() : new Color(30, 30, 30);
			g.setColor(bg);
			g.fillRect(0, 0, WIDTH, height);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			panel.printAll(g);
			g.dispose();

			ImageIO.write(image, "png", file);
			System.out.println("  " + file.getName() + "  (" + WIDTH + "x" + height + ")");
		}
		catch (Exception ex)
		{
			System.out.println("  FAILED " + file.getName() + ": " + ex);
		}
	}

	private static void layoutTree(Component c)
	{
		if (c instanceof Container)
		{
			Container container = (Container) c;
			container.doLayout();
			for (Component child : container.getComponents())
			{
				layoutTree(child);
			}
		}
	}

	private PanelPreview()
	{
	}
}
