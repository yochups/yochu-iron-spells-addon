package com.nidza.evocationexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nidza.evocationexpansion.entity.ChainAnchorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ChainAnchorRenderer extends EntityRenderer<ChainAnchorEntity> {
    private static final ResourceLocation CHAIN_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/chain.png");
    private static final RenderType CHAIN_RENDER_TYPE = RenderType.entityTranslucentCull(CHAIN_TEXTURE);
    private static final double HALF_CHAIN_WIDTH = 3.0 / 32.0;

    public ChainAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ChainAnchorEntity anchor, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 anchorPosition = anchor.getPosition(partialTick);
        Vec3 startPosition = getChainStart(anchor, partialTick);
        if (startPosition == null) {
            return;
        }

        Vec3 span = anchorPosition.subtract(startPosition);
        double distance = span.length();
        if (distance < 0.05) {
            return;
        }

        int segmentCount = Mth.clamp((int) Math.ceil(distance), 1, 48);
        float alpha = anchor.getChainState() == ChainAnchorEntity.FADING
                ? Mth.clamp(1.0f - (anchor.getFadeAge() + partialTick) / ChainAnchorEntity.FADE_DURATION, 0.0f, 1.0f)
                : 1.0f;

        VertexConsumer consumer = buffer.getBuffer(CHAIN_RENDER_TYPE);
        Vec3 direction = span.normalize();
        Vec3 firstAxis = direction.cross(new Vec3(0.0, 1.0, 0.0));
        if (firstAxis.lengthSqr() < 0.001) {
            firstAxis = direction.cross(new Vec3(1.0, 0.0, 0.0));
        }
        firstAxis = firstAxis.normalize();
        Vec3 secondAxis = direction.cross(firstAxis).normalize();
        Vec3 firstPlane = firstAxis.add(secondAxis).normalize().scale(HALF_CHAIN_WIDTH);
        Vec3 secondPlane = firstAxis.subtract(secondAxis).normalize().scale(HALF_CHAIN_WIDTH);
        PoseStack.Pose pose = poseStack.last();
        int alphaByte = Mth.clamp((int) (alpha * 255.0f), 0, 255);

        for (int index = 0; index < segmentCount; index++) {
            double startProgress = (double) index / segmentCount;
            double endProgress = (double) (index + 1) / segmentCount;
            Vec3 segmentStart = getChainPoint(anchor, startPosition, anchorPosition, firstAxis, secondAxis,
                    startProgress, distance, partialTick).subtract(anchorPosition);
            Vec3 segmentEnd = getChainPoint(anchor, startPosition, anchorPosition, firstAxis, secondAxis,
                    endProgress, distance, partialTick).subtract(anchorPosition);
            renderPlane(consumer, pose, packedLight, segmentStart, segmentEnd, firstPlane,
                    0.0f, 3.0f / 16.0f, alphaByte);
            renderPlane(consumer, pose, packedLight, segmentStart, segmentEnd, secondPlane,
                    3.0f / 16.0f, 6.0f / 16.0f, alphaByte);
        }

        super.render(anchor, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static Vec3 getChainPoint(ChainAnchorEntity anchor, Vec3 start, Vec3 end,
                                      Vec3 firstAxis, Vec3 secondAxis, double progress,
                                      double distance, float partialTick) {
        Vec3 point = start.lerp(end, progress);
        double launchAge = anchor.tickCount + partialTick;
        if (anchor.getChainState() != ChainAnchorEntity.FLYING || launchAge > 4.0
                || progress <= 0.0 || progress >= 1.0) {
            return point;
        }

        double envelope = Math.sin(Math.PI * progress);
        double amplitude = Math.min(1.1, 0.32 + distance * 0.025) * envelope;
        double phase = launchAge * 2.35 + anchor.getId() * 0.73;
        double sidewaysWave = Math.sin(progress * Math.PI * 8.0 - phase)
                + Math.sin(progress * Math.PI * 18.0 + phase * 1.6) * 0.28;
        double crossingWave = Math.sin(progress * Math.PI * 13.0 + phase * 1.25)
                + Math.sin(progress * Math.PI * 23.0 - phase * 1.9) * 0.22;
        return point.add(firstAxis.scale(sidewaysWave * amplitude))
                .add(secondAxis.scale(crossingWave * amplitude * 0.7));
    }

    private Vec3 getChainStart(ChainAnchorEntity anchor, float partialTick) {
        if (anchor.getChainState() == ChainAnchorEntity.FADING) {
            return anchor.getLooseEnd();
        }
        Entity owner = anchor.getOwnerEntity();
        if (owner instanceof Player player) {
            return getPlayerShoulderPosition(player, partialTick);
        }
        return owner == null
                ? null
                : owner.getPosition(partialTick).add(0.0, owner.getBbHeight() * 0.65, 0.0);
    }

    private static void renderPlane(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                                    Vec3 start, Vec3 end, Vec3 side, float minU, float maxU, int alpha) {
        Vec3 normal = side.cross(end.subtract(start)).normalize();
        vertex(consumer, pose, packedLight, start.subtract(side), minU, 0.0f, normal, alpha);
        vertex(consumer, pose, packedLight, start.add(side), maxU, 0.0f, normal, alpha);
        vertex(consumer, pose, packedLight, end.add(side), maxU, 1.0f, normal, alpha);
        vertex(consumer, pose, packedLight, end.subtract(side), minU, 1.0f, normal, alpha);

        Vec3 backNormal = normal.scale(-1.0);
        vertex(consumer, pose, packedLight, start.add(side), minU, 0.0f, backNormal, alpha);
        vertex(consumer, pose, packedLight, start.subtract(side), maxU, 0.0f, backNormal, alpha);
        vertex(consumer, pose, packedLight, end.subtract(side), maxU, 1.0f, backNormal, alpha);
        vertex(consumer, pose, packedLight, end.add(side), minU, 1.0f, backNormal, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               Vec3 position, float u, float v, Vec3 normal, int alpha) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private Vec3 getPlayerShoulderPosition(Player player, float partialTick) {
        float bodyRotation = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(bodyRotation);
        double cos = Mth.cos(bodyRotation);
        float playerScale = player.getScale();
        double sideOffset = 0.34 * playerScale;
        double forwardOffset = 0.04 * playerScale;
        float crouchOffset = player.isCrouching() ? -0.1875f : 0.0f;
        return player.getEyePosition(partialTick).add(
                -cos * sideOffset - sin * forwardOffset,
                crouchOffset - 0.27 * playerScale,
                -sin * sideOffset + cos * forwardOffset
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ChainAnchorEntity entity) {
        return CHAIN_TEXTURE;
    }
}
