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
import net.p4pingvin4ik.pepelandbadges.util.NickPaintsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
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
        if (playerListEntry == null || playerListEntry.getDisplayName() == null) {
            return originalName;
        }

        Text tabDisplayName = playerListEntry.getDisplayName();
        String realNameString = player.getName().getString();
        TabNameParts tabNameParts = splitTabName(tabDisplayName, realNameString);

        String originalString = originalName.getString();
        boolean hasStandardNamePlacement = originalString.equals(realNameString) || originalString.startsWith(realNameString + " ");
        Text finalBody = originalName;
        if (hasStandardNamePlacement && tabNameParts.name() != null) {
            Style tabNameStyle = getPrimaryStyle(tabNameParts.name());
            Text tail = splitLeadingNameTail(originalName, realNameString);
            if (tail != null) {
                Text recolored = Text.literal(realNameString).setStyle(tabNameStyle).copy().append(tail);
                finalBody = recolored;
            }
        }

        if (tabNameParts.prefix().getString().isBlank()) {
            return finalBody;
        }

        MutableText result = Text.empty()
                .append(protect(tabNameParts.prefix()));

        String prefixString = tabNameParts.prefix().getString();
        String bodyString = finalBody.getString();
        if (!prefixString.isEmpty() && !bodyString.isEmpty()) {
            char prefixLast = prefixString.charAt(prefixString.length() - 1);
            char bodyFirst = bodyString.charAt(0);
            if (!Character.isWhitespace(prefixLast) && !Character.isWhitespace(bodyFirst)) {
                result.append(Text.literal(" "));
            }
        }

        return result.append(finalBody);
    }

    @Unique
    private Text protect(Text component) {
        MutableText protectedText = Text.empty();
        component.visit((style, string) -> {
            if (!string.isEmpty()) {
                Style protectedStyle = style.withInsertion(NickPaintsCompat.PROTECTED_TAG_INSERTION_KEY);
                protectedText.append(Text.literal(string).setStyle(protectedStyle));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return protectedText;
    }

    @Unique
    private TabNameParts splitTabName(Text tabDisplayName, String realNameString) {
        List<StyledChunk> chunks = new ArrayList<>();
        tabDisplayName.visit((style, string) -> {
            if (!string.isEmpty()) {
                chunks.add(new StyledChunk(style, string));
            }
            return Optional.empty();
        }, Style.EMPTY);

        for (int start = 0; start < chunks.size(); start++) {
            StringBuilder current = new StringBuilder();
            MutableText candidateName = Text.empty();

            for (int end = start; end < chunks.size(); end++) {
                StyledChunk chunk = chunks.get(end);
                current.append(chunk.value());
                candidateName.append(Text.literal(chunk.value()).setStyle(chunk.style()));

                String currentString = current.toString();
                if (realNameString.equals(currentString)) {
                    MutableText prefix = Text.empty();
                    for (int i = 0; i < start; i++) {
                        StyledChunk prefixChunk = chunks.get(i);
                        prefix.append(Text.literal(prefixChunk.value()).setStyle(prefixChunk.style()));
                    }
                    return new TabNameParts(prefix, candidateName);
                }

                if (!realNameString.startsWith(currentString)) {
                    break;
                }
            }
        }

        return new TabNameParts(Text.empty(), null);
    }

    @Unique
    private Style getPrimaryStyle(Text text) {
        final Style[] styleRef = {Style.EMPTY};
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                styleRef[0] = style;
                return Optional.of(Boolean.TRUE);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return styleRef[0];
    }

    @Unique
    private Text splitLeadingNameTail(Text source, String realNameString) {
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

        MutableText rebuiltTail = Text.empty();
        if (chunkIndex < chunks.size()) {
            StyledChunk partial = chunks.get(chunkIndex);
            String tail = partial.value().substring(consumedInChunk);
            if (!tail.isEmpty()) {
                rebuiltTail.append(Text.literal(tail).setStyle(partial.style()));
            }
            chunkIndex++;
        }

        for (int i = chunkIndex; i < chunks.size(); i++) {
            StyledChunk chunk = chunks.get(i);
            rebuiltTail.append(Text.literal(chunk.value()).setStyle(chunk.style()));
        }

        return rebuiltTail;
    }

    @Unique
    private record StyledChunk(Style style, String value) {
    }

    @Unique
    private record TabNameParts(Text prefix, Text name) {
    }
}