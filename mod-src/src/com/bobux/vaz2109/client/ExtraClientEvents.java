package com.bobux.vaz2109.client;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.ChosoEntity;
import com.bobux.vaz2109.computer.ComputerScreen;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.FrogRenderer;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** Renderers for Choso, the turret, the swamp pack, the Piercing Blood jet and the kick scooter; the ability wheel key; the computer screen. */
@EventBusSubscriber(modid = "vaz2109", bus = Bus.MOD, value = Dist.CLIENT)
public final class ExtraClientEvents {
   /** Ability wheel: hold, point with the mouse, let go. Separate from the class key, which only casts. */
   public static final KeyMapping WHEEL = new KeyMapping("key.vaz2109.wheel", KeyConflictContext.IN_GAME, Type.KEYSYM, 90, "key.categories.vaz2109");

   /** Cyberware: fires the Sandevistan. */
   public static final KeyMapping IMPLANT = new KeyMapping("key.vaz2109.implant", KeyConflictContext.IN_GAME, Type.KEYSYM, 66, "key.categories.vaz2109");

   private ExtraClientEvents() {
   }

   @SubscribeEvent
   public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
      event.registerAboveAll("awakening", AwakeningClient::render);
      event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "cyberware", CyberClient::render);
   }

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent event) {
      event.register(WHEEL);
      event.register(IMPLANT);
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(
         ModRegistry.CHOSO.get(), ctx -> new PeopleRenderer<ChosoEntity>(ctx, ChosoSkin.LAYER, "vaz2109_choso", ChosoSkin::new, 1.0F)
      );
      event.registerEntityRenderer(ModRegistry.ALLY_FROG.get(), FrogRenderer::new);
      event.registerEntityRenderer(ModRegistry.TURRET.get(), TurretRenderer::new);
      event.registerEntityRenderer(ModRegistry.BLOOD_BEAM.get(), BloodBeamRenderer::new);
      event.registerEntityRenderer(ModRegistry.SCOOTER.get(), ScooterRenderer::new);
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> MenuScreens.register(ModRegistry.COMPUTER_MENU.get(), ComputerScreen::new));
   }

   @SubscribeEvent
   public static void registerLayers(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(ChosoSkin.LAYER, () -> ArthasModel.build(ChosoSkin.PARTS));
      event.registerLayerDefinition(ScooterRenderer.LAYER, () -> ArthasModel.build(ScooterSkin.PARTS));
   }
}
