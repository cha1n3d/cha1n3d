package com.bobux.vaz2109.item;

import com.bobux.vaz2109.role.Role;
import com.bobux.vaz2109.role.Roles;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Every drink of alcohol and every cigarette makes the head spin, except for the Brewer. Alcohol and nicotine build up on the player and wear off with time (stored as a level plus the tick it was
 * last updated, decayed lazily). Too much in a short time hurts; the Brewer holds twice as much drink.
 */
public final class Intoxication {
   /** Alcohol units lost per minute. */
   private static final float SOBER_PER_MIN = 1.0F;
   /** Cigarettes "forgotten" per two minutes. */
   private static final float NICOTINE_PER_2MIN = 1.0F;

   private Intoxication() {
   }

   private static float level(Player player, String key, float perTicks, int ticks) {
      CompoundTag data = player.getPersistentData();
      long elapsed = player.level().getGameTime() - data.getLong(key + "At");
      return Math.max(0.0F, data.getFloat(key) - (float)elapsed / (float)ticks * perTicks);
   }

   private static float add(Player player, String key, float perTicks, int ticks, float amount) {
      float now = level(player, key, perTicks, ticks) + amount;
      CompoundTag data = player.getPersistentData();
      data.putFloat(key, now);
      data.putLong(key + "At", player.level().getGameTime());
      return now;
   }

   /** Nausea for at least this long (never shortens a longer one). */
   private static void spin(Player player, int ticks) {
      MobEffectInstance now = player.getEffect(MobEffects.CONFUSION);
      if (now == null || now.getDuration() < ticks) {
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
      }
   }

   public static float drunk(Player player) {
      return level(player, "vaz2109Drunk", SOBER_PER_MIN, 1200);
   }

   /** Adds alcohol units and applies what the total does to you. */
   public static void drink(Player player, float units) {
      if (units <= 0.0F) {
         return;
      }

      boolean brewer = Roles.is(player, Role.BREWER);
      float total = add(player, "vaz2109Drunk", SOBER_PER_MIN, 1200, units) / (brewer ? 2.0F : 1.0F);
      if (!brewer) {
         // every drink spins the head (the Brewer is used to it)
         spin(player, 100 + Math.round(units * 60.0F));
      }

      if (total >= 10.0F) {
         player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
         player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 1));
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600, 0));
         player.displayClientMessage(Component.translatable("message.vaz2109.drunk.3").withStyle(ChatFormatting.DARK_RED), true);
      } else if (total >= 7.0F) {
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0));
         player.displayClientMessage(Component.translatable("message.vaz2109.drunk.2").withStyle(ChatFormatting.RED), true);
      } else if (total >= 4.0F) {
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
         player.displayClientMessage(Component.translatable("message.vaz2109.drunk.1").withStyle(ChatFormatting.GOLD), true);
      }
   }

   /** One cigarette: a short Haste, and smoking a lot in a row makes you weak and hungry. */
   public static void smoke(Player player) {
      float total = add(player, "vaz2109Nicotine", NICOTINE_PER_2MIN, 2400, 1.0F);
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 900, 0));
      if (!Roles.is(player, Role.BREWER)) {
         spin(player, 120);
      }

      if (total >= 5.0F) {
         player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
         player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
         player.hurt(player.damageSources().magic(), 1.0F);
         player.displayClientMessage(Component.translatable("message.vaz2109.smoke.cough").withStyle(ChatFormatting.GRAY), true);
      }
   }
}
