package net.bandit.reskillable.client;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.Minecraft;

public final class ClientUtils {
    private ClientUtils() {}

    public static SkillModel getSkillModel() {
        if (Minecraft.getInstance().player == null) return null;
        return SkillModel.get(Minecraft.getInstance().player);
    }
}
