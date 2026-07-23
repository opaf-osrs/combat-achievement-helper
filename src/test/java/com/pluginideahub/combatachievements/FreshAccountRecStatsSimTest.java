package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.LowHangingFruitRanker;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.RankedTask;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end regression for the fresh-account fix: a brand-new low-combat account that has done the
 * quests gating Morytania/raid content must NOT be recommended endgame CAs. The soft rec-stats sink
 * (and the redundant-but-safe hard gate) push attemptable-but-underlevelled content down the list
 * without ever hiding it. Guards against the "level-45 account told to do CoX Grandmaster" bug.
 */
public class FreshAccountRecStatsSimTest
{
	// Chambers of Xeric (5-Scale) Speed-Runner: Grandmaster, "Maxed" team, and — critically — no OSRS
	// level gate and no quest gate, so only the soft rec-stats sink can keep it off a fresh account's list.
	private static final int COX_GM_5SCALE = 325;

	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final TaskDifficultyLibrary diff = TaskDifficultyLibrary.loadBundled();
	private final RecStatsLibrary recStats = RecStatsLibrary.loadBundled();

	private static PlayerProfile freshAccount()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("Attack", 40);
		levels.put("Strength", 45);
		levels.put("Defence", 30);
		levels.put("Hitpoints", 44);
		levels.put("Ranged", 35);
		levels.put("Magic", 33);
		levels.put("Prayer", 31);
		levels.put("Slayer", 20);
		levels.put("Mining", 30);
		levels.put("Firemaking", 30);
		// Quests that unlock Morytania/raid content — this is what let endgame CAs slip through before.
		Set<String> quests = new HashSet<>(Arrays.asList(
			"Priest in Peril", "The Restless Ghost", "Vampyre Slayer", "Nature Spirit",
			"Death Plateau", "Dragon Slayer I"));
		return PlayerProfile.of(levels, quests, quests);
	}

	private List<RankedTask> rankDoableNow(SignalsProvider signals)
	{
		return new LowHangingFruitRanker(effort, EffortModel.standard(), diff)
			.rank(lib.all(), Collections.emptySet(), signals, true);
	}

	private static int indexOfTask(List<RankedTask> ranked, int taskId)
	{
		for (int i = 0; i < ranked.size(); i++)
		{
			if (ranked.get(i).achievement().id() == taskId)
			{
				return i;
			}
		}
		return -1;
	}

	@Test
	public void recStatsSinkBuriesEndgameForAFreshAccount()
	{
		PlayerProfile profile = freshAccount();
		List<RankedTask> without = rankDoableNow(new ProfileSignalsProvider(effort, profile));
		List<RankedTask> with = rankDoableNow(new ProfileSignalsProvider(effort, recStats, profile));

		int rankWithout = indexOfTask(without, COX_GM_5SCALE);
		int rankWith = indexOfTask(with, COX_GM_5SCALE);

		// The task is ungated, so it is doable-now (present in the list) in BOTH rankings — the fix is
		// about ORDER, not hiding. If the dataset ever gains a real gate here, this precondition tells us.
		assertTrue("CoX GM 5-scale should be an ungated doable-now task", rankWithout >= 0 && rankWith >= 0);

		// The soft sink must push it far down relative to where the naive points/effort ranking put it.
		assertTrue("rec-stats sink should bury CoX GM (was #" + (rankWithout + 1) + ", now #" + (rankWith + 1) + ")",
			rankWith > rankWithout + 30);

		RankedTask cox = with.get(rankWith);
		assertTrue("flagged below rec stats", cox.belowRecStats());
		assertTrue("large shortfall for a fresh account", cox.recStatsShortfall() > 200);
	}

	@Test
	public void freshAccountTopRecommendationsAreNotGrandmaster()
	{
		List<RankedTask> with = rankDoableNow(new ProfileSignalsProvider(effort, recStats, freshAccount()));
		int limit = Math.min(10, with.size());
		for (int i = 0; i < limit; i++)
		{
			RankedTask rt = with.get(i);
			assertFalse("no Grandmaster CA in the fresh-account top 10 (#" + (i + 1) + " "
					+ rt.achievement().name() + ")",
				rt.achievement().tier() == AchievementTier.GRANDMASTER);
		}
	}

	private SidePanelViewModel buildVm(SignalsProvider signals)
	{
		PlayerProfile profile = freshAccount();
		// A fresh account with a real (present) but empty progress snapshot — nothing completed yet.
		ProgressSnapshot snapshot = new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L);
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(diff)
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(snapshot, signals, null);
	}

	private static List<String> sessionMonsters(SidePanelViewModel vm)
	{
		List<String> names = new ArrayList<>();
		for (SidePanelViewModel.SessionView s : vm.sessions())
		{
			names.add(s.monster);
		}
		return names;
	}

	/** monster -> the biggest soft rec-stats shortfall among its doable tasks, for a rec-stats-aware fresh acct. */
	private Map<String, Integer> maxShortfallByMonster()
	{
		Map<String, Integer> byMonster = new HashMap<>();
		for (RankedTask rt : rankDoableNow(new ProfileSignalsProvider(effort, recStats, freshAccount())))
		{
			String m = rt.achievement().monster();
			byMonster.merge(m, rt.recStatsShortfall(), Math::max);
		}
		return byMonster;
	}

	private static int totalShortfall(List<String> monsters, int topN, Map<String, Integer> byMonster)
	{
		int total = 0;
		for (int i = 0; i < Math.min(topN, monsters.size()); i++)
		{
			total += byMonster.getOrDefault(monsters.get(i), 0);
		}
		return total;
	}

	@Test
	public void sessionsRespectRecStatsSink()
	{
		// The "Next session" list is an opinionated recommendation, so it must prefer bosses the account is
		// actually ready for. With the rec-stats sink, the top sessions should skew toward lower-shortfall
		// bosses than the naive points/time ordering does.
		SidePanelViewModel without = buildVm(new ProfileSignalsProvider(effort, freshAccount()));
		SidePanelViewModel with = buildVm(new ProfileSignalsProvider(effort, recStats, freshAccount()));

		List<String> withSessions = sessionMonsters(with);
		List<String> withoutSessions = sessionMonsters(without);
		assertFalse("sessions are produced for a fresh account (precondition)", withSessions.isEmpty());

		// The sink must actually change the ordering (otherwise it's a no-op on this surface)...
		assertFalse("rec-stats sink should re-order the session suggestions",
			withSessions.equals(withoutSessions));

		// ...and the change must be in the right direction: the sunk top-3 sessions carry less rec-stats
		// shortfall (i.e. the account is more ready for them) than the naive top-3.
		Map<String, Integer> shortfall = maxShortfallByMonster();
		int withTop3 = totalShortfall(withSessions, 3, shortfall);
		int withoutTop3 = totalShortfall(withoutSessions, 3, shortfall);
		assertTrue("sunk top sessions should be more attainable (shortfall " + withTop3
				+ " vs naive " + withoutTop3 + ")",
			withTop3 < withoutTop3);
	}
}
