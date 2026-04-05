package net.p4pingvin4ik.pepelandbadges.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.p4pingvin4ik.pepelandbadges.client.PepelandbadgesClient;
import net.p4pingvin4ik.pepelandbadges.util.NameplateTextUtil;
import net.p4pingvin4ik.pepelandbadges.util.PlayerListEntryTabText;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper.TabNameParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntityRenderer.class, priority = 1100)
public abstract class PlayerEntityRendererMixin {

    @Unique
    private static final ThreadLocal<PlayerEntityRenderState> PEPeland_CURRENT_STATE = new ThreadLocal<>();

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void pepeland$captureState(PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PEPeland_CURRENT_STATE.set(state);
    }

    @Inject(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void pepeland$clearState(PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        PEPeland_CURRENT_STATE.remove();
    }

    @ModifyArg(
            method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 1
            ),
            index = 1
    )
    private Text pepeland$tabGradientAfterFigura(Text text) {
        if (!PepelandbadgesClient.MOD_ENABLED) {
            return text;
        }

        PlayerEntityRenderState state = PEPeland_CURRENT_STATE.get();
        if (state == null) {
            return text;
        }

        return pepeland$applyTabGradientToPlainName(text, state);
    }

    private static Text pepeland$applyTabGradientToPlainName(Text text, PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.world == null) {
            return text;
        }

        PlayerEntity player = null;
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p.getId() == state.id) {
                player = p;
                break;
            }
        }
        if (player == null) {
            return text;
        }

        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null) {
            return text;
        }

        Text tabLine = PlayerListEntryTabText.getEffectiveDisplayName(entry);
        String realName = player.getName().getString();
        TabNameParts parts = TabNameplateHelper.splitTabName(tabLine, realName);
        if (parts.name() == null) {
            return text;
        }

        Text gradientName = parts.name().copy();
        String full = text.getString();
        if (!full.contains(realName)) {
            return text;
        }

        Text protectedPrefix = TabNameplateHelper.protect(parts.prefix());
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
