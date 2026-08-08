package net.p4pingvin4ik.pepelandbadges.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
            method = "extractRenderState",
            at = @At("RETURN")
    )
    private void modifyDisplayNameAfterUpdate(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (state.nameTag == null || !PepelandbadgesClient.MOD_ENABLED || !(entity instanceof Player)) {
            return;
        }

        Component modifiedText = getBadgesTextForPlayer((Player) entity, state.nameTag);

        state.nameTag = modifiedText;
    }

    @Unique
    private Component getBadgesTextForPlayer(Player player, Component originalName) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return originalName;
        }

        PlayerInfo playerListEntry = client.getConnection().getPlayerInfo(player.getUUID());
        if (playerListEntry == null) {
            return originalName;
        }

        Component tabDisplayName = PlayerListEntryTabText.getEffectiveDisplayName(playerListEntry);
        String realNameString = player.getName().getString();
        TabNameParts tabNameParts = TabNameplateHelper.splitTabName(tabDisplayName, realNameString);

        String originalString = originalName.getString();
        boolean hasStandardNamePlacement = originalString.equals(realNameString) || originalString.startsWith(realNameString + " ");
        Component finalBody = originalName;
        if (hasStandardNamePlacement && tabNameParts.name() != null) {
            Component tail = TabNameplateHelper.splitLeadingNameTail(originalName, realNameString);
            if (tail != null) {
                final Style[] nameStyle = {Style.EMPTY};
                originalName.visit((style, string) -> {
                    if (!string.isEmpty()) {
                        nameStyle[0] = style;
                        return Optional.of(true);
                    }
                    return Optional.empty();
                }, Style.EMPTY);

                finalBody = Component.literal(realNameString).setStyle(nameStyle[0]).append(tail);
            }
        }

        if (tabNameParts.prefix().getString().isBlank()) {
            return finalBody;
        }

        MutableComponent result = Component.empty()
                .append(TabNameplateHelper.protect(tabNameParts.prefix()));

        String prefixString = tabNameParts.prefix().getString();
        String bodyString = finalBody.getString();
        if (!prefixString.isEmpty() && !bodyString.isEmpty()) {
            char prefixLast = prefixString.charAt(prefixString.length() - 1);
            char bodyFirst = bodyString.charAt(0);
            if (pepeland$needsSeparatorBeforeName(prefixLast, bodyFirst)) {
                result.append(Component.literal(" "));
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
