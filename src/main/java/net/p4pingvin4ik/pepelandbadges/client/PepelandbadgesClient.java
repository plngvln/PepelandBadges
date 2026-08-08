package net.p4pingvin4ik.pepelandbadges.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PepelandbadgesClient implements ClientModInitializer {
    public static boolean MOD_ENABLED = true;

    private static KeyMapping toggleKeyBinding;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("pepelandbadges", "pepelandbadges")
        );

        toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pepelandbadges.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeyBinding.consumeClick()) {
                MOD_ENABLED = !MOD_ENABLED;

                Component message = Component.translatable(MOD_ENABLED ? "chat.pepelandbadges.enabled" : "chat.pepelandbadges.disabled");

                if (client.player != null) {
                    client.player.sendOverlayMessage(message);
                }
            }
        });
    }
}
