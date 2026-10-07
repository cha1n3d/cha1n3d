package com.bobux.vaz2109.client;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.ChosoEntity;
import net.minecraft.client.renderer.entity.FrogRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Renderers for Choso, the mechanic's turret and the frog-man's swamp pack. */
@EventBusSubscriber(modid = "vaz2109", bus = Bus.MOD, value = Dist.CLIENT)
public final class ExtraClientEvents {
   private ExtraClientEvents() {
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(
         ModRegistry.CHOSO.get(), ctx -> new PeopleRenderer<ChosoEntity>(ctx, ChosoSkin.LAYER, "vaz2109_choso", ChosoSkin::new, 1.0F)
      );
      event.registerEntityRenderer(ModRegistry.ALLY_FROG.get(), FrogRenderer::new);
      event.registerEntityRenderer(ModRegistry.TURRET.get(), TurretRenderer::new);
   }

   @SubscribeEvent
   public static void registerLayers(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(ChosoSkin.LAYER, () -> ArthasModel.build(ChosoSkin.PARTS));
   }
}
