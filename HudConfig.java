package de.bettersouth.hud;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.lwjgl.glfw.GLFW;

final class HudConfig {
	private static final Path FILE = Path.of("config", "bettersouth-hud.properties");
	private static final System.Logger LOGGER = System.getLogger(HudConfig.class.getName());
	static final int HOTKEY_COUNT = 9;
	final WidgetPosition[] positions = new WidgetPosition[HudWidget.values().length];
	final HotkeyEntry[] hotkeys = new HotkeyEntry[HOTKEY_COUNT];

	HudConfig() {
		for (HudWidget widget : HudWidget.values()) {
			positions[widget.ordinal()] = new WidgetPosition(widget.defaultX, widget.defaultY, true);
		}
		for (int i = 0; i < hotkeys.length; i++) {
			hotkeys[i] = new HotkeyEntry(i, GLFW.GLFW_KEY_F6 + i);
		}
		load();
	}

	void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(FILE)) {
			properties.load(input);
			for (HudWidget widget : HudWidget.values()) {
				WidgetPosition position = positions[widget.ordinal()];
				String key = widget.name().toLowerCase();
				position.x = getInt(properties, key + ".x", position.x);
				position.y = getInt(properties, key + ".y", position.y);
				position.scale = Math.clamp(getInt(properties, key + ".scale", position.scale), 50, 200);
				position.enabled = Boolean.parseBoolean(properties.getProperty(key + ".enabled", "true"));
				position.backgroundEnabled = Boolean.parseBoolean(
						properties.getProperty(key + ".background", "true"));
				position.opacity = Math.clamp(getInt(properties, key + ".opacity", position.opacity), 0, 100);
				position.borderEnabled = Boolean.parseBoolean(properties.getProperty(key + ".border", "true"));
				position.iconEnabled = Boolean.parseBoolean(properties.getProperty(key + ".icon", "true"));
				position.accentTheme = Math.clamp(getInt(properties, key + ".accent", position.accentTheme), 0, 2);
			}
			for (int i = 0; i < hotkeys.length; i++) {
				HotkeyEntry hotkey = hotkeys[i];
				String key = "hotkey." + (i + 1) + ".";
				hotkey.name = properties.getProperty(key + "name", hotkey.name);
				hotkey.action = properties.getProperty(key + "action", hotkey.action);
				hotkey.enabled = Boolean.parseBoolean(properties.getProperty(key + "enabled", "false"));
				hotkey.command = Boolean.parseBoolean(properties.getProperty(key + "command", "true"));
				hotkey.keyCode = Math.max(0, getInt(properties, key + "key", hotkey.keyCode));
			}
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.ERROR,
					"BetterSouth-HUD-Konfiguration konnte nicht gelesen werden; Standardwerte werden verwendet",
					exception);
		}
	}

	void save() {
		Properties properties = new Properties();
		for (HudWidget widget : HudWidget.values()) {
			WidgetPosition position = positions[widget.ordinal()];
			String key = widget.name().toLowerCase();
			properties.setProperty(key + ".x", Integer.toString(position.x));
			properties.setProperty(key + ".y", Integer.toString(position.y));
			properties.setProperty(key + ".scale", Integer.toString(position.scale));
			properties.setProperty(key + ".enabled", Boolean.toString(position.enabled));
			properties.setProperty(key + ".background", Boolean.toString(position.backgroundEnabled));
			properties.setProperty(key + ".opacity", Integer.toString(position.opacity));
			properties.setProperty(key + ".border", Boolean.toString(position.borderEnabled));
			properties.setProperty(key + ".icon", Boolean.toString(position.iconEnabled));
			properties.setProperty(key + ".accent", Integer.toString(position.accentTheme));
		}
		for (int i = 0; i < hotkeys.length; i++) {
			HotkeyEntry hotkey = hotkeys[i];
			String key = "hotkey." + (i + 1) + ".";
			properties.setProperty(key + "name", hotkey.name);
			properties.setProperty(key + "action", hotkey.action);
			properties.setProperty(key + "enabled", Boolean.toString(hotkey.enabled));
			properties.setProperty(key + "command", Boolean.toString(hotkey.command));
			properties.setProperty(key + "key", Integer.toString(hotkey.keyCode));
		}
		try {
			Files.createDirectories(FILE.getParent());
			try (OutputStream output = Files.newOutputStream(FILE)) {
				properties.store(output, "BetterSouth HUD widget positions and visibility");
			}
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.ERROR,
					"BetterSouth-HUD-Konfiguration konnte nicht gespeichert werden", exception);
		}
	}

	private static int getInt(Properties properties, String key, int fallback) {
		String value = properties.getProperty(key);
		if (value == null) {
			return fallback;
		}
		try {
			return Math.max(0, Integer.parseInt(value));
		} catch (NumberFormatException exception) {
			LOGGER.log(System.Logger.Level.WARNING,
					"Ungültiger HUD-Konfigurationswert für {0}: {1}", key, value);
			return fallback;
		}
	}

	static final class WidgetPosition {
		int x;
		int y;
		int scale = 100;
		boolean enabled;
		boolean backgroundEnabled = true;
		int opacity = 92;
		boolean borderEnabled = true;
		boolean iconEnabled = true;
		int accentTheme;

		WidgetPosition(int x, int y, boolean enabled) {
			this.x = x;
			this.y = y;
			this.enabled = enabled;
		}
	}

	static final class HotkeyEntry {
		String name;
		String action = "";
		boolean enabled;
		boolean command = true;
		int keyCode;

		HotkeyEntry(int slot, int keyCode) {
			this.keyCode = keyCode;
			this.name = "Hotkey " + (slot + 1);
		}
	}
}
