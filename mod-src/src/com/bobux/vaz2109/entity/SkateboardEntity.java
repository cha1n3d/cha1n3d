package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.block.SkateRampBlock;
import com.bobux.vaz2109.client.SkateClient;
import com.bobux.vaz2109.fx.ModParticles;
import com.bobux.vaz2109.item.SkateboardItem;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.SkateC2S;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class SkateboardEntity extends Entity {
   private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SkateboardEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_FLIP_TIME = SynchedEntityData.defineId(SkateboardEntity.class, EntityDataSerializers.INT);
   public static final float PUSH_SPEED = 0.45F;
   private static final float PUSH = 0.1F;
   private static final float MAX_SPEED = 1.6F;
   private static final float OLLIE = 0.42F;
   public static final int FLIP_TICKS = 9;
   private static final double DECK = 0.155;
   private float inputForward;
   private float inputTurn;
   private boolean inputJump;
   private boolean prevJump;
   private float speed;
   private double velX;
   private double velZ;
   private double slip;
   private boolean skidding;
   private int pushCooldown;
   public int pushTicks;
   private boolean airborne;
   private int airTicks;
   private double airPeak;
   private double airVy;
   private float airSpin;
   private int flipAge = -1;
   private boolean grinding;
   private float deltaYaw;
   private boolean wasOnRamp;
   private double rampVy;
   public float visualPitch;
   public float visualPitchO;
   public float pop;
   public float popO;
   public int visualPush;
   public int visualPushO;
   public boolean visualGrind;
   public float visualLean;
   public float visualLeanO;
   private double lastMoved;
   private boolean soundStarted;
   private ItemStack displayStack = ItemStack.EMPTY;
   private int displayColor = Integer.MIN_VALUE;
   private int lerpSteps;
   private double lerpX;
   private double lerpY;
   private double lerpZ;
   private double lerpYRot;
   private static final double[][] FOOTPRINT = new double[][]{{0.0, 0.0}, {0.35, 0.0}, {-0.35, 0.0}, {0.0, 0.35}, {0.0, -0.35}};

   public SkateboardEntity(EntityType<? extends SkateboardEntity> type, Level level) {
      super(type, level);
      this.setMaxUpStep(0.55F);
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_COLOR, -1);
      this.entityData.define(DATA_FLIP_TIME, -1000);
   }

   public int getColor() {
      return (Integer)this.entityData.get(DATA_COLOR);
   }

   public void setColor(int color) {
      this.entityData.set(DATA_COLOR, color);
   }

   public ItemStack displayStack() {
      if (this.displayColor != this.getColor() || this.displayStack.isEmpty()) {
         this.displayColor = this.getColor();
         this.displayStack = SkateboardItem.withColor(this.displayColor);
      }

      return this.displayStack;
   }

   /** A kick scooter rides on the same physics with handlebar steering, a rear brake and no flip tricks. */
   protected boolean scooter() {
      return false;
   }

   public boolean isSkidding() {
      return this.skidding;
   }

   public void setInput(float forward, float turn, boolean jump) {
      this.inputForward = forward;
      this.inputTurn = turn;
      this.inputJump = jump;
   }

   public float getSpeed() {
      return this.isControlledByLocalInstance()
         ? (this.airborne ? (float)Math.sqrt(this.velX * this.velX + this.velZ * this.velZ) : this.speed)
         : (float)Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo) + (this.getZ() - this.zo) * (this.getZ() - this.zo));
   }

   public boolean isGrinding() {
      return this.grinding;
   }

   public void markFlip() {
      this.entityData.set(DATA_FLIP_TIME, (int)this.level().getGameTime());
   }

   public float flipAngle(float partialTick) {
      if (this.isControlledByLocalInstance()) {
         return this.flipAge < 0 ? 0.0F : Math.min(1.0F, ((float)this.flipAge + partialTick) / 9.0F) * 360.0F;
      } else {
         float age = (float)((int)this.level().getGameTime() - (Integer)this.entityData.get(DATA_FLIP_TIME)) + partialTick;
         return age >= 0.0F && age < 9.0F ? age / 9.0F * 360.0F : 0.0F;
      }
   }

   public void tick() {
      super.tick();
      this.tickLerp();
      this.popO = this.pop;
      this.pop = Math.max(0.0F, this.pop - 0.2F);
      if (this.isControlledByLocalInstance()) {
         if (!(this.getControllingPassenger() instanceof Player)) {
            this.setInput(0.0F, 0.0F, false);
         }

         this.ride(this.getControllingPassenger() instanceof Player player ? player : null);
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      this.checkInsideBlocks();
      if (this.level().isClientSide) {
         this.clientEffects();
      }
   }

   @Nullable
   private SkateboardEntity.Ramp rampUnder() {
      for (double[] o : FOOTPRINT) {
         double x = this.getX() + o[0];
         double z = this.getZ() + o[1];

         for (double dy : new double[]{-0.05, 0.05}) {
            BlockPos pos = BlockPos.containing(x, this.getY() + dy, z);
            BlockState state = this.level().getBlockState(pos);
            if (state.getBlock() instanceof SkateRampBlock ramp) {
               Direction facing = (Direction)state.getValue(SkateRampBlock.FACING);
               double surface = (double)pos.getY() + ramp.height(facing, x - (double)pos.getX(), z - (double)pos.getZ());
               if (Math.abs(this.getY() - surface) <= 0.3) {
                  return new SkateboardEntity.Ramp(facing, ramp.rise, surface);
               }
            }
         }
      }

      return null;
   }

   private void ride(@Nullable Player rider) {
      boolean ridden = rider != null;
      float yawBefore = this.getYRot();
      SkateboardEntity.Ramp ramp = this.rampUnder();
      BlockPos centre = BlockPos.containing(this.getX(), this.getY() - 0.05, this.getZ());
      boolean launch = ramp == null
         && this.wasOnRamp
         && this.rampVy > 0.06
         && this.level().getBlockState(centre).getCollisionShape(this.level(), centre).isEmpty();
      this.wasOnRamp = ramp != null;
      if (launch) {
         this.airborne = true;
         this.airTicks = 0;
         this.airPeak = this.getY();
         this.airSpin = 0.0F;
         this.flipAge = -1;
         this.pop = 0.5F;
         this.setDeltaMovement(this.velX, this.rampVy, this.velZ);
         this.sound(SoundEvents.WOOD_PLACE, 0.6F, 1.2F);
      }

      boolean ground = !launch && (this.onGround() || ramp != null);
      BlockPos below = this.getBlockPosBelowThatAffectsMyMovement();
      BlockState floor = this.level().getBlockState(below);
      this.grinding = ground && this.speed > 0.08F && this.grindable();
      float roll = this.grinding ? 0.997F : (ground ? this.rolling(floor, below) : 1.0F);
      boolean rough = roll < 0.99F;
      boolean jumpPressed = this.inputJump && !this.prevJump;
      this.prevJump = this.inputJump;
      double vy = this.getDeltaMovement().y;
      if (ground) {
         boolean scooter = this.scooter();
         float before = this.speed;
         if (ridden) {
            float limit = scooter ? (rough ? 0.6F : 1.1F) : (rough ? 0.55F : 1.0F);
            if (this.inputForward > 0.0F && !this.grinding) {
               if (this.pushCooldown == 0 && this.speed < limit) {
                  float kick = (scooter ? 0.17F : 0.18F) * Math.max(0.3F, 1.0F - this.speed / limit);
                  this.speed = Math.min(limit, this.speed + kick);
                  this.pushCooldown = scooter ? 11 : 9;
                  this.pushTicks = 8;
                  this.sound(SoundEvents.STONE_STEP, 0.25F, scooter ? 1.1F : 1.3F);
               }
            } else if (this.inputForward < 0.0F) {
               this.speed = Math.max(0.0F, this.speed - (scooter ? 0.035F : 0.02F));
               if (this.speed > 0.05F && this.tickCount % 3 == 0) {
                  this.sound(scooter ? SoundEvents.GRINDSTONE_USE : SoundEvents.GRAVEL_STEP, scooter ? 0.12F : 0.2F, 1.4F);
               }
            }

            if (!this.grinding) {
               float turn;
               if (scooter) {
                  float rate = this.speed < 0.03F ? 5.0F : 5.5F / (1.0F + this.speed * 2.0F) + 0.8F;
                  turn = this.inputTurn * rate;
               } else {
                  float rate = this.speed < 0.05F ? 10.0F : 6.5F / (1.0F + this.speed * 3.0F) + 1.5F;
                  float diff = Mth.wrapDegrees(rider.getYRot() - this.getYRot());
                  turn = Math.abs(diff) < 100.0F ? Mth.clamp(diff, -rate, rate) : 0.0F;
                  turn += this.inputTurn * rate;
               }

               this.setYRot(this.getYRot() + turn);
            }
         }

         float yaw = this.getYRot() * (float) (Math.PI / 180.0);
         double hx = (double)(-Mth.sin(yaw));
         double hz = (double)Mth.cos(yaw);
         double sx = (double)Mth.cos(yaw);
         double sz = (double)Mth.sin(yaw);
         double carried = Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
         if (carried > 0.01 && !this.grinding) {
            // The board keeps its momentum; the wheels only bend it toward the deck's heading.
            double along = this.velX * hx + this.velZ * hz;
            double lat = this.velX * sx + this.velZ * sz;
            double angle = Math.atan2(lat, Math.abs(along));
            double grip = scooter ? 0.65 : 0.55;
            this.skidding = Math.abs(angle) > Math.toRadians(scooter ? 30.0 : 24.0) && carried > 0.22;
            if (this.skidding) {
               grip = 0.12;
               carried *= 0.965;
               if (this.tickCount % 3 == 0) {
                  this.sound(SoundEvents.GRAVEL_STEP, 0.35F, 1.7F);
               }
            } else {
               carried *= 1.0 - 0.015 * Math.abs(angle);
            }

            double kept = angle * (1.0 - grip);
            this.speed = Math.max(0.0F, (float)(Math.cos(kept) * carried) + (this.speed - before));
            this.slip = Math.sin(kept) * carried;
         } else {
            this.skidding = false;
            this.slip = 0.0;
         }

         this.speed = Math.min(1.6F, this.speed * roll);
         this.speed -= 0.007F * this.speed * this.speed;
         this.slip *= roll;
         if (this.isInWater()) {
            this.speed *= 0.7F;
            this.slip *= 0.7;
         }

         if (this.speed < 0.003F) {
            this.speed = 0.0F;
         }

         this.velX = hx * (double)this.speed + sx * this.slip;
         this.velZ = hz * (double)this.speed + sz * this.slip;
         if (ridden && jumpPressed) {
            this.ollie();
            vy = scooter ? (double)(0.34F + this.speed * 0.1F) : (double)(0.42F + this.speed * 0.15F);
         } else {
            vy = -0.08;
         }

         if (ramp != null) {
            vy = this.rideSlope(ramp, ridden && jumpPressed, vy);
            this.slip = 0.0;
         }
      } else {
         this.airTicks++;
         if (ridden && this.airborne) {
            if (jumpPressed && this.flipAge < 0 && this.airTicks > 1 && !this.scooter()) {
               this.flipAge = 0;
               this.sound(SoundEvents.WOOD_HIT, 0.5F, 1.7F);
               Network.CHANNEL.sendToServer(new SkateC2S(this.getId(), (byte)1, 0));
            }

            if (this.inputTurn != 0.0F) {
               float spin = this.inputTurn * (this.scooter() ? 4.0F : 15.0F);
               this.setYRot(this.getYRot() + spin);
               this.airSpin += spin;
            }
         }

         this.airborne = true;
         this.airPeak = Math.max(this.airPeak, this.getY());
         vy = (vy - 0.08) * 0.98;
         this.velX *= 0.995;
         this.velZ *= 0.995;
      }

      if (this.flipAge >= 0 && this.flipAge < 9) {
         this.flipAge++;
      }

      if (this.pushCooldown > 0) {
         this.pushCooldown--;
      }

      if (this.pushTicks > 0) {
         this.pushTicks--;
      }

      this.airVy = vy;
      double y0 = this.getY();
      this.setDeltaMovement(this.velX, vy, this.velZ);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.deltaYaw = Mth.wrapDegrees(this.getYRot() - yawBefore);
      if (this.onGround() && this.airborne && this.airTicks > 1) {
         this.land(ridden);
      } else if (this.onGround()) {
         this.airborne = false;
         this.airTicks = 0;
         if (this.getY() > y0 + 0.2 && vy <= 0.0 && ramp == null) {
            if (ridden && this.speed > 0.6F) {
               this.bail(1);
            } else {
               this.speed *= 0.45F;
               this.sound(SoundEvents.WOOD_HIT, 0.5F, 0.8F);
            }
         }
      }

      if (this.horizontalCollision && ramp == null) {
         double was = Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
         if (ridden && was > 0.42 && !this.grinding) {
            this.bail(2);
         }

         this.speed *= 0.2F;
         this.velX *= 0.2;
         this.velZ *= 0.2;
      }

      if (ridden && this.grinding && this.level().isClientSide) {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkateClient.grind(this));
      }
   }

   private double rideSlope(SkateboardEntity.Ramp ramp, boolean ollie, double vy) {
      double fx = (double)ramp.facing().getStepX();
      double fz = (double)ramp.facing().getStepZ();
      float yaw = this.getYRot() * (float) (Math.PI / 180.0);
      double hx = (double)(-Mth.sin(yaw));
      double hz = (double)Mth.cos(yaw);
      double up = hx * fx + hz * fz;
      double sin = (double)ramp.rise() / Math.sqrt((double)(1.0F + ramp.rise() * ramp.rise()));
      this.speed -= (float)(0.04 * sin * up);
      if (this.speed < 0.0F) {
         this.setYRot(this.getYRot() + 180.0F);
         this.speed = -this.speed;
         hx = -hx;
         hz = -hz;
      }

      this.speed = Math.min(1.6F, this.speed);
      this.velX = hx * (double)this.speed;
      this.velZ = hz * (double)this.speed;
      this.rampVy = (this.velX * fx + this.velZ * fz) * (double)ramp.rise();
      if (ollie) {
         return vy + Math.max(0.0, this.rampVy);
      } else {
         double nx = this.getX() + this.velX;
         double nz = this.getZ() + this.velZ;
         double rest = Math.max(
            this.groundHeight(nx, nz), Math.max(this.groundHeight(nx + fx * 0.35, nz + fz * 0.35), this.groundHeight(nx - fx * 0.35, nz - fz * 0.35))
         );
         return Mth.clamp(rest - this.getY() + 0.002, -0.6, 1.0);
      }
   }

   private double groundHeight(double x, double z) {
      for (double dy : new double[]{0.9, -0.1, -1.1}) {
         BlockPos pos = BlockPos.containing(x, this.getY() + dy, z);
         BlockState state = this.level().getBlockState(pos);
         if (state.getBlock() instanceof SkateRampBlock ramp) {
            return (double)pos.getY() + ramp.height((Direction)state.getValue(SkateRampBlock.FACING), x - (double)pos.getX(), z - (double)pos.getZ());
         }

         VoxelShape shape = state.getCollisionShape(this.level(), pos);
         if (!shape.isEmpty()) {
            return (double)pos.getY() + shape.max(Axis.Y);
         }
      }

      return this.getY() - 0.08;
   }

   private void ollie() {
      this.pop = 1.0F;
      this.airborne = true;
      this.airTicks = 0;
      this.airPeak = this.getY();
      this.airSpin = 0.0F;
      this.flipAge = -1;
      this.sound(SoundEvents.WOOD_PLACE, 0.7F, 1.4F);
      this.sound((SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), 0.4F, 0.8F);
   }

   private void land(boolean ridden) {
      this.airborne = false;
      int air = this.airTicks;
      this.airTicks = 0;
      double horizontal = Math.sqrt(this.velX * this.velX + this.velZ * this.velZ);
      boolean flipped = this.flipAge >= 0;
      boolean flipDone = this.flipAge >= 8;
      this.flipAge = -1;
      double fall = this.airPeak - this.getY();
      if (!ridden) {
         this.speed = (float)horizontal;
      } else {
         float yaw = this.getYRot() * (float) (Math.PI / 180.0);
         double along = horizontal > 0.01 ? ((double)(-Mth.sin(yaw)) * this.velX + (double)Mth.cos(yaw) * this.velZ) / horizontal : 1.0;
         boolean straight = horizontal < 0.05 || Math.abs(along) > Math.cos(Math.toRadians(40.0));
         if ((!flipped || flipDone) && straight && !(fall > 4.5)) {
            if (along < 0.0) {
               this.setYRot(this.getYRot() + 180.0F);
            }

            this.speed = (float)Math.min(1.6F, horizontal * (0.97 - Math.min(0.15, fall * 0.03)));
            this.slip = 0.0;
            this.sound(SoundEvents.WOOD_FALL, 0.8F, 0.9F);
            this.sound((SoundEvent)SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.5F, 0.7F);
            if (this.level().isClientSide) {
               int halfTurns = Math.round(Math.abs(this.airSpin) / 180.0F);
               DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkateClient.landed(air, flipped, halfTurns));
            }
         } else {
            this.bail((int)Math.max(2.0, fall - 2.0));
         }
      }
   }

   private void bail(int damage) {
      this.flipAge = -1;
      this.airborne = false;
      this.sound(SoundEvents.WOOD_BREAK, 0.8F, 1.2F);
      if (this.level().isClientSide && this.getControllingPassenger() instanceof Player) {
         Network.CHANNEL.sendToServer(new SkateC2S(this.getId(), (byte)2, damage));
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkateClient.bailed());
      }

      this.speed *= 0.4F;
   }

   private void sound(SoundEvent sound, float volume, float pitch) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
   }

   private float rolling(BlockState s, BlockPos pos) {
      if (s.getFriction(this.level(), pos, this) > 0.8F) {
         return 0.998F;
      } else if (s.is(Blocks.SOUL_SAND) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.HONEY_BLOCK) || s.is(Blocks.SLIME_BLOCK) || s.is(BlockTags.WOOL)) {
         return 0.75F;
      } else {
         return !s.is(BlockTags.DIRT)
               && !s.is(BlockTags.SAND)
               && !s.is(Blocks.GRAVEL)
               && !s.is(BlockTags.SNOW)
               && !s.is(Blocks.FARMLAND)
               && !s.is(BlockTags.LEAVES)
               && !s.is(Blocks.MOSS_BLOCK)
               && !s.is(Blocks.CLAY)
            ? 0.996F
            : 0.975F;
      }
   }

   private boolean grindable() {
      if (this.scooter()) {
         return false;
      }

      BlockState under = this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.55, this.getZ()));
      return !under.is(BlockTags.FENCES)
            && !under.is(BlockTags.WALLS)
            && !(under.getBlock() instanceof IronBarsBlock)
            && !(under.getBlock() instanceof ChainBlock)
         ? this.level().getBlockState(this.blockPosition()).getBlock() instanceof BaseRailBlock
         : true;
   }

   private void clientEffects() {
      if (this.getY() - this.yo > 0.08 && !this.isControlledByLocalInstance()) {
         this.pop = 1.0F;
      }

      if (!this.soundStarted) {
         this.soundStarted = true;
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkateClient.startRollingSound(this));
      }

      double moved = Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo) + (this.getZ() - this.zo) * (this.getZ() - this.zo));
      this.visualPushO = this.visualPush;
      if (this.isControlledByLocalInstance() && this.pushTicks > 0) {
         this.visualPush = this.pushTicks;
      } else if (this.onGround() && moved - this.lastMoved > 0.035) {
         this.visualPush = 8;
      } else if (this.visualPush > 0) {
         this.visualPush--;
      }

      this.lastMoved = moved;
      this.visualGrind = this.onGround() && moved > 0.08 && this.grindable();
      this.visualPitchO = this.visualPitch;
      SkateboardEntity.Ramp ramp = this.rampUnder();
      float pitch = 0.0F;
      if (ramp != null) {
         float yaw = this.getYRot() * (float) (Math.PI / 180.0);
         double up = (double)(-Mth.sin(yaw) * (float)ramp.facing().getStepX() + Mth.cos(yaw) * (float)ramp.facing().getStepZ());
         pitch = (float)(Math.toDegrees(Math.atan((double)ramp.rise())) * up);
      } else if (!this.onGround()) {
         pitch = this.visualPitch * 0.9F;
      }

      this.visualPitch = this.visualPitch + (pitch - this.visualPitch) * 0.4F;
      this.visualLeanO = this.visualLean;
      float turn = Mth.wrapDegrees(this.getYRot() - this.yRotO);
      this.visualLean = this.visualLean + (Mth.clamp(turn * 0.05F, -0.35F, 0.35F) - this.visualLean) * 0.3F;
      if (this.skidding && this.isControlledByLocalInstance()) {
         this.level().addParticle((ParticleOptions)ModParticles.SMOKE.get(), this.getX(), this.getY() + 0.05, this.getZ(), 0.0, 0.02, 0.0);
      }

      if (this.visualGrind) {
         for (int i = 0; i < 2; i++) {
            this.level()
               .addParticle(
                  (ParticleOptions)ModParticles.SPARK.get(),
                  this.getX(),
                  this.getY() + 0.05,
                  this.getZ(),
                  (this.random.nextDouble() - 0.5) * 0.2,
                  0.1 + this.random.nextDouble() * 0.1,
                  (this.random.nextDouble() - 0.5) * 0.2
               );
         }
      }
   }

   public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
      this.lerpX = x;
      this.lerpY = y;
      this.lerpZ = z;
      this.lerpYRot = (double)yRot;
      this.lerpSteps = steps;
   }

   private void tickLerp() {
      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         double x = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
         double y = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
         double z = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
         double yaw = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
         this.setYRot(this.getYRot() + (float)yaw / (float)this.lerpSteps);
         this.lerpSteps--;
         this.setPos(x, y, z);
         this.setRot(this.getYRot(), this.getXRot());
      }
   }

   public boolean isControlledByLocalInstance() {
      return this.getControllingPassenger() instanceof Player player ? player.isLocalPlayer() : !this.level().isClientSide;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      return this.getFirstPassenger() instanceof Player player ? player : null;
   }

   protected boolean canAddPassenger(Entity passenger) {
      return this.getPassengers().isEmpty();
   }

   public boolean shouldRiderSit() {
      return false;
   }

   protected void positionRider(Entity passenger, MoveFunction callback) {
      if (this.hasPassenger(passenger)) {
         callback.accept(passenger, this.getX(), this.getY() + (this.scooter() ? 0.13 : 0.155), this.getZ());
         this.clampRotation(passenger);
      }
   }

   private void clampRotation(Entity passenger) {
      float body = this.getYRot() + (this.scooter() ? 0.0F : 90.0F);
      passenger.setYBodyRot(body);
      float rel = Mth.wrapDegrees(passenger.getYRot() - body);
      float clamped = Mth.clamp(rel, this.scooter() ? -110.0F : -125.0F, this.scooter() ? 110.0F : 125.0F);
      passenger.yRotO += clamped - rel;
      passenger.setYRot(passenger.getYRot() + clamped - rel);
      passenger.setYHeadRot(passenger.getYRot());
   }

   public void onPassengerTurned(Entity passenger) {
      this.clampRotation(passenger);
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (player.isSecondaryUseActive()) {
         if (!this.level().isClientSide && !this.isVehicle()) {
            ItemStack item = this.displayStack().copy();
            if (!player.getInventory().add(item)) {
               player.drop(item, false);
            }

            this.discard();
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (this.isVehicle()) {
         return InteractionResult.PASS;
      } else if (!this.level().isClientSide) {
         return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source) || this.level().isClientSide || this.isRemoved()) {
         return false;
      } else if (source.getEntity() != null && this.hasPassenger(source.getEntity())) {
         return false;
      } else {
         boolean var10000;
         label27: {
            if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
               var10000 = true;
               break label27;
            }

            var10000 = false;
         }

         boolean creative = var10000;
         if (!creative && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(this.displayStack().copy());
         }

         this.ejectPassengers();
         this.discard();
         return true;
      }
   }

   public ItemStack getPickResult() {
      return this.displayStack().copy();
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   protected MovementEmission getMovementEmission() {
      return MovementEmission.EVENTS;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.setColor(tag.contains("Color") ? tag.getInt("Color") : -1);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.getColor() >= 0) {
         tag.putInt("Color", this.getColor());
      }
   }

   private static record Ramp(Direction facing, float rise, double surface) {
   }
}
