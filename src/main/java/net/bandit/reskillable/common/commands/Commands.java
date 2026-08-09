package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.SyncSkillConfig;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "reskillable")
public class Commands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                net.minecraft.commands.Commands.literal("skills");

        root.then(GetCommand.register());

        SetCommand.register(root);

        root.then(
                net.minecraft.commands.Commands.literal("reload")
                        .requires(net.minecraft.commands.Commands.hasPermission(
                                net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
                        ))
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

                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Skill configuration reloaded."
                                    ),
                                    true
                            );

                            return 1;
                        })
        );

        root.then(
                net.minecraft.commands.Commands.literal("scanmod")
                        .requires(net.minecraft.commands.Commands.hasPermission(
                                net.minecraft.commands.Commands.LEVEL_GAMEMASTERS
                        ))
                        .then(
                                net.minecraft.commands.Commands.argument(
                                                "mod",
                                                StringArgumentType.string()
                                        )
                                        .executes(context -> {
                                            String modId =
                                                    StringArgumentType.getString(
                                                            context,
                                                            "mod"
                                                    );

                                            return scanModCommand(
                                                    context.getSource(),
                                                    modId
                                            );
                                        })
                        )
        );

        event.getDispatcher().register(root);
    }

    private static int scanModCommand(
            CommandSourceStack source,
            String modId
    ) {
        try {
            int itemCount = Configuration.scanModItems(modId);

            if (itemCount > 0) {
                source.sendSuccess(
                        () -> Component.literal(
                                "Added "
                                        + itemCount
                                        + " items from mod '"
                                        + modId
                                        + "' to skill_locks.json."
                        ),
                        true
                );

                return 1;
            }

            source.sendFailure(
                    Component.literal(
                            "No items found for mod ID: " + modId
                    )
            );

            return 0;
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "An error occurred while scanning items: "
                                    + exception.getMessage()
                    )
            );

            exception.printStackTrace();
            return 0;
        }
    }
}