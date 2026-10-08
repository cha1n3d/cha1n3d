package com.bobux.vaz2109.weapon;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.RocketEntity;
import com.bobux.vaz2109.entity.VazEntity;
import com.bobux.vaz2109.fx.ModParticles;
import com.bobux.vaz2109.item.ChainsawItem;
import com.bobux.vaz2109.item.GunItem;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.TracerS2C;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.Tags.Blocks;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class Weapons {
   public static final float CROUCH_SPREAD = 0.6F;
   public static final float AIM_SPREAD = 0.45F;
   private static final Map<UUID, Weapons.Trigger> TRIGGERS = new HashMap<>();

   private Weapons() {
   }

   public static void setTrigger(ServerPlayer player, boolean held) {
      Weapons.Trigger t = TRIGGERS.computeIfAbsent(player.getUUID(), id -> new Weapons.Trigger());
      boolean pressed = held && !t.held;
      t.held = held;
      t.sawTicks = 0L;
      ItemStack stack = player.getMainHandItem();
      if (pressed && stack.getItem() instanceof GunItem gun) {
         tryFire(player, stack, gun, t);
      }
   }

   public static void setAiming(ServerPlayer player, boolean aiming) {
      TRIGGERS.computeIfAbsent(player.getUUID(), id -> new Weapons.Trigger()).aiming = aiming;
   }

   private static boolean aiming(Player player) {
      Weapons.Trigger t = TRIGGERS.get(player.getUUID());
      return t != null && t.aiming;
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         Weapons.Trigger t = TRIGGERS.get(player.getUUID());
         if (t != null && t.held) {
            ItemStack stack = player.getMainHandItem();
            if (!player.isSpectator() && !player.isDeadOrDying()) {
               if (stack.getItem() instanceof GunItem gun) {
                  if (automatic(gun.type, Mask.worn(player))) {
                     tryFire(player, stack, gun, t);
                  }
               } else if (stack.getItem() instanceof ChainsawItem) {
                  saw(player, stack, t);
               } else {
                  t.held = false;
               }
            } else {
               t.held = false;
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      TRIGGERS.remove(event.getEntity().getUUID());
   }

   public static boolean automatic(GunType type, Mask mask) {
      return type.automatic || type == GunType.PM && mask == Mask.RICHTER;
   }

   private static boolean silenced(GunType type, Mask mask) {
      return mask == Mask.PETER || type == GunType.PM && mask == Mask.RICHTER;
   }

   private static void tryFire(ServerPlayer player, ItemStack stack, GunItem gun, Weapons.Trigger t) {
      long now = player.level().getGameTime();
      Mask mask = Mask.worn(player);
      int delay = gun.type == GunType.PM && mask == Mask.RICHTER ? 3 : gun.type.fireDelay;
      if (now - t.lastShot.getOrDefault(gun.type, -1000L) >= (long)delay && !player.getCooldowns().isOnCooldown(gun)) {
         t.lastShot.put(gun.type, now);
         int ammo = GunItem.getAmmo(stack);
         if (ammo <= 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 2.0F);
            player.displayClientMessage(Component.translatable("message.vaz2109.empty").withStyle(ChatFormatting.RED), true);
            t.held = false;
         } else {
            if (mask != Mask.RAMI || player.getRandom().nextFloat() >= 0.25F) {
               GunItem.setAmmo(stack, ammo - 1);
            }

            fire(player, gun.type, false, mask);
            if (mask == Mask.MARK) {
               fireOffhand(player, t, now);
            }
         }
      }
   }

   private static void fireOffhand(ServerPlayer player, Weapons.Trigger t, long now) {
      ItemStack off = player.getOffhandItem();
      if (off.getItem() instanceof GunItem gun
         && !player.getCooldowns().isOnCooldown(gun)
         && now - t.lastShotOff.getOrDefault(gun.type, -1000L) >= (long)gun.type.fireDelay) {
         int ammo = GunItem.getAmmo(off);
         if (ammo <= 0) {
            return;
         }

         t.lastShotOff.put(gun.type, now);
         GunItem.setAmmo(off, ammo - 1);
         fire(player, gun.type, true, Mask.MARK);
         return;
      }
   }

   private static void fire(ServerPlayer player, GunType type, boolean offhand, Mask mask) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
      Vec3 muzzle = eye.add(look.scale(0.9)).add(right.scale(offhand ? -0.22 : 0.22)).add(0.0, -0.12, 0.0);
      if (type.pellets == 0) {
         RocketEntity rocket = new RocketEntity(level, player);
         rocket.setPos(eye.x + look.x * 0.8, eye.y - 0.1 + look.y * 0.8, eye.z + look.z * 0.8);
         rocket.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.6F, type.spread);
         level.addFreshEntity(rocket);
         level.sendParticles((SimpleParticleType)ModParticles.SMOKE.get(), eye.x - look.x * 1.5, eye.y - 0.1, eye.z - look.z * 1.5, 14, 0.4, 0.3, 0.4, 0.05);
         level.sendParticles((SimpleParticleType)ModParticles.DARK_SMOKE.get(), eye.x - look.x * 2.0, eye.y - 0.1, eye.z - look.z * 2.0, 2, 0.3, 0.2, 0.3, 0.02);
         level.sendParticles((SimpleParticleType)ModParticles.FIREBALL.get(), eye.x - look.x * 1.2, eye.y - 0.1, eye.z - look.z * 1.2, 1, 0.1, 0.1, 0.1, 0.0);
         sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0F, 0.5F);
         sound(player, SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
         broadcastTracer(player, type, muzzle, muzzle, (byte)0, null, BlockPos.ZERO);
      } else {
         float spread = mask == Mask.RICK ? type.spread * 0.4F : type.spread;
         if (aiming(player)) {
            spread *= type == GunType.SVD ? 0.0F : 0.45F;
         }

         if (player.isCrouching()) {
            spread *= 0.6F;
         }

         for (int i = 0; i < type.pellets; i++) {
            Vec3 dir = spread(look, spread, player);
            Vec3 end = eye.add(dir.scale((double)type.range));
            BlockHitResult block = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
            Vec3 stop = block.getType() == Type.MISS ? end : block.getLocation();
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
               level,
               player,
               eye,
               stop,
               new AABB(eye, stop).inflate(1.0),
               e -> !e.isSpectator() && e.isPickable() && e != player.getVehicle() && e.getRootVehicle() != player
            );
            if (hit != null) {
               Entity target = hit.getEntity();
               Vec3 at = target.getBoundingBox().inflate(0.3).clip(eye, stop).orElse(target.position());
               hurt(player, target, type, at, dir);
               broadcastTracer(player, type, muzzle, at, (byte)2, null, BlockPos.ZERO);
            } else if (block.getType() != Type.MISS) {
               boolean shattered = impact(level, player, block);
               broadcastTracer(player, type, muzzle, stop, (byte)(shattered ? 0 : 1), block.getDirection(), block.getBlockPos());
            } else {
               broadcastTracer(player, type, muzzle, stop, (byte)0, null, BlockPos.ZERO);
            }
         }

         if (silenced(type, mask)) {
            sound(player, SoundEvents.ARROW_SHOOT, 0.5F, 1.9F);
            sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 0.15F, 2.0F);
         } else {
            switch (type) {
               case PM:
                  sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, 1.5F);
                  sound(player, SoundEvents.GENERIC_EXPLODE, 0.25F, 2.0F);
                  break;
               case AK74:
                  sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 1.15F);
                  sound(player, SoundEvents.GENERIC_EXPLODE, 0.3F, 1.8F);
                  break;
               case SVD:
                  sound(player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 3.0F, 0.85F);
                  sound(player, SoundEvents.GENERIC_EXPLODE, 0.6F, 1.5F);
                  break;
               default:
                  sound(player, SoundEvents.GENERIC_EXPLODE, 0.9F, 1.4F);
                  sound(player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.0F, 0.9F);
            }
         }
      }
   }

   private static Vec3 spread(Vec3 look, float degrees, Player player) {
      if (degrees <= 0.0F) {
         return look;
      } else {
         float extra = player.onGround() ? (player.getDeltaMovement().horizontalDistanceSqr() > 0.01 ? 1.5F : 1.0F) : 2.0F;
         double r = Math.toRadians((double)(degrees * extra));
         double yaw = (player.getRandom().nextDouble() - 0.5) * 2.0 * r;
         double pitch = (player.getRandom().nextDouble() - 0.5) * 2.0 * r;
         Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         }

         right = right.normalize();
         Vec3 up = right.cross(look).normalize();
         return look.add(right.scale(Math.tan(yaw))).add(up.scale(Math.tan(pitch))).normalize();
      }
   }

   private static void hurt(ServerPlayer player, Entity target, GunType type, Vec3 at, Vec3 dir) {
      float damage = type.damage;
      if (target instanceof LivingEntity living && at.y > living.getEyeY() - 0.22) {
         damage *= 1.6F;
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.4F, 1.6F);
      }

      if (target instanceof VazEntity) {
         damage *= 0.3F;
      }

      DamageSource source = new DamageSource(
         player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModRegistry.BULLET_DAMAGE), player, player
      );
      target.invulnerableTime = 0;
      if (target.hurt(source, damage) && target instanceof LivingEntity) {
         ServerLevel level = player.serverLevel();

         for (int i = 0; i < 8; i++) {
            level.sendParticles(
               (SimpleParticleType)ModParticles.BLOOD.get(),
               at.x,
               at.y,
               at.z,
               0,
               dir.x * 0.25 + (player.getRandom().nextDouble() - 0.5) * 0.2,
               0.08 + player.getRandom().nextDouble() * 0.12,
               dir.z * 0.25 + (player.getRandom().nextDouble() - 0.5) * 0.2,
               1.0
            );
         }
      }
   }

   private static boolean impact(ServerLevel level, ServerPlayer player, BlockHitResult hit) {
      BlockPos pos = hit.getBlockPos();
      BlockState state = level.getBlockState(pos);
      Vec3 at = hit.getLocation();
      if ((state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANES)) && player.mayBuild()) {
         level.destroyBlock(pos, false, player);
         return true;
      } else {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 6, 0.05, 0.05, 0.05, 0.1);
         level.playSound(null, pos, state.getSoundType(level, pos, player).getHitSound(), SoundSource.BLOCKS, 0.7F, 1.4F);
         return false;
      }
   }

   private static void broadcastTracer(ServerPlayer player, GunType type, Vec3 from, Vec3 to, byte hit, Direction face, BlockPos pos) {
      Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new TracerS2C(player.getId(), type, from, to, hit, face, pos));
   }

   private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
      player.level()
         .playSound(
            null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch * (0.95F + player.getRandom().nextFloat() * 0.1F)
         );
   }

   public static void reload(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() instanceof GunItem gun && !player.getCooldowns().isOnCooldown(gun)) {
         ItemStack off = player.getOffhandItem();
         boolean offReloaded = Mask.worn(player) == Mask.MARK && off.getItem() instanceof GunItem offGun && refill(player, off, offGun);
         if (!refill(player, stack, gun) && !offReloaded) {
            return;
         }

         player.getCooldowns().addCooldown(gun, gun.type.reloadTicks);
         TRIGGERS.computeIfAbsent(player.getUUID(), id -> new Weapons.Trigger()).held = false;
         sound(player, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1.0F, 1.2F);
         sound(player, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.3F);
         return;
      }
   }

   private static boolean refill(ServerPlayer player, ItemStack stack, GunItem gun) {
      int ammo = GunItem.getAmmo(stack);
      int need = gun.type.capacity - ammo;
      if (need <= 0) {
         return false;
      } else {
         int got = player.getAbilities().instabuild ? need : take(player, gun.ammoItem(), need);
         if (got <= 0) {
            player.displayClientMessage(
               Component.translatable("message.vaz2109.no_ammo", new Object[]{gun.ammoItem().getDescription()}).withStyle(ChatFormatting.RED), true
            );
            return false;
         } else {
            GunItem.setAmmo(stack, ammo + got);
            return true;
         }
      }
   }

   private static int take(Player player, Item item, int max) {
      int got = 0;

      for (ItemStack s : player.getInventory().items) {
         if (got >= max) {
            break;
         }

         if (s.is(item)) {
            int n = Math.min(s.getCount(), max - got);
            s.shrink(n);
            got += n;
         }
      }

      return got;
   }

   private static void saw(ServerPlayer player, ItemStack stack, Weapons.Trigger t) {
      ServerLevel level = player.serverLevel();
      long tick = t.sawTicks++;
      if (tick % 8L == 0L) {
         level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 0.9F, 0.55F);
      }

      if (tick % 4L == 0L) {
         Vec3 eye = player.getEyePosition();
         Vec3 look = player.getLookAngle();
         Vec3 end = eye.add(look.scale(4.0));
         BlockHitResult block = level.clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, player));
         Vec3 stop = block.getType() == Type.MISS ? end : block.getLocation();
         EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            level,
            player,
            eye,
            stop,
            new AABB(eye, stop).inflate(1.0),
            e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity && e.getRootVehicle() != player
         );
         if (hit != null) {
            Entity target = hit.getEntity();
            target.invulnerableTime = 0;
            if (target.hurt(player.damageSources().playerAttack(player), 5.0F)) {
               Vec3 at = target.getBoundingBox().getCenter();
               level.sendParticles((SimpleParticleType)ModParticles.BLOOD.get(), at.x, at.y, at.z, 14, 0.25, 0.3, 0.25, 0.15);
               level.sendParticles(
                  new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()),
                  at.x,
                  at.y,
                  at.z,
                  8,
                  0.2,
                  0.2,
                  0.2,
                  0.15
               );
               level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8F, 0.6F);
               stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            }
         } else {
            if (block.getType() != Type.MISS) {
               BlockPos pos = block.getBlockPos();
               BlockState state = level.getBlockState(pos);
               if (ChainsawItem.cuts(state) && player.gameMode.destroyBlock(pos)) {
                  Vec3 at = block.getLocation();
                  level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.1);
               }
            }
         }
      }
   }

   private static final class Trigger {
      boolean held;
      boolean aiming;
      final Map<GunType, Long> lastShot = new EnumMap<>(GunType.class);
      final Map<GunType, Long> lastShotOff = new EnumMap<>(GunType.class);
      long sawTicks;
   }
}
