package net.bandit.reskillable.event;

import net.bandit.reskillable.common.capabilities.SkillModel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class SkillAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "reskillable");

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SkillModel>> SKILL_MODEL =
            ATTACHMENTS.register("skill_model", () ->
                    AttachmentType.serializable(SkillModel::new).copyOnDeath().build()
            );

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
