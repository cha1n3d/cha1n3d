package com.fallenroses.entity.curse;

import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class CurseEntity extends Monster implements GeoEntity, CursedSpirit {
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation SPECIAL = RawAnimation.begin().thenPlay("special");
    protected static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID summoner;
    private int summonLife = -1;

    protected CurseEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    protected int deathTicks() {
        return 30;
    }

    protected float exorcismScale() {
        return this.getBbHeight();
    }

    protected void standardGoals(double speed) {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, speed, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    public static <T extends CurseEntity> T summon(EntityType<T> type, ServerPlayer owner, Vec3 at, int life) {
        ServerLevel level = owner.serverLevel();
        T e = (T)type.create(level);
        if (e == null) {
            return null;
        } else {
            e.moveTo(at.x, at.y, at.z, owner.getYRot(), 0.0F);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            ((CurseEntity)e).summoner = owner.getUUID();
            ((CurseEntity)e).summonLife = life;
            e.setPersistenceRequired();
            level.addFreshEntity(e);
            Vfx.play(level, VfxKind.SUMMON, at, Vec3.ZERO, Math.max(1.0F, e.getBbHeight()), 3844744, e.getId(), 0);
            return e;
        }
    }

    public boolean isSummon() {
        return this.summoner != null;
    }

    @Nullable
    public Player summonerPlayer() {
        return this.summoner == null ? null : this.level().getPlayerByUUID(this.summoner);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (this.summoner != null) {
            if (this.summoner.equals(other.getUUID())) {
                return true;
            }

            if (other instanceof CurseEntity c && this.summoner.equals(c.summoner)) {
                return true;
            }
        }

        if (other instanceof CurseEntity c && c.summoner != null && c.summoner.equals(this.getUUID())) {
            return true;
        }

        return super.isAlliedTo(other);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null) {
            if (this.summoner != null) {
                if (target instanceof Player || this.isAlliedTo(target)) {
                    return;
                }
            } else if (target instanceof Player player && this.getLastHurtByMob() != player && isFellowSpirit(player)) {
                return;
            }
        }

        super.setTarget(target);
    }

    private static boolean isFellowSpirit(Player player) {
        PlayerData data = PlayerDataCap.get(player);
        return data != null && data.getPlayerClass() == PlayerClass.CURSED_SPIRIT;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.summoner != null && !this.isDeadOrDying()) {
            Player owner = this.summonerPlayer();
            if (owner != null && owner.isAlive() && --this.summonLife > 0) {
                LivingEntity target = this.getTarget();
                if (target == null || !target.isAlive() || target == owner) {
                    LivingEntity pick = owner.getLastHurtMob();
                    if (pick == null || !pick.isAlive() || pick.distanceTo(owner) > 24.0F || this.isAlliedTo(pick)) {
                        pick = owner.getLastHurtByMob();
                    }

                    if (pick == null || !pick.isAlive() || this.isAlliedTo(pick)) {
                        pick = this.level()
                            .getNearestEntity(
                                this.level()
                                    .getEntitiesOfClass(
                                        Monster.class,
                                        owner.getBoundingBox().inflate(12.0),
                                        m -> m != this && m instanceof Enemy && !this.isAlliedTo(m) && m.isAlive()
                                    ),
                                TargetingConditions.forCombat(),
                                this,
                                this.getX(),
                                this.getY(),
                                this.getZ()
                            );
                    }

                    super.setTarget(pick != null && pick.isAlive() && !this.isAlliedTo(pick) ? pick : null);
                }

                double d = (double)this.distanceTo(owner);
                if (d > 32.0) {
                    this.teleportTo(owner.getX(), owner.getY() + 1.0, owner.getZ());
                } else if (this.getTarget() == null && d > 6.0 && this.tickCount % 10 == 0) {
                    this.getNavigation().moveTo(owner, 1.1);
                }
            } else {
                this.vanish();
            }
        }
    }

    private void vanish() {
        if (this.level() instanceof ServerLevel level) {
            Vfx.play(level, VfxKind.EXORCISM, this.getBoundingBox().getCenter(), Vec3.ZERO, this.exorcismScale() * 0.6F, 3844744, this.getId(), 4);
        }

        this.discard();
    }

    @Override
    protected boolean shouldDropLoot() {
        return this.summoner == null && super.shouldDropLoot();
    }

    @Override
    public int getExperienceReward() {
        return this.summoner == null ? super.getExperienceReward() : 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.summoner != null) {
            tag.putUUID("Summoner", this.summoner);
            tag.putInt("SummonLife", this.summonLife);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Summoner")) {
            this.summoner = tag.getUUID("Summoner");
            this.summonLife = tag.getInt("SummonLife");
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<>(
                this, "move", 5, state -> this.isDeadOrDying() ? state.setAndContinue(DEATH) : state.setAndContinue(state.isMoving() ? WALK : IDLE)
            )
        );
        controllers.add(
            new AnimationController<>(this, "action", 2, state -> PlayState.STOP).triggerableAnim("attack", ATTACK).triggerableAnim("special", SPECIAL)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.triggerAnim("action", "attack");
        return super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            Vfx.play(
                level,
                VfxKind.EXORCISM,
                this.getBoundingBox().getCenter(),
                Vec3.ZERO,
                this.exorcismScale(),
                this.summoner != null ? 3844744 : 6957744,
                this.getId(),
                this.grade()
            );
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= this.deathTicks() && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.summoner == null && this.grade() > 0;
    }
}
