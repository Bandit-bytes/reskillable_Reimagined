package net.bandit.reskillable.client;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.logging.Logger;

public class Tooltip {
    private static final Logger LOGGER = Logger.getLogger(Tooltip.class.getName());

    @SubscribeEvent
    public void onTooltipDisplay(ItemTooltipEvent event) {
        if (Minecraft.getInstance().player != null) {
            ItemStack stack = event.getItemStack();
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemRegistryName != null) {
                Requirement[] requirements = Configuration.getRequirements(itemRegistryName);
//                LOGGER.info("Retrieved requirements for " + itemRegistryName + ": " + Arrays.toString(requirements));

                if (requirements != null) {
                    List<Component> tooltips = event.getToolTip();
                    tooltips.add(Component.literal(""));
                    tooltips.add(Component.translatable("tooltip.requirements").append(":").withStyle(ChatFormatting.GRAY));

                    SkillModel skillModel = SkillModel.get(Minecraft.getInstance().player);

                    for (Requirement requirement : requirements) {
                        ChatFormatting colour = skillModel.getSkillLevel(requirement.skill) >= requirement.level ? ChatFormatting.GREEN : ChatFormatting.RED;
                        tooltips.add(Component.translatable(requirement.skill.displayName).append(" " + requirement.level).withStyle(colour));
                    }
                }
            }
        }
    }
}
