package com.nidza.evocationexpansion.client;

import com.nidza.evocationexpansion.EvocationExpansion;
import io.redspace.ironsspellbooks.entity.spells.fireball.FireballRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;

public final class MagicFireballRenderer extends FireballRenderer {
    private static final ResourceLocation CORE_TEXTURE =
            EvocationExpansion.id("textures/entity/magic_fireball/fireball_core.png");
    private static final ResourceLocation[] FIRE_TEXTURES = {
            EvocationExpansion.id("textures/entity/magic_fireball/fire_0.png"),
            EvocationExpansion.id("textures/entity/magic_fireball/fire_1.png"),
            EvocationExpansion.id("textures/entity/magic_fireball/fire_2.png"),
            EvocationExpansion.id("textures/entity/magic_fireball/fire_3.png")
    };

    public MagicFireballRenderer(EntityRendererProvider.Context context) {
        super(context, 1.25f);
    }

    @Override
    public ResourceLocation getTextureLocation(Projectile entity) {
        return CORE_TEXTURE;
    }

    @Override
    public ResourceLocation getFireTextureLocation(Projectile entity) {
        return FIRE_TEXTURES[(entity.tickCount / 2) % FIRE_TEXTURES.length];
    }
}
