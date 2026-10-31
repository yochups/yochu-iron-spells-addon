package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.entity.DisarmProjectile;
import com.nidza.evocationexpansion.particle.WhiteStrikeParticleOptions;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DisarmSpell extends AbstractSpell {
    private static final ResourceLocation ID = EvocationExpansion.id("disarm");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public DisarmSpell() {
        baseManaCost = 25;
        manaCostPerLevel = 5;
        castTime = 10;
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
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_WAVY_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST_FINISH;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        Vec3 forward = caster.getLookAngle().normalize();
        DisarmProjectile projectile = new DisarmProjectile(level, caster);
        projectile.setPos(caster.getEyePosition().add(forward.scale(0.5)));
        projectile.shoot(forward);
        level.addFreshEntity(projectile);

        Vec3 effectPosition = caster.position()
                .add(0, caster.getBbHeight() * 0.3 + 0.5, 0)
                .add(forward.scale(1.9));
        MagicManager.spawnParticles(
                level,
                new WhiteStrikeParticleOptions((float) forward.x, (float) forward.y, (float) forward.z, 0.55f),
                effectPosition.x,
                effectPosition.y,
                effectPosition.z,
                1,
                0,
                0,
                0,
                0,
                true
        );
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }
}
