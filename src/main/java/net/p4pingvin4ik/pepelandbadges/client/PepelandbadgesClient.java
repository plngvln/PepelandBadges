package net.p4pingvin4ik.pepelandbadges.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PepelandbadgesClient implements ClientModInitializer {
    public static boolean MOD_ENABLED = true;

    private static KeyBinding toggleKeyBinding;

    @Override
    public void onInitializeClient() {
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pepelandbadges.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.pepelandbadges"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeyBinding.wasPressed()) {
                MOD_ENABLED = !MOD_ENABLED;

                Text message = Text.translatable(MOD_ENABLED ? "chat.pepelandbadges.enabled" : "chat.pepelandbadges.disabled");

                if (client.player != null) {
                    client.player.sendMessage(message, true);
                }
            }
        });
    }
}