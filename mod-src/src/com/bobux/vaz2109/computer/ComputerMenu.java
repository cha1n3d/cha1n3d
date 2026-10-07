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
 * Computer: a sell slot over the player's inventory, the shop and the daily orders. Clicks arrive as
 * {@link com.bobux.vaz2109.network.ComputerC2S} (vanilla menu buttons only carry a byte). The ruble balance,
 * the market day, today's finished orders and the server's quote for the sell slot are synced as 16-bit data slots.
 */
public class ComputerMenu extends AbstractContainerMenu {
   public static final int SELL = 0;
   public static final int SELL_SAME = 1;
   public static final int BUY = 2;
   public static final int BUY_TEN = 3;
   public static final int ORDER = 4;
   public static final int SELL_X = 34;
   public static final int SELL_Y = 58;
   public static final int INV_X = 9;
   public static final int INV_Y = 140;
   private final SimpleContainer sell = new SimpleContainer(1);
   private final ContainerLevelAccess access;
   private final Player player;
   private final int[] synced = new int[7];

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

      for (int i = 0; i < this.synced.length; i++) {
         final int slot = i;
         this.addDataSlot(new DataSlot() {
            public int get() {
               return ComputerMenu.this.serverValue(slot);
            }

            public void set(int value) {
               ComputerMenu.this.synced[slot] = value & 65535;
            }
         });
      }
   }

   private int serverValue(int slot) {
      return switch (slot) {
         case 0 -> (int)(ComputerShop.balance(this.player) & 65535L);
         case 1 -> (int)(ComputerShop.balance(this.player) >> 16 & 65535L);
         case 2 -> ComputerShop.day(this.player.level());
         case 3 -> ComputerShop.ordersDone(this.player);
         case 4 -> (int)(Math.min((long)Integer.MAX_VALUE, ComputerShop.quote(this.player, this.selling())) & 65535L);
         case 5 -> (int)(Math.min((long)Integer.MAX_VALUE, ComputerShop.quote(this.player, this.selling())) >> 16 & 65535L);
         default -> ComputerShop.demand(this.player, this.selling());
      };
   }

   private boolean client() {
      return this.player.level().isClientSide;
   }

   public long balance() {
      return this.client() ? (long)this.synced[1] << 16 | (long)this.synced[0] : ComputerShop.balance(this.player);
   }

   public int day() {
      return this.client() ? this.synced[2] : ComputerShop.day(this.player.level());
   }

   public int ordersDone() {
      return this.client() ? this.synced[3] : ComputerShop.ordersDone(this.player);
   }

   /** What the computer pays for the sell slot right now (demand included). */
   public long quote() {
      return this.client() ? (long)this.synced[5] << 16 | (long)this.synced[4] : ComputerShop.quote(this.player, this.selling());
   }

   public int demand() {
      return this.client() ? this.synced[6] : ComputerShop.demand(this.player, this.selling());
   }

   public ItemStack selling() {
      return this.sell.getItem(0);
   }

   private void sound(Player player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
   }

   /** Server side: one click from the computer window. */
   public void act(Player player, int action, int arg) {
      if (action == SELL || action == SELL_SAME) {
         ItemStack stack = this.sell.getItem(0);
         if (!stack.isEmpty()) {
            ItemStack template = stack.copy();
            ComputerShop.sell(player, stack);
            this.sell.setItem(0, ItemStack.EMPTY);
            if (action == SELL_SAME) {
               for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                  ItemStack other = player.getInventory().getItem(i);
                  if (ItemStack.isSameItemSameTags(other, template)) {
                     ComputerShop.sell(player, other);
                     player.getInventory().setItem(i, ItemStack.EMPTY);
                  }
               }
            }

            this.sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.4F);
         }
      } else if (action == BUY || action == BUY_TEN) {
         List<ComputerShop.Offer> offers = ComputerShop.offers();
         if (arg >= 0 && arg < offers.size()) {
            int lots = action == BUY_TEN ? 10 : 1;
            long cost = (long)ComputerShop.price(arg, ComputerShop.day(player.level())) * (long)lots;
            if (ComputerShop.balance(player) < cost) {
               this.sound(player, SoundEvents.VILLAGER_NO, 0.6F, 1.2F);
            } else {
               ComputerShop.setBalance(player, ComputerShop.balance(player) - cost);

               for (int i = 0; i < lots; i++) {
                  ItemStack bought = offers.get(arg).stack();
                  if (!player.getInventory().add(bought)) {
                     player.drop(bought, false);
                  }
               }

               this.sound(player, SoundEvents.VILLAGER_YES, 0.5F, 1.3F);
            }
         }
      } else if (action == ORDER) {
         if (ComputerShop.complete(player, arg)) {
            this.sound(player, SoundEvents.PLAYER_LEVELUP, 0.5F, 1.6F);
         } else {
            this.sound(player, SoundEvents.VILLAGER_NO, 0.6F, 1.2F);
         }
      }

      this.broadcastChanges();
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
