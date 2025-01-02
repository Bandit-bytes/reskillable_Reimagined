package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.network.SyncSkillConfigPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.EnumArgument;

import java.util.logging.Logger;

@Mod.EventBusSubscriber
public class GetCommand {
    private static final Logger LOGGER = Logger.getLogger(GetCommand.class.getName());

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                .executes(GetCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Skill skill = context.getArgument("skill", Skill.class);
        int level = SkillModel.get(player).getSkillLevel(skill);

        context.getSource().sendSuccess(() -> Component.translatable(skill.displayName).append(" " + level), true);

        return level;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("skills")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                                .executes(GetCommand::execute))))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    Configuration.load();
                                    context.getSource().sendSuccess(() -> Component.literal("Skill configuration reloaded"), true);
                                    SyncSkillConfigPacket.sendToAllClients();
                                    LOGGER.info("Executed /skills reload command and sent SyncSkillConfigPacket to clients.");
                                    return 1;
                                }))
        );
    }
}