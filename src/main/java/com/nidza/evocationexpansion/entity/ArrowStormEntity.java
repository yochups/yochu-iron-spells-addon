package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ArrowStormEntity extends Entity {
    private static final int WAVE_COUNT = 6;
    private static final int ARROWS_PER_WAVE = 4;
    private static final int WAVE_INTERVAL_TICKS = 2;
    private static final double ARROW_SPEED = 1.35;
    public static final int ARROW_COUNT = WAVE_COUNT * ARROWS_PER_WAVE;

    private int ownerId = -1;
    private float damage;
    private int wavesFired;

    public ArrowStormEntity(EntityType<? extends ArrowStormEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ArrowStormEntity(Level level, LivingEntity owner, float damage) {
        this(ModEntities.ARROW_STORM.get(), level);
        ownerId = owner.getId();
        this.damage = damage;
        setPos(owner.position());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        Entity owner = level().getEntity(ownerId);
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive()) {
            discard();
            return;
        }
        setPos(caster.position());

        if ((tickCount - 1) % WAVE_INTERVAL_TICKS != 0) {
            return;
        }

        fireWave(caster);
        wavesFired++;
        if (wavesFired >= WAVE_COUNT) {
            discard();
        }
    }

    private void fireWave(LivingEntity caster) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 horizontalForward = new Vec3(look.x, 0, look.z);
        if (horizontalForward.lengthSqr() < 1.0E-6) {
            horizontalForward = new Vec3(0, 0, 1);
        } else {
            horizontalForward = horizontalForward.normalize();
        }
        Vec3 right = new Vec3(-horizontalForward.z, 0, horizontalForward.x);

        for (int i = 0; i < ARROWS_PER_WAVE; i++) {
            double backDistance = 2.5 + random.nextDouble() * 3.5;
            double sideOffset = (random.nextDouble() - 0.5) * 7.0;
            double heightOffset = 0.6 + random.nextDouble() * 3.8;
            Vec3 proposedSpawn = caster.position()
                    .subtract(horizontalForward.scale(backDistance))
                    .add(right.scale(sideOffset))
                    .add(0, heightOffset, 0);
            Vec3 spawn = clampBeforeObstacle(caster, proposedSpawn, horizontalForward);

            Vec3 aimPoint = caster.getEyePosition()
                    .add(look.scale(20.0))
                    .add(right.scale(Utils.getRandomScaled(1.8)))
                    .add(0, Utils.getRandomScaled(1.1), 0);
            Vec3 velocity = aimPoint.subtract(spawn).normalize()
                    .add(Utils.getRandomVec3(0.025))
                    .normalize()
                    .scale(ARROW_SPEED * (0.92 + random.nextDouble() * 0.16));

            ArrowStormArrow arrow = new ArrowStormArrow(level(), caster);
            arrow.setDamage(damage);
            arrow.setPos(spawn);
            arrow.shoot(velocity);
            level().addFreshEntity(arrow);
            MagicManager.spawnParticles(level(), ParticleTypes.FIREWORK, spawn.x, spawn.y, spawn.z,
                    2, 0.1, 0.1, 0.1, 0.05, false);
        }

        level().playSound(null, caster.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.NEUTRAL, 1.0f, 1.1f + random.nextFloat() * 0.25f);
        level().playSound(null, caster.blockPosition(), SoundRegistry.BOW_SHOOT.get(),
                SoundSource.NEUTRAL, 1.2f, 1.6f + random.nextFloat() * 0.3f);
    }

    private Vec3 clampBeforeObstacle(LivingEntity caster, Vec3 proposedSpawn, Vec3 forward) {
        HitResult hit = level().clip(new ClipContext(
                caster.getEyePosition(),
                proposedSpawn,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                caster
        ));
        return hit.getType() == HitResult.Type.MISS
                ? proposedSpawn
                : hit.getLocation().add(forward.scale(0.25));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
