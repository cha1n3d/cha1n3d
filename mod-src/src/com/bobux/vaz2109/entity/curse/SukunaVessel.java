package com.bobux.vaz2109.entity.curse;

import com.bobux.vaz2109.entity.BossResistance;
import com.bobux.vaz2109.fx.ModSounds;
import com.bobux.vaz2109.item.CursedToolItem;
import com.bobux.vaz2109.network.CursedFxS2C;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.PossessionS2C;
import com.bobux.vaz2109.role.Roles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class SukunaVessel {
   public static final int MAX_FINGERS = 20;
   private static final String FINGERS = "vaz2109_sukuna_fingers";
   private static final String POSSESSED = "vaz2109_sukuna_possessed";
   private static final String POSSESSED_TOTAL = "vaz2109_sukuna_possessed_total";
   private static final UUID HEALTH_ID = UUID.fromString("5d1f3c62-8e0e-4f0e-9c55-2b3e8d0a7a11");
   private static final UUID ATTACK_ID = UUID.fromString("a2c47e91-3b6d-4d8f-8f3a-6c1e0b9d4e22");
   private static final Map<UUID, SukunaVessel.Brain> BRAINS = new HashMap<>();

   private SukunaVessel() {
   }

   public static int fingers(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted").getInt("vaz2109_sukuna_fingers");
   }

   public static void setFingers(Player player, int count) {
      CompoundTag data = player.getPersistentData();
      CompoundTag persisted = data.getCompound("PlayerPersisted");
      persisted.putInt("vaz2109_sukuna_fingers", Math.max(0, Math.min(20, count)));
      data.put("PlayerPersisted", persisted);
      apply(player);
   }

   public static void apply(Player player) {
      int n = fingers(player);
      modifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, "Sukuna's fingers: health", (double)n * 1.0);
      modifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, "Sukuna's fingers: attack", (double)n * 0.3);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void modifier(AttributeInstance attribute, UUID id, String name, double amount) {
      if (attribute != null) {
         attribute.removeModifier(id);
         if (amount > 0.0) {
            attribute.addPermanentModifier(new AttributeModifier(id, name, amount, Operation.ADDITION));
         }
      }
   }

   public static boolean possessed(Player player) {
      return player.getPersistentData().getInt("vaz2109_sukuna_possessed") > 0;
   }

   public static int possessionLeft(Player player) {
      return player.getPersistentData().getInt("vaz2109_sukuna_possessed");
   }

   public static int possessionTotal(Player player) {
      return Math.max(1, player.getPersistentData().getInt("vaz2109_sukuna_possessed_total"));
   }

   public static void possess(ServerPlayer player) {
      possess(player, 160 + 10 * fingers(player));
   }

   public static void possess(ServerPlayer player, int time) {
      CompoundTag data = player.getPersistentData();
      data.putInt("vaz2109_sukuna_possessed", time);
      data.putInt("vaz2109_sukuna_possessed_total", time);
      SukunaVessel.Brain brain = new SukunaVessel.Brain();
      brain.next = player.level().getGameTime() + 20L;
      BRAINS.put(player.getUUID(), brain);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, time, 1, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, time, 1, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, time, 1, false, false));
      Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PossessionS2C(time, 1));
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.8F, 1.3F);
      Component line = Component.translatable(
            "chat.vaz2109.sukuna",
            new Object[]{Component.translatable("chat.vaz2109.sukuna." + player.getRandom().nextInt(4)).withStyle(ChatFormatting.ITALIC)}
         )
         .withStyle(ChatFormatting.DARK_RED);

      for (Player p : player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(32.0))) {
         p.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.takeover", new Object[]{player.getDisplayName()}).withStyle(ChatFormatting.RED));
         p.sendSystemMessage(line);
      }

      Roles.sync(player);
   }

   private static void release(ServerPlayer player) {
      player.getPersistentData().remove("vaz2109_sukuna_possessed");
      BRAINS.remove(player.getUUID());
      cancelCharge(player);
      Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PossessionS2C(0, 1));
      player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
      player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
      player.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.released").withStyle(ChatFormatting.GRAY));
      Roles.sync(player);
   }

   private static void think(ServerPlayer player, ServerLevel level, SukunaVessel.Brain b) {
      int n = fingers(player);
      long now = level.getGameTime();
      LivingEntity target = null;
      double best = Double.MAX_VALUE;

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(24.0),
         ex -> ex != player && ex.isAlive() && !ex.isSpectator() && !(ex instanceof ArmorStand) && (!(ex instanceof Player p) || !p.isCreative())
      )) {
         double d = e.distanceToSqr(player);
         if (d < best && player.hasLineOfSight(e)) {
            best = d;
            target = e;
         }
      }

      Vec3 eye = player.getEyePosition();
      if (b.fugaLeft >= 0) {
         if (b.fugaLeft-- == 0) {
            Vec3 dir = target != null ? target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye) : player.getLookAngle();
            fuga(player, eye, dir, 1.0F, n);
         }
      } else if (target != null && now >= b.next) {
         double dist = Math.sqrt(best);
         Vec3 aim = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye);
         if (SukunaAwakening.busy(player)) {
            b.next = now + 10L;
         } else if (n >= 5 && dist > 7.0 && now >= b.fugaReady && player.getRandom().nextFloat() < 0.4F) {
            b.fugaLeft = 30;
            b.fugaReady = now + 200L;
            charge(player, 30);
            b.next = now + 45L;
         } else if (dist < 4.5) {
            cleave(player, target, n);
            b.next = now + 30L;
         } else {
            dismantle(player, eye, aim, 6.0F + 0.5F * (float)n, 1 + n / 7);
            b.next = now + 18L + (long)player.getRandom().nextInt(14);
         }
      }
   }

   public static void dismantle(ServerPlayer caster, Vec3 from, Vec3 dir, float damage, int count) {
      ServerLevel level = caster.serverLevel();
      dir = dir.normalize();
      Vec3 u = perp(dir);
      Vec3 v = dir.cross(u);
      double range = 28.0;

      for (int c = 0; c < count; c++) {
         double tilt = caster.getRandom().nextDouble() * Math.PI;
         Vec3 side = u.scale(Math.cos(tilt)).add(v.scale(Math.sin(tilt)));
         Vec3 start = from.add(dir.scale((double)c * 0.6));
         int budget = 80;

         for (double s = 1.0; s <= range && budget > 0; s += 0.5) {
            Vec3 p = start.add(dir.scale(s));

            for (double k = -1.5; k <= 1.5; k += 0.75) {
               if (cut(level, BlockPos.containing(p.add(side.scale(k))), caster)) {
                  budget--;
               }
            }

            if (s % 3.5 == 0.0) {
               CursedFxS2C.send(level, 11, caster, null, p.subtract(side.scale(1.8)), p.add(side.scale(1.8)), 0.28F);
            }
         }

         Vec3 end = start.add(dir.scale(range));

         for (LivingEntity e : level.getEntitiesOfClass(
            LivingEntity.class, new AABB(start, end).inflate(2.0), ex -> ex != caster && ex.isAlive() && !ex.isSpectator()
         )) {
            Vec3 ce = e.position().add(0.0, (double)(e.getBbHeight() / 2.0F), 0.0);
            double t = Math.max(0.0, Math.min(range, ce.subtract(start).dot(dir)));
            if (ce.distanceTo(start.add(dir.scale(t))) < 1.6 + (double)(e.getBbWidth() / 2.0F)) {
               e.invulnerableTime = 0;
               e.hurt(BossResistance.sorcery(caster), damage);
               xSlash(level, caster, e, 0.3F);
               ModSounds.play(level, ce, 1.0F, ModSounds.SOUL_HIT);
            }
         }
      }

      level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.6F);
      ModSounds.play(level, caster.position(), 1.0F, ModSounds.SOUL_CUT);
   }

   public static void cleave(ServerPlayer caster, LivingEntity target, int fingers) {
      ServerLevel level = caster.serverLevel();
      float damage = Math.min(50.0F, 8.0F + 0.25F * target.getMaxHealth()) + 0.5F * (float)fingers;
      target.invulnerableTime = 0;
      target.hurt(BossResistance.sorcery(caster), damage);
      xSlash(level, caster, target, 0.45F);
      Vec3 c = target.position().add(0.0, (double)(target.getBbHeight() / 2.0F), 0.0);
      level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 25, 0.3, 0.5, 0.3, 0.5);
      ModSounds.play(level, c, 1.2F, ModSounds.SOUL_HIT, ModSounds.RATIO_CUT);
   }

   private static void xSlash(ServerLevel level, LivingEntity caster, LivingEntity target, float width) {
      Vec3 c = target.position().add(0.0, (double)(target.getBbHeight() / 2.0F), 0.0);
      Vec3 side = caster.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale(0.6 + (double)target.getBbWidth());
      Vec3 up = new Vec3(0.0, 0.5 + (double)target.getBbHeight() * 0.5, 0.0);
      CursedFxS2C.send(level, 11, caster, target, c.add(side).add(up), c.subtract(side).subtract(up), width);
      CursedFxS2C.send(level, 11, caster, target, c.subtract(side).add(up), c.add(side).subtract(up), width);
   }

   public static void charge(ServerPlayer caster, int ticks) {
      CursedFxS2C.send(caster.serverLevel(), 13, caster, null, caster.position(), caster.position(), (float)ticks);
      if (ticks > 0) {
         caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.6F);
         caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
      }
   }

   public static void cancelCharge(ServerPlayer caster) {
      charge(caster, 0);
   }

   public static void fuga(ServerPlayer caster, Vec3 eye, Vec3 dir, float power, int fingers) {
      ServerLevel level = caster.serverLevel();
      Vec3 end = eye.add(dir.normalize().scale(50.0));
      BlockHitResult block = level.clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, caster));
      Vec3 stop = block.getType() == Type.MISS ? end : block.getLocation();
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         level, caster, eye, stop, new AABB(eye, stop).inflate(1.0), e -> e instanceof LivingEntity && !e.isSpectator() && e.isAlive()
      );
      if (hit != null) {
         stop = hit.getLocation();
      }

      float radius = 2.5F + 3.5F * power;
      float damage = 8.0F + 22.0F * power + 0.5F * (float)fingers;

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(stop, stop).inflate((double)radius), ex -> ex != caster && ex.isAlive() && !ex.isSpectator()
      )) {
         double d = e.position().add(0.0, (double)(e.getBbHeight() / 2.0F), 0.0).distanceTo(stop);
         if (!(d > (double)(radius + e.getBbWidth()))) {
            e.invulnerableTime = 0;
            e.hurt(BossResistance.sorcery(caster), damage * (float)Math.max(0.35, 1.0 - d / (double)(radius + 1.0F)));
            e.setSecondsOnFire(10);
         }
      }

      if (stop.distanceTo(caster.position()) > (double)radius + 1.5) {
         level.explode(caster, stop.x, stop.y, stop.z, 1.5F + 2.5F * power, true, ExplosionInteraction.MOB);
      }

      for (int i = 0; (float)i < 12.0F + 20.0F * power; i++) {
         BlockPos p = BlockPos.containing(
            stop.add((caster.getRandom().nextDouble() - 0.5) * (double)radius * 2.0, 1.0, (caster.getRandom().nextDouble() - 0.5) * (double)radius * 2.0)
         );

         for (int dy = 0; dy < 4; p = p.below()) {
            if (level.isEmptyBlock(p) && BaseFireBlock.canBePlacedAt(level, p, Direction.UP)) {
               level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
               break;
            }

            dy++;
         }
      }

      level.sendParticles(
         ParticleTypes.FLAME, stop.x, stop.y, stop.z, (int)(60.0F + 100.0F * power), (double)radius * 0.5, (double)radius * 0.4, (double)radius * 0.5, 0.12
      );
      level.sendParticles(ParticleTypes.LAVA, stop.x, stop.y, stop.z, 20, 1.0, 0.5, 1.0, 0.0);
      CursedFxS2C.send(level, 12, caster, null, eye.add(0.0, -0.25, 0.0), stop, radius * 1.3F);
      ModSounds.play(level, caster.position(), 1.4F, ModSounds.JET_IGNITE, ModSounds.JET_ROAR);
      level.playSound(null, stop.x, stop.y, stop.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0F, 0.6F);
      caster.displayClientMessage(
         Component.translatable("message.vaz2109.sukuna.fuga").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), true
      );
   }

   public static boolean cut(ServerLevel level, BlockPos pos, Player caster) {
      BlockState state = level.getBlockState(pos);
      if (!state.isAir() && !state.hasBlockEntity() && !(state.getBlock() instanceof LiquidBlock) && !MalevolentShrine.isShrineBlock(level, pos)) {
         float hardness = state.getDestroySpeed(level, pos);
         if (!(hardness < 0.0F) && !(hardness > 3.0F)) {
            return MinecraftForge.EVENT_BUS.post(new BreakEvent(level, pos, state, caster))
               ? false
               : level.destroyBlock(pos, caster.getRandom().nextFloat() < 0.2F, caster);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Vec3 perp(Vec3 dir) {
      Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      return dir.cross(up).normalize();
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      apply(event.getEntity());
      event.getEntity().getPersistentData().remove("vaz2109_sukuna_possessed");
   }

   @SubscribeEvent
   public static void onRespawn(PlayerRespawnEvent event) {
      apply(event.getEntity());
      if (!event.isEndConquered()) {
         event.getEntity().setHealth(event.getEntity().getMaxHealth());
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      BRAINS.remove(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         CompoundTag data = player.getPersistentData();
         int left = data.getInt("vaz2109_sukuna_possessed");
         if (left > 0) {
            if (!player.isAlive()) {
               data.remove("vaz2109_sukuna_possessed");
               BRAINS.remove(player.getUUID());
            } else {
               data.putInt("vaz2109_sukuna_possessed", left - 1);
               ServerLevel level = player.serverLevel();
               if (left % 4 == 0) {
                  level.sendParticles(CurseEntity.CURSED_ENERGY_DARK, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.4, 0.6, 0.4, 0.0);
               }

               think(player, level, BRAINS.computeIfAbsent(player.getUUID(), u -> new SukunaVessel.Brain()));
               if (left - 1 == 0) {
                  release(player);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof CurseEntity && event.getSource().getEntity() instanceof Player player) {
         float var3 = 1.0F + 0.05F * (float)fingers(player);
         if (player.getMainHandItem().getItem() instanceof CursedToolItem) {
            var3 += 0.5F;
         }

         event.setAmount(event.getAmount() * var3);
      }
   }

   private static final class Brain {
      long next;
      long fugaReady;
      int fugaLeft = -1;
      boolean shrine;
   }
}
