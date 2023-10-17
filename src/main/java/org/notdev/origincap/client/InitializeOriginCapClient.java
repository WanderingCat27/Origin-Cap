package org.notdev.origincap.client;

import org.notdev.origincap.global.CapHandler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class InitializeOriginCapClient implements ClientModInitializer  {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
            CapHandler.registerClientPackets();
        }));

        }
}
