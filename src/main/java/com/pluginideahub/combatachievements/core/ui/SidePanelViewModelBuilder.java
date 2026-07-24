package com.pluginideahub.combatachievements.core.ui;

import com.pluginideahub.combatachievements.core.achievement.AchievementTier;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievement;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.QuestRequirement;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDetail;
import com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficulty;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskEffortData;
import com.pluginideahub.combatachievements.core.achievement.TierReward;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.core.effort.BossSession;
import com.pluginideahub.combatachievements.core.effort.BossTiming;
import com.pluginideahub.combatachievements.core.effort.BossDifficultyLibrary;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.effort.SynergyRanker;
import com.pluginideahub.combatachievements.core.effort.TrainingPlanner;
import com.pluginideahub.combatachievements.core.effort.TrainingSuggestion;
import com.pluginideahub.combatachievements.core.effort.ScalingLibrary;
import com.pluginideahub.combatachievements.core.effort.TaskTimeModel;
import com.pluginideahub.combatachievements.core.effort.UnlockPlanner;
import com.pluginideahub.combatachievements.core.effort.UnlockSuggestion;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.guide.Guide;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideStep;
import com.pluginideahub.combatachievements.core.path.OptimalPathSolver;
import com.pluginideahub.combatachievements.core.path.PathPlan;
import com.pluginideahub.combatachievements.core.path.PathStep;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.LowHangingFruitRanker;
import com.pluginideahub.combatachievements.core.ranking.RankedTask;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.tier.TierMath;
import com.pluginideahub.combatachievements.core.tier.TierProgress;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure assembler: turns a {@link ProgressSnapshot} plus the loaded libraries into a render-ready
 * {@link SidePanelViewModel}. No RuneLite types — all live account facts arrive via
 * {@link SignalsProvider}, so the whole view can be built and tested without a game client.
 */
public final class SidePanelViewModelBuilder
{
	private final CombatAchievementLibrary lib;
	private final EffortDataLibrary effortLib;
	private final VideoGuideLibrary videoLib;
	private final GuideLibrary guideLib;
	private final TierRewardLibrary rewardLib;
	private final EffortModel model;
	private TaskDifficultyLibrary difficultyLib = TaskDifficultyLibrary.empty();
	private TaskDetailLibrary detailLib = TaskDetailLibrary.empty();
	private LowHangingFruitRanker ranker;
	private final OptimalPathSolver solver;

	// Effort-engine inputs (optional; default to empty/neutral so existing callers are unaffected).
	private BossTimingLibrary timingLib = BossTimingLibrary.empty();
	private QuestEffortLibrary questEffortLib = QuestEffortLibrary.empty();
	private SkillXpLibrary skillXpLib = SkillXpLibrary.empty();
	private CombatExperience experience = CombatExperience.empty();
	private PlayerProfile profile = PlayerProfile.empty();
	private ScalingLibrary scalingLib = ScalingLibrary.defaults();
	private RecStatsLibrary recStatsLib = RecStatsLibrary.empty();
	private BossDifficultyLibrary bossDifficultyLib = BossDifficultyLibrary.empty();
	private int tripOverheadMinutes = 6;
	// Configurable CAs-ranking weights (1.0 = neutral). The Route no longer uses these — it has its own
	// time-based cost (see routeCost) — so the CAs list and Route are decoupled.
	private double caPointsWeight = 1.0;
	private double caDifficultyWeight = 1.0;
	private double unlockBias = 1.0;
	private double unlockDifficultyWeight = 1.0;
	private long routeShuffleSeed;
	/** Task ids the player has barred from the Route ("I'm not doing that one"). */
	private Set<Integer> barredTasks = Collections.emptySet();
	/** Task ids the player has pinned INTO the Route ("I want to do this one"). */
	private Set<Integer> pinnedTasks = Collections.emptySet();
	// Deliberately fixed (not config): the panel shows a focused shortlist of the best sessions/unlocks;
	// the underlying rankings are complete, these just cap what is rendered.
	private static final int SESSIONS_LIMIT = 8;
	private static final int UNLOCKS_LIMIT = 6;
	private static final int LOCKED_ROUTE_CAS_LIMIT = 30;
	private static final int TRAININGS_LIMIT = 4;
	/**
	 * Combat level at which the beginner rule stops applying regardless of CA points. An established player
	 * who has simply never touched Combat Achievements starts at 0 points, and must not be treated as new.
	 */
	private static final int BEGINNER_COMBAT_LEVEL = 70;

	public SidePanelViewModelBuilder(CombatAchievementLibrary lib, EffortDataLibrary effortLib,
		VideoGuideLibrary videoLib, GuideLibrary guideLib, EffortModel model)
	{
		this(lib, effortLib, videoLib, guideLib, TierRewardLibrary.empty(), model);
	}

	public SidePanelViewModelBuilder(CombatAchievementLibrary lib, EffortDataLibrary effortLib,
		VideoGuideLibrary videoLib, GuideLibrary guideLib, TierRewardLibrary rewardLib, EffortModel model)
	{
		this.lib = lib;
		this.effortLib = effortLib == null ? EffortDataLibrary.empty() : effortLib;
		this.videoLib = videoLib == null ? VideoGuideLibrary.empty() : videoLib;
		this.guideLib = guideLib == null ? GuideLibrary.empty() : guideLib;
		this.rewardLib = rewardLib == null ? TierRewardLibrary.empty() : rewardLib;
		this.model = model == null ? EffortModel.standard() : model;
		rebuildRanker();
		this.solver = new OptimalPathSolver();
	}

	/**
	 * Supplies the curated pure-skill Difficulty library the Recommended/Quick-wins ranking uses to
	 * order tasks (easy, worthwhile wins first). Optional — when omitted, ranking falls back to the
	 * neutral difficulty and behaves as before. Rebuilds the ranker so the data takes effect.
	 */
	public SidePanelViewModelBuilder difficulty(TaskDifficultyLibrary difficultyLib)
	{
		this.difficultyLib = difficultyLib == null ? TaskDifficultyLibrary.empty() : difficultyLib;
		rebuildRanker();
		return this;
	}

	private void rebuildRanker()
	{
		// Hand the ranker the same per-task minutes the Route costs by, so a "kill it 15 times" task is
		// priced at what it actually takes rather than by a repeat count. Safe before the effort engine is
		// wired: taskMinutes falls back to the empty timing library and the ranker ignores a zero.
		this.ranker = new LowHangingFruitRanker(this.effortLib, this.model, this.difficultyLib,
			caPointsWeight, caDifficultyWeight)
			.withMinutes(this::curatedTaskMinutes);
	}

	/**
	 * Sets the CAs-ranking weights: {@code pointsWeight} (exponent on points) and {@code difficultyWeight}
	 * (how strongly difficulty inflates effort), each 1.0 = neutral. Affects the CAs list only — the Route
	 * has its own time-based cost ({@link #routeCost}). Rebuilds the ranker.
	 */
	public SidePanelViewModelBuilder rankingWeights(double pointsWeight, double difficultyWeight)
	{
		this.caPointsWeight = pointsWeight;
		this.caDifficultyWeight = difficultyWeight;
		rebuildRanker();
		return this;
	}

	/**
	 * Supplies the per-boss data carrying the curated "Endgame access" flag, used to hold raid/group content
	 * back until the player has unlocked the Easy tier. Optional — without it nothing is held back.
	 */
	public SidePanelViewModelBuilder bossDifficulty(BossDifficultyLibrary bossDifficultyLib)
	{
		this.bossDifficultyLib = bossDifficultyLib == null
			? BossDifficultyLibrary.empty() : bossDifficultyLib;
		return this;
	}

	/**
	 * Supplies the curated recommended stats so the "Train next" planner can model what training would open
	 * up. Optional — without it the training section is simply empty.
	 */
	public SidePanelViewModelBuilder recStats(RecStatsLibrary recStatsLib)
	{
		this.recStatsLib = recStatsLib == null ? RecStatsLibrary.empty() : recStatsLib;
		return this;
	}

	/** Sets the Route "Unlock next" weights: bias (favour big vs quick unlocks) + difficulty weighting. */
	public SidePanelViewModelBuilder unlockWeights(double unlockBias, double unlockDifficultyWeight)
	{
		this.unlockBias = unlockBias;
		this.unlockDifficultyWeight = unlockDifficultyWeight;
		return this;
	}

	/**
	 * Tasks the player has barred from the Route. They are dropped from the candidate set before the solve,
	 * so the solver closes the gap with the next-best task instead — or, if nothing can replace them, leaves
	 * the gap open and the Route says what is still needed.
	 */
	public SidePanelViewModelBuilder barred(Set<Integer> taskIds)
	{
		this.barredTasks = taskIds == null || taskIds.isEmpty()
			? Collections.emptySet() : new HashSet<>(taskIds);
		return this;
	}

	/**
	 * Tasks the player has pinned into the Route. They are taken first and their points come off the gap,
	 * then the solver optimally fills whatever is left — so pinning steers the route without abandoning
	 * the optimisation for everything else.
	 */
	public SidePanelViewModelBuilder pinned(Set<Integer> taskIds)
	{
		this.pinnedTasks = taskIds == null || taskIds.isEmpty()
			? Collections.emptySet() : new HashSet<>(taskIds);
		return this;
	}

	/**
	 * Seed for the Route "suggest new" reshuffle: a non-zero value perturbs the path cost so the solver
	 * re-picks a different but still-quick set. 0 = the true optimum.
	 */
	public SidePanelViewModelBuilder routeShuffle(long seed)
	{
		this.routeShuffleSeed = seed;
		return this;
	}

	/** Supplies the curated stats/setup/strategy/items text for the CA-detail drill-in. Optional. */
	public SidePanelViewModelBuilder detail(TaskDetailLibrary detailLib)
	{
		this.detailLib = detailLib == null ? TaskDetailLibrary.empty() : detailLib;
		return this;
	}

	/**
	 * Supplies the effort-engine inputs that drive the synergy "Next session" bundles and the
	 * "Unlock next" suggestions. Optional — when omitted those views are simply empty.
	 *
	 * @param tripOverheadMinutes the fixed per-visit overhead (the clustering-strength dial)
	 */
	public SidePanelViewModelBuilder effortEngine(BossTimingLibrary timing, QuestEffortLibrary questEffort,
		SkillXpLibrary skillXp, CombatExperience experience, PlayerProfile profile, int tripOverheadMinutes)
	{
		this.timingLib = timing == null ? BossTimingLibrary.empty() : timing;
		this.questEffortLib = questEffort == null ? QuestEffortLibrary.empty() : questEffort;
		this.skillXpLib = skillXp == null ? SkillXpLibrary.empty() : skillXp;
		this.experience = experience == null ? CombatExperience.empty() : experience;
		this.profile = profile == null ? PlayerProfile.empty() : profile;
		this.tripOverheadMinutes = Math.max(0, tripOverheadMinutes);
		rebuildRanker(); // timing data has landed; re-bind so the ranker costs by real minutes
		return this;
	}

	/**
	 * Supplies the curated attempt-scaling table (the single place the time model gets its per-type base
	 * attempts, ability curve and competence discounts). Optional — defaults to the built-in constants.
	 */
	public SidePanelViewModelBuilder scaling(ScalingLibrary scaling)
	{
		this.scalingLib = scaling == null ? ScalingLibrary.defaults() : scaling;
		return this;
	}

	/** The time model built on the curated scaling table, shared by the session ranker and CA details. */
	private TaskTimeModel timeModel()
	{
		return TaskTimeModel.standard(scalingLib);
	}

	/** Estimated minutes to finish one task, folding in difficulty + attempts + the player's kill count. */
	/**
	 * Estimated minutes, but only when this boss actually HAS curated timing — 0 otherwise, which tells the
	 * ranker to fall back to its effort model. Without the guard a boss with no timing produced a fallback
	 * figure carrying no real signal, and costing by it quietly flattened the difficulty ordering the
	 * quick-wins list depends on.
	 */
	private int curatedTaskMinutes(CombatAchievement task)
	{
		return timingLib.timingFor(task.monster()) == BossTiming.UNKNOWN ? 0 : taskMinutes(task);
	}

	private int taskMinutes(CombatAchievement task)
	{
		TaskDifficulty diff = difficultyLib.difficultyFor(task.id());
		BossTiming timing = timingLib.timingFor(task.monster());
		return timeModel().estimate(task, timing, experience, diff.difficulty(), diff.attemptsOverride())
			.minutes();
	}

	/**
	 * The Route's per-task cost: estimated minutes, weighted by difficulty so an EASY task is preferred
	 * over a HARD one even when the hard one is quicker per point. Without this the route optimised pure
	 * time and sent a player to Nex (difficulty 7–8) instead of finishing the easy CAs at a boss they were
	 * already at (Chaos Fanatic, difficulty 2–4), because Nex is time-efficient per point. The linear
	 * {@code difficulty/3} factor matches how the CAs list ranks, so the two surfaces now agree.
	 */
	/** Same boss key the route solver groups on: the monster, or the task itself when it has none. */
	private static String routeBossKey(CombatAchievement a)
	{
		return a.hasMonster() ? a.monster() : (" solo:" + a.id());
	}

	private double routeCost(RankedTask rt)
	{
		CombatAchievement task = rt.achievement();
		double minutes = Math.max(1, taskMinutes(task));
		int d = rt.difficulty() == null ? 3 : Math.max(1, rt.difficulty().difficulty());
		double difficultyFactor = d / 3.0; // linear, neutral at difficulty 3 — as the CAs ranker uses
		// Below the task's soft recommended stats -> push it later in the route (attemptable, not advised).
		// Same shared sink as the CAs ranker and the Sessions/Bosses ordering.
		double recStatsFactor = rt.recStatsSinkFactor();
		// "Suggest new" reshuffle: a ±30% cost jitter nudges the solver onto a different but still-quick set.
		double jitter = routeShuffleSeed == 0 ? 1.0 : 0.7 + 0.6 * hashUnit(routeShuffleSeed, task.id());
		return minutes * difficultyFactor * recStatsFactor * jitter;
	}

	/** Deterministic uniform [0,1) hash of (seed, id) via a splitmix64 finaliser (decorrelated seeds). */
	private static double hashUnit(long seed, int id)
	{
		long h = seed * 0x9E3779B97F4A7C15L + (id + 1L) * 0xD1B54A32D192ED03L;
		h ^= h >>> 30;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 27;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 31;
		return (h >>> 11) * 0x1.0p-53;
	}

	/**
	 * @param snapshot     the account progress (or {@link ProgressSnapshot#absent()})
	 * @param signals      live per-task signals
	 * @param explicitTarget optional tier the user is aiming for; null = the next tier to unlock
	 */
	public SidePanelViewModel build(ProgressSnapshot snapshot, SignalsProvider signals,
		AchievementTier explicitTarget)
	{
		List<CombatAchievement> all = lib.all();

		if (snapshot == null || !snapshot.isPresent())
		{
			// Guides (their static parts) are available pre-login; live routes fill in once synced.
			return SidePanelViewModel.loggedOut(buildGuides(all, 0, null));
		}

		int gamePoints = snapshot.gamePoints();
		int totalAvailable = TierMath.totalPointsAvailable(all);

		List<TierProgress> tiers = TierMath.progressByTier(snapshot.completedIds(), all, gamePoints);
		List<SidePanelViewModel.TierRow> tierRows = new ArrayList<>();
		for (TierProgress tp : tiers)
		{
			TierReward reward = rewardLib.forTier(tp.tier());
			tierRows.add(new SidePanelViewModel.TierRow(
				tp.tier().displayName(), tp.completedCount(), tp.totalCount(),
				tp.earnedPointsInTier(), tp.totalPointsInTier(), tp.cumulativeThreshold(),
				tp.unlocked(), tp.pointsRemainingToUnlock(), tp.fullyComplete(),
				reward.headline(), reward.rewards()));
		}

		AchievementTier currentTier = TierMath.currentTierFor(gamePoints, all);
		TierMath.TierGap gap = TierMath.gapToNextTier(gamePoints, all);
		String currentTierName = currentTier == null ? "None" : currentTier.displayName();
		String nextTierName = gap == null ? "" : gap.nextTier().displayName();
		int nextNeeded = gap == null ? 0 : gap.pointsNeeded();
		boolean allUnlocked = gap == null;

		// Rank every incomplete task once. The CAs panel gets the FULL doable-now list (it filters by
		// search + sort and caps rendering itself), so those work over everything, not just a top-N.
		List<RankedTask> rankedIncomplete = ranker.rank(all, snapshot.completedIds(), signals, false);

		// Beginner rule: until the Easy tier is unlocked, hold back raid/group content ("Endgame access") —
		// unless the account is already 70+ combat, in which case it is an established player who simply
		// hasn't started Combat Achievements, and nothing should be held back from them.
		// It is NOT hidden because those tasks are hard — several are genuinely easy mechanics — but because
		// a brand-new account cannot realistically get INTO that content, and recommending it reads as noise.
		// Once Easy is unlocked, everything returns and the normal readiness weighting takes over.
		boolean beginner = gamePoints < TierMath.thresholdFor(AchievementTier.EASY, all)
			&& profile.combatLevel() < BEGINNER_COMBAT_LEVEL;
		List<RankedTask> recommendable = beginner ? withoutGated(rankedIncomplete) : rankedIncomplete;

		List<SidePanelViewModel.TaskRow> quickWins = new ArrayList<>();
		for (RankedTask rt : recommendable)
		{
			if (rt.doableNow())
			{
				quickWins.add(toTaskRow(rt));
			}
		}

		// Unlock suggestions drive both the "Unlock next" list and the locked CAs shown in the Route.
		Map<Integer, CombatAchievement> byId = new HashMap<>();
		for (CombatAchievement a : all)
		{
			byId.put(a.id(), a);
		}
		List<UnlockSuggestion> unlockSuggestions = readinessRanked(
			new UnlockPlanner(questEffortLib, skillXpLib)
				.plan(all, snapshot.completedIds(), effortLib, profile, difficultyLib, unlockBias,
					unlockDifficultyWeight, recStatsLib),
			byId, signals);
		List<SidePanelViewModel.CaDetail> lockedRouteCas = lockedRouteCas(unlockSuggestions, all, signals, beginner);

		SidePanelViewModel.PathView path = buildPath(all, gamePoints, gap, explicitTarget,
			recommendable, lockedRouteCas);

		List<SidePanelViewModel.SessionView> sessions = buildSessions(recommendable);
		List<SidePanelViewModel.UnlockView> unlocks = buildUnlocks(unlockSuggestions);
		List<SidePanelViewModel.TrainingView> trainings = buildTrainings(
			beginner ? withoutGatedTasks(all) : all, snapshot.completedIds());
		// Beginner-gated like every other surface: a brand-new account had Chambers of Xeric and Theatre of
		// Blood sitting in its boss directory because this one list was built from the ungated ranking.
		List<SidePanelViewModel.BossRow> bosses = buildBossDirectory(recommendable, all,
			snapshot.completedIds());

		return SidePanelViewModel.ready(lib.version(), gamePoints, totalAvailable,
			snapshot.completedCount(), lib.taskCount(), currentTierName, nextTierName, nextNeeded,
			allUnlocked, snapshot.datasetLooksStale(), tierRows, quickWins, path,
			buildGuides(all, gamePoints, rankedIncomplete), sessions, unlocks, trainings, bosses,
			snapshot.isSample());
	}

	/**
	 * Builds the guide views. When a guide has a target tier and the player is synced
	 * ({@code rankedIncomplete != null}), its live optimal route to that tier is resolved too.
	 */
	private List<SidePanelViewModel.GuideView> buildGuides(List<CombatAchievement> all,
		int gamePoints, List<RankedTask> rankedIncomplete)
	{
		List<SidePanelViewModel.GuideView> views = new ArrayList<>();
		for (Guide guide : guideLib.all())
		{
			List<String> tips = new ArrayList<>();
			for (GuideStep step : guide.steps())
			{
				String note = step.note();
				if (step.hasTask())
				{
					CombatAchievement task = lib.byId(step.taskId());
					if (task != null)
					{
						note = task.name() + " — " + note;
					}
				}
				tips.add(note);
			}

			SidePanelViewModel.PathView route = null;
			if (guide.targetTier() != null && rankedIncomplete != null)
			{
				route = buildPath(all, gamePoints, null, guide.targetTier(), rankedIncomplete,
					java.util.Collections.<SidePanelViewModel.CaDetail>emptyList());
			}

			views.add(new SidePanelViewModel.GuideView(guide.id(), guide.title(), guide.author(),
				guide.summary(), guide.videoUrl(),
				guide.targetTier() == null ? "" : guide.targetTier().displayName(),
				guide.tags(), tips, route));
		}
		return views;
	}

	private SidePanelViewModel.PathView buildPath(List<CombatAchievement> all, int gamePoints,
		TierMath.TierGap gap, AchievementTier explicitTarget, List<RankedTask> rankedIncomplete,
		List<SidePanelViewModel.CaDetail> lockedCas)
	{
		AchievementTier target = explicitTarget != null
			? explicitTarget
			: (gap != null ? gap.nextTier() : AchievementTier.GRANDMASTER);

		PathPlan plan;
		boolean trainFirst = false;
		List<RankedTask> pinnedSteps = Collections.emptyList();
		// The gap the PLAYER still has to the tier. Pinning shrinks what the solver has left to cover, but
		// it must never change the "N pts to go" the panel shows: that is a fact about the account, not
		// about which tasks are pinned, and watching it drop as you pin reads as if pinning earned points.
		int trueGap = 0;
		if (target == AchievementTier.GRANDMASTER)
		{
			// GM additionally requires completing every task, so the path is all incomplete tasks.
			plan = solver.solveCompleteAll(target, rankedIncomplete, this::routeCost);
			trueGap = plan.pointsGap();
		}
		else
		{
			int threshold = TierMath.thresholdFor(target, all);
			int pointsGap = Math.max(0, threshold - gamePoints);
			trueGap = pointsGap;
			List<RankedTask> doableNow = new ArrayList<>();
			List<RankedTask> ready = new ArrayList<>();
			// Pinned tasks are taken whatever the solver would have chosen. They are honoured even when the
			// account is below their recommended stats: the player asked for this one specifically, and
			// second-guessing an explicit choice is worse than letting them take a hard task early.
			List<RankedTask> forced = new ArrayList<>();
			int forcedPoints = 0;
			int doablePoints = 0;
			int readyPoints = 0;
			for (RankedTask rt : rankedIncomplete)
			{
				if (pinnedTasks.contains(rt.achievement().id()) && rt.doableNow())
				{
					forced.add(rt);
					forcedPoints += rt.achievement().points();
					continue;
				}
				// Barred tasks are ones the player has explicitly said they do not want to do. Dropping them
				// before the solve (rather than filtering the result) is what lets the next-best task take
				// the freed slot; if nothing is left to take it, the gap simply stays open and the Route
				// falls back to Train next / "not enough doable tasks", which is the honest answer.
				if (!rt.doableNow() || barredTasks.contains(rt.achievement().id()))
				{
					continue;
				}
				doableNow.add(rt);
				doablePoints += rt.achievement().points();
				if (isReadyFor(rt))
				{
					ready.add(rt);
					readyPoints += rt.achievement().points();
				}
			}

			// The route is built from what the player is READY for, not merely what is ungated. Without this a
			// level-3 was routed at Dagannoth Kings: those tasks have no hard gate, so they counted as doable
			// and the solver happily used them to close the gap 80 levels early. Stopping short and handing
			// over to "Train next" is the honest answer — see the trainFirst flag.
			// Pinned points already count toward the tier, so only the remainder still needs solving.
			int remainingGap = Math.max(0, pointsGap - forcedPoints);
			trainFirst = readyPoints + forcedPoints < pointsGap && doablePoints + forcedPoints >= pointsGap;
			// Quickest path to the tier, boss-aware: a fixed trip overhead per boss makes the solver keep you
			// at a boss (do several CAs there) rather than scatter one-CA visits across the game and close the
			// last gap at a far-off boss. tripOverheadMinutes is the clustering-strength dial.
			plan = solver.solveClustered(target, remainingGap, ready, this::routeCost, tripOverheadMinutes);
			pinnedSteps = forced;
		}

		// Pinned tasks sit wherever they naturally belong, not bolted to the front. They are merged with
		// the solved set and the whole lot is ordered the way the solver orders its own output — grouped by
		// boss, cheapest boss first — so a pinned CA lands beside its boss-mates instead of dragging its
		// boss to the top of the route.
		List<CombatAchievement> ordered = new ArrayList<>();
		Map<String, Double> bossTotal = new LinkedHashMap<>();
		Map<Integer, Double> costById = new HashMap<>();
		List<RankedTask> merged = new ArrayList<>(pinnedSteps);
		for (PathStep step : plan.steps())
		{
			for (RankedTask rt : rankedIncomplete)
			{
				if (rt.achievement().id() == step.achievement().id())
				{
					merged.add(rt);
					break;
				}
			}
		}
		for (RankedTask rt : merged)
		{
			double c = routeCost(rt);
			costById.put(rt.achievement().id(), c);
			bossTotal.merge(routeBossKey(rt.achievement()), c, Double::sum);
		}
		merged.sort(Comparator
			.comparingDouble((RankedTask rt) -> bossTotal.getOrDefault(routeBossKey(rt.achievement()), 0.0))
			.thenComparing(rt -> routeBossKey(rt.achievement()))
			.thenComparingDouble(rt -> costById.getOrDefault(rt.achievement().id(), 0.0))
			.thenComparingInt(rt -> rt.achievement().id()));
		merged.forEach(rt -> ordered.add(rt.achievement()));

		List<SidePanelViewModel.PathRow> steps = new ArrayList<>();
		int running = 0;
		for (CombatAchievement a : ordered)
		{
			running += a.points();
			steps.add(new SidePanelViewModel.PathRow(a.id(), a.name(), a.tier().displayName(),
				a.points(), running, a.wikiUrl(),
				videoLib.bestGuideUrl(a.id(), a.name()), buildCaDetail(a, true, "")));
		}
		// The barred pile, so the panel can list what was set aside and offer it back. Drawn from the
		// incomplete tasks, so anything since completed drops off the list by itself.
		List<SidePanelViewModel.CaDetail> barredCas = new ArrayList<>();
		if (!barredTasks.isEmpty())
		{
			for (RankedTask rt : rankedIncomplete)
			{
				if (barredTasks.contains(rt.achievement().id()))
				{
					barredCas.add(buildCaDetail(rt.achievement(), rt.doableNow(), rt.lockReason()));
				}
			}
		}

		return new SidePanelViewModel.PathView(target.displayName(), trueGap,
			plan.reachable(), plan.alreadyUnlocked(), steps, rewardLib.forTier(target).headline(),
			lockedCas, trainFirst, barredCas);
	}

	/**
	 * Whether the account is close enough to a task's recommended stats to be sent there. Uses the WORST
	 * single-stat gap on {@link TrainingPlanner#VIABLE_WORST_GAP} — the same "ready" line Train next
	 * counts by. With no recommended-stats data (or no profile) the gap is 0, so nothing is filtered and
	 * callers that never supplied them behave exactly as before.
	 */
	private boolean isReadyFor(RankedTask rt)
	{
		return profile.worstShortfall(recStatsLib.softFor(rt.achievement().id()))
			<= TrainingPlanner.VIABLE_WORST_GAP;
	}

	/**
	 * The CAs the top recommended unlocks would open — built as LOCKED detail cards (with a "needs
	 * &lt;quest&gt;" reason) so the Route can list them in red beneath the doable steps. Deduped by id,
	 * bounded so the Route stays readable.
	 */
	private List<SidePanelViewModel.CaDetail> lockedRouteCas(List<UnlockSuggestion> unlockSuggestions,
		List<CombatAchievement> all, SignalsProvider signals, boolean beginner)
	{
		Map<Integer, CombatAchievement> byId = new java.util.HashMap<>();
		for (CombatAchievement a : all)
		{
			byId.put(a.id(), a);
		}
		SignalsProvider sig = signals == null ? SignalsProvider.defaults() : signals;

		// Every CA the top unlock suggestions would open, each tagged with how far below its recommended
		// stats the player is (the same distance the CAs list weights by).
		List<LockedCandidate> candidates = new ArrayList<>();
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		int quests = 0;
		for (UnlockSuggestion s : unlockSuggestions)
		{
			if (quests++ >= UNLOCKS_LIMIT)
			{
				break;
			}
			// The quest's time amortised across the CAs it opens — added to each CA's own time so the
			// Route can slot "quest + CA" in by its true total time vs a harder doable CA.
			int amortizedQuestMinutes = s.totalMinutes() / Math.max(1, s.unlockedTaskIds().size());
			for (int id : s.unlockedTaskIds())
			{
				CombatAchievement task = byId.get(id);
				if (task != null && beginner && bossDifficultyLib.isEndgameAccess(task.monster()))
				{
					continue; // don't dangle raid content in front of a pre-Easy-tier account
				}
				if (task != null && seen.add(id))
				{
					candidates.add(new LockedCandidate(task, s.questName(), amortizedQuestMinutes,
						sig.signalsFor(id).recStatsShortfall()));
				}
			}
		}

		// Weight by readiness the same way the CAs list does — points ÷ the soft-stat sink factor — so
		// distance competes against value. A level-3 is hundreds of levels short of Theatre of Blood, so its
		// ×-huge sink crushes ToB's value and it sinks past the display cap; a near-ready main's ×~1 sink
		// leaves ToB's value intact so it stays. Not a hard wall, just the distance-weight applied here too.
		candidates.sort(Comparator.comparingDouble((LockedCandidate c) -> readinessValue(c)).reversed()
			.thenComparingInt(c -> c.task.id()));

		List<SidePanelViewModel.CaDetail> out = new ArrayList<>();
		for (LockedCandidate c : candidates)
		{
			if (out.size() >= LOCKED_ROUTE_CAS_LIMIT)
			{
				break;
			}
			out.add(buildCaDetail(c.task, false, "needs " + c.questName, c.amortizedMinutes));
		}
		return out;
	}

	/**
	 * Re-orders the unlock suggestions by how much of the value they open this player can actually reach.
	 * An unlock is only worth suggesting for the points you can earn — so a level-3's Priest in Peril (which
	 * opens Barrows, Theatre of Blood, the Nightmare… all far out of reach) drops down the list, taking those
	 * CAs out of the route with it, while for a near-ready main it stays near the top. Layers the readiness
	 * factor on the planner's own score so its difficulty/time/bias tuning is preserved.
	 */
	private List<UnlockSuggestion> readinessRanked(List<UnlockSuggestion> suggestions,
		Map<Integer, CombatAchievement> byId, SignalsProvider signals)
	{
		SignalsProvider sig = signals == null ? SignalsProvider.defaults() : signals;
		List<UnlockSuggestion> ranked = new ArrayList<>(suggestions);
		ranked.sort(Comparator.comparingDouble(
			(UnlockSuggestion s) -> s.score() * readinessFactor(s, byId, sig)).reversed());
		return ranked;
	}

	/** Fraction of a suggestion's unlocked points the player is ready for: Σ(points/sink) ÷ Σ(points), in (0,1]. */
	private static double readinessFactor(UnlockSuggestion s, Map<Integer, CombatAchievement> byId,
		SignalsProvider sig)
	{
		double weighted = 0;
		double raw = 0;
		for (int id : s.unlockedTaskIds())
		{
			CombatAchievement t = byId.get(id);
			if (t != null)
			{
				int pts = Math.max(1, t.points());
				raw += pts;
				weighted += pts / RankedTask.recStatsSinkFactor(sig.signalsFor(id).recStatsShortfall());
			}
		}
		return raw <= 0 ? 1.0 : weighted / raw;
	}

	/** Points discounted by how far the player is below the CA's recommended stats (higher = show sooner). */
	private static double readinessValue(LockedCandidate c)
	{
		return Math.max(1, c.task.points()) / RankedTask.recStatsSinkFactor(c.shortfall);
	}

	/** A CA a suggested unlock would open, tagged with the player's readiness (soft-stat shortfall). */
	private static final class LockedCandidate
	{
		private final CombatAchievement task;
		private final String questName;
		private final int amortizedMinutes;
		private final int shortfall;

		private LockedCandidate(CombatAchievement task, String questName, int amortizedMinutes, int shortfall)
		{
			this.task = task;
			this.questName = questName;
			this.amortizedMinutes = amortizedMinutes;
			this.shortfall = shortfall;
		}
	}

	/**
	 * The full Bosses directory: every boss/activity with an incomplete CA, its incomplete CAs split into
	 * doable and locked. Bosses with no doable CA are flagged locked (greyed in the panel). Ordered
	 * doable-first by projected (doable) points.
	 */
	private List<SidePanelViewModel.BossRow> buildBossDirectory(List<RankedTask> rankedIncomplete,
		List<CombatAchievement> all, java.util.Set<Integer> completedIds)
	{
		// Completed CAs never reach the ranker (it only ranks what is left), so they are gathered here
		// straight from the library. A boss whose CAs are ALL done still will not appear: the directory
		// lists bosses with something outstanding, and that is deliberate.
		Map<String, List<SidePanelViewModel.CaDetail>> completedByBoss = new LinkedHashMap<>();
		for (CombatAchievement a : all)
		{
			if (a.hasMonster() && completedIds.contains(a.id()))
			{
				completedByBoss.computeIfAbsent(a.monster(), k -> new ArrayList<>())
					.add(buildCaDetail(a, true, ""));
			}
		}
		Map<String, List<RankedTask>> byBoss = new LinkedHashMap<>();
		for (RankedTask rt : rankedIncomplete)
		{
			CombatAchievement a = rt.achievement();
			if (a.hasMonster())
			{
				byBoss.computeIfAbsent(a.monster(), k -> new ArrayList<>()).add(rt);
			}
		}

		List<SidePanelViewModel.BossRow> rows = new ArrayList<>();
		for (Map.Entry<String, List<RankedTask>> e : byBoss.entrySet())
		{
			List<SidePanelViewModel.CaDetail> doable = new ArrayList<>();
			List<SidePanelViewModel.CaDetail> locked = new ArrayList<>();
			List<Double> doableSinks = new ArrayList<>();
			int projected = 0;
			String stats = "";
			for (RankedTask rt : e.getValue())
			{
				CombatAchievement a = rt.achievement();
				SidePanelViewModel.CaDetail d = buildCaDetail(a, rt.doableNow(), rt.lockReason());
				if (stats.isEmpty() && !d.stats.isEmpty())
				{
					stats = d.stats; // representative recommended stats for the boss
				}
				if (rt.doableNow())
				{
					doable.add(d);
					doableSinks.add(rt.recStatsSinkFactor());
					projected += a.points();
				}
				else
				{
					locked.add(d);
				}
			}
			rows.add(new SidePanelViewModel.BossRow(e.getKey(), projected, doable.size(), locked.size(),
				doable.isEmpty(), stats, doable, locked, medianSink(doableSinks),
				completedByBoss.getOrDefault(e.getKey(), Collections.emptyList())));
		}

		// The Bosses tab is an honest directory: every boss with an incomplete CA, ordered doable-first by
		// the points actually available there. This default order (and the "Most points" sort) is
		// deliberately NOT sunk by rec-stats — the sort key would diverge from the shown points, and a
		// 29-CA raid legitimately holds the most points. Readiness rides on the row instead
		// ({@code readinessSink}) so only the opinionated "Recommended" sort uses it.
		rows.sort(Comparator
			.comparing((SidePanelViewModel.BossRow b) -> b.locked) // doable bosses first
			.thenComparing(Comparator.comparingInt((SidePanelViewModel.BossRow b) -> b.projectedPoints).reversed())
			.thenComparing(b -> b.monster));
		return rows;
	}

	/**
	 * A boss's readiness penalty: the MEDIAN rec-stats sink across its doable CAs. Median rather than min
	 * because several bosses carry one outlier CA with no recommended stats at all (Chambers of Xeric's
	 * "Playing with Lasers" reads distance 0 for a level-3), and a min would let that one row declare the
	 * whole raid ready. 1.0 = the account is up to it.
	 */
	private static double medianSink(List<Double> sinks)
	{
		if (sinks == null || sinks.isEmpty())
		{
			return 1.0;
		}
		List<Double> sorted = new ArrayList<>(sinks);
		java.util.Collections.sort(sorted);
		int mid = sorted.size() / 2;
		double median = sorted.size() % 2 == 1
			? sorted.get(mid)
			: (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
		return Math.max(1.0, median);
	}

	/** Synergy-ranked boss sessions from the player's doable, incomplete tasks. */
	private List<SidePanelViewModel.SessionView> buildSessions(List<RankedTask> rankedIncomplete)
	{
		List<CombatAchievement> doable = new ArrayList<>();
		Map<Integer, Integer> shortfallById = new HashMap<>();
		for (RankedTask rt : rankedIncomplete)
		{
			if (rt.doableNow())
			{
				doable.add(rt.achievement());
				shortfallById.put(rt.achievement().id(), rt.recStatsShortfall());
			}
		}
		SynergyRanker ranker = new SynergyRanker(timeModel(), tripOverheadMinutes);
		List<BossSession> ranked = new ArrayList<>(ranker.rank(doable, timingLib, experience, difficultyLib));

		// Re-order by a rec-stats-adjusted score: divide each bundle's points/hour by its points-weighted
		// soft-sink factor, so a fresh account isn't suggested an endgame boss (CoX/ToB) as a top "next
		// session" just because it can technically enter it. Applied before the top-N cut below.
		ranked.sort(Comparator
			.comparingDouble((BossSession s) ->
				s.pointsPerHour() / sessionRecStatsSink(s, shortfallById)).reversed()
			.thenComparing(BossSession::monster));

		List<SidePanelViewModel.SessionView> views = new ArrayList<>();
		for (BossSession session : ranked)
		{
			if (views.size() >= SESSIONS_LIMIT)
			{
				break;
			}
			List<SidePanelViewModel.SessionTaskView> tasks = new ArrayList<>();
			for (BossSession.Item item : session.items())
			{
				CombatAchievement a = item.task();
				TaskTimeModel.Estimate est = item.estimate();
				String progress = est.hasPartialProgress()
					? Math.min(est.currentKc(), est.requiredKills()) + " / " + est.requiredKills() + " kills"
					: "";
				tasks.add(new SidePanelViewModel.SessionTaskView(a.id(), a.name(), a.tier().displayName(),
					a.points(), item.sessionMinutes(), progress, a.wikiUrl(),
					videoLib.bestGuideUrl(a.id(), a.name()), buildCaDetail(a, true, "")));
			}
			views.add(new SidePanelViewModel.SessionView(session.monster(), session.taskCount(),
				session.totalPoints(), session.totalMinutes(),
				(int) Math.round(session.pointsPerHour()), tasks));
		}
		return views;
	}

	/**
	 * A boss session's soft rec-stats sink: the points-weighted average of its tasks' sink factors (1.0 when
	 * the player meets every task's recommended stats). Sinks a bundle in proportion to how much of its
	 * value sits behind stats the player is short of.
	 */
	private static double sessionRecStatsSink(BossSession session, Map<Integer, Integer> shortfallById)
	{
		double weighted = 0;
		double points = 0;
		for (BossSession.Item item : session.items())
		{
			int pts = Math.max(0, item.task().points());
			int shortfall = shortfallById.getOrDefault(item.task().id(), 0);
			weighted += pts * RankedTask.recStatsSinkFactor(shortfall);
			points += pts;
		}
		return points <= 0 ? 1.0 : weighted / points;
	}

	/** Drops ranked tasks whose activity is flagged "Endgame access" (raids / coordinated group content). */
	private List<RankedTask> withoutGated(List<RankedTask> tasks)
	{
		List<RankedTask> out = new ArrayList<>();
		for (RankedTask rt : tasks)
		{
			if (!bossDifficultyLib.isEndgameAccess(rt.achievement().monster()))
			{
				out.add(rt);
			}
		}
		return out;
	}

	/** Same filter over raw tasks, for the training planner's view of what is worth opening up. */
	private List<CombatAchievement> withoutGatedTasks(List<CombatAchievement> tasks)
	{
		List<CombatAchievement> out = new ArrayList<>();
		for (CombatAchievement a : tasks)
		{
			if (!bossDifficultyLib.isEndgameAccess(a.monster()))
			{
				out.add(a);
			}
		}
		return out;
	}

	/**
	 * "Train next": skill goals that would open up CAs, for a player held back by levels. Empty for anyone
	 * who already meets what they need, so the section simply disappears for an established account.
	 */
	private List<SidePanelViewModel.TrainingView> buildTrainings(List<CombatAchievement> all,
		java.util.Set<Integer> completedIds)
	{
		List<SidePanelViewModel.TrainingView> views = new ArrayList<>();
		for (TrainingSuggestion s : new TrainingPlanner(skillXpLib)
			.plan(all, completedIds, effortLib, recStatsLib, profile, TRAININGS_LIMIT))
		{
			views.add(new SidePanelViewModel.TrainingView(s.label(), s.unlockedTaskCount(),
				s.unlockedPoints(), s.trainingMinutes(), s.unlocksHint(), s.isCalendarTime()));
		}
		return views;
	}

	/** "Unlock next" quest suggestions, best first, from the already-computed suggestions. */
	private List<SidePanelViewModel.UnlockView> buildUnlocks(List<UnlockSuggestion> suggestions)
	{
		List<SidePanelViewModel.UnlockView> views = new ArrayList<>();
		for (UnlockSuggestion s : suggestions)
		{
			if (views.size() >= UNLOCKS_LIMIT)
			{
				break;
			}
			// Shows what the quest OPENS, not what is doable the same day — a quest is permanent progress
			// and worth doing before you can use it. Reachability decides the ORDER (so a questline whose
			// CAs are forty levels away sinks) but never whether the suggestion appears: hiding them left a
			// new account with no "what quest next" advice at all, which is the one thing this answers.
			views.add(new SidePanelViewModel.UnlockView(s.questName(), s.difficulty(),
				s.unlockedTaskCount(), s.unlockedPoints(), s.totalMinutes(),
				String.join(", ", s.remainingPrerequisites()), String.join(", ", s.unmetSkills())));
		}
		return views;
	}

	private SidePanelViewModel.TaskRow toTaskRow(RankedTask rt)
	{
		CombatAchievement a = rt.achievement();
		return new SidePanelViewModel.TaskRow(a.id(), a.name(), a.description(), a.tier().displayName(),
			a.monster(), a.points(), rt.difficulty().difficulty(), rt.difficulty().reason(), rt.rationale(),
			rt.lockReason(), rt.doableNow(), rt.curated(),
			videoLib.hasCuratedGuide(a.id()), a.wikiUrl(), videoLib.bestGuideUrl(a.id(), a.name()),
			buildCaDetail(a, rt.doableNow(), rt.lockReason()));
	}

	private SidePanelViewModel.CaDetail buildCaDetail(CombatAchievement task, boolean doableNow,
		String lockReason)
	{
		return buildCaDetail(task, doableNow, lockReason, 0);
	}

	/** Assembles the full CA-detail view for a task: itemised requirements (met/unmet vs the player),
	 *  Difficulty breakdown, effort estimate, and the curated stats/setup/strategy/items. {@code
	 *  extraMinutes} is the amortised unlock/quest cost for locked CAs (0 for doable ones). */
	private SidePanelViewModel.CaDetail buildCaDetail(CombatAchievement task, boolean doableNow,
		String lockReason, int extraMinutes)
	{
		TaskEffortData effort = effortLib.effortFor(task.id());
		TaskDifficulty diff = difficultyLib.difficultyFor(task.id());
		TaskDetail det = detailLib.detailFor(task.id());
		BossTiming timing = timingLib.timingFor(task.monster());
		TaskTimeModel.Estimate est =
			timeModel().estimate(task, timing, experience, diff.difficulty(), diff.attemptsOverride());
		int mins = est.minutes();
		int pph = mins > 0 ? (int) Math.round(task.points() * 60.0 / mins) : 0;

		List<SidePanelViewModel.CaReq> reqs = new ArrayList<>();
		for (Map.Entry<String, Integer> e : effort.levelReqs().entrySet())
		{
			int have = profile.levelOf(e.getKey());
			boolean met = have >= e.getValue();
			reqs.add(new SidePanelViewModel.CaReq(e.getValue() + " " + e.getKey(), met,
				met ? "" : "you have " + have));
		}
		for (QuestRequirement qr : effort.questReqs())
		{
			if (qr.quest().isEmpty())
			{
				continue;
			}
			boolean met = qr.startedSuffices() ? profile.hasStarted(qr.quest()) : profile.hasCompleted(qr.quest());
			reqs.add(new SidePanelViewModel.CaReq(qr.quest(), met, ""));
		}

		// The same ready line the Route builds its path from, carried onto the card so lists can group by it.
		boolean withinReach = profile.worstShortfall(recStatsLib.softFor(task.id()))
			<= TrainingPlanner.VIABLE_WORST_GAP;

		return new SidePanelViewModel.CaDetail(task.id(), task.name(), task.monster(),
			task.tier().displayName(), task.type() == null ? "" : task.type().displayName(), task.points(),
			task.description(), doableNow, lockReason, diff.difficulty(), diff.bossDifficulty(), diff.bump(),
			diff.reason(), mins, pph, reqs, det.stats(), det.setup(), det.strategy(), det.items(),
			task.wikiUrl(), videoLib.bestGuideUrl(task.id(), task.name()), videoLib.hasCuratedGuide(task.id()),
			extraMinutes, withinReach);
	}
}
