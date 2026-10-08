package com.bobux.vaz2109.network;

import com.bobux.vaz2109.client.SandeClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Someone nearby fired a Sandevistan: draw their afterimages for this many ticks. */
public class SandeS2C {
   public final int entity;
   public final int ticks;

   public SandeS2C(int entity, int ticks) {
      this.entity = entity;
      this.ticks = ticks;
   }

   public SandeS2C(FriendlyByteBuf buf) {
      this(buf.readVarInt(), buf.readVarInt());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.entity);
      buf.writeVarInt(this.ticks);
   }

   public void handle(Supplier<Context> ctx) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SandeClient.start(this.entity, this.ticks));
   }
}
