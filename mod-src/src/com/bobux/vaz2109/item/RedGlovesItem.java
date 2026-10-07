package com.bobux.vaz2109.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Yuji Itadori's red gloves from the last arc of the manga: fast blood-soaked punches that slow the target. For the
 * Vessel they count as bare fists (Divergent Fist works through them) and add 5% to the Black Flash chance.
 */
public class RedGlovesItem extends SwordItem {
   private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.05F), 1.1F);

   public RedGlovesItem(Properties properties) {
      super(Tiers.IRON, 2, -1.6F, properties);
   }

   public boolean canPerformAction(ItemStack stack, ToolAction action) {
      return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
   }

   public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
      return repair.is(Items.LEATHER);
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
      if (target.level() instanceof ServerLevel server) {
         server.sendParticles(BLOOD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.25, 0.25, 0.25, 0.0);
      }

      return super.hurtEnemy(stack, target, attacker);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.red_gloves").withStyle(ChatFormatting.DARK_RED));
      tooltip.add(Component.translatable("tooltip.vaz2109.red_gloves.1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.red_gloves.2").withStyle(ChatFormatting.GOLD));
   }
}
