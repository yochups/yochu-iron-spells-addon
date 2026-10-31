package com.nidza.evocationexpansion.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nidza.evocationexpansion.particle.WhiteSlashParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class WhiteSlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final Vec3 forward;
    private Vec3 up;
    private final int followEntityId;
    private final float followForwardOffset;
    private final float followVerticalOffset;
    private final float followRightOffset;
    private Vector3f[] localVertices;

    private WhiteSlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                               WhiteSlashParticleOptions options) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.lifetime = 5;
        this.gravity = 0;
        this.quadSize = options.scale * 3.25f;
        this.forward = new Vec3(options.forwardX, options.forwardY, options.forwardZ).normalize();
        this.up = new Vec3(options.upX, options.upY, options.upZ).normalize();
        this.followEntityId = options.followEntityId;
        this.followForwardOffset = options.followForwardOffset;
        this.followVerticalOffset = options.followVerticalOffset;
        this.followRightOffset = options.followRightOffset;
        this.localVertices = calculateVertices();
        this.xd = forward.x * 0.1;
        this.yd = forward.y * 0.1;
        this.zd = forward.z * 0.1;
        this.friction = 1;
    }

    private Vector3f[] calculateVertices() {
        Vec3 right = forward.cross(up);
        Vector3f[] vertices = {
                new Vector3f(-1, -1, 0),
                new Vector3f(-1, 1, 0),
                new Vector3f(1, 1, 0),
                new Vector3f(1, -1, 0)
        };

        for (int i = 0; i < vertices.length; i++) {
            Vector3f vertex = vertices[i];
            vertices[i] = new Vector3f(
                    (float) (forward.x * vertex.x + right.x * vertex.y),
                    (float) (forward.y * vertex.x + right.y * vertex.y),
                    (float) (forward.z * vertex.x + right.z * vertex.y)
            );
        }
        return vertices;
    }

    @Override
    public void tick() {
        Entity followedEntity = level.getEntity(followEntityId);
        if (followedEntity == null) {
            move(xd, yd, zd);
        } else {
            xo = x;
            yo = y;
            zo = z;
            Vec3 look = followedEntity.getLookAngle().normalize();
            Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
            if (horizontalLook.lengthSqr() < 1.0E-6) {
                horizontalLook = new Vec3(0, 0, 1);
            } else {
                horizontalLook = horizontalLook.normalize();
            }
            Vec3 right = new Vec3(-horizontalLook.z, 0, horizontalLook.x);
            Vec3 trackedPosition = followedEntity.position()
                    .add(look.scale(followForwardOffset))
                    .add(0, followVerticalOffset, 0)
                    .add(right.scale(followRightOffset));
            up = right;
            localVertices = calculateVertices();
            setPos(trackedPosition.x, trackedPosition.y, trackedPosition.z);
        }
        if (age++ >= lifetime) {
            remove();
        } else {
            setSpriteFromAge(sprites);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - cameraPosition.x());
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - cameraPosition.y());
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - cameraPosition.z());
        Vector3f[] vertices = new Vector3f[4];

        for (int i = 0; i < vertices.length; i++) {
            Vector3f localVertex = localVertices[i];
            vertices[i] = new Vector3f(localVertex).mul(getQuadSize(partialTick)).add(x, y, z);
        }

        int light = getLightColor(partialTick);
        makeCornerVertex(buffer, vertices[0], getU1(), getV1(), light);
        makeCornerVertex(buffer, vertices[1], getU1(), getV0(), light);
        makeCornerVertex(buffer, vertices[2], getU0(), getV0(), light);
        makeCornerVertex(buffer, vertices[3], getU0(), getV1(), light);
        makeCornerVertex(buffer, vertices[3], getU0(), getV1(), light);
        makeCornerVertex(buffer, vertices[2], getU0(), getV0(), light);
        makeCornerVertex(buffer, vertices[1], getU1(), getV0(), light);
        makeCornerVertex(buffer, vertices[0], getU1(), getV1(), light);
    }

    private void makeCornerVertex(VertexConsumer buffer, Vector3f vertex, float u, float v, int light) {
        buffer.addVertex(vertex.x(), vertex.y(), vertex.z())
                .setUv(u, v)
                .setColor(rCol, gCol, bCol, alpha)
                .setLight(light);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<WhiteSlashParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull WhiteSlashParticleOptions options, @NotNull ClientLevel level,
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            WhiteSlashParticle particle = new WhiteSlashParticle(level, x, y, z, sprites, options);
            particle.setSpriteFromAge(sprites);
            particle.setAlpha(1);
            return particle;
        }
    }
}
