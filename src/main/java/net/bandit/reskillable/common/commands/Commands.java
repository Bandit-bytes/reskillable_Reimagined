package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.bandit.reskillable.Configuration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.chat.Component;

/**
 * Class responsible for registering commands for the mod.
 */
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
        );
    }
}