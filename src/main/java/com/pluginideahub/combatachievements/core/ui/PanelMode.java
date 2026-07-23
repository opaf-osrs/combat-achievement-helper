package com.pluginideahub.combatachievements.core.ui;

/**
 * The three top-level modes of the Quest-Helper-style side panel (see docs/adr/0001). Replaces the
 * old five-tab layout: a persistent toggle, default {@link #RECOMMENDED}.
 */
public enum PanelMode
{
	/** Strictly doable-now CAs, ranked by ease + points (+ a light time penalty). Sort picks the order. */
	RECOMMENDED("CAs"),
	/** Browse bosses by their doable-now projected points. */
	BOSSES("Bosses"),
	/** The fastest path to a target tier (authored routes + the generic next-tier route). */
	ROUTE("Route");

	private final String label;

	PanelMode(String label)
	{
		this.label = label;
	}

	public String label()
	{
		return label;
	}
}
