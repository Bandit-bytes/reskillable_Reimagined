package net.bandit.reskillable.common.commands.skills;

import net.bandit.reskillable.Configuration;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum SkillAttributeBonus {
    ATTACK(Skill.ATTACK, () -> Attributes.ATTACK_DAMAGE, () -> Configuration.ATTACK_DAMAGE_BONUS.get()),
    GATHERING(Skill.GATHERING, () -> null, () -> Configuration.GATHERING_RANGE_BONUS.get()),
    MINING(Skill.MINING, () -> null, () -> Configuration.MINING_SPEED_MULTIPLIER.get()),
    FARMING(Skill.FARMING, () -> null, () -> Configuration.CROP_GROWTH_CHANCE.get()),
    BUILDING(Skill.BUILDING, () -> ForgeMod.BLOCK_REACH.get(), () -> Configuration.BLOCK_REACH_BONUS.get()),
    DEFENSE(Skill.DEFENSE, () -> Attributes.ARMOR, () -> Configuration.ARMOR_BONUS.get()),
    AGILITY(Skill.AGILITY, () -> Attributes.MOVEMENT_SPEED, () -> Configuration.MOVEMENT_SPEED_BONUS.get()),
    MAGIC(Skill.MAGIC, Configuration::getConfiguredMagicAttribute, () -> Configuration.LUCK_BONUS.get());

    public final Skill skill;
    private final Supplier<Attribute> attributeSupplier;
    private final Supplier<Double> bonusSupplier;

    SkillAttributeBonus(Skill skill, Supplier<Attribute> attributeSupplier, Supplier<Double> bonusSupplier) {
        this.skill = skill;
        this.attributeSupplier = attributeSupplier;
        this.bonusSupplier = bonusSupplier;
    }

    public double getBonusPerStep() {
        return bonusSupplier.get();
    }

    public Attribute getAttribute() {
        return attributeSupplier.get();
    }

    public static @Nullable SkillAttributeBonus getBySkill(Skill skill) {
        for (SkillAttributeBonus bonus : values()) {
            if (bonus.skill == skill) return bonus;
        }
        return null;
    }
}

