package com.bobux.vaz2109.item;

import java.util.List;
import java.util.function.BiConsumer;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A bottled drink: effects on finishing, alcohol units toward {@link Intoxication}, the bottle comes back. */
public class DrinkItem extends Item {
   public final String id;
   public final float units;
   private final int food;
   private final BiConsumer<Player, Level> effects;

   public DrinkItem(String id, float units, int food, BiConsumer<Player, Level> effects, Properties properties) {
      super(properties);
      this.id = id;
      this.units = units;
      this.food = food;
      this.effects = effects;
   }

   public int getUseDuration(ItemStack stack) {
      return 32;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.DRINK;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return ItemUtils.startUsingInstantly(level, player, hand);
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide && entity instanceof Player player) {
         this.effects.accept(player, level);
         player.getFoodData().eat(this.food, 0.3F);
         Intoxication.drink(player, this.units);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), this.units > 0.0F ? SoundEvents.PLAYER_BURP : SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7F, 0.9F);
      }

      if (entity instanceof Player player && !player.getAbilities().instabuild) {
         stack.shrink(1);
         ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
         if (stack.isEmpty()) {
            return bottle;
         }

         if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
         }
      }

      return stack;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109." + this.id).withStyle(ChatFormatting.GRAY));
      if (this.units > 0.0F) {
         tooltip.add(Component.translatable("tooltip.vaz2109.drink.units", new Object[]{this.units}).withStyle(ChatFormatting.DARK_RED));
      } else {
         tooltip.add(Component.translatable("tooltip.vaz2109.drink.soft").withStyle(ChatFormatting.DARK_GREEN));
      }
   }

   static void give(Player player, net.minecraft.world.effect.MobEffect effect, int ticks, int amplifier) {
      player.addEffect(new MobEffectInstance(effect, ticks, amplifier));
   }

   // ---- the drinks ---------------------------------------------------------------------------------

   public static void kvass(Player p, Level l) {
      give(p, MobEffects.MOVEMENT_SPEED, 600, 0);
      p.getFoodData().eat(2, 0.6F);
   }

   public static void wine(Player p, Level l) {
      give(p, MobEffects.REGENERATION, 160, 0);
      give(p, MobEffects.DAMAGE_BOOST, 900, 0);
   }

   public static void champagne(Player p, Level l) {
      give(p, MobEffects.MOVEMENT_SPEED, 1200, 0);
      give(p, MobEffects.JUMP, 1200, 0);
   }

   public static void vodka(Player p, Level l) {
      give(p, MobEffects.DAMAGE_RESISTANCE, 1200, 0);
      p.setTicksFrozen(0);
      p.clearFire();
   }

   public static void cognac(Player p, Level l) {
      give(p, MobEffects.DAMAGE_BOOST, 1800, 0);
      give(p, MobEffects.ABSORPTION, 1200, 0);
   }

   public static void moonshine(Player p, Level l) {
      give(p, MobEffects.DAMAGE_BOOST, 900, 1);
      give(p, MobEffects.FIRE_RESISTANCE, 900, 0);
      give(p, MobEffects.CONFUSION, 300, 0);
      if (p.getRandom().nextFloat() < 0.25F) {
         give(p, MobEffects.POISON, 100, 0);
         give(p, MobEffects.BLINDNESS, 60, 0);
      }
   }
}
