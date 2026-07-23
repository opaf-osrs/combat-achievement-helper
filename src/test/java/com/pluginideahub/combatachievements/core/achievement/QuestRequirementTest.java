package com.pluginideahub.combatachievements.core.achievement;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestRequirementTest
{
	@Test
	public void equalityIsCaseInsensitiveOnNameAndConsidersStartedFlag()
	{
		assertEquals(new QuestRequirement("Dragon Slayer II", false),
			new QuestRequirement("dragon slayer ii", false));
		assertEquals(new QuestRequirement("Regicide", true).hashCode(),
			new QuestRequirement("REGICIDE", true).hashCode());
		// The started flag is part of identity.
		assertFalse(new QuestRequirement("Regicide", true)
			.equals(new QuestRequirement("Regicide", false)));
	}

	@Test
	public void deduplicatesInASet()
	{
		Set<QuestRequirement> set = new HashSet<>();
		set.add(new QuestRequirement("Song of the Elves", false));
		set.add(new QuestRequirement("SONG OF THE ELVES", false));
		assertEquals(1, set.size());
	}

	@Test
	public void trimsAndExposesFields()
	{
		QuestRequirement r = new QuestRequirement("  Priest in Peril  ", false);
		assertEquals("Priest in Peril", r.quest());
		assertFalse(r.startedSuffices());
		assertTrue(new QuestRequirement("Regicide", true).startedSuffices());
	}
}
