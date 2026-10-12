package com.fallenroses.entity;

import com.fallenroses.registry.ModParticles;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SlashEntity extends TechniqueProjectile {
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(SlashEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(SlashEntity.class, EntityDataSerializers.FLOAT);

    public SlashEntity(EntityType<? extends SlashEntity> type, Level level) {
        super(type, level);
        this.maxLife = 14;
        this.pierce = true;
        this.hitRadius = 1.2;
    }

    public SlashEntity setup(float roll, float width) {
        this.entityData.set(ROLL, roll);
        this.entityData.set(WIDTH, width);
        this.hitRadius = (double)width * 0.45;
        return this;
    }

    public float roll() {
        return this.entityData.get(ROLL);
    }

    public float width() {
        return this.entityData.get(WIDTH);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROLL, 0.0F);
        this.entityData.define(WIDTH, 3.0F);
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
        Combat.hurt(target, this, this.getOwner(), 7.0F * this.power());
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        Vfx.play(
            (ServerLevel)this.level(), VfxKind.SLASH_HIT, target.getBoundingBox().getCenter(), this.getDeltaMovement(), 1.0F, 16777215, -1, (int)this.roll()
        );
    }

    @Override
    protected void clientTick() {
        if (this.tickCount % 2 == 0) {
            Vec3 m = this.getDeltaMovement().normalize();
            this.level().addParticle(ModParticles.spark(16777215, 0.4F, 5), this.getX(), this.getY(), this.getZ(), -m.x * 0.4, -m.y * 0.4, -m.z * 0.4);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ROLL, tag.getFloat("Roll"));
        this.entityData.set(WIDTH, tag.getFloat("Width"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Roll", this.roll());
        tag.putFloat("Width", this.width());
    }
}
