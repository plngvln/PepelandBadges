package net.p4pingvin4ik.pepelandbadges.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.p4pingvin4ik.pepelandbadges.client.PepelandbadgesClient;
import net.p4pingvin4ik.pepelandbadges.util.NameplateBadges;
import net.p4pingvin4ik.pepelandbadges.util.NameplateStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderer.class, priority = 1100)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "extractRenderState",
            at = @At("RETURN")
    )
    private void modifyDisplayNameAfterUpdate(T entity, S state, float tickProgress, CallbackInfo ci) {
        ((NameplateStateAccess) state).pepeland$setVanillaExtracted(true);

        if (state.nameTag == null || !PepelandbadgesClient.MOD_ENABLED || !(entity instanceof Player)) {
            return;
        }

        state.nameTag = NameplateBadges.applyBadges((Player) entity, state.nameTag);
    }

    /**
     * Culling mods such as EntityCulling cancel the vanilla extraction and hand over a bare render
     * state carrying the undecorated display name, so badges are applied here as a last resort.
     */
    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("HEAD")
    )
    private void pepeland$badgeUnextractedNameTag(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, int verticalOffset, CallbackInfo ci) {
        if (!PepelandbadgesClient.MOD_ENABLED || state.nameTag == null) {
            return;
        }
        if (((NameplateStateAccess) state).pepeland$isVanillaExtracted()) {
            return;
        }

        Player player = NameplateBadges.resolvePlayer(state);
        if (player == null) {
            return;
        }

        Component badged = NameplateBadges.applyBadges(player, state.nameTag);
        state.nameTag = NameplateBadges.applyTabGradient(player, badged);
    }
}
