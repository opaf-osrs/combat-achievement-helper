package com.pluginideahub.combatachievements.ui;

import com.pluginideahub.combatachievements.core.ui.PanelMode;
import com.pluginideahub.combatachievements.core.ui.SidePanelViewModel;

/**
 * Where the side panel is: the active {@link PanelMode} plus the drill-in selections layered over
 * it. Immutable — every transition returns a new value, so the panel can never be caught between
 * two places. There is no history: leaving a drill-in just clears its selection, and whatever the
 * remaining value shows is what shows.
 */
public final class PanelRoute
{
	private final PanelMode mode;
	/** When non-null, the CA-detail drill-in is shown (over whatever mode is active). */
	private final SidePanelViewModel.CaDetail ca;
	/** When non-null (Bosses mode), the boss-detail drill-in is shown. */
	private final String boss;
	/** When non-null (Route mode), the quest-unlock drill-in is shown; a selected CA overlays it. */
	private final SidePanelViewModel.UnlockView unlock;

	private PanelRoute(PanelMode mode, SidePanelViewModel.CaDetail ca, String boss,
		SidePanelViewModel.UnlockView unlock)
	{
		this.mode = mode;
		this.ca = ca;
		this.boss = boss;
		this.unlock = unlock;
	}

	/** A mode selected fresh, with no drill-in open. */
	public static PanelRoute of(PanelMode mode)
	{
		return new PanelRoute(mode, null, null, null);
	}

	public PanelMode mode()
	{
		return mode;
	}

	public SidePanelViewModel.CaDetail ca()
	{
		return ca;
	}

	public String boss()
	{
		return boss;
	}

	public SidePanelViewModel.UnlockView unlock()
	{
		return unlock;
	}

	/** Opens the CA detail over whatever is showing. */
	public PanelRoute withCa(SidePanelViewModel.CaDetail ca)
	{
		return new PanelRoute(mode, ca, boss, unlock);
	}

	/** Back out of the CA detail, to whatever it was covering. */
	public PanelRoute clearCa()
	{
		return new PanelRoute(mode, null, boss, unlock);
	}

	/** Opens a boss page (shown when the mode is Bosses). */
	public PanelRoute withBoss(String boss)
	{
		return new PanelRoute(mode, ca, boss, unlock);
	}

	/** Back out of the boss page, to the boss list. */
	public PanelRoute clearBoss()
	{
		return new PanelRoute(mode, ca, null, unlock);
	}

	/** Opens a quest-unlock page (shown when the mode is Route). */
	public PanelRoute withUnlock(SidePanelViewModel.UnlockView unlock)
	{
		return new PanelRoute(mode, ca, boss, unlock);
	}

	/** Back out of the quest-unlock page, to the Route list. */
	public PanelRoute clearUnlock()
	{
		return new PanelRoute(mode, ca, boss, null);
	}

	/** Jumps to a boss page from anywhere: Bosses mode, this boss open, any CA detail closed. */
	public PanelRoute toBoss(String boss)
	{
		return new PanelRoute(PanelMode.BOSSES, null, boss, unlock);
	}

	/** True when a drill-in page is showing instead of the mode's own list. */
	public boolean detailOpen()
	{
		return ca != null
			|| (boss != null && mode == PanelMode.BOSSES)
			|| (unlock != null && mode == PanelMode.ROUTE);
	}
}
