package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.particle.WhiteSlashParticleOptions;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class UppercutSpell extends AbstractSpell {
    private static final ResourceLocation ID = EvocationExpansion.id("uppercut");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public UppercutSpell() {
        baseManaCost = 20;
        manaCostPerLevel = 4;
        baseSpellPower = 2;
        spellPowerPerLevel = 1;
        castTime = 6;
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
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable(
                "ui.irons_spellbooks.damage",
                Utils.stringTruncation(getSpellPower(spellLevel, caster), 1)
        ));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 effectPosition = caster.position().add(look.scale(1.4)).add(0, 0.7, 0);
        AABB hitbox = AABB.ofSize(effectPosition, 3.5, 3.0, 3.5);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitbox, target -> target != caster)) {
            Vec3 direction = target.getBoundingBox().getCenter().subtract(caster.getEyePosition()).normalize();
            if (direction.dot(look) >= 0.35 && !DamageSources.isFriendlyFireBetween(caster, target)) {
                DamageSources.applyDamage(target, getSpellPower(spellLevel, caster), getDamageSource(caster));
                Vec3 movement = target.getDeltaMovement();
                target.setDeltaMovement(movement.x * 0.4, 1.1, movement.z * 0.4);
                target.resetFallDistance();
                target.hurtMarked = true;
            }
        }

        Vec3 casterMovement = caster.getDeltaMovement();
        caster.setDeltaMovement(casterMovement.x, Math.max(casterMovement.y, 0.9), casterMovement.z);
        caster.resetFallDistance();
        caster.hurtMarked = true;

        Vec3 horizontalLook = new Vec3(look.x, 0, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-6) {
            horizontalLook = new Vec3(0, 0, 1);
        } else {
            horizontalLook = horizontalLook.normalize();
        }
        Vec3 horizontalRight = new Vec3(-horizontalLook.z, 0, horizontalLook.x);
        Vec3 particlePosition = effectPosition.add(horizontalRight.scale(0.3));
        MagicManager.spawnParticles(
                level,
                new WhiteSlashParticleOptions(
                        0,
                        1,
                        0,
                        (float) horizontalRight.x,
                        0,
                        (float) horizontalRight.z,
                        0.8f,
                        caster.getId(),
                        1.4f,
                        0.7f,
                        0.3f
                ),
                particlePosition.x,
                particlePosition.y,
                particlePosition.z,
                1,
                0,
                0,
                0,
                0,
                true
        );
        super.onCast(level, spellLevel, caster, castSource, magicData);
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
