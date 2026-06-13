package net.bandit.reskillable.common.network;

import net.bandit.reskillable.client.Overlay;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.NotifyWarning;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

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
            net.bandit.reskillable.Configuration.applySyncedClientConfig(
                    msg.skillLocks(),
                    msg.craftSkillLocks(),
                    msg.attackSkillLocks(),
                    msg.skillLevelingEnabled(),
                    msg.maximumLevel(),
                    msg.maxTotalSpentLevels(),
                    msg.customSkills()
            );
        }
    }
}
