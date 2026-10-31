package com.nidza.evocationexpansion.spell;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.entity.ChainAnchorEntity;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SwingSpell extends AbstractSpell {
    public static final ResourceLocation ID = EvocationExpansion.id("swing");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public SwingSpell() {
        baseManaCost = 1;
        manaCostPerLevel = 0;
        castTime = 200;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
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
                "ui.irons_spellbooks.distance",
                Utils.stringTruncation(getRange(spellLevel), 1)
        ));
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, @Nullable MagicData magicData) {
        super.onServerPreCast(level, spellLevel, caster, magicData);
        ChainAnchorEntity anchor = new ChainAnchorEntity(level, caster, ChainAnchorEntity.Mode.SWING, getRange(spellLevel));
        level.addFreshEntity(anchor);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData magicData, boolean cancelled) {
        level.getEntitiesOfClass(
                ChainAnchorEntity.class,
                caster.getBoundingBox().inflate(getRange(spellLevel) + 8.0),
                anchor -> anchor.isOwnedBy(caster) && anchor.getMode() == ChainAnchorEntity.Mode.SWING
        ).forEach(ChainAnchorEntity::releaseSwing);
        super.onServerCastComplete(level, spellLevel, caster, magicData, cancelled);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    public static float getRange(int spellLevel) {
        return 16.0f + 4.0f * (spellLevel - 1);
    }
}
