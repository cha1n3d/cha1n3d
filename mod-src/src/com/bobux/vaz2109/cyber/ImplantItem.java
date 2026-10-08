package com.bobux.vaz2109.cyber;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Right-click to have it put in (it hurts). An implant already in that slot comes out and back into the inventory. */
public class ImplantItem extends Item {
   public final Implant implant;

   public ImplantItem(Implant implant, Properties properties) {
      super(properties);
      this.implant = implant;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player instanceof ServerPlayer sp) {
         if (!Cyberware.install(sp, this.implant)) {
            return InteractionResultHolder.fail(stack);
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.implant." + this.implant.id).withStyle(ChatFormatting.AQUA));
      tooltip.add(
         Component.translatable(
               "tooltip.vaz2109.implant.slot",
               new Object[]{Component.translatable("gui.vaz2109.cyber.slot." + this.implant.slot.id()), this.implant.cost}
            )
            .withStyle(ChatFormatting.GRAY)
      );
      tooltip.add(Component.translatable("tooltip.vaz2109.implant.install").withStyle(ChatFormatting.DARK_GRAY));
   }
}
