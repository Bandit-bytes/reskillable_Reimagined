package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.SyncToClient;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.EnumArgument;

@Mod.EventBusSubscriber
public class SetCommand {

    /**
     * Registers the skill commands and returns the LiteralArgumentBuilder.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("skills")
                .then(Commands.literal("add")
                        .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(SetCommand::executeAdd))))
                .then(Commands.literal("set")
                        .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Configuration.getMaxLevel()))
                                        .executes(SetCommand::executeSet))));
    }

    /**
     * Executes the add command.
     *
     * @param context The command context.
     * @return A success code.
     * @throws CommandSyntaxException If there is an error in the command syntax.
     */
    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        Skill skill = context.getArgument("skill", Skill.class);
        int amount = IntegerArgumentType.getInteger(context, "amount");

        SkillModel skillModel = SkillModel.get(player);
        int currentLevel = skillModel.getSkillLevel(skill);
        int newLevel = Math.min(currentLevel + amount, Configuration.getMaxLevel());

        skillModel.setSkillLevel(skill, newLevel);
        SyncToClient.send(player);

        source.sendSuccess(() -> Component.translatable(skill.displayName)
                .append(" increased by " + amount + " to " + newLevel), true);

        return 1;
    }

    /**
     * Executes the set command.
     *
     * @param context The command context.
     * @return A success code.
     * @throws CommandSyntaxException If there is an error in the command syntax.
     */
    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        Skill skill = context.getArgument("skill", Skill.class);
        int level = IntegerArgumentType.getInteger(context, "level");

        SkillModel skillModel = SkillModel.get(player);
        skillModel.setSkillLevel(skill, level);
        SyncToClient.send(player);

        source.sendSuccess(() -> Component.translatable(skill.displayName)
                .append(" set to " + level), true);

        return 1;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}