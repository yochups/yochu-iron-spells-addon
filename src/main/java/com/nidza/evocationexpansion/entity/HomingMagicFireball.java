package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.registry.ModSpells;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public final class HomingMagicFireball extends MagicFireball {
    private static final EntityDataAccessor<Integer> HOMING_TARGET_ID =
            SynchedEntityData.defineId(HomingMagicFireball.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DEFLECTED =
            SynchedEntityData.defineId(HomingMagicFireball.class, EntityDataSerializers.BOOLEAN);
    private static final float HOMING_STRENGTH = 0.12f;
    private static final double HOMING_RANGE = 24.0;

    public HomingMagicFireball(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public HomingMagicFireball(Level level, LivingEntity owner) {
        this(ModEntities.MAGIC_FIREBALL.get(), level);
        setOwner(owner);
    }

    @Override
    protected void handleEntityHoming() {
        if (entityData.get(DEFLECTED)) {
            return;
        }

        Entity target = getHomingTarget();
        if (target == null && !level().isClientSide) {
            Entity owner = getOwner();
            target = level().getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(HOMING_RANGE),
                            candidate -> candidate != owner
                                    && candidate.isAlive()
                                    && (owner == null || !DamageSources.isFriendlyFireBetween(owner, candidate))
                    ).stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);
            if (target instanceof LivingEntity livingTarget) {
                setHomingTarget(livingTarget);
            }
        }

        if (target == null || target.isRemoved()) {
            stopEntityHoming();
            return;
        }

        Vec3 wantedPosition = target.getBoundingBox().getCenter().add(target.getDeltaMovement());
        Vec3 newMotion = homeTowards(wantedPosition, HOMING_STRENGTH);
        if (newMotion.dot(wantedPosition.subtract(position())) < -0.25 && tickCount > 10) {
            stopEntityHoming();
        }
    }

    @Override
    public boolean deflect(ProjectileDeflection deflection, Entity deflector, Entity newOwner,
                           boolean deflectedByPlayer) {
        entityData.set(DEFLECTED, true);
        stopEntityHoming();
        return super.deflect(deflection, deflector, newOwner, deflectedByPlayer);
    }

    @Override
    public Entity getHomingTarget() {
        int targetId = entityData.get(HOMING_TARGET_ID);
        Entity target = targetId < 0 ? null : level().getEntity(targetId);
        return target != null ? target : super.getHomingTarget();
    }

    @Override
    public void setHomingTarget(LivingEntity entity) {
        super.setHomingTarget(entity);
        entityData.set(HOMING_TARGET_ID, entity.getId());
    }

    @Override
    public void stopEntityHoming() {
        super.stopEntityHoming();
        entityData.set(HOMING_TARGET_ID, -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOMING_TARGET_ID, -1);
        builder.define(DEFLECTED, false);
    }

    @Override
    protected void onHit(@NotNull HitResult hitResult) {
        if (level().isClientSide) {
            return;
        }

        float radius = getExplosionRadius();
        double radiusSqr = radius * radius;
        Vec3 lineOfSightPoint = Utils.raycastForBlock(
                level(), position(), position().add(0.0, 2.0, 0.0), ClipContext.Fluid.NONE
        ).getLocation();

        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(radius))) {
            double distanceSqr = entity.distanceToSqr(hitResult.getLocation());
            if (distanceSqr < radiusSqr && canHitEntity(entity)
                    && Utils.hasLineOfSight(level(), lineOfSightPoint, entity.getBoundingBox().getCenter(), true)) {
                float damage = (float) (getDamage() * (1.0 - distanceSqr / radiusSqr));
                DamageSources.applyDamage(
                        entity,
                        damage,
                        ModSpells.MAGIC_FIREBALL.get().getDamageSource(this, getOwner())
                );
            }
        }

        PacketDistributor.sendToPlayersTrackingEntity(
                this,
                new FieryExplosionParticlesPacket(
                        hitResult.getLocation().subtract(getDeltaMovement().scale(0.5)),
                        radius
                )
        );
        playSound(
                SoundEvents.GENERIC_EXPLODE.value(),
                4.0f,
                (1.0f + (level().random.nextFloat() - level().random.nextFloat()) * 0.2f) * 0.7f
        );
        discardHelper(hitResult);
    }
}
