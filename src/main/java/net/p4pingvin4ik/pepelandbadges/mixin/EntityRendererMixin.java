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

        if (tabDisplayName.getString().equals(realNameString)) {
            return originalName;
        }

        List<Text> prefixes = new ArrayList<>();
        boolean nameFound = false;

        List<Text> allComponents = new ArrayList<>();
        MutableText head = tabDisplayName.copy();
        head.getSiblings().clear();
        allComponents.add(head);
        allComponents.addAll(tabDisplayName.getSiblings());

        for (Text component : allComponents) {
            if (component.getString().isBlank()) continue;

            if (!nameFound && component.getString().trim().equals(realNameString)) {
                nameFound = true;
            } else if (!nameFound) {
                prefixes.add(component);
            }
        }

        if (prefixes.isEmpty()) {
            return originalName;
        }

        MutableText finalName = Text.empty();
        for (Text prefix : prefixes) {
            finalName.append(protect(prefix));
        }
        finalName.append(originalName);

        return finalName;
    }

    @Unique
    private Text protect(Text component) {
        Style protectedStyle = component.getStyle().withInsertion(NickPaintsCompat.PROTECTED_TAG_INSERTION_KEY);
        return component.copy().setStyle(protectedStyle);
    }
}