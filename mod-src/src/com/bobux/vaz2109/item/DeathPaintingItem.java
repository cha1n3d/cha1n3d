package com.bobux.vaz2109.item;

import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.BloodArts;
import com.bobux.vaz2109.role.Role;
import com.bobux.vaz2109.role.Roles;
import com.bobux.vaz2109.role.Trial;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

/** Choso's Death Painting womb. Only Sukuna's vessel can swallow it and take in the blood of the Kamo line. */
@EventBusSubscriber(modid = "vaz2109")
public class DeathPaintingItem extends Item {
   private static final UUID HEALTH_ID = UUID.fromString("9b1e7d44-6a2f-4c3e-8d5b-1f0a2c7e9d31");

   public DeathPaintingItem(Properties properties) {
      super(properties);
   }

   public static boolean eaten(Player player) {
      return Trial.PAINTING.done(player);
   }

   public int getUseDuration(ItemStack stack) {
      return 48;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.EAT;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (eaten(player)) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.vaz2109.painting.already").withStyle(ChatFormatting.DARK_RED), true);
         }

         return InteractionResultHolder.fail(stack);
      } else {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(stack);
      }
   }

   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      if (entity instanceof ServerPlayer player && level instanceof ServerLevel server) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.4F);
         BloodArts.splash(server, player.position().add(0.0, 1.2, 0.0), player.getLookAngle(), 20);
         if (!Roles.is(player, Role.VESSEL)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6F, 0.6F);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().magic(), 12.0F);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 2));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
            player.sendSystemMessage(Component.translatable("message.vaz2109.painting.reject").withStyle(ChatFormatting.DARK_RED));
         } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 0.8F);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().magic(), 4.0F);
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            Trial.PAINTING.add(player, 1);
            apply(player);
            player.setHealth(player.getMaxHealth());
            player.sendSystemMessage(Component.translatable("message.vaz2109.painting.eaten").withStyle(ChatFormatting.DARK_RED));
            if (!Ability.PIERCING_BLOOD.unlocked(player)) {
               player.sendSystemMessage(
                  Component.translatable("message.vaz2109.ability.locked", new Object[]{
                     Component.translatable("ability.vaz2109.piercing_blood"), Ability.PIERCING_BLOOD.requirement()
                  }).withStyle(ChatFormatting.GRAY)
               );
            }
         }

         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return stack;
   }

   /** +4 hearts while the blood of the painting runs in a vessel. */
   public static void apply(Player player) {
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         health.removeModifier(HEALTH_ID);
         if (eaten(player) && Roles.is(player, Role.VESSEL)) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_ID, "Death Painting", 8.0, AttributeModifier.Operation.ADDITION));
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      apply(event.getEntity());
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onRespawn(PlayerRespawnEvent event) {
      apply(event.getEntity());
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.death_painting").withStyle(ChatFormatting.DARK_RED));
      tooltip.add(Component.translatable("tooltip.vaz2109.death_painting.1").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.death_painting.2").withStyle(ChatFormatting.GRAY));
   }
}
