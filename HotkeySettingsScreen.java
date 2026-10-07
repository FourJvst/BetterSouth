package de.bettersouth.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class HotkeySettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 740;
	private static final int PANEL_HEIGHT = 424;
	private static final int SIDEBAR_WIDTH = 214;
	private static final int ROW_HEIGHT = 36;
	private static final int MAX_ACTION_LENGTH = 256;

	private final HudConfig config;
	private final KeyMapping[] keyMappings;
	private final Screen parent;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int selected;
	private int capturingSlot = -1;
	private float uiScale;
	private float uiOffsetX;
	private float uiOffsetY;
	private String notice = "Deine Hotkeys werden nur an den verbundenen Server gesendet.";
	private EditBox nameField;
	private EditBox actionField;

	HotkeySettingsScreen(HudConfig config, KeyMapping[] keyMappings) {
		this(config, keyMappings, null);
	}

	HotkeySettingsScreen(HudConfig config, KeyMapping[] keyMappings, Screen parent) {
		super(Component.literal("BetterSouth Hotkeys"));
		this.config = config;
		this.keyMappings = keyMappings;
		this.parent = parent;
	}

	@Override
	protected void init() {
		uiScale = Math.min(1.0f, Math.min((width - 20.0f) / PANEL_WIDTH, (height - 20.0f) / PANEL_HEIGHT));
		uiScale = Math.max(0.45f, uiScale);
		uiOffsetX = (width - PANEL_WIDTH * uiScale) / 2.0f;
		uiOffsetY = (height - PANEL_HEIGHT * uiScale) / 2.0f;
		panelX = 0;
		panelY = 0;
		panelWidth = PANEL_WIDTH;
		panelHeight = PANEL_HEIGHT;

		int editorX = panelX + SIDEBAR_WIDTH + 26;
		int fieldWidth = panelWidth - SIDEBAR_WIDTH - 54;
		nameField = addRenderableWidget(new EditBox(font, editorX, panelY + 115, fieldWidth, 25,
				Component.literal("Hotkey-Name")));
		nameField.setMaxLength(24);
		nameField.setHint(Component.literal("z. B. Polizei rufen"));
		actionField = addRenderableWidget(new EditBox(font, editorX, panelY + 179, fieldWidth, 29,
				Component.literal("Befehl oder Nachricht")));
		actionField.setMaxLength(MAX_ACTION_LENGTH);
		actionField.setHint(Component.literal("z. B. /help oder Ich brauche Hilfe!"));
		positionField(nameField, editorX, panelY + 115, fieldWidth, 25);
		positionField(actionField, editorX, panelY + 179, fieldWidth, 29);
		loadSelected();
	}

	private void positionField(EditBox field, int x, int y, int fieldWidth, int fieldHeight) {
		field.setX(Math.round(uiOffsetX + x * uiScale));
		field.setY(Math.round(uiOffsetY + y * uiScale));
		field.setWidth(Math.max(60, Math.round(fieldWidth * uiScale)));
		field.setHeight(Math.max(16, Math.round(fieldHeight * uiScale)));
	}

	private void loadSelected() {
		HudConfig.HotkeyEntry entry = config.hotkeys[selected];
		nameField.setValue(entry.name);
		actionField.setValue(entry.action);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xB8070A11);
		graphics.pose().pushMatrix();
		graphics.pose().translate(uiOffsetX, uiOffsetY);
		graphics.pose().scale(uiScale, uiScale);

		graphics.fill(panelX + 5, panelY + 6, panelX + panelWidth + 5, panelY + panelHeight + 6, 0x76000000);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF2141925);
		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, 0xFF49DCC7);
		graphics.fill(panelX, panelY + 50, panelX + panelWidth, panelY + 51, 0xFF30394A);
		graphics.text(font, "BETTERSOUTH", 22, 14, 0xFF57E0D0);
		graphics.text(font, "HOTKEY STUDIO", 149, 14, 0xFFF1F5FB);
		graphics.text(font, "K  HOTKEYS  /  ESC  SCHLIESSEN", panelWidth - 240, 15, 0xFF8996A9);

		drawSidebar(graphics);
		drawEditor(graphics);
		graphics.pose().popMatrix();
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	private void drawSidebar(GuiGraphicsExtractor graphics) {
		int sidebarX = panelX + 12;
		int sidebarY = panelY + 60;
		graphics.fill(sidebarX, sidebarY, sidebarX + SIDEBAR_WIDTH, panelY + panelHeight - 14, 0xFF191F2D);
		graphics.text(font, "DEINE SHORTCUTS", sidebarX + 12, sidebarY + 11, 0xFF9BA9BC);
		for (int index = 0; index < config.hotkeys.length; index++) {
			HudConfig.HotkeyEntry entry = config.hotkeys[index];
			int rowX = sidebarX + 7;
			int rowY = sidebarY + 34 + index * ROW_HEIGHT;
			boolean active = selected == index;
			graphics.fill(rowX, rowY, sidebarX + SIDEBAR_WIDTH - 7, rowY + ROW_HEIGHT - 3,
					active ? 0xFF2A3A4B : 0xFF202735);
			if (active) {
				graphics.fill(rowX, rowY + 4, rowX + 2, rowY + ROW_HEIGHT - 7, 0xFF57E0D0);
			}
			String label = (index + 1) + ". " + entry.name;
			graphics.text(font, truncate(label, 20), rowX + 9, rowY + 7,
					entry.enabled ? 0xFFF0F4FA : 0xFF9AA6B7);
			String keyName = KeyMappingHelper.getBoundKeyOf(keyMappings[index]).getDisplayName().getString();
			int badgeWidth = Math.max(38, font.width(keyName) + 14);
			int badgeX = sidebarX + SIDEBAR_WIDTH - badgeWidth - 14;
			graphics.fill(badgeX, rowY + 5, badgeX + badgeWidth, rowY + 24,
					capturingSlot == index ? 0xFF8B6120 : 0xFF33495A);
			graphics.text(font, keyName, badgeX + (badgeWidth - font.width(keyName)) / 2,
					rowY + 10, 0xFFB6F3E8);
		}
	}

	private void drawEditor(GuiGraphicsExtractor graphics) {
		int x = panelX + SIDEBAR_WIDTH + 28;
		int right = panelX + panelWidth - 24;
		int width = right - x;
		HudConfig.HotkeyEntry entry = config.hotkeys[selected];

		graphics.text(font, "HOTKEY EINRICHTEN", x, panelY + 67, 0xFF57E0D0);
		graphics.text(font, "Bearbeite deinen Shortcut und speichere ihn fuer das naechste Mal.",
				x, panelY + 84, 0xFF9BA9BC);

		graphics.text(font, "NAME", x, panelY + 105, 0xFFCAD4E1);
		graphics.fill(x - 1, panelY + 113, right, panelY + 142, 0xFF101722);
		graphics.text(font, "AKTION", x, panelY + 162, 0xFFCAD4E1);
		graphics.fill(x - 1, panelY + 177, right, panelY + 210, 0xFF101722);

		graphics.text(font, "WAS SOLL GESENDET WERDEN?", x, panelY + 223, 0xFFCAD4E1);
		int modeY = panelY + 237;
		drawModeButton(graphics, x, modeY, 145, 31, "BEFEHL", entry.command);
		drawModeButton(graphics, x + 154, modeY, 145, 31, "CHAT-NACHRICHT", !entry.command);
		graphics.text(font, entry.command
						? "Wird als normaler Spielerbefehl an den Server gesendet."
						: "Wird wie eine normale Chatnachricht gesendet.",
				x, modeY + 39, 0xFF8290A4);

		int bindY = panelY + 296;
		graphics.fill(x, bindY, x + width, bindY + 43, 0xFF1D2734);
		graphics.text(font, "TASTENBELEGUNG", x + 12, bindY + 8, 0xFFEAF0F7);
		graphics.text(font, capturingSlot == selected
						? "Taste druecken...  (Esc zum Abbrechen)"
						: "Taste: " + KeyMappingHelper.getBoundKeyOf(keyMappings[selected])
								.getDisplayName().getString() + "   •   Klicken zum Aendern",
				x + 12, bindY + 25, capturingSlot == selected ? 0xFFFFD166 : 0xFF98A6B8);

		int buttonsY = panelY + panelHeight - 62;
		drawButton(graphics, x, buttonsY, 148, 34, entry.enabled ? "PAUSIEREN" : "AKTIVIEREN",
				entry.enabled ? 0xFF75404D : 0xFF176C60);
		drawButton(graphics, x + 158, buttonsY, 133, 34, "ZURUECKSETZEN", 0xFF394657);
		drawButton(graphics, right - 128, buttonsY, 128, 34, "SPEICHERN", 0xFF197A70);
		graphics.text(font, notice, x, buttonsY - 17, notice.startsWith("Gespeichert")
				? 0xFF8DEADD : 0xFFFFD166);
		graphics.text(font, "Taste auswaehlen und Shortcut druecken, um Befehl/Nachricht direkt zu senden.",
				x, panelY + panelHeight - 13, 0xFF7D899B);
	}

	private void drawModeButton(GuiGraphicsExtractor graphics, int x, int y, int buttonWidth, int buttonHeight,
			String label, boolean active) {
		graphics.fill(x, y, x + buttonWidth, y + buttonHeight, active ? 0xFF176C60 : 0xFF293241);
		if (active) {
			graphics.fill(x, y + buttonHeight - 2, x + buttonWidth, y + buttonHeight, 0xFF57E0D0);
		}
		graphics.text(font, label, x + 12, y + 11, active ? 0xFFFFFFFF : 0xFFADB9C9);
	}

	private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int buttonWidth, int buttonHeight,
			String label, int color) {
		graphics.fill(x, y, x + buttonWidth, y + buttonHeight, color);
		graphics.text(font, label, x + (buttonWidth - font.width(label)) / 2, y + 13, 0xFFF1F5FB);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = toLogicalX(event.x());
		double mouseY = toLogicalY(event.y());
		if (inside(mouseX, mouseY, panelWidth - 240, 10, 218, 30)) {
			onClose();
			return true;
		}
		int sidebarX = 12;
		int sidebarY = 60;
		for (int index = 0; index < config.hotkeys.length; index++) {
			int rowY = sidebarY + 34 + index * ROW_HEIGHT;
			if (inside(mouseX, mouseY, sidebarX + 7, rowY, SIDEBAR_WIDTH - 14, ROW_HEIGHT - 3)) {
				saveFields();
				selected = index;
				capturingSlot = -1;
				loadSelected();
				return true;
			}
		}

		int x = SIDEBAR_WIDTH + 28;
		int right = panelWidth - 24;
		int modeY = 237;
		if (inside(mouseX, mouseY, x, modeY, 145, 31)) {
			config.hotkeys[selected].command = true;
			notice = "Befehlsmodus ausgewaehlt.";
			return true;
		}
		if (inside(mouseX, mouseY, x + 154, modeY, 145, 31)) {
			config.hotkeys[selected].command = false;
			notice = "Nachrichtenmodus ausgewaehlt.";
			return true;
		}
		int bindY = 296;
		if (inside(mouseX, mouseY, x, bindY, right - x, 43)) {
			capturingSlot = selected;
			notice = "Taste druecken. Escape bricht die Tastenaufnahme ab.";
			return true;
		}
		int buttonsY = panelHeight - 62;
		if (inside(mouseX, mouseY, x, buttonsY, 148, 34)) {
			saveFields();
			config.hotkeys[selected].enabled = !config.hotkeys[selected].enabled;
			config.save();
			notice = config.hotkeys[selected].enabled ? "Hotkey aktiviert." : "Hotkey pausiert.";
			return true;
		}
		if (inside(mouseX, mouseY, x + 158, buttonsY, 133, 34)) {
			HudConfig.HotkeyEntry entry = config.hotkeys[selected];
			entry.name = "Hotkey " + (selected + 1);
			entry.action = "";
			entry.command = true;
			entry.enabled = false;
			actionField.setValue("");
			nameField.setValue(entry.name);
			config.save();
			notice = "Hotkey zurueckgesetzt.";
			return true;
		}
		if (inside(mouseX, mouseY, right - 128, buttonsY, 128, 34)) {
			saveFields();
			HudConfig.HotkeyEntry entry = config.hotkeys[selected];
			if (entry.action.isBlank()) {
				entry.enabled = false;
				notice = "Bitte zuerst einen Befehl oder eine Nachricht eintragen.";
			} else {
				entry.enabled = true;
				notice = "Gespeichert und aktiviert.";
			}
			config.save();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void saveFields() {
		HudConfig.HotkeyEntry entry = config.hotkeys[selected];
		String name = nameField.getValue().trim();
		entry.name = name.isEmpty() ? "Hotkey " + (selected + 1) : name;
		entry.action = actionField.getValue().trim();
		config.save();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (capturingSlot >= 0) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				capturingSlot = -1;
				notice = "Tastenaufnahme abgebrochen.";
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_UNKNOWN ||
					event.key() == GLFW.GLFW_KEY_K ||
					event.key() == GLFW.GLFW_KEY_H) {
				notice = "Diese Taste ist reserviert oder nicht verfuegbar.";
				return true;
			}
			int slot = capturingSlot;
			InputConstants.Key newKey = InputConstants.getKey(event);
			for (int i = 0; i < keyMappings.length; i++) {
				if (i != slot && KeyMappingHelper.getBoundKeyOf(keyMappings[i]).equals(newKey)) {
					notice = "Diese Taste ist bereits Hotkey " + (i + 1) + " zugewiesen.";
					return true;
				}
			}
			keyMappings[slot].setKey(newKey);
			config.hotkeys[slot].keyCode = newKey.getValue();
			KeyMapping.resetMapping();
			config.save();
			capturingSlot = -1;
			notice = "Taste gespeichert.";
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		return super.mouseDragged(event, deltaX, deltaY);
	}

	@Override
	public boolean isPauseScreen() {
		return parent instanceof PauseScreen ||
				parent instanceof HudStudioSelectorScreen selector && selector.isPauseScreen();
	}

	@Override
	public void onClose() {
		saveFields();
		Minecraft.getInstance().gui.setScreen(parent);
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

	private static String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
	}
}
