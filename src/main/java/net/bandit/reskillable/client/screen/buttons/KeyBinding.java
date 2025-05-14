package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class KeyBinding {
    public static final String KEY_CATEGORY = "key.category.reskillable";
    public static final String OPEN_SKILLS_KEY = "key.reskillable.open_skills";

    public static final KeyMapping SKILLS_KEY = new KeyMapping
            (OPEN_SKILLS_KEY, KeyConflictContext.IN_GAME,
                    InputConstants.getKey(InputConstants.KEY_G, -1),
                    KEY_CATEGORY
            );
}

