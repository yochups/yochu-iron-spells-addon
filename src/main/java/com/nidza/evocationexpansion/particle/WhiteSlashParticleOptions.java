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

public final class WhiteSlashParticleOptions implements ParticleOptions {
    public static final StreamCodec<? super ByteBuf, WhiteSlashParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buffer, option) -> {
                buffer.writeFloat(option.forwardX);
                buffer.writeFloat(option.forwardY);
                buffer.writeFloat(option.forwardZ);
                buffer.writeFloat(option.upX);
                buffer.writeFloat(option.upY);
                buffer.writeFloat(option.upZ);
                buffer.writeFloat(option.scale);
                buffer.writeInt(option.followEntityId);
                buffer.writeFloat(option.followForwardOffset);
                buffer.writeFloat(option.followVerticalOffset);
                buffer.writeFloat(option.followRightOffset);
            },
            buffer -> new WhiteSlashParticleOptions(
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readInt(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat()
            )
    );
    public static final MapCodec<WhiteSlashParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("forward_x").forGetter(option -> option.forwardX),
                    Codec.FLOAT.fieldOf("forward_y").forGetter(option -> option.forwardY),
                    Codec.FLOAT.fieldOf("forward_z").forGetter(option -> option.forwardZ),
                    Codec.FLOAT.fieldOf("up_x").forGetter(option -> option.upX),
                    Codec.FLOAT.fieldOf("up_y").forGetter(option -> option.upY),
                    Codec.FLOAT.fieldOf("up_z").forGetter(option -> option.upZ),
                    Codec.FLOAT.fieldOf("scale").forGetter(option -> option.scale),
                    Codec.INT.fieldOf("follow_entity_id").forGetter(option -> option.followEntityId),
                    Codec.FLOAT.fieldOf("follow_forward_offset").forGetter(option -> option.followForwardOffset),
                    Codec.FLOAT.fieldOf("follow_vertical_offset").forGetter(option -> option.followVerticalOffset),
                    Codec.FLOAT.fieldOf("follow_right_offset").forGetter(option -> option.followRightOffset)
            ).apply(instance, WhiteSlashParticleOptions::new)
    );

    public final float forwardX;
    public final float forwardY;
    public final float forwardZ;
    public final float upX;
    public final float upY;
    public final float upZ;
    public final float scale;
    public final int followEntityId;
    public final float followForwardOffset;
    public final float followVerticalOffset;
    public final float followRightOffset;

    public WhiteSlashParticleOptions(float forwardX, float forwardY, float forwardZ,
                                     float upX, float upY, float upZ, float scale,
                                     int followEntityId, float followForwardOffset,
                                     float followVerticalOffset, float followRightOffset) {
        this.forwardX = forwardX;
        this.forwardY = forwardY;
        this.forwardZ = forwardZ;
        this.upX = upX;
        this.upY = upY;
        this.upZ = upZ;
        this.scale = scale;
        this.followEntityId = followEntityId;
        this.followForwardOffset = followForwardOffset;
        this.followVerticalOffset = followVerticalOffset;
        this.followRightOffset = followRightOffset;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.WHITE_SLASH.get();
    }
}
