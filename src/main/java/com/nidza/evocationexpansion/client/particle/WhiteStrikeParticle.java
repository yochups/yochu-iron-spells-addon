package com.nidza.evocationexpansion.client.particle;

import com.nidza.evocationexpansion.particle.WhiteStrikeParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class WhiteStrikeParticle extends OrientedQuadParticle {
    private WhiteStrikeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                                WhiteStrikeParticleOptions options) {
        this(level, x, y, z, sprites, options, axes(options));
    }

    private WhiteStrikeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                                WhiteStrikeParticleOptions options, Vec3[] axes) {
        super(level, x, y, z, sprites, axes[0], axes[1], Vec3.ZERO, options.scale, 6);
    }

    private static Vec3[] axes(WhiteStrikeParticleOptions options) {
        Vec3 forward = new Vec3(options.forwardX, options.forwardY, options.forwardZ).normalize();
        Vec3 worldUp = Math.abs(forward.dot(new Vec3(0, 1, 0))) > 0.999
                ? new Vec3(1, 0, 0)
                : new Vec3(0, 1, 0);
        Vec3 right = worldUp.cross(forward).normalize();
        Vec3 up = forward.cross(right).normalize();
        return new Vec3[]{right, up};
    }

    public static final class Provider implements ParticleProvider<WhiteStrikeParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull WhiteStrikeParticleOptions options, @NotNull ClientLevel level,
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WhiteStrikeParticle(level, x, y, z, sprites, options);
        }
    }
}
