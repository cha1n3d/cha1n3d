package com.bobux.vaz2109.item;

import com.bobux.vaz2109.entity.FirecrackerEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A firecracker: lit and thrown on right-click. */
public class ThrowItem extends Item {
   public ThrowItem(Properties properties) {
      super(properties);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
      if (!level.isClientSide) {
         FirecrackerEntity e = new FirecrackerEntity(level, player);
         e.setItem(stack);
         e.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.1F, 1.0F);
         level.addFreshEntity(e);
      }

      player.getCooldowns().addCooldown(this, 10);
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.firecracker").withStyle(ChatFormatting.GRAY));
   }
}
