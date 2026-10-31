package com.nidza.evocationexpansion.client;

import com.nidza.evocationexpansion.EvocationExpansion;
import com.nidza.evocationexpansion.client.particle.WhiteSlashParticle;
import com.nidza.evocationexpansion.client.particle.WhiteStrikeParticle;
import com.nidza.evocationexpansion.entity.ChainAnchorEntity;
import com.nidza.evocationexpansion.registry.ModEntities;
import com.nidza.evocationexpansion.registry.ModParticles;
import com.nidza.evocationexpansion.spell.HookShotSpell;
import com.nidza.evocationexpansion.spell.SuccessiveFlameStrikeSpell;
import com.nidza.evocationexpansion.spell.SwingSpell;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = EvocationExpansion.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final double ACTIVE_CHAIN_SEARCH_RADIUS = 48.0;
    private static final Map<PlayerModel<?>, HiddenPlayerRenderState> HIDDEN_PLAYER_STATE = new IdentityHashMap<>();
    private static final float SUCCESSIVE_SLASH_ANIMATION_SPEED =
            15.0f / SuccessiveFlameStrikeSpell.SLASH_ANIMATION_TICKS;
    private static final Map<Player, SuccessiveSlashAnimationModifiers> SUCCESSIVE_SLASH_MODIFIERS =
            new WeakHashMap<>();
    private static HiddenFirstPersonAnimation hiddenFirstPersonAnimation;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WHITE_SLASH.get(), WhiteSlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WHITE_STRIKE.get(), WhiteStrikeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.DISARM_PROJECTILE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.CHAIN_ANCHOR.get(), ChainAnchorRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_FIREBALL.get(), MagicFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.SUCCESSIVE_FLAME_STRIKE.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ARROW_STORM.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ARROW_STORM_ARROW.get(), ArrowStormArrowRenderer::new);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hideRightArm(RenderPlayerEvent.Pre event) {
        if (!hasActiveChain(event.getEntity())) {
            return;
        }

        PlayerModel<?> model = event.getRenderer().getModel();
        Player player = event.getEntity();
        InteractionHand hiddenHand = getRightHand(player);
        HIDDEN_PLAYER_STATE.put(model, new HiddenPlayerRenderState(
                model.rightArm.visible,
                model.rightSleeve.visible,
                model.rightArm.xScale,
                model.rightArm.yScale,
                model.rightArm.zScale,
                player,
                hiddenHand,
                player.getItemInHand(hiddenHand)
        ));
        model.rightArm.visible = false;
        model.rightSleeve.visible = false;
        model.rightArm.xScale = 0.0f;
        model.rightArm.yScale = 0.0f;
        model.rightArm.zScale = 0.0f;
        player.setItemInHand(hiddenHand, ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreRightArm(RenderPlayerEvent.Post event) {
        PlayerModel<?> model = event.getRenderer().getModel();
        HiddenPlayerRenderState state = HIDDEN_PLAYER_STATE.remove(model);
        if (state != null) {
            model.rightArm.visible = state.arm();
            model.rightSleeve.visible = state.sleeve();
            model.rightArm.xScale = state.armScaleX();
            model.rightArm.yScale = state.armScaleY();
            model.rightArm.zScale = state.armScaleZ();
            state.player().setItemInHand(state.hand(), state.item());
        }
    }

    @SubscribeEvent
    public static void hideFirstPersonRightHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && isRightHand(player, event.getHand()) && hasActiveChain(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideFirstPersonRightArm(RenderArmEvent event) {
        if (event.getArm() == HumanoidArm.RIGHT && hasActiveChain(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void hideIronFirstPersonModel(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            restoreIronFirstPersonModel();
            return;
        }

        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(SpellAnimations.ANIMATION_RESOURCE);
        IAnimation currentAnimation = layer == null ? null : layer.getAnimation();
        if (hasActiveChain(player) && currentAnimation instanceof KeyframeAnimationPlayer animation) {
            if (hiddenFirstPersonAnimation == null || hiddenFirstPersonAnimation.animation() != animation) {
                restoreIronFirstPersonModel();
                hiddenFirstPersonAnimation = new HiddenFirstPersonAnimation(
                        animation,
                        animation.getFirstPersonMode(0.0f),
                        animation.getFirstPersonConfiguration(0.0f)
                );
            }
            animation.setFirstPersonMode(FirstPersonMode.DISABLED);
        } else {
            restoreIronFirstPersonModel();
        }
    }

    private static void restoreIronFirstPersonModel() {
        if (hiddenFirstPersonAnimation == null) {
            return;
        }
        hiddenFirstPersonAnimation.animation()
                .setFirstPersonMode(hiddenFirstPersonAnimation.mode())
                .setFirstPersonConfiguration(hiddenFirstPersonAnimation.configuration());
        hiddenFirstPersonAnimation = null;
    }

    @SuppressWarnings("unchecked")
    public static void playSuccessiveFlameStrikeAnimation(int playerId, boolean mirrored) {
        if (Minecraft.getInstance().level == null
                || !(Minecraft.getInstance().level.getEntity(playerId) instanceof AbstractClientPlayer player)) {
            return;
        }

        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(SpellAnimations.ANIMATION_RESOURCE);
        if (layer == null) {
            return;
        }

        SuccessiveSlashAnimationModifiers modifiers = SUCCESSIVE_SLASH_MODIFIERS.get(player);
        if (modifiers == null) {
            SuccessiveSlashMirrorModifier mirrorModifier = new SuccessiveSlashMirrorModifier(player);
            SuccessiveSlashSpeedModifier speedModifier = new SuccessiveSlashSpeedModifier(player);
            layer.addModifierLast(mirrorModifier);
            layer.addModifierLast(speedModifier);
            modifiers = new SuccessiveSlashAnimationModifiers(mirrorModifier, speedModifier);
            SUCCESSIVE_SLASH_MODIFIERS.put(player, modifiers);
        }
        modifiers.mirror().setMirrored(mirrored);
        SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION.getForPlayer()
                .ifPresent(animation -> AnimationHelper.animatePlayerStart(player, animation));
    }

    public static boolean hasActiveChain(Player player) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.isCasting()) {
            String spellId = magicData.getCastingSpellId();
            if (SwingSpell.ID.toString().equals(spellId) || HookShotSpell.ID.toString().equals(spellId)) {
                return true;
            }
        }

        return !player.level().getEntitiesOfClass(
                ChainAnchorEntity.class,
                player.getBoundingBox().inflate(ACTIVE_CHAIN_SEARCH_RADIUS),
                anchor -> anchor.isOwnedBy(player) && anchor.getChainState() != ChainAnchorEntity.FADING
        ).isEmpty();
    }

    private static boolean isRightHand(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() == HumanoidArm.RIGHT
                : player.getMainArm() == HumanoidArm.LEFT;
    }

    private static InteractionHand getRightHand(Player player) {
        return player.getMainArm() == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private record HiddenPlayerRenderState(boolean arm, boolean sleeve,
                                           float armScaleX, float armScaleY, float armScaleZ, Player player,
                                           InteractionHand hand, ItemStack item) {
    }

    private record HiddenFirstPersonAnimation(KeyframeAnimationPlayer animation, FirstPersonMode mode,
                                              FirstPersonConfiguration configuration) {
    }

    private static final class SuccessiveSlashMirrorModifier extends MirrorModifier {
        private final Player player;
        private boolean mirrored;

        private SuccessiveSlashMirrorModifier(Player player) {
            this.player = player;
        }

        private void setMirrored(boolean mirrored) {
            this.mirrored = mirrored;
        }

        @Override
        public boolean isEnabled() {
            return mirrored && isCastingSuccessiveFlameStrike(player);
        }
    }

    private static final class SuccessiveSlashSpeedModifier extends SpeedModifier {
        private final Player player;

        private SuccessiveSlashSpeedModifier(Player player) {
            this.player = player;
        }

        @Override
        protected void step(float delta) {
            speed = isCastingSuccessiveFlameStrike(player) ? SUCCESSIVE_SLASH_ANIMATION_SPEED : 1.0f;
            super.step(delta);
        }
    }

    private static boolean isCastingSuccessiveFlameStrike(Player player) {
        return SuccessiveFlameStrikeSpell.ID.toString().equals(
                ClientMagicData.getSyncedSpellData(player).getCastingSpellId()
        );
    }

    private record SuccessiveSlashAnimationModifiers(SuccessiveSlashMirrorModifier mirror,
                                                      SuccessiveSlashSpeedModifier speed) {
    }
}
