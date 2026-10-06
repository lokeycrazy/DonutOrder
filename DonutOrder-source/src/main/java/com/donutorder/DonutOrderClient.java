package com.donutorder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;

public class DonutOrderClient implements ClientModInitializer {

    public static final String MOD_ID = "donutorder";

    // ---- Settings (slot numbers are 0-indexed: slot 53 = index 52, slot 54 = index 53) ----
    private static final int DROP_SLOT = 52;   // dropper "drop page" button
    private static final int NEXT_SLOT = 53;   // arrow "next page" button
    private static final long DELAY_MS = 500L; // delay between every input
    // ----------------------------------------------------------------------------------------

    private static KeyMapping toggleKey;
    private static boolean enabled = false;
    private static boolean nextIsDrop = true;
    private static long nextActionAt = 0L;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.donutorder.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_O,
                category));

        // Detect the toggle key while a GUI (the chest) is open.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.beforeKeyPress(screen).register((s, keyEvent) -> {
                    if (toggleKey.matches(keyEvent)) {
                        toggle(client);
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(DonutOrderClient::tick);
    }

    private static void toggle(Minecraft client) {
        enabled = !enabled;
        nextIsDrop = true;
        nextActionAt = System.currentTimeMillis();
        if (client.player != null) {
            // Overlay (action-bar) popup: vanilla fades it out after ~3 seconds, nothing goes to chat.
            client.player.sendOverlayMessage(
                    Component.literal("DonutOrder auto drop: " + (enabled ? "ON" : "OFF")));
        }
    }

    private static void tick(Minecraft client) {
        if (!enabled) return;

        var player = client.player;
        if (player == null || client.gameMode == null) return;

        // Only click while a container (the chest GUI) is open.
        if (player.containerMenu == player.inventoryMenu) return;

        long now = System.currentTimeMillis();
        if (now < nextActionAt) return;

        int slot = nextIsDrop ? DROP_SLOT : NEXT_SLOT;
        client.gameMode.handleContainerInput(
                player.containerMenu.containerId, slot, 0, ContainerInput.PICKUP, player);

        nextIsDrop = !nextIsDrop;
        nextActionAt = now + DELAY_MS;
    }
}
