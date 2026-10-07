package de.bettersouth.hudbridge;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BetterSouthHudBridge extends JavaPlugin implements Listener, PluginMessageListener {
	private static final String CHANNEL = "bettersouth:hud";
	private static final String PRESENCE_CHANNEL = "bettersouth:presence";
	private static final String EMOTE_CHANNEL = "bettersouth:emote";
	private static final long EMOTE_COOLDOWN_MILLIS = 1500L;

	private Plugin moneySystem;
	private Plugin factionSystem;
	private Plugin drugsPlugin;
	private Method cashBalanceMethod;
	private Method factionMethod;
	private Method rankMethod;
	private Method wantedMethod;
	private Method storedDrugAmountMethod;
	private Object[] drugTypes;
	private Field factionDisplayNameField;
	private Method factionRankNameMethod;
	private Set<Material> drugMaterials;
	private Set<String> drugNameFragments;
	private BukkitTask updateTask;
	private boolean channelRegistered;
	private final Map<UUID, Component> originalPlayerListNames = new HashMap<>();
	private final Map<UUID, Long> lastEmoteAt = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		loadDrugMatchers();
		getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
		getServer().getMessenger().registerIncomingPluginChannel(this, PRESENCE_CHANNEL, this);
		getServer().getMessenger().registerIncomingPluginChannel(this, EMOTE_CHANNEL, this);
		channelRegistered = true;
		getServer().getPluginManager().registerEvents(this, this);
		if (!resolvePluginApis()) {
			getLogger().severe("HUD-Daten sind deaktiviert, bis MoneySystem und Fraktionssystem mit kompatibler API geladen sind. Emotes bleiben verfügbar.");
			return;
		}
		long interval = Math.max(10L, getConfig().getLong("update-interval-ticks", 20L));
		updateTask = getServer().getScheduler().runTaskTimer(this, this::sendToOnlinePlayers, 1L, interval);
		if (storedDrugAmountMethod == null && drugMaterials.isEmpty() && drugNameFragments.isEmpty()) {
			getLogger().warning("Drogen-Erkennung ist noch nicht konfiguriert; das HUD zeigt dafür „nicht konfiguriert“.");
		}
	}

	@Override
	public void onDisable() {
		if (updateTask != null) {
			updateTask.cancel();
		}
		if (channelRegistered) {
			getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
			getServer().getMessenger().unregisterIncomingPluginChannel(this, PRESENCE_CHANNEL, this);
			getServer().getMessenger().unregisterIncomingPluginChannel(this, EMOTE_CHANNEL, this);
		}
		for (Player player : getServer().getOnlinePlayers()) {
			restorePlayerListName(player);
		}
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (EMOTE_CHANNEL.equals(channel)) {
			handleEmoteRequest(player, message);
			return;
		}
		if (!PRESENCE_CHANNEL.equals(channel) || message.length != 1 || message[0] != 1) {
			return;
		}
		if (originalPlayerListNames.containsKey(player.getUniqueId())) {
			return;
		}
		Component currentName = player.playerListName();
		originalPlayerListNames.put(player.getUniqueId(), currentName);
		Component baseName = currentName == null ? player.displayName() : currentName;
		player.playerListName(Component.text("✓ ", NamedTextColor.GREEN).append(baseName));
	}

	private void handleEmoteRequest(Player player, byte[] message) {
		if (message.length != 2 || message[0] != 1) {
			return;
		}
		int emoteId = Byte.toUnsignedInt(message[1]);
		if (emoteId > 3) {
			return;
		}
		long now = System.currentTimeMillis();
		long lastUsed = lastEmoteAt.getOrDefault(player.getUniqueId(), 0L);
		if (now - lastUsed < EMOTE_COOLDOWN_MILLIS) {
			return;
		}
		lastEmoteAt.put(player.getUniqueId(), now);
		playEmote(player, emoteId);
	}

	private void playEmote(Player player, int emoteId) {
		Location start = player.getLocation();
		float startYaw = start.getYaw();
		float startPitch = start.getPitch();
		int beats = switch (emoteId) {
			case 0 -> 4;
			case 1 -> 3;
			case 2, 3 -> 8;
			default -> throw new IllegalArgumentException("Unknown emote id: " + emoteId);
		};
		long period = emoteId < 2 ? 6L : 3L;
		if (emoteId == 1) {
			player.playSound(start, Sound.ENTITY_PLAYER_LEVELUP, 0.45F, 1.35F);
			spawnEmoteParticles(player, Particle.HAPPY_VILLAGER, 8);
		} else if (emoteId == 2) {
			player.playSound(start, Sound.BLOCK_NOTE_BLOCK_HAT, 0.65F, 1.2F);
		} else if (emoteId == 3) {
			player.playSound(start, Sound.BLOCK_NOTE_BLOCK_PLING, 0.4F, 1.15F);
			spawnEmoteParticles(player, Particle.NOTE, 5);
		}

		int[] beat = {0};
		getServer().getScheduler().runTaskTimer(this, task -> {
			if (!player.isOnline() || beat[0] >= beats) {
				if (emoteId == 3 && player.isOnline()) {
					player.setRotation(startYaw, startPitch);
				}
				task.cancel();
				return;
			}
			switch (emoteId) {
				case 0 -> player.swingMainHand();
				case 1 -> {
					player.swingMainHand();
					player.swingOffHand();
					spawnEmoteParticles(player, Particle.HAPPY_VILLAGER, 4);
				}
				case 2 -> {
					if ((beat[0] & 1) == 0) {
						player.swingMainHand();
					} else {
						player.swingOffHand();
					}
				}
				case 3 -> {
					if ((beat[0] & 1) == 0) {
						player.swingMainHand();
					} else {
						player.swingOffHand();
					}
					player.setRotation(startYaw + (beat[0] + 1) * 45.0F, startPitch);
					spawnEmoteParticles(player, Particle.NOTE, 2);
				}
				default -> task.cancel();
			}
			beat[0]++;
		}, 0L, period);
	}

	private void spawnEmoteParticles(Player player, Particle particle, int count) {
		World world = player.getWorld();
		Location location = player.getLocation().add(0, 1.2, 0);
		world.spawnParticle(particle, location, count, 0.35, 0.35, 0.35, 0.05);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		restorePlayerListName(event.getPlayer());
		lastEmoteAt.remove(event.getPlayer().getUniqueId());
	}

	private void restorePlayerListName(Player player) {
		if (!originalPlayerListNames.containsKey(player.getUniqueId())) {
			return;
		}
		player.playerListName(originalPlayerListNames.remove(player.getUniqueId()));
	}

	private boolean resolvePluginApis() {
		Plugin money = getServer().getPluginManager().getPlugin("MoneySystem");
		Plugin factions = getServer().getPluginManager().getPlugin("Fraktionssystem");
		if (money == null || !money.isEnabled()) {
			getLogger().severe("MoneySystem fehlt oder ist deaktiviert; Kontostand kann nicht gesendet werden.");
			return false;
		}
		if (factions == null || !factions.isEnabled()) {
			getLogger().severe("Fraktionssystem fehlt oder ist deaktiviert; Wanted/Fraktion/Rang können nicht gesendet werden.");
			return false;
		}
		try {
			moneySystem = money;
			factionSystem = factions;
			cashBalanceMethod = money.getClass().getMethod("getCashBalance", UUID.class);
			factionMethod = accessibleMethod(factions.getClass(), "getPlayerFaction", Player.class);
			rankMethod = accessibleMethod(factions.getClass(), "getPlayerRank", Player.class);
			wantedMethod = accessibleMethod(factions.getClass(), "getWantedPoints", Player.class);
			Class<?> factionClass = factionMethod.getReturnType();
			factionDisplayNameField = factionClass.getField("displayName");
			factionRankNameMethod = factionClass.getMethod("rankName", int.class);
			resolveDrugApi();
			return true;
		} catch (ReflectiveOperationException | RuntimeException exception) {
			getLogger().severe("Plugin-API passt nicht zu den installierten JARs (MoneySystem " +
					money.getPluginMeta().getVersion() + ", Fraktionssystem " +
					factions.getPluginMeta().getVersion() + "): " + exception.getMessage());
			return false;
		}
	}

	private void resolveDrugApi() {
		Plugin candidate = getServer().getPluginManager().getPlugin("DrugsPlugin");
		if (candidate == null || !candidate.isEnabled()) {
			return;
		}
		try {
			Class<?> drugType = Class.forName(
					"plugin.roleplay.guns.drugs.DrugsPlugin$Drug", false, candidate.getClass().getClassLoader());
			if (!drugType.isEnum()) {
				throw new ReflectiveOperationException("DrugsPlugin$Drug ist kein Enum");
			}
			Method amountMethod = accessibleMethod(candidate.getClass(), "getStoredAmount", Player.class, drugType);
			drugsPlugin = candidate;
			storedDrugAmountMethod = amountMethod;
			drugTypes = drugType.getEnumConstants();
			getLogger().info("Drogen-Anzeige verwendet die DrugsPlugin-API.");
		} catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
			getLogger().warning("DrugsPlugin gefunden, aber dessen Drogen-API ist nicht kompatibel: " +
					exception.getMessage());
		}
	}

	private static Method accessibleMethod(Class<?> owner, String name, Class<?>... parameters)
			throws NoSuchMethodException {
		Method method = owner.getDeclaredMethod(name, parameters);
		method.setAccessible(true);
		return method;
	}

	private void loadDrugMatchers() {
		drugMaterials = new HashSet<>();
		for (String name : getConfig().getStringList("drugs.materials")) {
			Material material = Material.matchMaterial(name);
			if (material == null) {
				getLogger().warning("Unbekanntes Drogen-Material in config.yml: " + name);
			} else {
				drugMaterials.add(material);
			}
		}
		drugNameFragments = new HashSet<>();
		for (String fragment : getConfig().getStringList("drugs.name-contains")) {
			if (!fragment.isBlank()) {
				drugNameFragments.add(fragment.toLowerCase(Locale.ROOT));
			}
		}
	}

	private void sendToOnlinePlayers() {
		for (Player player : getServer().getOnlinePlayers()) {
			sendData(player);
		}
	}

	private void sendData(Player player) {
		try {
			if (!moneySystem.isEnabled() || !factionSystem.isEnabled()) {
				throw new IllegalStateException("MoneySystem oder Fraktionssystem wurde deaktiviert");
			}
			Object balance = cashBalanceMethod.invoke(moneySystem, player.getUniqueId());
			if (!(balance instanceof BigDecimal cash)) {
				throw new IllegalStateException("MoneySystem.getCashBalance(UUID) gab keinen BigDecimal zurück");
			}
			Object faction = factionMethod.invoke(factionSystem, player);
			int rank = ((Number) rankMethod.invoke(factionSystem, player)).intValue();
			int wanted = ((Number) wantedMethod.invoke(factionSystem, player)).intValue();

			String factionName = "";
			String rankName = "";
			if (faction != null) {
				factionName = String.valueOf(factionDisplayNameField.get(faction));
				rankName = String.valueOf(factionRankNameMethod.invoke(faction, rank));
			}
			int drugs = getDrugCount(player);
			long playtimeSeconds = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
			String currency = ((JavaPlugin) moneySystem).getConfig().getString("currency", "$");
			player.sendPluginMessage(this, CHANNEL, createPayload(
					currency + cash.toPlainString(), wanted, factionName, rankName, drugs, playtimeSeconds));
		} catch (ReflectiveOperationException | ArithmeticException | ClassCastException | IllegalStateException exception) {
			getLogger().severe("HUD-Daten für " + player.getUniqueId() + " konnten nicht gelesen werden: " +
					exception.getMessage());
		}
	}

	private int getDrugCount(Player player) throws ReflectiveOperationException {
		if (storedDrugAmountMethod != null && drugsPlugin != null && drugsPlugin.isEnabled()) {
			int total = 0;
			for (Object drugType : drugTypes) {
				Object amount = storedDrugAmountMethod.invoke(drugsPlugin, player, drugType);
				if (!(amount instanceof Number number)) {
					throw new IllegalStateException("DrugsPlugin.getStoredAmount gab keinen Zahlenwert zurück");
				}
				int count = number.intValue();
				if (count < 0) {
					throw new IllegalStateException("DrugsPlugin.getStoredAmount gab eine negative Menge zurück");
				}
				total = Math.addExact(total, count);
			}
			return total;
		}
		if (drugMaterials.isEmpty() && drugNameFragments.isEmpty()) {
			return -1;
		}
		return countInventoryDrugs(player);
	}

	private int countInventoryDrugs(Player player) {
		int total = 0;
		for (ItemStack item : player.getInventory().getContents()) {
			if (item == null || item.getType().isAir()) {
				continue;
			}
			if (drugMaterials.contains(item.getType()) || matchesDrugName(item.getItemMeta())) {
				total += item.getAmount();
			}
		}
		return total;
	}

	private boolean matchesDrugName(ItemMeta meta) {
		if (meta == null || drugNameFragments.isEmpty() || meta.displayName() == null) {
			return false;
		}
		String displayName = PlainTextComponentSerializer.plainText()
				.serialize(meta.displayName()).toLowerCase(Locale.ROOT);
		return drugNameFragments.stream().anyMatch(displayName::contains);
	}

	private static byte[] createPayload(
			String balance, int wanted, String faction, String rank, int drugs, long playtimeSeconds) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream output = new DataOutputStream(bytes);
			output.writeByte(1);
			writeString(output, balance);
			output.writeInt(wanted);
			writeString(output, faction);
			writeString(output, rank);
			output.writeInt(drugs);
			output.writeLong(playtimeSeconds);
			output.flush();
			return bytes.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("HUD-Paket konnte nicht erstellt werden", exception);
		}
	}

	private static void writeString(DataOutputStream output, String value) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > 65535) {
			throw new IllegalArgumentException("HUD-Text überschreitet das Protokoll-Limit");
		}
		output.writeShort(bytes.length);
		output.write(bytes);
	}
}
