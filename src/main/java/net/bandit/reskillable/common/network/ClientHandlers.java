package net.bandit.reskillable.common.network;

import net.bandit.reskillable.common.network.payload.*;
import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ClientHandlers {

    public static void handleSyncToClient(SyncToClient msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            SkillModel clientSkillModel = SkillModel.get(player);
            if (clientSkillModel != null) {
                clientSkillModel.readFromNetwork(msg);
            }
        }
    }

    public static void handleNotifyWarning(NotifyWarning msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            Overlay.showWarning(msg.resource(), msg.requirementType());
        }
    }

    public static void handleSyncSkillConfig(SyncSkillConfig msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            net.bandit.reskillable.Configuration.setSkillLocks(msg.skillLocks());
            net.bandit.reskillable.Configuration.setCraftSkillLocks(msg.craftSkillLocks());
            net.bandit.reskillable.Configuration.setAttackSkillLocks(msg.attackSkillLocks());

            if (msg.isFinalChunk()) {
                player.sendSystemMessage(Component.literal("Skill configuration updated!"));
            }
        }
    }
}
