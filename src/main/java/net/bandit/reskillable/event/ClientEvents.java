package net.bandit.reskillable.event;

import net.bandit.reskillable.client.screen.SkillScreen;
import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class ClientEvents {
    private ClientEvents() {}
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBinding.SKILLS_KEY.consumeClick()) {
                client.setScreen(new SkillScreen());
            }
        });
    }
}
