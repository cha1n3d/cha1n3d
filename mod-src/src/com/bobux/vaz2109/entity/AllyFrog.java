package com.bobux.vaz2109.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Frog-man's swamp pack: Timokha's battle frogs, but loyal to the player who called them. */
public class AllyFrog extends TimokhaFrog {
   public static final int LIFETIME = 600;
   @Nullable
   private UUID owner;
   private int life;

   public AllyFrog(EntityType<? extends AllyFrog> type, Level level) {
      super(type, level);
      this.xpReward = 0;
   }

   public static Builder createAttributes() {
      return TimokhaFrog.createAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   public void setOwner(Player player) {
      this.owner = player.getUUID();
   }

   @Nullable
   public Player owner() {
      return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
   }

   protected void registerGoals() {
      super.registerGoals();
      this.targetSelector.removeAllGoals(g -> true);
      this.targetSelector.addGoal(1, new AllyFrog.DefendOwnerGoal());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false, e -> e instanceof Enemy && !(e instanceof AllyFrog)));
      this.goalSelector.addGoal(3, new AllyFrog.FollowOwnerGoal());
   }

   public boolean isAlliedTo(Entity other) {
      return other instanceof AllyFrog || other instanceof TurretEntity || this.owner != null && this.owner.equals(other.getUUID()) || super.isAlliedTo(other);
   }

   public boolean hurt(DamageSource source, float amount) {
      return source.getEntity() != null && this.isAlliedTo(source.getEntity()) ? false : super.hurt(source, amount);
   }

   public void aiStep() {
      super.aiStep();
      if (this.level() instanceof ServerLevel level && (++this.life > LIFETIME || this.owner != null && this.owner() == null && this.life > 40)) {
         level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.3, this.getZ(), 16, 0.2, 0.2, 0.2, 0.05);
         level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.3, this.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
         this.discard();
      }
   }

   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      tag.putInt("Life", this.life);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("Owner")) {
         this.owner = tag.getUUID("Owner");
      }

      this.life = tag.getInt("Life");
   }

   private final class DefendOwnerGoal extends TargetGoal {
      @Nullable
      private LivingEntity enemy;

      DefendOwnerGoal() {
         super(AllyFrog.this, false);
         this.setFlags(EnumSet.of(Goal.Flag.TARGET));
      }

      public boolean canUse() {
         Player owner = AllyFrog.this.owner();
         if (owner == null) {
            return false;
         } else {
            LivingEntity attacker = owner.getLastHurtByMob();
            LivingEntity victim = owner.getLastHurtMob();
            this.enemy = attacker != null && attacker.isAlive() && !AllyFrog.this.isAlliedTo(attacker)
               ? attacker
               : (victim != null && victim.isAlive() && !AllyFrog.this.isAlliedTo(victim) && !(victim instanceof Player) ? victim : null);
            return this.enemy != null && this.enemy != AllyFrog.this.getTarget();
         }
      }

      public void start() {
         AllyFrog.this.setTarget(this.enemy);
         super.start();
      }
   }

   private final class FollowOwnerGoal extends Goal {
      FollowOwnerGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      public boolean canUse() {
         Player owner = AllyFrog.this.owner();
         return owner != null && AllyFrog.this.getTarget() == null && AllyFrog.this.distanceToSqr(owner) > 36.0;
      }

      public void tick() {
         Player owner = AllyFrog.this.owner();
         if (owner != null) {
            if (AllyFrog.this.distanceToSqr(owner) > 576.0) {
               AllyFrog.this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            } else {
               AllyFrog.this.getNavigation().moveTo(owner, 1.4);
            }
         }
      }
   }
}
