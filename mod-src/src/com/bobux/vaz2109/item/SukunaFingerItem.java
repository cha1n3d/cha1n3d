package com.bobux.vaz2109.item;

import com.bobux.vaz2109.entity.curse.CurseEntity;
import com.bobux.vaz2109.entity.curse.SukunaVessel;
import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.Role;
import com.bobux.vaz2109.role.Roles;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SukunaFingerItem extends Item {
   public SukunaFingerItem(Properties properties) {
      super(properties);
   }

   public int getUseDuration(ItemStack stack) {
      return 40;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.EAT;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (SukunaVessel.fingers(player) >= 20) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.vaz2109.sukuna.full").withStyle(ChatFormatting.DARK_RED), true);
         }

         return InteractionResultHolder.fail(stack);
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(stack);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!(entity instanceof Player player)) {
         return stack;
      } else {
         if (!level.isClientSide && !Roles.is(player, Role.VESSEL)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6F, 0.8F);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().magic(), 10.0F);
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 2));
            player.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.reject").withStyle(ChatFormatting.DARK_RED));
         } else if (!level.isClientSide) {
            int before = SukunaVessel.fingers(player);
            int now = before + 1;
            SukunaVessel.setFingers(player, now);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.6F);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().magic(), before == 0 ? 6.0F : 3.0F);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            if (level instanceof ServerLevel server) {
               server.sendParticles(CurseEntity.CURSED_ENERGY_DARK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.5, 0.8, 0.5, 0.0);
            }

            player.sendSystemMessage(
               Component.translatable(before == 0 ? "message.vaz2109.sukuna.vessel" : "message.vaz2109.sukuna.finger", new Object[]{now, 20})
                  .withStyle(ChatFormatting.DARK_RED)
            );
            if (now == 20) {
               player.sendSystemMessage(
                  Component.translatable("message.vaz2109.sukuna.all").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})
               );
            }

            for (Ability a : Ability.of(Role.VESSEL)) {
               if (a.fingers == now && a.unlocked(player)) {
                  player.sendSystemMessage(
                     Component.translatable("message.vaz2109.ability.unlocked", new Object[]{Component.translatable("ability.vaz2109." + a.id)})
                        .withStyle(ChatFormatting.AQUA)
                  );
               }
            }

            if (now == 15) {
               player.sendSystemMessage(Component.translatable("message.vaz2109.ability.black_flash").withStyle(ChatFormatting.DARK_RED));
            }

            if (player.isAlive() && player instanceof ServerPlayer sp) {
               if (now == 15 && !com.bobux.vaz2109.entity.curse.SukunaAwakening.awakened(sp)) {
                  com.bobux.vaz2109.entity.curse.SukunaAwakening.start(sp);
               } else {
                  SukunaVessel.possess(sp);
               }
            }
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         return stack;
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.sukuna_finger").withStyle(ChatFormatting.DARK_RED));
      tooltip.add(Component.translatable("tooltip.vaz2109.sukuna_finger.1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.sukuna_finger.2").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.sukuna_finger.3").withStyle(ChatFormatting.GRAY));
   }
}
