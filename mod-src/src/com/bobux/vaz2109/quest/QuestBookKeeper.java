package com.bobux.vaz2109.quest;

import com.bobux.vaz2109.item.QuestBookItem;
import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** The quest book is never lost: it stays out of the death drops and comes back into the inventory on respawn. */
@EventBusSubscriber(modid = "vaz2109")
public final class QuestBookKeeper {
   private static final String KEPT = "vaz2109:kept_quest_books";

   private QuestBookKeeper() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onDrops(LivingDropsEvent event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
         return;
      }

      ListTag kept = player.getPersistentData().getList(KEPT, 10);
      Iterator<ItemEntity> it = event.getDrops().iterator();

      while (it.hasNext()) {
         ItemStack stack = it.next().getItem();
         if (stack.getItem() instanceof QuestBookItem) {
            kept.add(stack.save(new CompoundTag()));
            it.remove();
         }
      }

      if (!kept.isEmpty()) {
         player.getPersistentData().put(KEPT, kept);
      }
   }

   @SubscribeEvent
   public static void onClone(PlayerEvent.Clone event) {
      Player original = event.getOriginal();
      if (!event.isWasDeath() || !original.getPersistentData().contains(KEPT)) {
         return;
      }

      ListTag kept = original.getPersistentData().getList(KEPT, 10);
      original.getPersistentData().remove(KEPT);
      Player player = event.getEntity();

      for (int i = 0; i < kept.size(); i++) {
         ItemStack stack = ItemStack.of(kept.getCompound(i));
         if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
         }
      }

      player.getPersistentData().remove(KEPT);
   }
}
