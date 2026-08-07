package net.bandit.reskillable.common.gating;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementGateUtil {
    private AdvancementGateUtil() {}

    public static boolean has(ServerPlayer player, ResourceLocation id) {
        MinecraftServer server = player.server;
        if (server == null) return false;

        AdvancementHolder holder = server.getAdvancements().get(id);
        if (holder == null) return false;

        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
