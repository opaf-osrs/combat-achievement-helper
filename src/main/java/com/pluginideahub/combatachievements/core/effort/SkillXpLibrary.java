package com.pluginideahub.combatachievements.core.effort;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the bundled {@code skills_xp.json} (skill → normal-method XP/hour by level bracket) and turns
 * it into "hours to train skill X→Y", so the cost of meeting a quest's skill requirement can be folded
 * into a CA's unlock effort. Pure Java (Gson only); sparse and safe (missing data → 0 hours).
 */
public final class SkillXpLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/skills_xp.json";

	/** Total experience required to reach each level (index = level, 1..99). OSRS standard table. */
	private static final int[] XP_AT_LEVEL = buildXpTable();

	/** Rate assumed for a skill with no bracket covering the level being trained. */
	private static final int FALLBACK_XP_PER_HOUR = 30000;
	private static final double HOURS_PER_DAY = 24.0;

	/** One rate over a level range for a skill: either per hour played, or per calendar day. */
	public static final class Bracket
	{
		final int fromLevel;
		final int toLevel;
		final int xpPerHour;
		/**
		 * Set instead of {@link #xpPerHour} for a DAILY-GATED bracket — Farming, whose patches grow on
		 * multi-hour timers, so what you get is bounded by the calendar rather than by time at the keyboard.
		 */
		final int xpPerDay;

		Bracket(int fromLevel, int toLevel, int xpPerHour, int xpPerDay)
		{
			this.fromLevel = fromLevel;
			this.toLevel = toLevel;
			this.xpPerDay = Math.max(0, xpPerDay);
			this.xpPerHour = Math.max(1, xpPerHour);
		}

		/**
		 * XP per hour of ELAPSED time. For an ordinary skill that is just the hourly rate; for a daily-gated
		 * one it is the daily allowance spread over the day, so "hours to train" comes out in real time and a
		 * skill you cannot rush stops looking like an afternoon's work.
		 */
		double xpPerElapsedHour()
		{
			return xpPerDay > 0 ? xpPerDay / HOURS_PER_DAY : xpPerHour;
		}
	}

	private final String version;
	private final Map<String, List<Bracket>> bySkill;

	private SkillXpLibrary(String version, Map<String, List<Bracket>> bySkill)
	{
		this.version = version;
		this.bySkill = bySkill;
	}

	public static SkillXpLibrary empty()
	{
		return new SkillXpLibrary("none", new LinkedHashMap<>());
	}

	public static SkillXpLibrary loadBundled()
	{
		try (InputStream in = SkillXpLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? empty() : load(in);
		}
		catch (IOException ex)
		{
			return empty();
		}
	}

	public static SkillXpLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return empty();
			}
			JsonObject root = parsed.getAsJsonObject();
			String version = root.has("version") && !root.get("version").isJsonNull()
				? root.get("version").getAsString() : "unknown";

			Map<String, List<Bracket>> map = new LinkedHashMap<>();
			JsonElement el = root.get("skills");
			if (el != null && el.isJsonObject())
			{
				for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
				{
					if (!e.getValue().isJsonObject())
					{
						continue;
					}
					List<Bracket> brackets = new ArrayList<>();
					JsonElement bEl = e.getValue().getAsJsonObject().get("brackets");
					if (bEl != null && bEl.isJsonArray())
					{
						for (JsonElement b : bEl.getAsJsonArray())
						{
							if (!b.isJsonObject())
							{
								continue;
							}
							JsonObject bo = b.getAsJsonObject();
							try
							{
								int perHour = optInt(bo, "xpPerHour");
								int perDay = optInt(bo, "xpPerDay");
								if (perHour > 0 || perDay > 0)
								{
									brackets.add(new Bracket(bo.get("fromLevel").getAsInt(),
										bo.get("toLevel").getAsInt(), perHour, perDay));
								}
							}
							catch (RuntimeException ignored)
							{
								// skip malformed bracket
							}
						}
					}
					if (!brackets.isEmpty())
					{
						map.put(e.getKey().trim().toLowerCase(Locale.ROOT), brackets);
					}
				}
			}
			return new SkillXpLibrary(version, map);
		}
		catch (RuntimeException | IOException ex)
		{
			return empty();
		}
	}

	public String version()
	{
		return version;
	}

	public int count()
	{
		return bySkill.size();
	}

	/** Total experience to reach {@code level} (clamped to 1..99). */
	public static int xpAtLevel(int level)
	{
		int l = Math.max(1, Math.min(99, level));
		return XP_AT_LEVEL[l];
	}

	private static int optInt(JsonObject o, String key)
	{
		JsonElement el = o.get(key);
		return el == null || el.isJsonNull() ? 0 : el.getAsInt();
	}

	/**
	 * Estimated hours to train {@code skill} from {@code fromLevel} to {@code toLevel} using the bundled
	 * normal-method rates. Returns 0 when already at/above the target or when no rate data exists.
	 *
	 * <p>For a daily-gated skill these are hours of ELAPSED time, not hours of play — training Farming is a
	 * calendar commitment however long you sit there, and the estimate says so.</p>
	 */
	public double hoursToTrain(String skill, int fromLevel, int toLevel)
	{
		if (skill == null || toLevel <= fromLevel)
		{
			return 0.0;
		}
		List<Bracket> brackets = bySkill.get(skill.trim().toLowerCase(Locale.ROOT));
		if (brackets == null || brackets.isEmpty())
		{
			return 0.0;
		}
		int from = Math.max(1, Math.min(99, fromLevel));
		int to = Math.max(1, Math.min(99, toLevel));
		double hours = 0.0;
		for (int level = from; level < to; level++)
		{
			int xpThisLevel = XP_AT_LEVEL[level + 1] - XP_AT_LEVEL[level];
			hours += xpThisLevel / rateAt(brackets, level);
		}
		return hours;
	}

	/** XP per elapsed hour for one level, from the covering bracket (or the nearest one). */
	private static double rateAt(List<Bracket> brackets, int level)
	{
		Bracket best = null;
		for (Bracket b : brackets)
		{
			if (level >= b.fromLevel && level < b.toLevel)
			{
				return b.xpPerElapsedHour();
			}
			// Remember the nearest bracket as a fallback for levels outside all ranges.
			if (best == null || Math.abs(b.fromLevel - level) < Math.abs(best.fromLevel - level))
			{
				best = b;
			}
		}
		return best == null ? FALLBACK_XP_PER_HOUR : best.xpPerElapsedHour();
	}

	private static int[] buildXpTable()
	{
		int[] xp = new int[100];
		double points = 0;
		xp[1] = 0;
		for (int level = 1; level < 99; level++)
		{
			points += Math.floor(level + 300 * Math.pow(2, level / 7.0));
			xp[level + 1] = (int) Math.floor(points / 4);
		}
		return xp;
	}
}
