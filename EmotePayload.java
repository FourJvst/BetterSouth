package de.bettersouth.hud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EmotePayload(int emoteId) implements CustomPacketPayload {
	public static final Type<EmotePayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath("bettersouth", "emote"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EmotePayload> CODEC = StreamCodec.of(
			(buffer, payload) -> {
				buffer.writeByte(1);
				buffer.writeByte(payload.emoteId);
			},
			buffer -> {
				int protocol = buffer.readUnsignedByte();
				if (protocol != 1) {
					throw new IllegalArgumentException("Unsupported BetterSouth emote protocol: " + protocol);
				}
				int emoteId = buffer.readUnsignedByte();
				if (emoteId < 0 || emoteId > 3) {
					throw new IllegalArgumentException("Unknown BetterSouth emote id: " + emoteId);
				}
				return new EmotePayload(emoteId);
			}
	);

	public EmotePayload {
		if (emoteId < 0 || emoteId > 3) {
			throw new IllegalArgumentException("Unknown BetterSouth emote id: " + emoteId);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
