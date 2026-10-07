package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/** Kick scooter: shares the board's physics, steered with A/D on the handlebar, kick with W, rear brake on S, Space to hop. */
public class ScooterEntity extends SkateboardEntity {
   public ScooterEntity(EntityType<? extends ScooterEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected boolean scooter() {
      return true;
   }

   @Override
   public ItemStack displayStack() {
      return new ItemStack((ItemLike)ModRegistry.SCOOTER_ITEM.get());
   }
}
