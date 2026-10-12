package com.fallenroses.registry;

import com.fallenroses.FallenRoses;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final TagKey<EntityType<?>> CURSED_SPIRITS = TagKey.create(Registries.ENTITY_TYPE, FallenRoses.id("cursed_spirits"));

    private ModTags() {
    }
}
