package net.bandit.reskillable.event;

import net.bandit.reskillable.Reskillable;
import net.bandit.reskillable.common.capabilities.SkillModel;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

public final class SkillAttachments {
    private SkillAttachments() {}

    public static final AttachmentType<SkillModel> SKILL_MODEL = AttachmentRegistry.create(
            ResourceLocation.fromNamespaceAndPath(Reskillable.MOD_ID, "skill_model"),
            builder -> builder
                    .initializer(SkillModel::new)
                    .persistent(SkillModel.CODEC)
                    .copyOnDeath()
    );

    public static void init() {
        // Referencing the static field registers the attachment type.
    }
}
