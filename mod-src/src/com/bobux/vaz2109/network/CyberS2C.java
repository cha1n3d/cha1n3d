package com.bobux.vaz2109.network;

import com.bobux.vaz2109.client.CyberClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** The player's cyberware for the HUD: installed implants, humanity, Sandevistan time and cooldown, psychosis episode. */
public class CyberS2C {
   public final int mask;
   public final int humanity;
   public final int sande;
   public final int cooldown;
   public final int episode;

   public CyberS2C(int mask, int humanity, int sande, int cooldown, int episode) {
      this.mask = mask;
      this.humanity = humanity;
      this.sande = sande;
      this.cooldown = cooldown;
      this.episode = episode;
   }

   public CyberS2C(FriendlyByteBuf buf) {
      this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.mask);
      buf.writeVarInt(this.humanity);
      buf.writeVarInt(this.sande);
      buf.writeVarInt(this.cooldown);
      buf.writeVarInt(this.episode);
   }

   public void handle(Supplier<Context> ctx) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CyberClient.handle(this));
   }
}
