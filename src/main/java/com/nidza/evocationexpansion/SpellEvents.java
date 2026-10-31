package com.nidza.evocationexpansion;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EvocationExpansion.MOD_ID)
public final class SpellEvents {
    private SpellEvents() {
    }

    @SubscribeEvent
    public static void reduceCooldownBySpellLevel(SpellCooldownAddedEvent.Pre event) {
        if (!event.getSpell().getSpellResource().getNamespace().equals(EvocationExpansion.MOD_ID)) {
            return;
        }

        if (event.getEffectiveCooldown() <= 0) {
            return;
        }

        int castingLevel = MagicData.getPlayerMagicData(event.getEntity()).getCastingSpellLevel();
        int spellLevel = Math.max(1, Math.min(castingLevel, event.getSpell().getMaxLevel()));
        float multiplier = 1.0f - 0.1f * (spellLevel - 1);
        event.setEffectiveCooldown(Math.max(1, Math.round(event.getEffectiveCooldown() * multiplier)));
    }
}
