package net.p4pingvin4ik.pepelandbadges.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import net.p4pingvin4ik.pepelandbadges.client.PepelandbadgesClient;
import net.p4pingvin4ik.pepelandbadges.util.NameplateBadges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AvatarRenderer.class, priority = 1100)
public abstract class PlayerEntityRendererMixin {

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void pepeland$tabGradientAfterFigura(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (!PepelandbadgesClient.MOD_ENABLED || state.nameTag == null) {
            return;
        }

        Player player = NameplateBadges.resolvePlayer(state);
        if (player == null) {
            return;
        }

        state.nameTag = NameplateBadges.applyTabGradient(player, state.nameTag);
    }
}
