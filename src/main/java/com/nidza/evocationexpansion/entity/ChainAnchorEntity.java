package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.spell.SwingSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class ChainAnchorEntity extends Entity {
    public enum Mode {
        SWING,
        HOOK_SHOT
    }

    public static final byte FLYING = 0;
    public static final byte ATTACHED = 1;
    public static final byte RETRACTING = 2;
    public static final byte FADING = 3;
    public static final int FADE_DURATION = 18;

    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MODE =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> STATE =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> MAX_RANGE =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROPE_LENGTH =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FADE_AGE =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOOSE_X =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOOSE_Y =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOOSE_Z =
            SynchedEntityData.defineId(ChainAnchorEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 launchPosition = Vec3.ZERO;
    private Vec3 looseVelocity = Vec3.ZERO;
    private int retractAge;

    public ChainAnchorEntity(EntityType<? extends ChainAnchorEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public ChainAnchorEntity(Level level, LivingEntity owner, Mode mode, float maxRange) {
        this(ModEntities.CHAIN_ANCHOR.get(), level);
        Vec3 look = owner.getLookAngle().normalize();
        Vec3 start = owner.getEyePosition().add(look.scale(0.45));
        setPos(start);
        launchPosition = start;
        setDeltaMovement(look.scale(mode == Mode.HOOK_SHOT ? 2.6 : 1.8));
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(MODE, (byte) mode.ordinal());
        entityData.set(MAX_RANGE, maxRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
        builder.define(TARGET_ID, -1);
        builder.define(MODE, (byte) 0);
        builder.define(STATE, FLYING);
        builder.define(MAX_RANGE, 16.0f);
        builder.define(ROPE_LENGTH, 4.0f);
        builder.define(FADE_AGE, 0);
        builder.define(LOOSE_X, 0.0f);
        builder.define(LOOSE_Y, 0.0f);
        builder.define(LOOSE_Z, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            followTarget();
            if (getChainState() == ATTACHED) {
                Entity owner = getOwnerEntity();
                if (owner != null) {
                    applySwingPhysics(owner);
                }
            }
            return;
        }

        switch (getChainState()) {
            case FLYING -> tickFlying();
            case ATTACHED -> tickSwing();
            case RETRACTING -> tickHookShot();
            case FADING -> tickFade();
            default -> discard();
        }
    }

    private void tickFlying() {
        Entity owner = getOwnerEntity();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : start.distanceToSqr(blockHit.getLocation());

        EntityHit entityHit = level().getEntities(this, getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.35),
                        candidate -> canAttachTo(candidate, owner)).stream()
                .map(candidate -> candidate.getBoundingBox().inflate(0.25).clip(start, end)
                        .map(point -> new EntityHit(candidate, point)).orElse(null))
                .filter(hit -> hit != null)
                .filter(hit -> start.distanceToSqr(hit.point()) < blockDistance)
                .min(Comparator.comparingDouble(hit -> start.distanceToSqr(hit.point())))
                .orElse(null);

        if (entityHit != null) {
            attachToEntity(entityHit.entity());
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            attachToBlock(blockHit.getLocation());
            return;
        }

        setPos(end);
        if (launchPosition == Vec3.ZERO) {
            launchPosition = start;
        }
        if (position().distanceToSqr(launchPosition) >= entityData.get(MAX_RANGE) * entityData.get(MAX_RANGE)) {
            if (getMode() == Mode.SWING && owner instanceof ServerPlayer serverPlayer) {
                CancelCastPacket.cancelCast(serverPlayer, true);
            }
            beginFade();
        }
    }

    private boolean canAttachTo(Entity candidate, Entity owner) {
        return candidate != owner
                && candidate.isAlive()
                && !candidate.isSpectator()
                && candidate.isPickable();
    }

    private void attachToEntity(Entity target) {
        entityData.set(TARGET_ID, target.getId());
        setPos(target.getBoundingBox().getCenter());
        attach();
    }

    private void attachToBlock(Vec3 point) {
        entityData.set(TARGET_ID, -1);
        setPos(point.subtract(getDeltaMovement().normalize().scale(0.035)));
        attach();
    }

    private void attach() {
        setDeltaMovement(Vec3.ZERO);
        Entity owner = getOwnerEntity();
        if (owner == null) {
            discard();
            return;
        }
        float distance = (float) getOwnerRopePoint(owner).distanceTo(position());
        entityData.set(ROPE_LENGTH, Math.max(2.5f, distance));
        entityData.set(STATE, getMode() == Mode.SWING ? ATTACHED : RETRACTING);
    }

    private void tickSwing() {
        Entity owner = getOwnerEntity();
        if (!(owner instanceof LivingEntity livingOwner) || !owner.isAlive()) {
            beginFade();
            return;
        }
        followTarget();

        MagicData magicData = MagicData.getPlayerMagicData(livingOwner);
        if (!magicData.isCasting() || !SwingSpell.ID.toString().equals(magicData.getCastingSpellId())) {
            releaseSwing();
            return;
        }

        applySwingPhysics(owner);
    }

    private void applySwingPhysics(Entity owner) {
        Vec3 fromAnchor = getOwnerRopePoint(owner).subtract(position());
        double distance = fromAnchor.length();
        if (distance < 0.001) {
            return;
        }

        Vec3 outward = fromAnchor.scale(1.0 / distance);
        Vec3 velocity = owner.getDeltaMovement();
        double ropeLength = entityData.get(ROPE_LENGTH);

        Vec3 predictedFromAnchor = fromAnchor.add(velocity);
        if (predictedFromAnchor.length() > ropeLength) {
            double outwardSpeed = velocity.dot(outward);
            if (outwardSpeed > 0.0) {
                velocity = velocity.subtract(outward.scale(outwardSpeed));
            }
            double overstretch = Math.max(0.0, distance - ropeLength);
            if (overstretch > 0.0) {
                velocity = velocity.subtract(outward.scale(Math.min(0.12, overstretch * 0.08)));
            }
        }

        Vec3 tangent = velocity.subtract(outward.scale(velocity.dot(outward)));
        Vec3 gravity = new Vec3(0.0, -1.0, 0.0);
        Vec3 gravityTangent = gravity.subtract(outward.scale(gravity.dot(outward)));
        boolean climbing = gravityTangent.lengthSqr() > 0.001 && tangent.dot(gravityTangent) < 0.0;
        Vec3 predictedDirection = fromAnchor.add(velocity).normalize();

        if (climbing && predictedDirection.y >= -0.02) {
            velocity = velocity.subtract(tangent);
            if (gravityTangent.lengthSqr() > 0.001) {
                velocity = velocity.add(gravityTangent.normalize().scale(0.06));
            }
        } else {
            if (!owner.onGround()) {
                Vec3 preservedTangent = new Vec3(
                        tangent.x / 0.91,
                        tangent.y / 0.98,
                        tangent.z / 0.91
                );
                velocity = velocity.add(preservedTangent.subtract(tangent));
            }
        }

        owner.setDeltaMovement(velocity);
        owner.resetFallDistance();
        owner.hurtMarked = true;
    }

    public void releaseSwing() {
        if (getChainState() != ATTACHED || getMode() != Mode.SWING) {
            beginFade();
            return;
        }

        Entity owner = getOwnerEntity();
        if (owner != null) {
            Vec3 fromAnchor = getOwnerRopePoint(owner).subtract(position());
            if (fromAnchor.lengthSqr() > 0.001) {
                Vec3 outward = fromAnchor.normalize();
                Vec3 velocity = owner.getDeltaMovement();
                double radialSpeed = velocity.dot(outward);
                Vec3 tangent = velocity.subtract(outward.scale(radialSpeed));
                Vec3 inward = outward.scale(Math.min(0.0, radialSpeed));
                owner.setDeltaMovement(tangent.scale(1.25).add(inward));
                owner.hurtMarked = true;
            }
        }
        beginFade();
    }

    private void tickHookShot() {
        Entity owner = getOwnerEntity();
        if (owner == null || !owner.isAlive()) {
            beginFade();
            return;
        }
        followTarget();

        Entity target = getTargetEntity();
        boolean entityAnchor = entityData.get(TARGET_ID) >= 0;
        boolean arrived;
        if (entityAnchor) {
            if (target == null || !target.isAlive()) {
                beginFade();
                return;
            }
            arrived = pullToward(target, target.getBoundingBox().getCenter(), getOwnerRopePoint(owner), 1.4, 1.4);
        } else {
            arrived = pullToward(owner, getOwnerRopePoint(owner), position(), 1.2, 1.7);
        }

        if (arrived) {
            beginFade();
            return;
        }

        retractAge++;
        if (retractAge >= 40) {
            beginFade();
        }
    }

    private static boolean pullToward(Entity mover, Vec3 moverPoint, Vec3 destination,
                                      double stoppingDistance, double maxSpeed) {
        Vec3 difference = destination.subtract(moverPoint);
        double distance = difference.length();
        double remaining = distance - stoppingDistance;
        if (remaining <= 0.12) {
            mover.setDeltaMovement(Vec3.ZERO);
            mover.hurtMarked = true;
            return true;
        }

        Vec3 direction = difference.scale(1.0 / distance);
        Vec3 currentVelocity = mover.getDeltaMovement();
        Vec3 sidewaysVelocity = currentVelocity.subtract(direction.scale(currentVelocity.dot(direction))).scale(0.15);
        double brakingSpeed = Math.sqrt(remaining * 0.7);
        double speed = Math.min(remaining, Math.min(maxSpeed, Math.max(0.22, brakingSpeed)));
        assert speed <= remaining + 1.0E-6;

        mover.setDeltaMovement(direction.scale(speed).add(sidewaysVelocity));
        mover.resetFallDistance();
        mover.hurtMarked = true;
        return false;
    }

    public void beginFade() {
        if (getChainState() == FADING || isRemoved()) {
            return;
        }
        Entity owner = getOwnerEntity();
        Vec3 looseEnd = owner == null ? position() : approximateShoulderPosition(owner);
        setLooseEnd(looseEnd);
        looseVelocity = owner == null ? Vec3.ZERO : owner.getDeltaMovement().scale(0.45);
        entityData.set(STATE, FADING);
        entityData.set(FADE_AGE, 0);
        entityData.set(TARGET_ID, -1);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickFade() {
        int age = entityData.get(FADE_AGE) + 1;
        entityData.set(FADE_AGE, age);
        looseVelocity = looseVelocity.scale(0.92).add(0.0, -0.045, 0.0);
        setLooseEnd(getLooseEnd().add(looseVelocity));
        if (age >= FADE_DURATION) {
            discard();
        }
    }

    private void followTarget() {
        Entity target = getTargetEntity();
        if (target != null && target.isAlive()) {
            setPos(target.getBoundingBox().getCenter());
        }
    }

    private static Vec3 getOwnerRopePoint(Entity owner) {
        return owner.position().add(0.0, owner.getBbHeight() * 0.65, 0.0);
    }

    private static Vec3 approximateShoulderPosition(Entity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        if (right.lengthSqr() > 0.001) {
            right = right.normalize().scale(0.34);
        }
        return owner.getEyePosition().add(look.scale(0.04)).add(right).add(0.0, -0.27, 0.0);
    }

    public boolean isOwnedBy(Entity entity) {
        return entityData.get(OWNER_ID) == entity.getId();
    }

    public Entity getOwnerEntity() {
        int id = entityData.get(OWNER_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    public Entity getTargetEntity() {
        int id = entityData.get(TARGET_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    public Mode getMode() {
        return Mode.values()[Math.min(entityData.get(MODE), (byte) (Mode.values().length - 1))];
    }

    public byte getChainState() {
        return entityData.get(STATE);
    }

    public int getFadeAge() {
        return entityData.get(FADE_AGE);
    }

    public Vec3 getLooseEnd() {
        return new Vec3(entityData.get(LOOSE_X), entityData.get(LOOSE_Y), entityData.get(LOOSE_Z));
    }

    private void setLooseEnd(Vec3 position) {
        entityData.set(LOOSE_X, (float) position.x);
        entityData.set(LOOSE_Y, (float) position.y);
        entityData.set(LOOSE_Z, (float) position.z);
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

    private record EntityHit(Entity entity, Vec3 point) {
    }
}
