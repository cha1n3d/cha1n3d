package com.bobux.vaz2109.station;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.role.Roles;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class StationMenu extends AbstractContainerMenu {
   public static final int MAX_BATCH = 16;
   public final Station station;
   private final ContainerLevelAccess access;
   private final Player player;

   public StationMenu(int id, Inventory inv, FriendlyByteBuf buf) {
      this(id, inv, (Station)buf.readEnum(Station.class), ContainerLevelAccess.NULL);
   }

   public StationMenu(int id, Inventory inv, Station station, ContainerLevelAccess access) {
      super((MenuType)ModRegistry.STATION_MENU.get(), id);
      this.station = station;
      this.access = access;
      this.player = inv.player;
   }

   public List<StationRecipe> recipes() {
      return StationRecipe.list(this.player.level(), this.station);
   }

   public boolean clickMenuButton(Player player, int id) {
      List<StationRecipe> list = this.recipes();
      int index = id >> 1;
      if (index >= 0 && index < list.size()) {
         StationRecipe recipe = list.get(index);
         int times = (id & 1) == 1 ? 16 : 1;
         int made = 0;

         for (int i = 0; i < times && recipe.take(player.getInventory(), false); i++) {
            ItemStack out = recipe.result();
            if (!player.getInventory().add(out)) {
               player.drop(out, false);
            }

            if (Roles.doubles(player, this.station, out)) {
               ItemStack extra = recipe.result();
               if (!player.getInventory().add(extra)) {
                  player.drop(extra, false);
               }
            }

            made++;
         }

         if (made > 0) {
            this.access.execute((level, pos) -> level.playSound(null, pos, (SoundEvent)this.station.craftSound().get(), SoundSource.BLOCKS, 1.0F, 1.0F));
            player.inventoryMenu.broadcastChanges();
         }

         return made > 0;
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      return (Boolean)this.access
         .evaluate(
            (level, pos) -> {
               if (level.getBlockState(pos).getBlock() instanceof StationBlock b
                  && b.station == this.station
                  && player.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5) <= 64.0) {
                  return true;
               }

               return false;
            },
            true
         );
   }
}
