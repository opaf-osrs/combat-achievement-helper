package com.pluginideahub.combatachievements.core.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Shared loader for the bundled JSON datasets. Every dataset follows the same contract: a missing or
 * unreadable resource, or a root that is not a JSON object, yields null (so callers fall back to
 * their empty library); a malformed individual entry is skipped, never fatal. Entry iteration
 * preserves file order. Pure Java (Gson only).
 */
public final class BundledJson
{
	private BundledJson()
	{
	}

	/** Parses a classpath resource; null when the resource is absent or unreadable. */
	public static JsonObject readBundled(Class<?> anchor, String resource)
	{
		try (InputStream in = anchor.getResourceAsStream(resource))
		{
			return in == null ? null : read(in);
		}
		catch (IOException ex)
		{
			return null;
		}
	}

	/** Parses a UTF-8 stream (closing it); null unless the root is a JSON object. */
	public static JsonObject read(InputStream in)
	{
		try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			// The instance form, not the newer static parseReader: the plugin hub compiles against the
			// gson the client actually ships, which predates it.
			JsonElement parsed = new JsonParser().parse(reader);
			return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
		}
		catch (RuntimeException | IOException ex)
		{
			return null;
		}
	}

	/** The named member as an object; null when the root or the member is absent or not an object. */
	public static JsonObject objectMember(JsonObject root, String memberName)
	{
		if (root == null)
		{
			return null;
		}
		JsonElement el = root.get(memberName);
		return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
	}

	/**
	 * Builds an id-keyed map from the named object member. Keys are parsed as integers (trimmed).
	 * Entries whose value is not an object, whose parse returns null, or whose key or parse throws a
	 * {@link RuntimeException} are skipped.
	 */
	public static <V> Map<Integer, V> idMap(JsonObject root, String memberName,
		Function<JsonObject, V> parseEntry)
	{
		return idMapValues(root, memberName,
			el -> el.isJsonObject() ? parseEntry.apply(el.getAsJsonObject()) : null);
	}

	/** As {@link #idMap} but hands the raw value element to the parser, for primitive-valued maps. */
	public static <V> Map<Integer, V> idMapValues(JsonObject root, String memberName,
		Function<JsonElement, V> parseValue)
	{
		Map<Integer, V> out = new LinkedHashMap<>();
		JsonObject member = objectMember(root, memberName);
		if (member == null)
		{
			return out;
		}
		for (Map.Entry<String, JsonElement> entry : member.entrySet())
		{
			try
			{
				int id = Integer.parseInt(entry.getKey().trim());
				V value = parseValue.apply(entry.getValue());
				if (value != null)
				{
					out.put(id, value);
				}
			}
			catch (RuntimeException ignored)
			{
				// skip malformed entry
			}
		}
		return out;
	}

	/**
	 * Builds a name-keyed map from the named object member. Map keys are trimmed and lower-cased
	 * (the datasets are looked up case-insensitively); the parser receives the raw key and the entry
	 * object. Entries whose value is not an object, whose parse returns null, or whose parse throws a
	 * {@link RuntimeException} are skipped.
	 */
	public static <V> Map<String, V> nameMap(JsonObject root, String memberName,
		BiFunction<String, JsonObject, V> parseEntry)
	{
		return nameMapCore(root, memberName,
			(key, el) -> el.isJsonObject() ? parseEntry.apply(key, el.getAsJsonObject()) : null);
	}

	/** As {@link #nameMap} but hands the raw value element to the parser, for primitive-valued maps. */
	public static <V> Map<String, V> nameMapValues(JsonObject root, String memberName,
		Function<JsonElement, V> parseValue)
	{
		return nameMapCore(root, memberName, (key, el) -> parseValue.apply(el));
	}

	private static <V> Map<String, V> nameMapCore(JsonObject root, String memberName,
		BiFunction<String, JsonElement, V> parseValue)
	{
		Map<String, V> out = new LinkedHashMap<>();
		JsonObject member = objectMember(root, memberName);
		if (member == null)
		{
			return out;
		}
		for (Map.Entry<String, JsonElement> entry : member.entrySet())
		{
			try
			{
				V value = parseValue.apply(entry.getKey(), entry.getValue());
				if (value != null)
				{
					out.put(entry.getKey().trim().toLowerCase(Locale.ROOT), value);
				}
			}
			catch (RuntimeException ignored)
			{
				// skip malformed entry
			}
		}
		return out;
	}

	/** The member as a string; the fallback when absent, null, or not readable as a string. */
	public static String optString(JsonObject o, String key, String fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsString();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	/** The member as an int; the fallback when absent, null, or not readable as an int. */
	public static int optInt(JsonObject o, String key, int fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsInt();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	/** The member as a double; the fallback when absent, null, or not readable as a double. */
	public static double optDouble(JsonObject o, String key, double fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsDouble();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	/** The member as a boolean; the fallback when absent, null, or not readable as a boolean. */
	public static boolean optBoolean(JsonObject o, String key, boolean fallback)
	{
		JsonElement el = o.get(key);
		if (el == null || el.isJsonNull())
		{
			return fallback;
		}
		try
		{
			return el.getAsBoolean();
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}
}
