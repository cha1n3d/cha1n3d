package com.fallenroses.registry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "fallenroses");
    public static final RegistryObject<MobEffect> OVERLOAD = EFFECTS.register(
        "overload", () -> new ModEffects.Paralysis(15266047, "5a1b7d2e-1f0b-4c8e-9a51-3e0d9b7c6a01")
    );
    public static final RegistryObject<MobEffect> BOUND = EFFECTS.register(
        "bound", () -> new ModEffects.Paralysis(11894015, "5a1b7d2e-1f0b-4c8e-9a51-3e0d9b7c6a02")
    );
    public static final RegistryObject<MobEffect> ROT = EFFECTS.register("rot", ModEffects.Rot::new);

    private ModEffects() {
    }

    public static class Paralysis extends MobEffect {
        Paralysis(int color, String uuid) {
            super(MobEffectCategory.HARMFUL, color);
            this.addAttributeModifier(Attributes.MOVEMENT_SPEED, uuid, -1.0, Operation.MULTIPLY_TOTAL);
        }
    }

    public static class Rot extends MobEffect {
        Rot() {
            super(MobEffectCategory.HARMFUL, 9071136);
        }

        @Override
        public void applyEffectTick(LivingEntity e, int amplifier) {
            e.invulnerableTime = 0;
            e.hurt(e.damageSources().wither(), 1.0F + (float)amplifier);
            if (e.level() instanceof ServerLevel level) {
                level.sendParticles(
                    ModParticles.smoke(6969880, 0.6F, 20),
                    e.getX(),
                    e.getY() + (double)e.getBbHeight() * 0.6,
                    e.getZ(),
                    3 + amplifier * 2,
                    (double)e.getBbWidth() * 0.4,
                    (double)e.getBbHeight() * 0.3,
                    (double)e.getBbWidth() * 0.4,
                    0.01
                );
            }
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return duration % 20 == 0;
        }
    }
}
