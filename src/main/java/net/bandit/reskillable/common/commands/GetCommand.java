package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Configuration.CustomSkillSlot;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public class GetCommand {

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(GetCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String skillName = StringArgumentType.getString(context, "skill").trim().toLowerCase(Locale.ROOT);

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            context.getSource().sendFailure(Component.literal("Could not access the player's skill data."));
            return 0;
        }

        Skill builtInSkill = Skill.fromString(skillName);
        if (builtInSkill != null) {
            int level = model.getSkillLevel(builtInSkill);

            context.getSource().sendSuccess(() ->
                    Component.translatable(builtInSkill.getDisplayName())
                            .append(" " + level), true);

            return level;
        }

        CustomSkillSlot customSkill = Configuration.findCustomSkillById(skillName);
        if (customSkill != null) {
            int level = model.getCustomSkillLevel(customSkill.getId());

            context.getSource().sendSuccess(() ->
                    Component.literal(customSkill.getDisplayName() + " " + level), true);

            return level;
        }

        context.getSource().sendFailure(Component.literal("Unknown skill: " + skillName));
        return 0;
    }
}
