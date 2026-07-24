package com.pluginideahub.combatachievements.core.ui;

import java.util.Collections;
import java.util.List;

/**
 * Immutable, render-ready model for the side panel. The Swing panel reads this and never touches the
 * game client. Built by {@link SidePanelViewModelBuilder}. Mirrors roguescape's
 * {@code core.ui.SidePanelViewModel} role.
 */
public final class SidePanelViewModel
{
	public enum State
	{
		LOGGED_OUT, DATA_ERROR, READY
	}

	/** Per-tier progress row. */
	public static final class TierRow
	{
		public final String name;
		public final int completed;
		public final int total;
		public final int earnedPoints;
		public final int totalPoints;
		public final int threshold;
		public final boolean unlocked;
		public final int pointsRemaining;
		public final boolean fullyComplete;
		/** The iconic unlock for completing this tier (e.g. "Ghommal's hilt (2)"); "" when unknown. */
		public final String rewardHeadline;
		/** Fuller list of this tier's unlocks (tooltip/detail); empty when unknown. */
		public final List<String> rewards;

		public TierRow(String name, int completed, int total, int earnedPoints, int totalPoints,
			int threshold, boolean unlocked, int pointsRemaining, boolean fullyComplete,
			String rewardHeadline, List<String> rewards)
		{
			this.name = name;
			this.completed = completed;
			this.total = total;
			this.earnedPoints = earnedPoints;
			this.totalPoints = totalPoints;
			this.threshold = threshold;
			this.unlocked = unlocked;
			this.pointsRemaining = pointsRemaining;
			this.fullyComplete = fullyComplete;
			this.rewardHeadline = rewardHeadline == null ? "" : rewardHeadline;
			this.rewards = Collections.unmodifiableList(
				rewards == null ? Collections.emptyList() : rewards);
		}
	}

	/** One requirement line in the CA detail (a skill or quest gate), with whether the player meets it. */
	public static final class CaReq
	{
		public final String label;
		public final boolean met;
		/** Extra note, e.g. "you have 88"; "" when none. */
		public final String note;

		public CaReq(String label, boolean met, String note)
		{
			this.label = label == null ? "" : label;
			this.met = met;
			this.note = note == null ? "" : note;
		}
	}

	/**
	 * The lean, self-contained CA-detail view: breadcrumb (boss · tier · pts · type), description,
	 * itemised requirements, Difficulty breakdown, effort, and the curated how-to. Rendered when a CA
	 * card is clicked from anywhere (CAs list, Boss detail, Route).
	 */
	public static final class CaDetail
	{
		public final int id;
		public final String name;
		public final String monster;
		public final String tierName;
		public final String type;
		public final int points;
		public final String description;
		public final boolean doableNow;
		public final String lockReason;
		public final int difficulty;
		public final int bossDifficulty;
		public final double bump;
		public final String difficultyReason;
		public final int estMinutes;
		public final int pointsPerHour;
		public final List<CaReq> requirements;
		public final String stats;
		public final String setup;
		public final String strategy;
		public final String items;
		public final String wikiUrl;
		public final String guideUrl;
		public final boolean curatedVideo;
		/** Extra minutes to make this CA doable (amortised unlock/quest time); 0 for already-doable CAs. */
		public final int extraMinutes;
		/**
		 * Whether the player is close enough to this CA's recommended stats to attempt it. Nothing is hidden
		 * on it — the panel groups by it, so a list can separate what you could go and do from what is
		 * technically ungated but forty levels away.
		 */
		public final boolean withinReach;

		public CaDetail(int id, String name, String monster, String tierName, String type, int points,
			String description, boolean doableNow, String lockReason, int difficulty, int bossDifficulty,
			double bump, String difficultyReason, int estMinutes, int pointsPerHour, List<CaReq> requirements,
			String stats, String setup, String strategy, String items, String wikiUrl, String guideUrl,
			boolean curatedVideo, int extraMinutes)
		{
			this(id, name, monster, tierName, type, points, description, doableNow, lockReason, difficulty,
				bossDifficulty, bump, difficultyReason, estMinutes, pointsPerHour, requirements, stats,
				setup, strategy, items, wikiUrl, guideUrl, curatedVideo, extraMinutes, true);
		}

		public CaDetail(int id, String name, String monster, String tierName, String type, int points,
			String description, boolean doableNow, String lockReason, int difficulty, int bossDifficulty,
			double bump, String difficultyReason, int estMinutes, int pointsPerHour, List<CaReq> requirements,
			String stats, String setup, String strategy, String items, String wikiUrl, String guideUrl,
			boolean curatedVideo, int extraMinutes, boolean withinReach)
		{
			this.withinReach = withinReach;
			this.id = id;
			this.name = name == null ? "" : name;
			this.monster = monster == null ? "" : monster;
			this.tierName = tierName == null ? "" : tierName;
			this.type = type == null ? "" : type;
			this.points = points;
			this.description = description == null ? "" : description;
			this.doableNow = doableNow;
			this.lockReason = lockReason == null ? "" : lockReason;
			this.difficulty = difficulty;
			this.bossDifficulty = bossDifficulty;
			this.bump = bump;
			this.difficultyReason = difficultyReason == null ? "" : difficultyReason;
			this.estMinutes = estMinutes;
			this.pointsPerHour = pointsPerHour;
			this.requirements = Collections.unmodifiableList(
				requirements == null ? Collections.emptyList() : requirements);
			this.stats = stats == null ? "" : stats;
			this.setup = setup == null ? "" : setup;
			this.strategy = strategy == null ? "" : strategy;
			this.items = items == null ? "" : items;
			this.wikiUrl = wikiUrl == null ? "" : wikiUrl;
			this.guideUrl = guideUrl == null ? "" : guideUrl;
			this.curatedVideo = curatedVideo;
			this.extraMinutes = Math.max(0, extraMinutes);
		}

		/** Total minutes to actually get this CA: its own time plus any unlock/quest cost. */
		public int totalMinutes()
		{
			return estMinutes + extraMinutes;
		}
	}

	/** A task row in the quick-wins list. */
	public static final class TaskRow
	{
		public final int id;
		public final String name;
		public final String description;
		public final String tierName;
		/** The boss/activity this CA is performed at ("" for non-boss tasks). Drives the By-boss sort. */
		public final String monster;
		public final int points;
		/** Pure-skill Difficulty (1–10); 0 when unknown. */
		public final int difficulty;
		/** Short tag for the Difficulty breakdown (e.g. "no-damage", "speed", "kill count"); "" if none. */
		public final String difficultyReason;
		public final String rationale;
		/** Short reason the task is not doable now (e.g. "needs Dragon Slayer II"); "" when doable. */
		public final String lockReason;
		public final boolean doableNow;
		public final boolean curated;
		public final boolean curatedVideo;
		public final String wikiUrl;
		public final String guideUrl;
		/** Full detail for the drill-in view; never null. */
		public final CaDetail detail;

		public TaskRow(int id, String name, String description, String tierName, String monster, int points,
			int difficulty, String difficultyReason, String rationale, String lockReason,
			boolean doableNow, boolean curated, boolean curatedVideo, String wikiUrl, String guideUrl,
			CaDetail detail)
		{
			this.id = id;
			this.name = name;
			this.description = description;
			this.tierName = tierName;
			this.monster = monster == null ? "" : monster;
			this.points = points;
			this.difficulty = difficulty;
			this.difficultyReason = difficultyReason == null ? "" : difficultyReason;
			this.rationale = rationale;
			this.lockReason = lockReason == null ? "" : lockReason;
			this.doableNow = doableNow;
			this.curated = curated;
			this.curatedVideo = curatedVideo;
			this.wikiUrl = wikiUrl;
			this.guideUrl = guideUrl;
			this.detail = detail;
		}
	}

	/** One step of the optimal-path view. */
	public static final class PathRow
	{
		public final int id;
		public final String name;
		public final String tierName;
		public final int points;
		public final int cumulativePoints;
		public final String wikiUrl;
		public final String guideUrl;
		/** Full detail for the drill-in view; never null. */
		public final CaDetail detail;

		public PathRow(int id, String name, String tierName, int points, int cumulativePoints,
			String wikiUrl, String guideUrl, CaDetail detail)
		{
			this.id = id;
			this.name = name;
			this.tierName = tierName;
			this.points = points;
			this.cumulativePoints = cumulativePoints;
			this.wikiUrl = wikiUrl;
			this.guideUrl = guideUrl;
			this.detail = detail;
		}
	}

	/** The optimal-path-to-next-tier view. */
	public static final class PathView
	{
		public final String targetTierName;
		public final int pointsGap;
		public final boolean reachable;
		public final boolean alreadyUnlocked;
		public final List<PathRow> steps;
		/** The reward unlocked on reaching the target tier (e.g. "Ghommal's hilt (2)"); "" when unknown. */
		public final String targetRewardHeadline;
		/** CAs the recommended unlocks would open — shown locked/red in the Route list, still clickable. */
		public final List<CaDetail> lockedCas;
		/**
		 * The route stops short because the player is not ready for enough content — not because the game
		 * has run out of tasks. The remedy is training, so the panel points at "Train next" instead of
		 * padding the route with content the account cannot realistically do.
		 */
		public final boolean trainFirst;

		/** Points the listed CAs add up to — what following the whole visible route is worth. */
		public int shownPoints()
		{
			int sum = 0;
			for (PathRow r : steps)
			{
				sum += r.points;
			}
			return sum;
		}

		/** CAs the player has barred from the Route, so they can be reviewed and put back. */
		public final List<CaDetail> barredCas;

		public PathView(String targetTierName, int pointsGap, boolean reachable,
			boolean alreadyUnlocked, List<PathRow> steps, String targetRewardHeadline,
			List<CaDetail> lockedCas, boolean trainFirst)
		{
			this(targetTierName, pointsGap, reachable, alreadyUnlocked, steps, targetRewardHeadline,
				lockedCas, trainFirst, Collections.emptyList());
		}

		public PathView(String targetTierName, int pointsGap, boolean reachable,
			boolean alreadyUnlocked, List<PathRow> steps, String targetRewardHeadline,
			List<CaDetail> lockedCas, boolean trainFirst, List<CaDetail> barredCas)
		{
			this.barredCas = Collections.unmodifiableList(
				barredCas == null ? Collections.emptyList() : barredCas);
			this.targetTierName = targetTierName;
			this.pointsGap = pointsGap;
			this.reachable = reachable;
			this.alreadyUnlocked = alreadyUnlocked;
			this.trainFirst = trainFirst;
			this.steps = Collections.unmodifiableList(steps);
			this.targetRewardHeadline = targetRewardHeadline == null ? "" : targetRewardHeadline;
			this.lockedCas = Collections.unmodifiableList(
				lockedCas == null ? Collections.emptyList() : lockedCas);
		}
	}

	/** An authored route guide, with its live route to the target tier resolved (when applicable). */
	public static final class GuideView
	{
		public final String id;
		public final String title;
		public final String author;
		public final String summary;
		public final String videoUrl;
		public final String targetTierName;
		public final List<String> tags;
		public final List<String> tips;
		public final PathView route;

		public GuideView(String id, String title, String author, String summary, String videoUrl,
			String targetTierName, List<String> tags, List<String> tips, PathView route)
		{
			this.id = id;
			this.title = title;
			this.author = author;
			this.summary = summary;
			this.videoUrl = videoUrl;
			this.targetTierName = targetTierName;
			this.tags = Collections.unmodifiableList(tags);
			this.tips = Collections.unmodifiableList(tips);
			this.route = route;
		}

		public boolean hasVideo()
		{
			return videoUrl != null && !videoUrl.isEmpty();
		}
	}

	/** One CA within a recommended boss session. */
	public static final class SessionTaskView
	{
		public final int id;
		public final String name;
		public final String tierName;
		public final int points;
		public final int estMinutes;
		/** Partial-progress note for multi-kill tasks, e.g. "31 / 50 done"; "" otherwise. */
		public final String progress;
		public final String wikiUrl;
		public final String guideUrl;
		/** Full detail for the drill-in view; never null. */
		public final CaDetail detail;

		public SessionTaskView(int id, String name, String tierName, int points, int estMinutes,
			String progress, String wikiUrl, String guideUrl, CaDetail detail)
		{
			this.id = id;
			this.name = name;
			this.tierName = tierName;
			this.points = points;
			this.estMinutes = estMinutes;
			this.progress = progress == null ? "" : progress;
			this.wikiUrl = wikiUrl;
			this.guideUrl = guideUrl;
			this.detail = detail;
		}
	}

	/** A recommended one-trip boss session: do these CAs here, in this order. */
	public static final class SessionView
	{
		public final String monster;
		public final int taskCount;
		public final int totalPoints;
		public final int totalMinutes;
		/** Points per hour for the whole trip — this tab's headline ranking metric. */
		public final int pointsPerHour;
		public final List<SessionTaskView> tasks;

		public SessionView(String monster, int taskCount, int totalPoints, int totalMinutes,
			int pointsPerHour, List<SessionTaskView> tasks)
		{
			this.monster = monster;
			this.taskCount = taskCount;
			this.totalPoints = totalPoints;
			this.totalMinutes = totalMinutes;
			this.pointsPerHour = pointsPerHour;
			this.tasks = Collections.unmodifiableList(tasks);
		}
	}

	/** One boss in the browsable Bosses directory: its incomplete CAs split into doable + locked. */
	public static final class BossRow
	{
		public final String monster;
		/** Points from the boss's doable-now incomplete CAs (the realistic prize for going now). */
		public final int projectedPoints;
		public final int doableCount;
		public final int lockedCount;
		/** True when the boss has no doable CAs — the whole boss is gated (shown greyed). */
		public final boolean locked;
		/** Recommended stats from a representative CA; "" if none. */
		public final String recommendedStats;
		public final List<CaDetail> doable;
		public final List<CaDetail> lockedCas;
		/**
		 * How far off this boss's doable CAs are from the player's recommended stats (1.0 = ready, higher =
		 * further away). Used ONLY by the opinionated "Recommended" sort — the directory's own order and the
		 * "Most points" sort stay an honest raw listing.
		 */
		public final double readinessSink;

		public BossRow(String monster, int projectedPoints, int doableCount, int lockedCount,
			boolean locked, String recommendedStats, List<CaDetail> doable, List<CaDetail> lockedCas)
		{
			this(monster, projectedPoints, doableCount, lockedCount, locked, recommendedStats, doable,
				lockedCas, 1.0);
		}

		public BossRow(String monster, int projectedPoints, int doableCount, int lockedCount,
			boolean locked, String recommendedStats, List<CaDetail> doable, List<CaDetail> lockedCas,
			double readinessSink)
		{
			this.readinessSink = Math.max(1.0, readinessSink);
			this.monster = monster == null ? "" : monster;
			this.projectedPoints = projectedPoints;
			this.doableCount = doableCount;
			this.lockedCount = lockedCount;
			this.locked = locked;
			this.recommendedStats = recommendedStats == null ? "" : recommendedStats;
			this.doable = Collections.unmodifiableList(doable == null ? Collections.emptyList() : doable);
			this.lockedCas = Collections.unmodifiableList(
				lockedCas == null ? Collections.emptyList() : lockedCas);
		}
	}

	/** An "unlock next" suggestion: a quest worth doing for the CAs it opens up. */
	/** A "train this to open up CAs" goal — the skilling counterpart to {@link UnlockView}. */
	public static final class TrainingView
	{
		/** e.g. "Fishing 35" or "All combat 20". */
		public final String label;
		public final int unlockedTaskCount;
		public final int unlockedPoints;
		public final int trainingMinutes;
		/** The boss/activity this mostly opens up, e.g. "Tempoross"; "" when spread across many. */
		public final String unlocksHint;
		/**
		 * True when {@link #trainingMinutes} is elapsed calendar time on a daily-gated skill (Farming),
		 * not time spent playing — those read in days, everything else in hours.
		 */
		public final boolean calendarTime;

		public TrainingView(String label, int unlockedTaskCount, int unlockedPoints, int trainingMinutes,
			String unlocksHint)
		{
			this(label, unlockedTaskCount, unlockedPoints, trainingMinutes, unlocksHint, false);
		}

		public TrainingView(String label, int unlockedTaskCount, int unlockedPoints, int trainingMinutes,
			String unlocksHint, boolean calendarTime)
		{
			this.calendarTime = calendarTime;
			this.label = label == null ? "" : label;
			this.unlockedTaskCount = unlockedTaskCount;
			this.unlockedPoints = unlockedPoints;
			this.trainingMinutes = trainingMinutes;
			this.unlocksHint = unlocksHint == null ? "" : unlocksHint;
		}
	}

	public static final class UnlockView
	{
		public final String questName;
		public final String difficulty;
		public final int unlockedTaskCount;
		public final int unlockedPoints;
		public final int totalMinutes;
		/** Remaining prerequisite quests, comma-joined; "" if none. */
		public final String prerequisites;
		/** Skills still short for the chain, comma-joined; "" if none. */
		public final String unmetSkills;

		public UnlockView(String questName, String difficulty, int unlockedTaskCount,
			int unlockedPoints, int totalMinutes, String prerequisites, String unmetSkills)
		{
			this.questName = questName;
			this.difficulty = difficulty;
			this.unlockedTaskCount = unlockedTaskCount;
			this.unlockedPoints = unlockedPoints;
			this.totalMinutes = totalMinutes;
			this.prerequisites = prerequisites == null ? "" : prerequisites;
			this.unmetSkills = unmetSkills == null ? "" : unmetSkills;
		}
	}

	private final State state;
	private final String message;
	private final String datasetVersion;
	private final int gamePoints;
	private final int totalAvailablePoints;
	private final int completedCount;
	private final int totalTasks;
	private final String currentTierName;
	private final String nextTierName;
	private final int nextTierPointsNeeded;
	private final boolean allUnlocked;
	private final boolean datasetStale;
	private final List<TierRow> tierRows;
	private final List<TaskRow> quickWins;
	private final PathView path;
	private final List<GuideView> guides;
	private final List<SessionView> sessions;
	private final List<UnlockView> unlocks;
	private final List<TrainingView> trainings;
	private final List<BossRow> bosses;
	private final boolean sampleData;

	private SidePanelViewModel(State state, String message, String datasetVersion, int gamePoints,
		int totalAvailablePoints, int completedCount, int totalTasks, String currentTierName,
		String nextTierName, int nextTierPointsNeeded, boolean allUnlocked, boolean datasetStale,
		List<TierRow> tierRows, List<TaskRow> quickWins, PathView path, List<GuideView> guides,
		List<SessionView> sessions, List<UnlockView> unlocks, List<TrainingView> trainings,
		List<BossRow> bosses, boolean sampleData)
	{
		this.state = state;
		this.message = message;
		this.datasetVersion = datasetVersion;
		this.gamePoints = gamePoints;
		this.totalAvailablePoints = totalAvailablePoints;
		this.completedCount = completedCount;
		this.totalTasks = totalTasks;
		this.currentTierName = currentTierName;
		this.nextTierName = nextTierName;
		this.nextTierPointsNeeded = nextTierPointsNeeded;
		this.allUnlocked = allUnlocked;
		this.datasetStale = datasetStale;
		this.tierRows = Collections.unmodifiableList(tierRows);
		this.quickWins = Collections.unmodifiableList(quickWins);
		this.path = path;
		this.guides = Collections.unmodifiableList(guides);
		this.sessions = Collections.unmodifiableList(sessions);
		this.unlocks = Collections.unmodifiableList(unlocks);
		this.trainings = Collections.unmodifiableList(trainings);
		this.bosses = Collections.unmodifiableList(bosses);
		this.sampleData = sampleData;
	}

	public static SidePanelViewModel loggedOut()
	{
		return loggedOut(Collections.emptyList());
	}

	public static SidePanelViewModel loggedOut(List<GuideView> guides)
	{
		return new SidePanelViewModel(State.LOGGED_OUT, "Log in to sync your Combat Achievements.",
			"", 0, 0, 0, 0, "None", "", 0, false, false,
			Collections.emptyList(), Collections.emptyList(), null, guides,
			Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(), false);
	}

	public static SidePanelViewModel dataError(String reason)
	{
		return new SidePanelViewModel(State.DATA_ERROR, reason, "", 0, 0, 0, 0, "None", "", 0,
			false, false, Collections.emptyList(), Collections.emptyList(), null,
			Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList(), false);
	}

	public static SidePanelViewModel ready(String datasetVersion, int gamePoints,
		int totalAvailablePoints, int completedCount, int totalTasks, String currentTierName,
		String nextTierName, int nextTierPointsNeeded, boolean allUnlocked, boolean datasetStale,
		List<TierRow> tierRows, List<TaskRow> quickWins, PathView path, List<GuideView> guides,
		List<SessionView> sessions, List<UnlockView> unlocks, List<BossRow> bosses, boolean sampleData)
	{
		return ready(datasetVersion, gamePoints, totalAvailablePoints, completedCount, totalTasks,
			currentTierName, nextTierName, nextTierPointsNeeded, allUnlocked, datasetStale, tierRows,
			quickWins, path, guides, sessions, unlocks, Collections.emptyList(), bosses, sampleData);
	}

	public static SidePanelViewModel ready(String datasetVersion, int gamePoints,
		int totalAvailablePoints, int completedCount, int totalTasks, String currentTierName,
		String nextTierName, int nextTierPointsNeeded, boolean allUnlocked, boolean datasetStale,
		List<TierRow> tierRows, List<TaskRow> quickWins, PathView path, List<GuideView> guides,
		List<SessionView> sessions, List<UnlockView> unlocks, List<TrainingView> trainings,
		List<BossRow> bosses, boolean sampleData)
	{
		return new SidePanelViewModel(State.READY, "", datasetVersion, gamePoints,
			totalAvailablePoints, completedCount, totalTasks, currentTierName, nextTierName,
			nextTierPointsNeeded, allUnlocked, datasetStale, tierRows, quickWins, path, guides,
			sessions, unlocks, trainings, bosses,
			sampleData);
	}

	public State state()
	{
		return state;
	}

	public String message()
	{
		return message;
	}

	public String datasetVersion()
	{
		return datasetVersion;
	}

	public int gamePoints()
	{
		return gamePoints;
	}

	public int totalAvailablePoints()
	{
		return totalAvailablePoints;
	}

	public int completedCount()
	{
		return completedCount;
	}

	public int totalTasks()
	{
		return totalTasks;
	}

	public String currentTierName()
	{
		return currentTierName;
	}

	public String nextTierName()
	{
		return nextTierName;
	}

	public int nextTierPointsNeeded()
	{
		return nextTierPointsNeeded;
	}

	public boolean allUnlocked()
	{
		return allUnlocked;
	}

	public boolean datasetStale()
	{
		return datasetStale;
	}

	public List<TierRow> tierRows()
	{
		return tierRows;
	}

	public List<TaskRow> quickWins()
	{
		return quickWins;
	}

	public PathView path()
	{
		return path;
	}

	public List<GuideView> guides()
	{
		return guides;
	}

	/** Recommended boss sessions (synergy-ranked bundles), best first. */
	public List<SessionView> sessions()
	{
		return sessions;
	}

	/** "Unlock next" quest suggestions, best first. */
	/** "Train next" goals — empty for an account that isn't held back by levels. */
	public List<TrainingView> trainings()
	{
		return trainings;
	}

	public List<UnlockView> unlocks()
	{
		return unlocks;
	}

	/** The full browsable Bosses directory (every boss with an incomplete CA), best first. */
	public List<BossRow> bosses()
	{
		return bosses;
	}

	/** True when the figures shown are placeholder sample data, not the player's real account. */
	public boolean sampleData()
	{
		return sampleData;
	}
}
