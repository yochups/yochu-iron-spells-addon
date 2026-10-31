package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.registry.ModSpells;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.small_magic_arrow.SmallMagicArrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public final class ArrowStormArrow extends SmallMagicArrow {
    public ArrowStormArrow(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public ArrowStormArrow(Level level, Entity owner) {
        this(ModEntities.ARROW_STORM_ARROW.get(), level);
        setOwner(owner);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (level().isClientSide || getOwner() == null) {
            return;
        }

        Entity target = result.getEntity();
        if (DamageSources.applyDamage(
                target,
                getDamage(),
                ModSpells.ARROW_STORM.get().getDamageSource(this, getOwner())
        )) {
            consumeEntityImpact(result, true);
        } else {
            setDeltaMovement(getDeltaMovement().scale(-0.1));
            setYRot(getYRot() + 180.0f);
            yRotO += 180.0f;
        }
    }
}
