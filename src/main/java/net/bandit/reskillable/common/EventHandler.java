package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.capabilities.SkillProvider;
import net.bandit.reskillable.common.network.SyncToClient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {
    private SkillModel lastDiedPlayerSkills = null;
    private static final Map<UUID, SkillModel> lastDiedPlayerSkillsMap = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative() && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative() && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();

        if (!player.isCreative() && !model.canUseItem(player, item)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Entity entity = event.getTarget();
        ItemStack item = event.getItemStack();

        if (!player.isCreative() && (!model.canUseEntity(player, entity) || !model.canUseItem(player, item))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = player.getMainHandItem();

        if (!player.isCreative() && (!model.canUseItem(player, item) || !model.canAttackEntity(player, event.getTarget()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeEquipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            if (!player.isCreative() && event.getSlot().getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack item = event.getTo();
                if (!model.canUseItem(player, item)) {
                    player.drop(item.copy(), false);
                    item.setCount(0);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDrops(LivingDropsEvent event) {
        if (Configuration.getDisableWool() && event.getEntity() instanceof Sheep) {
            event.getDrops().removeIf(item -> item.getItem().is(ItemTags.WOOL));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            SkillModel model = SkillModel.get(player);
            if (model == null) return;

            if (Configuration.getDeathReset() && !lastDiedPlayerSkillsMap.containsKey(player.getUUID())) {
                model.resetSkills();
            }
            lastDiedPlayerSkillsMap.put(player.getUUID(), model);

        }
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            SkillModel skillModel = new SkillModel();
            SkillProvider provider = new SkillProvider(skillModel);
            event.addCapability(new ResourceLocation("reskillable", "cap_skills"), provider);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide()) {
            UUID playerUUID = event.getOriginal().getUUID();
            SkillModel skillModel = SkillModel.get(event.getEntity());
            if (skillModel != null) {
                SkillModel lastSkills = lastDiedPlayerSkillsMap.getOrDefault(playerUUID, SkillModel.get(event.getOriginal()));
                if (lastSkills != null) {
                    skillModel.cloneFrom(lastSkills);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        SkillModel skillModel = SkillModel.get(event.getEntity());
        if (skillModel != null) {
            SyncToClient.send(event.getEntity());
        }
    }


    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            // Allow time for the player to fully respawn before syncing
            player.getServer().execute(() -> {
                SkillModel skillModel = SkillModel.get(player);
                if (skillModel == null) {
//                    System.out.println("SkillModel temporarily missing for player: " + player.getName().getContents());
                    return; // Avoid proceeding with logic that requires the SkillModel
                }
                // Sync the SkillModel with the client
                SyncToClient.send(player);
//                System.out.println("Skills successfully synced for player: " + player.getName().getContents());
            });
        }
    }


    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            SyncToClient.send(event.getEntity());
        }
    }
}
