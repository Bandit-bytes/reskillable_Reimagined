package net.bandit.reskillable.client;

import net.bandit.reskillable.client.screen.buttons.KeyBinding;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public final class Keybind {
    private Keybind() {}
    public static void register() {
        KeyBindingHelper.registerKeyBinding(KeyBinding.SKILLS_KEY);
    }
}
