package net.p4pingvin4ik.pepelandbadges.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.p4pingvin4ik.pepelandbadges.util.NickPaintsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {

    @Redirect(
            method = "updateRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderer;getDisplayName(Lnet/minecraft/entity/Entity;)Lnet/minecraft/text/Text;"
            )
    )
    private Text getDisplayNameFromTab(EntityRenderer<T, ?> instance, T entity) {
        if (entity instanceof PlayerEntity player) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry playerListEntry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());

                if (playerListEntry != null && playerListEntry.getDisplayName() != null) {
                    Text tabDisplayName = playerListEntry.getDisplayName();
                    String realNameString = player.getName().getString();

                    if (tabDisplayName.getString().equals(realNameString)) {
                        return tabDisplayName;
                    }

                    List<Text> prefixes = new ArrayList<>();
                    Text nameComponent = null;
                    List<Text> suffixes = new ArrayList<>();
                    boolean nameFound = false;

                    List<Text> allComponents = new ArrayList<>();
                    MutableText head = tabDisplayName.copy();
                    head.getSiblings().clear();
                    allComponents.add(head);
                    allComponents.addAll(tabDisplayName.getSiblings());

                    for (Text component : allComponents) {
                        if (component.getString().isBlank()) {
                            continue;
                        }

                        if (!nameFound && component.getString().trim().equals(realNameString)) {
                            nameComponent = component;
                            nameFound = true;
                        } else if (!nameFound) {
                            prefixes.add(component);
                        } else {
                            suffixes.add(component);
                        }
                    }

                    MutableText reconstructedName = Text.empty();

                    for (Text prefix : prefixes) {
                        reconstructedName.append(protect(prefix));
                    }

                    if (nameComponent != null) {
                        reconstructedName.append(nameComponent);
                    }

                    for (Text suffix : suffixes) {
                        reconstructedName.append(protect(suffix));
                    }

                    return reconstructedName;
                }
            }
        }

        return ((EntityRendererAccessor) instance).invokeGetDisplayName(entity);
    }

    private Text protect(Text component) {
        Style protectedStyle = component.getStyle().withInsertion(NickPaintsCompat.PROTECTED_TAG_INSERTION_KEY);
        return component.copy().setStyle(protectedStyle);
    }
}