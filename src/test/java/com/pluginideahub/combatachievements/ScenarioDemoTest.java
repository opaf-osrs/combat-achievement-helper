package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import com.pluginideahub.combatachievements.core.effort.BossSession;
import com.pluginideahub.combatachievements.core.effort.BossTiming;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.SynergyRanker;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.effort.TaskTimeModel;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.Test;

/** Runs the real effort engine on a few player scenarios and writes a narrated report. */
public class ScenarioDemoTest
{
	private static final String[] SKILLS = {
		"Attack", "Strength", "Defence", "Ranged", "Prayer", "Magic", "Runecraft", "Hitpoints",
		"Crafting", "Mining", "Smithing", "Fishing", "Cooking", "Firemaking", "Woodcutting",
		"Agility", "Herblore", "Thieving", "Fletching", "Slayer", "Farming", "Construction", "Hunter"
	};

	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final BossTimingLibrary timing = BossTimingLibrary.loadBundled();
	private final QuestEffortLibrary questEffort = QuestEffortLibrary.loadBundled();
	private final SkillXpLibrary skillXp = SkillXpLibrary.loadBundled();
	private final TaskDifficultyLibrary difficulties = TaskDifficultyLibrary.loadBundled();
	private final TaskTimeModel timeModel = TaskTimeModel.standard();
	private final StringBuilder out = new StringBuilder();

	private static Map<String, Integer> allSkills(int level)
	{
		Map<String, Integer> m = new HashMap<>();
		for (String s : SKILLS)
		{
			m.put(s, level);
		}
		return m;
	}

	private static Set<String> quests(String... q)
	{
		return new HashSet<>(Arrays.asList(q));
	}

	private SidePanelViewModel vm(Map<String, Integer> levels, Set<String> done, Map<String, Integer> kc)
	{
		PlayerProfile profile = PlayerProfile.of(levels, done, done);
		SignalsProvider signals = new ProfileSignalsProvider(effort, profile);
		ProgressSnapshot snapshot = SampleProgress.build(lib);
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(difficulties)
			.effortEngine(timing, questEffort, skillXp, CombatExperience.of(kc), profile, 6)
			.build(snapshot, signals, null);
	}

	private CombatAchievement find(Predicate<CombatAchievement> p)
	{
		for (CombatAchievement a : lib.all())
		{
			if (p.test(a))
			{
				return a;
			}
		}
		return null;
	}

	private void line(String s)
	{
		out.append(s).append('\n');
	}

	private void estimateLine(String label, CombatAchievement a, CombatExperience exp)
	{
		BossTiming t = timing.timingFor(a.monster());
		TaskTimeModel.Estimate e = timeModel.estimate(a, t, exp, difficulties.difficultyFor(a.id()).difficulty());
		double score = e.minutes() == 0 ? a.points() : a.points() / (double) e.minutes();
		line(String.format("  %-22s %-34s %s | TTK %ds+%ds | rating %d | %d pts | ~%d min | score %.3f",
			label, a.name() + " [" + a.tier().displayName() + "/" + a.type().displayName() + "]",
			"kc=" + (a.hasMonster() ? exp.kcFor(a.monster()) : 0),
			t.ttkSeconds(), t.respawnSeconds(), e.abilityRating(), a.points(), e.minutes(), score));
	}

	@Test
	public void writeScenarios() throws Exception
	{
		// ---- Scenario A: synergy — "which boss should I go to next?" ----
		line("SCENARIO A — synergy: a geared main, where to spend the next session");
		line("(all skills 92; broad quest log; KC: Zulrah 600, Vorkath 250, Graardor 40)");
		Map<String, Integer> kcA = new HashMap<>();
		kcA.put("Zulrah", 600);
		kcA.put("Vorkath", 250);
		kcA.put("General Graardor", 40);
		SidePanelViewModel a = vm(allSkills(92),
			quests("Priest in Peril", "Dragon Slayer II", "Monkey Madness II", "Song of the Elves",
				"Regicide", "While Guthix Sleeps", "Bone Voyage", "Children of the Sun"),
			kcA);
		line("Top boss sessions (points / amortised minutes incl. ~6 min trip overhead):");
		int n = 0;
		for (SidePanelViewModel.SessionView s : a.sessions())
		{
			double score = s.totalMinutes == 0 ? s.totalPoints : s.totalPoints / (double) s.totalMinutes;
			line(String.format("  #%d  %-26s %2d CAs | %3d pts | ~%3d min | score %.3f",
				++n, s.monster, s.taskCount, s.totalPoints, s.totalMinutes, score));
			if (n >= 5)
			{
				break;
			}
		}
		// Per-CA breakdown of the two cheapest sessions.
		for (SidePanelViewModel.SessionView s : a.sessions())
		{
			if (!s.monster.equals("Zulrah") && !s.monster.equals("Tormented Demon"))
			{
				continue;
			}
			line("");
			line("  " + s.monster + " breakdown (" + s.totalPoints + " pts, ~" + s.totalMinutes + " min total):");
			for (SidePanelViewModel.SessionTaskView t : s.tasks)
			{
				CombatAchievement ca = lib.byId(t.id);
				String type = ca == null ? "?" : ca.type().displayName();
				String desc = ca == null ? "" : ca.description();
				line(String.format("    %-26s [%-11s] %d pts  ~%2d min  %s",
					t.name, type, t.points, t.estMinutes,
					t.progress.isEmpty() ? "" : "(" + t.progress + ")"));
				line("        " + desc);
			}
		}
		line("");

		// ---- Scenario B: grind length — "easy but long" vs "quick" at equal-ish points ----
		line("SCENARIO B — grind length: a long KC task vs a quick one");
		CombatAchievement barrows = find(x -> x.monster().equals("Barrows") && x.description().contains("25"));
		CombatAchievement quick = find(x -> x.type().displayName().equals("Kill Count")
			&& x.description().toLowerCase().contains("kill")
			&& TaskTimeModel.requiredKills(x.description()) == 1
			&& timing.timingFor(x.monster()).ttkSeconds() > 0
			&& timing.timingFor(x.monster()).ttkSeconds() < 30);
		CombatExperience none = CombatExperience.empty();
		if (barrows != null)
		{
			estimateLine("long grind:", barrows, none);
		}
		if (quick != null)
		{
			estimateLine("quick tick:", quick, none);
		}
		line("  -> same tier can rank very differently once real grind time is counted.");
		line("");

		// ---- Scenario C: unlock-vs-grind — a short quest opening many CAs ----
		line("SCENARIO C — unlock next: a fresh-ish main who hasn't done the gating quests");
		line("(all skills 80; NO quests done)");
		SidePanelViewModel c = vm(allSkills(80), quests(), new HashMap<>());
		line("Top 'Unlock next' suggestions (unlocked CA points / quest+training minutes):");
		int u = 0;
		for (SidePanelViewModel.UnlockView v : c.unlocks())
		{
			double score = v.totalMinutes == 0 ? v.unlockedPoints : v.unlockedPoints / (double) v.totalMinutes;
			line(String.format("  #%d  %-26s %-12s unlocks %2d CAs (%3d pts) | ~%3d min | score %.3f",
				++u, v.questName, "[" + v.difficulty + "]", v.unlockedTaskCount, v.unlockedPoints,
				v.totalMinutes, score));
			if (u >= 5)
			{
				break;
			}
		}
		line("");

		// ---- Scenario D: competence — same hard task, veteran vs novice ----
		line("SCENARIO D — competence: the same execution task for a veteran vs a novice");
		CombatAchievement exec = find(x -> (x.type().displayName().equals("Perfection")
			|| x.type().displayName().equals("Speed"))
			&& timing.timingFor(x.monster()).ttkSeconds() > 0
			&& TaskTimeModel.requiredKills(x.description()) == 1);
		if (exec != null)
		{
			Map<String, Integer> vet = new HashMap<>();
			vet.put(exec.monster(), 500);
			estimateLine("veteran (kc 500):", exec, CombatExperience.of(vet));
			estimateLine("novice  (kc 0):  ", exec, CombatExperience.empty());
			line("  -> KC at the boss lowers expected retries, so the veteran's estimate is smaller.");
		}

		// ---- Scenario E: stacked Kill Count collapses to the binding threshold ----
		line("");
		line("SCENARIO E — stacked Kill Count at one boss (fresh grinder, 0 KC)");
		java.util.List<CombatAchievement> zulrahKc = new java.util.ArrayList<>();
		for (CombatAchievement ca : lib.all())
		{
			if (ca.monster().equals("Zulrah") && ca.type() == TaskType.KILL_COUNT)
			{
				zulrahKc.add(ca);
			}
		}
		BossTiming zt = timing.timingFor("Zulrah");
		int naiveSum = 0;
		for (CombatAchievement ca : zulrahKc)
		{
			naiveSum += timeModel.estimate(ca, zt, CombatExperience.empty(),
				difficulties.difficultyFor(ca.id()).difficulty()).minutes();
		}
		BossSession zs = new SynergyRanker(timeModel, 0)
			.rank(zulrahKc, timing, CombatExperience.empty(), difficulties).get(0);
		line(String.format("  Zulrah KC tasks (TTK %ds + respawn %ds):", zt.ttkSeconds(), zt.respawnSeconds()));
		for (BossSession.Item it : zs.items())
		{
			line(String.format("    %-18s reach %3d KC | this rung adds ~%3d min", it.task().name(),
				it.estimate().requiredKills(), it.sessionMinutes()));
		}
		line("  naive (each task its own grind): ~" + naiveSum + " min");
		line("  incremental rungs (one grind):   ~" + zs.totalMinutes() + " min  (but you can stop at any rung)");

		Files.createDirectories(Paths.get("build"));
		Files.write(Paths.get("build/scenarios.txt"), out.toString().getBytes());
	}
}
