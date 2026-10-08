package com.bobux.vaz2109.network;

import com.bobux.vaz2109.client.AwakeningClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/** Sukuna's awakening on the client: the track started, a hit of the music, the sky cutscene. */
public class AwakeningS2C {
   public static final int TRACK = 0;
   public static final int PULSE = 1;
   public static final int CUTSCENE = 2;
   public final int kind;
   public final int entity;
   public final float value;

   public AwakeningS2C(int kind, int entity, float value) {
      this.kind = kind;
      this.entity = entity;
      this.value = value;
   }

   public AwakeningS2C(FriendlyByteBuf buf) {
      this(buf.readByte(), buf.readVarInt(), buf.readFloat());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeByte(this.kind);
      buf.writeVarInt(this.entity);
      buf.writeFloat(this.value);
   }

   public void handle(Supplier<Context> ctx) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AwakeningClient.handle(this));
   }
}
