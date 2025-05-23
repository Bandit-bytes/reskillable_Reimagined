package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.network.SyncToClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.EnumArgument;

@Mod.EventBusSubscriber
public class SetCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("skills")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(SetCommand::executeAdd)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, Configuration.getMaxLevel()))
                                                .executes(SetCommand::executeSet)))));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Skill skill = context.getArgument("skill", Skill.class);
        int amount = IntegerArgumentType.getInteger(context, "amount");

        SkillModel model = SkillModel.get(player);
        int current = model.getSkillLevel(skill);
        int newLevel = Math.min(current + amount, Configuration.getMaxLevel());
        model.setSkillLevel(skill, newLevel);
        SyncToClient.send(player);

        context.getSource().sendSuccess(() -> Component.literal("Increased ")
                .append(player.getName())
                .append("'s ")
                .append(Component.literal(skill.name()))
                .append(" by " + amount + " to " + newLevel), true);

        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Skill skill = context.getArgument("skill", Skill.class);
        int level = IntegerArgumentType.getInteger(context, "level");

        SkillModel model = SkillModel.get(player);
        model.setSkillLevel(skill, level);
        SyncToClient.send(player);

        context.getSource().sendSuccess(() -> Component.literal("Set ")
                .append(player.getName())
                .append("'s ")
                .append(skill.displayName)
                .append(" to " + level), true);

        return 1;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}
