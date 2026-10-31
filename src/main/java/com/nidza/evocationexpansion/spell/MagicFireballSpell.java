package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.entity.HomingMagicFireball;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public final class MagicFireballSpell extends AbstractSpell {
    public static final ResourceLocation ID = EvocationExpansion.id("magic_fireball");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(25)
            .build();

    public MagicFireballSpell() {
        manaCostPerLevel = 15;
        baseSpellPower = 1;
        spellPowerPerLevel = 1;
        castTime = 40;
        baseManaCost = 60;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIREBALL_START.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamage(spellLevel, caster), 2)
                ),
                Component.translatable("ui.irons_spellbooks.radius", getRadius(spellLevel, caster))
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        Vec3 origin = caster.getEyePosition();
        HomingMagicFireball fireball = new HomingMagicFireball(level, caster);
        fireball.setDamage(getDamage(spellLevel, caster));
        fireball.setExplosionRadius(getRadius(spellLevel, caster));
        fireball.setPos(origin.add(caster.getForward()).subtract(0.0, fireball.getBbHeight() / 2.0, 0.0));
        fireball.shoot(caster.getLookAngle());

        level.addFreshEntity(fireball);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return 5.0f + 5.0f * getSpellPower(spellLevel, caster);
    }

    public int getRadius(int spellLevel, LivingEntity caster) {
        return 2 + (int) getSpellPower(spellLevel, caster);
    }
}
