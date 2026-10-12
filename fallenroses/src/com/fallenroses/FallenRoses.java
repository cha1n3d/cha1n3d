package com.fallenroses;

import com.fallenroses.config.FRConfig;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.registry.ModCreativeTabs;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModItems;
import com.fallenroses.registry.ModLootModifiers;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("fallenroses")
public class FallenRoses {
    public static final String MODID = "fallenroses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FallenRoses() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);
        ModLoadingContext.get().registerConfig(Type.COMMON, FRConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(Type.CLIENT, FRConfig.CLIENT_SPEC);
        ModNetwork.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation("fallenroses", path);
    }
}
