package com.fallenroses.entity;

import com.fallenroses.technique.Combat;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public abstract class TechniqueProjectile extends Projectile {
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(TechniqueProjectile.class, EntityDataSerializers.FLOAT);
    protected int maxLife = 40;
    protected boolean pierce;
    protected boolean passBlocks;
    protected double hitRadius = 0.3;
    private final Set<Integer> hit = new HashSet<>();

    protected TechniqueProjectile(EntityType<? extends TechniqueProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void launch(Entity owner, Vec3 from, Vec3 velocity, float power) {
        this.setOwner(owner);
        this.setPower(power);
        this.setPos(from.x, from.y, from.z);
        this.setDeltaMovement(velocity);
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
        owner.level().addFreshEntity(this);
    }

    public void setPower(float power) {
        this.entityData.set(POWER, power);
    }

    public float power() {
        return this.entityData.get(POWER);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > this.maxLife) {
            this.expire();
            this.discard();
        } else {
            Vec3 pos = this.position();
            Vec3 motion = this.getDeltaMovement();
            Vec3 next = pos.add(motion);
            if (!this.level().isClientSide) {
                Entity owner = this.getOwner();

                for (LivingEntity e : this.level()
                    .getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().expandTowards(motion).inflate(this.hitRadius),
                        ex -> Combat.valid(ex, (Entity)(owner == null ? this : owner))
                    )) {
                    if (this.hit.add(e.getId())) {
                        this.onHitLiving(e);
                        if (!this.pierce) {
                            this.impact(e.getBoundingBox().getCenter());
                            this.discard();
                            return;
                        }
                    }
                }

                if (!this.passBlocks) {
                    BlockHitResult wall = this.level().clip(new ClipContext(pos, next, Block.COLLIDER, Fluid.NONE, this));
                    if (wall.getType() != Type.MISS) {
                        this.impact(wall.getLocation());
                        this.discard();
                        return;
                    }
                }

                this.serverTick();
            } else {
                this.clientTick();
            }

            this.setPos(next.x, next.y, next.z);
        }
    }

    protected abstract void onHitLiving(LivingEntity var1);

    protected void impact(Vec3 at) {
    }

    protected void expire() {
        this.impact(this.position());
    }

    protected void serverTick() {
    }

    protected void clientTick() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPower(tag.getFloat("Power"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Power", this.power());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }
}
