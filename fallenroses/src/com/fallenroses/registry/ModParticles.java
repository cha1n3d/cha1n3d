package com.fallenroses.registry;

import com.fallenroses.vfx.FRParticleOptions;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleOptions.Deserializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "fallenroses");
    public static final RegistryObject<ParticleType<FRParticleOptions>> GLOW = reg("glow");
    public static final RegistryObject<ParticleType<FRParticleOptions>> SPARK = reg("spark");
    public static final RegistryObject<ParticleType<FRParticleOptions>> SMOKE = reg("smoke");
    public static final RegistryObject<ParticleType<FRParticleOptions>> PETAL = reg("petal");
    public static final RegistryObject<ParticleType<FRParticleOptions>> STAR = reg("star");
    public static final RegistryObject<ParticleType<FRParticleOptions>> EMBER = reg("ember");

    private static RegistryObject<ParticleType<FRParticleOptions>> reg(String name) {
        return PARTICLES.register(name, () -> new ParticleType<FRParticleOptions>(false, FRParticleOptions.DESERIALIZER) {
                @Override
                public Codec<FRParticleOptions> codec() {
                    return FRParticleOptions.codec(this);
                }
            });
    }

    public static FRParticleOptions glow(int color, float scale, int life) {
        return new FRParticleOptions(GLOW.get(), color, scale, life);
    }

    public static FRParticleOptions spark(int color, float scale, int life) {
        return new FRParticleOptions(SPARK.get(), color, scale, life);
    }

    public static FRParticleOptions smoke(int color, float scale, int life) {
        return new FRParticleOptions(SMOKE.get(), color, scale, life);
    }

    public static FRParticleOptions petal(int color, float scale, int life) {
        return new FRParticleOptions(PETAL.get(), color, scale, life);
    }

    public static FRParticleOptions star(int color, float scale, int life) {
        return new FRParticleOptions(STAR.get(), color, scale, life);
    }

    public static FRParticleOptions ember(int color, float scale, int life) {
        return new FRParticleOptions(EMBER.get(), color, scale, life);
    }

    private ModParticles() {
    }
}
