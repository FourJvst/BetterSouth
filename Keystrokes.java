package de.bettersouth.hud;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

final class Keystrokes {
	private static final int[] KEYS = {
			GLFW.GLFW_KEY_W,
			GLFW.GLFW_KEY_A,
			GLFW.GLFW_KEY_S,
			GLFW.GLFW_KEY_D,
			GLFW.GLFW_KEY_SPACE,
			GLFW.GLFW_KEY_LEFT_SHIFT,
			GLFW.GLFW_KEY_LEFT_CONTROL
	};
	private static volatile Set<Integer> pressed = Set.of();

	private Keystrokes() {
	}

	static void tick(Minecraft client) {
		com.mojang.blaze3d.platform.Window window = client.getWindow();
		if (client.gui.screen() != null || !window.isFocused()) {
			pressed = Set.of();
			return;
		}
		Set<Integer> current = new java.util.HashSet<>();
		for (int key : KEYS) {
			if (InputConstants.isKeyDown(window, key)) {
				current.add(key);
			}
		}
		pressed = Set.copyOf(current);
	}

	static boolean isPressed(int key) {
		return pressed.contains(key);
	}
}
