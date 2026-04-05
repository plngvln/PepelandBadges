package net.p4pingvin4ik.pepelandbadges.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Optional;

/**
 * Replaces a UTF-16 code unit range in a {@link Text} tree with another {@link Text},
 * preserving styles outside the range.
 */
public final class NameplateTextUtil {

    private NameplateTextUtil() {
    }

    public static Text replaceUtf16Range(Text root, int startInclusive, int endExclusive, Text insertion) {
        if (startInclusive < 0 || endExclusive < startInclusive) {
            return root;
        }

        MutableText out = Text.empty();
        int[] index = {0};
        boolean[] inserted = {false};
        StringBuilder buf = new StringBuilder();
        Style[] bufStyle = {Style.EMPTY};

        Runnable flush = () -> {
            if (buf.length() > 0) {
                out.append(Text.literal(buf.toString()).setStyle(bufStyle[0]));
                buf.setLength(0);
            }
        };

        root.visit((style, str) -> {
            for (int i = 0; i < str.length(); i++) {
                int pos = index[0];
                index[0]++;
                boolean inRange = pos >= startInclusive && pos < endExclusive;
                if (inRange) {
                    flush.run();
                    if (!inserted[0]) {
                        out.append(insertion.copy());
                        inserted[0] = true;
                    }
                } else {
                    if (buf.length() == 0) {
                        bufStyle[0] = style;
                    } else if (!bufStyle[0].equals(style)) {
                        flush.run();
                        bufStyle[0] = style;
                    }
                    buf.append(str.charAt(i));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);

        flush.run();
        return out;
    }
}
