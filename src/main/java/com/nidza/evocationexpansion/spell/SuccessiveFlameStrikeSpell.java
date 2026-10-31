package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.entity.SuccessiveFlameStrikeEntity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class SuccessiveFlameStrikeSpell extends AbstractSpell {
    public static final ResourceLocation ID = EvocationExpansion.id("successive_flame_strike");
    public static final int SLASH_COUNT = 5;
    public static final int WINDUP_TICKS = 10;
    public static final int SLASH_INTERVAL_TICKS = 9;
    public static final int SLASH_PARTICLE_DELAY_TICKS = 2;
    public static final int SLASH_ANIMATION_TICKS = 9;
    private static final float DAMAGE_MULTIPLIER = 0.25f;
    private static final float FORWARD_IMPULSE = 0.25f;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .build();

    public SuccessiveFlameStrikeSpell() {
        manaCostPerLevel = 15;
        baseSpellPower = 5;
        spellPowerPerLevel = 2;
        castTime = WINDUP_TICKS + (SLASH_COUNT - 1) * SLASH_INTERVAL_TICKS + SLASH_ANIMATION_TICKS;
        baseManaCost = 30;
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
    public boolean canBeInterrupted(@Nullable Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FLAMING_STRIKE_UPSWING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable(
                "ui.irons_spellbooks.damage",
                getDamageText(spellLevel, caster) + " x " + SLASH_COUNT
        ));
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData magicData) {
        SuccessiveFlameStrikeEntity sequence = new SuccessiveFlameStrikeEntity(level, caster, spellLevel);
        level.addFreshEntity(sequence);
        super.onServerPreCast(level, spellLevel, caster, magicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData,
                                     boolean cancelled) {
        level.getEntitiesOfClass(
                SuccessiveFlameStrikeEntity.class,
                caster.getBoundingBox().inflate(16.0),
                sequence -> sequence.isOwnedBy(caster)
        ).forEach(Entity::discard);
        super.onServerCastComplete(level, spellLevel, caster, magicData, cancelled);
    }

    public void performSlash(ServerLevel level, int spellLevel, LivingEntity caster, boolean mirrored) {
        float radius = 3.25f;
        float distance = 1.9f;
        Vec3 forward = caster.getForward();
        Vec3 hitLocation = caster.position().add(0, caster.getBbHeight() * 0.3f, 0).add(forward.scale(distance));
        var targets = level.getEntities(caster, AABB.ofSize(hitLocation, radius * 2, radius, radius * 2));
        var damageSource = getDamageSource(caster);

        for (Entity target : targets) {
            if (target instanceof LivingEntity
                    && target.isAlive()
                    && caster.isPickable()
                    && target.position().subtract(caster.getEyePosition()).dot(forward) >= 0
                    && caster.distanceToSqr(target) < radius * radius
                    && Utils.hasLineOfSight(level, caster.getEyePosition(), target.getBoundingBox().getCenter(), true)
                    && DamageSources.applyDamage(target, getDamage(spellLevel, caster), damageSource)) {
                MagicManager.spawnParticles(
                        level,
                        ParticleHelper.FIRE,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.5f,
                        target.getZ(),
                        30,
                        target.getBbWidth() * 0.5f,
                        target.getBbHeight() * 0.5f,
                        target.getBbWidth() * 0.5f,
                        0.03,
                        false
                );
                EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
            }
        }

        MagicManager.spawnParticles(
                level,
                new FlameStrikeParticleOptions(
                        (float) forward.x,
                        (float) forward.y,
                        (float) forward.z,
                        mirrored,
                        false,
                        1.0f
                ),
                hitLocation.x,
                hitLocation.y + 0.5,
                hitLocation.z,
                1,
                0,
                0,
                0,
                0,
                true
        );
        level.playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                SoundRegistry.FLAMING_STRIKE_SWING.get(),
                caster.getSoundSource(),
                1.0f,
                1.0f
        );

        Vec3 horizontalForward = new Vec3(forward.x, 0, forward.z);
        if (horizontalForward.lengthSqr() > 1.0E-6) {
            caster.setDeltaMovement(caster.getDeltaMovement().add(horizontalForward.normalize().scale(FORWARD_IMPULSE)));
            caster.hurtMarked = true;
        }
    }

    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setFireTicks(15).setIFrames(0);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_RAISED_HAND;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return (getSpellPower(spellLevel, caster) + getAdditionalDamage(caster)) * DAMAGE_MULTIPLIER;
    }

    private float getAdditionalDamage(LivingEntity caster) {
        float weaponDamage = Utils.getWeaponDamage(caster);
        var weapon = caster.getWeaponItem();
        if (!weapon.isEmpty() && weapon.has(DataComponents.ENCHANTMENTS)) {
            weaponDamage += Utils.getEnchantmentLevel(
                    caster.level(),
                    Enchantments.FIRE_ASPECT,
                    weapon.get(DataComponents.ENCHANTMENTS)
            );
        }
        return weaponDamage;
    }

    private String getDamageText(int spellLevel, LivingEntity caster) {
        if (caster == null) {
            return Utils.stringTruncation(getSpellPower(spellLevel, null) * DAMAGE_MULTIPLIER, 1);
        }
        float weaponDamage = Utils.getWeaponDamage(caster) * DAMAGE_MULTIPLIER;
        String weaponText = weaponDamage > 0 ? " (+" + Utils.stringTruncation(weaponDamage, 1) + ")" : "";
        return Utils.stringTruncation(getDamage(spellLevel, caster), 1) + weaponText;
    }
}
