package com.fallenroses.entity;

import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BlueOrbEntity extends Entity {
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(BlueOrbEntity.class, EntityDataSerializers.FLOAT);
    public static final double RADIUS = 8.0;
    public static final int LIFETIME = 70;
    @Nullable
    private UUID ownerId;

    public BlueOrbEntity(EntityType<? extends BlueOrbEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(Entity owner, float power) {
        this.ownerId = owner.getUUID();
        this.entityData.set(POWER, power);
    }

    public float power() {
        return this.entityData.get(POWER);
    }

    @Nullable
    private Entity owner() {
        return this.ownerId != null && this.level() instanceof ServerLevel server ? server.getEntity(this.ownerId) : null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(POWER, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.clientParticles();
        } else {
            ServerLevel level = (ServerLevel)this.level();
            if (this.tickCount >= 70) {
                Vfx.play(level, VfxKind.RED_IMPACT, this.position(), Vec3.ZERO, 3.0F, 4885759, -1, 1);
                Combat.sound(this, this.position(), ModSounds.INFINITY_BLOCK, 2.0F, 0.5F);
                Entity owner = this.owner();

                for (LivingEntity e : Combat.around((Entity)(owner == null ? this : owner), this.position(), 3.5)) {
                    Combat.hurt(e, this, owner, 6.0F * this.power());
                    Combat.knock(e, e.position().subtract(this.position()), 0.9, 0.4);
                }

                this.discard();
            } else {
                if (this.tickCount % 30 == 1) {
                    Combat.sound(this, this.position(), ModSounds.BLUE_LOOP, 1.5F, 1.0F);
                }

                if (this.tickCount % 6 == 0) {
                    Combat.shake(level, this.position(), 0.25F, 8, 14.0);
                }

                Entity owner = this.owner();
                Vec3 center = this.position();

                for (Entity e : level.getEntities(
                    this,
                    new AABB(center, center).inflate(8.0),
                    ex -> ex != owner && !ex.isSpectator() && (ex instanceof LivingEntity || ex instanceof ItemEntity || ex instanceof Projectile)
                )) {
                    if (!(e instanceof LivingEntity) || Combat.valid(e, (Entity)(owner == null ? this : owner))) {
                        Vec3 toCenter = center.subtract(e.position().add(0.0, (double)(e.getBbHeight() / 2.0F), 0.0));
                        double dist = toCenter.length();
                        if (!(dist > 8.0) && !(dist < 1.0E-4)) {
                            Vec3 pull = toCenter.normalize().scale(0.16 + 0.34 * (1.0 - dist / 8.0));
                            e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(pull));
                            e.hurtMarked = true;
                            e.fallDistance = 0.0F;
                            if (e instanceof LivingEntity) {
                                LivingEntity living = (LivingEntity)e;
                                if (dist < 2.8 && this.tickCount % 8 == 0) {
                                    Combat.hurt(living, this, owner, 3.5F * this.power());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void clientParticles() {
        for (int i = 0; i < 5; i++) {
            Vec3 d = new Vec3(this.random.nextGaussian(), this.random.nextGaussian() * 0.7, this.random.nextGaussian()).normalize();
            double r = 2.5 + this.random.nextDouble() * 4.5;
            Vec3 p = this.position().add(d.scale(r));
            Vec3 v = d.scale(-r * 0.12);
            this.level().addParticle(ModParticles.spark(i % 3 == 0 ? 16777215 : 5941503, 0.45F, 8), p.x, p.y, p.z, v.x, v.y, v.z);
        }

        if (this.random.nextInt(2) == 0) {
            Vec3 d = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).normalize().scale(1.2);
            this.level()
                .addParticle(ModParticles.glow(2775807, 1.8F, 8), this.getX() + d.x, this.getY() + d.y, this.getZ() + d.z, -d.x * 0.1, -d.y * 0.1, -d.z * 0.1);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }

        this.entityData.set(POWER, tag.getFloat("Power"));
        this.tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }

        tag.putFloat("Power", this.power());
        tag.putInt("Age", this.tickCount);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
