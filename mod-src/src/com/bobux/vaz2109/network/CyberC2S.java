package com.bobux.vaz2109.network;

import com.bobux.vaz2109.cyber.Cyberware;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** The implant key was pressed. */
public class CyberC2S {
   public CyberC2S() {
   }

   public CyberC2S(FriendlyByteBuf buf) {
   }

   public void encode(FriendlyByteBuf buf) {
   }

   public void handle(Supplier<Context> ctx) {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) {
         Cyberware.activate(player);
      }
   }
}
