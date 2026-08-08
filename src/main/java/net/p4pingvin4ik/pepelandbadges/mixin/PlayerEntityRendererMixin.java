package net.p4pingvin4ik.pepelandbadges.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.p4pingvin4ik.pepelandbadges.client.PepelandbadgesClient;
import net.p4pingvin4ik.pepelandbadges.util.NameplateTextUtil;
import net.p4pingvin4ik.pepelandbadges.util.PlayerListEntryTabText;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper.TabNameParts;
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

        state.nameTag = pepeland$applyTabGradientToPlainName(state.nameTag, state);
    }

    private static Component pepeland$applyTabGradientToPlainName(Component text, AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.level == null) {
            return text;
        }

        Player player = null;
        for (Player p : client.level.players()) {
            if (p.getId() == state.id) {
                player = p;
                break;
            }
        }
        if (player == null) {
            return text;
        }

        PlayerInfo entry = client.getConnection().getPlayerInfo(player.getUUID());
        if (entry == null) {
            return text;
        }

        Component tabLine = PlayerListEntryTabText.getEffectiveDisplayName(entry);
        String realName = player.getName().getString();
        TabNameParts parts = TabNameplateHelper.splitTabName(tabLine, realName);
        if (parts.name() == null) {
            return text;
        }

        Component gradientName = parts.name().copy();
        String full = text.getString();
        if (!full.contains(realName)) {
            return text;
        }

        Component protectedPrefix = TabNameplateHelper.protect(parts.prefix());
        int prefixLen = protectedPrefix.getString().length();
        int bodyStart = prefixLen;
        if (bodyStart < full.length() && full.charAt(bodyStart) == ' ') {
            bodyStart++;
        }

        if (bodyStart + realName.length() > full.length()) {
            return text;
        }
        if (!full.regionMatches(bodyStart, realName, 0, realName.length())) {
            return text;
        }

        return NameplateTextUtil.replaceUtf16Range(text, bodyStart, bodyStart + realName.length(), gradientName);
    }
}
