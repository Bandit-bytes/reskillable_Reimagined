package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

@EventBusSubscriber
public class SetCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("skills")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(SetCommand::executeAdd)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(SetCommand::executeSet)))));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(context, "targets");
        String skillId = normalizeSkillId(StringArgumentType.getString(context, "skill"));
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (!Configuration.isKnownSkill(skillId)) {
            source.sendFailure(Component.literal("Unknown skill: " + skillId));
            return 0;
        }

        int changed = 0;

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);
            if (model == null) continue;

            int currentLevel = model.getSkillLevel(skillId);
            int newLevel = Math.min(currentLevel + amount, Configuration.getMaxLevel());

            model.setSkillLevel(skillId, newLevel);

            SyncToClient.send(target);
            model.updateSkillAttributeBonuses(target);

            changed++;
        }

        int finalChanged = changed;
        source.sendSuccess(
                () -> Component.literal("Added " + amount + " to ")
                        .append(getSkillDisplayComponent(skillId))
                        .append(" for " + finalChanged + " player(s)."),
                true
        );

        return changed;
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(context, "targets");
        String skillId = normalizeSkillId(StringArgumentType.getString(context, "skill"));
        int level = IntegerArgumentType.getInteger(context, "level");

        if (!Configuration.isKnownSkill(skillId)) {
            source.sendFailure(Component.literal("Unknown skill: " + skillId));
            return 0;
        }

        level = Math.min(level, Configuration.getMaxLevel());

        int changed = 0;

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);
            if (model == null) continue;

            model.setSkillLevel(skillId, level);

            SyncToClient.send(target);
            model.updateSkillAttributeBonuses(target);

            changed++;
        }

        int finalChanged = changed;
        int finalLevel = level;
        source.sendSuccess(
                () -> Component.literal("Set ")
                        .append(getSkillDisplayComponent(skillId))
                        .append(" to " + finalLevel + " for " + finalChanged + " player(s)."),
                true
        );

        return changed;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static MutableComponent getSkillDisplayComponent(String skillId) {
        String normalized = normalizeSkillId(skillId);

        try {
            Skill vanilla = Skill.valueOf(normalized.toUpperCase(Locale.ROOT));
            return Component.translatable(vanilla.displayName);
        } catch (Exception ignored) {
        }

        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(normalized);
        if (custom != null) {
            return Component.literal(custom.getDisplayName());
        }

        return Component.literal(normalized);
    }
}