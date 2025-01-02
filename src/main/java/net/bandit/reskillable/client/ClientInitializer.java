package net.bandit.reskillable.client;

import net.bandit.reskillable.event.ClientEvents;
import net.minecraftforge.common.MinecraftForge;

public class ClientInitializer {
    public static void registerClientEvents() {
        MinecraftForge.EVENT_BUS.register(new Tooltip());
        MinecraftForge.EVENT_BUS.register(new Overlay());
        MinecraftForge.EVENT_BUS.register(new Keybind());
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }
}
