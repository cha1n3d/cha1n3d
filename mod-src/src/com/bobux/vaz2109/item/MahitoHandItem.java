package com.bobux.vaz2109.item;

import com.bobux.vaz2109.entity.MahitoEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Mahito's severed hand: its touch still reshapes souls — damage through armour, withering, and weak mobs it kills twist into transfigured humans. */
public class MahitoHandItem extends SwordItem {
   public MahitoHandItem(Properties properties) {
      super(Tiers.NETHERITE, 2, -2.2F, properties);
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (attacker instanceof net.minecraft.world.entity.player.Player p && p.getAttackStrengthScale(0.5F) > 0.9F) {
         MahitoEntity.transfigure(attacker, target, 2.0F);
      }

      return super.hurtEnemy(stack, target, attacker);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.mahito_hand").withStyle(ChatFormatting.DARK_AQUA));
      tooltip.add(Component.translatable("tooltip.vaz2109.mahito_hand.1").withStyle(ChatFormatting.GRAY));
   }
}
