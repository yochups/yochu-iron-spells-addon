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

import java.util.List;

public final class HookShotSpell extends AbstractSpell {
    public static final ResourceLocation ID = EvocationExpansion.id("hook_shot");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(7)
            .build();

    public HookShotSpell() {
        baseManaCost = 18;
        manaCostPerLevel = 4;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        ChainAnchorEntity anchor = new ChainAnchorEntity(level, caster, ChainAnchorEntity.Mode.HOOK_SHOT, getRange(spellLevel));
        level.addFreshEntity(anchor);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.THROW_SINGLE_ITEM;
    }

    public static float getRange(int spellLevel) {
        return 14.0f + 3.0f * (spellLevel - 1);
    }
}
