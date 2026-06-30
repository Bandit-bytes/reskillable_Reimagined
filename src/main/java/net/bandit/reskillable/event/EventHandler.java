package net.bandit.reskillable.event;

import net.bandit.reskillable.Configuration;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.bandit.reskillable.common.capabilities.SkillProvider;
import net.bandit.reskillable.common.commands.skills.Requirement;
import net.bandit.reskillable.common.commands.skills.Skill;
import net.bandit.reskillable.common.commands.skills.SkillAttributeBonus;
import net.bandit.reskillable.common.network.SyncToClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {
    private static final Map<UUID, SkillModel> lastDiedPlayerSkillsMap = new ConcurrentHashMap<>();

    /**
     * Automation mods such as Create use Forge FakePlayer instances to perform
     * block and item interactions. Fake players cannot earn Reskillable levels,
     * so they must not be blocked by normal player skill requirements.
     */
    private static boolean bypassSkillRequirements(Player player) {
        return player instanceof FakePlayer;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative()
                && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack item = event.getItemStack();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();

        if (!player.isCreative()
                && (!model.canUseItem(player, item) || !model.canUseBlock(player, block))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;

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
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Entity entity = event.getTarget();
        ItemStack item = event.getItemStack();

        if (!player.isCreative()
                && (!model.canUseEntity(player, entity) || !model.canUseItem(player, item))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || player.isCreative()) return;

        ItemStack mainHand = player.getMainHandItem();

        if (!model.canUseItem(player, mainHand)
                || !model.canAttackEntity(player, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChangeEquipment(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (bypassSkillRequirements(player)) return;

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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDrops(LivingDropsEvent event) {
        if (Configuration.getDisableWool() && event.getEntity() instanceof Sheep) {
            event.getDrops().removeIf(item -> item.getItem().is(ItemTags.WOOL));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (bypassSkillRequirements(player)) return;
        if (player.level().isClientSide()) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        if (Configuration.getDeathReset()
                && !lastDiedPlayerSkillsMap.containsKey(player.getUUID())) {
            model.resetSkills();
        }

        lastDiedPlayerSkillsMap.put(player.getUUID(), model);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            SkillModel skillModel = new SkillModel();
            SkillProvider provider = new SkillProvider(skillModel);
            event.addCapability(
                    new ResourceLocation("reskillable", "cap_skills"),
                    provider
            );
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        if (bypassSkillRequirements(original) || bypassSkillRequirements(clone)) return;

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
        if (bypassSkillRequirements(player)) return;

        SkillModel skillModel = SkillModel.get(player);

        if (skillModel != null) {
            SyncToClient.send(player);
            skillModel.updateSkillAttributeBonuses(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;
        if (player.level().isClientSide()) return;

        SkillModel model = SkillModel.get(player);

        if (model != null) {
            SyncToClient.send(player);
            model.updateSkillAttributeBonuses(player);
        }
    }

    @SubscribeEvent
    public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;
        if (player.level().isClientSide()) return;

        SkillModel model = SkillModel.get(player);

        if (model != null) {
            model.syncSkills(player);
            model.updateSkillAttributeBonuses(player);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        int miningLevel = model.getSkillLevel(Skill.MINING);

        if (miningLevel >= 5) {
            SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(Skill.MINING);

            if (bonus != null && model.isPerkEnabled(Skill.MINING)) {
                float multiplier = 1.0f
                        + (miningLevel / 5.0f) * (float) bonus.getBonusPerStep();
                event.setNewSpeed(event.getNewSpeed() * multiplier);
            }
        }
    }

    @SubscribeEvent
    public void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        SkillAttributeBonus farmingBonus = SkillAttributeBonus.getBySkill(Skill.FARMING);
        float chancePerStep = farmingBonus != null
                ? (float) farmingBonus.getBonusPerStep()
                : 0.0f;

        level.players().forEach(player -> {
            if (bypassSkillRequirements(player)) return;

            if (player.distanceToSqr(Vec3.atCenterOf(event.getPos())) < 64) {
                SkillModel model = SkillModel.get(player);

                if (model != null && model.isPerkEnabled(Skill.FARMING)) {
                    int farmingLevel = model.getSkillLevel(Skill.FARMING);

                    if (farmingLevel >= 5) {
                        float steps = farmingLevel / 5.0f;
                        float chance = Math.min(steps * chancePerStep, 1.0f);

                        if (level.random.nextFloat() < chance) {
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
        if (bypassSkillRequirements(player)) return;
        if (player.level().isClientSide()) return;

        SkillModel model = SkillModel.get(player);
        if (model == null || !model.isPerkEnabled(Skill.GATHERING)) return;

        int gatheringLevel = model.getSkillLevel(Skill.GATHERING);
        if (gatheringLevel < 5) return;

        SkillAttributeBonus bonus = SkillAttributeBonus.getBySkill(Skill.GATHERING);

        if (bonus != null) {
            double bonusPercentPerStep = bonus.getBonusPerStep();
            int bonusSteps = gatheringLevel / 5;
            float originalXp = event.getOrb().value;

            int bonusXp = Math.round(
                    originalXp * (float) (bonusSteps * bonusPercentPerStep)
            );

            if (bonusXp > 0) {
                player.giveExperiencePoints(bonusXp);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (event.player.tickCount % 20 != 0) return;

        Player player = event.player;
        if (bypassSkillRequirements(player)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        ItemStack offhand = player.getOffhandItem();

        if (offhand.is(Items.TOTEM_OF_UNDYING)) {
            Requirement[] requirements = Configuration.getRequirements(
                    Items.TOTEM_OF_UNDYING
                            .builtInRegistryHolder()
                            .key()
                            .location()
            );

            if (requirements != null) {
                for (Requirement requirement : requirements) {
                    if (model.getSkillLevel(requirement.skill) < requirement.level) {
                        ItemStack droppedTotem = offhand.copy();

                        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                        player.drop(droppedTotem, false);
                        player.sendSystemMessage(
                                Component.literal(
                                                "You lack the required skill to use the Totem of Undying!"
                                        )
                                        .withStyle(ChatFormatting.RED)
                        );
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (bypassSkillRequirements(player)) return;
        if (player.level().isClientSide()) return;

        SkillModel model = SkillModel.get(player);

        if (model != null) {
            model.updateSkillAttributeBonuses(player);
        }
    }

    @SubscribeEvent
    public void onUseTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (bypassSkillRequirements(player)) return;

        ItemStack totem = event.getTotem();
        if (!totem.is(Items.TOTEM_OF_UNDYING)) return;

        SkillModel model = SkillModel.get(player);
        if (model == null) return;

        Requirement[] requirements = Configuration.getRequirements(
                Items.TOTEM_OF_UNDYING
                        .builtInRegistryHolder()
                        .key()
                        .location()
        );

        if (requirements != null) {
            for (Requirement requirement : requirements) {
                if (model.getSkillLevel(requirement.skill) < requirement.level) {
                    event.setCanceled(true);
                    player.sendSystemMessage(
                            Component.literal(
                                            "You lack the skill to use the Totem of Undying."
                                    )
                                    .withStyle(ChatFormatting.RED)
                    );
                    return;
                }
            }
        }
    }
}
