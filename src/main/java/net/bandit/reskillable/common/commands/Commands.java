package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class Commands {
    private Commands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(SetCommand.register());
        dispatcher.register(net.minecraft.commands.Commands.literal("skills")
                .then(GetCommand.register())
                .then(net.minecraft.commands.Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            Configuration.load();
                            for (var player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                SkillModel model = SkillModel.get(player);
                                if (model != null) {
                                    model.updateSkillAttributeBonuses(player);
                                    SyncToClient.send(player);
                                }
                                SyncSkillConfig.send(player);
                            }
                            context.getSource().sendSuccess(() -> Component.literal("Skill configuration reloaded"), true);
                            return 1;
                        }))
                .then(net.minecraft.commands.Commands.literal("scanmod")
                        .requires(source -> source.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("mod", StringArgumentType.string())
                                .executes(context -> scanMod(context.getSource(), StringArgumentType.getString(context, "mod"))))));
    }

    private static int scanMod(CommandSourceStack source, String modId) {
        try {
            int itemCount = Configuration.scanModItems(modId);
            if (itemCount > 0) {
                source.sendSuccess(() -> Component.literal("Added " + itemCount + " items from mod '" + modId + "' to skill_locks.json."), true);
                return 1;
            }
            source.sendFailure(Component.literal("No items found for mod ID: " + modId));
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while scanning items: " + e.getMessage()));
        }
        return 0;
    }
}
