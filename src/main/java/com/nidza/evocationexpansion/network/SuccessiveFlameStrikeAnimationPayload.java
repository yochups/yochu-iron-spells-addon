package com.nidza.evocationexpansion.network;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SuccessiveFlameStrikeAnimationPayload implements CustomPacketPayload {
    public static final Type<SuccessiveFlameStrikeAnimationPayload> TYPE =
            new Type<>(EvocationExpansion.id("successive_flame_strike_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SuccessiveFlameStrikeAnimationPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SuccessiveFlameStrikeAnimationPayload::write,
                    SuccessiveFlameStrikeAnimationPayload::new);

    private final int playerId;
    private final boolean mirrored;

    public SuccessiveFlameStrikeAnimationPayload(int playerId, boolean mirrored) {
        this.playerId = playerId;
        this.mirrored = mirrored;
    }

    private SuccessiveFlameStrikeAnimationPayload(FriendlyByteBuf buffer) {
        playerId = buffer.readVarInt();
        mirrored = buffer.readBoolean();
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeBoolean(mirrored);
    }

    public static void handle(SuccessiveFlameStrikeAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientEvents.playSuccessiveFlameStrikeAnimation(
                payload.playerId,
                payload.mirrored
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
