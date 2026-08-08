package net.p4pingvin4ik.pepelandbadges.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Parses tab list display names into prefix + player name, and splits world name into name + tail.
 */
public final class TabNameplateHelper {

    private TabNameplateHelper() {
    }

    public static Component protect(Component component) {
        MutableComponent protectedText = Component.empty();
        component.visit((style, string) -> {
            if (!string.isEmpty()) {
                Style protectedStyle = style.withInsertion(NickPaintsCompat.PROTECTED_TAG_INSERTION_KEY);
                protectedText.append(Component.literal(string).setStyle(protectedStyle));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return protectedText;
    }

    public static TabNameParts splitTabName(Component tabDisplayName, String realNameString) {
        List<StyledChunk> chunks = new ArrayList<>();
        tabDisplayName.visit((style, string) -> {
            if (!string.isEmpty()) {
                chunks.add(new StyledChunk(style, string));
            }
            return Optional.empty();
        }, Style.EMPTY);

        for (int start = 0; start < chunks.size(); start++) {
            StringBuilder current = new StringBuilder();
            MutableComponent candidateName = Component.empty();

            for (int end = start; end < chunks.size(); end++) {
                StyledChunk chunk = chunks.get(end);
                current.append(chunk.value());
                candidateName.append(Component.literal(chunk.value()).setStyle(chunk.style()));

                String currentString = current.toString();
                if (realNameString.equals(currentString)) {
                    MutableComponent prefix = Component.empty();
                    for (int i = 0; i < start; i++) {
                        StyledChunk prefixChunk = chunks.get(i);
                        prefix.append(Component.literal(prefixChunk.value()).setStyle(prefixChunk.style()));
                    }
                    return new TabNameParts(stripLeadingAsciiWhitespace(prefix), candidateName);
                }

                if (!realNameString.startsWith(currentString)) {
                    break;
                }
            }
        }

        return new TabNameParts(Component.empty(), null);
    }

    /**
     * Removes leading {@code ' '} from tab prefix (servers often send a stray space before badges).
     */
    public static Component stripLeadingAsciiWhitespace(Component source) {
        if (source == null) {
            return Component.empty();
        }
        if (source.getString().isEmpty()) {
            return source;
        }
        MutableComponent result = Component.empty();
        boolean[] stillLeading = {true};
        source.visit((style, string) -> {
            if (string.isEmpty()) {
                return Optional.empty();
            }
            int i = 0;
            if (stillLeading[0]) {
                while (i < string.length() && string.charAt(i) == ' ') {
                    i++;
                }
                stillLeading[0] = false;
            }
            if (i < string.length()) {
                result.append(Component.literal(string.substring(i)).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static Component splitLeadingNameTail(Component source, String realNameString) {
        List<StyledChunk> chunks = new ArrayList<>();
        source.visit((style, string) -> {
            if (!string.isEmpty()) {
                chunks.add(new StyledChunk(style, string));
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (chunks.isEmpty()) {
            return null;
        }

        String remaining = realNameString;
        int chunkIndex = 0;
        int consumedInChunk = 0;

        while (!remaining.isEmpty() && chunkIndex < chunks.size()) {
            String value = chunks.get(chunkIndex).value();
            if (value.isEmpty()) {
                chunkIndex++;
                continue;
            }

            if (remaining.startsWith(value)) {
                remaining = remaining.substring(value.length());
                chunkIndex++;
                consumedInChunk = 0;
                continue;
            }

            if (value.startsWith(remaining)) {
                consumedInChunk = remaining.length();
                remaining = "";
                break;
            }

            return null;
        }

        if (!remaining.isEmpty()) {
            return null;
        }

        MutableComponent rebuiltTail = Component.empty();
        if (chunkIndex < chunks.size()) {
            StyledChunk partial = chunks.get(chunkIndex);
            String tail = partial.value().substring(consumedInChunk);
            if (!tail.isEmpty()) {
                rebuiltTail.append(Component.literal(tail).setStyle(partial.style()));
            }
            chunkIndex++;
        }

        for (int i = chunkIndex; i < chunks.size(); i++) {
            StyledChunk chunk = chunks.get(i);
            rebuiltTail.append(Component.literal(chunk.value()).setStyle(chunk.style()));
        }

        return rebuiltTail;
    }

    public record StyledChunk(Style style, String value) {
    }

    public record TabNameParts(Component prefix, Component name) {
    }
}
