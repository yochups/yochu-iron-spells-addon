package com.nidza.evocationexpansion.effect;

import io.redspace.ironsspellbooks.entity.spells.gust.GustCollider;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FloatEffect extends MobEffect {
    public FloatEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(movement.x, 0, movement.z);
        entity.resetFallDistance();

        if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 5 == 0) {
            GustCollider gust = new GustCollider(serverLevel, entity);
            gust.setOwner(null);
            gust.setPos(entity.getX(), entity.getY() - 0.35, entity.getZ());
            gust.setYRot(entity.getYRot());
            gust.setXRot(90);
            gust.range = 0;
            gust.strength = 0;
            serverLevel.addFreshEntity(gust);
        }
        return true;
    }
}
