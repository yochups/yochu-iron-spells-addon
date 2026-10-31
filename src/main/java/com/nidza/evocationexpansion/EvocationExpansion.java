package com.nidza.evocationexpansion;

import com.nidza.evocationexpansion.registry.ModEffects;
import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.registry.ModParticles;
import com.nidza.evocationexpansion.registry.ModSpells;
import com.nidza.evocationexpansion.network.ModNetworking;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(EvocationExpansion.MOD_ID)
public final class EvocationExpansion {
    public static final String MOD_ID = "evocation_expansion";

    public EvocationExpansion(IEventBus modEventBus) {
        ModSpells.SPELLS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
