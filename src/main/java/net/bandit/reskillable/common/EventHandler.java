package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.payload.SyncToClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {
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
    public void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack item = event.getItem();
        if (!model.canUseItem(player, item)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You lack the skill to use this item.").withStyle(ChatFormatting.RED));
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
        if (model == null || player.isCreative()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean canUseMain = model.canUseItem(player, mainHand);
        boolean canUseOff = model.canUseItem(player, offHand);

        if (!canUseMain || !canUseOff || !model.canAttackEntity(player, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeEquipment(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        // Only intercept armor equips
        if (event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            ItemStack newItem = event.getTo();
            ItemStack oldItem = event.getFrom();

            if (!model.canUseItem(player, newItem)) {
                // Revert the armor equip
                player.setItemSlot(event.getSlot(), oldItem);

                // Drop the armor piece
                player.drop(newItem.copy(), false);

                // Optional message
                player.sendSystemMessage(Component.literal("You lack the skill to equip this armor.").withStyle(ChatFormatting.RED));
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
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        original.revive();
        SkillModel originalModel = SkillModel.get(original);
        SkillModel cloneModel = SkillModel.get(clone);

        if (originalModel != null && cloneModel != null) {
            cloneModel.cloneFrom(originalModel);
            cloneModel.syncSkills(clone);
//            cloneModel.updateSkillAttributeBonuses(clone);
        }
//        original.invalidateCaps();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SkillModel model = SkillModel.get(player);
        if (model != null) {
            SyncToClient.send(player);
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack item = event.getItem();
        if (!model.canUseItem(player, item)) {
            player.sendSystemMessage(Component.literal("You lack the skill to use this item.").withStyle(ChatFormatting.RED));
            event.setCanceled(true);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack tool = player.getMainHandItem();
        if (!model.canUseItem(player, tool)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You lack the skill to use this tool.").withStyle(ChatFormatting.RED));
        }
    }



    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);

        if (!player.level().isClientSide()) {
            SkillModel skillModel = SkillModel.get(player);

            if (skillModel != null) {
                SyncToClient.send((ServerPlayer) player);
//                model.updateSkillAttributeBonuses(player);
            }
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);

        if (!player.level().isClientSide()) {
            SkillModel skillModel = SkillModel.get(player);
            if (skillModel != null) {
                skillModel.syncSkills(player);
//                model.updateSkillAttributeBonuses(player);
            }
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);
        if (model != null) {
            int miningLevel = model.getSkillLevel(Skill.MINING);
            if (miningLevel >= 5) {
                var bonus = SkillAttributeBonus.getBySkill(Skill.MINING);
                if (bonus != null && model.isPerkEnabled(Skill.MINING)) {
                    float multiplier = 1.0f + (miningLevel / 5f) * (float) bonus.getBonusPerStep();
                    event.setNewSpeed(event.getNewSpeed() * multiplier);
                }
            }
        }
    }

    @SubscribeEvent
    public void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        level.players().forEach(player -> {
            if (player.distanceToSqr(Vec3.atCenterOf(event.getPos())) < 64) {
                SkillModel model = SkillModel.get(player);
                if (model != null) {
                    int farmingLevel = model.getSkillLevel(Skill.FARMING);
                    var bonus = SkillAttributeBonus.getBySkill(Skill.FARMING);
                    if (bonus != null && model.isPerkEnabled(Skill.FARMING)) {
                        float chance = (farmingLevel / 5f) * (float) bonus.getBonusPerStep();
                        if (farmingLevel >= 5 && level.random.nextFloat() < chance) {
                            event.setResult(CropGrowEvent.Pre.Result.GROW);
                        }
                    }
                }
            }
        });
    }


    @SubscribeEvent
    public void onXpPickup(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || !model.isPerkEnabled(Skill.GATHERING)) return;

        int gatheringLevel = model.getSkillLevel(Skill.GATHERING);
        if (gatheringLevel < 5) return;

        var bonus = SkillAttributeBonus.getBySkill(Skill.GATHERING);
        if (bonus != null) {
            double bonusPercentPerStep = bonus.getBonusPerStep();
            int bonusSteps = gatheringLevel / 5;
            float originalXp = event.getOrb().value;

            int bonusXp = Math.round(originalXp * (float)(bonusSteps * bonusPercentPerStep));
            if (bonusXp > 0) {
                player.giveExperiencePoints(bonusXp);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        SkillModel model = SkillModel.get(player);
        if (model == null || player.level().isClientSide) return;

//        model.updateSkillAttributeBonuses(player);
    }
//    @SubscribeEvent
//    public void onPlayerTickAgility(PlayerTickEvent event) {
//        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
//
//        Player player = event.player;
//        SkillModel model = SkillModel.get(player);
//        if (model == null) return;
//
//        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
//        if (attribute == null) return;
//
//        var bonus = SkillAttributeBonus.getBySkill(Skill.AGILITY);
//        if (bonus == null) return;
//
//        UUID agilityId = UUID.nameUUIDFromBytes("reskillable:agility".getBytes());
//
//        // Always remove old modifier
//        attribute.removeModifier(agilityId);
//
//        int agilityLevel = model.getSkillLevel(Skill.AGILITY);
//        if (agilityLevel >= 5 && model.isPerkEnabled(Skill.AGILITY)) {
//            double multiplier = (agilityLevel / 5.0) * bonus.getBonusPerStep();
//            if (multiplier > 0) {
//                AttributeModifier mod = new AttributeModifier(
//                        agilityId,
//                        "Reskillable AGILITY bonus",
//                        multiplier,
//                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
//                );
//                attribute.addTransientModifier(mod);
//            }
//        }
//    }
    @SubscribeEvent
    public void onUseTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack totem = event.getTotem();
        if (!totem.is(Items.TOTEM_OF_UNDYING)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Requirement[] reqs = Configuration.getRequirements(Items.TOTEM_OF_UNDYING.builtInRegistryHolder().key().location());

        for (Requirement req : reqs) {
            if (model.getSkillLevel(req.skill) < req.level) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("You lack the skill to use the Totem of Undying.").withStyle(ChatFormatting.RED));
                return;
            }
        }
    }
}