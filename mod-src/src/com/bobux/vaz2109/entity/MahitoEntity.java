package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.curse.SukunaVessel;
import com.bobux.vaz2109.quest.BossQuest;
import com.bobux.vaz2109.role.Role;
import com.bobux.vaz2109.role.Roles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Mahito, the curse born of human hatred. Idle Transfiguration: his touch reshapes the soul (damage that ignores
 * armour, withering; weak mobs he touches turn into transfigured humans). He lunges with a blade grown from his
 * arm, throws transfigured humans into the fight, reshapes his own soul to heal — unless the Vessel has hit him:
 * Yuji's blows land on the soul and stop that — and at a third of his health opens his domain, Self-Embodiment
 * of Perfection, which the Vessel at 15+ fingers shrugs off (Sukuna will not allow it).
 */
public class MahitoEntity extends QuestBoss {
   private static final DustParticleOptions SOUL = new DustParticleOptions(new Vector3f(0.45F, 0.62F, 0.78F), 1.4F);
   private static final DustParticleOptions HANDS = new DustParticleOptions(new Vector3f(0.2F, 0.25F, 0.32F), 2.0F);
   private int lungeCooldown = 50;
   private int summonCooldown = 120;
   private int lungeIn = -1;
   private int domainLeft = -1;
   private int soulLocked;
   private boolean healed;
   private boolean domainUsed;

   public MahitoEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, BossQuest.MAHITO, BossBarColor.BLUE, ChatFormatting.DARK_AQUA);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 560.0)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.MOVEMENT_SPEED, 0.33)
         .add(Attributes.ARMOR, 6.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   /** The Vessel's hits wound the soul itself. */
   private static boolean vessel(Entity e) {
      return e instanceof Player p && Roles.is(p, Role.VESSEL);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (vessel(source.getEntity())) {
         amount *= 1.5F;
         this.soulLocked = 200;
         if (this.level() instanceof ServerLevel level && this.random.nextInt(3) == 0) {
            level.sendParticles(SOUL, this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
         }
      }

      return super.hurt(source, amount);
   }

   /** Idle Transfiguration: the soul is reshaped through the touch. */
   public static void transfigure(LivingEntity attacker, LivingEntity target, float soulDamage) {
      if (!(attacker.level() instanceof ServerLevel level)) {
         return;
      }

      target.invulnerableTime = 0;
      target.hurt(attacker.damageSources().indirectMagic(attacker, attacker), soulDamage);
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
      level.sendParticles(SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.3, 0.4, 0.3, 0.0);
      if (attacker instanceof MahitoEntity && !(target instanceof Player) && !BossResistance.isBoss(target) && target.getMaxHealth() <= 40.0F && target.getHealth() <= target.getMaxHealth() * 0.3F
         && !(target instanceof TransfiguredEntity)) {
         TransfiguredEntity human = ModRegistry.TRANSFIGURED.get().create(level);
         if (human != null) {
            human.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0.0F);
            human.finalizeSpawn(level, level.getCurrentDifficultyAt(target.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            target.discard();
            level.addFreshEntity(human);
            level.playSound(null, human.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F);
         }
      }
   }

   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living) {
         transfigure(this, living, 4.0F);
      }

      return hit;
   }

   @Override
   protected void moves(LivingEntity target, double distance) {
      ServerLevel level = (ServerLevel)this.level();
      if (this.soulLocked > 0) {
         this.soulLocked--;
      }

      if (this.domainLeft >= 0) {
         this.domain(level);
         return;
      }

      if (this.lungeIn >= 0) {
         this.getNavigation().stop();
         this.getLookControl().setLookAt(target, 30.0F, 30.0F);
         if (this.lungeIn-- == 0) {
            Vec3 dir = this.toward(target);
            this.setDeltaMovement(dir.x * 1.6, 0.25, dir.z * 1.6);
            this.hurtMarked = true;
            AABB swing = this.getBoundingBox().expandTowards(dir.scale(4.0)).inflate(1.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, swing, e -> e != this && e.isAlive() && !(e instanceof TransfiguredEntity))) {
               e.hurt(this.damageSources().mobAttack(this), 11.0F);
               transfigure(this, e, 2.0F);
            }

            level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + dir.x * 2.0, this.getY() + 1.0, this.getZ() + dir.z * 2.0, 3, 0.6, 0.2, 0.6, 0.0);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
            this.lungeCooldown = 70 + this.random.nextInt(40);
         }
      } else {
         this.lungeCooldown--;
         this.summonCooldown--;
         if (this.lungeCooldown <= 0 && distance > 3.0 && distance < 9.0) {
            this.lungeIn = 12;
            this.say("blade");
            this.playSound(SoundEvents.SLIME_SQUISH, 1.5F, 0.5F);
            level.sendParticles(SOUL, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
         } else if (this.summonCooldown <= 0) {
            this.summon(level, target);
         }
      }

      if (!this.healed && this.getHealth() < this.getMaxHealth() * 0.45F) {
         if (this.soulLocked > 0) {
            if (this.tickCount % 60 == 0) {
               this.say("locked");
            }
         } else {
            this.healed = true;
            this.say("heal");
            this.heal(this.getMaxHealth() * 0.15F);
            level.sendParticles(SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 50, 0.5, 0.9, 0.5, 0.0);
            this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.5F, 0.6F);
         }
      }

      if (!this.domainUsed && this.getHealth() < this.getMaxHealth() * 0.3F) {
         this.domainUsed = true;
         this.domainLeft = 200;
         this.say("domain");
         this.playSound(SoundEvents.WARDEN_EMERGE, 2.0F, 1.2F);
      }
   }

   private void summon(ServerLevel level, LivingEntity target) {
      int alive = level.getEntitiesOfClass(TransfiguredEntity.class, this.getBoundingBox().inflate(32.0)).size();
      this.summonCooldown = 300 + this.random.nextInt(120);
      if (alive >= 6) {
         return;
      }

      this.say("summon");
      for (int i = 0; i < 2 + this.random.nextInt(2); i++) {
         TransfiguredEntity human = ModRegistry.TRANSFIGURED.get().create(level);
         if (human != null) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            human.moveTo(this.getX() + Math.cos(a) * 2.0, this.getY(), this.getZ() + Math.sin(a) * 2.0, (float)Math.toDegrees(a), 0.0F);
            human.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            human.setTarget(target);
            Vec3 throwDir = target.position().subtract(human.position()).normalize();
            human.setDeltaMovement(throwDir.x * 0.9, 0.5, throwDir.z * 0.9);
            level.addFreshEntity(human);
         }
      }

      this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.5F, 0.7F);
   }

   /** Self-Embodiment of Perfection: ten seconds in which every touch of the soul inside lands. */
   private void domain(ServerLevel level) {
      this.getNavigation().stop();
      int left = this.domainLeft--;
      double r = 12.0;
      if (left % 4 == 0) {
         for (int i = 0; i < 16; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double d = this.random.nextDouble() * r;
            level.sendParticles(HANDS, this.getX() + Math.cos(a) * d, this.getY() + 0.2 + this.random.nextDouble() * 3.0, this.getZ() + Math.sin(a) * d, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }

      if (left % 20 == 0) {
         for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), e -> e != this && e.isAlive() && !(e instanceof TransfiguredEntity))) {
            if (e.distanceTo(this) > r || e instanceof Player p && (p.isCreative() || p.isSpectator())) {
               continue;
            }

            if (e instanceof Player p && Roles.is(p, Role.VESSEL) && SukunaVessel.fingers(p) >= 15) {
               p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vaz2109.mahito.sukuna").withStyle(ChatFormatting.DARK_RED), true);
               continue;
            }

            transfigure(this, e, e instanceof Player ? 3.0F : 6.0F);
            e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
         }

         this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.5F, 0.6F);
      }
   }

   protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
      super.dropCustomDeathLoot(source, looting, hit);
      this.spawnAtLocation(new ItemStack(ModRegistry.MAHITO_HAND.get()));
      net.minecraft.world.item.Item finger = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("vaz2109", "sukuna_finger"));
      if (finger != null) {
         this.spawnAtLocation(new ItemStack(finger));
      }
      this.spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + this.random.nextInt(3)));
      this.spawnAtLocation(new ItemStack(Items.ECHO_SHARD, 2 + this.random.nextInt(3)));
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("Healed", this.healed);
      tag.putBoolean("Domain", this.domainUsed);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.healed = tag.getBoolean("Healed");
      this.domainUsed = tag.getBoolean("Domain");
   }
}
