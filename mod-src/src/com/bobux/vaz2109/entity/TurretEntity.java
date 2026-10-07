package com.bobux.vaz2109.entity;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.TracerS2C;
import com.bobux.vaz2109.weapon.GunType;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** Mechanic's home-made sentry: a PM welded onto a box of car parts. Lives 20 seconds and burns the owner's 9x18 ammo. */
public class TurretEntity extends Entity {
   public static final int LIFETIME = 400;
   private static final double RANGE = 18.0;
   private static final EntityDataAccessor<Float> AIM = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
   @Nullable
   private UUID owner;
   private int age;
   private int cooldown = 10;
   private float health = 24.0F;
   private boolean warned;

   public TurretEntity(EntityType<? extends TurretEntity> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData() {
      this.entityData.define(AIM, 0.0F);
   }

   public void setOwner(Player player) {
      this.owner = player.getUUID();
   }

   public float aim() {
      return this.entityData.get(AIM);
   }

   @Nullable
   private Player owner() {
      return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
   }

   public void tick() {
      super.tick();
      if (!this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.98, 0.5));
      if (this.level() instanceof ServerLevel level) {
         if (++this.age > LIFETIME) {
            this.fold(level);
            return;
         }

         LivingEntity target = this.findTarget(level);
         if (target != null) {
            Vec3 to = target.getEyePosition().subtract(this.muzzle());
            float yaw = (float)(Mth.atan2(to.z, to.x) * 180.0F / (float)Math.PI) - 90.0F;
            this.entityData.set(AIM, Mth.approachDegrees(this.aim(), yaw, 25.0F));
            this.setYRot(this.aim());
            if (--this.cooldown <= 0 && Math.abs(Mth.degreesDifference(this.aim(), yaw)) < 8.0F) {
               this.cooldown = 8;
               this.fire(level, target);
            }
         }
      }
   }

   private Vec3 muzzle() {
      return this.position().add(0.0, 0.85, 0.0);
   }

   @Nullable
   private LivingEntity findTarget(ServerLevel level) {
      Player owner = this.owner();
      LivingEntity best = null;
      double bestDist = RANGE * RANGE;

      for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RANGE), ex -> ex.isAlive() && !ex.isSpectator())) {
         boolean hostile = e instanceof Enemy || e instanceof Mob mob && owner != null && mob.getTarget() == owner;
         if (hostile && !(e instanceof AllyFrog) && !(e instanceof Player)) {
            double d = e.distanceToSqr(this);
            if (d < bestDist && this.canSee(level, e)) {
               bestDist = d;
               best = e;
            }
         }
      }

      return best;
   }

   private boolean canSee(ServerLevel level, LivingEntity e) {
      HitResult hit = level.clip(new ClipContext(this.muzzle(), e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      return hit.getType() == HitResult.Type.MISS;
   }

   private boolean takeAmmo(Player owner) {
      if (owner.getAbilities().instabuild) {
         return true;
      } else {
         Item ammo = ModRegistry.ITEMS_BY_ID.get(GunType.PM.ammoId).get();

         for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (stack.is(ammo)) {
               stack.shrink(1);
               return true;
            }
         }

         return false;
      }
   }

   private void fire(ServerLevel level, LivingEntity target) {
      Player owner = this.owner();
      if (owner == null || owner.distanceToSqr(this) > 48.0 * 48.0 || !this.takeAmmo(owner)) {
         this.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 1.6F);
         if (owner instanceof ServerPlayer sp && !this.warned) {
            this.warned = true;
            sp.displayClientMessage(Component.translatable("message.vaz2109.turret.no_ammo").withStyle(ChatFormatting.GRAY), true);
         }

         this.cooldown = 20;
         return;
      }

      Vec3 from = this.muzzle();
      Vec3 aimAt = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
      Vec3 dir = aimAt.subtract(from).normalize()
         .add(this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02)
         .normalize();
      Vec3 end = from.add(dir.scale(RANGE + 4.0));
      BlockHitResult block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
      Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
      byte kind = block.getType() == HitResult.Type.MISS ? TracerS2C.HIT_NONE : TracerS2C.HIT_BLOCK;
      if (target.getBoundingBox().inflate(0.2).clip(from, stop).isPresent()) {
         stop = target.getBoundingBox().inflate(0.2).clip(from, stop).get();
         kind = TracerS2C.HIT_ENTITY;
         DamageSource source = new DamageSource(
            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModRegistry.BULLET_DAMAGE), this, owner
         );
         target.invulnerableTime = 0;
         target.hurt(source, GunType.PM.damage);
      }

      BlockPos pos = kind == TracerS2C.HIT_BLOCK ? block.getBlockPos() : BlockPos.ZERO;
      Network.CHANNEL.send(
         PacketDistributor.TRACKING_ENTITY.with(() -> this),
         new TracerS2C(this.getId(), GunType.PM, from, stop, kind, kind == TracerS2C.HIT_BLOCK ? block.getDirection() : null, pos)
      );
      level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.4F, 1.5F);
      level.playSound(null, from.x, from.y, from.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.2F, 2.0F);
   }

   private void fold(ServerLevel level) {
      level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
      level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0F, 0.7F);
      this.discard();
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source) || this.level().isClientSide) {
         return false;
      } else {
         this.health -= amount;
         this.playSound(SoundEvents.ANVIL_LAND, 0.3F, 1.8F);
         if (this.health <= 0.0F && this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 12, 0.3, 0.3, 0.3, 0.03);
            this.fold(level);
         }

         return true;
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.owner = tag.getUUID("Owner");
      }

      this.age = tag.getInt("Age");
      this.health = tag.contains("Health") ? tag.getFloat("Health") : 24.0F;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      tag.putInt("Age", this.age);
      tag.putFloat("Health", this.health);
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }
}
