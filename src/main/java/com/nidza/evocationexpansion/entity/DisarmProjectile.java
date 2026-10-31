package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class DisarmProjectile extends AbstractMagicProjectile {
    public DisarmProjectile(EntityType<? extends DisarmProjectile> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public DisarmProjectile(Level level, LivingEntity owner) {
        this(ModEntities.DISARM_PROJECTILE.get(), level);
        setOwner(owner);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= 8) {
            discard();
        }
    }

    @Override
    public float getSpeed() {
        return 1.25f;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.empty();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        discard();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!level().isClientSide && hitResult.getEntity() instanceof LivingEntity target
                && !DamageSources.isFriendlyFireBetween(getOwner(), target)) {
            ItemStack heldItem = target.getMainHandItem();
            if (!heldItem.isEmpty()) {
                ItemStack droppedStack = heldItem.copy();
                target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                ItemEntity droppedItem = target.spawnAtLocation(droppedStack, 0.25f);
                if (droppedItem != null) {
                    droppedItem.setPickUpDelay(20);
                    droppedItem.setDeltaMovement(getDeltaMovement().scale(0.2).add(0, 0.2, 0));
                }
            }
            consumeEntityImpact(hitResult, true);
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(level(), ParticleTypes.GUST, x, y, z, 3, 0.15, 0.15, 0.15, 0, true);
        MagicManager.spawnParticles(level(), ParticleTypes.POOF, x, y, z, 10, 0.2, 0.2, 0.2, 0.05, true);
    }

    @Override
    public void trailParticles() {
        Vec3 motion = getDeltaMovement();
        Vec3 trailPosition = position().subtract(motion.scale(0.35));
        level().addParticle(ParticleTypes.POOF, trailPosition.x, trailPosition.y, trailPosition.z, 0, 0, 0);
    }
}
