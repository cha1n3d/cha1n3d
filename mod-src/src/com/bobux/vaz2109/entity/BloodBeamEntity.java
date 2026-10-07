package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.fx.ModParticles;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Piercing Blood as a continuous jet. First the caster compresses a ball of blood between the palms, then a
 * high-pressure stream fires for as long as it is held. The stream has real inertia: it swings toward where the
 * caster looks only slowly, pulses and shakes, pushes the caster back, erodes what it hits and drains the
 * caster's own blood every few ticks.
 */
public class BloodBeamEntity extends Entity {
   public static final double RANGE = 40.0;
   private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(BloodBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(BloodBeamEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(BloodBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(BloodBeamEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(BloodBeamEntity.class, EntityDataSerializers.FLOAT);
   private Vec3 dir = new Vec3(0.0, 0.0, 1.0);
   private float steer = 0.06F;
   private float damage = 3.0F;
   private int maxFire = 140;
   private boolean drain = true;
   private boolean erode = true;
   private int age;
   private final Map<BlockPos, Integer> erosion = new HashMap<>();
   public float yawO;
   public float pitchO;
   public float lengthO;

   public BloodBeamEntity(EntityType<? extends BloodBeamEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   /** charge: ticks of compression before the jet; maxFire: jet ticks; steer: 0..1 how fast it follows the aim. */
   public void setup(LivingEntity owner, int charge, int maxFire, float steer, float damage, boolean drain, boolean erode) {
      this.entityData.set(OWNER, owner.getId());
      this.entityData.set(CHARGE, charge);
      this.maxFire = maxFire;
      this.steer = steer;
      this.damage = damage;
      this.drain = drain;
      this.erode = erode;
      this.dir = owner.getLookAngle();
      this.setDir(this.dir);
      this.follow(owner);
   }

   protected void defineSynchedData() {
      this.entityData.define(OWNER, -1);
      this.entityData.define(CHARGE, 20);
      this.entityData.define(YAW, 0.0F);
      this.entityData.define(PITCH, 0.0F);
      this.entityData.define(LENGTH, 0.0F);
   }

   @Nullable
   public LivingEntity owner() {
      return this.level().getEntity(this.entityData.get(OWNER)) instanceof LivingEntity l ? l : null;
   }

   public int chargeTicks() {
      return this.entityData.get(CHARGE);
   }

   public boolean firing() {
      return this.tickCount > this.chargeTicks();
   }

   public float beamYaw() {
      return this.entityData.get(YAW);
   }

   public float beamPitch() {
      return this.entityData.get(PITCH);
   }

   public float length() {
      return this.entityData.get(LENGTH);
   }

   public int firedTicks() {
      return Math.max(0, this.age - this.chargeTicks());
   }

   public static Vec3 muzzle(LivingEntity owner, float partialTick) {
      Vec3 eye = owner.getEyePosition(partialTick);
      Vec3 look = owner.getViewVector(partialTick);
      return eye.add(look.scale(0.55)).add(0.0, -0.32, 0.0);
   }

   private void setDir(Vec3 d) {
      this.entityData.set(YAW, (float)(Mth.atan2(d.z, d.x) * 180.0F / (float)Math.PI));
      this.entityData.set(PITCH, (float)(Math.asin(Mth.clamp(d.y, -1.0, 1.0)) * 180.0F / (float)Math.PI));
   }

   public static Vec3 direction(float yaw, float pitch) {
      float y = yaw * (float)(Math.PI / 180.0);
      float p = pitch * (float)(Math.PI / 180.0);
      return new Vec3(Mth.cos(y) * Mth.cos(p), Mth.sin(p), Mth.sin(y) * Mth.cos(p));
   }

   private void follow(LivingEntity owner) {
      Vec3 m = muzzle(owner, 1.0F);
      this.setPos(m.x, m.y, m.z);
   }

   public void tick() {
      this.yawO = this.beamYaw();
      this.pitchO = this.beamPitch();
      this.lengthO = this.length();
      super.tick();
      LivingEntity owner = this.owner();
      if (owner == null || !owner.isAlive()) {
         if (!this.level().isClientSide) {
            this.discard();
         }

         return;
      }

      this.follow(owner);
      if (this.level() instanceof ServerLevel level) {
         this.age++;
         if (this.age <= this.chargeTicks()) {
            this.dir = owner.getLookAngle();
            this.setDir(this.dir);
            if (this.age % 5 == 0) {
               level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.9F, 0.4F + this.age * 0.02F);
            }

            if (this.age == this.chargeTicks()) {
               level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.9F);
            }
         } else if (this.firedTicks() > this.maxFire) {
            this.discard();
         } else {
            this.fire(level, owner);
         }
      }
   }

   private Vec3 aim(LivingEntity owner) {
      if (owner instanceof Mob mob && mob.getTarget() != null) {
         LivingEntity t = mob.getTarget();
         return t.position().add(0.0, t.getBbHeight() * 0.5, 0.0).subtract(this.position()).normalize();
      } else {
         return owner.getLookAngle();
      }
   }

   private void fire(ServerLevel level, LivingEntity owner) {
      int t = this.firedTicks();
      Vec3 want = this.aim(owner);
      double wobble = 0.018 + Math.min(0.03, t * 0.0004);
      Vec3 shake = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).scale(wobble);
      Vec3 pulse = new Vec3(-want.z, 0.0, want.x).scale(Math.sin(t * 0.9) * 0.012).add(0.0, Math.cos(t * 1.3) * 0.01, 0.0);
      this.dir = this.dir.add(want.subtract(this.dir).scale(this.steer)).add(shake).add(pulse).normalize();
      this.setDir(this.dir);
      Vec3 from = this.position();
      Vec3 end = from.add(this.dir.scale(RANGE));
      BlockPos hitBlock = null;

      for (double s = 0.3; s <= RANGE; s += 0.2) {
         Vec3 p = from.add(this.dir.scale(s));
         BlockPos pos = BlockPos.containing(p);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock) && !state.getCollisionShape(level, pos).isEmpty()) {
            end = p;
            hitBlock = pos;
            break;
         }
      }

      this.entityData.set(LENGTH, (float)end.distanceTo(from));
      if (hitBlock != null) {
         this.erodeBlock(level, owner, hitBlock, end);
      }

      if (t % 4 == 0) {
         for (LivingEntity e : level.getEntitiesOfClass(
            LivingEntity.class, new AABB(from, end).inflate(0.6), ex -> ex != owner && ex.isAlive() && !ex.isSpectator() && !ex.isAlliedTo(owner)
         )) {
            if (e.getBoundingBox().inflate(0.15).clip(from, end).isPresent()) {
               e.invulnerableTime = 0;
               e.hurt(BossResistance.sorcery(owner), this.damage);
               e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
               e.setDeltaMovement(e.getDeltaMovement().add(this.dir.x * 0.18, 0.02, this.dir.z * 0.18));
               e.hurtMarked = true;
               Vec3 at = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
               level.sendParticles(ModParticles.BLOOD.get(), at.x, at.y, at.z, 10, 0.2, 0.25, 0.2, 0.2);
            }
         }
      }

      level.sendParticles(ModParticles.BLOOD.get(), end.x, end.y, end.z, 3, 0.08, 0.08, 0.08, 0.25);
      if (owner instanceof Player player) {
         player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 2, false, false, false));
         if (t % 3 == 0) {
            Vec3 push = this.dir.scale(-0.07);
            player.setDeltaMovement(player.getDeltaMovement().add(push.x, 0.0, push.z));
            player.hurtMarked = true;
         }
      }

      if (this.drain && t % 4 == 0) {
         if (owner.getHealth() <= 2.0F) {
            this.discard();
            return;
         }

         owner.setHealth(owner.getHealth() - 1.0F);
      }

      if (t % 6 == 0) {
         level.playSound(null, from.x, from.y, from.z, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.7F, 1.8F);
      }

      if (t % 10 == 0) {
         level.playSound(null, from.x, from.y, from.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9F, 1.5F);
      }
   }

   private void erodeBlock(ServerLevel level, LivingEntity owner, BlockPos pos, Vec3 at) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (this.erode && hardness >= 0.0F && hardness <= 2.0F && !state.hasBlockEntity()) {
         int need = 4 + Math.round(hardness * 10.0F);
         int done = this.erosion.merge(pos, 1, Integer::sum);
         level.destroyBlockProgress(this.getId(), pos, Math.min(9, done * 10 / need));
         if (done >= need) {
            boolean allowed = owner instanceof Player player
               ? !MinecraftForge.EVENT_BUS.post(new BreakEvent(level, pos, state, player))
               : level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            this.erosion.remove(pos);
            level.destroyBlockProgress(this.getId(), pos, -1);
            if (allowed) {
               level.destroyBlock(pos, false, owner);
            }
         }
      }
   }

   public void remove(Entity.RemovalReason reason) {
      if (this.level() instanceof ServerLevel level) {
         for (BlockPos pos : this.erosion.keySet()) {
            level.destroyBlockProgress(this.getId(), pos, -1);
         }
      }

      super.remove(reason);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 128.0 * 128.0;
   }

   public boolean isPickable() {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }
}
