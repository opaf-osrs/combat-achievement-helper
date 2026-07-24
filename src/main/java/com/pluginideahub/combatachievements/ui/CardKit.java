package com.pluginideahub.combatachievements.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Stateless building blocks shared by the side panel's cards: wrapped html labels, full-width
 * sizing, spacers, click/hover wiring, themed colour lookups and the small link/action buttons.
 */
public final class CardKit
{
	private CardKit()
	{
	}

	/**
	 * An html JLabel that truly wraps at {@code width} real pixels. Swing's CSS treats px as scaled
	 * units (~1.3x with this look-and-feel), so a hard-coded {@code body width} style lays out wider
	 * than asked and the text clips at the panel edge — the Route's unlock cards lost their last
	 * prerequisite this way, invisibly, for as long as they have existed. This measures the html view's
	 * actual span and re-asks for a proportionally smaller CSS width, so the real span lands on
	 * {@code width} whatever the scale factor is, then pins the label to the wrapped height.
	 */
	public static JLabel wrappedHtmlLabel(String innerHtml, int width)
	{
		JLabel label = new JLabel(htmlAt(innerHtml, width));
		javax.swing.text.View view = htmlView(label);
		if (view != null)
		{
			float natural = view.getPreferredSpan(javax.swing.text.View.X_AXIS);
			if (natural > width)
			{
				label.setText(htmlAt(innerHtml, Math.max(50, (int) (width * (width / natural)))));
				view = htmlView(label);
			}
			view.setSize(width, 0);
			int height = (int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS));
			label.setPreferredSize(new Dimension(width, height));
			label.setMaximumSize(new Dimension(width, height));
		}
		return label;
	}

	public static String htmlAt(String innerHtml, int cssWidth)
	{
		return "<html><body style='width:" + cssWidth + "px'>" + innerHtml + "</body></html>";
	}

	public static javax.swing.text.View htmlView(JLabel label)
	{
		return (javax.swing.text.View) label.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
	}

	public static <T extends JPanel> T fullWidth(T panel)
	{
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	public static JLabel fullWidth(JLabel label)
	{
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	public static JPanel spacer()
	{
		JPanel p = new JPanel();
		p.setOpaque(false);
		p.setPreferredSize(new Dimension(1, 6));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		return p;
	}

	public static String escape(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** Adds a click handler + hand cursor to a card, without disturbing its inner link buttons. */
	public static void onClick(JComponent c, Runnable action)
	{
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				action.run();
			}
		});
	}

	public static void addHover(JComponent c, Color base, Color hover)
	{
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				c.setBackground(hover);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				c.setBackground(base);
			}
		});
	}

	/** Hover that brightens the TEXT (for transparent link-style buttons), mirroring {@link #addHover}. */
	public static void addForegroundHover(JComponent c, Color base, Color hover)
	{
		c.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				c.setForeground(hover);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				c.setForeground(base);
			}
		});
	}

	/** Easy → mid → hard on the 1–10 Difficulty scale, using the theme's dedicated difficulty ramp so a
	 *  "diff N" never reads as the green points or a gold header sharing its line. */
	public static Color difficultyColor(int d)
	{
		if (d <= 0)
		{
			return CombatAchievementsTheme.DESC;
		}
		if (d <= 3)
		{
			return CombatAchievementsTheme.DIFF_EASY;
		}
		if (d <= 6)
		{
			return CombatAchievementsTheme.DIFF_MID;
		}
		return CombatAchievementsTheme.DIFF_HARD;
	}

	/** Themed grey for the muted "·" separators / secondary meta, as an HTML hex (follows the palette). */
	public static String metaHex()
	{
		return CombatAchievementsTheme.hex(CombatAchievementsTheme.NEUTRAL_META);
	}

	public static JButton linkButton(String text, String url)
	{
		return actionButton(text, () -> LinkBrowser.browse(url));
	}

	public static JButton actionButton(String text, Runnable action)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(CombatAchievementsTheme.HEADER_GOLD);
		button.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		button.addActionListener(e -> action.run());
		addHover(button, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARK_GRAY_HOVER_COLOR);
		return button;
	}
}
