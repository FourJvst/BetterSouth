package de.bettersouth.hud;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

final class SpotifyCompanion {
	private static final System.Logger LOGGER = System.getLogger(SpotifyCompanion.class.getName());
	private static final AtomicReference<Track> TRACK = new AtomicReference<>(Track.EMPTY);
	private static final Path DIRECTORY = Path.of("config", "bettersouth-hud", "spotify");
	private static final Path EXECUTABLE = DIRECTORY.resolve("bettersouth-spotify-helper.exe");
	private static final Path STATE_FILE = DIRECTORY.resolve("now-playing.txt");
	private static final Path COVER_FILE = DIRECTORY.resolve("cover.img");
	private static final Path COMMAND_DIRECTORY = DIRECTORY.resolve("commands");
	private static final Identifier COVER_TEXTURE =
			Identifier.fromNamespaceAndPath("bettersouth_hud", "spotify/cover");
	private static final AtomicLong COMMAND_SEQUENCE = new AtomicLong();
	private static Process process;
	private static long lastModified = Long.MIN_VALUE;
	private static long lastCoverModified = Long.MIN_VALUE;
	private static int ticksUntilPoll;
	private static boolean coverLoaded;

	private SpotifyCompanion() {
	}

	static void start() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		if (!os.contains("win") || !(architecture.contains("amd64") || architecture.contains("x86_64"))) {
			LOGGER.log(System.Logger.Level.INFO,
					"Spotify-Medienoverlay ist nur unter Windows x64 verfügbar.");
			return;
		}

		try {
			Files.createDirectories(DIRECTORY);
			Files.createDirectories(COMMAND_DIRECTORY);
			extractHelper();
			Files.deleteIfExists(STATE_FILE);
			Files.deleteIfExists(COVER_FILE);
			coverLoaded = false;
			try (var commands = Files.list(COMMAND_DIRECTORY)) {
				for (Path command : commands.toList()) {
					Files.deleteIfExists(command);
				}
			}
			Path logFile = DIRECTORY.resolve("helper.log");
			process = new ProcessBuilder(EXECUTABLE.toAbsolutePath().toString(), STATE_FILE.toAbsolutePath().toString())
					.redirectErrorStream(true)
					.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
					.start();
			Runtime.getRuntime().addShutdownHook(new Thread(SpotifyCompanion::stop, "bettersouth-spotify-cleanup"));
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.ERROR,
					"Spotify-Begleiter konnte nicht gestartet werden; Details stehen in der Minecraft-Konsole.",
					exception);
			TRACK.set(new Track("Spotify-Begleiter konnte nicht gestartet werden", "", ""));
		}
	}

	static void control(String command) {
		if (process == null || !process.isAlive()) {
			LOGGER.log(System.Logger.Level.WARNING,
					"Spotify-Steuerung ist nicht verfügbar, weil der Medienbegleiter nicht läuft.");
			return;
		}
		if (!"previous".equals(command) && !"toggle".equals(command) && !"next".equals(command)) {
			throw new IllegalArgumentException("Unknown Spotify command: " + command);
		}
		try {
			long sequence = COMMAND_SEQUENCE.incrementAndGet();
			Path commandFile = COMMAND_DIRECTORY.resolve(String.format(Locale.ROOT, "%020d.cmd", sequence));
			Path temporaryFile = COMMAND_DIRECTORY.resolve(commandFile.getFileName() + ".tmp");
			Files.writeString(temporaryFile, command, StandardCharsets.US_ASCII);
			try {
				Files.move(temporaryFile, commandFile, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
				Files.move(temporaryFile, commandFile);
			}
		} catch (IOException exception) {
			LOGGER.log(System.Logger.Level.ERROR, "Spotify-Befehl konnte nicht übergeben werden", exception);
		}
	}

	static void poll() {
		if (process == null || ++ticksUntilPoll < 20) {
			return;
		}
		ticksUntilPoll = 0;
		if (!process.isAlive()) {
			TRACK.set(new Track("Spotify-Begleiter wurde beendet", "", ""));
			return;
		}
		try {
			if (Files.isRegularFile(STATE_FILE)) {
				long modified = Files.getLastModifiedTime(STATE_FILE).toMillis();
				if (modified != lastModified) {
					List<String> lines = Files.readAllLines(STATE_FILE, StandardCharsets.UTF_8);
					if (lines.size() != 3) {
						throw new IOException("Unerwartetes Spotify-Statusformat");
					}
					TRACK.set(new Track(decode(lines.get(0)), decode(lines.get(1)), decode(lines.get(2))));
					lastModified = modified;
				}
			}
			refreshCover();
		} catch (IOException | IllegalArgumentException exception) {
			LOGGER.log(System.Logger.Level.WARNING, "Spotify-Titelinformationen konnten nicht gelesen werden", exception);
		}
	}

	private static void refreshCover() throws IOException {
		if (!Files.isRegularFile(COVER_FILE)) {
			if (coverLoaded) {
				Minecraft.getInstance().getTextureManager().release(COVER_TEXTURE);
				coverLoaded = false;
			}
			lastCoverModified = Long.MIN_VALUE;
			return;
		}

		long modified = Files.getLastModifiedTime(COVER_FILE).toMillis();
		if (modified == lastCoverModified) {
			return;
		}
		NativeImage image;
		try (InputStream input = Files.newInputStream(COVER_FILE)) {
			image = NativeImage.read(input);
		}
		Minecraft client = Minecraft.getInstance();
		DynamicTexture texture = new DynamicTexture(() -> "BetterSouth Spotify cover", image);
		if (coverLoaded) {
			client.getTextureManager().release(COVER_TEXTURE);
		}
		client.getTextureManager().register(COVER_TEXTURE, texture);
		coverLoaded = true;
		lastCoverModified = modified;
	}

	static Identifier coverTexture() {
		return coverLoaded ? COVER_TEXTURE : null;
	}

	static String displayText() {
		Track track = TRACK.get();
		if (track.title.isBlank()) {
			return "Nichts wird wiedergegeben";
		}
		String prefix = "PAUSED".equalsIgnoreCase(track.playback) ? "⏸ " : "";
		return track.artist.isBlank()
				? prefix + track.title
				: prefix + track.title + " — " + track.artist;
	}

	static boolean isPlaying() {
		return "PLAYING".equalsIgnoreCase(TRACK.get().playback);
	}

	static void stop() {
		Process running = process;
		process = null;
		if (running != null && running.isAlive()) {
			running.destroy();
			try {
				if (!running.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
					running.destroyForcibly();
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				running.destroyForcibly();
			}
		}
	}

	private static void extractHelper() throws IOException {
		Path temporary = DIRECTORY.resolve("bettersouth-spotify-helper.exe.tmp");
		try (InputStream input = SpotifyCompanion.class.getResourceAsStream(
				"/assets/bettersouth_hud/bettersouth-spotify-helper.exe")) {
			if (input == null) {
				throw new IOException("Das eingebettete Windows-Spotify-Hilfsprogramm fehlt im Mod-JAR");
			}
			Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
		}
		try {
			Files.move(temporary, EXECUTABLE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException exception) {
			Files.move(temporary, EXECUTABLE, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String decode(String encoded) {
		if (encoded.isEmpty()) {
			return "";
		}
		return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
	}

	private record Track(String title, String artist, String playback) {
		private static final Track EMPTY = new Track("", "", "");
	}
}
