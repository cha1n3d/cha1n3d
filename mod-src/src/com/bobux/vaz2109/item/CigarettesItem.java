package com.bobux.vaz2109.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A pack of 20: hold right-click to smoke one (smoke comes out of the mouth). Not under water. */
public class CigarettesItem extends Item {
   public CigarettesItem(Properties properties) {
      super(properties);
   }

   public int getUseDuration(ItemStack stack) {
      return 40;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.TOOT_HORN;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (player.isUnderWater()) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.vaz2109.smoke.wet"), true);
         }

         return InteractionResultHolder.fail(player.getItemInHand(hand));
      } else {
         return ItemUtils.startUsingInstantly(level, player, hand);
      }
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int left) {
      if (level.isClientSide && left % 6 == 0) {
         Vec3 look = entity.getLookAngle();
         Vec3 mouth = entity.getEyePosition().add(look.scale(0.35)).add(0.0, -0.12, 0.0);
         level.addParticle(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, look.x * 0.02, 0.02, look.z * 0.02);
         if (left < 14) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, look.x * 0.01, 0.005, look.z * 0.01);
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof Player player) {
         if (!level.isClientSide) {
            Intoxication.smoke(player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25F, 1.8F);
         }

         if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
         }

         player.getCooldowns().addCooldown(this, 60);
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.cigarettes.left", new Object[]{stack.getMaxDamage() - stack.getDamageValue()}).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.cigarettes").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.cigarettes.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
   }
}
