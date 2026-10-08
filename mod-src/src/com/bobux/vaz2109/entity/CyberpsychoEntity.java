package com.bobux.vaz2109.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.joml.Vector3f;

/**
 * A cyberpsycho: someone who put in too much chrome. Hunts people at night with mantis blades, fires a
 * Sandevistan when it closes in (a burst of speed with a colour trail) and its reflexes slip arrows and bullets.
 * Drops neuroblockers and now and then an implant.
 */
public class CyberpsychoEntity extends Monster {
   private int sandeCooldown = 100;
   private int sandeLeft;
   private boolean announced;

   public CyberpsychoEntity(EntityType<? extends CyberpsychoEntity> type, Level level) {
      super(type, level);
      this.xpReward = 20;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.ATTACK_DAMAGE, 6.0)
         .add(Attributes.MOVEMENT_SPEED, 0.31)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      ServerLevel level = (ServerLevel)this.level();
      LivingEntity target = this.getTarget();
      if (target != null && !this.announced && target instanceof Player) {
         this.announced = true;
         for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0))) {
            p.displayClientMessage(Component.translatable("message.vaz2109.cyberpsycho.spotted").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
         }
      }

      if (this.sandeLeft > 0) {
         this.sandeLeft--;
         int rgb = Mth.hsvToRgb((float)(this.tickCount % 30) / 30.0F, 0.85F, 1.0F);
         DustParticleOptions dust = new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F), 1.3F);
         for (double y = 0.2; y < 1.9; y += 0.4) {
            level.sendParticles(dust, this.xo, this.yo + y, this.zo, 1, 0.1, 0.05, 0.1, 0.0);
         }
      } else if (--this.sandeCooldown <= 0 && target != null && this.distanceTo(target) < 16.0F) {
         this.sandeCooldown = 300 + this.random.nextInt(200);
         this.sandeLeft = 50;
         this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 3, false, false));
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 0, false, false));
         this.playSound(SoundEvents.BEACON_ACTIVATE, 1.0F, 1.9F);
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.getDirectEntity() instanceof Projectile && this.random.nextFloat() < 0.3F) {
         this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.6F);
         return false;
      }

      return super.hurt(source, amount);
   }

   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living) {
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
         this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);
      }

      return hit;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.PILLAGER_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   public float getVoicePitch() {
      return 0.7F + this.random.nextFloat() * 0.2F;
   }

   public static boolean canSpawn(EntityType<CyberpsychoEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
      return Monster.checkMonsterSpawnRules(type, (ServerLevel)level.getLevel(), reason, pos, random) && random.nextInt(4) == 0;
   }
}
