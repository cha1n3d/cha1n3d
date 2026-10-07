package com.bobux.vaz2109.item;

import com.bobux.vaz2109.role.Role;
import com.bobux.vaz2109.role.Roles;
import com.bobux.vaz2109.role.Trial;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BeerItem extends Item {
   public BeerItem(Properties properties) {
      super(properties);
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
      if (!level.isClientSide) {
         boolean brewer = entity instanceof Player p && Roles.is(p, Role.BREWER);
         if (entity instanceof ServerPlayer sp) {
            Trial.BEERS.add(sp, 1);
         }

         entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, brewer ? 1800 : 1200, brewer ? 1 : 0));
         entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
         if (brewer) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
         }

         level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8F, 0.8F);
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
      tooltip.add(Component.translatable("tooltip.vaz2109.beer").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.beer.brewer").withStyle(ChatFormatting.GOLD));
   }
}
