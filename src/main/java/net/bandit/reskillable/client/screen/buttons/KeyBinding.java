package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class KeyBinding {
    private KeyBinding() {}
    public static final String KEY_CATEGORY = "key.category.reskillable";
    public static final String OPEN_SKILLS_KEY = "key.reskillable.open_skills";
    public static final KeyMapping SKILLS_KEY = new KeyMapping(
            OPEN_SKILLS_KEY,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            KEY_CATEGORY
    );
}
