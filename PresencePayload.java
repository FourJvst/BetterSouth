package de.bettersouth.hud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PresencePayload() implements CustomPacketPayload {
	public static final Type<PresencePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("bettersouth", "presence"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> buffer.writeByte(1),
			buffer -> {
				int version = buffer.readUnsignedByte();
				if (version != 1) {
					throw new IllegalArgumentException("Unsupported BetterSouth presence protocol: " + version);
				}
				return new PresencePayload();
			}
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
