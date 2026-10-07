package de.bettersouth.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

final class HudSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 1000;
	private static final int PANEL_HEIGHT = 600;
	private static final int SIDEBAR_WIDTH = 224;
	private static final int ROW_HEIGHT = 46;
	private static final int MIN_SCALE = 50;
	private static final int MAX_SCALE = 200;
	private static final int SETTINGS_WIDTH = 220;

	private final HudConfig config;
	private final HudWidget[] widgets;
	private final WidgetText widgetText;
	private final Screen parent;
	private int selected;
	private int panelX;
	private int panelY;
	private int previewX;
	private int previewY;
	private int previewWidth;
	private int previewHeight;
	private int settingsX;
	private float uiScale;
	private float uiOffsetX;
	private float uiOffsetY;
	private int dragging = -1;
	private int resizeAxis;
	private int resizeStartScale;
	private double resizeStartX;
	private double resizeStartY;
	private double dragOffsetX;
	private double dragOffsetY;

	HudSettingsScreen(HudConfig config, int selected, WidgetText widgetText) {
		this(config, selected, widgetText, null);
	}

	HudSettingsScreen(HudConfig config, int selected, WidgetText widgetText, Screen parent) {
		super(Component.literal("BetterSouth HUD"));
		this.config = config;
		this.widgets = HudWidget.values();
		this.selected = Math.clamp(selected, 0, widgets.length - 1);
		this.widgetText = widgetText;
		this.parent = parent;
	}

	@Override
	protected void init() {
		uiScale = Math.min(1.0f, Math.min((width - 24.0f) / PANEL_WIDTH, (height - 24.0f) / PANEL_HEIGHT));
		uiScale = Math.max(0.35f, uiScale);
		uiOffsetX = (width - PANEL_WIDTH * uiScale) / 2.0f;
		uiOffsetY = (height - PANEL_HEIGHT * uiScale) / 2.0f;
		panelX = 0;
		panelY = 0;
		previewX = panelX + SIDEBAR_WIDTH + 16;
		previewY = panelY + 78;
		previewWidth = 500;
		previewHeight = 400;
		settingsX = previewX + previewWidth + 20;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x66070A11);
		graphics.pose().pushMatrix();
		graphics.pose().translate(uiOffsetX, uiOffsetY);
		graphics.pose().scale(uiScale, uiScale);
		graphics.fill(panelX + 4, panelY + 5, panelX + PANEL_WIDTH + 4, panelY + PANEL_HEIGHT + 5, 0x70000000);
		graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC9141925);
		graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 3, 0xFF44D7C8);
		graphics.fill(panelX, panelY + 48, panelX + PANEL_WIDTH, panelY + 49, 0xFF30394A);
		graphics.fill(panelX + 14, panelY + 10, panelX + 42, panelY + 38, 0xFF1D393D);
		graphics.text(font, "B", panelX + 24, panelY + 20, 0xFF63E2D1);

		graphics.text(font, "BETTERSOUTH", panelX + 52, panelY + 10, 0xFF57E0D0);
		graphics.text(font, "HUD-STUDIO", panelX + 52, panelY + 27, 0xFFB6C1D2);
		graphics.text(font, "HUD DESIGN  /  LIVE-VORSCHAU", panelX + 204, panelY + 18, 0xFF8290A4);
		boolean closeHovered = inside(toLogicalX(mouseX), toLogicalY(mouseY),
				panelX + PANEL_WIDTH - 128, panelY + 12, 112, 26);
		graphics.fill(panelX + PANEL_WIDTH - 128, panelY + 12, panelX + PANEL_WIDTH - 16,
				panelY + 38, closeHovered ? 0xFF3A4658 : 0xFF232D3B);
		graphics.text(font, "ESC  SCHLIESSEN", panelX + PANEL_WIDTH - 116, panelY + 21,
				closeHovered ? 0xFFFFFFFF : 0xFFB8C4D5);

		drawSidebar(graphics);
		drawPreview(graphics);
		drawAppearanceSettings(graphics);
		graphics.pose().popMatrix();
	}

	private void drawSidebar(GuiGraphicsExtractor graphics) {
		graphics.fill(panelX + 10, panelY + 58, panelX + SIDEBAR_WIDTH, panelY + PANEL_HEIGHT - 12, 0xC0191F2D);
		graphics.text(font, "WIDGETS", panelX + 18, panelY + 65, 0xFF8D9BB0);
		for (int index = 0; index < widgets.length; index++) {
			HudWidget widget = widgets[index];
			HudConfig.WidgetPosition position = config.positions[index];
			int rowX = panelX + 14;
			int rowY = panelY + 78 + index * ROW_HEIGHT;
			boolean active = index == selected;

			graphics.fill(rowX, rowY, panelX + SIDEBAR_WIDTH - 4, rowY + ROW_HEIGHT - 3,
					active ? 0xFF273747 : 0xFF202735);
			if (active) {
				graphics.fill(rowX, rowY + 4, rowX + 3, rowY + ROW_HEIGHT - 7, 0xFF57E0D0);
			}
			graphics.fill(rowX + 8, rowY + 9, rowX + 29, rowY + 30, 0xFF33495A);
			graphics.text(font, widget.icon, rowX + 15, rowY + 16, 0xFF8DEADD);
			graphics.text(font, widget.label, rowX + 36, rowY + 8, 0xFFF0F4FA);
			graphics.text(font, position.opacity + "% Deckkraft", rowX + 36, rowY + 24, 0xFF8190A4);
			drawButton(graphics, rowX + 143, rowY + 5, 58, 21,
					position.enabled ? "AN" : "AUS",
					position.enabled ? 0xFF176C60 : 0xFF4A5362);

			graphics.text(font, position.scale + "%", rowX + 158, rowY + 29,
					active ? 0xFF57E0D0 : 0xFFBAC5D5);
		}
		drawButton(graphics, panelX + 20, panelY + 526, 83, 24, "Alle an", 0xFF176C60);
		drawButton(graphics, panelX + 110, panelY + 526, 96, 24, "Layout reset", 0xFF394657);
		graphics.text(font, "Aenderungen werden direkt gespeichert",
				panelX + 19, panelY + 558, 0xFF8290A4);
	}

	private void drawPreview(GuiGraphicsExtractor graphics) {
		graphics.text(font, "BILDSCHIRM-VORSCHAU", previewX, panelY + 61, 0xFFB8C4D5);
		String previewHint = widgets[selected] == HudWidget.SPOTIFY
				? "Spotify: Buttons anklicken zum Steuern"
				: "Ziehen zum Positionieren  |  Mausrad zum Skalieren";
		graphics.text(font, previewHint, previewX, panelY + 72, 0xFF8290A4);
		graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xC0101722);
		drawGrid(graphics);

		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		float scale = Math.min(previewWidth / (float) screenWidth, previewHeight / (float) screenHeight);
		float offsetX = (previewWidth - screenWidth * scale) / 2.0f;
		float offsetY = (previewHeight - screenHeight * scale) / 2.0f;
		graphics.pose().pushMatrix();
		graphics.pose().translate(previewX + offsetX, previewY + offsetY);
		graphics.pose().scale(scale, scale);
		for (HudWidget widget : widgets) {
			HudConfig.WidgetPosition position = config.positions[widget.ordinal()];
			if (!position.enabled) {
				continue;
			}
			boolean active = widget.ordinal() == selected;
			String text = widgetText.textFor(widget);
			drawHudWidget(graphics, widget, text, position.x, position.y,
					position.scale / 100.0f, active, position);
			if (active) {
				int widgetWidth = widgetWidth(client, widget, text);
				float widgetScale = position.scale / 100.0f;
				int gripWidth = Math.max(2, (int) Math.ceil(3 / scale));
				int gripHeight = Math.max(6, (int) Math.ceil(14 / scale));
				int gripX = (int) (position.x + (widgetWidth - 3) * widgetScale);
				int gripY = (int) (position.y + (widgetHeight(widget) / 2.0 - 7) * widgetScale);
				graphics.fill(gripX, gripY, gripX + gripWidth, gripY + gripHeight, 0xFFFFD166);
			}
		}
		graphics.pose().popMatrix();
		graphics.fill(previewX, previewY + previewHeight + 12, previewX + previewWidth,
				previewY + previewHeight + 48, 0xC01A2230);
		graphics.text(font, "AUSGEWAEHLT", previewX + 12, previewY + previewHeight + 19, 0xFF8290A4);
		graphics.text(font, widgets[selected].label + "  /  " +
						config.positions[selected].scale + "%  /  " +
						config.positions[selected].opacity + "% Deckkraft",
				previewX + 12, previewY + previewHeight + 33, 0xFF57E0D0);
	}

	private void drawAppearanceSettings(GuiGraphicsExtractor graphics) {
		HudConfig.WidgetPosition position = config.positions[selected];
		int x = settingsX;
		int y = previewY;
		graphics.text(font, "DESIGN", x, panelY + 61, 0xFFB8C4D5);
		graphics.text(font, widgets[selected].label.toUpperCase(), x, panelY + 72, 0xFF8290A4);
		graphics.fill(x, y, x + SETTINGS_WIDTH, y + previewHeight, 0xC0191F2D);
		graphics.fill(x, y, x + 2, y + previewHeight, 0xFF354252);

		graphics.text(font, "Hintergrund", x + 14, y + 18, 0xFFE6ECF4);
		graphics.text(font, position.backgroundEnabled ? "Panel einblenden" : "Transparent",
				x + 14, y + 33, 0xFF8796AA);
		drawButton(graphics, x + 154, y + 16, 52, 25,
				position.backgroundEnabled ? "AN" : "AUS",
				position.backgroundEnabled ? 0xFF176C60 : 0xFF4A5362);

		graphics.text(font, "Deckkraft", x + 14, y + 72, 0xFFE6ECF4);
		graphics.text(font, position.opacity + "%", x + SETTINGS_WIDTH - 36, y + 72, 0xFF57E0D0);
		graphics.fill(x + 14, y + 91, x + SETTINGS_WIDTH - 14, y + 99, 0xFF354252);
		int sliderWidth = SETTINGS_WIDTH - 28;
		int fillWidth = sliderWidth * position.opacity / 100;
		if (fillWidth > 0) {
			graphics.fill(x + 14, y + 91, x + 14 + fillWidth, y + 99, 0xFF43C9B8);
		}
		int knobX = x + 14 + fillWidth;
		graphics.fill(knobX - 3, y + 88, knobX + 3, y + 102, 0xFFE9FFFC);
		graphics.text(font, "Transparent", x + 14, y + 108, 0xFF8796AA);
		graphics.text(font, "Deckend", x + SETTINGS_WIDTH - 59, y + 108, 0xFF8796AA);

		drawToggleRow(graphics, x, y + 137, "Akzent-Kontur", "Obere Farblinie",
				position.borderEnabled);
		drawToggleRow(graphics, x, y + 190, "Symbol / Cover", "Widget-Icon anzeigen",
				position.iconEnabled);

		graphics.text(font, "Akzentfarbe", x + 14, y + 256, 0xFFE6ECF4);
		int[][] swatches = {{0xFF4AD7C6, 0xFFB18CFF, 0xFFFFC857}};
		String[] labels = {"MINT", "VIOLETT", "GOLD"};
		for (int index = 0; index < swatches[0].length; index++) {
			int swatchX = x + 14 + index * 64;
			graphics.fill(swatchX, y + 275, swatchX + 56, y + 307,
					position.accentTheme == index ? 0xFF344253 : 0xFF252D3A);
			graphics.fill(swatchX + 5, y + 280, swatchX + 17, y + 292, swatches[0][index]);
			graphics.text(font, labels[index], swatchX + 20, y + 282,
					position.accentTheme == index ? 0xFFFFFFFF : 0xFFAFBAC9);
			if (position.accentTheme == index) {
				graphics.fill(swatchX, y + 305, swatchX + 56, y + 307, swatches[0][index]);
			}
		}

		graphics.text(font, "Groesse", x + 14, y + 335, 0xFFE6ECF4);
		drawButton(graphics, x + 14, y + 351, 34, 25, "-", 0xFF394657);
		graphics.text(font, position.scale + "%", x + 83, y + 360, 0xFF57E0D0);
		drawButton(graphics, x + SETTINGS_WIDTH - 48, y + 351, 34, 25, "+", 0xFF394657);
		drawButton(graphics, x + 14, y + 382, SETTINGS_WIDTH - 28, 24,
				"Position zuruecksetzen", 0xFF394657);
		graphics.text(font, "X " + position.x + "   Y " + position.y,
				x + 14, y + previewHeight - 16, 0xFF8290A4);
	}

	private void drawToggleRow(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			String title,
			String subtitle,
			boolean enabled
	) {
		graphics.text(font, title, x + 14, y + 5, 0xFFE6ECF4);
		graphics.text(font, subtitle, x + 14, y + 21, 0xFF8796AA);
		drawButton(graphics, x + SETTINGS_WIDTH - 66, y + 7, 52, 24,
				enabled ? "AN" : "AUS", enabled ? 0xFF176C60 : 0xFF4A5362);
	}

	private void drawGrid(GuiGraphicsExtractor graphics) {
		for (int x = previewX + 12; x < previewX + previewWidth; x += 20) {
			graphics.fill(x, previewY + 1, x + 1, previewY + previewHeight - 1, 0x171E2A38);
		}
		for (int y = previewY + 12; y < previewY + previewHeight; y += 20) {
			graphics.fill(previewX + 1, y, previewX + previewWidth - 1, y + 1, 0x171E2A38);
		}
		graphics.fill(previewX, previewY, previewX + previewWidth, previewY + 1, 0xFF344052);
		graphics.fill(previewX, previewY + previewHeight - 1, previewX + previewWidth,
				previewY + previewHeight, 0xFF344052);
		graphics.fill(previewX, previewY, previewX + 1, previewY + previewHeight, 0xFF344052);
		graphics.fill(previewX + previewWidth - 1, previewY,
				previewX + previewWidth, previewY + previewHeight, 0xFF344052);
	}

	static void drawHudWidget(
			GuiGraphicsExtractor graphics,
			HudWidget widget,
			String text,
			int x,
			int y,
			float scale,
			boolean selected,
			HudConfig.WidgetPosition style
	) {
		Minecraft client = Minecraft.getInstance();
		int contentWidth = client.font.width(text);
		int widgetWidth = contentWidth + 34;
		int widgetHeight = 26;
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		if (widget == HudWidget.SPOTIFY) {
			drawSpotifyWidget(graphics, client, text, widgetWidth(client, widget, text), selected, style);
			graphics.pose().popMatrix();
			return;
		}
		if (widget == HudWidget.KEYSTROKES) {
			drawKeystrokesWidget(graphics, client, selected, style);
			graphics.pose().popMatrix();
			return;
		}
		int accent = accentColor(widget, style.accentTheme);
		if (style.backgroundEnabled) {
			graphics.fill(2, 3, widgetWidth + 2, widgetHeight + 3,
					withOpacity(0x65000000, style.opacity));
			graphics.fill(0, 0, widgetWidth, widgetHeight,
					withOpacity(selected ? 0xFF2B3545 : 0xFF1B2230, style.opacity));
			if (style.iconEnabled) {
				graphics.fill(7, 4, 24, 21, withOpacity(0xFF334A5C, style.opacity));
			}
		}
		if (style.borderEnabled) {
			graphics.fill(0, 0, 2, widgetHeight, accent);
			graphics.fill(2, 0, widgetWidth, 1, withOpacity(accent, style.opacity));
		}
		if (style.iconEnabled) {
			graphics.text(client.font, widget.icon, 13, 9, accent);
		}
		graphics.text(client.font, text, style.iconEnabled ? 29 : 7, 9, 0xFFF1F5FB);
		if (selected && style.borderEnabled) {
			graphics.fill(widgetWidth - 4, 0, widgetWidth, 3, 0xFFFFD166);
			graphics.fill(widgetWidth - 4, widgetHeight - 3, widgetWidth, widgetHeight, 0xFFFFD166);
		}
		graphics.pose().popMatrix();
	}

	private static void drawSpotifyWidget(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			String text,
			int width,
			boolean selected,
			HudConfig.WidgetPosition style
	) {
		int height = 49;
		int accent = accentColor(HudWidget.SPOTIFY, style.accentTheme);
		if (style.backgroundEnabled) {
			graphics.fill(2, 3, width + 2, height + 3, withOpacity(0x65000000, style.opacity));
			graphics.fill(0, 0, width, height,
					withOpacity(selected ? 0xFF2B3545 : 0xFF1B2230, style.opacity));
			if (style.iconEnabled) {
				graphics.fill(5, 4, 48, 47, withOpacity(0xFF334A5C, style.opacity));
			}
		}
		if (style.borderEnabled) {
			graphics.fill(0, 0, 2, height, accent);
			graphics.fill(2, 0, width, 1, withOpacity(accent, style.opacity));
		}
		if (style.iconEnabled) {
			Identifier cover = SpotifyCompanion.coverTexture();
			if (cover == null) {
				graphics.text(client.font, "♪", 21, 20, accent);
			} else {
				graphics.blit(RenderPipelines.GUI_TEXTURED, cover, 7, 6, 0, 0, 40, 40, 40, 40);
			}
		}
		String fitted = fitText(client, text, width - 60);
		graphics.text(client.font, fitted, 53, 9, 0xFFF1F5FB);

		drawSpotifyButton(graphics, client, 53, 28, 24, 16, "<<");
		String playPauseIcon = SpotifyCompanion.isPlaying() ? "||" : ">";
		drawSpotifyButton(graphics, client, 81, 28, 24, 16, playPauseIcon);
		drawSpotifyButton(graphics, client, 109, 28, 24, 16, ">>");
		if (selected && style.borderEnabled) {
			graphics.fill(width - 4, 0, width, 3, 0xFFFFD166);
			graphics.fill(width - 4, height - 3, width, height, 0xFFFFD166);
		}
	}

	private static void drawSpotifyButton(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			int x,
			int y,
			int width,
			int height,
			String label
	) {
		graphics.fill(x, y, x + width, y + height, 0xFF176C60);
		graphics.text(client.font, label, x + (width - client.font.width(label)) / 2,
				y + 4, 0xFFFFFFFF);
	}

	private static void drawKeystrokesWidget(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			boolean selected,
			HudConfig.WidgetPosition style
	) {
		int width = 128;
		int height = 75;
		int accent = accentColor(HudWidget.KEYSTROKES, style.accentTheme);
		if (style.backgroundEnabled) {
			graphics.fill(2, 3, width + 2, height + 3, withOpacity(0x65000000, style.opacity));
			graphics.fill(0, 0, width, height,
					withOpacity(selected ? 0xFF2B3545 : 0xFF1B2230, style.opacity));
		}
		if (style.borderEnabled) {
			graphics.fill(0, 0, 2, height, accent);
			graphics.fill(2, 0, width, 1, withOpacity(accent, style.opacity));
			if (selected) {
				graphics.fill(width - 4, 0, width, 3, 0xFFFFD166);
				graphics.fill(width - 4, height - 3, width, height, 0xFFFFD166);
			}
		}

		drawKeycap(graphics, client, 49, 7, 28, 19, "W", GLFW.GLFW_KEY_W, style.opacity);
		drawKeycap(graphics, client, 19, 29, 28, 19, "A", GLFW.GLFW_KEY_A, style.opacity);
		drawKeycap(graphics, client, 49, 29, 28, 19, "S", GLFW.GLFW_KEY_S, style.opacity);
		drawKeycap(graphics, client, 79, 29, 28, 19, "D", GLFW.GLFW_KEY_D, style.opacity);
		drawKeycap(graphics, client, 7, 53, 39, 16, "SHIFT",
				GLFW.GLFW_KEY_LEFT_SHIFT, style.opacity);
		drawKeycap(graphics, client, 49, 53, 30, 16, "SPACE",
				GLFW.GLFW_KEY_SPACE, style.opacity);
		drawKeycap(graphics, client, 82, 53, 39, 16, "CTRL",
				GLFW.GLFW_KEY_LEFT_CONTROL, style.opacity);
	}

	private static void drawKeycap(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			int x,
			int y,
			int width,
			int height,
			String label,
			int key,
			int opacity
	) {
		boolean down = Keystrokes.isPressed(key);
		int background = down ? 0xFF485565 : 0xFF303B4B;
		graphics.fill(x, y, x + width, y + height, withOpacity(background, opacity));
		graphics.fill(x, y, x + width, y + 1, withOpacity(down ? 0xFF9AAFC2 : 0xFF566477, opacity));
		int textColor = down ? 0xFFFFFFFF : 0xFFDAE2ED;
		graphics.text(client.font, label,
				x + Math.max(2, (width - client.font.width(label)) / 2),
				y + Math.max(3, (height - 9) / 2), textColor);
	}

	private static String fitText(Minecraft client, String text, int maxWidth) {
		if (client.font.width(text) <= maxWidth) {
			return text;
		}
		String suffix = "...";
		String fitted = text;
		while (!fitted.isEmpty() && client.font.width(fitted + suffix) > maxWidth) {
			fitted = fitted.substring(0, fitted.length() - 1);
		}
		return fitted + suffix;
	}

	private static int widgetWidth(Minecraft client, HudWidget widget, String text) {
		if (widget == HudWidget.SPOTIFY) {
			return Math.max(145, client.font.width(text) + 62);
		}
		if (widget == HudWidget.KEYSTROKES) {
			return 128;
		}
		return client.font.width(text) + 34;
	}

	private static int widgetHeight(HudWidget widget) {
		return switch (widget) {
			case SPOTIFY -> 49;
			case KEYSTROKES -> 75;
			default -> 26;
		};
	}

	private static int accentColor(HudWidget widget, int theme) {
		if (theme == 0 && widget == HudWidget.WANTED) {
			return 0xFFFF6A77;
		}
		return switch (theme) {
			case 1 -> 0xFFB18CFF;
			case 2 -> 0xFFFFC857;
			default -> 0xFF4AD7C6;
		};
	}

	private static int withOpacity(int color, int opacity) {
		int alpha = ((color >>> 24) * opacity / 100) << 24;
		return (color & 0x00FFFFFF) | alpha;
	}

	private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int buttonWidth, int buttonHeight,
			String text, int color) {
		graphics.fill(x, y, x + buttonWidth, y + buttonHeight, color);
		graphics.text(font, text, x + Math.max(4, (buttonWidth - font.width(text)) / 2),
				y + Math.max(3, (buttonHeight - 9) / 2), 0xFFF1F5FB);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double mouseX = toLogicalX(event.x());
		double mouseY = toLogicalY(event.y());
		if (inside(mouseX, mouseY, panelX + PANEL_WIDTH - 128, panelY + 12, 112, 26)) {
			onClose();
			return true;
		}
		for (int index = 0; index < widgets.length; index++) {
			int rowX = panelX + 14;
			int rowY = panelY + 78 + index * ROW_HEIGHT;
			if (inside(mouseX, mouseY, rowX, rowY, SIDEBAR_WIDTH - 18, ROW_HEIGHT - 3)) {
				selected = index;
				HudConfig.WidgetPosition position = config.positions[index];
				if (inside(mouseX, mouseY, rowX + 143, rowY + 3, 58, 24)) {
					position.enabled = !position.enabled;
					config.save();
				}
				return true;
			}
		}
		if (inside(mouseX, mouseY, panelX + 20, panelY + 526, 83, 24)) {
			for (HudConfig.WidgetPosition position : config.positions) {
				position.enabled = true;
			}
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, panelX + 110, panelY + 526, 96, 24)) {
			for (HudWidget widget : widgets) {
				HudConfig.WidgetPosition position = config.positions[widget.ordinal()];
				position.x = widget.defaultX;
				position.y = widget.defaultY;
				position.scale = 100;
				position.enabled = true;
			}
			config.save();
			return true;
		}
		if (handleAppearanceClick(mouseX, mouseY)) {
			return true;
		}
		if (inside(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)) {
			String spotifyControl = spotifyControlAt(mouseX, mouseY);
			if (spotifyControl != null) {
				SpotifyCompanion.control(spotifyControl);
				return true;
			}
			int hit = widgetAt(mouseX, mouseY);
			if (hit >= 0) {
				selected = hit;
				HudConfig.WidgetPosition position = config.positions[hit];
				double scale = previewScale();
				double gameX = (mouseX - previewOriginX()) / scale;
				double gameY = (mouseY - previewOriginY()) / scale;
				double widgetScale = position.scale / 100.0;
				double right = position.x + widgetWidth(Minecraft.getInstance(), widgets[hit],
						widgetText.textFor(widgets[hit])) * widgetScale;
				double bottom = position.y + widgetHeight(widgets[hit]) * widgetScale;
				double edgeSize = 8.0 / scale;
				boolean onRightEdge = gameX >= right - edgeSize && gameX <= right &&
						gameY >= position.y && gameY <= bottom;
				boolean onBottomEdge = gameY >= bottom - edgeSize && gameY <= bottom &&
						gameX >= position.x && gameX <= right;
				resizeAxis = onRightEdge ? 1 : onBottomEdge ? 2 : 0;
				if (resizeAxis != 0) {
					resizeStartScale = position.scale;
					resizeStartX = gameX;
					resizeStartY = gameY;
				} else {
					dragOffsetX = gameX - position.x;
					dragOffsetY = gameY - position.y;
				}
				dragging = hit;
			}
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (dragging < 0 || event.button() != 0) {
			return super.mouseDragged(event, deltaX, deltaY);
		}
		double scale = previewScale();
		HudConfig.WidgetPosition position = config.positions[dragging];
		double mouseX = toLogicalX(event.x());
		double mouseY = toLogicalY(event.y());
		if (resizeAxis != 0) {
			double gameX = (mouseX - previewOriginX()) / scale;
			double gameY = (mouseY - previewOriginY()) / scale;
			String text = widgetText.textFor(widgets[dragging]);
			int baseSize = resizeAxis == 1
					? widgetWidth(Minecraft.getInstance(), widgets[dragging], text)
					: widgetHeight(widgets[dragging]);
			double distance = resizeAxis == 1 ? gameX - resizeStartX : gameY - resizeStartY;
			position.scale = Math.clamp(resizeStartScale + (int) Math.round(distance / baseSize * 100),
					MIN_SCALE, MAX_SCALE);
			return true;
		}
		position.x = Math.max(0, (int) Math.round((mouseX - previewOriginX()) / scale - dragOffsetX));
		position.y = Math.max(0, (int) Math.round((mouseY - previewOriginY()) / scale - dragOffsetY));
		clampPosition(position, widgets[dragging]);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging >= 0) {
			dragging = -1;
			resizeAxis = 0;
			config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		mouseX = toLogicalX(mouseX);
		mouseY = toLogicalY(mouseY);
		if (inside(mouseX, mouseY, previewX, previewY, previewWidth, previewHeight)) {
			changeScale(config.positions[selected], verticalAmount > 0 ? 10 : -10);
			return true;
		}
		if (inside(mouseX, mouseY, settingsX + 14, previewY + 86, SETTINGS_WIDTH - 28, 30)) {
			changeOpacity(config.positions[selected], verticalAmount > 0 ? 5 : -5);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private boolean handleAppearanceClick(double mouseX, double mouseY) {
		int x = settingsX;
		int y = previewY;
		HudConfig.WidgetPosition position = config.positions[selected];
		if (inside(mouseX, mouseY, x + 146, y + 13, 64, 31)) {
			position.backgroundEnabled = !position.backgroundEnabled;
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, x + 10, y + 86, SETTINGS_WIDTH - 20, 30)) {
			position.opacity = Math.clamp((int) Math.round((mouseX - (x + 14)) /
					(double) (SETTINGS_WIDTH - 28) * 100), 0, 100);
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, x + SETTINGS_WIDTH - 72, y + 141, 64, 34)) {
			position.borderEnabled = !position.borderEnabled;
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, x + SETTINGS_WIDTH - 72, y + 194, 64, 34)) {
			position.iconEnabled = !position.iconEnabled;
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, x + 10, y + 270, SETTINGS_WIDTH - 20, 44)) {
			int swatch = Math.clamp((int) ((mouseX - (x + 14)) / 64), 0, 2);
			position.accentTheme = swatch;
			config.save();
			return true;
		}
		if (inside(mouseX, mouseY, x + 10, y + 347, 42, 33)) {
			changeScale(position, -10);
			return true;
		}
		if (inside(mouseX, mouseY, x + SETTINGS_WIDTH - 52, y + 347, 42, 33)) {
			changeScale(position, 10);
			return true;
		}
		if (inside(mouseX, mouseY, x + 10, y + 378, SETTINGS_WIDTH - 20, 32)) {
			position.x = widgets[selected].defaultX;
			position.y = widgets[selected].defaultY;
			config.save();
			return true;
		}
		return false;
	}

	private int widgetAt(double mouseX, double mouseY) {
		double scale = previewScale();
		double gameX = (mouseX - previewOriginX()) / scale;
		double gameY = (mouseY - previewOriginY()) / scale;
		for (int index = widgets.length - 1; index >= 0; index--) {
			HudConfig.WidgetPosition position = config.positions[index];
			if (!position.enabled) {
				continue;
			}
			String text = widgetText.textFor(widgets[index]);
			int widgetWidth = widgetWidth(Minecraft.getInstance(), widgets[index], text);
			int widgetHeight = widgetHeight(widgets[index]);
			double widgetScale = position.scale / 100.0;
			if (gameX >= position.x && gameX <= position.x + widgetWidth * widgetScale &&
					gameY >= position.y && gameY <= position.y + widgetHeight * widgetScale) {
				return index;
			}
		}
		return -1;
	}

	private String spotifyControlAt(double mouseX, double mouseY) {
		HudConfig.WidgetPosition position = config.positions[HudWidget.SPOTIFY.ordinal()];
		if (!position.enabled) {
			return null;
		}
		double previewScale = previewScale();
		double gameX = (mouseX - previewOriginX()) / previewScale;
		double gameY = (mouseY - previewOriginY()) / previewScale;
		double scale = position.scale / 100.0;
		double localX = (gameX - position.x) / scale;
		double localY = (gameY - position.y) / scale;
		if (localY < 28 || localY > 44) {
			return null;
		}
		if (localX >= 53 && localX <= 77) {
			return "previous";
		}
		if (localX >= 81 && localX <= 105) {
			return "toggle";
		}
		if (localX >= 109 && localX <= 133) {
			return "next";
		}
		return null;
	}

	private double previewScale() {
		Minecraft client = Minecraft.getInstance();
		return Math.min(previewWidth / (double) client.getWindow().getGuiScaledWidth(),
				previewHeight / (double) client.getWindow().getGuiScaledHeight());
	}

	private double previewOriginX() {
		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		double scale = previewScale();
		return previewX + (previewWidth - screenWidth * scale) / 2.0;
	}

	private double previewOriginY() {
		Minecraft client = Minecraft.getInstance();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		double scale = previewScale();
		return previewY + (previewHeight - screenHeight * scale) / 2.0;
	}

	private double toLogicalX(double mouseX) {
		return (mouseX - uiOffsetX) / uiScale;
	}

	private double toLogicalY(double mouseY) {
		return (mouseY - uiOffsetY) / uiScale;
	}

	private void clampPosition(HudConfig.WidgetPosition position, HudWidget widget) {
		Minecraft client = Minecraft.getInstance();
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int width = (int) (widgetWidth(Minecraft.getInstance(), widget, widgetText.textFor(widget)) *
				position.scale / 100.0);
		int height = (int) (widgetHeight(widget) * position.scale / 100.0);
		position.x = Math.clamp(position.x, 0, Math.max(0, screenWidth - width));
		position.y = Math.clamp(position.y, 0, Math.max(0, screenHeight - height));
	}

	private void changeScale(HudConfig.WidgetPosition position, int amount) {
		position.scale = Math.clamp(position.scale + amount, MIN_SCALE, MAX_SCALE);
		clampPosition(position, widgets[selected]);
		config.save();
	}

	private void changeOpacity(HudConfig.WidgetPosition position, int amount) {
		position.opacity = Math.clamp(position.opacity + amount, 0, 100);
		config.save();
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int boxWidth, int boxHeight) {
		return mouseX >= x && mouseX <= x + boxWidth && mouseY >= y && mouseY <= y + boxHeight;
	}

	@Override
	public void onClose() {
		config.save();
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return parent instanceof PauseScreen ||
				parent instanceof HudStudioSelectorScreen selector && selector.isPauseScreen();
	}

	@FunctionalInterface
	interface WidgetText {
		String textFor(HudWidget widget);
	}
}
