package de.bettersouth.hud;

import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class EmoteWheelScreen extends Screen {
	private static final int CARD_WIDTH = 132;
	private static final int CARD_HEIGHT = 48;
	private final Consumer<Integer> onSelect;
	private final BooleanSupplier canSend;
	private int hovered = -1;

	EmoteWheelScreen(Consumer<Integer> onSelect, BooleanSupplier canSend) {
		super(Component.literal("BetterSouth Emotes"));
		this.onSelect = onSelect;
		this.canSend = canSend;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, 0xB9080D16);
		int centerX = width / 2;
		int centerY = height / 2;
		hovered = cardAt(mouseX, mouseY, centerX, centerY);

		graphics.text(font, "BETTERSOUTH  /  EMOTE-WHEEL",
				centerX - 83, centerY - 143, 0xFF63E2D1);
		graphics.text(font, "Emote waehlen und anklicken",
				centerX - 78, centerY - 129, 0xFF98A7BA);

		drawCard(graphics, centerX - CARD_WIDTH / 2, centerY - 108, 0,
				"Winken", "Gruesse die anderen", "HI");
		drawCard(graphics, centerX + 28, centerY - CARD_HEIGHT / 2, 1,
				"Jubeln", "Feiere den Moment", "YAY");
		drawCard(graphics, centerX - CARD_WIDTH / 2, centerY + 60, 2,
				"Klatschen", "Applaus fuer alle", "GG");
		drawCard(graphics, centerX - CARD_WIDTH - 28, centerY - CARD_HEIGHT / 2, 3,
				"Tanzen", "Zeig deine Moves", "♪");

		graphics.fill(centerX - 39, centerY - 39, centerX + 39, centerY + 39, 0xFF111A27);
		graphics.fill(centerX - 35, centerY - 35, centerX + 35, centerY + 35, 0xFF1E3440);
		graphics.fill(centerX - 31, centerY - 31, centerX + 31, centerY + 31, 0xFF17232F);
		graphics.text(font, "EMOTE", centerX - 18, centerY - 5, 0xFFF0F6FC);
		graphics.text(font, "RAD", centerX - 11, centerY + 9, 0xFF63E2D1);

		String footer = canSend.getAsBoolean()
				? "ESC  Schliessen"
				: "Server-Bridge erforderlich";
		graphics.text(font, footer, centerX - font.width(footer) / 2,
				centerY + 125, canSend.getAsBoolean() ? 0xFF98A7BA : 0xFFFFA76B);
	}

	private void drawCard(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int emoteId,
			String title,
			String description,
			String icon
	) {
		boolean active = hovered == emoteId;
		int background = active ? 0xF1324D5D : 0xED1C2634;
		int accent = active ? 0xFF63E2D1 : 0xFF42566A;
		graphics.fill(x + 2, y + 3, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 3, 0x75000000);
		graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, background);
		graphics.fill(x, y + 4, x + 3, y + CARD_HEIGHT - 4, accent);
		graphics.fill(x + 10, y + 9, x + 42, y + 39, 0xFF273A49);
		graphics.text(font, icon, x + 16, y + 20, 0xFF9AF1E4);
		graphics.text(font, title, x + 50, y + 9, 0xFFF2F6FC);
		graphics.text(font, description, x + 50, y + 25, 0xFF9EACBE);
	}

	private static int cardAt(int mouseX, int mouseY, int centerX, int centerY) {
		if (inside(mouseX, mouseY, centerX - CARD_WIDTH / 2, centerY - 108)) {
			return 0;
		}
		if (inside(mouseX, mouseY, centerX + 28, centerY - CARD_HEIGHT / 2)) {
			return 1;
		}
		if (inside(mouseX, mouseY, centerX - CARD_WIDTH / 2, centerY + 60)) {
			return 2;
		}
		if (inside(mouseX, mouseY, centerX - CARD_WIDTH - 28, centerY - CARD_HEIGHT / 2)) {
			return 3;
		}
		return -1;
	}

	private static boolean inside(int mouseX, int mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0) {
			return super.mouseClicked(event, doubleClick);
		}
		int emoteId = cardAt((int) event.x(), (int) event.y(), width / 2, height / 2);
		if (emoteId >= 0 && canSend.getAsBoolean()) {
			onSelect.accept(emoteId);
			onClose();
		}
		return true;
	}
}
