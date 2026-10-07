package com.bobux.vaz2109.network;

import com.bobux.vaz2109.role.Roles;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class RoleC2S {
   public static final int CHOOSE = 0;
   public static final int KEY = 1;
   public static final int RELEASE = 2;
   public static final int SELECT = 3;
   public final int kind;
   public final int value;

   public RoleC2S(int kind, int value) {
      this.kind = kind;
      this.value = value;
   }

   public RoleC2S(FriendlyByteBuf buf) {
      this(buf.readByte(), buf.readVarInt());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeByte(this.kind);
      buf.writeVarInt(this.value);
   }

   public void handle(Supplier<Context> ctx) {
      ServerPlayer player = ctx.get().getSender();
      if (player != null) {
         if (this.kind == 0) {
            Roles.choose(player, this.value);
         } else if (this.kind == 1) {
            Roles.press(player, this.value == 1);
         } else if (this.kind == 2) {
            Roles.release(player);
         } else if (this.kind == 3) {
            Roles.select(player, this.value);
         }
      }
   }
}
