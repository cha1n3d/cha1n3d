package com.bobux.vaz2109.role;

import com.bobux.vaz2109.entity.BossResistance;
import com.bobux.vaz2109.fx.ModParticles;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import org.joml.Vector3f;

/** Blood Manipulation shared by Choso and a Vessel who ate the Death Painting. */
public final class BloodArts {
   public static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.03F), 1.1F);
   public static final DustParticleOptions BLOOD_DARK = new DustParticleOptions(new Vector3f(0.3F, 0.0F, 0.02F), 1.4F);
   public static final DustParticleOptions BLOOD_CORE = new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.08F), 0.7F);

   private BloodArts() {
   }

   /** Convergence: the blood being compressed in front of the palms, spiralling inward. */
   public static void convergence(ServerLevel level, LivingEntity caster, int ticks) {
      Vec3 look = caster.getLookAngle();
      Vec3 hands = caster.getEyePosition().add(look.scale(0.7)).add(0.0, -0.35, 0.0);
      double r = Math.max(0.12, 0.7 - ticks * 0.015);

      for (int i = 0; i < 3; i++) {
         double a = (ticks * 0.6 + i * 2.1);
         Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize();
         Vec3 p = hands.add(side.scale(Math.cos(a) * r)).add(0.0, Math.sin(a) * r, 0.0);
         level.sendParticles(i == 0 ? BLOOD_DARK : BLOOD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      level.sendParticles(BLOOD_CORE, hands.x, hands.y, hands.z, 1, 0.02, 0.02, 0.02, 0.0);
   }

   /**
    * Piercing Blood: a supersonic jet of compressed blood. Goes through every living thing on the line,
    * punches through a few soft blocks and stops at the first hard one.
    */
   public static void piercingBlood(ServerLevel level, LivingEntity caster, Vec3 from, Vec3 dir, float power, float damage) {
      dir = dir.normalize();
      double range = 12.0 + 36.0 * power;
      int budget = 1 + Math.round(4.0F * power);
      Vec3 end = from.add(dir.scale(range));

      for (double s = 0.6; s <= range; s += 0.25) {
         Vec3 p = from.add(dir.scale(s));
         BlockPos pos = BlockPos.containing(p);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && !state.getCollisionShape(level, pos).isEmpty()) {
            float hardness = state.getDestroySpeed(level, pos);
            boolean soft = hardness >= 0.0F && hardness <= 0.8F && !state.hasBlockEntity();
            boolean allowed = soft
               && budget > 0
               && (
                  caster instanceof Player player
                     ? !MinecraftForge.EVENT_BUS.post(new BreakEvent(level, pos, state, player))
                     : level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
               );
            if (!allowed) {
               end = p;
               break;
            }

            budget--;
            level.destroyBlock(pos, level.random.nextFloat() < 0.3F, caster);
         }
      }

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(from, end).inflate(1.0), ex -> ex != caster && ex.isAlive() && !ex.isSpectator() && !ex.isAlliedTo(caster)
      )) {
         Optional<Vec3> hit = e.getBoundingBox().inflate(0.25).clip(from, end);
         if (hit.isPresent()) {
            Vec3 at = hit.get();
            e.invulnerableTime = 0;
            e.hurt(BossResistance.sorcery(caster), damage);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + Math.round(30.0F * power), 1));
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 0.5 * power, 0.12, dir.z * 0.5 * power));
            e.hurtMarked = true;
            splash(level, at, dir, 14);
         }
      }

      double length = end.distanceTo(from);

      for (double s = 0.8; s < length; s += 0.22) {
         Vec3 p = from.add(dir.scale(s));
         level.sendParticles(s % 0.66 < 0.22 ? BLOOD_CORE : BLOOD, p.x, p.y, p.z, 1, 0.015, 0.015, 0.015, 0.0);
      }

      Vec3 muzzle = from.add(dir.scale(0.9));
      level.sendParticles(ParticleTypes.CLOUD, muzzle.x, muzzle.y, muzzle.z, 6, 0.05, 0.05, 0.05, 0.08);
      splash(level, end, dir.reverse(), 18);
      level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.6F, 1.9F);
      level.playSound(null, from.x, from.y, from.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.35F + 0.3F * power, 1.9F);
      level.playSound(null, end.x, end.y, end.z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.2F, 0.6F);
   }

   /** Supernova: orbs of blood around the caster burst into needles that hit everyone nearby. */
   public static void supernova(ServerLevel level, LivingEntity caster, double radius, float damage) {
      Vec3 c = caster.position().add(0.0, caster.getBbHeight() * 0.6, 0.0);

      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4.0;
         Vec3 orb = c.add(Math.cos(a) * 1.6, Math.sin(a * 2.0) * 0.4, Math.sin(a) * 1.6);
         level.sendParticles(BLOOD_DARK, orb.x, orb.y, orb.z, 10, 0.12, 0.12, 0.12, 0.0);

         for (int k = 1; k <= 6; k++) {
            Vec3 p = orb.add(Math.cos(a) * k * radius / 6.0, 0.0, Math.sin(a) * k * radius / 6.0);
            level.sendParticles(BLOOD, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
         }
      }

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, caster.getBoundingBox().inflate(radius), ex -> ex != caster && ex.isAlive() && !ex.isSpectator() && !ex.isAlliedTo(caster)
      )) {
         if (e.distanceTo(caster) <= radius) {
            e.invulnerableTime = 0;
            e.hurt(BossResistance.sorcery(caster), damage);
            Vec3 away = e.position().subtract(caster.position()).multiply(1.0, 0.0, 1.0).normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.7, 0.3, away.z * 0.7));
            e.hurtMarked = true;
            splash(level, e.position().add(0.0, e.getBbHeight() * 0.5, 0.0), away, 8);
         }
      }

      level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.5F);
      level.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
   }

   public static void splash(ServerLevel level, Vec3 at, Vec3 dir, int count) {
      level.sendParticles(ModParticles.BLOOD.get(), at.x, at.y, at.z, count, 0.2, 0.2, 0.2, 0.15);
      level.sendParticles(BLOOD_DARK, at.x + dir.x * 0.3, at.y, at.z + dir.z * 0.3, count / 2, 0.25, 0.25, 0.25, 0.0);
   }
}
