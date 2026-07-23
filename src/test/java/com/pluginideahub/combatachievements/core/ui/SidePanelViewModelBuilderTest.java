package com.pluginideahub.combatachievements.core.ui;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.guide.GuideLibrary;
import com.pluginideahub.combatachievements.core.progress.ProgressSnapshot;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.PlayerProfile;
import com.pluginideahub.combatachievements.core.ranking.ProfileSignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import com.pluginideahub.combatachievements.core.ranking.TaskLiveSignals;
import com.pluginideahub.combatachievements.core.video.VideoGuideLibrary;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end guard that the injected Difficulty library actually reaches the ranking THROUGH the
 * builder API — not just through the ranker/model constructors the unit tests use. Without this test,
 * the plugin's {@code .difficulty(...)} call (or the builder's ranker rebuild in {@code difficulty()})
 * could be removed and every other test would still pass, silently disconnecting the headline feature.
 */
public class SidePanelViewModelBuilderTest
{
	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}

	// Two incomplete, doable tasks: an easy Medium (2 pts) and a hard Grandmaster (6 pts). On raw
	// points/effort the 6-pt task wins; a difficulty library that rates it hard must flip the order.
	private static CombatAchievementLibrary twoTaskLib()
	{
		return CombatAchievementLibrary.load(stream("{\"tasks\":["
			+ "{\"id\":10,\"name\":\"Easy\",\"tier\":\"Medium\",\"type\":\"Kill Count\","
			+ "\"monster\":\"Boss\",\"description\":\"\"},"
			+ "{\"id\":11,\"name\":\"Hard\",\"tier\":\"Grandmaster\",\"type\":\"Kill Count\","
			+ "\"monster\":\"Raid\",\"description\":\"\"}"
			+ "]}"));
	}

	private static SidePanelViewModelBuilder builder(CombatAchievementLibrary lib)
	{
		return new SidePanelViewModelBuilder(lib, EffortDataLibrary.empty(), VideoGuideLibrary.empty(),
			GuideLibrary.empty(), EffortModel.standard());
	}

	private static SidePanelViewModel build(SidePanelViewModelBuilder b)
	{
		ProgressSnapshot present = new ProgressSnapshot(Collections.<Integer>emptySet(), 0, 0, null, 0L);
		SignalsProvider allDoable = id -> new TaskLiveSignals(true, true, false, true);
		return b.build(present, allDoable, null);
	}

	@Test
	public void caDetailIsAssembledAndCarriesCuratedText()
	{
		// A doable task with curated setup/strategy: the drill-in detail must be attached to its row,
		// with the curated text and a breadcrumb type/monster.
		CombatAchievementLibrary lib = twoTaskLib();
		com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary detail =
			com.pluginideahub.combatachievements.core.achievement.TaskDetailLibrary.load(
				stream("{\"tasks\":{\"10\":{\"setup\":\"Bring a rock hammer\",\"strategy\":\"Smash it\"}}}"));

		SidePanelViewModel vm = build(builder(lib).detail(detail));

		SidePanelViewModel.TaskRow row = null;
		for (SidePanelViewModel.TaskRow r : vm.quickWins())
		{
			if (r.id == 10)
			{
				row = r;
				break;
			}
		}
		assertNotNull("row present", row);
		assertNotNull("detail attached to the row", row.detail);
		assertEquals("Bring a rock hammer", row.detail.setup);
		assertEquals("Smash it", row.detail.strategy);
		assertEquals("Boss", row.detail.monster);
		assertEquals("Kill Count", row.detail.type);
	}

	@Test
	public void bossFullyGatedByAQuestShowsLockedWithTheQuest()
	{
		// Vorkath's only CA is gated by a quest the player has NOT done → the boss appears in the
		// directory flagged locked, with the CA carrying the specific "needs Dragon Slayer II" reason.
		CombatAchievementLibrary lib = CombatAchievementLibrary.load(stream("{\"tasks\":["
			+ "{\"id\":10,\"name\":\"Vorkath Kill\",\"tier\":\"Elite\",\"type\":\"Kill Count\","
			+ "\"monster\":\"Vorkath\",\"description\":\"\"}]}"));
		EffortDataLibrary el = EffortDataLibrary.load(
			stream("{\"tasks\":{\"10\":{\"questReqs\":[\"Dragon Slayer II\"]}}}"));
		SignalsProvider signals = new ProfileSignalsProvider(el, PlayerProfile.empty()); // quest not done
		ProgressSnapshot present = new ProgressSnapshot(Collections.<Integer>emptySet(), 0, 0, null, 0L);

		SidePanelViewModel vm = new SidePanelViewModelBuilder(lib, el, VideoGuideLibrary.empty(),
			GuideLibrary.empty(), EffortModel.standard()).build(present, signals, null);

		SidePanelViewModel.BossRow vork = null;
		for (SidePanelViewModel.BossRow b : vm.bosses())
		{
			if ("Vorkath".equals(b.monster))
			{
				vork = b;
				break;
			}
		}
		assertNotNull("Vorkath appears in the Bosses directory", vork);
		assertTrue("a fully quest-gated boss is flagged locked", vork.locked);
		assertEquals("no doable CAs", 0, vork.doableCount);
		assertEquals("its one CA is locked", 1, vork.lockedCount);
		assertEquals("needs Dragon Slayer II", vork.lockedCas.get(0).lockReason);
	}

	@Test
	public void bossesModeCountsOnlyIncompleteCas()
	{
		// Two CAs at the same boss; one is already completed. The Bosses (sessions) view must exclude the
		// completed CA from both the CA count and the projected points.
		CombatAchievementLibrary lib = CombatAchievementLibrary.load(stream("{\"tasks\":["
			+ "{\"id\":10,\"name\":\"Done\",\"tier\":\"Medium\",\"type\":\"Kill Count\","
			+ "\"monster\":\"Boss\",\"description\":\"\"},"
			+ "{\"id\":11,\"name\":\"Todo\",\"tier\":\"Medium\",\"type\":\"Kill Count\","
			+ "\"monster\":\"Boss\",\"description\":\"\"}"
			+ "]}"));
		SignalsProvider allDoable = id -> new TaskLiveSignals(true, true, false, true);
		// id 10 is already completed; only id 11 remains.
		ProgressSnapshot snap = new ProgressSnapshot(Collections.singleton(10), 0, 0, null, 0L);

		SidePanelViewModel vm = builder(lib).build(snap, allDoable, null);

		SidePanelViewModel.SessionView boss = null;
		for (SidePanelViewModel.SessionView s : vm.sessions())
		{
			if ("Boss".equals(s.monster))
			{
				boss = s;
				break;
			}
		}
		assertNotNull("the boss should still appear (it has an incomplete CA)", boss);
		assertEquals("only the 1 incomplete CA is counted", 1, boss.taskCount);
		assertEquals("projected points exclude the completed CA", 2, boss.totalPoints);
		for (SidePanelViewModel.SessionTaskView t : boss.tasks)
		{
			assertTrue("the completed CA (id 10) must not be listed", t.id != 10);
		}
	}

	@Test
	public void injectedDifficultyLibraryChangesQuickWinsOrderThroughTheBuilder()
	{
		CombatAchievementLibrary lib = twoTaskLib();

		// Without difficulty data, the 6-pt Grandmaster task leads on raw points/effort.
		SidePanelViewModel noDifficulty = build(builder(lib));
		assertEquals("no difficulty -> higher-points task first", 11, noDifficulty.quickWins().get(0).id);

		// Injecting a difficulty library that rates the Grandmaster task hard (9) and the Medium easy (2)
		// must flip the order — proving .difficulty() is genuinely wired into the built ranker.
		TaskDifficultyLibrary diff = TaskDifficultyLibrary.load(
			stream("{\"tasks\":{\"10\":{\"difficulty\":2},\"11\":{\"difficulty\":9}}}"));
		SidePanelViewModel withDifficulty = build(builder(lib).difficulty(diff));
		assertEquals("difficulty wired -> easy task first", 10, withDifficulty.quickWins().get(0).id);
	}

	@Test
	public void rankingWeightsThroughTheBuilderTuneThePointsAndDifficultyBalance()
	{
		CombatAchievementLibrary lib = twoTaskLib(); // 10 = easy 2pt, 11 = hard 6pt
		TaskDifficultyLibrary diff = TaskDifficultyLibrary.load(
			stream("{\"tasks\":{\"10\":{\"difficulty\":2},\"11\":{\"difficulty\":9}}}"));

		// Neutral weights: the hard-but-high-points task sinks below the easy one (difficulty wins).
		assertEquals("neutral -> easy task first",
			10, build(builder(lib).difficulty(diff).rankingWeights(1.0, 1.0)).quickWins().get(0).id);

		// Difficulty weight 0 ignores difficulty, so the 6-pt task leads again on points.
		assertEquals("difficulty weight 0 -> high-point task first",
			11, build(builder(lib).difficulty(diff).rankingWeights(1.0, 0.0)).quickWins().get(0).id);

		// A strong points weight overpowers the difficulty penalty and re-floats the 6-pt task.
		assertEquals("high points weight -> high-point task first",
			11, build(builder(lib).difficulty(diff).rankingWeights(3.0, 1.0)).quickWins().get(0).id);
	}
}
