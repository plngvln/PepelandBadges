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

                    MutableText reconstructedName = Text.empty();

                    List<Text> components = new ArrayList<>();

                    MutableText headComponent = tabDisplayName.copy();
                    headComponent.getSiblings().clear();
                    components.add(headComponent);

                    components.addAll(tabDisplayName.getSiblings());

                    for (Text component : components) {

                        if (component.getString().isEmpty()) continue;

                        if (component.getString().equals(realNameString)) {
                            reconstructedName.append(component);
                        } else {
                            Style originalStyle = component.getStyle();
                            Style protectedStyle = originalStyle.withInsertion(NickPaintsCompat.PROTECTED_TAG_INSERTION_KEY);

                            reconstructedName.append(component.copy().setStyle(protectedStyle));
                        }
                    }
                    return reconstructedName;
                }
            }
        }

        return entity.getDisplayName();
    }
}