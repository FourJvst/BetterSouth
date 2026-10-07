package de.bettersouth.hud;

import com.mojang.blaze3d.platform.InputConstants;
import de.bettersouth.hud.mixin.ScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class BetterSouthHudClient implements ClientModInitializer {
	private static final HudConfig CONFIG = new HudConfig();
	private static HudPayload serverData;
	private static int selectedWidget;

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bettersouth_hud", "main"));
	private static final KeyMapping EDIT_KEY = key("key.bettersouth_hud.edit", GLFW.GLFW_KEY_H);
	private static final KeyMapping HOTKEY_MENU_KEY = key("key.bettersouth_hud.hotkeys", GLFW.GLFW_KEY_K);
	private static final KeyMapping EMOTE_WHEEL_KEY = key("key.bettersouth_hud.emotes", GLFW.GLFW_KEY_G);
	private static final KeyMapping[] HOTKEY_KEYS = createHotkeyKeys();

	@Override
	public void onInitializeClient() {
		PayloadTypeRegistry.clientboundPlay().register(HudPayload.TYPE, HudPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PresencePayload.TYPE, PresencePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(EmotePayload.TYPE, EmotePayload.CODEC);
		ClientPlayNetworking.registerGlobalReceiver(HudPayload.TYPE, (payload, context) -> serverData = payload);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (ClientPlayNetworking.canSend(PresencePayload.TYPE)) {
				ClientPlayNetworking.send(new PresencePayload());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverData = null);
		SpotifyCompanion.start();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SpotifyCompanion.stop());
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			Keystrokes.tick(client);
			handleKeys(client);
			SpotifyCompanion.poll();
		});
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof PauseScreen) {
				PauseButtonBounds bounds = hudSettingsButtonBounds(screen);
				if (bounds != null) {
					Button hudButton = Button.builder(Component.literal("BetterSouth HUD"), button ->
									openStudioSelector(client, screen))
							.bounds(bounds.x, bounds.y - bounds.height - 4, bounds.width, bounds.height)
							.build();
					((ScreenAccessor) screen).bettersouth$addRenderableWidget(hudButton);
				}
				return;
			}
			if (screen instanceof HudSettingsScreen) {
				return;
			}
			ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, event) -> {
				if (client.player == null || event.button() != 0) {
					return true;
				}
				String control = spotifyControlAt(event.x(), event.y());
				if (control == null) {
					return true;
				}
				SpotifyCompanion.control(control);
				return false;
			});
		});
		HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath("bettersouth_hud", "widgets"),
				BetterSouthHudClient::render
		);
	}

	private static PauseButtonBounds hudSettingsButtonBounds(Screen screen) {
		String returnLabel = Component.translatable("menu.returnToGame").getString();
		for (var child : screen.children()) {
			if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(returnLabel)) {
				return new PauseButtonBounds(widget.getX(), widget.getY() - widget.getHeight() - 4,
						widget.getWidth(), widget.getHeight());
			}
		}
		return null;
	}

	private record PauseButtonBounds(int x, int y, int width, int height) {
	}

	private static KeyMapping key(String name, int keyCode) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				name,
				InputConstants.Type.KEYSYM,
				keyCode,
				CATEGORY
		));
	}

	private static void handleKeys(Minecraft client) {
		boolean screenOpen = client.gui.screen() != null;
		if (screenOpen) {
			EDIT_KEY.consumeClick();
			HOTKEY_MENU_KEY.consumeClick();
			EMOTE_WHEEL_KEY.consumeClick();
			for (KeyMapping key : HOTKEY_KEYS) {
				key.consumeClick();
			}
			return;
		}
		if (client.player == null) {
			return;
		}
		if (EDIT_KEY.consumeClick()) {
			openStudioSelector(client, null);
		}
		if (HOTKEY_MENU_KEY.consumeClick()) {
			client.gui.setScreen(new HotkeySettingsScreen(CONFIG, HOTKEY_KEYS));
		}
		if (EMOTE_WHEEL_KEY.consumeClick()) {
			client.gui.setScreen(new EmoteWheelScreen(
					emoteId -> ClientPlayNetworking.send(new EmotePayload(emoteId)),
					() -> ClientPlayNetworking.canSend(EmotePayload.TYPE)));
		}
		for (int i = 0; i < HOTKEY_KEYS.length; i++) {
			if (HOTKEY_KEYS[i].consumeClick()) {
				sendHotkey(client, CONFIG.hotkeys[i]);
			}
		}
	}

	private static void openStudioSelector(Minecraft client, Screen parent) {
		client.gui.setScreen(new HudStudioSelectorScreen(CONFIG, HOTKEY_KEYS,
				widget -> valueFor(widget, client), selectedWidget, parent));
	}

	private static KeyMapping[] createHotkeyKeys() {
		KeyMapping[] mappings = new KeyMapping[HudConfig.HOTKEY_COUNT];
		for (int i = 0; i < mappings.length; i++) {
			int defaultKey = GLFW.GLFW_KEY_F6 + i;
			mappings[i] = key("key.bettersouth_hud.hotkey_" + (i + 1), defaultKey);
			mappings[i].setKey(InputConstants.Type.KEYSYM.getOrCreate(CONFIG.hotkeys[i].keyCode));
		}
		KeyMapping.resetMapping();
		return mappings;
	}

	private static void sendHotkey(Minecraft client, HudConfig.HotkeyEntry hotkey) {
		if (!hotkey.enabled || hotkey.action.isBlank() || client.player == null) {
			return;
		}
		if (hotkey.command) {
			String command = hotkey.action.startsWith("/")
					? hotkey.action.substring(1).trim()
					: hotkey.action.trim();
			if (!command.isEmpty()) {
				client.player.connection.sendCommand(command);
			}
		} else {
			client.player.connection.sendChat(hotkey.action);
		}
	}

	private static void render(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		for (HudWidget widget : HudWidget.values()) {
			HudConfig.WidgetPosition position = CONFIG.positions[widget.ordinal()];
			if (!position.enabled) {
				continue;
			}
			String text = widget.label + ": " + valueFor(widget, client);
			HudSettingsScreen.drawHudWidget(graphics, widget, text, position.x, position.y,
					position.scale / 100.0f, false, position);
		}
	}

	private static String valueFor(HudWidget widget, Minecraft client) {
		return switch (widget) {
			case MONEY -> serverData == null ? "--" : serverData.balance();
			case WANTED -> serverData == null ? "--" : Integer.toString(serverData.wanted());
			case DRUGS -> serverData == null || serverData.drugs() < 0
					? "nicht konfiguriert"
					: Integer.toString(serverData.drugs());
			case FACTION -> serverData == null || serverData.faction().isBlank()
					? serverData == null ? "--" : "Keine"
					: serverData.faction();
			case RANK -> serverData == null || serverData.rank().isBlank()
					? serverData == null ? "--" : "Keiner"
					: serverData.rank();
			case FPS -> Integer.toString(client.getFps());
			case PLAYTIME -> serverData == null ? "--" : formatPlaytime(serverData.playtimeSeconds());
			case SPOTIFY -> SpotifyCompanion.displayText();
			case KEYSTROKES -> "";
		};
	}

	private static String spotifyControlAt(double mouseX, double mouseY) {
		HudConfig.WidgetPosition position = CONFIG.positions[HudWidget.SPOTIFY.ordinal()];
		if (!position.enabled) {
			return null;
		}
		double scale = position.scale / 100.0;
		double localX = (mouseX - position.x) / scale;
		double localY = (mouseY - position.y) / scale;
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

	private static String formatPlaytime(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long days = hours / 24;
		hours %= 24;
		return days > 0
				? days + "T " + hours + "h"
				: hours + "h " + minutes + "m";
	}
}
