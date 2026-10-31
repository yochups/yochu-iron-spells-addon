package com.nidza.evocationexpansion.entity;

import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.registry.ModSpells;
import com.nidza.evocationexpansion.network.SuccessiveFlameStrikeAnimationPayload;
import com.nidza.evocationexpansion.spell.SuccessiveFlameStrikeSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SuccessiveFlameStrikeEntity extends Entity {
    private int ownerId = -1;
    private int spellLevel = 1;
    private int slashIndex;

    public SuccessiveFlameStrikeEntity(EntityType<? extends SuccessiveFlameStrikeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public SuccessiveFlameStrikeEntity(Level level, LivingEntity owner, int spellLevel) {
        this(ModEntities.SUCCESSIVE_FLAME_STRIKE.get(), level);
        ownerId = owner.getId();
        this.spellLevel = spellLevel;
        setPos(owner.position());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        Entity owner = level().getEntity(ownerId);
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive()) {
            discard();
            return;
        }
        setPos(caster.position());

        MagicData magicData = MagicData.getPlayerMagicData(caster);
        if (!magicData.isCasting()
                || !SuccessiveFlameStrikeSpell.ID.toString().equals(magicData.getCastingSpellId())) {
            discard();
            return;
        }

        int sequenceTick = tickCount - SuccessiveFlameStrikeSpell.WINDUP_TICKS;
        if (sequenceTick < 0) {
            return;
        }

        int intervalTick = sequenceTick % SuccessiveFlameStrikeSpell.SLASH_INTERVAL_TICKS;
        if (intervalTick == 0 && slashIndex < SuccessiveFlameStrikeSpell.SLASH_COUNT) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    caster,
                    new SuccessiveFlameStrikeAnimationPayload(caster.getId(), slashIndex % 2 == 1)
            );
        } else if (intervalTick == SuccessiveFlameStrikeSpell.SLASH_PARTICLE_DELAY_TICKS) {
            assert slashIndex >= 0 && slashIndex < SuccessiveFlameStrikeSpell.SLASH_COUNT;
            boolean alternateDirection = slashIndex % 2 == 1;
            boolean handedMirror = magicData.getCastingEquipmentSlot().equals(SpellSelectionManager.OFFHAND)
                    ^ caster.getMainArm() == HumanoidArm.LEFT;
            ((SuccessiveFlameStrikeSpell) ModSpells.SUCCESSIVE_FLAME_STRIKE.get())
                    .performSlash((ServerLevel) level(), spellLevel, caster, alternateDirection ^ handedMirror);
            slashIndex++;
            if (slashIndex >= SuccessiveFlameStrikeSpell.SLASH_COUNT) {
                discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public boolean isOwnedBy(Entity entity) {
        return ownerId == entity.getId();
    }
}
