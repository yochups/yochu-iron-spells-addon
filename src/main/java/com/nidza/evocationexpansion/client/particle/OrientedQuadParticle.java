package com.nidza.evocationexpansion.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

abstract class OrientedQuadParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vector3f[] localVertices;

    protected OrientedQuadParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                                   Vec3 primaryAxis, Vec3 secondaryAxis, Vec3 velocity,
                                   float scale, int lifetime) {
        super(level, x, y, z, velocity.x, velocity.y, velocity.z);
        this.sprites = sprites;
        this.localVertices = createVertices(primaryAxis.normalize(), secondaryAxis.normalize());
        this.quadSize = scale * 3.25f;
        this.lifetime = lifetime;
        this.friction = 1;
        setSpriteFromAge(sprites);
    }

    private static Vector3f[] createVertices(Vec3 primary, Vec3 secondary) {
        return new Vector3f[]{
                transform(primary, secondary, -1, -1),
                transform(primary, secondary, -1, 1),
                transform(primary, secondary, 1, 1),
                transform(primary, secondary, 1, -1)
        };
    }

    private static Vector3f transform(Vec3 primary, Vec3 secondary, float x, float y) {
        return new Vector3f(
                (float) (primary.x * x + secondary.x * y),
                (float) (primary.y * x + secondary.y * y),
                (float) (primary.z * x + secondary.z * y)
        );
    }

    @Override
    public void tick() {
        move(xd, yd, zd);
        if (age++ >= lifetime) {
            remove();
        } else {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - cameraPosition.x);
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - cameraPosition.y);
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - cameraPosition.z);
        Vector3f[] vertices = new Vector3f[4];

        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = new Vector3f(localVertices[i]).mul(getQuadSize(partialTick)).add(x, y, z);
        }

        float topV = getV0();
        float bottomV = getV1();
        vertex(consumer, vertices[0], getU1(), bottomV);
        vertex(consumer, vertices[1], getU1(), topV);
        vertex(consumer, vertices[2], getU0(), topV);
        vertex(consumer, vertices[3], getU0(), bottomV);
        vertex(consumer, vertices[3], getU0(), bottomV);
        vertex(consumer, vertices[2], getU0(), topV);
        vertex(consumer, vertices[1], getU1(), topV);
        vertex(consumer, vertices[0], getU1(), bottomV);
    }

    private void vertex(VertexConsumer consumer, Vector3f position, float u, float v) {
        consumer.addVertex(position.x, position.y, position.z)
                .setUv(u, v)
                .setColor(rCol, gCol, bCol, alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
