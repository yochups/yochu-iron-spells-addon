package com.nidza.evocationexpansion.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nidza.evocationexpansion.registry.ModParticles;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public final class WhiteStrikeParticleOptions implements ParticleOptions {
    public static final StreamCodec<? super ByteBuf, WhiteStrikeParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buffer, option) -> {
                buffer.writeFloat(option.forwardX);
                buffer.writeFloat(option.forwardY);
                buffer.writeFloat(option.forwardZ);
                buffer.writeFloat(option.scale);
            },
            buffer -> new WhiteStrikeParticleOptions(
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat()
            )
    );
    public static final MapCodec<WhiteStrikeParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("forward_x").forGetter(option -> option.forwardX),
                    Codec.FLOAT.fieldOf("forward_y").forGetter(option -> option.forwardY),
                    Codec.FLOAT.fieldOf("forward_z").forGetter(option -> option.forwardZ),
                    Codec.FLOAT.fieldOf("scale").forGetter(option -> option.scale)
            ).apply(instance, WhiteStrikeParticleOptions::new)
    );

    public final float forwardX;
    public final float forwardY;
    public final float forwardZ;
    public final float scale;

    public WhiteStrikeParticleOptions(float forwardX, float forwardY, float forwardZ, float scale) {
        this.forwardX = forwardX;
        this.forwardY = forwardY;
        this.forwardZ = forwardZ;
        this.scale = scale;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.WHITE_STRIKE.get();
    }
}
