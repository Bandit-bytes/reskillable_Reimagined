package net.bandit.reskillable.client;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Reskillable.MOD_ID, value = Dist.CLIENT)
public final class Tooltip {
    private Tooltip() {
    }

    @SubscribeEvent
    public static void onTooltipDisplay(ItemTooltipEvent event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        ItemStack stack = event.getItemStack();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }

        Requirement[] requirements = Configuration.getRequirements(itemId);
        if (requirements == null || requirements.length == 0) {
            return;
        }

        SkillModel skillModel = SkillModel.get(Minecraft.getInstance().player);
        if (skillModel == null) {
            return;
        }

        List<Component> tooltips = event.getToolTip();
        tooltips.add(Component.empty());
        tooltips.add(
                Component.translatable("tooltip.requirements")
                        .append(":")
                        .withStyle(ChatFormatting.GRAY)
        );

        for (Requirement req : requirements) {
            boolean meets = skillModel.getSkillLevel(req.skill) >= req.level;

            tooltips.add(
                    Component.literal("- ")
                            .append(getSkillDisplayComponent(req.skill))
                            .append(" " + req.level)
                            .withStyle(meets ? ChatFormatting.GREEN : ChatFormatting.RED)
            );
        }
    }

    private static MutableComponent getSkillDisplayComponent(String skillId) {
        String normalized = normalizeSkillId(skillId);

        Skill vanilla = getVanillaSkillOrNull(normalized);
        if (vanilla != null) {
            return Component.translatable("skill." + normalized);
        }

        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(normalized);
        if (custom != null) {
            return Component.literal(custom.getDisplayName());
        }

        return Component.literal(normalized);
    }

    private static Skill getVanillaSkillOrNull(String skillId) {
        try {
            return Skill.valueOf(skillId.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeSkillId(String skillId) {
        return skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
    }
}
