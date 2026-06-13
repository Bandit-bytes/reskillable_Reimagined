package net.bandit.reskillable.client.screen.buttons;

import com.mojang.blaze3d.platform.InputConstants;
import net.bandit.reskillable.Reskillable;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class KeyBinding {
    public static final String OPEN_SKILLS_KEY = "key.reskillable.open_skills";

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Reskillable.MOD_ID, "controls")
    );

    public static final KeyMapping SKILLS_KEY = new KeyMapping(
            OPEN_SKILLS_KEY,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_G),
            CATEGORY
    );
}
