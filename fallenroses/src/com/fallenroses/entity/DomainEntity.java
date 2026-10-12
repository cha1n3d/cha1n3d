package com.fallenroses.entity;

import com.fallenroses.entity.curse.CursedSpirit;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.registry.ModDamageTypes;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.impl.DeadlySentencing;
import com.fallenroses.technique.impl.IdleDeathGamble;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DomainEntity extends Entity {
    public static final int VOID = 0;
    public static final int SHRINE = 1;
    public static final int FLAMES = 2;
    public static final int MAHITO = 3;
    public static final int TIDES = 4;
    public static final int CEMETERY = 5;
    public static final int GAMBLE = 6;
    public static final int JUDGEMENT = 7;
    public static final int MOON_PALACE = 8;
    public static final int STATION = 9;
    public static final int CLOSE = 15;
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(DomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DomainEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(DomainEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(DomainEntity.class, EntityDataSerializers.INT);
    @Nullable
    private UUID ownerId;
    private float power = 1.0F;
    private final Set<UUID> insiders = new HashSet<>();
    private final Map<Integer, Vec3> lastPos = new HashMap<>();
    @Nullable
    private UUID defendant;
    private boolean breaking;
    private boolean shattered;
    /** Barrier veins and rim colour per domain type (also the client renderer's palette). */
    public static final int[] EDGE = new int[]{15266047, 16722480, 16738832, 8044728, 9099519, 10133600, 6987898, 15259808, 8050920, 16746560};

    public DomainEntity(EntityType<? extends DomainEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(Entity owner, int type, float radius, int life, float power) {
        this.ownerId = owner.getUUID();
        this.entityData.set(TYPE, type);
        this.entityData.set(RADIUS, radius);
        this.entityData.set(LIFE, life);
        this.entityData.set(OWNER_ID, owner.getId());
        this.power = power;
    }

    public int domainType() {
        return this.entityData.get(TYPE);
    }

    public float radius() {
        return this.entityData.get(RADIUS);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    public void endIn(int ticks) {
        this.entityData.set(LIFE, Math.min(this.life(), this.tickCount + ticks));
    }

    public int ownerEntityId() {
        return this.entityData.get(OWNER_ID);
    }

    @Nullable
    public UUID ownerUuid() {
        return this.ownerId;
    }

    public float power() {
        return this.power;
    }

    public boolean isInsider(Entity e) {
        return this.insiders.contains(e.getUUID());
    }

    public float openness(float partial) {
        float age = (float)this.tickCount + partial;
        return Mth.clamp(Math.min(age / 20.0F, ((float)this.life() - age) / 15.0F), 0.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE, 0);
        this.entityData.define(RADIUS, 14.0F);
        this.entityData.define(LIFE, 200);
        this.entityData.define(OWNER_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.ambience();
        } else {
            ServerLevel level = (ServerLevel)this.level();
            Entity owner = this.ownerId == null ? null : level.getEntity(this.ownerId);
            boolean held = owner != null && owner.isAlive() && owner.level() == level;
            if (!held && !this.breaking) {
                this.endIn(15);
            }

            int life = this.life();
            if (this.tickCount >= life) {
                this.discard();
                return;
            }

            if (!this.breaking && this.tickCount >= life - 15) {
                this.breaking = true;
                Combat.sound(this, this.position(), ModSounds.DOMAIN_SHATTER, 5.0F, 1.0F);
            }

            if (this.breaking) {
                if (!this.shattered && this.tickCount >= life - 7) {
                    this.shattered = true;
                    Vfx.play(level, VfxKind.DOMAIN_BREAK, this.position(), Vec3.ZERO, this.radius(), EDGE[Mth.clamp(this.domainType(), 0, 9)], this.getId(), this.domainType());
                    Combat.shake(level, this.position(), 1.4F, 14, (double)(this.radius() * 2.5F));
                }
            } else {
                int type = this.domainType();
                if (this.tickCount % 60 == 1) {
                    Combat.sound(this, this.position(), switch (type) {
                        case 1 -> ModSounds.DOMAIN_SHRINE;
                        case 2 -> ModSounds.DOMAIN_AMBIENT_FLAMES;
                        case 3 -> ModSounds.DOMAIN_AMBIENT_FLESH;
                        case 4 -> ModSounds.DOMAIN_AMBIENT_SEA;
                        case 5 -> ModSounds.DOMAIN_AMBIENT_CEMETERY;
                        case 6 -> ModSounds.DOMAIN_AMBIENT_PACHINKO;
                        case 7 -> ModSounds.DOMAIN_AMBIENT_COURT;
                        case 8 -> ModSounds.DOMAIN_AMBIENT_FILM;
                        case 9 -> ModSounds.DOMAIN_AMBIENT_STATION;
                        default -> ModSounds.DOMAIN_VOID;
                    }, 2.5F, 1.0F);
                }

                float r = this.radius();
                if (this.tickCount == 15) {
                    for (LivingEntity e : level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(this.position(), this.position()).inflate((double)r),
                        ex -> ex.isAlive() && !ex.isSpectator() && ex.position().distanceTo(this.position()) < (double)r
                    )) {
                        this.insiders.add(e.getUUID());
                    }

                    this.insiders.add(owner.getUUID());
                }

                if (this.tickCount >= 15) {
                    this.enforceBarrier(level, r);
                    List<LivingEntity> targets = new ArrayList<>();

                    for (LivingEntity e : Combat.around(owner, this.position(), (double)r)) {
                        if (this.insiders.contains(e.getUUID())) {
                            targets.add(e);
                        }
                    }

                    switch (type) {
                        case 0:
                            for (LivingEntity exxxx : targets) {
                                exxxx.addEffect(new MobEffectInstance(ModEffects.OVERLOAD.get(), 30, 0, false, false, true), owner);
                                if (exxxx instanceof Mob mob) {
                                    mob.getNavigation().stop();
                                    mob.setTarget(null);
                                }

                                exxxx.setDeltaMovement(0.0, Math.min(0.0, exxxx.getDeltaMovement().y), 0.0);
                                exxxx.hurtMarked = true;
                                if (this.tickCount % 20 == 0) {
                                    Combat.hurt(exxxx, this, owner, 3.0F * this.power);
                                }
                            }
                            break;
                        case 1:
                        default:
                            if (this.tickCount % 4 == 0) {
                                for (LivingEntity exxx : targets) {
                                    Combat.hurt(exxx, this, owner, 2.2F * this.power);
                                    if (this.random.nextInt(2) == 0) {
                                        Vfx.play(
                                            level,
                                            VfxKind.SLASH_HIT,
                                            exxx.getBoundingBox().getCenter(),
                                            new Vec3(this.random.nextGaussian(), this.random.nextGaussian() * 0.3, this.random.nextGaussian()),
                                            1.3F,
                                            16777215,
                                            -1,
                                            this.random.nextInt(180)
                                        );
                                    }
                                }
                            }

                            if (this.tickCount % 3 == 0) {
                                double a = this.random.nextDouble() * (float) (Math.PI * 2);
                                double d = this.random.nextDouble() * (double)r;
                                Vec3 p = this.position().add(Math.cos(a) * d, this.random.nextDouble() * 2.0, Math.sin(a) * d);
                                Vfx.play(
                                    level,
                                    VfxKind.SLASH_HIT,
                                    p,
                                    new Vec3(this.random.nextGaussian(), 0.0, this.random.nextGaussian()),
                                    2.0F,
                                    16777215,
                                    -1,
                                    this.random.nextInt(180)
                                );
                            }
                            break;
                        case 2:
                            for (LivingEntity ex : targets) {
                                ex.setSecondsOnFire(4);
                                if (this.tickCount % 10 == 0) {
                                    Combat.hurt(ex, this, owner, 2.0F * this.power);
                                    if (this.random.nextInt(3) == 0) {
                                        Vfx.play(level, VfxKind.FIRE_PILLAR, ex.position(), Vec3.ZERO, 0.8F, 16738832, -1, 1);
                                    }
                                }
                            }

                            if (this.tickCount % 5 == 0) {
                                double a = this.random.nextDouble() * (float) (Math.PI * 2);
                                double d = this.random.nextDouble() * (double)r * 0.9;
                                Vfx.play(
                                    level, VfxKind.FIRE_PILLAR, this.position().add(Math.cos(a) * d, 0.0, Math.sin(a) * d), Vec3.ZERO, 0.6F, 16738832, -1, 1
                                );
                            }
                            break;
                        case 3:
                            if (this.tickCount % 20 == 0) {
                                for (LivingEntity exxxx : targets) {
                                    boolean var10000;
                                    label147: {
                                        if (exxxx instanceof CursedSpirit spirit && spirit.grade() <= 0) {
                                            var10000 = true;
                                            break label147;
                                        }

                                        var10000 = false;
                                    }

                                    boolean boss = var10000;
                                    if (!boss && !(exxxx instanceof Player) && exxxx.getHealth() < exxxx.getMaxHealth() * 0.35F) {
                                        Vfx.play(
                                            level,
                                            VfxKind.SOUL_TOUCH,
                                            exxxx.getBoundingBox().getCenter(),
                                            Vec3.ZERO,
                                            exxxx.getBbHeight(),
                                            8044728,
                                            exxxx.getId(),
                                            1
                                        );
                                        exxxx.hurt(this.damageSources().magic(), Float.MAX_VALUE);
                                    } else {
                                        exxxx.invulnerableTime = 0;
                                        exxxx.hurt(ModDamageTypes.technique(this, owner), 6.0F * this.power);
                                        Vfx.play(
                                            level,
                                            VfxKind.SOUL_TOUCH,
                                            exxxx.getBoundingBox().getCenter(),
                                            Vec3.ZERO,
                                            exxxx.getBbHeight(),
                                            8044728,
                                            exxxx.getId(),
                                            0
                                        );
                                    }

                                    exxxx.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, false, true));
                                }
                            }
                            break;
                        case 4:
                            for (LivingEntity exx : targets) {
                                if (this.tickCount % 10 == 0) {
                                    Combat.hurt(exx, this, owner, 2.2F * this.power);
                                    exx.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false, true));
                                    if (this.random.nextInt(2) == 0) {
                                        Vfx.play(level, VfxKind.SPLASH, exx.getBoundingBox().getCenter(), Vec3.ZERO, 1.2F, 2788048, -1, 0);
                                    }
                                }
                            }

                            if (this.tickCount % 12 == 0) {
                                double a = this.random.nextDouble() * (float) (Math.PI * 2);
                                Vec3 from = this.position()
                                    .add(Math.cos(a) * (double)r * 0.85, 1.5 + this.random.nextDouble() * 2.0, Math.sin(a) * (double)r * 0.85);
                                new ShotEntity(ModEntities.SHOT.get(), level)
                                    .kind(ShotEntity.Kind.FISH)
                                    .launch(owner, from, this.position().subtract(from).normalize().scale(0.5), this.power);
                            }
                            break;
                        case 5:
                            this.cemetery(level, owner, targets);
                            break;
                        case 6:
                            this.gamble(level, owner, targets);
                            break;
                        case 7:
                            this.judgement(level, owner, targets);
                            break;
                        case 8:
                            this.moonPalace(level, owner, targets);
                            break;
                        case 9:
                            this.station(level, owner, targets);
                    }
                }
            }
        }
    }

    /** Client side: the air inside the domain (ash, embers, souls, spray...). */
    private void ambience() {
        float open = this.openness(0.0F);
        if (!(open < 0.6F)) {
            Level level = this.level();
            float r = this.radius();
            int type = this.domainType();

            for (int i = 0; i < 4; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double d = Math.sqrt(this.random.nextDouble()) * (double)r * 0.9;
                double x = this.getX() + Math.cos(a) * d;
                double z = this.getZ() + Math.sin(a) * d;
                double y = this.getY() + this.random.nextDouble() * Math.sqrt(Math.max(1.0, (double)(r * r) - d * d)) * 0.8;
                double ground = this.getY() + 0.2;
                switch (type) {
                    case 0:
                        level.addParticle(ModParticles.glow(i % 2 == 0 ? 15266047 : 12572927, 0.25F, 40), x, y, z, 0.0, 0.0, 0.0);
                        break;
                    case 1:
                        level.addParticle(ModParticles.ember(i % 2 == 0 ? 16722480 : 4194312, 0.3F, 50), x, y, z, 0.0, -0.03, 0.0);
                        break;
                    case 2:
                        level.addParticle(ModParticles.ember(16738832, 0.35F, 40), x, ground, z, 0.0, 0.12 + this.random.nextDouble() * 0.1, 0.0);
                        break;
                    case 3:
                        level.addParticle(ModParticles.glow(i % 2 == 0 ? 11192575 : 8044728, 0.3F, 50), x, ground, z, 0.0, 0.04, 0.0);
                        break;
                    case 4:
                        level.addParticle(ModParticles.glow(i % 2 == 0 ? 14743807 : 9099519, 0.25F, 40), x, ground, z, 0.0, 0.05, 0.0);
                        break;
                    case 5:
                        level.addParticle(ModParticles.smoke(i % 2 == 0 ? 5002832 : 3158064, 1.6F, 60), x, ground, z, 0.01, 0.005, 0.0);
                        break;
                    case 6:
                        level.addParticle(
                            ModParticles.star(i % 3 == 0 ? 16769088 : (i % 3 == 1 ? 6987898 : 16738954), 0.3F, 30), x, y, z, 0.0, 0.02, 0.0
                        );
                        break;
                    case 7:
                        level.addParticle(ModParticles.glow(15259808, 0.2F, 60), x, y, z, 0.0, -0.01, 0.0);
                        break;
                    case 8:
                        level.addParticle(ModParticles.star(i % 2 == 0 ? 15266047 : 8050920, 0.25F, 40), x, y, z, 0.0, 0.0, 0.0);
                        break;
                    default:
                        level.addParticle(ModParticles.smoke(3810322, 1.0F, 50), x, ground, z, 0.0, 0.01, 0.0);
                }
            }
        }
    }

    private void enforceBarrier(ServerLevel level, float r) {
        Vec3 c = this.position();

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate((double)(r + 3.0F)), ex -> ex.isAlive() && !ex.isSpectator())) {
            if (!(e instanceof ServerPlayer sp) || !sp.isCreative()) {
                Vec3 p = e.position();
                double dy = p.y - c.y;
                double d = p.distanceTo(c);
                boolean inside = this.insiders.contains(e.getUUID());
                if (inside && d > (double)r - 0.8) {
                    Vec3 dir = p.subtract(c).normalize();
                    Vec3 back = c.add(dir.scale((double)r - 1.6));
                    e.teleportTo(back.x, Math.max(back.y, Math.min(p.y, c.y + 1.0)), back.z);
                    e.setDeltaMovement(dir.scale(-0.3));
                    e.hurtMarked = true;
                    if (e instanceof ServerPlayer spx && this.tickCount % 20 == 0) {
                        spx.displayClientMessage(Component.translatable("message.fallenroses.barrier_locked").withStyle(ChatFormatting.DARK_GRAY), true);
                    }
                } else if (!inside && d < (double)r + 0.6) {
                    Vec3 flat = new Vec3(p.x - c.x, 0.0, p.z - c.z);
                    double fl = flat.length();
                    flat = fl < 0.001 ? new Vec3(1.0, 0.0, 0.0) : flat.scale(1.0 / fl);
                    double hr = Math.sqrt(Math.max(0.0, ((double)r + 1.4) * ((double)r + 1.4) - dy * dy));
                    if (hr < 1.5) {
                        e.teleportTo(p.x, c.y + (double)r + 1.5, p.z);
                    } else {
                        e.teleportTo(c.x + flat.x * hr, p.y, c.z + flat.z * hr);
                    }

                    e.setDeltaMovement(flat.scale(0.5).add(0.0, 0.2, 0.0));
                    e.hurtMarked = true;
                    if (e instanceof ServerPlayer spx && this.tickCount % 20 == 0) {
                        spx.displayClientMessage(Component.translatable("message.fallenroses.barrier_blocked").withStyle(ChatFormatting.DARK_GRAY), true);
                    }
                }
            }
        }

        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate((double)(r + 2.0F)))) {
            Entity o = pr.getOwner();
            boolean ownerInside = o != null && this.insiders.contains(o.getUUID());
            double d = pr.position().distanceTo(c);
            if (ownerInside && d > (double)r - 0.3 || !ownerInside && d < (double)r - 0.3) {
                pr.discard();
            }
        }
    }

    private void cemetery(ServerLevel level, Entity owner, List<LivingEntity> targets) {
        if (this.tickCount % 25 == 0) {
            for (LivingEntity e : targets) {
                Vec3 from = e.position().add(this.random.nextGaussian() * 0.6, 8.0, this.random.nextGaussian() * 0.6);
                new ShotEntity(ModEntities.SHOT.get(), level).kind(ShotEntity.Kind.GRAVESTONE).launch(owner, from, new Vec3(0.0, -0.6, 0.0), this.power);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
            }
        }

        if (this.tickCount % 70 == 20 && !targets.isEmpty()) {
            LivingEntity t = targets.get(this.random.nextInt(targets.size()));
            Vfx.play(level, VfxKind.COFFIN, t.position(), Vec3.ZERO, Math.max(1.0F, t.getBbHeight()), 5917232, t.getId(), 45);
            t.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 45, 0), owner);
            Scheduler.later(45, () -> {
                if (t.isAlive() && !this.isRemoved()) {
                    Vfx.play(level, VfxKind.GRAVE_IMPACT, t.position(), Vec3.ZERO, 2.0F, 10133600, -1, 1);
                    Combat.sound(this, t.position(), ModSounds.GRAVE_THUD, 2.4F, 0.6F);
                    Combat.hurt(t, this, owner, 14.0F * this.power);
                }
            });
        }

        if (this.tickCount % 10 == 0) {
            double a = this.random.nextDouble() * (float) (Math.PI * 2);
            double d = this.random.nextDouble() * (double)this.radius() * 0.9;
            level.sendParticles(
                ModParticles.smoke(4870200, 1.6F, 40), this.getX() + Math.cos(a) * d, this.getY() + 0.4, this.getZ() + Math.sin(a) * d, 2, 0.5, 0.2, 0.5, 0.01
            );
        }
    }

    private void gamble(ServerLevel level, Entity owner, List<LivingEntity> targets) {
        if (owner instanceof ServerPlayer p) {
            if (this.tickCount == 20 || this.tickCount == 82 || this.tickCount == 144) {
                boolean win = IdleDeathGamble.rollDomain(p);

                for (LivingEntity e : targets) {
                    e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 30, 0), owner);
                    Vfx.play(level, VfxKind.SHUTTER_DOORS, e.position(), new Vec3(1.0, 0.0, 0.0), Math.max(1.2F, e.getBbHeight()), 7004282, e.getId(), 1);
                    Combat.hurt(e, this, owner, 5.0F * this.power);
                }

                if (win) {
                    Scheduler.later(58, () -> {
                        if (p.isAlive()) {
                            PlayerData d = PlayerDataCap.get(p);
                            if (d != null) {
                                IdleDeathGamble.win(p, d);
                            }

                            this.endIn(25);
                        }
                    });
                }
            }
        }
    }

    private void judgement(ServerLevel level, Entity owner, List<LivingEntity> targets) {
        if (owner instanceof ServerPlayer p) {
            if (this.tickCount == 20) {
                LivingEntity pick = DeadlySentencing.pickDefendant(p, targets);
                if (pick == null) {
                    p.displayClientMessage(Component.translatable("message.fallenroses.trial_no_defendant").withStyle(ChatFormatting.GRAY), true);
                    this.endIn(20);
                    return;
                }

                this.defendant = pick.getUUID();
                DeadlySentencing.openTrial(p, this, pick);

                for (LivingEntity e : targets) {
                    e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 55, 0), owner);
                }
            }

            if (this.tickCount == 72 && this.defendant != null) {
                if (level.getEntity(this.defendant) instanceof LivingEntity t && t.isAlive()) {
                    DeadlySentencing.verdict(p, this, t);
                }

                this.endIn(22);
            }
        }
    }

    private void moonPalace(ServerLevel level, Entity owner, List<LivingEntity> targets) {
        if (owner instanceof LivingEntity lo) {
            lo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 2, false, false, false));
        }

        if (this.tickCount % 5 == 0) {
            for (LivingEntity e : targets) {
                Vec3 was = this.lastPos.put(e.getId(), e.position());
                if (was != null) {
                    double moved = was.distanceTo(e.position());
                    if (moved > 0.15) {
                        Combat.hurt(e, this, owner, (float)Math.min(9.0, 1.5 + moved * 3.5) * this.power);
                        Vfx.play(level, VfxKind.HIT_SPARK, e.getBoundingBox().getCenter(), Vec3.ZERO, 0.8F, 8050920, -1, 0);
                    } else if (this.tickCount % 20 == 0) {
                        Combat.hurt(e, this, owner, 1.5F * this.power);
                    }
                }
            }
        }

        if (this.tickCount % 40 == 0) {
            for (LivingEntity ex : targets) {
                ex.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 14, 0), owner);
                Vfx.play(level, VfxKind.FRAME_PANEL, ex.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, ex.getBbHeight()), 8050920, ex.getId(), 0);
            }
        }
    }

    private void station(ServerLevel level, Entity owner, List<LivingEntity> targets) {
        if (owner instanceof ServerPlayer p) {
            PlayerData d = PlayerDataCap.get(p);
            if (d != null) {
                d.stationTicks = 10;
            }
        }

        if (this.tickCount % 20 == 0) {
            for (LivingEntity e : targets) {
                for (MobEffectInstance eff : new ArrayList<>(e.getActiveEffects())) {
                    if (eff.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                        e.removeEffect(eff.getEffect());
                    }
                }

                e.invulnerableTime = 0;
                e.hurt(this.damageSources().indirectMagic(this, owner), 3.5F * this.power);
                Vfx.play(level, VfxKind.SOUL_TOUCH, e.getBoundingBox().getCenter(), Vec3.ZERO, e.getBbHeight(), 14692412, e.getId(), 0);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25600.0;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
