package de.bettersouth.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class HudStudioSelectorScreen extends Screen {
	private static final int PANEL_WIDTH = 520;
	private static final int PANEL_HEIGHT = 250;
	private final HudConfig config;
	private final net.minecraft.client.KeyMapping[] keyMappings;
	private final HudSettingsScreen.WidgetText widgetText;
	private final Screen parent;
	private final int selectedWidget;
	private float uiScale;
	private float uiOffsetX;
	private float uiOffsetY;

	HudStudioSelectorScreen(
			HudConfig config,
			net.minecraft.client.KeyMapping[] keyMappings,
			HudSettingsScreen.WidgetText widgetText,
			int selectedWidget,
			Screen parent
	) {
		super(Component.literal("BetterSouth Studio"));
		this.config = config;
		this.keyMappings = keyMappings;
		this.widgetText = widgetText;
		this.selectedWidget = selectedWidget;
		this.parent = parent;
	}

	@Override
	protected void init() {
		uiScale = Math.min(1.0f, Math.min((width - 24.0f) / PANEL_WIDTH, (height - 24.0f) / PANEL_HEIGHT));
		uiScale = Math.max(0.45f, uiScale);
		uiOffsetX = (width - PANEL_WIDTH * uiScale) / 2.0f;
		uiOffsetY = (height - PANEL_HEIGHT * uiScale) / 2.0f;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0x66070A11);
		graphics.pose().pushMatrix();
		graphics.pose().translate(uiOffsetX, uiOffsetY);
		graphics.pose().scale(uiScale, uiScale);

		graphics.fill(14, 18, PANEL_WIDTH + 14, PANEL_HEIGHT + 18, 0x70000000);
		graphics.fill(0, 0, PANEL_WIDTH, PANEL_HEIGHT, 0xD2141925);
		graphics.fill(0, 0, PANEL_WIDTH, 3, 0xFF44D7C8);
		graphics.fill(0, 49, PANEL_WIDTH, 50, 0xFF30394A);
		graphics.text(font, "BETTERSOUTH", 22, 15, 0xFF57E0D0);
		graphics.text(font, "STUDIO-AUSWAHL", 143, 15, 0xFFF1F5FB);

		int closeX = PANEL_WIDTH - 130;
		int closeY = 11;
		boolean closeHovered = inside(toLogicalX(mouseX), toLogicalY(mouseY), closeX, closeY, 112, 27);
		graphics.fill(closeX, closeY, closeX + 112, closeY + 27,
				closeHovered ? 0xFF3A4658 : 0xFF232D3B);
		graphics.text(font, "SCHLIESSEN  X", closeX + 15, closeY + 9,
				closeHovered ? 0xFFFFFFFF : 0xFFB8C4D5);

		graphics.text(font, "Was moechtest du bearbeiten?", 22, 68, 0xFFC4CEDC);
		drawChoice(graphics, 22, 100, 228, "WIDGETS", "Position, Groesse und Design", true,
				inside(toLogicalX(mouseX), toLogicalY(mouseY), 22, 100, 228, 112));
		drawChoice(graphics, 270, 100, 228, "HOTKEYS", "Befehle und Nachrichten", false,
				inside(toLogicalX(mouseX), toLogicalY(mouseY), 270, 100, 228, 112));
		graphics.text(font, "Esc schliesst dieses Fenster", 22, PANEL_HEIGHT - 27, 0xFF8290A4);
		graphics.pose().popMatrix();
	}

	private void drawChoice(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int cardWidth,
			String title,
			String description,
			boolean widgets,
			boolean hovered
	) {
		int background = hovered ? 0xE0344657 : 0xD0232D3B;
		int accent = widgets ? 0xFF57E0D0 : 0xFFB18CFF;
		graphics.fill(x, y, x + cardWidth, y + 112, background);
		graphics.fill(x, y, x + 3, y + 112, accent);
		graphics.fill(x + 15, y + 16, x + 48, y + 49, 0xFF33495A);
		graphics.text(font, widgets ? "W" : "K", x + 27, y + 28, accent);
		graphics.text(font, title, x + 61, y + 22, 0xFFF1F5FB);
		graphics.text(font, description, x + 61, y + 42, 0xFFB8C4D5);
		graphics.fill(x + 15, y + 77, x + cardWidth - 15, y + 78, 0xFF465365);
		graphics.text(font, "Zum Oeffnen klicken  >", x + 17, y + 88, accent);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		double mouseX = toLogicalX(event.x());
		double mouseY = toLogicalY(event.y());
		if (inside(mouseX, mouseY, PANEL_WIDTH - 130, 11, 112, 27)) {
			onClose();
			return true;
		}
		if (inside(mouseX, mouseY, 22, 100, 228, 112)) {
			Minecraft.getInstance().gui.setScreen(new HudSettingsScreen(
					config, selectedWidget, widgetText, this));
			return true;
		}
		if (inside(mouseX, mouseY, 270, 100, 228, 112)) {
			Minecraft.getInstance().gui.setScreen(new HotkeySettingsScreen(config, keyMappings, this));
			return true;
		}
		return true;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return parent instanceof PauseScreen;
	}

	private double toLogicalX(double x) {
		return (x - uiOffsetX) / uiScale;
	}

	private double toLogicalY(double y) {
		return (y - uiOffsetY) / uiScale;
	}

	private static boolean inside(double x, double y, int boxX, int boxY, int boxWidth, int boxHeight) {
		return x >= boxX && x <= boxX + boxWidth && y >= boxY && y <= boxY + boxHeight;
	}
}
