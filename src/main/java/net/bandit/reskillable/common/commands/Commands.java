package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.bandit.reskillable.Configuration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Commands {

    /**
     * Event handler for registering commands.
     *
     * @param event The event that triggers the command registration.
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("skills")
                        .requires(source -> source.hasPermission(2))
                        .then(SetCommand.register())
                        .then(GetCommand.register())
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                                .executes(context -> {
                                    Configuration.load();
                                    context.getSource().sendSuccess(() -> Component.literal("Skill configuration reloaded"), true);
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("scanmod")
                                .then(net.minecraft.commands.Commands.argument("mod", StringArgumentType.string())
                                        .executes(context -> {
                                            String modId = StringArgumentType.getString(context, "mod");
                                            return scanModCommand(context.getSource(), modId);
                                        })
                                )
                        )
        );
    }

    /**
     * Logic for the /skills scanmod <mod> command.
     *
     * @param source The command source (e.g., the player or server).
     * @param modId  The mod ID to scan.
     * @return The result code (1 for success, 0 for failure).
     */
    private int scanModCommand(CommandSourceStack source, String modId) {
        try {
            // Scan items for the given mod ID
            int itemCount = Configuration.scanModItems(modId);
            if (itemCount > 0) {
                source.sendSuccess(() -> Component.literal("Added " + itemCount + " items from mod '" + modId + "' to skill_locks.json."), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("No items found for mod ID: " + modId));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred while scanning items: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
