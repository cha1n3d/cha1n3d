package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A thrown firecracker: a loud bang that stings and sends mobs running; it never breaks blocks. */
public class FirecrackerEntity extends ThrowableItemProjectile {
   public FirecrackerEntity(EntityType<? extends FirecrackerEntity> type, Level level) {
      super(type, level);
   }

   public FirecrackerEntity(Level level, LivingEntity owner) {
      super(ModRegistry.FIRECRACKER_ENTITY.get(), owner, level);
   }

   protected Item getDefaultItem() {
      return ModRegistry.FIRECRACKER.get();
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (this.level() instanceof ServerLevel level) {
         Vec3 c = this.position();
         level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 30, 0.2, 0.2, 0.2, 0.15);
         level.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 15, 0.3, 0.2, 0.3, 0.02);
         level.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.5F, 1.2F);
         level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6F, 2.0F);

         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0), LivingEntity::isAlive)) {
            double d = e.position().distanceTo(c);
            if (d < 2.5) {
               e.hurt(this.damageSources().thrown(this, this.getOwner()), 3.0F);
            }

            if (e instanceof PathfinderMob mob && d < 6.0 && e != this.getOwner()) {
               Vec3 away = mob.position().subtract(c).normalize().scale(8.0).add(mob.position());
               mob.getNavigation().moveTo(away.x, away.y, away.z, 1.6);
               mob.setDeltaMovement(mob.getDeltaMovement().add(mob.position().subtract(c).normalize().multiply(0.6, 0.0, 0.6).add(0.0, 0.3, 0.0)));
               mob.hurtMarked = true;
            }
         }

         this.discard();
      }
   }
}
