package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.mixin.AbstractArrowAccessor;
import io.redspace.ironsspellbooks.particle.EnderSlashParticleOptions;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HeavyShadowSlashSpell extends AbstractSpell {
    public static final ResourceLocation ID = EvocationExpansion.id("heavy_shadow_slash");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(20)
            .build();

    public HeavyShadowSlashSpell() {
        baseManaCost = 50;
        manaCostPerLevel = 15;
        baseSpellPower = 8;
        spellPowerPerLevel = 2;
        castTime = 20;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return ID;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.SHADOW_SLASH.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable(
                "ui.irons_spellbooks.damage",
                getDamageText(spellLevel, caster)
        ));
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity caster, ICastData castData) {
        super.onClientCast(level, spellLevel, caster, castData);
        caster.setYBodyRot(caster.getYRot());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        float distance = 12.0f;
        Vec3 forward = caster.getForward();
        Vec3 end = Utils.raycastForBlock(
                level,
                caster.getEyePosition(),
                caster.getEyePosition().add(forward.scale(distance)),
                ClipContext.Fluid.NONE
        ).getLocation();
        AABB hitbox = caster.getHitbox().expandTowards(end.subtract(caster.getEyePosition())).inflate(2.4);
        var targets = level.getEntities(caster, hitbox, entity ->
                !entity.isSpectator()
                        && (entity instanceof LivingEntity || entity instanceof Projectile)
                        && entity.getBoundingBox().getCenter()
                        .subtract(caster.getBoundingBox().getCenter())
                        .normalize()
                        .dot(caster.getForward()) >= 0.82
        );
        targets.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(caster)));

        if (!targets.isEmpty() && targets.getFirst().distanceToSqr(caster) < distance * distance) {
            Entity closest = targets.getFirst();
            float radius = 3.0f;
            AABB damageBox = AABB.ofSize(
                    closest.getBoundingBox().getCenter(),
                    radius,
                    radius + 1.2,
                    radius
            ).move(forward.scale(radius / 2.0));
            end = damageBox.getCenter().add(end).scale(0.5);
            var damageSource = getDamageSource(caster);
            boolean parriedProjectile = false;

            for (Entity target : level.getEntities(caster, damageBox)) {
                if (target instanceof Projectile projectile
                        && !projectile.noPhysics
                        && !(projectile instanceof AbstractArrow arrow && ((AbstractArrowAccessor) arrow).isInGround())
                        && !projectile.getType().is(ModTags.CANT_PARRY)) {
                    parriedProjectile = true;
                    projectile.setOwner(caster);
                    projectile.shoot(forward.x, forward.y, forward.z,
                            (float) projectile.getDeltaMovement().length() * 1.25f, 0.0f);
                } else if (target.isAlive()
                        && caster.isPickable()
                        && Utils.hasLineOfSight(level, caster.getEyePosition(), target.getBoundingBox().getCenter(), true)
                        && DamageSources.applyDamage(target, getDamage(spellLevel, caster), damageSource)) {
                    MagicManager.spawnParticles(
                            level,
                            ParticleHelper.ENDER_SPARKS,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5,
                            target.getZ(),
                            24,
                            target.getBbWidth() * 0.5,
                            target.getBbHeight() * 0.5,
                            target.getBbWidth() * 0.5,
                            0.3,
                            false
                    );
                    EnchantmentHelper.doPostAttackEffects((ServerLevel) level, target, damageSource);
                    Vec3 knockback = target.position()
                            .subtract(caster.position())
                            .normalize()
                            .add(0, 0.6, 0)
                            .normalize()
                            .scale(1.25 * Utils.clampedKnockbackResistanceFactor(target, 0.2f, 1.0f));
                    target.setDeltaMovement(target.getDeltaMovement().add(knockback));
                    target.hurtMarked = true;
                }
            }

            if (parriedProjectile) {
                level.playSound(null, closest.getX(), closest.getY(), closest.getZ(),
                        SoundRegistry.FIRE_DAGGER_PARRY.get(), caster.getSoundSource());
                MagicManager.spawnParticles(level, ParticleHelper.ENDER_SPARKS,
                        closest.getX(), closest.getY() + closest.getBbHeight() * 0.5, closest.getZ(),
                        32, 0, 0, 0, 0.45, false);
            }
        }

        Vec3 rayVector = end.subtract(caster.getEyePosition());
        Vec3 impulse = rayVector.scale(1.0 / 5.0).add(0, 0.12, 0);
        caster.setDeltaMovement(caster.getDeltaMovement().scale(0.15).add(impulse));
        caster.hurtMarked = true;
        caster.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY, 24, 0, false, false, true));

        forward = impulse.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (forward.dot(up) > 0.999) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = up.cross(forward);
        Vec3 particlePosition = end.subtract(forward.scale(3.3)).add(right.scale(-0.3));
        MagicManager.spawnParticles(
                level,
                new EnderSlashParticleOptions(
                        (float) forward.x,
                        (float) forward.y,
                        (float) forward.z,
                        (float) right.x,
                        (float) right.y,
                        (float) right.z,
                        1.2f
                ),
                particlePosition.x,
                particlePosition.y + 0.3,
                particlePosition.z,
                1,
                0,
                0,
                0,
                0,
                true
        );

        int trailParticles = 20;
        double trailSpeed = rayVector.length() / 12.0 * 0.85;
        for (int i = 0; i < trailParticles; i++) {
            Vec3 start = caster.getBoundingBox().getCenter().add(Utils.getRandomVec3(1 + caster.getBbWidth()));
            Vec3 finish = start.add(rayVector);
            MagicManager.spawnParticles(
                    level,
                    new TraceParticleOptions(Utils.v3f(finish), new Vector3f(1.0f, 0.333f, 1.0f)),
                    start.x,
                    start.y,
                    start.z,
                    1,
                    0,
                    0,
                    0,
                    trailSpeed,
                    false
            );
        }

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) + Utils.getWeaponDamage(caster);
    }

    private String getDamageText(int spellLevel, LivingEntity caster) {
        if (caster == null) {
            return Utils.stringTruncation(getSpellPower(spellLevel, null), 1);
        }
        float weaponDamage = Utils.getWeaponDamage(caster);
        String weaponText = weaponDamage > 0
                ? " (+" + Utils.stringTruncation(weaponDamage, 1) + ")"
                : "";
        return Utils.stringTruncation(getDamage(spellLevel, caster), 1) + weaponText;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ONE_HANDED_VERTICAL_UPSWING_ANIMATION;
    }
}
