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

import java.util.Locale;

public class SetCommand {

    public static void register(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        root.then(
                Commands.literal("add")
                        .requires(Commands.hasPermission(
                                Commands.LEVEL_GAMEMASTERS
                        ))
                        .then(
                                Commands.argument(
                                                "targets",
                                                EntityArgument.players()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "skill",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "amount",
                                                                                IntegerArgumentType.integer(1)
                                                                        )
                                                                        .executes(
                                                                                SetCommand::executeAdd
                                                                        )
                                                        )
                                        )
                        )
        );

        root.then(
                Commands.literal("set")
                        .requires(Commands.hasPermission(
                                Commands.LEVEL_GAMEMASTERS
                        ))
                        .then(
                                Commands.argument(
                                                "targets",
                                                EntityArgument.players()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "skill",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "level",
                                                                                IntegerArgumentType.integer(1)
                                                                        )
                                                                        .executes(
                                                                                SetCommand::executeSet
                                                                        )
                                                        )
                                        )
                        )
        );

        root.then(
                Commands.literal("respec")
                        .requires(Commands.hasPermission(
                                Commands.LEVEL_GAMEMASTERS
                        ))
                        .then(
                                Commands.argument(
                                                "targets",
                                                EntityArgument.players()
                                        )
                                        .executes(SetCommand::executeRespec)
                        )
        );
    }

    private static int executeAdd(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(
                context,
                "targets"
        );

        String skillId = normalizeSkillId(
                StringArgumentType.getString(context, "skill")
        );

        int amount = IntegerArgumentType.getInteger(
                context,
                "amount"
        );

        int changed = 0;

        if (skillId.equals("all")) {
            for (ServerPlayer target : targets) {
                SkillModel model = SkillModel.get(target);

                if (model == null) {
                    continue;
                }

                for (Skill builtIn : Configuration.getEnabledBuiltInSkills()) {
                    int currentLevel = model.getSkillLevel(builtIn);

                    int newLevel = Math.min(
                            currentLevel + amount,
                            Configuration.getMaxLevel()
                    );

                    model.setSkillLevel(builtIn, newLevel);
                }

                for (
                        Configuration.CustomSkillSlot custom
                        : Configuration.getCustomSkills()
                ) {
                    if (custom == null || !custom.isEnabled()) {
                        continue;
                    }

                    String id = normalizeSkillId(custom.id);
                    int currentLevel = model.getSkillLevel(id);

                    int newLevel = Math.min(
                            currentLevel + amount,
                            Configuration.getMaxLevel()
                    );

                    model.setSkillLevel(id, newLevel);
                }

                model.updateSkillAttributeBonuses(target);
                SyncToClient.send(target);
                changed++;
            }

            int finalChanged = changed;

            source.sendSuccess(
                    () -> Component.literal(
                            "Added "
                                    + amount
                                    + " to all skills for "
                                    + finalChanged
                                    + " player(s)."
                    ),
                    true
            );

            return changed;
        }

        if (!Configuration.isKnownSkill(skillId)) {
            source.sendFailure(
                    Component.literal("Unknown skill: " + skillId)
            );

            return 0;
        }

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);

            if (model == null) {
                continue;
            }

            int currentLevel = model.getSkillLevel(skillId);

            int newLevel = Math.min(
                    currentLevel + amount,
                    Configuration.getMaxLevel()
            );

            model.setSkillLevel(skillId, newLevel);
            model.updateSkillAttributeBonuses(target);
            SyncToClient.send(target);

            changed++;
        }

        int finalChanged = changed;

        source.sendSuccess(
                () -> Component.literal("Added " + amount + " to ")
                        .append(getSkillDisplayComponent(skillId))
                        .append(
                                " for "
                                        + finalChanged
                                        + " player(s)."
                        ),
                true
        );

        return changed;
    }

    private static int executeSet(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(
                context,
                "targets"
        );

        String skillId = normalizeSkillId(
                StringArgumentType.getString(context, "skill")
        );

        int requestedLevel = IntegerArgumentType.getInteger(
                context,
                "level"
        );

        int level = Math.min(
                requestedLevel,
                Configuration.getMaxLevel()
        );

        int changed = 0;

        if (skillId.equals("all")) {
            for (ServerPlayer target : targets) {
                SkillModel model = SkillModel.get(target);

                if (model == null) {
                    continue;
                }

                for (Skill builtIn : Configuration.getEnabledBuiltInSkills()) {
                    model.setSkillLevel(builtIn, level);
                }

                for (
                        Configuration.CustomSkillSlot custom
                        : Configuration.getCustomSkills()
                ) {
                    if (custom == null || !custom.isEnabled()) {
                        continue;
                    }

                    model.setSkillLevel(
                            normalizeSkillId(custom.id),
                            level
                    );
                }

                model.updateSkillAttributeBonuses(target);
                SyncToClient.send(target);
                changed++;
            }

            int finalChanged = changed;
            int finalLevel = level;

            source.sendSuccess(
                    () -> Component.literal(
                            "Set all skills to "
                                    + finalLevel
                                    + " for "
                                    + finalChanged
                                    + " player(s)."
                    ),
                    true
            );

            return changed;
        }

        if (!Configuration.isKnownSkill(skillId)) {
            source.sendFailure(
                    Component.literal("Unknown skill: " + skillId)
            );

            return 0;
        }

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);

            if (model == null) {
                continue;
            }

            model.setSkillLevel(skillId, level);
            model.updateSkillAttributeBonuses(target);
            SyncToClient.send(target);

            changed++;
        }

        int finalChanged = changed;
        int finalLevel = level;

        source.sendSuccess(
                () -> Component.literal("Set ")
                        .append(getSkillDisplayComponent(skillId))
                        .append(
                                " to "
                                        + finalLevel
                                        + " for "
                                        + finalChanged
                                        + " player(s)."
                        ),
                true
        );

        return changed;
    }

    private static int executeRespec(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        var targets = EntityArgument.getPlayers(
                context,
                "targets"
        );

        int changed = 0;
        int totalRefund = 0;

        for (ServerPlayer target : targets) {
            SkillModel model = SkillModel.get(target);

            if (model == null) {
                continue;
            }

            int refund =
                    model.resetAllSkillsAndReturnRefund(target);

            if (refund > 0) {
                target.giveExperiencePoints(refund);
            }

            model.updateSkillAttributeBonuses(target);
            SyncToClient.send(target);

            totalRefund += refund;
            changed++;
        }

        int finalChanged = changed;
        int finalRefund = totalRefund;

        source.sendSuccess(
                () -> Component.literal(
                        "Respecced "
                                + finalChanged
                                + " player(s) and refunded "
                                + finalRefund
                                + " total XP."
                ),
                true
        );

        return changed;
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null
                ? ""
                : skillId.trim().toLowerCase(Locale.ROOT);
    }

    private static MutableComponent getSkillDisplayComponent(
            String skillId
    ) {
        String normalized = normalizeSkillId(skillId);

        Skill builtIn = Configuration.resolveBuiltInSkill(normalized);
        if (builtIn != null) {
            String configuredName = Configuration.getBuiltInSkillDisplayName(builtIn);
            return configuredName.isBlank()
                    ? Component.translatable(builtIn.getDisplayName())
                    : Component.literal(configuredName);
        }

        Configuration.CustomSkillSlot custom =
                Configuration.getCustomSkill(normalized);

        if (custom != null) {
            return Component.literal(custom.getDisplayName());
        }

        return Component.literal(normalized);
    }
}