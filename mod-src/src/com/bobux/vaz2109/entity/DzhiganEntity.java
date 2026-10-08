package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.TracerS2C;
import com.bobux.vaz2109.quest.BossQuest;
import com.bobux.vaz2109.quest.BossQuests;
import com.bobux.vaz2109.weapon.GunType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Dzhigan, the rapper from the block, who never goes out without two Makarovs. He keeps his distance and fires from
 * both hands in turn; after sixteen shots he has to reload, and that is the window. Close in and he pistol-whips
 * you and jumps back. Now and then he does a "chorus" — a fan of bullets — and twice he calls his crew of gopniks.
 * At a third of his health the last verse starts: faster fire, faster feet. Bullets stop at blocks, so cover works,
 * and he aims where you were a moment ago, so running sideways saves you.
 */
public class DzhiganEntity extends QuestBoss {
   private static final int CLIP = 8;
   private static final float BULLET = 3.5F;
   private static final double RANGE = 32.0;
   /** He aims where the target was this many ticks ago: strafing makes him miss. */
   private static final int AIM_LAG = 4;
   private final Vec3[] trail = new Vec3[AIM_LAG + 1];
   private int right = CLIP;
   private int left = CLIP;
   private boolean rightNext = true;
   private int shotIn = 20;
   private int reload = -1;
   private int whipCooldown;
   private int sprayCooldown = 160;
   private int spray = -1;
   private int strafeFlip;
   private boolean strafeRight;
   private int crewCalls;
   private boolean lastVerse;

   public DzhiganEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level, BossQuest.DZHIGAN, BossBarColor.YELLOW, ChatFormatting.GOLD);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 320.0)
         .add(Attributes.ATTACK_DAMAGE, 7.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
         .add(Attributes.FOLLOW_RANGE, 40.0);
   }

   /** No melee goal: he shoots, and walks on his own in {@link #moves}. */
   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, GopnikEntity.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, p -> p instanceof Player pl && BossQuests.mayFight(pl, BossQuest.DZHIGAN)));
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
      SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
      this.setLeftHanded(false);
      Item pm = pm();
      if (pm != null) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pm));
         this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(pm));
      }

      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
      return out;
   }

   @Nullable
   private static Item pm() {
      return ForgeRegistries.ITEMS.getValue(new ResourceLocation("vaz2109", "pm"));
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.getTarget() == null || !this.getTarget().isAlive()) {
         this.setAggressive(false);
         this.spray = -1;
      }
   }

   @Override
   protected void moves(LivingEntity target, double distance) {
      ServerLevel level = (ServerLevel)this.level();
      this.setAggressive(true);
      System.arraycopy(this.trail, 0, this.trail, 1, AIM_LAG);
      this.trail[0] = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
      boolean sees = this.hasLineOfSight(target);
      this.face(target);
      this.walk(target, distance, sees);
      if (this.whipCooldown > 0) {
         this.whipCooldown--;
      }

      if (this.sprayCooldown > 0) {
         this.sprayCooldown--;
      }

      if (distance < 2.4 && this.whipCooldown <= 0) {
         this.whip(level, target);
      }

      if (this.spray >= 0) {
         // the chorus: a fast fan of bullets that does not touch the clips
         if (this.spray-- % 2 == 0 && sees) {
            this.shoot(level, target, this.spray % 4 == 0, 0.11);
         }

         if (this.spray < 0) {
            this.sprayCooldown = 220 + this.random.nextInt(80);
         }

         return;
      }

      if (this.reload >= 0) {
         if (this.reload == 30) {
            this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8F, 1.7F);
         }

         if (this.reload-- == 0) {
            this.right = this.left = CLIP;
            this.playSound(SoundEvents.IRON_DOOR_CLOSE, 0.7F, 1.9F);
            this.shotIn = 6;
         }

         return;
      }

      if (this.sprayCooldown <= 0 && sees && distance < 16.0 && this.getHealth() < this.getMaxHealth() * 0.75F) {
         this.spray = 20;
         this.say("spray");
         this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 2.0F, 0.6F);
         return;
      }

      if (--this.shotIn <= 0 && sees && distance < RANGE) {
         boolean hand = this.rightNext && this.right > 0 || this.left <= 0;
         if (hand) {
            this.right--;
         } else {
            this.left--;
         }

         this.rightNext = !hand;
         this.shoot(level, target, hand, this.lastVerse ? 0.05 : 0.04);
         this.shotIn = this.lastVerse ? 4 : 6;
         if (this.right <= 0 && this.left <= 0) {
            this.reload = this.lastVerse ? 34 : 50;
            this.playSound(SoundEvents.IRON_TRAPDOOR_OPEN, 0.8F, 1.6F);
            if (this.random.nextInt(3) == 0) {
               this.say("reload");
            }
         }
      }

      this.phases(level, target);
   }

   private void face(LivingEntity target) {
      this.getLookControl().setLookAt(target, 60.0F, 60.0F);
      Vec3 d = target.position().subtract(this.position());
      float yaw = (float)(Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
      this.setYRot(yaw);
      this.yBodyRot = yaw;
      this.yHeadRot = yaw;
   }

   /** Keeps 6 to 12 blocks from the target, strafing; walks up when he cannot see it. */
   private void walk(LivingEntity target, double distance, boolean sees) {
      if (!sees || distance > 14.0) {
         this.getNavigation().moveTo(target, 1.15);
         return;
      }

      this.getNavigation().stop();
      if (--this.strafeFlip <= 0) {
         this.strafeFlip = 25 + this.random.nextInt(35);
         this.strafeRight = this.random.nextBoolean();
      }

      float forward = distance < 6.0 ? -0.6F : (distance > 12.0 ? 0.5F : 0.0F);
      float speed = this.lastVerse ? 0.75F : 0.55F;
      this.getMoveControl().strafe(forward, this.strafeRight ? speed : -speed);
   }

   /** Up close: the butt of the pistol, then a jump back out of reach. */
   private void whip(ServerLevel level, LivingEntity target) {
      this.whipCooldown = 50;
      this.swing(this.random.nextBoolean() ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
      Vec3 dir = this.toward(target);
      target.hurt(this.damageSources().mobAttack(this), (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
      target.knockback(1.1, -dir.x, -dir.z);
      this.setDeltaMovement(-dir.x * 1.1, 0.42, -dir.z * 1.1);
      this.hurtMarked = true;
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.3, this.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
      this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
      if (this.random.nextInt(3) == 0) {
         this.say("back");
      }
   }

   private Vec3 muzzle(boolean rightHand) {
      double yaw = Math.toRadians(this.yBodyRot);
      double fx = -Math.sin(yaw);
      double fz = Math.cos(yaw);
      double side = rightHand ? -0.36 : 0.36;
      return this.position().add(fx * 0.7 + fz * side, 1.38, fz * 0.7 - fx * side);
   }

   /** One hit-scan bullet, like the player's PM: stopped by blocks, shown with the mod's tracer. */
   private void shoot(ServerLevel level, LivingEntity target, boolean rightHand, double spread) {
      Vec3 from = this.muzzle(rightHand);
      Vec3 aim = this.trail[AIM_LAG] != null ? this.trail[AIM_LAG] : this.trail[0];
      Vec3 dir = aim.subtract(from).normalize()
         .add(this.random.nextGaussian() * spread, this.random.nextGaussian() * spread, this.random.nextGaussian() * spread)
         .normalize();
      Vec3 end = from.add(dir.scale(RANGE + 4.0));
      BlockHitResult block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
      byte kind = block.getType() == HitResult.Type.MISS ? TracerS2C.HIT_NONE : TracerS2C.HIT_BLOCK;
      var through = target.getBoundingBox().inflate(0.1).clip(from, stop);
      if (through.isPresent()) {
         stop = through.get();
         kind = TracerS2C.HIT_ENTITY;
         DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModRegistry.BULLET_DAMAGE), this, this);
         target.invulnerableTime = 0;
         target.hurt(source, BULLET);
      }

      BlockPos pos = kind == TracerS2C.HIT_BLOCK ? block.getBlockPos() : BlockPos.ZERO;
      Network.CHANNEL.send(
         PacketDistributor.TRACKING_ENTITY.with(() -> this),
         new TracerS2C(this.getId(), GunType.PM, from, stop, kind, kind == TracerS2C.HIT_BLOCK ? block.getDirection() : null, pos)
      );
      this.swing(rightHand ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND);
      level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 2, 0.03, 0.03, 0.03, 0.01);
      sound(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, 1.5F);
      sound(level, from, SoundEvents.GENERIC_EXPLODE, 0.25F, 2.0F);
   }

   private void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
      level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch * (0.95F + this.random.nextFloat() * 0.1F));
   }

   private void phases(ServerLevel level, LivingEntity target) {
      float hp = this.getHealth() / this.getMaxHealth();
      if (this.crewCalls == 0 && hp < 0.6F || this.crewCalls == 1 && hp < 0.3F) {
         this.crewCalls++;
         this.crew(level, target);
      }

      if (!this.lastVerse && hp < 0.35F) {
         this.lastVerse = true;
         this.say("phase2");
         this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 2.5F, 0.5F);
         level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.2, this.getZ(), 12, 0.8, 0.4, 0.8, 1.0);
      }

      if (this.lastVerse) {
         if (!this.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
         }

         if (this.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.2, this.getZ(), 1, 0.4, 0.2, 0.4, 1.0);
         }
      }
   }

   /** His crew from the block: two or three gopniks with bats who go straight for the target. */
   private void crew(ServerLevel level, LivingEntity target) {
      this.say("crew");
      int alive = level.getEntitiesOfClass(GopnikEntity.class, this.getBoundingBox().inflate(32.0)).size();
      for (int i = alive; i < Math.min(4, alive + 2 + this.random.nextInt(2)); i++) {
         GopnikEntity mate = ModRegistry.GOPNIK.get().create(level);
         if (mate == null) {
            continue;
         }

         double a = this.random.nextDouble() * Math.PI * 2.0;
         BlockPos at = BlockPos.containing(this.getX() + Math.cos(a) * 3.0, this.getY(), this.getZ() + Math.sin(a) * 3.0);
         mate.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, (float)Math.toDegrees(a), 0.0F);
         mate.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null, null);
         mate.setTarget(target);
         level.addFreshEntity(mate);
         level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mate.getX(), mate.getY() + 0.5, mate.getZ(), 4, 0.3, 0.4, 0.3, 0.01);
      }

      this.playSound(SoundEvents.VINDICATOR_CELEBRATE, 1.5F, 0.8F);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      // his own crew's bats do not count
      return !(source.getEntity() instanceof GopnikEntity) && super.hurt(source, amount);
   }

   protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
      super.dropCustomDeathLoot(source, looting, hit);
      Item pm = pm();
      if (pm != null) {
         for (int i = 0; i < 2; i++) {
            ItemStack gun = new ItemStack(pm);
            gun.setHoverName(Component.translatable("item.vaz2109.pm.dzhigan").withStyle(ChatFormatting.GOLD));
            this.spawnAtLocation(gun);
         }
      }

      Item ammo = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vaz2109", "ammo_9x18"));
      if (ammo != null) {
         this.spawnAtLocation(new ItemStack(ammo, 24 + this.random.nextInt(17)));
      }

      Item money = ForgeRegistries.ITEMS.getValue(new ResourceLocation("vaz2109", "banknote_1000"));
      if (money != null) {
         this.spawnAtLocation(new ItemStack(money, 1 + this.random.nextInt(2)));
      }

      this.spawnAtLocation(new ItemStack(Items.GOLD_INGOT, 6 + this.random.nextInt(5)));
      this.spawnAtLocation(new ItemStack(Items.DIAMOND, 2 + this.random.nextInt(2)));
      this.spawnAtLocation(new ItemStack(Items.MUSIC_DISC_PIGSTEP));
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("CrewCalls", this.crewCalls);
      tag.putBoolean("LastVerse", this.lastVerse);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.crewCalls = tag.getInt("CrewCalls");
      this.lastVerse = tag.getBoolean("LastVerse");
   }
}
