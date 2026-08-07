package net.bandit.reskillable.client;

import net.bandit.reskillable.client.screen.InventoryTabs;
import net.bandit.reskillable.common.network.ClientNetworkInit;
import net.bandit.reskillable.event.ClientEvents;
import net.fabricmc.api.ClientModInitializer;

public class ReskillableClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkInit.register();
        Keybind.register();
        ClientEvents.register();
        OverlayRegistration.register();
        Tooltip.register();
        InventoryTabs.register();
    }
}
