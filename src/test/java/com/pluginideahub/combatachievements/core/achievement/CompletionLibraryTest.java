package com.pluginideahub.combatachievements.core.achievement;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompletionLibraryTest
{
	@Test
	public void bundledCompletionCoversEveryTask()
	{
		CompletionLibrary lib = CompletionLibrary.loadBundled();
		assertEquals(637, lib.count());
		// Task 0 (Noxious Foe) is a common Easy task.
		assertEquals(58.1, lib.completionFor(0), 0.001);
		assertTrue(lib.completionFor(0) > lib.completionFor(574));
	}

	@Test
	public void unknownTaskIsNegative()
	{
		CompletionLibrary lib = CompletionLibrary.load(stream("{\"tasks\":{\"5\":42.0}}"));
		assertEquals(42.0, lib.completionFor(5), 0.001);
		assertTrue(lib.completionFor(999) < 0);
	}

	@Test
	public void malformedFileIsEmpty()
	{
		assertEquals(0, CompletionLibrary.load(stream("nope")).count());
	}

	private static ByteArrayInputStream stream(String json)
	{
		return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
	}
}
