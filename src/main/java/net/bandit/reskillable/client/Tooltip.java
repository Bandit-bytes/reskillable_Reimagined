package net.bandit.reskillable.client;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.skills.Requirement;
import net.bandit.reskillable.common.skills.Skill;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;

public final class Tooltip {
    private Tooltip() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(Tooltip::appendRequirements);
    }

    private static void appendRequirements(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                           net.minecraft.world.item.TooltipFlag tooltipType, List<Component> tooltips) {
        if (Minecraft.getInstance().player == null) return;

        ResourceLocation baseKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation effectiveKey = baseKey;

        // Keep TaCZ's synthetic gun-id convention without linking against TaCZ classes.
        if (FabricLoader.getInstance().isModLoaded("tacz") && baseKey != null && "tacz".equals(baseKey.getNamespace())) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data != null ? data.copyTag() : null;
            if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {
                String cleanId = tag.getString("GunId").replace("tacz:", "").replace("tacz", "");
                effectiveKey = ResourceLocation.fromNamespaceAndPath("tacz", "%s__%s".formatted(baseKey.getPath(), cleanId));
            }
        }

        Requirement[] requirements = effectiveKey == null ? null : Configuration.getRequirements(effectiveKey);
        if (requirements == null || requirements.length == 0) return;

        tooltips.add(Component.empty());
        tooltips.add(Component.translatable("tooltip.requirements").append(":").withStyle(ChatFormatting.GRAY));

        SkillModel model = SkillModel.get(Minecraft.getInstance().player);
        for (Requirement req : requirements) {
            boolean meets = model.getSkillLevel(req.skill) >= req.level;
            tooltips.add(Component.literal("")
                    .append(getSkillDisplayComponent(req.skill))
                    .append(" " + req.level)
                    .withStyle(meets ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }

    private static MutableComponent getSkillDisplayComponent(String skillId) {
        String normalized = skillId == null ? "" : skillId.trim().toLowerCase(Locale.ROOT);
        Skill builtIn = Configuration.resolveBuiltInSkill(normalized);
        if (builtIn != null) {
            String configured = Configuration.getBuiltInSkillDisplayName(builtIn);
            return configured.isBlank() ? Component.translatable(builtIn.getDisplayName()) : Component.literal(configured);
        }
        Configuration.CustomSkillSlot custom = Configuration.getCustomSkill(normalized);
        return custom != null ? Component.literal(custom.getDisplayName()) : Component.literal(normalized);
    }
}
