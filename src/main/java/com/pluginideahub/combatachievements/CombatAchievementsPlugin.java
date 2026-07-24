package com.pluginideahub.combatachievements;

import com.google.inject.Provides;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementDataException;
import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.bridge.AccountReader;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.RecStatsLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.achievement.TierRewardLibrary;
import com.pluginideahub.combatachievements.bridge.HiscoreKcReader;
import com.pluginideahub.combatachievements.bridge.KillCountTracker;
import com.pluginideahub.combatachievements.core.debug.DebugSimulation;
import com.pluginideahub.combatachievements.core.effort.BossTimingLibrary;
import com.pluginideahub.combatachievements.core.effort.CombatExperience;
import com.pluginideahub.combatachievements.core.effort.QuestEffortLibrary;
import com.pluginideahub.combatachievements.core.effort.ScalingLibrary;
import com.pluginideahub.combatachievements.core.effort.SkillXpLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ui.PanelAction;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModelBuilder;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import com.pluginideahub.combatachievements.varbit.CaVarbitIds;
import com.pluginideahub.combatachievements.varbit.CombatAchievementVarbitReader;
import javax.inject.Inject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Combat Achievement Helper",
	description = "Tracks Combat Achievements live, surfaces quick wins, plans the optimal path to your next tier, and links a guide video per task.",
	tags = {"combat", "achievements", "ca", "pvm", "progression", "tasks"}
)
public class CombatAchievementsPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CombatAchievementsPlugin.class);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private CombatAchievementsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private HiscoreClient hiscoreClient;

	private static final String KC_CONFIG_GROUP = "pluginideahub-combatachievements";
	private static final Gson GSON = new Gson();

	private CombatAchievementsPanel panel;
	/** Bumped by the Route refresh button so each press re-solves toward a different set (0 = the optimum). */
	private long routeShuffleSeed;
	/** Task ids the player barred from the Route; persisted per account so the choice survives restarts. */
	private final Set<Integer> barredTasks = new HashSet<>();
	/** Task ids pinned INTO the Route from a boss page; persisted per account alongside barred. */
	private final Set<Integer> pinnedTasks = new HashSet<>();
	private NavigationButton navigationButton;

	private CombatAchievementLibrary library;
	private EffortDataLibrary effortLibrary;
	private RecStatsLibrary recStatsLibrary;
	private VideoGuideLibrary videoLibrary;
	private GuideLibrary guideLibrary;
	private TaskDifficultyLibrary taskDifficultyLibrary;
	private TaskDetailLibrary taskDetailLibrary;
	private TierRewardLibrary tierRewardLibrary;
	private BossTimingLibrary bossTimingLibrary;
	private QuestEffortLibrary questEffortLibrary;
	private SkillXpLibrary skillXpLibrary;
	private ScalingLibrary scalingLibrary;
	private final CombatAchievementVarbitReader reader = new CombatAchievementVarbitReader();
	private final KillCountTracker killCountTracker = new KillCountTracker();

	private boolean dataError;
	private String dataErrorMessage = "";
	private long lastAccountHash = -1L;
	// Coalesces the flood of login/varbit refreshes into at most one rebuild per game tick (the
	// per-varbit full refresh was the login-lag culprit). Volatile: set from the async hiscore callback.
	private volatile boolean refreshPending;

	@Override
	protected void startUp()
	{
		dataError = false;
		try
		{
			library = CombatAchievementLibrary.loadBundled();
			effortLibrary = EffortDataLibrary.loadBundled();
			recStatsLibrary = RecStatsLibrary.loadBundled();
			videoLibrary = VideoGuideLibrary.loadBundled();
			guideLibrary = GuideLibrary.loadBundled();
			taskDifficultyLibrary = TaskDifficultyLibrary.loadBundled();
			taskDetailLibrary = TaskDetailLibrary.loadBundled();
			tierRewardLibrary = TierRewardLibrary.loadBundled();
			bossTimingLibrary = BossTimingLibrary.loadBundled();
			questEffortLibrary = QuestEffortLibrary.loadBundled();
			skillXpLibrary = SkillXpLibrary.loadBundled();
			scalingLibrary = ScalingLibrary.loadBundled();
			log.info("Combat Achievement Helper: loaded {} tasks (dataset {}), {} curated effort, {} curated videos, {} guides, {} difficulties, {} tier rewards",
				library.taskCount(), library.version(), effortLibrary.curatedCount(), videoLibrary.curatedCount(),
				guideLibrary.count(), taskDifficultyLibrary.count(), tierRewardLibrary.count());
		}
		catch (CombatAchievementDataException ex)
		{
			dataError = true;
			dataErrorMessage = "Combat Achievement Helper data failed to load: " + ex.getMessage();
			log.error("Combat Achievement Helper dataset failed to load", ex);
		}

		panel = new CombatAchievementsPanel(this::onAction);
		panel.applyTheme(config.panelTheme().palette());
		panel.setHowToDefault(config.showHowTo());
		panel.setDeveloperMode(config.developerMode());
		panel.setBarHandlers(this::barTask, this::unbarTask, this::clearBarredTasks);
		panel.setRouteHandlers(this::addToRoute, this::removeFromRoute);
		navigationButton = NavigationButton.builder()
			.tooltip("Combat Achievement Helper")
			.icon(ImageUtil.loadImageResource(CombatAchievementsPlugin.class, "icon.png"))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);

		refresh();
	}

	@Override
	protected void shutDown()
	{
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
		library = null;
		effortLibrary = null;
		videoLibrary = null;
		guideLibrary = null;
		taskDifficultyLibrary = null;
		taskDetailLibrary = null;
		tierRewardLibrary = null;
		bossTimingLibrary = null;
		questEffortLibrary = null;
		skillXpLibrary = null;
		killCountTracker.clear();
		lastAccountHash = -1L;
	}

	@Provides
	CombatAchievementsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatAchievementsConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (panel == null)
		{
			return;
		}
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			// Refresh on login (even pre-M3) so the player's skills are read for the doable-now filter.
			// Reset on account switch so we never show the previous account's progress (§5.6).
			long hash = client.getAccountHash();
			if (hash != lastAccountHash)
			{
				lastAccountHash = hash;
				loadKillCounts(hash);
				loadBarredTasks(hash);
				loadPinnedTasks(hash);
				backfillKillCountsFromHiscores();
			}
			requestRefresh();
		}
		else if (reader.isVerified() && (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING))
		{
			panel.render(SidePanelViewModel.loggedOut());
		}
	}

	/** Marks the view dirty; the actual rebuild happens at most once per tick in {@link #onGameTick}. */
	private void requestRefresh()
	{
		refreshPending = true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (refreshPending)
		{
			refreshPending = false;
			refresh();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Only a CA-completion varp changing can affect what we show. Ignoring every other varp change is
		// what stops the login flood (hundreds of unrelated varps) from triggering hundreds of rebuilds.
		if (reader.isVerified() && isLoggedIn() && CaVarbitIds.isCompletionVarp(event.getVarpId()))
		{
			requestRefresh();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Boss kill-count lines feed the experience tracker (partial progress + competence).
		if (killCountTracker.onMessage(event.getMessage()))
		{
			saveKillCounts();
			requestRefresh();
		}
	}

	/**
	 * Seeds historical per-boss KC from the OSRS HiScores (async network call), so competence and
	 * partial-progress work from kill 1 rather than only for kills observed while the plugin runs.
	 * Must be invoked on the client thread (reads the local player); the lookup itself is off-thread.
	 */
	private void backfillKillCountsFromHiscores()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		final String name = client.getLocalPlayer().getName();
		if (name == null || name.isEmpty())
		{
			return;
		}
		final HiscoreEndpoint endpoint = HiscoreEndpoint.fromWorldTypes(client.getWorldType());
		try
		{
			hiscoreClient.lookupAsync(name, endpoint).whenComplete((result, ex) ->
			{
				if (result == null || ex != null)
				{
					return;
				}
				killCountTracker.seed(HiscoreKcReader.read(result));
				saveKillCounts();
				requestRefresh();
			});
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: hiscore KC backfill failed", ex);
		}
	}

	/** Persists the accumulated KC map per account so competence/progress survive restarts. */
	private void saveKillCounts()
	{
		if (lastAccountHash == -1L)
		{
			return;
		}
		try
		{
			configManager.setConfiguration(KC_CONFIG_GROUP, kcKey(lastAccountHash),
				GSON.toJson(killCountTracker.asMap()));
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to persist kill counts", ex);
		}
	}

	private void loadKillCounts(long accountHash)
	{
		killCountTracker.clear();
		try
		{
			String json = configManager.getConfiguration(KC_CONFIG_GROUP, kcKey(accountHash));
			if (json != null && !json.isEmpty())
			{
				Map<String, Integer> map = GSON.fromJson(json,
					new TypeToken<HashMap<String, Integer>>() { }.getType());
				killCountTracker.load(map);
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to load kill counts", ex);
		}
	}

	private static String kcKey(long accountHash)
	{
		return "killcounts_" + accountHash;
	}

	private static String barredKey(long accountHash)
	{
		return "barred_" + accountHash;
	}

	private static String pinnedKey(long accountHash)
	{
		return "pinned_" + accountHash;
	}

	/** Pin a CA into the Route. Pinning also clears any bar on it — the two are opposites. */
	private void addToRoute(int taskId)
	{
		boolean changed = pinnedTasks.add(taskId);
		changed |= barredTasks.remove(taskId);
		if (changed)
		{
			saveBarredTasks();
			savePinnedTasks();
			refresh();
		}
	}

	/**
	 * Remove a CA from the Route. If it was pinned, simply un-pin it and let the solver decide again; if
	 * the solver chose it, bar it so the solver stops choosing it. Barring a merely-pinned task would
	 * otherwise be a surprise: undoing your own pin should not blacklist the task.
	 */
	private void removeFromRoute(int taskId)
	{
		if (pinnedTasks.remove(taskId))
		{
			savePinnedTasks();
		}
		else
		{
			barredTasks.add(taskId);
			saveBarredTasks();
		}
		refresh();
	}

	private void savePinnedTasks()
	{
		if (lastAccountHash == -1L)
		{
			return;
		}
		try
		{
			configManager.setConfiguration(KC_CONFIG_GROUP, pinnedKey(lastAccountHash),
				GSON.toJson(pinnedTasks));
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to persist pinned tasks", ex);
		}
	}

	private void loadPinnedTasks(long accountHash)
	{
		pinnedTasks.clear();
		try
		{
			String json = configManager.getConfiguration(KC_CONFIG_GROUP, pinnedKey(accountHash));
			if (json != null && !json.isEmpty())
			{
				Set<Integer> saved = GSON.fromJson(json, new TypeToken<HashSet<Integer>>() { }.getType());
				if (saved != null)
				{
					pinnedTasks.addAll(saved);
				}
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to load pinned tasks", ex);
		}
	}

	/** Bars a task from the Route and re-solves, so the next-best task takes the freed slot. */
	private void barTask(int taskId)
	{
		if (barredTasks.add(taskId))
		{
			saveBarredTasks();
			refresh();
		}
	}

	/** Puts a single barred task back in the running. */
	private void unbarTask(int taskId)
	{
		if (barredTasks.remove(taskId))
		{
			saveBarredTasks();
			refresh();
		}
	}

	private void clearBarredTasks()
	{
		if (!barredTasks.isEmpty())
		{
			barredTasks.clear();
			saveBarredTasks();
			refresh();
		}
	}

	private void saveBarredTasks()
	{
		if (lastAccountHash == -1L)
		{
			return;
		}
		try
		{
			configManager.setConfiguration(KC_CONFIG_GROUP, barredKey(lastAccountHash),
				GSON.toJson(barredTasks));
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to persist barred tasks", ex);
		}
	}

	private void loadBarredTasks(long accountHash)
	{
		barredTasks.clear();
		try
		{
			String json = configManager.getConfiguration(KC_CONFIG_GROUP, barredKey(accountHash));
			if (json != null && !json.isEmpty())
			{
				Set<Integer> saved = GSON.fromJson(json,
					new TypeToken<HashSet<Integer>>() { }.getType());
				if (saved != null)
				{
					barredTasks.addAll(saved);
				}
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Combat Achievement Helper: failed to load barred tasks", ex);
		}
	}

	private void onAction(PanelAction action)
	{
		if (action == PanelAction.REFRESH)
		{
			refresh();
		}
		else if (action == PanelAction.RESHUFFLE_ROUTE)
		{
			routeShuffleSeed++; // re-solve the Route toward a different but still-quick set
			refresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("pluginideahub-combatachievements".equals(event.getGroup()))
		{
			// Kill-count persistence writes to this same config group (killcounts_* keys); only re-apply the
			// panel settings on an actual settings change, not on every KC save. KC changes still refresh.
			String key = event.getKey();
			if (panel != null && (key == null || !key.startsWith("killcounts_")))
			{
				panel.applyTheme(config.panelTheme().palette());
				panel.setHowToDefault(config.showHowTo());
				panel.setDeveloperMode(config.developerMode());
			}
			requestRefresh();
		}
	}

	private void refresh()
	{
		if (panel == null)
		{
			return;
		}
		if (dataError)
		{
			panel.render(SidePanelViewModel.dataError(dataErrorMessage));
			return;
		}
		// Reading skills (and completion, when verified) must happen on the client thread.
		clientThread.invokeLater(this::refreshOnClientThread);
	}

	private void refreshOnClientThread()
	{
		if (panel == null || library == null)
		{
			return;
		}
		boolean loggedIn = isLoggedIn();

		ProgressSnapshot snapshot;
		if (reader.isVerified())
		{
			// Real completion read on the client thread; logged out → the "log in to sync" state.
			snapshot = loggedIn ? reader.read(client, library) : ProgressSnapshot.absent();
		}
		else
		{
			// Reader not confirmed (dev): a deterministic sample so the UI is reviewable.
			snapshot = SampleProgress.build(library);
		}

		// Account-aware signals: the player's live skill levels + quests vs each task's requirements.
		PlayerProfile profile = loggedIn ? AccountReader.readProfile(client) : PlayerProfile.empty();

		// Seed the dev spinners from the REAL levels, before any simulation is applied. Unconditional while
		// logged in: the panel itself ignores this whenever levels are being simulated, so gating it here on
		// the simulation being inactive only meant that ticking "no CAs completed" — which has nothing to do
		// with levels — froze the grid at whatever it last showed.
		if (loggedIn)
		{
			panel.setRealLevels(levelMap(profile));
		}

		// Developer mode only: pretend this is a different account. A pure value substitution on the two
		// things read above — it writes nothing, so no simulated figure can reach the account's saved kill
		// counts. Applied BEFORE the signals below, or the doable-now list would stay real while the
		// beginner rule and Train next went simulated. Returns none() whenever developer mode is off.
		DebugSimulation simulation = panel.debugSimulation();
		if (simulation.isActive())
		{
			profile = simulation.apply(profile);
			snapshot = simulation.apply(snapshot);
		}

		SignalsProvider signals = loggedIn
			? new ProfileSignalsProvider(effortLibrary, recStatsLibrary, profile)
			: SignalsProvider.defaults();
		CombatExperience experience = loggedIn ? killCountTracker.snapshot() : CombatExperience.empty();

		// The ranking/effort weights are no longer user-tunable — everything runs at the neutral defaults
		// (EffortModel.standard(), builder weights 1.0, trip overhead 6). The dials were removed from the
		// config panel; the engine still uses the figures, just not adjustable ones.
		SidePanelViewModelBuilder builder = new SidePanelViewModelBuilder(
			library, effortLibrary, videoLibrary, guideLibrary, tierRewardLibrary, EffortModel.standard())
			.difficulty(taskDifficultyLibrary)
			.recStats(recStatsLibrary)
			.bossDifficulty(com.pluginideahub.combatachievements.core.effort.BossDifficultyLibrary.loadBundled())
			.routeShuffle(routeShuffleSeed)
			.barred(barredTasks)
			.pinned(pinnedTasks)
			.detail(taskDetailLibrary)
			.scaling(scalingLibrary)
			.effortEngine(bossTimingLibrary, questEffortLibrary, skillXpLibrary, experience, profile, 6);
		SidePanelViewModel viewModel = builder.build(snapshot, signals, null);
		panel.render(viewModel);
	}

	private boolean isLoggedIn()
	{
		return client != null && client.getGameState() == GameState.LOGGED_IN;
	}

	/** The account's real levels, for seeding the developer-mode spinners. */
	private static Map<String, Integer> levelMap(PlayerProfile profile)
	{
		Map<String, Integer> levels = new HashMap<>();
		for (String skill : DebugSimulation.SKILLS)
		{
			levels.put(skill, profile.levelOf(skill));
		}
		return levels;
	}
}
