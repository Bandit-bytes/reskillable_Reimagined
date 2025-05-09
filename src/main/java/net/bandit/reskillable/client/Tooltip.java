package net.bandit.reskillable.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
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

    private boolean isTaczLoaded = ModList.get().isLoaded("tacz");
    private boolean isIronsLoaded = ModList.get().isLoaded("irons_spellbooks");

    @SuppressWarnings("t")
    @SubscribeEvent
    public void onTooltipDisplay(ItemTooltipEvent event) {
        if (Minecraft.getInstance().player != null) {
            ItemStack stack = event.getItemStack();
            ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemRegistryName != null) {
                if (isTaczLoaded) {
                    if (Objects.equals(itemRegistryName.getNamespace(), "tacz")) {
                        // TACZ:    tacz:<gun type>__<gunid>
                        // eg:      tacz:modern_kinetic_gun__bf1_tg1918
                        CompoundTag tag = stack.getTag();
                        if (tag != null) {
                            itemRegistryName = new ResourceLocation("tacz",
                                    "%s__%s".formatted(
                                            itemRegistryName.getPath(),
                                            tag.getString("GunId").replaceAll(":", "_")));
                        }
                    }
                }
                if (isIronsLoaded) {
                    if (stack.getItem() instanceof Scroll) {
                        // ISS:     irons_spellbooks:scroll__<spell id>_<level>
                        // eg:      irons_spellbooks:scroll__teleport_1
                        SpellData spellAtIndex = ISpellContainer.get(stack).getSpellAtIndex(0);
                        AbstractSpell spell = spellAtIndex.getSpell();
                        String[] split = spell.getSpellId().split(":");
                        if (split.length == 2) {
                            itemRegistryName = new ResourceLocation("irons_spellbooks",
                                    "scroll__%s_%d".formatted(
                                            split[0x01],
                                            spellAtIndex.getLevel()
                                    ));
                        }
                    }
                }

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
