package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Skill;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.command.EnumArgument;

@EventBusSubscriber
public class SetCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("skills")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(SetCommand::executeAdd)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", EnumArgument.enumArgument(Skill.class))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, Configuration.getMaxLevel()))
                                                .executes(SetCommand::executeSet)))));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(context, "targets");
        Skill skill = context.getArgument("skill", Skill.class);
        int amount = IntegerArgumentType.getInteger(context, "amount");

        int changed = 0;

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);
            if (model == null) continue;

            int currentLevel = model.getSkillLevel(skill);
            int newLevel = Math.min(currentLevel + amount, Configuration.getMaxLevel());

            model.setSkillLevel(skill, newLevel);

            SyncToClient.send(target);
            model.updateSkillAttributeBonuses(target);

            changed++;
        }

        int finalChanged = changed;
        source.sendSuccess(() ->
                        Component.literal("Added " + amount + " to " + skill.displayName + " for " + finalChanged + " player(s)."),
                true
        );

        return changed;
    }


    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(context, "targets");
        Skill skill = context.getArgument("skill", Skill.class);
        int level = IntegerArgumentType.getInteger(context, "level");

        int changed = 0;

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);
            if (model == null) continue;

            model.setSkillLevel(skill, level);

            SyncToClient.send(target);
            model.updateSkillAttributeBonuses(target);

            changed++;
        }

        int finalChanged = changed;
        source.sendSuccess(() ->
                        Component.literal("Set " + skill.displayName + " to " + level + " for " + finalChanged + " player(s)."),
                true
        );

        return changed;
    }


    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}