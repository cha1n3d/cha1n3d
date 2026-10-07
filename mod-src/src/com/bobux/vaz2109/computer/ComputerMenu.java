package com.bobux.vaz2109.computer;

import com.bobux.vaz2109.ModRegistry;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Computer: a sell slot over the player's inventory and the shop list. Actions arrive as menu button clicks:
 * SELL sells the slot, SELL_SAME also sells every matching item from the inventory, BUY + n / BUY_TEN + n buys
 * one or ten lots of shop offer n. The ruble balance is synced as two 16-bit data slots.
 */
public class ComputerMenu extends AbstractContainerMenu {
   public static final int SELL = 0;
   public static final int SELL_SAME = 1;
   public static final int BUY = 1000;
   public static final int BUY_TEN = 2000;
   public static final int SELL_X = 34;
   public static final int SELL_Y = 58;
   public static final int INV_X = 9;
   public static final int INV_Y = 140;
   private final SimpleContainer sell = new SimpleContainer(1);
   private final ContainerLevelAccess access;
   private final Player player;
   private int low;
   private int high;

   public ComputerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
      this(id, inv, ContainerLevelAccess.NULL);
   }

   public ComputerMenu(int id, Inventory inv, ContainerLevelAccess access) {
      super(ModRegistry.COMPUTER_MENU.get(), id);
      this.access = access;
      this.player = inv.player;
      this.addSlot(new Slot(this.sell, 0, SELL_X, SELL_Y) {
         public boolean mayPlace(ItemStack stack) {
            return ComputerShop.value(stack) > 0;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
      }

      this.addDataSlot(new DataSlot() {
         public int get() {
            return (int)(ComputerShop.balance(ComputerMenu.this.player) & 65535L);
         }

         public void set(int value) {
            ComputerMenu.this.low = value & 65535;
         }
      });
      this.addDataSlot(new DataSlot() {
         public int get() {
            return (int)(ComputerShop.balance(ComputerMenu.this.player) >> 16 & 65535L);
         }

         public void set(int value) {
            ComputerMenu.this.high = value & 65535;
         }
      });
   }

   /** Balance as the client sees it (server side reads the wallet directly). */
   public long balance() {
      return this.player.level().isClientSide ? (long)this.high << 16 | (long)this.low : ComputerShop.balance(this.player);
   }

   public ItemStack selling() {
      return this.sell.getItem(0);
   }

   public boolean clickMenuButton(Player player, int id) {
      if (id == SELL || id == SELL_SAME) {
         ItemStack stack = this.sell.getItem(0);
         if (stack.isEmpty()) {
            return false;
         } else {
            long earned = (long)ComputerShop.value(stack) * (long)stack.getCount();
            if (id == SELL_SAME) {
               for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                  ItemStack other = player.getInventory().getItem(i);
                  if (ItemStack.isSameItemSameTags(other, stack)) {
                     earned += (long)ComputerShop.value(other) * (long)other.getCount();
                     player.getInventory().setItem(i, ItemStack.EMPTY);
                  }
               }
            }

            this.sell.setItem(0, ItemStack.EMPTY);
            ComputerShop.setBalance(player, ComputerShop.balance(player) + earned);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            this.broadcastChanges();
            return true;
         }
      } else if (id >= BUY && id < BUY_TEN + 1000) {
         List<ComputerShop.Offer> offers = ComputerShop.offers();
         int index = id >= BUY_TEN ? id - BUY_TEN : id - BUY;
         int lots = id >= BUY_TEN ? 10 : 1;
         if (index >= 0 && index < offers.size()) {
            ComputerShop.Offer offer = offers.get(index);
            long cost = (long)offer.price * (long)lots;
            if (ComputerShop.balance(player) < cost) {
               player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.6F, 1.2F);
               return false;
            } else {
               ComputerShop.setBalance(player, ComputerShop.balance(player) - cost);

               for (int i = 0; i < lots; i++) {
                  ItemStack bought = offer.stack();
                  if (!player.getInventory().add(bought)) {
                     player.drop(bought, false);
                  }
               }

               player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.5F, 1.3F);
               this.broadcastChanges();
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = slot.getItem();
         ItemStack copy = stack.copy();
         if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (ComputerShop.value(stack) <= 0 || !this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         return copy;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.clearContainer(player, this.sell);
   }

   public boolean stillValid(Player player) {
      return this.access
         .evaluate(
            (level, pos) -> level.getBlockState(pos).getBlock() instanceof ComputerBlock
               && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
            true
         );
   }
}
