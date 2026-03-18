package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Configuration.CustomSkillSlot;
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

import java.util.Locale;

@Mod.EventBusSubscriber
public class SetCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("skills")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(SetCommand::executeAdd)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, Configuration.getMaxLevel()))
                                                .executes(SetCommand::executeSet)))))
                .then(Commands.literal("respec")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(SetCommand::executeRespec)));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String skillName = StringArgumentType.getString(context, "skill").trim().toLowerCase(Locale.ROOT);
        int amount = IntegerArgumentType.getInteger(context, "amount");

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            context.getSource().sendFailure(Component.literal("Could not access the player's skill data."));
            return 0;
        }

        if (skillName.equals("all")) {
            int changed = 0;

            for (Skill skill : Skill.values()) {
                int current = model.getSkillLevel(skill);
                int newLevel = Math.min(current + amount, Configuration.getMaxLevel());
                if (newLevel != current) {
                    model.setSkillLevel(skill, newLevel);
                    changed++;
                }
            }

            for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
                if (slot != null && slot.isEnabled()) {
                    int current = model.getCustomSkillLevel(slot.getId());
                    int newLevel = Math.min(current + amount, Configuration.getMaxLevel());
                    if (newLevel != current) {
                        model.setCustomSkillLevel(slot.getId(), newLevel);
                        changed++;
                    }
                }
            }

            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            int finalChanged = changed;
            context.getSource().sendSuccess(() -> Component.literal("Increased all skills for ")
                    .append(player.getName())
                    .append(" by " + amount + " (" + finalChanged + " skills affected)"), true);

            return 1;
        }

        Skill builtInSkill = Skill.fromString(skillName);
        if (builtInSkill != null) {
            int current = model.getSkillLevel(builtInSkill);
            int newLevel = Math.min(current + amount, Configuration.getMaxLevel());
            model.setSkillLevel(builtInSkill, newLevel);
            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            context.getSource().sendSuccess(() -> Component.literal("Increased ")
                    .append(player.getName())
                    .append("'s ")
                    .append(Component.literal(builtInSkill.getSerializedName()))
                    .append(" by " + amount + " to " + newLevel), true);

            return 1;
        }

        CustomSkillSlot customSkill = Configuration.findCustomSkillById(skillName);
        if (customSkill != null) {
            int current = model.getCustomSkillLevel(customSkill.getId());
            int newLevel = Math.min(current + amount, Configuration.getMaxLevel());
            model.setCustomSkillLevel(customSkill.getId(), newLevel);
            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            context.getSource().sendSuccess(() -> Component.literal("Increased ")
                    .append(player.getName())
                    .append("'s ")
                    .append(Component.literal(customSkill.getDisplayName()))
                    .append(" by " + amount + " to " + newLevel), true);

            return 1;
        }

        context.getSource().sendFailure(Component.literal("Unknown skill: " + skillName));
        return 0;
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String skillName = StringArgumentType.getString(context, "skill").trim().toLowerCase(Locale.ROOT);
        int level = IntegerArgumentType.getInteger(context, "level");

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            context.getSource().sendFailure(Component.literal("Could not access the player's skill data."));
            return 0;
        }

        if (skillName.equals("all")) {
            int changed = 0;

            for (Skill skill : Skill.values()) {
                if (model.getSkillLevel(skill) != level) {
                    model.setSkillLevel(skill, level);
                    changed++;
                }
            }

            for (CustomSkillSlot slot : Configuration.getCustomSkills()) {
                if (slot != null && slot.isEnabled()) {
                    if (model.getCustomSkillLevel(slot.getId()) != level) {
                        model.setCustomSkillLevel(slot.getId(), level);
                        changed++;
                    }
                }
            }

            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            int finalChanged = changed;
            context.getSource().sendSuccess(() -> Component.literal("Set all skills for ")
                    .append(player.getName())
                    .append(" to " + level + " (" + finalChanged + " skills affected)"), true);

            return 1;
        }

        Skill builtInSkill = Skill.fromString(skillName);
        if (builtInSkill != null) {
            model.setSkillLevel(builtInSkill, level);
            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            context.getSource().sendSuccess(() -> Component.literal("Set ")
                    .append(player.getName())
                    .append("'s ")
                    .append(Component.literal(builtInSkill.getSerializedName()))
                    .append(" to " + level), true);

            return 1;
        }

        CustomSkillSlot customSkill = Configuration.findCustomSkillById(skillName);
        if (customSkill != null) {
            model.setCustomSkillLevel(customSkill.getId(), level);
            model.updateSkillAttributeBonuses(player);
            SyncToClient.send(player);

            context.getSource().sendSuccess(() -> Component.literal("Set ")
                    .append(player.getName())
                    .append("'s ")
                    .append(Component.literal(customSkill.getDisplayName()))
                    .append(" to " + level), true);

            return 1;
        }

        context.getSource().sendFailure(Component.literal("Unknown skill: " + skillName));
        return 0;
    }

    private static int executeRespec(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            context.getSource().sendFailure(Component.literal("Could not access the player's skill data."));
            return 0;
        }

        int refund = model.resetAllSkillsAndReturnRefund(player);
        if (refund > 0) {
            player.giveExperiencePoints(refund);
        }

        model.updateSkillAttributeBonuses(player);
        SyncToClient.send(player);

        context.getSource().sendSuccess(() -> Component.literal("Respecced ")
                .append(player.getName())
                .append(" and refunded " + refund + " XP."), true);

        return 1;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}