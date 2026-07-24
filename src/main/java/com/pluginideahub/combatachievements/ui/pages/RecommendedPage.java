package com.pluginideahub.combatachievements.ui.pages;

import com.pluginideahub.combatachievements.CombatAchievementsPanel;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;
import com.pluginideahub.combatachievements.ui.CardKit;
import com.pluginideahub.combatachievements.ui.CombatAchievementsTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** Renders the Recommended mode: the searched, sorted, shuffleable list of doable-now CA cards. */
public class RecommendedPage
{
	/** Cap on cards rendered at once in the (now full) CAs list; refine the search to see more. */
	private static final int RENDER_CAP = 60;
	/** Max rank positions a CA can move when the CAs list is shuffled — big enough to surface a different
	 *  set, small enough to keep good CAs near the top. */
	private static final double SHUFFLE_JITTER = 12.0;

	private final CombatAchievementsPanel panel;
	private final JPanel content;

	public RecommendedPage(CombatAchievementsPanel panel)
	{
		this.panel = panel;
		this.content = panel.content();
	}

	private static boolean matches(SidePanelViewModel.TaskRow r, String q)
	{
		return q.isEmpty()
			|| r.name.toLowerCase(Locale.ROOT).contains(q)
			|| r.monster.toLowerCase(Locale.ROOT).contains(q);
	}

	public void buildRecommended()
	{
		String q = panel.searchText();
		List<SidePanelViewModel.TaskRow> rows = new ArrayList<>();
		for (SidePanelViewModel.TaskRow r : panel.model().quickWins())
		{
			if (r.doableNow && matches(r, q)) // Recommended is strictly doable-now
			{
				rows.add(r);
			}
		}
		sortRows(rows);
		if (panel.shuffleSeed() != 0)
		{
			applyShuffle(rows);
		}

		if (rows.isEmpty())
		{
			content.add(panel.messageLabel(q.isEmpty()
				? "No doable Combat Achievements right now."
				: "No CAs match your search."));
			return;
		}
		int shown = 0;
		for (SidePanelViewModel.TaskRow r : rows)
		{
			if (shown >= RENDER_CAP)
			{
				content.add(panel.messageLabel("+" + (rows.size() - shown) + " more — refine your search."));
				break;
			}
			content.add(taskCard(r));
			content.add(CardKit.spacer());
			shown++;
		}
	}

	private void sortRows(List<SidePanelViewModel.TaskRow> rows)
	{
		switch (panel.sort())
		{
			case MOST_POINTS:
				rows.sort(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.points).reversed()
					.thenComparingInt(r -> r.id));
				break;
			case EASIEST:
				rows.sort(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.difficulty)
					.thenComparing(Comparator.comparingInt((SidePanelViewModel.TaskRow r) -> r.points).reversed())
					.thenComparingInt(r -> r.id));
				break;
			default: // RECOMMENDED: keep the model's ranked order
				break;
		}
	}

	/**
	 * Reorders the doable CAs with a bounded random rank jitter so each reshuffle surfaces a different but
	 * still-sensible set. Deterministic per {@link CombatAchievementsPanel#shuffleSeed}, so per-tick
	 * refreshes don't re-scramble the list — only pressing the shuffle button (which bumps the seed)
	 * changes it.
	 */
	private void applyShuffle(List<SidePanelViewModel.TaskRow> rows)
	{
		Map<Integer, Double> key = new HashMap<>();
		for (int i = 0; i < rows.size(); i++)
		{
			SidePanelViewModel.TaskRow r = rows.get(i);
			key.put(r.id, jitteredKey(i, r.id, panel.shuffleSeed()));
		}
		rows.sort(Comparator.comparingDouble(r -> key.get(r.id)));
	}

	/**
	 * A task's jittered sort key: its rank plus a bounded, deterministic offset. Uses a splitmix64 hash of
	 * (seed, id) rather than {@link java.util.Random} — Random's first output is correlated across nearby
	 * seeds, which would make consecutive reshuffles produce almost the same order.
	 */
	public static double jitteredKey(int rank, int id, long seed)
	{
		long h = seed * 0x9E3779B97F4A7C15L + (id + 1L) * 0xD1B54A32D192ED03L;
		h ^= h >>> 30;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 27;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 31;
		double u = (h >>> 11) * 0x1.0p-53; // uniform in [0, 1)
		return rank + (u * 2.0 - 1.0) * SHUFFLE_JITTER;
	}

	private JPanel taskCard(SidePanelViewModel.TaskRow row)
	{
		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		Color accent = row.doableNow ? CombatAchievementsTheme.NAME : CombatAchievementsTheme.LOCKED;
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(5, 7, 5, 7)));

		String name = CombatAchievementsTheme.hex(row.doableNow
			? CombatAchievementsTheme.NAME : CombatAchievementsTheme.LOCKED);
		StringBuilder sb = new StringBuilder("<html><body style='width:166px'>");
		sb.append("<span style='color:").append(name).append("'><b>").append(CardKit.escape(row.name)).append("</b></span>");
		if (!row.doableNow)
		{
			String lock = (row.lockReason == null || row.lockReason.isEmpty()) ? "locked" : row.lockReason;
			sb.append(" <span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.LOCKED))
				.append("'>(").append(CardKit.escape(lock)).append(")</span>");
		}
		if (!row.curated)
		{
			sb.append(" <span style='color:#6f6f6f'>&#9679;</span>");
		}
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.DESC))
			.append("'>").append(CardKit.escape(row.description)).append("</span>");
		sb.append("<br><span style='color:").append(CombatAchievementsTheme.hex(CombatAchievementsTheme.POINTS))
			.append("'>").append(row.points).append(" pts</span>")
			.append(" <span style='color:" + CardKit.metaHex() + "'>· ").append(CardKit.escape(row.tierName)).append("</span>");
		if (row.difficulty > 0)
		{
			sb.append(" <span style='color:" + CardKit.metaHex() + "'>· </span><span style='color:")
				.append(CombatAchievementsTheme.hex(CardKit.difficultyColor(row.difficulty)))
				.append("'>difficulty ").append(row.difficulty).append("</span>");
		}
		sb.append("</body></html>");
		JLabel label = new JLabel(sb.toString());
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		card.add(label, BorderLayout.CENTER);

		card.add(panel.linkRow(row.wikiUrl, row.guideUrl, row.curatedVideo), BorderLayout.SOUTH);
		CardKit.addHover(card, ColorScheme.DARK_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		CardKit.onClick(card, () -> {
			panel.setRoute(panel.route().withCa(row.detail));
			panel.rebuild();
		});
		return CardKit.fullWidth(card);
	}
}
