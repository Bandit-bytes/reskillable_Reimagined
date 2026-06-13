package net.bandit.reskillable.common.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
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
import java.util.logging.Logger;

@EventBusSubscriber
public class GetCommand {
    private static final Logger LOGGER = Logger.getLogger(GetCommand.class.getName());

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("get")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(GetCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String skillId = normalizeSkillId(StringArgumentType.getString(context, "skill"));

        if (!Configuration.isKnownSkill(skillId)) {
            context.getSource().sendFailure(Component.literal("Unknown skill: " + skillId));
            return 0;
        }

        int level = SkillModel.get(player).getSkillLevel(skillId);

        context.getSource().sendSuccess(
                () -> Component.literal("")
                        .append(getSkillDisplayComponent(skillId))
                        .append(" " + level),
                true
        );

        return level;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("skills")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("skill", StringArgumentType.word())
                                                .executes(GetCommand::execute))))
        );
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