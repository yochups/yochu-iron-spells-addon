package com.nidza.evocationexpansion.network;

import com.nidza.evocationexpansion.EvocationExpansion;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(EvocationExpansion.MOD_ID)
                .versioned("1.0.0")
                .optional()
                .playToClient(
                        SuccessiveFlameStrikeAnimationPayload.TYPE,
                        SuccessiveFlameStrikeAnimationPayload.STREAM_CODEC,
                        SuccessiveFlameStrikeAnimationPayload::handle
                );
    }
}
