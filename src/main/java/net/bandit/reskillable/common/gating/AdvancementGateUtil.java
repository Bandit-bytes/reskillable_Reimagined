package net.bandit.reskillable.common.gating;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementGateUtil {
    private AdvancementGateUtil() {}

    public static boolean has(ServerPlayer player, Identifier id) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        AdvancementHolder holder = server.getAdvancements().get(id);
        if (holder == null) return false;

        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
