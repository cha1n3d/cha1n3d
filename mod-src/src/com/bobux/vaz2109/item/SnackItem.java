package com.bobux.vaz2109.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Street food: sunflower seeds (husks fly out of the mouth), shawarma (now and then a bad one) and instant noodles. */
public class SnackItem extends Item {
   public final String id;
   private final int useTicks;

   public SnackItem(String id, int useTicks, Properties properties) {
      super(properties);
      this.id = id;
      this.useTicks = useTicks;
   }

   public int getUseDuration(ItemStack stack) {
      return this.useTicks;
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (!level.isClientSide && entity instanceof Player player) {
         switch (this.id) {
            case "sunflower_seeds" -> {
               if (level instanceof ServerLevel server) {
                  Vec3 look = player.getLookAngle();
                  Vec3 m = player.getEyePosition().add(look.scale(0.4)).add(0.0, -0.15, 0.0);
                  server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.BLACK_DYE)), m.x, m.y, m.z, 6, 0.05, 0.05, 0.05, 0.12);
               }
            }
            case "shawarma" -> {
               player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
               if (player.getRandom().nextFloat() < 0.12F) {
                  player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
                  player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                  player.displayClientMessage(Component.translatable("message.vaz2109.shawarma.bad").withStyle(ChatFormatting.DARK_GREEN), true);
               }
            }
            case "instant_noodles" -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            default -> {
            }
         }
      }

      return super.finishUsingItem(stack, level, entity);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109." + this.id).withStyle(ChatFormatting.GRAY));
   }
}
