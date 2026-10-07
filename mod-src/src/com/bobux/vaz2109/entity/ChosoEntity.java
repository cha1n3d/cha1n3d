package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.quest.BossQuest;
import com.bobux.vaz2109.role.BloodArts;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Choso, eldest of the Death Painting brothers. Blood Manipulation: a telegraphed Piercing Blood beam
 * (aim locks shortly before the shot, so it can be side-stepped), Supernova up close, blood armour when hurt
 * and Flowing Red Scale below half health.
 */
public class ChosoEntity extends QuestBoss {
   private static final int BEAM_WINDUP = 30;
   private static final int BEAM_LOCK = 8;
   private static final int NOVA_WINDUP = 14;
   private int beamCooldown = 60;
   private int novaCooldown = 40;
   private int armorCooldown;
   private int beamIn = -1;
   private int novaIn = -1;
   private Vec3 lockedAim = Vec3.ZERO;
   private boolean scale;

   public ChosoEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, BossQuest.CHOSO, BossBarColor.RED, ChatFormatting.DARK_RED);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 520.0)
         .add(Attributes.ATTACK_DAMAGE, 9.0)
         .add(Attributes.MOVEMENT_SPEED, 0.32)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.ARMOR_TOUGHNESS, 2.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void moves(LivingEntity target, double distance) {
      ServerLevel level = (ServerLevel)this.level();
      if (this.beamIn >= 0) {
         this.getNavigation().stop();
         this.getLookControl().setLookAt(target, 30.0F, 30.0F);
         BloodArts.convergence(level, this, BEAM_WINDUP - this.beamIn);
         if (this.beamIn == BEAM_LOCK) {
            this.lockedAim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 2.0F, 1.6F);
         }

         if (this.beamIn-- == 0) {
            Vec3 from = this.getEyePosition().add(0.0, -0.25, 0.0);
            BloodArts.piercingBlood(level, this, from, this.lockedAim.subtract(from), 0.75F, this.scale ? 18.0F : 14.0F);
            this.beamCooldown = (this.scale ? 70 : 100) + this.random.nextInt(40);
         }
      } else if (this.novaIn >= 0) {
         if (this.novaIn-- == 0) {
            BloodArts.supernova(level, this, 5.5, this.scale ? 11.0F : 8.0F);
            this.novaCooldown = 90 + this.random.nextInt(40);
         } else {
            BloodArts.convergence(level, this, this.novaIn * 3);
         }
      } else {
         this.beamCooldown--;
         this.novaCooldown--;
         if (this.novaCooldown <= 0 && distance < 4.5) {
            this.novaIn = NOVA_WINDUP;
            this.say("nova");
            this.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 2.0F, 0.5F);
         } else if (this.beamCooldown <= 0 && distance > 5.0 && distance < 34.0 && this.hasLineOfSight(target)) {
            this.beamIn = BEAM_WINDUP;
            this.say("pierce");
            this.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 2.0F, 0.4F);
         }
      }

      if (--this.armorCooldown <= 0 && this.getHealth() < this.getMaxHealth() * 0.4F) {
         this.armorCooldown = 600;
         this.say("armor");
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
         this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 3));
         level.sendParticles(BloodArts.BLOOD_DARK, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.4, 0.8, 0.4, 0.0);
         this.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.5F, 0.6F);
      }

      if (!this.scale && this.getHealth() < this.getMaxHealth() * 0.5F) {
         this.scale = true;
         this.say("scale");
         this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000, 1, false, false));
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12000, 0, false, false));
         level.sendParticles(BloodArts.BLOOD, this.getX(), this.getY() + 1.0, this.getZ(), 60, 0.5, 0.9, 0.5, 0.0);
         this.playSound(SoundEvents.WARDEN_ROAR, 0.8F, 1.6F);
      }

      if (this.scale && this.tickCount % 6 == 0) {
         level.sendParticles(BloodArts.BLOOD, this.getX(), this.getY() + 1.6, this.getZ(), 2, 0.25, 0.2, 0.25, 0.0);
      }
   }

   public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && this.level() instanceof ServerLevel level) {
         BloodArts.splash(level, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), this.getLookAngle(), 6);
      }

      return hit;
   }

   protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
      super.dropCustomDeathLoot(source, looting, hit);
      this.spawnAtLocation(new ItemStack((ItemLike)ModRegistry.DEATH_PAINTING.get()));
      this.spawnAtLocation(new ItemStack(Items.DIAMOND, 2 + this.random.nextInt(3)));
      this.spawnAtLocation(new ItemStack(Items.EMERALD, 10 + this.random.nextInt(10)));
      this.spawnAtLocation(new ItemStack(Items.REDSTONE, 16 + this.random.nextInt(16)));
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("Scale", this.scale);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.scale = tag.getBoolean("Scale");
   }
}
