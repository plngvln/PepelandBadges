package net.p4pingvin4ik.pepelandbadges.util;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Matches how the tab list chooses display text when {@link PlayerInfo#getTabListDisplayName()} is null
 * (vanilla uses scoreboard team decoration for the profile name).
 */
public final class PlayerListEntryTabText {

    private PlayerListEntryTabText() {
    }

    public static Component getEffectiveDisplayName(PlayerInfo entry) {
        Component custom = entry.getTabListDisplayName();
        if (custom != null) {
            return custom;
        }
        Component name = Component.literal(entry.getProfile().name());
        PlayerTeam team = entry.getTeam();
        if (team == null) {
            return name;
        }
        return team.getFormattedName(name);
    }
}
