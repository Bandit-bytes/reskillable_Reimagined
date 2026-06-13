package net.bandit.reskillable.client;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientUtils {

    public static SkillModel getClientSkillModel() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            return SkillModel.get(player);
        }
        throw new IllegalStateException("Minecraft client player is null!");
    }
}
