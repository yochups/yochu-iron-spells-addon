package com.nidza.evocationexpansion.registry;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.effect.FloatEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EvocationExpansion.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FLOATING = EFFECTS.register("floating", () ->
            new FloatEffect().addAttributeModifier(
                    Attributes.GRAVITY,
                    EvocationExpansion.id("floating_gravity"),
                    -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

    private ModEffects() {
    }
}
