package com.fallenroses.registry;

import com.fallenroses.FallenRoses;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> CURSED_TECHNIQUE = ResourceKey.create(Registries.DAMAGE_TYPE, FallenRoses.id("cursed_technique"));

    public static DamageSource technique(Entity direct, @Nullable Entity owner) {
        return new DamageSource(direct.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CURSED_TECHNIQUE), direct, owner);
    }

    private ModDamageTypes() {
    }
}
