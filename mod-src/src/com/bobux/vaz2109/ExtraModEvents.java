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
   }
}
