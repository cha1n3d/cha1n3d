package com.bobux.vaz2109.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "vaz2109"
)
public class HarutaSwordItem extends SwordItem {
   public static final int MAX_MIRACLES = 2;
   /** Kills of hostile mobs or players per miracle; a boss is one miracle by itself. */
   public static final int KILLS_PER_MIRACLE = 5;
   private static final int DASH_COOLDOWN = 160;
   private static final float DASH_DAMAGE = 7.0F;
   private static final double DASH = 7.0;
   /** Two miracles never come back to back: at least a minute between them. */
   private static final long MIRACLE_GAP = 1200L;

   public HarutaSwordItem(Properties properties) {
      super(Tiers.NETHERITE, 3, -2.6F, properties);
   }

   public static int miracles(ItemStack stack) {
      return stack.getTag() == null ? 0 : Math.min(stack.getTag().getInt("Miracles"), MAX_MIRACLES);
   }

   public static int kills(ItemStack stack) {
      return stack.getTag() == null ? 0 : stack.getTag().getInt("Kills");
   }

   private static void setMiracles(ItemStack stack, int n) {
      stack.getOrCreateTag().putInt("Miracles", Mth.clamp(n, 0, MAX_MIRACLES));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      player.swing(hand);
      if (level instanceof ServerLevel server) {
         Vec3 look = player.getLookAngle();
         Vec3 dir = new Vec3(look.x, 0.0, look.z).normalize();
         Vec3 start = player.position().add(0.0, 0.9, 0.0);
         Vec3 end = start.add(dir.scale(7.0));
         Set<LivingEntity> hit = new HashSet<>();

         for (LivingEntity e : server.getEntitiesOfClass(
            LivingEntity.class, new AABB(start, end).inflate(1.5), ex -> ex != player && ex.isAlive() && !player.isAlliedTo(ex)
         )) {
            Vec3 c = e.position().add(0.0, (double)(e.getBbHeight() / 2.0F), 0.0);
            double t = Mth.clamp(c.subtract(start).dot(dir) / 7.0, 0.0, 1.0);
            if (c.distanceTo(start.add(dir.scale(t * 7.0))) < 1.6 && hit.add(e) && e.hurt(player.damageSources().playerAttack(player), DASH_DAMAGE)) {
               e.knockback(0.6, -dir.x, -dir.z);
               server.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0.0);
            }
         }

         for (double d = 0.0; d <= 7.0; d += 0.5) {
            Vec3 p = start.add(dir.scale(d));
            server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.15, 0.3, 0.15, 0.0);
         }

         server.sendParticles(ParticleTypes.SWEEP_ATTACK, end.x, end.y, end.z, 3, 0.6, 0.3, 0.6, 0.0);
         player.setDeltaMovement(dir.x * 2.2, 0.2, dir.z * 2.2);
         player.hurtMarked = true;
         player.fallDistance = 0.0F;
         server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
         server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 1.0F, 1.4F);
         stack.hurtAndBreak(2, player, px -> px.broadcastBreakEvent(hand));
      }

      player.getCooldowns().addCooldown(this, DASH_COOLDOWN);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public boolean isFoil(ItemStack stack) {
      return miracles(stack) > 0;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.vaz2109.haruta_sword").withStyle(ChatFormatting.GOLD));
      tooltip.add(Component.translatable("tooltip.vaz2109.haruta_sword.miracles", new Object[]{miracles(stack), MAX_MIRACLES}).withStyle(ChatFormatting.YELLOW));
      if (miracles(stack) < MAX_MIRACLES) {
         tooltip.add(Component.translatable("tooltip.vaz2109.haruta_sword.kills", new Object[]{kills(stack), KILLS_PER_MIRACLE}).withStyle(ChatFormatting.DARK_GRAY));
      }

      tooltip.add(Component.translatable("tooltip.vaz2109.haruta_sword.desc").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.vaz2109.haruta_sword.use").withStyle(ChatFormatting.GRAY));
   }

   @SubscribeEvent
   public static void onKill(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide && event.getEntity() != player) {
         ItemStack stack = player.getMainHandItem();
         LivingEntity dead = event.getEntity();
         if (stack.getItem() instanceof HarutaSwordItem && miracles(stack) < MAX_MIRACLES && (dead instanceof Enemy || dead instanceof Player)) {
            int kills = dead.getType().is(Tags.EntityTypes.BOSSES) ? KILLS_PER_MIRACLE : kills(stack) + 1;
            if (kills >= KILLS_PER_MIRACLE) {
               stack.getOrCreateTag().putInt("Kills", 0);
               setMiracles(stack, miracles(stack) + 1);
               player.displayClientMessage(
                  Component.translatable("message.vaz2109.haruta_sword.stored", new Object[]{miracles(stack), MAX_MIRACLES}).withStyle(ChatFormatting.YELLOW), true
               );
            } else {
               stack.getOrCreateTag().putInt("Kills", kills);
               player.displayClientMessage(
                  Component.translatable("message.vaz2109.haruta_sword.progress", new Object[]{kills, KILLS_PER_MIRACLE}).withStyle(ChatFormatting.GRAY), true
               );
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof Player player && !player.level().isClientSide && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && player.level().getGameTime() >= player.getPersistentData().getLong("vaz2109:haruta_miracle") + MIRACLE_GAP) {
         for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof HarutaSwordItem && miracles(stack) > 0) {
               miracle(player, stack);
               event.setCanceled(true);
               return;
            }
         }

         ItemStack off = player.getOffhandItem();
         if (off.getItem() instanceof HarutaSwordItem && miracles(off) > 0) {
            miracle(player, off);
            event.setCanceled(true);
         }

         return;
      }
   }

   private static void miracle(Player player, ItemStack stack) {
      setMiracles(stack, miracles(stack) - 1);
      player.getPersistentData().putLong("vaz2109:haruta_miracle", player.level().getGameTime());
      player.setHealth(4.0F);
      player.clearFire();
      player.removeEffect(MobEffects.WITHER);
      player.removeEffect(MobEffects.POISON);
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
      // a miracle spends the sword too: no dash for a while
      player.getCooldowns().addCooldown(stack.getItem(), 600);
      if (player.level() instanceof ServerLevel server) {
         server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 0.8, 0.5, 0.45);
         server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
      }

      player.displayClientMessage(
         Component.translatable("message.vaz2109.haruta_sword.miracle", new Object[]{miracles(stack), MAX_MIRACLES}).withStyle(ChatFormatting.GOLD), true
      );
   }
}
