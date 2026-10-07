package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class FingerDrops {
   public static final float UNDEAD = 0.006F;
   public static final float UNDEAD_LOOTING = 0.003F;

   private FingerDrops() {
   }

   public static int ofBoss(EntityType<?> type) {
      if (type == ModRegistry.JACKET.get() || type == ModRegistry.ANDREY.get() || type == ModRegistry.TOJI.get() || type == ModRegistry.CHOSO.get()) {
         return 1;
      } else if (type == ModRegistry.TIMOKHA_CHAIN.get() || type == ModRegistry.ARTHAS.get() || type == ModRegistry.GOJO.get()) {
         return 2;
      } else {
         return type == ModRegistry.MAHORAGA.get() ? 3 : 0;
      }
   }

   @SubscribeEvent
   public static void onDrops(LivingDropsEvent event) {
      LivingEntity dead = event.getEntity();
      if (!dead.level().isClientSide) {
         int n = ofBoss(dead.getType());
         if (n == 0
            && dead.getMobType() == MobType.UNDEAD
            && !BossResistance.isBoss(dead)
            && event.getSource().getEntity() instanceof Player
            && dead.getRandom().nextFloat() < 0.006F + 0.003F * (float)event.getLootingLevel()) {
            n = 1;
         }

         if (n > 0) {
            ItemEntity item = new ItemEntity(
               dead.level(), dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack((ItemLike)ModRegistry.SUKUNA_FINGER.get(), n)
            );
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
         }
      }
   }
}
