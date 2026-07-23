package com.pluginideahub.combatachievements.core.achievement;

/**
 * Thrown when the bundled Combat Achievements dataset cannot be loaded or fails its structural
 * invariants (missing resource, parse error, empty task list, unknown tier/type). The plugin catches
 * this in {@code startUp()} and renders a data-error panel state rather than crashing. See
 * docs/DESIGN.md §5.4.
 */
public class CombatAchievementDataException extends RuntimeException
{
	public CombatAchievementDataException(String message)
	{
		super(message);
	}

	public CombatAchievementDataException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
