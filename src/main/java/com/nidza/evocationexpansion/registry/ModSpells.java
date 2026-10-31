package com.nidza.evocationexpansion.registry;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.spell.DisarmSpell;
import com.nidza.evocationexpansion.spell.ArrowStormSpell;
import com.nidza.evocationexpansion.spell.FloatSpell;
import com.nidza.evocationexpansion.spell.HookShotSpell;
import com.nidza.evocationexpansion.spell.HeavyShadowSlashSpell;
import com.nidza.evocationexpansion.spell.MagicFireballSpell;
import com.nidza.evocationexpansion.spell.SwingSpell;
import com.nidza.evocationexpansion.spell.SuccessiveFlameStrikeSpell;
import com.nidza.evocationexpansion.spell.UppercutSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, EvocationExpansion.MOD_ID);

    public static final Supplier<AbstractSpell> DISARM = register(new DisarmSpell());
    public static final Supplier<AbstractSpell> FLOAT = register(new FloatSpell());
    public static final Supplier<AbstractSpell> UPPERCUT = register(new UppercutSpell());
    public static final Supplier<AbstractSpell> SWING = register(new SwingSpell());
    public static final Supplier<AbstractSpell> HOOK_SHOT = register(new HookShotSpell());
    public static final Supplier<AbstractSpell> MAGIC_FIREBALL = register(new MagicFireballSpell());
    public static final Supplier<AbstractSpell> SUCCESSIVE_FLAME_STRIKE = register(new SuccessiveFlameStrikeSpell());
    public static final Supplier<AbstractSpell> ARROW_STORM = register(new ArrowStormSpell());
    public static final Supplier<AbstractSpell> HEAVY_SHADOW_SLASH = register(new HeavyShadowSlashSpell());

    private ModSpells() {
    }

    private static Supplier<AbstractSpell> register(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }
}
