package net.p4pingvin4ik.pepelandbadges.util;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

/**
 * Matches how the tab list chooses display text when {@link PlayerListEntry#getDisplayName()} is null
 * (vanilla uses scoreboard team decoration for the profile name).
 */
public final class PlayerListEntryTabText {

    private PlayerListEntryTabText() {
    }

    public static Text getEffectiveDisplayName(PlayerListEntry entry) {
        Text custom = entry.getDisplayName();
        if (custom != null) {
            return custom;
        }
        Text name = Text.literal(entry.getProfile().getName());
        Team team = entry.getScoreboardTeam();
        if (team == null) {
            return name;
        }
        return team.decorateName(name);
    }
}
