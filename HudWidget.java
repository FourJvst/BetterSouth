package de.bettersouth.hud;

enum HudWidget {
	MONEY("Geld", "$", 8, 24),
	WANTED("Wanted", "!", 8, 52),
	DRUGS("Drogen", "D", 8, 80),
	FACTION("Fraktion", "F", 8, 108),
	RANK("Rang", "R", 8, 136),
	FPS("FPS", "F", 8, 164),
	PLAYTIME("Spielzeit", "T", 8, 192),
	SPOTIFY("Spotify", "♪", 8, 220),
	KEYSTROKES("Tasten", "⌨", 8, 276);

	final String label;
	final String icon;
	final int defaultX;
	final int defaultY;

	HudWidget(String label, String icon, int defaultX, int defaultY) {
		this.label = label;
		this.icon = icon;
		this.defaultX = defaultX;
		this.defaultY = defaultY;
	}
}
