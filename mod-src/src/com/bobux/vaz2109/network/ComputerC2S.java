package com.bobux.vaz2109.network;

import com.bobux.vaz2109.computer.ComputerMenu;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

/** A click in the computer window: sell, buy offer n (one or ten lots) or hand in an order. */
public class ComputerC2S {
   public final int container;
   public final int action;
   public final int arg;

   public ComputerC2S(int container, int action, int arg) {
      this.container = container;
      this.action = action;
      this.arg = arg;
   }

   public ComputerC2S(FriendlyByteBuf buf) {
      this(buf.readVarInt(), buf.readByte(), buf.readVarInt());
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.container);
      buf.writeByte(this.action);
      buf.writeVarInt(this.arg);
   }

   public void handle(Supplier<Context> ctx) {
      ServerPlayer player = ctx.get().getSender();
      if (player != null && player.containerMenu instanceof ComputerMenu menu && menu.containerId == this.container && menu.stillValid(player)) {
         menu.act(player, this.action, this.arg);
      }
   }
}
