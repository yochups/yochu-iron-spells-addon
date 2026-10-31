package com.nidza.evocationexpansion.registry;

import com.mojang.serialization.MapCodec;
import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.particle.WhiteSlashParticleOptions;
import com.nidza.evocationexpansion.particle.WhiteStrikeParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, EvocationExpansion.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<WhiteSlashParticleOptions>> WHITE_SLASH =
            PARTICLES.register("white_slash", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<WhiteSlashParticleOptions> codec() {
                    return WhiteSlashParticleOptions.MAP_CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, WhiteSlashParticleOptions> streamCodec() {
                    return WhiteSlashParticleOptions.STREAM_CODEC;
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<WhiteStrikeParticleOptions>> WHITE_STRIKE =
            PARTICLES.register("white_strike", () -> new ParticleType<>(true) {
                @Override
                public MapCodec<WhiteStrikeParticleOptions> codec() {
                    return WhiteStrikeParticleOptions.MAP_CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, WhiteStrikeParticleOptions> streamCodec() {
                    return WhiteStrikeParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }
}
