package com.bobux.vaz2109.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
   private static final String VERSION = "14";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("vaz2109", "main"), () -> "14", "14"::equals, "14"::equals);

   private Network() {
   }

   public static void register() {
      int id = 0;
      CHANNEL.messageBuilder(PaintC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(PaintC2S::encode)
         .decoder(PaintC2S::new)
         .consumerMainThread(PaintC2S::handle)
         .add();
      CHANNEL.messageBuilder(PaintS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(PaintS2C::encode)
         .decoder(PaintS2C::new)
         .consumerMainThread(PaintS2C::handle)
         .add();
      CHANNEL.messageBuilder(SetSprayC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(SetSprayC2S::encode)
         .decoder(SetSprayC2S::new)
         .consumerMainThread(SetSprayC2S::handle)
         .add();
      CHANNEL.messageBuilder(HornC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(HornC2S::encode)
         .decoder(HornC2S::new)
         .consumerMainThread(HornC2S::handle)
         .add();
      CHANNEL.messageBuilder(RemovePartC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(RemovePartC2S::encode)
         .decoder(RemovePartC2S::new)
         .consumerMainThread(RemovePartC2S::handle)
         .add();
      CHANNEL.messageBuilder(RadioC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(RadioC2S::encode)
         .decoder(RadioC2S::new)
         .consumerMainThread(RadioC2S::handle)
         .add();
      CHANNEL.messageBuilder(TriggerC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(TriggerC2S::encode)
         .decoder(TriggerC2S::new)
         .consumerMainThread(TriggerC2S::handle)
         .add();
      CHANNEL.messageBuilder(ReloadC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(ReloadC2S::encode)
         .decoder(ReloadC2S::new)
         .consumerMainThread(ReloadC2S::handle)
         .add();
      CHANNEL.messageBuilder(TracerS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(TracerS2C::encode)
         .decoder(TracerS2C::new)
         .consumerMainThread(TracerS2C::handle)
         .add();
      CHANNEL.messageBuilder(FlashS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(FlashS2C::encode)
         .decoder(FlashS2C::new)
         .consumerMainThread(FlashS2C::handle)
         .add();
      CHANNEL.messageBuilder(ShakeS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(ShakeS2C::encode)
         .decoder(ShakeS2C::new)
         .consumerMainThread(ShakeS2C::handle)
         .add();
      CHANNEL.messageBuilder(ComboS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(ComboS2C::encode)
         .decoder(ComboS2C::new)
         .consumerMainThread(ComboS2C::handle)
         .add();
      CHANNEL.messageBuilder(SkateC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(SkateC2S::encode)
         .decoder(SkateC2S::new)
         .consumerMainThread(SkateC2S::handle)
         .add();
      CHANNEL.messageBuilder(PickupCarC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(PickupCarC2S::encode)
         .decoder(PickupCarC2S::new)
         .consumerMainThread(PickupCarC2S::handle)
         .add();
      CHANNEL.messageBuilder(GraffitiC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(GraffitiC2S::encode)
         .decoder(GraffitiC2S::new)
         .consumerMainThread(GraffitiC2S::handle)
         .add();
      CHANNEL.messageBuilder(GraffitiS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(GraffitiS2C::encode)
         .decoder(GraffitiS2C::new)
         .consumerMainThread(GraffitiS2C::handle)
         .add();
      CHANNEL.messageBuilder(GraffitiChunkS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(GraffitiChunkS2C::encode)
         .decoder(GraffitiChunkS2C::new)
         .consumerMainThread(GraffitiChunkS2C::handle)
         .add();
      CHANNEL.messageBuilder(AimC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(AimC2S::encode)
         .decoder(AimC2S::new)
         .consumerMainThread(AimC2S::handle)
         .add();
      CHANNEL.messageBuilder(CutsceneS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(CutsceneS2C::encode)
         .decoder(CutsceneS2C::new)
         .consumerMainThread(CutsceneS2C::handle)
         .add();
      CHANNEL.messageBuilder(PossessionS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(PossessionS2C::encode)
         .decoder(PossessionS2C::new)
         .consumerMainThread(PossessionS2C::handle)
         .add();
      CHANNEL.messageBuilder(PowerC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(PowerC2S::encode)
         .decoder(PowerC2S::new)
         .consumerMainThread(PowerC2S::handle)
         .add();
      CHANNEL.messageBuilder(PowerS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(PowerS2C::encode)
         .decoder(PowerS2C::new)
         .consumerMainThread(PowerS2C::handle)
         .add();
      CHANNEL.messageBuilder(TimeStopS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(TimeStopS2C::encode)
         .decoder(TimeStopS2C::new)
         .consumerMainThread(TimeStopS2C::handle)
         .add();
      CHANNEL.messageBuilder(CursedFxS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(CursedFxS2C::encode)
         .decoder(CursedFxS2C::new)
         .consumerMainThread(CursedFxS2C::handle)
         .add();
      CHANNEL.messageBuilder(RoleS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(RoleS2C::encode)
         .decoder(RoleS2C::new)
         .consumerMainThread(RoleS2C::handle)
         .add();
      CHANNEL.messageBuilder(RoleC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(RoleC2S::encode)
         .decoder(RoleC2S::new)
         .consumerMainThread(RoleC2S::handle)
         .add();
      CHANNEL.messageBuilder(QuestS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(QuestS2C::encode)
         .decoder(QuestS2C::new)
         .consumerMainThread(QuestS2C::handle)
         .add();
      CHANNEL.messageBuilder(QuestC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(QuestC2S::encode)
         .decoder(QuestC2S::new)
         .consumerMainThread(QuestC2S::handle)
         .add();
      CHANNEL.messageBuilder(AwakeningS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(AwakeningS2C::encode)
         .decoder(AwakeningS2C::new)
         .consumerMainThread(AwakeningS2C::handle)
         .add();
      CHANNEL.messageBuilder(CyberS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(CyberS2C::encode)
         .decoder(CyberS2C::new)
         .consumerMainThread(CyberS2C::handle)
         .add();
      CHANNEL.messageBuilder(SandeS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(SandeS2C::encode)
         .decoder(SandeS2C::new)
         .consumerMainThread(SandeS2C::handle)
         .add();
      CHANNEL.messageBuilder(CyberC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(CyberC2S::encode)
         .decoder(CyberC2S::new)
         .consumerMainThread(CyberC2S::handle)
         .add();
      CHANNEL.messageBuilder(ComputerC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
         .encoder(ComputerC2S::encode)
         .decoder(ComputerC2S::new)
         .consumerMainThread(ComputerC2S::handle)
         .add();
   }
}
