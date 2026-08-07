package net.bandit.reskillable.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public final class OverlayRegistration {
    private OverlayRegistration() {}
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> Overlay.INSTANCE.tick());
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> Overlay.INSTANCE.render(graphics, deltaTracker));
    }
}
