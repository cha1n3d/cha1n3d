package com.bobux.vaz2109.cyber;

import com.bobux.vaz2109.network.CyberS2C;
import com.bobux.vaz2109.network.Network;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Cyberware and cyberpsychosis. Humanity = 100 - what the implants take - strain (each Sandevistan run adds strain,
 * it wears off by one a minute). Under 40 humanity cyberpsychosis episodes come, more often the lower it goes:
 * a red rage (stronger, faster, glowing, nauseous) that nearby players are warned about. Neuroblockers end an
 * episode, take strain off and keep the next one away for a while.
 */
@EventBusSubscriber(modid = "vaz2109")
public final class Cyberware {
   public static final int SANDE_TICKS = 120;
   public static final int SANDE_COOLDOWN = 1800;
   public static final float SANDE_STRAIN = 6.0F;
   public static final double SANDE_RADIUS = 24.0;
   public static final int HEART_COOLDOWN = 24000;
   public static final int PSYCHOSIS = 40;
   public static final int SEVERE = 20;
   private static final String KEY = "vaz2109_cyber";
   /** Strain above which a Sandevistan run makes the nose bleed. */
   public static final float OVERUSE = 30.0F;
   private static final double SLOWMO = 0.2;
   private static final String SLOWED = "vaz2109Slowed";
   private static final String FROZEN = "vaz2109Frozen";
   private static final java.util.Set<Projectile> FROZEN_PROJECTILES = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
   private static final UUID ARMOR_ID = UUID.fromString("0c9b4a71-5e2d-4f13-a7b0-3d61e8f2c904");
   private static final UUID TOUGH_ID = UUID.fromString("7e3a19c5-b8d4-4a62-9f07-c1d5e2a8b316");

   private Cyberware() {
   }

   // ---- state -----------------------------------------------------------------------------------

   private static CompoundTag data(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted").getCompound(KEY);
   }

   private static void save(Player player, CompoundTag tag) {
      CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
      persisted.put(KEY, tag);
      player.getPersistentData().put("PlayerPersisted", persisted);
   }

   public static int installed(Player player) {
      return data(player).getInt("mask");
   }

   public static boolean has(Player player, Implant implant) {
      return (installed(player) & implant.bit()) != 0;
   }

   public static float strain(Player player) {
      CompoundTag d = data(player);
      long elapsed = player.level().getGameTime() - d.getLong("strainAt");
      return Math.max(0.0F, d.getFloat("strain") - (float)elapsed / 1200.0F);
   }

   private static void addStrain(Player player, float amount) {
      CompoundTag d = data(player);
      d.putFloat("strain", Math.max(0.0F, strain(player) + amount));
      d.putLong("strainAt", player.level().getGameTime());
      save(player, d);
   }

   public static int humanity(Player player) {
      int mask = installed(player);
      int cost = 0;

      for (Implant i : Implant.values()) {
         if ((mask & i.bit()) != 0) {
            cost += i.cost;
         }
      }

      return Math.max(-50, Math.round(100.0F - (float)cost - strain(player)));
   }

   public static boolean sandevistan(Player player) {
      return player.level().getGameTime() < data(player).getLong("sandeEnd");
   }

   public static boolean episode(Player player) {
      return player.level().getGameTime() < data(player).getLong("episodeEnd");
   }

   public static void sync(ServerPlayer player) {
      CompoundTag d = data(player);
      long now = player.level().getGameTime();
      Network.CHANNEL.send(
         PacketDistributor.PLAYER.with(() -> player),
         new CyberS2C(
            installed(player),
            humanity(player),
            (int)Math.max(0L, d.getLong("sandeEnd") - now),
            (int)Math.max(0L, d.getLong("sandeReady") - now),
            (int)Math.max(0L, d.getLong("episodeEnd") - now)
         )
      );
   }

   // ---- installing ------------------------------------------------------------------------------

   public static boolean install(ServerPlayer player, Implant implant) {
      int mask = installed(player);
      if ((mask & implant.bit()) != 0) {
         player.displayClientMessage(Component.translatable("message.vaz2109.cyber.already").withStyle(ChatFormatting.GRAY), true);
         return false;
      }

      Implant old = Implant.inSlot(mask, implant.slot);
      if (old != null) {
         mask &= ~old.bit();
         ItemStack back = new ItemStack(CyberItems.item(old));
         if (!player.getInventory().add(back)) {
            player.drop(back, false);
         }
      }

      CompoundTag d = data(player);
      d.putInt("mask", mask | implant.bit());
      if (!d.contains("nextEpisode")) {
         d.putLong("nextEpisode", player.level().getGameTime() + 2400L);
      }

      save(player, d);
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().magic(), 4.0F);
      player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.8F);
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.5F);
      apply(player);
      int humanity = humanity(player);
      player.sendSystemMessage(
         Component.translatable("message.vaz2109.cyber.installed", new Object[]{Component.translatable("item.vaz2109." + implant.id), humanity})
            .withStyle(humanity < PSYCHOSIS ? ChatFormatting.RED : ChatFormatting.AQUA)
      );
      if (humanity < PSYCHOSIS) {
         player.sendSystemMessage(Component.translatable("message.vaz2109.cyber.warning").withStyle(ChatFormatting.DARK_RED));
      }

      sync(player);
      return true;
   }

   /** Attribute bonuses that must survive respawns and logins. */
   public static void apply(Player player) {
      boolean skin = has(player, Implant.SUBDERMAL_ARMOR);
      modifier(player.getAttribute(Attributes.ARMOR), ARMOR_ID, "Subdermal armor", skin ? 3.0 : 0.0);
      modifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGH_ID, "Subdermal armor", skin ? 1.0 : 0.0);
   }

   private static void modifier(AttributeInstance attribute, UUID id, String name, double amount) {
      if (attribute != null) {
         attribute.removeModifier(id);
         if (amount > 0.0) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, Operation.ADDITION));
         }
      }
   }

   // ---- Sandevistan -----------------------------------------------------------------------------

   /** The implant key. */
   public static void activate(ServerPlayer player) {
      if (!has(player, Implant.SANDEVISTAN)) {
         player.displayClientMessage(Component.translatable("message.vaz2109.cyber.no_active").withStyle(ChatFormatting.GRAY), true);
         return;
      }

      CompoundTag d = data(player);
      long now = player.level().getGameTime();
      if (now < d.getLong("sandeReady")) {
         player.displayClientMessage(
            Component.translatable("message.vaz2109.cyber.cooldown", new Object[]{(d.getLong("sandeReady") - now + 19L) / 20L}).withStyle(ChatFormatting.GRAY), true
         );
         return;
      }

      d.putLong("sandeEnd", now + (long)SANDE_TICKS);
      d.putLong("sandeReady", now + (long)SANDE_TICKS + (long)SANDE_COOLDOWN);
      save(player, d);
      addStrain(player, SANDE_STRAIN);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SANDE_TICKS, 3, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, SANDE_TICKS, 2, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.JUMP, SANDE_TICKS, 1, false, false));
      ServerLevel level = player.serverLevel();
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.9F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 0.6F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.6F);
      for (ServerPlayer p : level.players()) {
         if (p.distanceTo(player) < 96.0) {
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new com.bobux.vaz2109.network.SandeS2C(player.getId(), SANDE_TICKS));
         }
      }

      // overuse: past a point the body pays for it
      if (strain(player) > OVERUSE) {
         player.hurt(player.damageSources().magic(), 3.0F);
         player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
         player.displayClientMessage(Component.translatable("message.vaz2109.cyber.nosebleed").withStyle(ChatFormatting.DARK_RED), true);
      }

      sync(player);
   }

   private static void sandeTick(ServerPlayer player, ServerLevel level, long now, long end) {
      // everything around runs at a fraction of normal speed: mobs crawl, falls hang in the air
      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SANDE_RADIUS), e -> e != player && e.isAlive())) {
         e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, e instanceof Player ? 3 : 6, false, false));
         e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6, 3, false, false));
         e.getPersistentData().putLong(SLOWED, now + 2L);
         if (!(e instanceof Player)) {
            e.setDeltaMovement(e.getDeltaMovement().scale(SLOWMO));
            e.hurtMarked = true;
         }
      }

      // bullets and arrows nearly stop in the air and fly on at full speed once time runs again
      for (Projectile p : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SANDE_RADIUS), p -> p.getOwner() != player)) {
         CompoundTag tag = p.getPersistentData();
         if (!tag.contains(FROZEN)) {
            Vec3 v = p.getDeltaMovement();
            tag.putDouble(FROZEN + "X", v.x);
            tag.putDouble(FROZEN + "Y", v.y);
            tag.putDouble(FROZEN + "Z", v.z);
            FROZEN_PROJECTILES.add(p);
         }

         tag.putLong(FROZEN, now + 2L);
         Vec3 v = new Vec3(tag.getDouble(FROZEN + "X"), tag.getDouble(FROZEN + "Y"), tag.getDouble(FROZEN + "Z"));
         p.setDeltaMovement(v.scale(0.04));
         p.hurtMarked = true;
      }

      if (strain(player) > OVERUSE && now % 40L == 0L) {
         player.hurt(player.damageSources().magic(), 1.0F);
         Vec3 nose = player.getEyePosition().add(player.getLookAngle().scale(0.3)).add(0.0, -0.2, 0.0);
         level.sendParticles(new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.02F), 1.0F), nose.x, nose.y, nose.z, 6, 0.05, 0.1, 0.05, 0.0);
      }

      // the afterimage: a trail of colour that shifts through the hues
      float hue = (float)(now % 40L) / 40.0F;
      int rgb = Mth.hsvToRgb(hue, 0.85F, 1.0F);
      DustParticleOptions dust = new DustParticleOptions(new Vector3f((float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F), 1.4F);
      for (double y = 0.2; y < 1.9; y += 0.35) {
         level.sendParticles(dust, player.xo, player.yo + y, player.zo, 2, 0.15, 0.05, 0.15, 0.0);
      }

      if (now == end - 1L) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
         sync(player);
      }
   }

   /** Projectiles stopped by a Sandevistan get their speed back when time runs again. */
   @SubscribeEvent
   public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
      if (event.phase != Phase.END || FROZEN_PROJECTILES.isEmpty()) {
         return;
      }

      FROZEN_PROJECTILES.removeIf(p -> {
         if (p.isRemoved()) {
            return true;
         }

         CompoundTag tag = p.getPersistentData();
         if (p.level().getGameTime() < tag.getLong(FROZEN)) {
            return false;
         }

         p.setDeltaMovement(tag.getDouble(FROZEN + "X"), tag.getDouble(FROZEN + "Y"), tag.getDouble(FROZEN + "Z"));
         p.hurtMarked = true;
         tag.remove(FROZEN);
         return true;
      });
   }

   /** Someone in slow motion can't keep up with the Sandevistan's owner: their hits miss. */
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onSandeHit(LivingAttackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && sandevistan(player) && event.getSource().getEntity() instanceof LivingEntity attacker
         && attacker != player && attacker.level().getGameTime() < attacker.getPersistentData().getLong(SLOWED) && player.getRandom().nextFloat() < 0.7F) {
         event.setCanceled(true);
      }
   }

   // ---- cyberpsychosis --------------------------------------------------------------------------

   public static void calm(ServerPlayer player) {
      CompoundTag d = data(player);
      d.putLong("episodeEnd", 0L);
      d.putLong("nextEpisode", player.level().getGameTime() + 3600L);
      save(player, d);
      addStrain(player, -25.0F);
      player.removeEffect(MobEffects.CONFUSION);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
      player.displayClientMessage(Component.translatable("message.vaz2109.cyber.calm").withStyle(ChatFormatting.AQUA), true);
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.4F);
      sync(player);
   }

   private static void episodeStart(ServerPlayer player, ServerLevel level, int humanity, long now) {
      CompoundTag d = data(player);
      int length = humanity < SEVERE ? 400 : 200;
      long gap = humanity <= 0 ? 600L + level.random.nextInt(600) : (humanity < SEVERE ? 1200L + level.random.nextInt(1200) : 2400L + level.random.nextInt(2400));
      d.putLong("episodeEnd", now + (long)length);
      d.putLong("nextEpisode", now + (long)length + gap);
      save(player, d);
      int power = humanity < SEVERE ? 1 : 0;
      give(player, MobEffects.DAMAGE_BOOST, length, power);
      give(player, MobEffects.MOVEMENT_SPEED, length, power);
      give(player, MobEffects.CONFUSION, Math.min(length, 160), 0);
      give(player, MobEffects.GLOWING, length, 0);
      if (humanity <= 0) {
         give(player, MobEffects.WITHER, 100, 0);
      }

      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 1.4F);
      player.sendSystemMessage(Component.translatable("message.vaz2109.cyber.episode." + level.random.nextInt(4)).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
      for (Player p : level.players()) {
         if (p != player && p.distanceTo(player) < 48.0) {
            p.sendSystemMessage(Component.translatable("message.vaz2109.cyber.alert", new Object[]{player.getDisplayName()}).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
         }
      }

      sync(player);
   }

   private static void give(Player player, MobEffect effect, int ticks, int amplifier) {
      player.addEffect(new MobEffectInstance(effect, ticks, amplifier));
   }

   // ---- events ----------------------------------------------------------------------------------

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || !(event.player instanceof ServerPlayer player)) {
         return;
      }

      int mask = installed(player);
      if (mask == 0) {
         return;
      }

      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      CompoundTag d = data(player);
      long sandeEnd = d.getLong("sandeEnd");
      if (now < sandeEnd) {
         sandeTick(player, level, now, sandeEnd);
      }

      if (player.tickCount % 20 == 0) {
         if ((mask & Implant.KIROSHI_OPTICS.bit()) != 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
            if (player.tickCount % 40 == 0) {
               for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0), e -> e instanceof Enemy && e.isAlive())) {
                  e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false, false));
               }
            }
         }

         if ((mask & Implant.REINFORCED_TENDONS.bit()) != 0) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 1, true, false, false));
         }

         int humanity = humanity(player);
         if (humanity < PSYCHOSIS && now >= d.getLong("nextEpisode") && !episode(player)) {
            episodeStart(player, level, humanity, now);
         }

         if (player.tickCount % 100 == 0) {
            sync(player);
         }
      }
   }

   /** Kerenzikov: a reflex boost — sprinting or in the air, a hit can be slipped entirely. */
   @SubscribeEvent
   public static void onAttack(LivingAttackEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && has(player, Implant.KERENZIKOV) && (player.isSprinting() || !player.onGround())) {
         CompoundTag d = data(player);
         long now = player.level().getGameTime();
         if (now >= d.getLong("dodgeReady") && event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.25F) {
            d.putLong("dodgeReady", now + 100L);
            save(player, d);
            event.setCanceled(true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.6F);
            player.displayClientMessage(Component.translatable("message.vaz2109.cyber.dodge").withStyle(ChatFormatting.AQUA), true);
         }
      }
   }

   /** Mantis blades and gorilla arms hit with an empty hand. */
   @SubscribeEvent
   public static void onHurt(LivingHurtEvent event) {
      if (event.getSource().getDirectEntity() instanceof ServerPlayer player && player.getMainHandItem().isEmpty()) {
         LivingEntity target = event.getEntity();
         if (has(player, Implant.MANTIS_BLADES)) {
            event.setAmount(event.getAmount() + 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.5F);
         } else if (has(player, Implant.GORILLA_ARMS)) {
            event.setAmount(event.getAmount() + 3.0F);
            Vec3 push = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(push.x * 1.2, 0.35, push.z * 1.2));
            target.hurtMarked = true;
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS, 0.7F, 0.8F);
         }
      }
   }

   @SubscribeEvent
   public static void onBreakSpeed(BreakSpeed event) {
      if (event.getEntity().getMainHandItem().isEmpty() && has(event.getEntity(), Implant.GORILLA_ARMS)) {
         event.setNewSpeed(event.getNewSpeed() * 2.5F);
      }
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof Player player && has(player, Implant.REINFORCED_TENDONS)) {
         event.setDistance(Math.max(0.0F, event.getDistance() - 7.0F));
      }
   }

   /** Second heart: once every 10 minutes it starts beating when the first one stops. */
   @SubscribeEvent(priority = EventPriority.LOW)
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && has(player, Implant.SECOND_HEART) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         CompoundTag d = data(player);
         long now = player.level().getGameTime();
         if (now >= d.getLong("heartReady")) {
            d.putLong("heartReady", now + (long)HEART_COOLDOWN);
            save(player, d);
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth() * 0.3F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
            player.sendSystemMessage(Component.translatable("message.vaz2109.cyber.second_heart").withStyle(ChatFormatting.AQUA));
         }
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      apply(event.getEntity());
      if (event.getEntity() instanceof ServerPlayer sp) {
         sync(sp);
      }
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      apply(event.getEntity());
      if (event.getEntity() instanceof ServerPlayer sp) {
         sync(sp);
      }
   }
}
