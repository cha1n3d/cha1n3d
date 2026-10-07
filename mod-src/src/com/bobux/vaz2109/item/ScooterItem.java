package com.bobux.vaz2109.item;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.ScooterEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ScooterItem extends Item {
   public ScooterItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!player.isPassenger() && player.onGround()) {
         if (!level.isClientSide) {
            this.place(level, player, player.position(), stack, true);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
      } else {
         return InteractionResultHolder.pass(stack);
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && context.getClickedFace() == Direction.UP && !player.isPassenger()) {
         Level level = context.getLevel();
         if (!level.isClientSide) {
            Vec3 at = context.getClickLocation();
            this.place(level, player, at, context.getItemInHand(), player.position().distanceTo(at) < 2.0);
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   private void place(Level level, Player player, Vec3 at, ItemStack stack, boolean hopOn) {
      ScooterEntity scooter = ModRegistry.SCOOTER.get().create(level);
      if (scooter != null) {
         scooter.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
         if (level.noCollision(scooter, scooter.getBoundingBox())) {
            level.addFreshEntity(scooter);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6F, 1.5F);
            if (hopOn) {
               player.startRiding(scooter);
            }

            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }
         }
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.scooter.ride").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.scooter.pickup").withStyle(ChatFormatting.DARK_GRAY));
   }
}
