package com.bobux.vaz2109.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A first-aid kit: two seconds of bandaging, then 5 hearts back and poison, wither and bleeding gone. */
public class MedkitItem extends Item {
   public MedkitItem(Properties properties) {
      super(properties);
   }

   public int getUseDuration(ItemStack stack) {
      return 40;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (player.getHealth() >= player.getMaxHealth() && !player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER)) {
         return InteractionResultHolder.fail(player.getItemInHand(hand));
      } else {
         return ItemUtils.startUsingInstantly(level, player, hand);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide && entity instanceof Player player) {
         player.heal(10.0F);
         player.removeEffect(MobEffects.POISON);
         player.removeEffect(MobEffects.WITHER);
         player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0F, 1.2F);
         player.getCooldowns().addCooldown(this, 200);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.medkit").withStyle(ChatFormatting.GRAY));
   }
}
