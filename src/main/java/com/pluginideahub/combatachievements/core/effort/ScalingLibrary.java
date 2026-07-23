package com.pluginideahub.combatachievements.core.effort;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pluginideahub.combatachievements.core.achievement.TaskType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The runtime half of the curated scaling table (bundled {@code scaling.json}, compiled from
 * {@code data/curation/scaling.csv}) — the single place the {@link TaskTimeModel} gets its attempt
 * multipliers: per-type base attempts, the difficulty→ability-factor curve, competence discounts and KC
 * thresholds. Sparse and safe: a missing/malformed file falls back to the built-in defaults (which match
 * the old hardcoded constants), so a data problem never breaks the time model. The keyword-offset and cap
 * sections of the file are build-time only and ignored here.
 */
public final class ScalingLibrary
{
	private static final String BUNDLED_RESOURCE =
		"/com/pluginideahub/combatachievements/scaling.json";

	private static final Map<String, Double> DEFAULT_BASE_ATTEMPTS = new HashMap<>();
	private static final Map<Integer, Double> DEFAULT_ABILITY = new HashMap<>();
	private static final Map<String, Double> DEFAULT_COMPETENCE = new HashMap<>();

	static
	{
		DEFAULT_BASE_ATTEMPTS.put("PERFECTION", 5.0);
		DEFAULT_BASE_ATTEMPTS.put("SPEED", 4.0);
		DEFAULT_BASE_ATTEMPTS.put("RESTRICTION", 2.0);
		DEFAULT_BASE_ATTEMPTS.put("MECHANICAL", 2.0);
		DEFAULT_BASE_ATTEMPTS.put("STAMINA", 2.0);
		DEFAULT_BASE_ATTEMPTS.put("KILL_COUNT", 1.0);
		DEFAULT_ABILITY.put(1, 0.5);
		DEFAULT_ABILITY.put(2, 0.7);
		DEFAULT_ABILITY.put(3, 1.0);
		DEFAULT_ABILITY.put(4, 1.8);
		DEFAULT_ABILITY.put(5, 3.0);
		DEFAULT_COMPETENCE.put("NOVICE", 1.0);
		DEFAULT_COMPETENCE.put("EXPERIENCED", 0.6);
		DEFAULT_COMPETENCE.put("VETERAN", 0.35);
	}

	private final Map<String, Double> baseAttempts;
	private final Map<Integer, Double> abilityFactor;
	private final Map<String, Double> competenceFactor;
	private final int experiencedThreshold;
	private final int veteranThreshold;

	private ScalingLibrary(Map<String, Double> baseAttempts, Map<Integer, Double> abilityFactor,
		Map<String, Double> competenceFactor, int experiencedThreshold, int veteranThreshold)
	{
		this.baseAttempts = baseAttempts;
		this.abilityFactor = abilityFactor;
		this.competenceFactor = competenceFactor;
		this.experiencedThreshold = experiencedThreshold;
		this.veteranThreshold = veteranThreshold;
	}

	/** The built-in defaults (matching the old hardcoded constants). */
	public static ScalingLibrary defaults()
	{
		return new ScalingLibrary(new HashMap<>(), new HashMap<>(), new HashMap<>(), 25, 150);
	}

	public static ScalingLibrary loadBundled()
	{
		try (InputStream in = ScalingLibrary.class.getResourceAsStream(BUNDLED_RESOURCE))
		{
			return in == null ? defaults() : load(in);
		}
		catch (IOException ex)
		{
			return defaults();
		}
	}

	public static ScalingLibrary load(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject())
			{
				return defaults();
			}
			JsonObject root = parsed.getAsJsonObject();
			Map<String, Double> base = readStringDoubles(root, "type_base_attempts");
			Map<Integer, Double> ability = new HashMap<>();
			for (Map.Entry<String, Double> e : readStringDoubles(root, "ability_factor").entrySet())
			{
				try
				{
					ability.put(Integer.parseInt(e.getKey().trim()), e.getValue());
				}
				catch (NumberFormatException ignored)
				{
					// skip non-integer ability rating key
				}
			}
			Map<String, Double> comp = readStringDoubles(root, "competence_factor");
			Map<String, Double> thresholds = readStringDoubles(root, "competence_threshold");
			int experienced = thresholds.containsKey("EXPERIENCED")
				? (int) Math.round(thresholds.get("EXPERIENCED")) : 25;
			int veteran = thresholds.containsKey("VETERAN")
				? (int) Math.round(thresholds.get("VETERAN")) : 150;
			return new ScalingLibrary(base, ability, comp, experienced, veteran);
		}
		catch (RuntimeException | IOException ex)
		{
			return defaults();
		}
	}

	private static Map<String, Double> readStringDoubles(JsonObject root, String section)
	{
		Map<String, Double> out = new HashMap<>();
		JsonElement el = root.get(section);
		if (el != null && el.isJsonObject())
		{
			for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet())
			{
				try
				{
					out.put(e.getKey().trim().toUpperCase(Locale.ROOT), e.getValue().getAsDouble());
				}
				catch (RuntimeException ignored)
				{
					// skip malformed value
				}
			}
		}
		return out;
	}

	/** Base attempts a novice needs per success for this task type (Kill Count = 1). */
	public double baseAttempts(TaskType type)
	{
		String key = type == null ? "KILL_COUNT" : type.name();
		Double v = baseAttempts.get(key);
		return v != null ? v : DEFAULT_BASE_ATTEMPTS.getOrDefault(key, 1.0);
	}

	/** Attempt multiplier for an execution ability rating 1..5 (3 = neutral). */
	public double abilityFactor(int rating)
	{
		int r = Math.max(1, Math.min(5, rating));
		Double v = abilityFactor.get(r);
		return v != null ? v : DEFAULT_ABILITY.getOrDefault(r, 1.0);
	}

	/** Attempt multiplier for a competence tier (veterans retry far less). */
	public double competenceFactor(CombatExperience.Competence competence)
	{
		String key = competence == null ? "NOVICE" : competence.name();
		Double v = competenceFactor.get(key);
		return v != null ? v : DEFAULT_COMPETENCE.getOrDefault(key, 1.0);
	}

	/** Classifies a kill count into a competence tier using the curated thresholds. */
	public CombatExperience.Competence competenceForKc(int kc)
	{
		if (kc >= veteranThreshold)
		{
			return CombatExperience.Competence.VETERAN;
		}
		if (kc >= experiencedThreshold)
		{
			return CombatExperience.Competence.EXPERIENCED;
		}
		return CombatExperience.Competence.NOVICE;
	}
}
