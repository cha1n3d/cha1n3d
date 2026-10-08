package com.bobux.vaz2109;

import com.bobux.vaz2109.entity.AllyFrog;
import com.bobux.vaz2109.entity.ChosoEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Attributes for the mobs added alongside Choso. */
@EventBusSubscriber(modid = "vaz2109", bus = Bus.MOD)
public final class ExtraModEvents {
   private ExtraModEvents() {
   }

   @SubscribeEvent
   public static void attributes(EntityAttributeCreationEvent event) {
      event.put(ModRegistry.CHOSO.get(), ChosoEntity.createAttributes().build());
      event.put(ModRegistry.ALLY_FROG.get(), AllyFrog.createAttributes().build());
      event.put(ModRegistry.GOPNIK.get(), com.bobux.vaz2109.entity.GopnikEntity.createAttributes().build());
      event.put(ModRegistry.MAHITO.get(), com.bobux.vaz2109.entity.MahitoEntity.createAttributes().build());
      event.put(ModRegistry.DZHIGAN.get(), com.bobux.vaz2109.entity.DzhiganEntity.createAttributes().build());
      event.put(ModRegistry.TRANSFIGURED.get(), com.bobux.vaz2109.entity.TransfiguredEntity.createAttributes().build());
      event.put(ModRegistry.CYBERPSYCHO.get(), com.bobux.vaz2109.entity.CyberpsychoEntity.createAttributes().build());
   }

   @SubscribeEvent
   public static void spawns(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
      event.register(
         ModRegistry.GOPNIK.get(),
         net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
         net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         com.bobux.vaz2109.entity.GopnikEntity::canSpawn,
         net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE
      );
      event.register(
         ModRegistry.CYBERPSYCHO.get(),
         net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
         net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
         com.bobux.vaz2109.entity.CyberpsychoEntity::canSpawn,
         net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE
      );
   }
}
