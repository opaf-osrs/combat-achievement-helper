package com.pluginideahub.combatachievements.core.ui;

/**
 * Parameterless actions the panel can ask the plugin to perform (mirrors roguescape's
 * {@code PanelAction}). Link/video opening is handled by the panel directly; these are the actions
 * that need the plugin/client.
 */
public enum PanelAction
{
	REFRESH,
	/** Re-solve the Route toward a different (but still quick) set of tasks. */
	RESHUFFLE_ROUTE
}
