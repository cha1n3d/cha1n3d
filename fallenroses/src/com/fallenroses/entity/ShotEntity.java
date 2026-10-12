package com.fallenroses.entity;

import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.impl.DeathPainting;
import com.fallenroses.technique.impl.StrawDoll;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ShotEntity extends TechniqueProjectile {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ShotEntity.class, EntityDataSerializers.INT);
    private float curve;
    private int orbitTicks;
    private float orbitAngle;

    public ShotEntity(EntityType<? extends ShotEntity> type, Level level) {
        super(type, level);
    }

    public ShotEntity kind(ShotEntity.Kind kind) {
        this.entityData.set(KIND, kind.ordinal());
        switch (kind) {
            case BLOOD_DISC:
                this.maxLife = 26;
                this.pierce = true;
                this.hitRadius = 0.7;
                break;
            case BLOOD_ORB:
                this.maxLife = 90;
                this.hitRadius = 0.5;
                break;
            case NAIL:
                this.maxLife = 25;
                this.hitRadius = 0.25;
                break;
            case EMBER_INSECT:
                this.maxLife = 70;
                this.hitRadius = 0.5;
                break;
            case WOOD_BALL:
                this.maxLife = 50;
                this.hitRadius = 1.1;
                this.pierce = true;
                break;
            case METEOR:
                this.maxLife = 60;
                this.hitRadius = 2.0;
                this.passBlocks = false;
                break;
            case SPIRIT_SWARM:
                this.maxLife = 70;
                this.hitRadius = 0.45;
                break;
            case ICE_SHARD:
                this.maxLife = 24;
                this.hitRadius = 0.35;
                break;
            case FISH:
                this.maxLife = 80;
                this.hitRadius = 0.5;
                break;
            case GRAVESTONE:
                this.maxLife = 60;
                this.hitRadius = 1.0;
                this.passBlocks = false;
                break;
            case PACHINKO_BALL:
                this.maxLife = 30;
                this.hitRadius = 0.3;
                break;
            case GARUDA:
                this.maxLife = 50;
                this.hitRadius = 1.0;
                this.pierce = true;
                break;
            case ROT_BLOOD:
                this.maxLife = 40;
                this.hitRadius = 0.4;
                break;
            case GAVEL:
                this.maxLife = 30;
                this.hitRadius = 0.9;
                break;
            case ENERGY_SHOT:
                this.maxLife = 30;
                this.hitRadius = 0.45;
                break;
            case TRUESPHERE:
                this.maxLife = 90;
                this.hitRadius = 1.2;
                this.passBlocks = false;
        }

        return this;
    }

    public ShotEntity curve(float degPerTick) {
        this.curve = degPerTick;
        return this;
    }

    public ShotEntity orbit(int ticks, float startAngle) {
        this.orbitTicks = ticks;
        this.orbitAngle = startAngle;
        return this;
    }

    public ShotEntity.Kind getKind() {
        return ShotEntity.Kind.values()[Math.max(0, Math.min(ShotEntity.Kind.values().length - 1, this.entityData.get(KIND)))];
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KIND, 0);
    }

    @Override
    protected void serverTick() {
        ShotEntity.Kind kind = this.getKind();
        Vec3 m = this.getDeltaMovement();
        switch (kind) {
            case BLOOD_DISC:
                if (this.curve != 0.0F) {
                    double a = Math.toRadians((double)this.curve);
                    this.setDeltaMovement(new Vec3(m.x * Math.cos(a) - m.z * Math.sin(a), m.y, m.x * Math.sin(a) + m.z * Math.cos(a)));
                }
                break;
            case BLOOD_ORB:
                Entity ownerx = this.getOwner();
                if (this.tickCount < this.orbitTicks && ownerx != null) {
                    this.orbitAngle += 22.0F;
                    double r = 1.6;
                    Vec3 c = ownerx.position().add(0.0, 1.4, 0.0);
                    Vec3 want = c.add(
                        Math.cos(Math.toRadians((double)this.orbitAngle)) * r,
                        Math.sin(Math.toRadians((double)this.orbitAngle * 0.5)) * 0.4,
                        Math.sin(Math.toRadians((double)this.orbitAngle)) * r
                    );
                    this.setDeltaMovement(want.subtract(this.position()));
                } else {
                    this.home(18.0, 0.25, 0.9);
                }
            case NAIL:
            case METEOR:
            case ICE_SHARD:
            default:
                break;
            case EMBER_INSECT:
                this.home(20.0, 0.18, 0.55);
                break;
            case WOOD_BALL:
                this.setDeltaMovement(m.add(0.0, -0.025, 0.0));
                break;
            case SPIRIT_SWARM:
                this.home(22.0, 0.2, 0.6);
                break;
            case FISH:
                if (this.tickCount > 4) {
                    this.home(24.0, 0.16, 0.6);
                }
                break;
            case GRAVESTONE:
                this.setDeltaMovement(m.add(0.0, -0.08, 0.0));
                break;
            case PACHINKO_BALL:
                this.setDeltaMovement(m.add(0.0, -0.03, 0.0));
                break;
            case GARUDA:
                if (this.tickCount > 30) {
                    Entity owner = this.getOwner();
                    if (owner != null) {
                        this.setDeltaMovement(owner.position().add(0.0, 1.2, 0.0).subtract(this.position()).normalize().scale(1.3));
                    }

                    if (owner != null && owner.distanceToSqr(this) < 2.5) {
                        this.discard();
                    }
                } else {
                    this.home(16.0, 0.12, 1.3);
                }
                break;
            case ROT_BLOOD:
                if (this.tickCount > 3) {
                    this.home(14.0, 0.08, 1.2);
                }

                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.01, 0.0));
        }
    }

    private void home(double range, double turn, double speed) {
        Entity owner = this.getOwner();
        LivingEntity best = null;
        double bestD = range;

        for (LivingEntity e : this.level()
            .getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), ex -> Combat.valid(ex, (Entity)(owner == null ? this : owner)))) {
            double d = (double)e.distanceTo(this);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }

        Vec3 m = this.getDeltaMovement();
        if (m.lengthSqr() < 1.0E-4) {
            m = new Vec3(0.0, 0.1, 0.0);
        }

        if (best != null) {
            Vec3 to = best.getBoundingBox().getCenter().subtract(this.position()).normalize();
            m = m.normalize().scale(1.0 - turn).add(to.scale(turn)).normalize().scale(speed);
        } else {
            m = m.normalize().scale(speed);
        }

        this.setDeltaMovement(m);
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
        Entity owner = this.getOwner();
        float p = this.power();
        switch (this.getKind()) {
            case BLOOD_DISC:
                Combat.hurt(target, this, owner, 6.0F * p);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), owner);
                Vfx.play(
                    (ServerLevel)this.level(),
                    VfxKind.SLASH_HIT,
                    target.getBoundingBox().getCenter(),
                    this.getDeltaMovement(),
                    1.2F,
                    16719920,
                    -1,
                    this.random.nextInt(180)
                );
            case BLOOD_ORB:
            case EMBER_INSECT:
            case METEOR:
            case SPIRIT_SWARM:
            case FISH:
            case GRAVESTONE:
            default:
                break;
            case NAIL:
                Combat.hurt(target, this, owner, 3.5F * p);
                StrawDoll.addNail(target);
                Vfx.play((ServerLevel)this.level(), VfxKind.NAIL_HIT, this.getBoundingBox().getCenter(), this.getDeltaMovement(), 0.6F, 16756848, -1, 0);
                Combat.sound(this, this.position(), ModSounds.NAIL_HIT, 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
                break;
            case WOOD_BALL:
                Combat.hurt(target, this, owner, 11.0F * p);
                Combat.knock(target, this.getDeltaMovement(), 1.6, 0.5);
                Combat.sound(this, this.position(), ModSounds.SLAM, 1.2F, 1.3F);
                break;
            case ICE_SHARD:
                Combat.hurt(target, this, owner, 4.0F * p);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 80, target.getTicksFrozen() + 50));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), owner);
                Vfx.play((ServerLevel)this.level(), VfxKind.ICE_SPIKE, this.getBoundingBox().getCenter(), this.getDeltaMovement(), 0.4F, 11067647, -1, 1);
                Combat.sound(this, this.position(), ModSounds.ICE_SHATTER, 0.8F, 1.5F + this.random.nextFloat() * 0.3F);
                break;
            case PACHINKO_BALL:
                Combat.hurt(target, this, owner, 3.5F * p);
                Combat.knock(target, this.getDeltaMovement(), 0.3, 0.1);
                if (owner instanceof ServerPlayer sp && this.random.nextInt(3) == 0) {
                    sp.getPersistentData().putInt("fr_fever", Math.min(10, sp.getPersistentData().getInt("fr_fever") + 1));
                }

                Vfx.play((ServerLevel)this.level(), VfxKind.HIT_SPARK, this.getBoundingBox().getCenter(), this.getDeltaMovement(), 0.5F, 14221280, -1, 0);
                Combat.sound(this, this.position(), ModSounds.NAIL_HIT, 0.8F, 1.6F + this.random.nextFloat() * 0.3F);
                break;
            case GARUDA:
                Combat.hurt(target, this, owner, 7.0F * p);
                Combat.knock(target, this.getDeltaMovement(), 1.4, 0.4);
                Vfx.play((ServerLevel)this.level(), VfxKind.MASS_IMPACT, target.getBoundingBox().getCenter(), this.getDeltaMovement(), 1.0F, 16762960, -1, 0);
                Combat.sound(this, this.position(), ModSounds.SLAM, 1.0F, 1.2F);
                break;
            case ROT_BLOOD:
                Combat.hurt(target, this, owner, 4.0F * p);
                if (owner instanceof LivingEntity lo) {
                    DeathPainting.rot(target, lo, 1, 140);
                }
                break;
            case GAVEL:
                Combat.hurt(target, this, owner, 9.0F * p);
                Combat.knock(target, this.getDeltaMovement(), 1.0, 0.3);
                target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 20, 0), owner);
                Vfx.play((ServerLevel)this.level(), VfxKind.MASS_IMPACT, target.getBoundingBox().getCenter(), this.getDeltaMovement(), 1.1F, 15259808, -1, 0);
                Combat.sound(this, this.position(), ModSounds.HAMMER, 1.6F, 0.6F);
                break;
            case ENERGY_SHOT:
                Combat.hurt(target, this, owner, 6.0F * p);
        }
    }

    @Override
    protected void impact(Vec3 at) {
        ServerLevel level = (ServerLevel)this.level();
        Entity owner = this.getOwner();
        Entity src = (Entity)(owner == null ? this : owner);
        float p = this.power();
        switch (this.getKind()) {
            case BLOOD_ORB:
                Vfx.play(level, VfxKind.BLOOD_BURST, at, Vec3.ZERO, 2.6F, 13635632, -1, 0);
                Combat.sound(this, at, ModSounds.BLOOD_BURST, 1.5F, 1.0F);

                for (LivingEntity e : Combat.around(src, at, 2.8)) {
                    Combat.hurt(e, this, owner, 8.0F * p);
                }
            case NAIL:
            case ICE_SHARD:
            case PACHINKO_BALL:
            case GAVEL:
            default:
                break;
            case EMBER_INSECT:
                Vfx.play(level, VfxKind.EMBER_BOOM, at, Vec3.ZERO, 1.8F, 16738832, -1, 0);
                Combat.sound(this, at, ModSounds.FUGA_IMPACT, 0.8F, 1.8F);

                for (LivingEntity e : Combat.around(src, at, 2.0)) {
                    Combat.hurt(e, this, owner, 4.5F * p);
                    e.setSecondsOnFire(4);
                }
                break;
            case WOOD_BALL:
                Vfx.play(level, VfxKind.SLAM, at, Vec3.ZERO, 3.0F);
                Combat.shake(level, at, 0.8F, 10, 16.0);

                for (LivingEntity e : Combat.around(src, at, 3.0)) {
                    Combat.hurt(e, this, owner, 6.0F * p);
                    Combat.knock(e, e.position().subtract(at), 1.0, 0.5);
                }
                break;
            case METEOR:
                Vfx.play(level, VfxKind.METEOR_IMPACT, at, Vec3.ZERO, 7.0F, 16738832, -1, 0);
                Combat.sound(this, at, ModSounds.FUGA_IMPACT, 5.0F, 0.6F);
                Combat.sound(this, at, ModSounds.SLAM, 4.0F, 0.5F);
                Combat.shake(level, at, 2.5F, 30, 48.0);
                Combat.flash(level, at, 16742944, 12, 0.6F, 40.0);

                for (LivingEntity e : Combat.around(src, at, 7.0)) {
                    double d = e.getBoundingBox().getCenter().distanceTo(at);
                    float k = (float)Math.max(0.35, 1.0 - d / 8.0);
                    Combat.hurt(e, this, owner, 26.0F * p * k);
                    e.setSecondsOnFire(8);
                    Combat.knock(e, e.position().subtract(at), 1.6 * (double)k, 0.8);
                }
                break;
            case SPIRIT_SWARM:
                Vfx.play(level, VfxKind.CURSE_ORB_HIT, at);

                for (LivingEntity e : Combat.around(src, at, 1.5)) {
                    Combat.hurt(e, this, owner, 4.5F * p);
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), owner);
                }
                break;
            case FISH:
                Vfx.play(level, VfxKind.SPLASH, at, Vec3.ZERO, 1.0F, 2788048, -1, 0);
                Combat.sound(this, at, ModSounds.SPLASH, 0.9F, 1.2F + this.random.nextFloat() * 0.3F);

                for (LivingEntity e : Combat.around(src, at, 1.8)) {
                    Combat.hurt(e, this, owner, 4.0F * p);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0), owner);
                }
                break;
            case GRAVESTONE:
                Vfx.play(level, VfxKind.GRAVE_IMPACT, at, Vec3.ZERO, 1.6F, 10133600, -1, 0);
                Combat.sound(this, at, ModSounds.GRAVE_THUD, 1.6F, 0.8F + this.random.nextFloat() * 0.2F);
                Combat.shake(level, at, 0.6F, 8, 14.0);

                for (LivingEntity e : Combat.around(src, at, 2.2)) {
                    Combat.hurt(e, this, owner, 8.0F * p);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2), owner);
                }
                break;
            case GARUDA:
                Vfx.play(level, VfxKind.MASS_IMPACT, at, this.getDeltaMovement(), 1.2F, 16762960, -1, 0);
                break;
            case ROT_BLOOD:
                Vfx.play(level, VfxKind.ROT_BURST, at, Vec3.ZERO, 0.7F, 10123824, -1, 0);
                Combat.sound(this, at, ModSounds.BLOOD_BURST, 0.7F, 0.7F);
                break;
            case ENERGY_SHOT:
                Vfx.play(level, VfxKind.RED_IMPACT, at, this.getDeltaMovement(), 1.4F, 15786160, -1, 0);
                Combat.sound(this, at, ModSounds.RED_IMPACT, 0.9F, 1.5F);

                for (LivingEntity e : Combat.around(src, at, 1.8)) {
                    Combat.hurt(e, this, owner, 3.0F * p);
                }
                break;
            case TRUESPHERE:
                Vfx.play(level, VfxKind.BLACK_HOLE, at, Vec3.ZERO, 6.0F, 15790335, -1, 1);
                Combat.sound(this, at, ModSounds.FUGA_IMPACT, 3.0F, 1.2F);
                Combat.shake(level, at, 2.2F, 24, 40.0);

                for (LivingEntity e : Combat.around(src, at, 6.0)) {
                    double d = e.getBoundingBox().getCenter().distanceTo(at);
                    float k = (float)Math.max(0.4, 1.0 - d / 7.0);
                    Combat.hurt(e, this, owner, 38.0F * p * k);
                    e.setDeltaMovement(at.subtract(e.position()).scale(0.25));
                    e.hurtMarked = true;
                }
        }
    }

    @Override
    protected void expire() {
        ShotEntity.Kind k = this.getKind();
        if (k != ShotEntity.Kind.BLOOD_DISC
            && k != ShotEntity.Kind.NAIL
            && k != ShotEntity.Kind.ICE_SHARD
            && k != ShotEntity.Kind.PACHINKO_BALL
            && k != ShotEntity.Kind.GARUDA) {
            this.impact(this.position());
        }
    }

    @Override
    protected void clientTick() {
        Vec3 m = this.getDeltaMovement();
        switch (this.getKind()) {
            case BLOOD_DISC:
                this.level().addParticle(ModParticles.glow(12584984, 0.5F, 8), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case BLOOD_ORB:
                this.level().addParticle(ModParticles.glow(16719936, 0.6F, 10), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            case NAIL:
            default:
                break;
            case EMBER_INSECT:
                this.level()
                    .addParticle(
                        ModParticles.ember(16742944, 0.25F, 16),
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        this.random.nextGaussian() * 0.02,
                        0.02,
                        this.random.nextGaussian() * 0.02
                    );
                this.level().addParticle(ModParticles.glow(16730640, 0.5F, 6), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case WOOD_BALL:
                if (this.tickCount % 2 == 0) {
                    this.level()
                        .addParticle(
                            ModParticles.petal(16033992, 0.5F, 30),
                            this.getX(),
                            this.getY() + 0.5,
                            this.getZ(),
                            this.random.nextGaussian() * 0.05,
                            0.05,
                            this.random.nextGaussian() * 0.05
                        );
                }
                break;
            case METEOR:
                for (int i = 0; i < 6; i++) {
                    double f = (double)i / 6.0;
                    this.level()
                        .addParticle(
                            ModParticles.ember(16738832, 0.8F, 30),
                            this.getX() - m.x * f + this.random.nextGaussian() * 0.8,
                            this.getY() - m.y * f + this.random.nextGaussian() * 0.8,
                            this.getZ() - m.z * f + this.random.nextGaussian() * 0.8,
                            this.random.nextGaussian() * 0.05,
                            0.05,
                            this.random.nextGaussian() * 0.05
                        );
                    this.level()
                        .addParticle(
                            ModParticles.smoke(1706500, 2.5F, 40), this.getX() - m.x * f, this.getY() - m.y * f + 0.5, this.getZ() - m.z * f, 0.0, 0.03, 0.0
                        );
                }
                break;
            case SPIRIT_SWARM:
                this.level().addParticle(ModParticles.smoke(662036, 0.5F, 14), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case ICE_SHARD:
                this.level().addParticle(ModParticles.spark(14218495, 0.25F, 8), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case FISH:
                if (this.tickCount % 2 == 0) {
                    this.level().addParticle(ParticleTypes.BUBBLE_POP, this.getX(), this.getY(), this.getZ(), 0.0, 0.02, 0.0);
                }

                this.level().addParticle(ModParticles.glow(2788048, 0.35F, 8), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case GRAVESTONE:
                this.level().addParticle(ModParticles.smoke(3815978, 0.8F, 16), this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.02, 0.0);
                break;
            case PACHINKO_BALL:
                this.level().addParticle(ModParticles.spark(14221280, 0.2F, 5), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                break;
            case GARUDA:
                this.level().addParticle(ModParticles.glow(16762960, 0.7F, 10), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                if (this.tickCount % 2 == 0) {
                    this.level()
                        .addParticle(
                            ModParticles.star(16773312, 0.3F, 14),
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            this.random.nextGaussian() * 0.03,
                            this.random.nextGaussian() * 0.03,
                            this.random.nextGaussian() * 0.03
                        );
                }
                break;
            case ROT_BLOOD:
                this.level().addParticle(ModParticles.smoke(5917200, 0.35F, 12), this.getX(), this.getY(), this.getZ(), 0.0, -0.01, 0.0);
                break;
            case GAVEL:
                if (this.tickCount % 2 == 0) {
                    this.level().addParticle(ModParticles.spark(15259808, 0.3F, 8), this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.0, 0.0);
                }
                break;
            case ENERGY_SHOT:
                this.level().addParticle(ModParticles.glow(15786160, 0.5F, 8), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.discard();
    }

    @Nullable
    @Override
    public Entity getOwner() {
        return super.getOwner();
    }

    public static enum Kind {
        BLOOD_DISC,
        BLOOD_ORB,
        NAIL,
        EMBER_INSECT,
        WOOD_BALL,
        METEOR,
        SPIRIT_SWARM,
        ICE_SHARD,
        FISH,
        GRAVESTONE,
        PACHINKO_BALL,
        GARUDA,
        ROT_BLOOD,
        GAVEL,
        ENERGY_SHOT,
        TRUESPHERE;
    }
}
