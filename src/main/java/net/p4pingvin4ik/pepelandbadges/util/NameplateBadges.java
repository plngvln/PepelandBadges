package net.p4pingvin4ik.pepelandbadges.util;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.p4pingvin4ik.pepelandbadges.util.TabNameplateHelper.TabNameParts;

/**
 * Builds nameplate text from the tab list entry of a player.
 */
public final class NameplateBadges {

    /**
     * Tolerance for matching a render state back to a player when the state carries no entity id,
     * which covers the interpolation gap between the extracted position and the current tick.
     */
    private static final double MATCH_DISTANCE_SQR = 16.0;

    private NameplateBadges() {
    }

    public static Component applyBadges(Player player, Component originalName) {
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
            if (needsSeparatorBeforeName(prefixLast, bodyFirst)) {
                result.append(Component.literal(" "));
            }
        }

        return result.append(finalBody);
    }

    public static Component applyTabGradient(Player player, Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
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

    /**
     * Finds the player a render state belongs to. Culling mods hand over bare states without an
     * entity id, so those are matched by name and position instead.
     */
    public static Player resolvePlayer(EntityRenderState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        if (state instanceof AvatarRenderState avatarState) {
            for (Player player : client.level.players()) {
                if (player.getId() == avatarState.id) {
                    return player;
                }
            }
            return null;
        }

        if (state.nameTag == null) {
            return null;
        }

        String nameTag = state.nameTag.getString();
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Player player : client.level.players()) {
            if (!nameTag.contains(player.getName().getString())) {
                continue;
            }
            double distance = player.distanceToSqr(state.x, state.y, state.z);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }

        return closestDistance <= MATCH_DISTANCE_SQR ? closest : null;
    }

    private static boolean needsSeparatorBeforeName(char prefixLast, char bodyFirst) {
        if (Character.isWhitespace(prefixLast) || Character.isWhitespace(bodyFirst)) {
            return false;
        }
        return isRoughlyRegexWordChar(prefixLast) && isRoughlyRegexWordChar(bodyFirst);
    }

    private static boolean isRoughlyRegexWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
