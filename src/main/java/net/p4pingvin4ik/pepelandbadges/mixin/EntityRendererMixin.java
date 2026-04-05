package net.p4pingvin4ik.pepelandbadges.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.p4pingvin4ik.pepelandbadges.client.PepelandbadgesClient;
import net.p4pingvin4ik.pepelandbadges.util.PlayerListEntryTabText;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper.TabNameParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = EntityRenderer.class, priority = 1100)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "updateRenderState",
            at = @At("RETURN")
    )
    private void modifyDisplayNameAfterUpdate(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (state.displayName == null || !PepelandbadgesClient.MOD_ENABLED || !(entity instanceof PlayerEntity)) {
            return;
        }

        Text modifiedText = getBadgesTextForPlayer((PlayerEntity) entity, state.displayName);

        state.displayName = modifiedText;
    }

    @Unique
    private Text getBadgesTextForPlayer(PlayerEntity player, Text originalName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return originalName;
        }

        PlayerListEntry playerListEntry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) {
            return originalName;
        }

        Text tabDisplayName = PlayerListEntryTabText.getEffectiveDisplayName(playerListEntry);
        String realNameString = player.getName().getString();
        TabNameParts tabNameParts = TabNameplateHelper.splitTabName(tabDisplayName, realNameString);

        String originalString = originalName.getString();
        boolean hasStandardNamePlacement = originalString.equals(realNameString) || originalString.startsWith(realNameString + " ");
        Text finalBody = originalName;
        if (hasStandardNamePlacement && tabNameParts.name() != null) {
            Text tail = TabNameplateHelper.splitLeadingNameTail(originalName, realNameString);
            if (tail != null) {
                final Style[] nameStyle = {Style.EMPTY};
                originalName.visit((style, string) -> {
                    if (!string.isEmpty()) {
                        nameStyle[0] = style;
                        return Optional.of(true);
                    }
                    return Optional.empty();
                }, Style.EMPTY);

                finalBody = Text.literal(realNameString).setStyle(nameStyle[0]).append(tail);
            }
        }

        if (tabNameParts.prefix().getString().isBlank()) {
            return finalBody;
        }

        MutableText result = Text.empty()
                .append(TabNameplateHelper.protect(tabNameParts.prefix()));

        String prefixString = tabNameParts.prefix().getString();
        String bodyString = finalBody.getString();
        if (!prefixString.isEmpty() && !bodyString.isEmpty()) {
            char prefixLast = prefixString.charAt(prefixString.length() - 1);
            char bodyFirst = bodyString.charAt(0);
            if (pepeland$needsSeparatorBeforeName(prefixLast, bodyFirst)) {
                result.append(Text.literal(" "));
            }
        }

        return result.append(finalBody);
    }

    @Unique
    private static boolean pepeland$needsSeparatorBeforeName(char prefixLast, char bodyFirst) {
        if (Character.isWhitespace(prefixLast) || Character.isWhitespace(bodyFirst)) {
            return false;
        }
        return pepeland$isRoughlyRegexWordChar(prefixLast) && pepeland$isRoughlyRegexWordChar(bodyFirst);
    }

    @Unique
    private static boolean pepeland$isRoughlyRegexWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
