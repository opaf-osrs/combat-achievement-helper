package com.pluginideahub.combatachievements;

import com.pluginideahub.combatachievements.core.achievement.CombatAchievementLibrary;
import com.pluginideahub.combatachievements.core.achievement.EffortDataLibrary;
import com.pluginideahub.combatachievements.core.achievement.TaskDifficultyLibrary;
import com.pluginideahub.combatachievements.core.ranking.EffortModel;
import com.pluginideahub.combatachievements.core.ranking.LowHangingFruitRanker;
import com.pluginideahub.combatachievements.core.ranking.RankedTask;
import com.pluginideahub.combatachievements.core.ranking.SignalsProvider;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Exports the per-task "easy heuristic" score (points ÷ heuristic effort × pure-skill difficulty)
 * for the curation spreadsheet, computed by the REAL {@link LowHangingFruitRanker} — the exact code
 * the Recommended/Quick wins ranking uses — so the sheet's heuristic column uses the same scoring
 * formula the plugin does (a given player's panel may still show different per-task numbers; see the
 * account-neutral note below).
 *
 * <p>Scores use account-neutral signals ({@link SignalsProvider#defaults()}: access/levels met, no
 * gear owned, boss not yet engaged), i.e. "if you could do this, how cheap is it?" — the right basis
 * for an account-agnostic spreadsheet. Writes {@code build/curation/heuristic_scores.csv}
 * ({@code id,heuristic_score}); {@code data/curation/apply-heuristic-column.py} merges it into
 * {@code tasks_master.csv}. Run with {@code ./gradlew curationExport}.</p>
 */
public final class CurationExport
{
	public static void main(String[] args) throws Exception
	{
		CombatAchievementLibrary lib = CombatAchievementLibrary.loadBundled();
		EffortDataLibrary effort = EffortDataLibrary.loadBundled();
		TaskDifficultyLibrary difficulty = TaskDifficultyLibrary.loadBundled();

		LowHangingFruitRanker ranker =
			new LowHangingFruitRanker(effort, EffortModel.standard(), difficulty);
		List<RankedTask> ranked = ranker.rank(lib.all(), Collections.<Integer>emptySet(),
			SignalsProvider.defaults(), false);
		// Stable, diffable order (the ranker returns score-desc); the spreadsheet joins by id anyway.
		ranked.sort(Comparator.comparingInt(rt -> rt.achievement().id()));

		File outDir = new File("build/curation");
		outDir.mkdirs();
		File out = new File(outDir, "heuristic_scores.csv");
		try (PrintWriter w = new PrintWriter(out, "UTF-8"))
		{
			w.println("id,heuristic_score");
			for (RankedTask rt : ranked)
			{
				w.println(rt.achievement().id() + ","
					+ String.format(Locale.ROOT, "%.4f", rt.score()));
			}
		}
		System.out.println("Wrote " + ranked.size() + " heuristic scores to " + out.getAbsolutePath());
	}

	private CurationExport()
	{
	}
}
