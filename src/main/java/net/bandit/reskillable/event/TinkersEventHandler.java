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

        ResourceLocation exactTinkersId = getTinkersRequirementId(stack);
        if (exactTinkersId == null) {
            return true;
        }

        // 1. Check exact material/version-based requirement first
        if (!checkRequirements(model, player, exactTinkersId)) {
            return false;
        }

        // 2. Then also check generic base item requirement
        ResourceLocation baseId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return baseId == null || checkRequirements(model, player, baseId);
    }

    private ResourceLocation getTinkersRequirementId(ItemStack stack) {
        ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        CompoundTag tag = stack.getTag();

        if (itemRegistryName == null || tag == null) {
            return null;
        }

        // Detect Tinkers-style tool data by NBT presence, so addon tools can work too
        if (!tag.contains("tic_materials", Tag.TAG_LIST)) {
            return null;
        }

        ListTag materials = tag.getList("tic_materials", Tag.TAG_STRING);

        // If somehow there are no materials, just use the base item
        if (materials.isEmpty()) {
            return itemRegistryName;
        }

        // Format:
        // <namespace>:<item>__<material1>__<material2>__<material3>
        // Example:
        // tconstruct:pickaxe__tconstruct_wood__tconstruct_wood__tconstruct_skyslime_vine
        StringBuilder path = new StringBuilder(itemRegistryName.getPath());

        for (int i = 0; i < materials.size(); i++) {
            String materialId = materials.getString(i);
            path.append("__").append(sanitize(materialId));
        }

        return new ResourceLocation(itemRegistryName.getNamespace(), path.toString());
    }

    private String sanitize(String input) {
        return input.replace(':', '_')
                .replace('/', '_')
                .replace('#', '_');
    }
    /**Example's:
     "tconstruct:pickaxe__tconstruct_wood__tconstruct_wood__tconstruct_skyslime_vine": [
     "mining:4"
     ],
     "tconstruct:kama__tconstruct_manyullyn__tconstruct_manyullyn__tconstruct_manyullyn": [
     "attack:12"
     ],
     "tconstruct:broad_axe__tconstruct_pig_iron__tconstruct_pig_iron__tconstruct_pig_iron__tconstruct_pig_iron": [
     "attack:10",
     "mining:10"
     ],
     "tconstruct:pickaxe": [
     "mining:2"
     ]
     }
     }
    ***/
}