package com.bobux.vaz2109.computer;

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

/** Paper rubles: bought and sold at face value on the computer, handed to other players, or right-clicked onto the account. */
public class BanknoteItem extends Item {
   public final int value;

   public BanknoteItem(int value, Properties properties) {
      super(properties);
      this.value = value;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         long sum = (long)this.value * (long)stack.getCount();
         ComputerShop.setBalance(player, ComputerShop.balance(player) + sum);
         player.displayClientMessage(
            Component.translatable("message.vaz2109.banknote.deposit", new Object[]{sum, ComputerShop.balance(player)}).withStyle(ChatFormatting.GOLD), true
         );
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.3F);
      }

      return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.banknote", new Object[]{this.value}).withStyle(ChatFormatting.GOLD));
      tooltip.add(Component.translatable("tooltip.vaz2109.banknote.use").withStyle(ChatFormatting.GRAY));
   }
}
