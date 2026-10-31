package com.nidza.evocationexpansion.registry;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.entity.ChainAnchorEntity;
import com.nidza.evocationexpansion.entity.ArrowStormEntity;
import com.nidza.evocationexpansion.entity.ArrowStormArrow;
import com.nidza.evocationexpansion.entity.DisarmProjectile;
import com.nidza.evocationexpansion.entity.HomingMagicFireball;
import com.nidza.evocationexpansion.entity.SuccessiveFlameStrikeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EvocationExpansion.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DisarmProjectile>> DISARM_PROJECTILE =
            ENTITIES.register("disarm_projectile", () -> EntityType.Builder
                    .<DisarmProjectile>of(DisarmProjectile::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("disarm_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChainAnchorEntity>> CHAIN_ANCHOR =
            ENTITIES.register("chain_anchor", () -> EntityType.Builder
                    .<ChainAnchorEntity>of(ChainAnchorEntity::new, MobCategory.MISC)
                    .sized(0.2f, 0.2f)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("chain_anchor"));

    public static final DeferredHolder<EntityType<?>, EntityType<HomingMagicFireball>> MAGIC_FIREBALL =
            ENTITIES.register("magic_fireball", () -> EntityType.Builder
                    .<HomingMagicFireball>of(HomingMagicFireball::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("magic_fireball"));

    public static final DeferredHolder<EntityType<?>, EntityType<SuccessiveFlameStrikeEntity>> SUCCESSIVE_FLAME_STRIKE =
            ENTITIES.register("successive_flame_strike", () -> EntityType.Builder
                    .<SuccessiveFlameStrikeEntity>of(SuccessiveFlameStrikeEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("successive_flame_strike"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArrowStormEntity>> ARROW_STORM =
            ENTITIES.register("arrow_storm", () -> EntityType.Builder
                    .<ArrowStormEntity>of(ArrowStormEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("arrow_storm"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArrowStormArrow>> ARROW_STORM_ARROW =
            ENTITIES.register("arrow_storm_arrow", () -> EntityType.Builder
                    .<ArrowStormArrow>of(ArrowStormArrow::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("arrow_storm_arrow"));

    private ModEntities() {
    }
}
