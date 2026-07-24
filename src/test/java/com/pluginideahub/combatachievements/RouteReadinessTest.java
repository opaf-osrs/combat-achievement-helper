package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.effort.BossDifficultyLibrary;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.effort.TrainingPlanner;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Route is built from what the player is READY for, not merely what is ungated. Dagannoth Kings and
 * the Mimic carry no hard gate, so a level-3 counted them as doable and the solver used them to close the
 * gap to Easy 80 levels early. When the ready set cannot cover the gap the route stops short and flags
 * {@code trainFirst} instead, handing over to "Train next".
 */
public class RouteReadinessTest
{
	private final CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
	private final EffortDataLibrary effort = EffortDataLibrary.loadBundled();
	private final RecStatsLibrary recStats = RecStatsLibrary.loadBundled();

	private static PlayerProfile account(int everyStat)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String s : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer",
			"Mining", "Herblore", "Farming", "Construction", "Agility", "Thieving", "Firemaking", "Fishing",
			"Woodcutting", "Crafting", "Fletching", "Runecraft", "Hunter", "Smithing", "Cooking"})
		{
			levels.put(s, everyStat);
		}
		levels.put("Hitpoints", Math.max(10, everyStat));
		Set<String> quests = new HashSet<>(Arrays.asList(
			"Priest in Peril", "The Restless Ghost", "Children of the Sun"));
		return PlayerProfile.of(levels, quests, quests);
	}

	private SidePanelViewModel.PathView routeFor(PlayerProfile profile, boolean withRecStats)
	{
		RecStatsLibrary rec = withRecStats ? recStats : RecStatsLibrary.empty();
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(rec)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, rec, profile), null)
			.path();
	}

	private SidePanelViewModel.PathView routeAtPoints(PlayerProfile profile, int gamePoints)
	{
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), gamePoints, gamePoints, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null)
			.path();
	}

	private SidePanelViewModel.PathView routeBarring(PlayerProfile profile, int gamePoints,
		java.util.Set<Integer> barred)
	{
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.barred(barred)
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), gamePoints, gamePoints, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null)
			.path();
	}

	private SidePanelViewModel.PathView routePinning(PlayerProfile profile, int gamePoints,
		java.util.Set<Integer> pinned)
	{
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.pinned(pinned)
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), gamePoints, gamePoints, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null)
			.path();
	}

	@Test
	public void pinningDoesNotChangeThePointsStillToGo()
	{
		PlayerProfile maxed = account(99);
		SidePanelViewModel.PathView before = routePinning(maxed, 1000, Collections.emptySet());
		java.util.Set<Integer> chosen = new HashSet<>();
		before.steps.forEach(r -> chosen.add(r.id));
		int unchosen = lib.all().stream()
			.filter(t -> t.hasMonster() && !chosen.contains(t.id()))
			.map(t -> t.id())
			.findFirst().orElseThrow(() -> new AssertionError("expected an unrouted CA"));

		SidePanelViewModel.PathView after = routePinning(maxed, 1000, Collections.singleton(unchosen));

		// The gap is a fact about the account. Pinning changes what the solver has left to cover, but
		// watching "N pts to go" fall as you pin would read as if pinning had earned you points.
		assertEquals("points still to go is unchanged by pinning", before.pointsGap, after.pointsGap);
	}

	@Test
	public void pinningATaskForcesItIntoTheRouteAndShrinksTheRest()
	{
		PlayerProfile maxed = account(99);
		SidePanelViewModel.PathView before = routePinning(maxed, 1000, Collections.emptySet());
		assertTrue("precondition: a real route", before.steps.size() > 2);

		// Pick a CA the solver did NOT choose, and pin it.
		java.util.Set<Integer> chosen = new HashSet<>();
		before.steps.forEach(r -> chosen.add(r.id));
		int unchosen = lib.all().stream()
			.filter(t -> t.hasMonster() && !chosen.contains(t.id()))
			.map(t -> t.id())
			.findFirst().orElseThrow(() -> new AssertionError("expected an unrouted CA"));

		SidePanelViewModel.PathView after = routePinning(maxed, 1000, Collections.singleton(unchosen));

		assertTrue("the pinned CA is now in the route",
			after.steps.stream().anyMatch(r -> r.id == unchosen));
		assertTrue("the route still covers the gap", after.shownPoints() >= after.pointsGap);
	}

	@Test
	public void barringARouteTaskReplacesItAndStillClosesTheGap()
	{
		PlayerProfile maxed = account(99);
		SidePanelViewModel.PathView before = routeBarring(maxed, 1000, Collections.emptySet());
		assertTrue("precondition: a real route", before.steps.size() > 2);
		assertTrue("precondition: it closes the gap", before.reachable);

		// Bar the route's first task: it must vanish, and the solver must close the same gap without it.
		int barredId = before.steps.get(0).id;
		SidePanelViewModel.PathView after = routeBarring(maxed, 1000,
			Collections.singleton(barredId));

		assertTrue("the barred task is gone from the route",
			after.steps.stream().noneMatch(s -> s.id == barredId));
		assertTrue("the gap is still closed by the next-best tasks", after.reachable);
		assertTrue("the route still covers the points gap",
			after.shownPoints() >= after.pointsGap);
	}

	@Test
	public void shownPointsIsTheSumOfTheListedTasks()
	{
		SidePanelViewModel.PathView path = routeAtPoints(account(99), 1000);
		int sum = path.steps.stream().mapToInt(s -> s.points).sum();
		assertEquals("the header total matches the cards", sum, path.shownPoints());
	}

	@Test
	public void aReadyAccountIsNotSentToHardContentWhenEasyContentClosesTheGap()
	{
		// The Nex-vs-Chaos-Fanatic case: a maxed account meets Nex's recommended stats, so a time-only route
		// would send it there (Nex is efficient per point). Weighting the route by difficulty means the easy
		// content it can also do is preferred, so a modest gap is closed without reaching for difficulty 7+.
		PlayerProfile maxed = account(99);
		SidePanelViewModel.PathView path = routeAtPoints(maxed, 1000); // ~64 pts short of Elite

		assertTrue("precondition: a real route with steps", path.steps.size() > 3);
		int hardest = path.steps.stream().mapToInt(s -> s.detail.difficulty).max().orElse(0);
		assertTrue("a small gap is closed with easy content, not difficulty-7+ bosses (hardest was "
				+ hardest + ")", hardest <= 6);
	}

	/** The worst single-stat gap the route asks the account to stretch across. */
	private int worstGapInRoute(PlayerProfile profile, SidePanelViewModel.PathView path)
	{
		int worst = 0;
		for (SidePanelViewModel.PathRow step : path.steps)
		{
			worst = Math.max(worst, profile.worstShortfall(recStats.softFor(step.id)));
		}
		return worst;
	}

	@Test
	public void aLevelThreeIsNeverRoutedAtContentItIsNotReadyFor()
	{
		PlayerProfile fresh = account(1);
		SidePanelViewModel.PathView path = routeFor(fresh, true);

		assertTrue("every route step is within the ready line (worst gap "
				+ worstGapInRoute(fresh, path) + ")",
			worstGapInRoute(fresh, path) <= TrainingPlanner.VIABLE_WORST_GAP);

		for (SidePanelViewModel.PathRow step : path.steps)
		{
			assertFalse("a level-3 must not be routed at Dagannoth Kings (" + step.name + ")",
				step.detail.monster.startsWith("Dagannoth"));
		}
	}

	@Test
	public void aLevelThreeIsToldToTrainRatherThanShownAFantasyPath()
	{
		SidePanelViewModel.PathView path = routeFor(account(1), true);
		assertTrue("the route stops short", !path.reachable);
		assertTrue("and says so as a training problem, not a missing-content one", path.trainFirst);
	}

	@Test
	public void anAccountThatIsReadyStillGetsARealRouteToTheTier()
	{
		// All-55s is combat 70, so the beginner rule has released and 112 pts of ready content stand against
		// a 41-pt gap. Readiness filtering must not cost this account its route.
		PlayerProfile ready = account(55);
		SidePanelViewModel.PathView path = routeFor(ready, true);

		assertTrue("a combat-70 account reaches Easy", path.reachable);
		assertFalse("so it is not told to train first", path.trainFirst);
		assertTrue("the route covers the gap",
			path.steps.get(path.steps.size() - 1).cumulativePoints >= path.pointsGap);
		assertTrue("without stretching past the ready line", worstGapInRoute(ready, path)
			<= TrainingPlanner.VIABLE_WORST_GAP);
	}

	@Test
	public void stoppingShortStillShowsEverythingTheAccountCanDo()
	{
		// "Stop early" means stop at the ready line, NOT hide content. An all-40s account is 7 pts short of
		// Easy once raid content is held back, and must still be given the ~25 CAs it can actually do.
		PlayerProfile mid = account(40);
		SidePanelViewModel.PathView path = routeFor(mid, true);

		assertTrue("this account is short of the tier", path.trainFirst);
		assertTrue("but keeps a substantial route (" + path.steps.size() + " steps)",
			path.steps.size() > 15);
		assertTrue("all of it within reach", worstGapInRoute(mid, path)
			<= TrainingPlanner.VIABLE_WORST_GAP);
		assertFalse("and Train next is populated to close the gap", trainingsFor(mid).isEmpty());
	}

	@Test
	public void aLevelThreeStillGetsToldWhichQuestToDoNext()
	{
		// A quest is permanent progress and worth doing before you can use what it opens, so this section
		// must never empty out — it is the only place that answers "what quest next". Filtering it on
		// same-day reachability once left a brand-new account with nothing here at all.
		SidePanelViewModel vm = viewModelFor(account(1));
		assertFalse("a level-3 is still told what quest to do", vm.unlocks().isEmpty());
		assertFalse("and is told what to train", vm.trainings().isEmpty());
		// The Route lists only CAs that can be attempted now — nothing out of reach and nothing behind a
		// quest — so for a level-3 those two sections are the whole page.
		assertTrue("every route step is doable now",
			vm.path().steps.stream().allMatch(s -> s.detail.doableNow && s.detail.withinReach));

		// Order still respects reach: the short novice quest leads, not a 25-hour questline.
		assertTrue("a quick quest leads the list, not a grandmaster one (" + vm.unlocks().get(0).questName
				+ ")",
			vm.unlocks().get(0).totalMinutes < vm.unlocks().get(vm.unlocks().size() - 1).totalMinutes);
	}

	@Test
	public void anEstablishedAccountStillGetsUnlockSuggestions()
	{
		SidePanelViewModel vm = viewModelFor(account(80));
		assertFalse("an all-80s account still gets unlock suggestions", vm.unlocks().isEmpty());
	}

	private SidePanelViewModel viewModelFor(PlayerProfile profile)
	{
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null);
	}

	private java.util.List<SidePanelViewModel.TrainingView> trainingsFor(PlayerProfile profile)
	{
		return new SidePanelViewModelBuilder(lib, effort, VideoGuideLibrary.loadBundled(),
			GuideLibrary.loadBundled(), TierRewardLibrary.loadBundled(), EffortModel.standard())
			.difficulty(TaskDifficultyLibrary.loadBundled())
			.recStats(recStats)
			.bossDifficulty(BossDifficultyLibrary.loadBundled())
			.detail(TaskDetailLibrary.loadBundled())
			.effortEngine(BossTimingLibrary.loadBundled(), QuestEffortLibrary.loadBundled(),
				SkillXpLibrary.loadBundled(), CombatExperience.empty(), profile, 6)
			.build(new ProgressSnapshot(Collections.emptySet(), 0, 0, null, 1L),
				new ProfileSignalsProvider(effort, recStats, profile), null)
			.trainings();
	}

	@Test
	public void aMaxedAccountIsUnaffected()
	{
		// Everything reads distance 0 at all-99s, so the readiness filter must be a no-op there.
		PlayerProfile maxed = account(99);
		SidePanelViewModel.PathView path = routeFor(maxed, true);
		assertFalse("a maxed account is never told to train", path.trainFirst);
		assertTrue("and still gets a route", !path.steps.isEmpty());
	}

	@Test
	public void withoutRecommendedStatsTheFilterIsANoOp()
	{
		// Callers that never supplied recommended stats (and the pre-login/empty-profile path) must behave
		// exactly as before — no data means no gap means nothing filtered.
		SidePanelViewModel.PathView path = routeFor(account(1), false);
		assertFalse("no rec-stats data cannot produce a train-first verdict", path.trainFirst);
		assertTrue("the unfiltered route still covers the gap",
			path.steps.get(path.steps.size() - 1).cumulativePoints >= path.pointsGap);
	}
}
