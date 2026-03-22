package net.bandit.reskillable.event;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class TinkersEventHandler extends AbsEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!handleTinkersCheck(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!handleTinkersCheck(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!handleTinkersCheck(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!handleTinkersCheck(player, player.getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    private boolean handleTinkersCheck(Player player, ItemStack stack) {
        if (player == null || player.isCreative() || stack.isEmpty()) {
            return true;
        }

        SkillModel model = SkillModel.get(player);
        if (model == null) {
            return true;
        }

        String exactKey = getTinkersRequirementKey(stack);
        if (exactKey == null) {
            return true;
        }

        // Check exact key + wildcard fallbacks
        for (String candidate : buildTinkersCandidates(exactKey)) {
            if (!checkRequirementsForKey(model, player, candidate)) {
                return false;
            }
        }

        // Also check plain base item requirement like before
        ResourceLocation baseId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return baseId == null || checkRequirements(model, player, baseId);
    }

    private String getTinkersRequirementKey(ItemStack stack) {
        ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        CompoundTag tag = stack.getTag();

        if (itemRegistryName == null || tag == null) {
            return null;
        }

        // Detect Tinkers-style tool data
        if (!tag.contains("tic_materials", Tag.TAG_LIST)) {
            return null;
        }

        ListTag materials = tag.getList("tic_materials", Tag.TAG_STRING);

        if (materials.isEmpty()) {
            return itemRegistryName.toString();
        }

        StringBuilder key = new StringBuilder(itemRegistryName.toString());

        for (int i = 0; i < materials.size(); i++) {
            key.append("__").append(sanitize(materials.getString(i)));
        }

        return key.toString();
    }

    private List<String> buildTinkersCandidates(String exactKey) {
        List<String> candidates = new ArrayList<>();
        candidates.add(exactKey);

        String[] parts = exactKey.split("__");

        // Build progressively broader wildcard matches:
        // exact
        // item__mat1__mat2__mat3__*
        // item__mat1__mat2__*
        // item__mat1__*
        // item__*
        if (parts.length > 1) {
            for (int i = parts.length - 1; i >= 1; i--) {
                StringBuilder builder = new StringBuilder(parts[0]);
                for (int j = 1; j < i; j++) {
                    builder.append("__").append(parts[j]);
                }
                builder.append("__*");
                candidates.add(builder.toString());
            }
        }

        // plain base item
        candidates.add(parts[0]);

        return candidates;
    }

    private String sanitize(String input) {
        return input.replace(':', '_')
                .replace('/', '_')
                .replace('#', '_');
    }

    /*
    Example supported keys:

    "tconstruct:pickaxe__tconstruct_wood__tconstruct_wood__tconstruct_skyslime_vine": [
      "mining:4"
    ],
    "tconstruct:kama__tconstruct_manyullyn__tconstruct_manyullyn__tconstruct_manyullyn": [
      "attack:12"
    ],
    "tconstruct:broad_axe__tconstruct_pig_iron__*": [
      "attack:10",
      "mining:10"
    ],
    "tconstruct:pickaxe": [
      "mining:2"
    ]
    */
}