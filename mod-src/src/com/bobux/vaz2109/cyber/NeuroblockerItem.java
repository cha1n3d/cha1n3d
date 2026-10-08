package com.bobux.vaz2109.cyber;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Neuroblockers: end a cyberpsychosis episode, push the next one back and take the strain off the mind. */
public class NeuroblockerItem extends Item {
   public NeuroblockerItem(Properties properties) {
      super(properties);
   }

   public int getUseDuration(ItemStack stack) {
      return 16;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.EAT;
   }

   public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
      return net.minecraft.world.item.ItemUtils.startUsingInstantly(level, player, hand);
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         Cyberware.calm(player);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.neuroblocker").withStyle(ChatFormatting.GRAY));
   }
}
