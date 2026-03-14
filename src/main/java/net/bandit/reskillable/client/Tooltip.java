package net.bandit.reskillable.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.Configuration.CustomSkillSlot;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class Tooltip {
    private static final Logger LOGGER = Logger.getLogger(Tooltip.class.getName());

    private static final boolean IS_TACZ_LOADED = ModList.get().isLoaded("tacz");
    private static final boolean IS_IRONS_LOADED = ModList.get().isLoaded("irons_spellbooks");

    @SubscribeEvent
    public void onTooltipDisplay(ItemTooltipEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        SkillModel skillModel = SkillModel.get(player);
        if (skillModel == null) return;

        ItemStack stack = event.getItemStack();
        ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemRegistryName == null) return;

        // TACZ: tacz:<gun type>__<gunid>
        if (IS_TACZ_LOADED && Objects.equals(itemRegistryName.getNamespace(), "tacz")) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("GunId")) {
                itemRegistryName = new ResourceLocation(
                        "tacz",
                        "%s__%s".formatted(
                                itemRegistryName.getPath(),
                                tag.getString("GunId").replace(":", "_")
                        )
                );
            }
        }

        // ISS: irons_spellbooks:scroll__<spell id>_<level>
        if (IS_IRONS_LOADED && stack.getItem() instanceof Scroll) {
            SpellData spellAtIndex = ISpellContainer.get(stack).getSpellAtIndex(0);
            if (spellAtIndex != null && spellAtIndex.getSpell() != null) {
                AbstractSpell spell = spellAtIndex.getSpell();
                String[] split = spell.getSpellId().split(":");
                if (split.length == 2) {
                    itemRegistryName = new ResourceLocation(
                            "irons_spellbooks",
                            "scroll__%s_%d".formatted(split[1], spellAtIndex.getLevel())
                    );
                }
            }
        }

        Requirement[] requirements = Configuration.getRequirements(itemRegistryName);
        if (requirements == null || requirements.length == 0) return;

        List<Component> tooltips = event.getToolTip();
        tooltips.add(Component.literal(""));
        tooltips.add(Component.translatable("tooltip.requirements")
                .append(":")
                .withStyle(ChatFormatting.GRAY));

        for (Requirement requirement : requirements) {
            if (requirement == null) continue;

            int currentLevel = 0;
            Component skillName = Component.literal("Unknown Skill");

            if (requirement.isVanillaSkill()) {
                currentLevel = skillModel.getSkillLevel(requirement.skill);
                skillName = Component.translatable(requirement.skill.displayName);
            } else if (requirement.isCustomSkill()) {
                currentLevel = skillModel.getCustomSkillLevel(requirement.customSkillId);

                CustomSkillSlot slot = Configuration.findCustomSkillById(requirement.customSkillId);
                if (slot != null) {
                    skillName = Component.literal(slot.getDisplayName());
                } else {
                    skillName = Component.literal(requirement.customSkillId);
                }
            }

            ChatFormatting colour = currentLevel >= requirement.level
                    ? ChatFormatting.GREEN
                    : ChatFormatting.RED;

            tooltips.add(skillName
                    .copy()
                    .append(" " + requirement.level)
                    .withStyle(colour));
        }
    }
}
