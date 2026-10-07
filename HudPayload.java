package de.bettersouth.hud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HudPayload(
		String balance,
		int wanted,
		String faction,
		String rank,
		int drugs,
		long playtimeSeconds
) implements CustomPacketPayload {
	public static final Type<HudPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("bettersouth", "hud"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeByte(1);
				writeString(buffer, payload.balance);
				buffer.writeInt(payload.wanted);
				writeString(buffer, payload.faction);
				writeString(buffer, payload.rank);
				buffer.writeInt(payload.drugs);
				buffer.writeLong(payload.playtimeSeconds);
			},
			buffer -> {
				int protocol = buffer.readUnsignedByte();
				if (protocol != 1) {
					throw new IllegalArgumentException("Unsupported BetterSouth HUD protocol: " + protocol);
				}
				return new HudPayload(
						readString(buffer),
						buffer.readInt(),
						readString(buffer),
						readString(buffer),
						buffer.readInt(),
						buffer.readLong()
				);
			}
	);

	private static void writeString(RegistryFriendlyByteBuf buffer, String value) {
		byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		if (bytes.length > 65535) {
			throw new IllegalArgumentException("HUD string exceeds the protocol limit");
		}
		buffer.writeShort(bytes.length);
		buffer.writeBytes(bytes);
	}

	private static String readString(RegistryFriendlyByteBuf buffer) {
		int length = buffer.readUnsignedShort();
		byte[] bytes = new byte[length];
		buffer.readBytes(bytes);
		return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
