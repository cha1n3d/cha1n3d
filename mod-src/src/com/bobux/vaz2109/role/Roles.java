package com.bobux.vaz2109.role;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.car.CarPart;
import com.bobux.vaz2109.entity.AllyFrog;
import com.bobux.vaz2109.entity.BloodBeamEntity;
import com.bobux.vaz2109.entity.TurretEntity;
import com.bobux.vaz2109.entity.VazEntity;
import com.bobux.vaz2109.item.GunItem;
import com.bobux.vaz2109.item.SlaughterDemonItem;
import com.bobux.vaz2109.item.VazItem;
import com.bobux.vaz2109.entity.curse.SukunaVessel;
import com.bobux.vaz2109.fx.ModSounds;
import com.bobux.vaz2109.network.CursedFxS2C;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.RoleS2C;
import com.bobux.vaz2109.station.Station;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Added;
import net.minecraftforge.event.entity.living.MobEffectEvent.Applicable;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class Roles {
   private static final String ROLE = "vaz2109_role";
   private static final String SELECTED = "vaz2109AbilitySel";
   private static final String READY = "vaz2109Cd_";
   private static final String CHARGE = "vaz2109ChargeStart";
   private static final String BLUE = "vaz2109BlueEnd";
   private static final String LEAP = "vaz2109FrogLeap";
   public static final int CHARGE_FULL = 40;
   public static final int CHARGE_MAX = 100;
   public static final int BLUE_TIME = 300;
   private static final UUID BELLY_ID = UUID.fromString("7c3e2a10-4b1d-4e7a-9a3f-0d2b6c8e1f55");
   private static final UUID SWIM_ID = UUID.fromString("3f6b9c2e-1d4a-4c8b-9e7f-5a2d8c1b6e44");
   private static final String BREATH = "vaz2109BreathEnd";
   public static final float BLACK_FLASH_CHANCE = 0.25F;
   public static final float BLACK_FLASH_ZONE_CHANCE = 0.45F;
   private static final Map<Player, Float> SWING = new WeakHashMap<>();
   private static final Map<UUID, BloodBeamEntity> BEAMS = new HashMap<>();
   public static final int SLASH_GAP = 4;
   private static final List<Roles.Slash> SLASHES = new ArrayList<>();
   private static final List<Roles.Impact> IMPACTS = new ArrayList<>();
   private static boolean applying;

   private Roles() {
   }

   @Nullable
   public static Role get(Player player) {
      CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
      return persisted.contains("vaz2109_role") ? Role.byName(persisted.getString("vaz2109_role")) : null;
   }

   public static boolean is(Player player, Role role) {
      return get(player) == role;
   }

   public static void set(ServerPlayer player, @Nullable Role role) {
      CompoundTag data = player.getPersistentData();
      CompoundTag persisted = data.getCompound("PlayerPersisted");
      if (role == null) {
         persisted.remove("vaz2109_role");
      } else {
         persisted.putString("vaz2109_role", role.id);
      }

      data.put("PlayerPersisted", persisted);
      data.remove("vaz2109AbilitySel");

      for (Ability a : Ability.values()) {
         data.remove("vaz2109Cd_" + a.id);
      }

      belly(player);
      sync(player);
   }

   public static int ready(Player player, Ability a) {
      return (int)Math.max(0L, player.getPersistentData().getLong("vaz2109Cd_" + a.id) - player.level().getGameTime());
   }

   @Nullable
   public static Ability selected(Player player) {
      Role role = get(player);
      if (role == null) {
         return null;
      } else {
         List<Ability> list = Ability.of(role);
         return list.get(Math.floorMod(player.getPersistentData().getInt("vaz2109AbilitySel"), list.size()));
      }
   }

   public static void sync(ServerPlayer player) {
      Role role = get(player);
      List<Ability> list = role == null ? List.of() : Ability.of(role);
      int mask = 0;
      int[] cd = new int[list.size()];

      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).unlocked(player)) {
            mask |= 1 << i;
         }

         cd[i] = ready(player, list.get(i));
      }

      long start = player.getPersistentData().getLong("vaz2109ChargeStart");
      int charging = start > 0L ? (int)(player.level().getGameTime() - start) : -1;
      int blue = (int)Math.max(0L, player.getPersistentData().getLong("vaz2109BlueEnd") - player.level().getGameTime());
      Network.CHANNEL
         .send(
            PacketDistributor.PLAYER.with(() -> player),
            new RoleS2C(
               role == null ? -1 : role.ordinal(),
               list.isEmpty() ? 0 : Math.floorMod(player.getPersistentData().getInt("vaz2109AbilitySel"), list.size()),
               mask,
               cd,
               SukunaVessel.possessionLeft(player),
               SukunaVessel.possessionTotal(player),
               SukunaVessel.fingers(player),
               charging,
               blue
            )
         );
   }

   public static boolean doubles(Player player, Station station) {
      Role role = get(player);
      return role != null && role.bench == station && player.getRandom().nextFloat() < 0.25F;
   }

   public static boolean doubles(Player player, Station station, ItemStack result) {
      return !(result.getItem() instanceof VazItem) && !(result.getItem() instanceof GunItem) && doubles(player, station);
   }

   /** Ability wheel: pick an ability of the player's class by its index, if it is unlocked. */
   public static void select(ServerPlayer player, int index) {
      Role role = get(player);
      if (role != null) {
         List<Ability> list = Ability.of(role);
         if (index >= 0 && index < list.size() && list.get(index).unlocked(player)) {
            player.getPersistentData().putInt("vaz2109AbilitySel", index);
            Ability a = list.get(index);
            player.displayClientMessage(Component.translatable("ability.vaz2109." + a.id).withStyle(s -> s.withColor(role.color)), true);
         }

         sync(player);
      }
   }

   public static void press(ServerPlayer player, boolean sneaking) {
      BloodBeamEntity active = BEAMS.get(player.getUUID());
      if (active != null && !active.isRemoved()) {
         release(player);
         return;
      }

      Role role = get(player);
      if (role != null && !player.isSpectator() && player.isAlive() && !SukunaVessel.possessed(player)) {
         List<Ability> list = Ability.of(role);
         if (!sneaking) {
            Ability a = selected(player);
            if (a != null) {
               if (!a.unlocked(player)) {
                  player.displayClientMessage(
                     Component.translatable("message.vaz2109.ability.locked", new Object[]{Component.translatable("ability.vaz2109." + a.id), a.requirement()})
                        .withStyle(ChatFormatting.GRAY),
                     true
                  );
               } else if (ready(player, a) <= 0) {
                  if (a.chargeable()) {
                     if (a == Ability.PIERCING_BLOOD && player.getHealth() <= 4.0F) {
                        player.displayClientMessage(Component.translatable("message.vaz2109.blood.too_weak").withStyle(ChatFormatting.DARK_RED), true);
                        return;
                     }

                     player.getPersistentData().putLong("vaz2109ChargeStart", player.level().getGameTime());
                     if (a == Ability.PIERCING_BLOOD) {
                        BloodBeamEntity beam = ModRegistry.BLOOD_BEAM.get().create(player.level());
                        if (beam != null) {
                           beam.setup(player, 16, 140, 1.0F, 4.0F, true, true);
                           player.level().addFreshEntity(beam);
                           BEAMS.put(player.getUUID(), beam);
                        }
                     } else {
                        SukunaVessel.charge(player, 40);
                     }

                     sync(player);
                  } else {
                     if (use(player, a, 1.0F)) {
                        player.getPersistentData().putLong("vaz2109Cd_" + a.id, player.level().getGameTime() + (long)a.cooldown);
                     }

                     sync(player);
                  }
               }
            }
         } else {
            int sel = player.getPersistentData().getInt("vaz2109AbilitySel");

            for (int i = 1; i <= list.size(); i++) {
               Ability a = list.get(Math.floorMod(sel + i, list.size()));
               if (a.unlocked(player)) {
                  player.getPersistentData().putInt("vaz2109AbilitySel", Math.floorMod(sel + i, list.size()));
                  player.displayClientMessage(Component.translatable("ability.vaz2109." + a.id).withStyle(s -> s.withColor(role.color)), true);
                  break;
               }
            }

            sync(player);
         }
      }
   }

   public static void release(ServerPlayer player) {
      long start = player.getPersistentData().getLong("vaz2109ChargeStart");
      if (start > 0L) {
         player.getPersistentData().remove("vaz2109ChargeStart");
         Ability a = selected(player);
         long held = player.level().getGameTime() - start;
         BloodBeamEntity beam = BEAMS.remove(player.getUUID());
         if (beam != null) {
            int fired = beam.firedTicks();
            beam.discard();
            player.getPersistentData()
               .putLong("vaz2109Cd_" + Ability.PIERCING_BLOOD.id, player.level().getGameTime() + (long)(Ability.PIERCING_BLOOD.cooldown * (0.3F + 0.7F * Math.min(1.0F, fired / 100.0F))));
            sync(player);
         } else if (a != null && a.chargeable() && a != Ability.PIERCING_BLOOD && !SukunaVessel.possessed(player)) {
            float power = Math.min(1.0F, Math.max(0.15F, (float)held / 40.0F));
            use(player, a, power);
            player.getPersistentData().putLong("vaz2109Cd_" + a.id, player.level().getGameTime() + (long)((float)a.cooldown * (0.4F + 0.6F * power)));
            sync(player);
         } else {
            SukunaVessel.cancelCharge(player);
            sync(player);
         }
      }
   }

   private static boolean use(ServerPlayer player, Ability a, float power) {
      ServerLevel level = player.serverLevel();
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      int n = SukunaVessel.fingers(player);
      switch (a) {
         case BLUE_FISTS:
            player.getPersistentData().putLong("vaz2109BlueEnd", level.getGameTime() + 300L);
            CursedFxS2C.send(level, 18, player, null, player.position(), player.position(), 300.0F);
            level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
            level.playSound(null, x, y, z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.5F, 1.2F);
            player.displayClientMessage(Component.translatable("message.vaz2109.ability.blue_fists").withStyle(ChatFormatting.BLUE), true);
            break;
         case DISMANTLE:
            int chain = slashes(n);

            for (int i = 0; i < chain; i++) {
               SLASHES.add(new Roles.Slash(player, level.getGameTime() + (long)(i * SLASH_GAP), 4.5F + 0.25F * (float)n));
            }

            break;
         case REPAIR:
            boolean fixed = false;

            for (VazEntity car : level.getEntitiesOfClass(VazEntity.class, player.getBoundingBox().inflate(6.0))) {
               car.setHealth(car.getHealth() + 10000.0F);
               level.sendParticles(ParticleTypes.WAX_OFF, car.getX(), car.getY() + 1.0, car.getZ(), 30, 1.2, 0.6, 1.2, 0.1);
               fixed = true;
            }

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               ItemStack stack = player.getItemBySlot(slot);
               if (stack.isDamageableItem() && stack.isDamaged()) {
                  stack.setDamageValue(Math.max(0, stack.getDamageValue() - stack.getMaxDamage() / 3));
                  fixed = true;
               }
            }

            if (!fixed) {
               player.displayClientMessage(Component.translatable("message.vaz2109.role.nothing_to_fix").withStyle(ChatFormatting.GRAY), true);
               return false;
            }

            level.playSound(null, x, y, z, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.displayClientMessage(Component.translatable("message.vaz2109.role.fixed").withStyle(ChatFormatting.GOLD), true);
            break;
         case SHOCK:
            LivingEntity target = lookTarget(player, 10.0);
            if (target == null) {
               return false;
            }

            target.invulnerableTime = 0;
            target.hurt(player.damageSources().lightningBolt(), 9.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            Vec3 from = player.getEyePosition().add(0.0, -0.3, 0.0);
            Vec3 to = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);

            for (int i = 0; i <= 16; i++) {
               Vec3 p = from.add(to.subtract(from).scale((double)i / 16.0));
               level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.05);
            }

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6F, 1.8F);
            break;
         case LEAP:
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 1.5, 0.95, look.z * 1.5);
            player.hurtMarked = true;
            player.getPersistentData().putLong("vaz2109FrogLeap", level.getGameTime());
            level.playSound(null, x, y, z, SoundEvents.FROG_LONG_JUMP, SoundSource.PLAYERS, 1.5F, 0.8F);
            break;
         case TONGUE:
            LivingEntity pulled = lookTarget(player, 14.0);
            if (pulled == null) {
               return false;
            }

            Vec3 pull = player.position().subtract(pulled.position()).normalize();
            pulled.setDeltaMovement(pull.x * 1.4, 0.35, pull.z * 1.4);
            pulled.hurtMarked = true;
            pulled.invulnerableTime = 0;
            pulled.hurt(player.damageSources().playerAttack(player), 6.0F);
            CursedFxS2C.send(level, 15, player, pulled, player.getEyePosition(), pulled.position(), 0.0F);
            level.playSound(null, x, y, z, SoundEvents.FROG_TONGUE, SoundSource.PLAYERS, 1.5F, 0.8F);
            break;
         case FEAST:
            for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0))) {
               p.heal(10.0F);
               p.getFoodData().eat(6, 0.6F);
               p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
               p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
               level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.4, 0.6, 0.4, 0.0);
            }

            level.playSound(null, x, y, z, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, x, y, z, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 0.7F);
            player.displayClientMessage(Component.translatable("message.vaz2109.ability.feast").withStyle(ChatFormatting.GOLD), true);
            break;
         case KEG:
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(player.getLookAngle().scale(20.0));
            BlockHitResult block = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
            Vec3 stop = block.getType() == Type.MISS ? end : block.getLocation();
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
               level, player, eye, stop, new AABB(eye, stop).inflate(1.0), ex -> ex instanceof LivingEntity && ex.isAlive() && !ex.isSpectator()
            );
            if (hit != null) {
               stop = hit.getLocation();
            }

            for (LivingEntity e : level.getEntitiesOfClass(
               LivingEntity.class, new AABB(stop, stop).inflate(4.5), ex -> ex != player && ex.isAlive() && !ex.isSpectator()
            )) {
               if (!(e.distanceToSqr(stop) > 20.25)) {
                  e.invulnerableTime = 0;
                  e.hurt(player.damageSources().playerAttack(player), 9.0F);
                  e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                  e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                  Vec3 away = e.position().subtract(stop).multiply(1.0, 0.0, 1.0).normalize();
                  e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.9, 0.4, away.z * 0.9));
                  e.hurtMarked = true;
               }
            }

            level.sendParticles(ParticleTypes.SPLASH, stop.x, stop.y + 0.5, stop.z, 120, 2.0, 0.8, 2.0, 0.3);
            level.sendParticles(ParticleTypes.FALLING_HONEY, stop.x, stop.y + 1.5, stop.z, 40, 2.0, 0.6, 2.0, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, stop.x, stop.y + 0.5, stop.z, 30, 1.5, 0.4, 1.5, 0.05);
            CursedFxS2C.send(level, 17, player, null, stop, stop, 4.5F);
            level.playSound(null, stop.x, stop.y, stop.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F, 1.2F);
            level.playSound(null, stop.x, stop.y, stop.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.5F, 0.8F);
            break;
         case PIERCING_BLOOD:
            BloodArts.piercingBlood(level, player, player.getEyePosition().add(0.0, -0.2, 0.0), player.getLookAngle(), power, 6.0F + 22.0F * power);
            Vec3 recoil = player.getLookAngle().scale(-0.35 * power);
            player.setDeltaMovement(player.getDeltaMovement().add(recoil.x, 0.0, recoil.z));
            player.hurtMarked = true;
            break;
         case TURRET:
            Vec3 spot = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.5));
            TurretEntity turret = ModRegistry.TURRET.get().create(level);
            if (turret == null || !level.noCollision(turret, turret.getType().getAABB(spot.x, spot.y, spot.z))) {
               player.displayClientMessage(Component.translatable("message.vaz2109.turret.no_room").withStyle(ChatFormatting.GRAY), true);
               return false;
            }

            turret.moveTo(spot.x, spot.y, spot.z, player.getYRot(), 0.0F);
            turret.setOwner(player);
            level.addFreshEntity(turret);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6F, 1.6F);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.CRIT, spot.x, spot.y + 0.6, spot.z, 12, 0.3, 0.3, 0.3, 0.1);
            break;
         case SWAMP_PACK:
            for (int i = 0; i < 3; i++) {
               AllyFrog frog = ModRegistry.ALLY_FROG.get().create(level);
               if (frog != null) {
                  double ang = player.getYRot() * (Math.PI / 180.0) + (i - 1) * 0.9;
                  double fx = x - Math.sin(ang) * 1.8;
                  double fz = z + Math.cos(ang) * 1.8;
                  frog.moveTo(fx, y, fz, player.getYRot(), 0.0F);
                  frog.finalizeSpawn(level, level.getCurrentDifficultyAt(frog.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                  frog.setOwner(player);
                  level.addFreshEntity(frog);
                  level.sendParticles(ParticleTypes.SPLASH, fx, y + 0.3, fz, 20, 0.3, 0.2, 0.3, 0.1);
               }
            }

            level.playSound(null, x, y, z, SoundEvents.FROG_AMBIENT, SoundSource.PLAYERS, 2.0F, 0.6F);
            level.playSound(null, x, y, z, SoundEvents.FROG_LONG_JUMP, SoundSource.PLAYERS, 1.5F, 1.0F);
            break;
         case FIRE_BREATH:
            player.getPersistentData().putLong(BREATH, level.getGameTime() + 30L);
            level.playSound(null, x, y, z, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 0.7F);
            level.playSound(null, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
      }

      return true;
   }

   /** Brewer's fire breath: strong moonshine sprayed over a lighter, a short cone of flame for 1.5 seconds. */
   private static void breathe(ServerPlayer player, ServerLevel level) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();

      for (int i = 0; i < 6; i++) {
         double s = 0.6 + level.random.nextDouble() * 6.0;
         Vec3 p = eye.add(look.scale(s)).add(level.random.nextGaussian() * 0.08 * s, level.random.nextGaussian() * 0.06 * s - 0.2, level.random.nextGaussian() * 0.08 * s);
         level.sendParticles(i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.01);
      }

      if (level.getGameTime() % 4L == 0L) {
         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(7.0), ex -> ex != player && ex.isAlive() && !ex.isSpectator())) {
            Vec3 to = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0).subtract(eye);
            double dist = to.length();
            if (dist <= 7.0 && to.normalize().dot(look) > 0.82 && player.hasLineOfSight(e)) {
               e.invulnerableTime = 0;
               e.hurt(player.damageSources().playerAttack(player), 2.5F);
               e.setSecondsOnFire(5);
            }
         }

         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.6F);
      }
   }

   @Nullable
   private static LivingEntity lookTarget(Player player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 end = eye.add(player.getLookAngle().scale(range));
      BlockHitResult block = player.level().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
      Vec3 stop = block.getType() == Type.MISS ? end : block.getLocation();
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player.level(), player, eye, stop, new AABB(eye, stop).inflate(1.5), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
      );
      return hit != null ? (LivingEntity)hit.getEntity() : null;
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         Role role = get(player);
         if (role != null) {
            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            CompoundTag data = player.getPersistentData();
            long start = data.getLong("vaz2109ChargeStart");
            BloodBeamEntity beam = BEAMS.get(player.getUUID());
            int limit = beam != null ? 170 : 100;
            if (start > 0L && (now - start >= (long)limit || beam != null && beam.isRemoved())) {
               release(player);
            }

            if (data.getLong(BREATH) > now) {
               breathe(player, level);
            }

            if (player.tickCount % 20 == 0 && player.getVehicle() instanceof VazEntity car && car.getPartMask() == CarPart.allMask()) {
               Trial.FULL_TUNE.add(player, 1);
            }

            if (role == Role.FROG && data.contains("vaz2109FrogLeap") && now - data.getLong("vaz2109FrogLeap") > 5L && player.onGround()) {
               data.remove("vaz2109FrogLeap");
               frogSlam(player, level);
            }

            if (player.tickCount % 5 == 0) {
               swim(player, role == Role.FROG && (player.isInWaterOrRain() || player.isInWaterOrBubble()));
            }

            if (player.tickCount % 20 == 0) {
               switch (role) {
                  case MECHANIC:
                     hidden(player, MobEffects.DIG_SPEED, 0);
                     break;
                  case FROG:
                     hidden(player, MobEffects.JUMP, 1);
                     if (player.isInWater()) {
                        hidden(player, MobEffects.DOLPHINS_GRACE, 0);
                     }
               }
            }
         }
      }
   }

   private static void frogSlam(ServerPlayer player, ServerLevel level) {
      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, player.getBoundingBox().inflate(3.5), ex -> ex != player && ex.isAlive() && !ex.isSpectator()
      )) {
         e.invulnerableTime = 0;
         e.hurt(player.damageSources().playerAttack(player), 9.0F);
         Vec3 away = e.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
         e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.9, 0.5, away.z * 0.9));
         e.hurtMarked = true;
      }

      CursedFxS2C.send(level, 2, player, null, player.position(), player.position(), 0.0F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.5F, 0.5F);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.6F);
   }

   /** Frog: +15% movement speed while wet. */
   private static void swim(Player player, boolean wet) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         boolean has = speed.getModifier(SWIM_ID) != null;
         if (wet && !has) {
            speed.addTransientModifier(new AttributeModifier(SWIM_ID, "Frog in water", 0.15, Operation.MULTIPLY_BASE));
         } else if (!wet && has) {
            speed.removeModifier(SWIM_ID);
         }
      }
   }

   private static void hidden(LivingEntity entity, MobEffect effect, int amplifier) {
      entity.addEffect(new MobEffectInstance(effect, 50, amplifier, true, false, false));
   }

   private static void belly(Player player) {
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         health.removeModifier(BELLY_ID);
         if (is(player, Role.BREWER)) {
            health.addPermanentModifier(new AttributeModifier(BELLY_ID, "Brewer's belly", 6.0, Operation.ADDITION));
         }

         if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
         }
      }
   }

   @SubscribeEvent
   public static void onHurt(LivingHurtEvent event) {
      if (!applying
         && event.getSource().getEntity() instanceof ServerPlayer player
         && event.getSource().getDirectEntity() == player
         && is(player, Role.VESSEL)
         && player.level().getGameTime() < player.getPersistentData().getLong("vaz2109BlueEnd")
         && player.getMainHandItem().isEmpty()) {
         LivingEntity target = event.getEntity();
         ServerLevel level = player.serverLevel();
         int n = SukunaVessel.fingers(player);
         Vec3 c = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);
         Vec3 push = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
         float swing = SWING.getOrDefault(player, 0.0F);
         float chance = SlaughterDemonItem.inZone(player) ? BLACK_FLASH_ZONE_CHANCE : BLACK_FLASH_CHANCE;
         if (n >= 15 && swing >= 0.95F && player.getRandom().nextFloat() < chance) {
            event.setAmount(event.getAmount() * 2.5F);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x * 1.8, 0.5, push.z * 1.8));
            target.hurtMarked = true;
            SlaughterDemonItem.blackFlash(level, player, target);
            level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 20, 0.3, 0.3, 0.3, 0.25);
         } else {
            IMPACTS.add(new Roles.Impact(player, target, level.getGameTime() + 6L, 1.5F + 0.1F * (float)n));
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 0.7F);
         }
      }
   }

   @SubscribeEvent
   public static void onAttack(AttackEntityEvent event) {
      SWING.put(event.getEntity(), event.getEntity().getAttackStrengthScale(0.5F));
   }

   @SubscribeEvent
   public static void onJump(LivingJumpEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && is(player, Role.FROG)) {
         Trial.JUMPS.add(player, 1);
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END && !IMPACTS.isEmpty()) {
         List<Roles.Impact> due = new ArrayList<>();
         IMPACTS.removeIf(ix -> {
            if (!ix.target.isAlive() || ix.target.level() != ix.attacker.level()) {
               return true;
            } else if (ix.attacker.level().getGameTime() < ix.at) {
               return false;
            } else {
               due.add(ix);
               return true;
            }
         });
         applying = true;

         try {
            for (Roles.Impact i : due) {
               ServerLevel level = i.attacker.serverLevel();
               i.target.invulnerableTime = 0;
               i.target.hurt(i.attacker.damageSources().playerAttack(i.attacker), i.damage);
               Vec3 push = i.target.position().subtract(i.attacker.position()).multiply(1.0, 0.0, 1.0).normalize();
               i.target.setDeltaMovement(i.target.getDeltaMovement().add(push.x * 0.9, 0.25, push.z * 0.9));
               i.target.hurtMarked = true;
               Vec3 c = i.target.position().add(0.0, (double)i.target.getBbHeight() * 0.55, 0.0);
               CursedFxS2C.send(level, 16, i.attacker, i.target, c, c, 0.0F);
               level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35F, 1.9F);
               level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 0.9F, 1.4F);
            }
         } finally {
            applying = false;
         }
      }
   }

   @SubscribeEvent
   public static void onEffect(Applicable event) {
      if (event.getEntity() instanceof Player player) {
         MobEffect var4 = event.getEffectInstance().getEffect();
         Role role = get(player);
         if (role == Role.VESSEL && (var4 == MobEffects.POISON || var4 == MobEffects.WITHER) || role == Role.BREWER && var4 == MobEffects.CONFUSION) {
            event.setResult(Result.DENY);
         }
      }
   }

   @SubscribeEvent
   public static void onEffectAdded(Added event) {
      if (event.getEntity() instanceof Player player
         && !player.level().isClientSide
         && is(player, Role.BREWER)
         && player.isUsingItem()
         && player.getUseItem().getItem() instanceof PotionItem) {
         MobEffectInstance e = event.getEffectInstance();
         if (!e.getEffect().isInstantenous()) {
            e.update(new MobEffectInstance(e.getEffect(), e.getDuration() * 3 / 2, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
         }
      }
   }

   @SubscribeEvent
   public static void onFall(LivingFallEvent event) {
      if (event.getEntity() instanceof Player player && is(player, Role.FROG)) {
         event.setDistance(player.getPersistentData().contains("vaz2109FrogLeap") ? 0.0F : event.getDistance() * 0.5F);
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         player.getPersistentData().remove("vaz2109ChargeStart");
         belly(player);
         sync(player);
      }
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         belly(player);
         if (!event.isEndConquered()) {
            player.setHealth(player.getMaxHealth());
         }

         sync(player);
      }
   }

   @SubscribeEvent
   public static void onCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("vazclass")
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("reset").requires(s -> s.hasPermission(2)))
                           .executes(c -> reset((CommandSourceStack)c.getSource(), List.of(((CommandSourceStack)c.getSource()).getPlayerOrException()))))
                        .then(
                           Commands.argument("targets", EntityArgument.players())
                              .executes(c -> reset((CommandSourceStack)c.getSource(), EntityArgument.getPlayers(c, "targets")))
                        )
                  ))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("set").requires(s -> s.hasPermission(2)))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("class", StringArgumentType.word())
                              .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Role.values()).map(r -> r.id), b))
                              .executes(
                                 c -> set(
                                       (CommandSourceStack)c.getSource(),
                                       StringArgumentType.getString(c, "class"),
                                       List.of(((CommandSourceStack)c.getSource()).getPlayerOrException())
                                    )
                              ))
                           .then(
                              Commands.argument("targets", EntityArgument.players())
                                 .executes(
                                    c -> set(
                                          (CommandSourceStack)c.getSource(), StringArgumentType.getString(c, "class"), EntityArgument.getPlayers(c, "targets")
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static int reset(CommandSourceStack source, Collection<ServerPlayer> players) {
      for (ServerPlayer p : players) {
         set(p, null);
      }

      source.sendSuccess(() -> Component.translatable("commands.vaz2109.class.reset", new Object[]{players.size()}), true);
      return players.size();
   }

   private static int set(CommandSourceStack source, String id, Collection<ServerPlayer> players) {
      Role role = Role.byName(id);
      if (role == null) {
         source.sendFailure(Component.translatable("commands.vaz2109.class.unknown", new Object[]{id}));
         return 0;
      } else {
         for (ServerPlayer p : players) {
            set(p, role);
         }

         source.sendSuccess(
            () -> Component.translatable("commands.vaz2109.class.set", new Object[]{Component.translatable("role.vaz2109." + role.id), players.size()}), true
         );
         return players.size();
      }
   }

   public static void choose(ServerPlayer player, int ordinal) {
      Role role = Role.byId(ordinal);
      if (role != null && get(player) == null) {
         set(player, role);
         player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
         player.sendSystemMessage(
            Component.translatable(
               "message.vaz2109.role.chosen", new Object[]{Component.translatable("role.vaz2109." + role.id).withStyle(s -> s.withColor(role.color))}
            )
         );
      }
   }

   /** Dismantle slashes in flight: fired one by one, each along wherever the caster is looking at that moment. */
   @SubscribeEvent
   public static void onSlashTick(ServerTickEvent event) {
      if (event.phase == Phase.END && !SLASHES.isEmpty()) {
         List<Roles.Slash> due = new ArrayList<>();
         SLASHES.removeIf(sl -> {
            if (!sl.caster.isAlive() || sl.caster.isRemoved()) {
               return true;
            } else if (sl.caster.level().getGameTime() < sl.at) {
               return false;
            } else {
               due.add(sl);
               return true;
            }
         });

         for (Roles.Slash sl : due) {
            SukunaVessel.dismantle(sl.caster, sl.caster.getEyePosition(), sl.caster.getLookAngle(), sl.damage, 1);
         }
      }
   }

   /** Slashes per Dismantle cast: 2, plus one for every 5 of Sukuna's fingers. */
   public static int slashes(int fingers) {
      return 2 + fingers / 5;
   }

   private static record Slash(ServerPlayer caster, long at, float damage) {
   }

   private static record Impact(ServerPlayer attacker, LivingEntity target, long at, float damage) {
   }
}
