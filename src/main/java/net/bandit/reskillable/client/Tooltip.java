package net.bandit.reskillable.client;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.logging.Logger;

public class Tooltip {
    private static final Logger LOGGER = Logger.getLogger(Tooltip.class.getName());

    private boolean isTaczLoaded = ModList.get().isLoaded("tacz");
    private boolean isIronsLoaded = ModList.get().isLoaded("irons_spellbooks");


    @SubscribeEvent
    public void onTooltipDisplay(ItemTooltipEvent event) {

        if (Minecraft.getInstance().player == null)
            return;

        ItemStack stack = event.getItemStack();
        ResourceLocation baseKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation effectiveKey = baseKey;

        if (isTaczLoaded && baseKey != null && "tacz".equals(baseKey.getNamespace())) {

            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data != null ? data.copyTag() : null;

            if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {

                String rawId = tag.getString("GunId");
                String cleanId = rawId
                        .replace("tacz:", "")
                        .replace("tacz", "");   // "m95"

                effectiveKey = ResourceLocation.fromNamespaceAndPath(
                        "tacz",
                        "%s__%s".formatted(
                                baseKey.getPath(),  // modern_kinetic_gun
                                cleanId             // m95
                        )
                );
            }
        }

        if (isIronsLoaded && stack.getItem() instanceof Scroll scroll) {

            SpellData spellAtIndex = ISpellContainer.get(stack).getSpellAtIndex(0);

            if (spellAtIndex != null && spellAtIndex.getSpell() != null) {
                AbstractSpell spell = spellAtIndex.getSpell();
                String[] split = spell.getSpellId().split(":");

                if (split.length == 2) {
                    effectiveKey = ResourceLocation.fromNamespaceAndPath(
                            "irons_spellbooks",
                            "scroll__%s_%d".formatted(
                                    split[1],
                                    spellAtIndex.getLevel()
                            )
                    );
                }
            }
        }
        Requirement[] requirements = Configuration.getRequirements(effectiveKey);

        if (requirements != null) {
            List<Component> tooltips = event.getToolTip();

            tooltips.add(Component.literal(""));
            tooltips.add(Component.translatable("tooltip.requirements")
                    .append(":")
                    .withStyle(ChatFormatting.GRAY));

            SkillModel skillModel = SkillModel.get(Minecraft.getInstance().player);

            for (Requirement req : requirements) {
                boolean meets = skillModel.getSkillLevel(req.skill) >= req.level;

                tooltips.add(
                        Component.translatable(req.skill.displayName)
                                .append(" " + req.level)
                                .withStyle(meets ? ChatFormatting.GREEN : ChatFormatting.RED)
                );
            }
        }
    }
}