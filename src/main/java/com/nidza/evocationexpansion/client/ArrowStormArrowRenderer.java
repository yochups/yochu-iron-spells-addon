package com.nidza.evocationexpansion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nidza.evocationexpansion.entity.ArrowStormArrow;
import io.redspace.ironsspellbooks.entity.spells.small_magic_arrow.SmallMagicArrowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ArrowStormArrowRenderer extends EntityRenderer<ArrowStormArrow> {
    public ArrowStormArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ArrowStormArrow arrow, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
        Vec3 motion = arrow.deltaMovementOld.add(
                arrow.getDeltaMovement().subtract(arrow.deltaMovementOld).scale(partialTick)
        );
        float xRot = -((float) (Mth.atan2(motion.horizontalDistance(), motion.y) * Mth.RAD_TO_DEG) - 90.0f);
        float yRot = -((float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) + 90.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        float shake = arrow.shakeTime - partialTick;
        if (shake > 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.sin(shake * 3.0f) * shake));
        }

        SmallMagicArrowRenderer.renderModel(poseStack, bufferSource);
        poseStack.popPose();
        super.render(arrow, yaw, partialTick, poseStack, bufferSource, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ArrowStormArrow arrow) {
        return SmallMagicArrowRenderer.getTextureLocation();
    }
}
