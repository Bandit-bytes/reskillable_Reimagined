package net.bandit.reskillable.common;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.capabilities.SkillProvider;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.SyncToClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;


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
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        original.reviveCaps();
        SkillModel originalModel = SkillModel.get(original);
        SkillModel cloneModel = SkillModel.get(clone);

        if (originalModel != null && cloneModel != null) {
            cloneModel.cloneFrom(originalModel);
            cloneModel.syncSkills(clone);
            cloneModel.updateSkillAttributeBonuses(clone);
        }
        original.invalidateCaps();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        SkillModel skillModel = SkillModel.get(player);

        if (skillModel != null) {
            SyncToClient.send(player);
            skillModel.updateSkillAttributeBonuses(player);

        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        SkillModel model = SkillModel.get(player);

        if (!player.level().isClientSide()) {
            SkillModel skillModel = SkillModel.get(player);

            if (skillModel != null) {
                SyncToClient.send(player);
                model.updateSkillAttributeBonuses(player);
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
                model.updateSkillAttributeBonuses(player);
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
    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
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
                            event.setResult(Event.Result.ALLOW);
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
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (event.player.tickCount % 20 != 0) return;

        Player player = event.player;
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            Requirement[] reqs = Configuration.getRequirements(Items.TOTEM_OF_UNDYING.builtInRegistryHolder().key().location());
            if (reqs != null) {
                for (Requirement req : reqs) {
                    if (model.getSkillLevel(req.skill) < req.level) {
                        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                        player.drop(offhand, false);
                        player.sendSystemMessage(Component.literal("You lack the required skill to use the Totem of Undying!").withStyle(ChatFormatting.RED));
                        break;
                    }
                }
            }
        }

        for (SkillAttributeBonus bonus : SkillAttributeBonus.values()) {
            if (bonus.getAttribute() != null) {
            }
        }
    }
    @SubscribeEvent
    public void onPlayerEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        SkillModel model = SkillModel.get(player);
        if (model == null || player.level().isClientSide) return;

        model.updateSkillAttributeBonuses(player);
    }
    @SubscribeEvent
    public void onPlayerTickAgility(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;

        var bonus = SkillAttributeBonus.getBySkill(Skill.AGILITY);
        if (bonus == null) return;

        UUID agilityId = UUID.nameUUIDFromBytes("reskillable:agility".getBytes());

        // Always remove old modifier
        attribute.removeModifier(agilityId);

        int agilityLevel = model.getSkillLevel(Skill.AGILITY);
        if (agilityLevel >= 5 && model.isPerkEnabled(Skill.AGILITY)) {
            double multiplier = (agilityLevel / 5.0) * bonus.getBonusPerStep();
            if (multiplier > 0) {
                AttributeModifier mod = new AttributeModifier(
                        agilityId,
                        "Reskillable AGILITY bonus",
                        multiplier,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                );
                attribute.addTransientModifier(mod);
            }
        }
    }
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