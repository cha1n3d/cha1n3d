package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EyeClusterEntity extends CurseEntity {
    public EyeClusterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 10;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 32.0)
            .add(Attributes.FLYING_SPEED, 0.4)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new EyeClusterEntity.GazeGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int grade() {
        return 3;
    }

    @Override
    public int xpReward() {
        return 90;
    }

    @Override
    public float energyReward() {
        return 2.5F;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPIDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SPIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FLY_DEATH.get();
    }

    static class GazeGoal extends Goal {
        private final EyeClusterEntity mob;
        private int cooldown = 40;
        private int charging = -1;

        GazeGoal(EyeClusterEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                double dist = (double)this.mob.distanceTo(target);
                if (this.charging < 0) {
                    if (dist > 12.0) {
                        this.mob.getNavigation().moveTo(target.getX(), target.getY() + 3.0, target.getZ(), 1.0);
                    } else if (dist < 6.0) {
                        Vec3 away = this.mob.position().subtract(target.position()).normalize().scale(4.0);
                        this.mob.getMoveControl().setWantedPosition(this.mob.getX() + away.x, target.getY() + 3.0, this.mob.getZ() + away.z, 1.0);
                    } else {
                        this.mob.getNavigation().stop();
                    }
                } else {
                    this.mob.getNavigation().stop();
                }

                ServerLevel level = (ServerLevel)this.mob.level();
                if (this.charging < 0 && --this.cooldown <= 0 && dist < 20.0 && this.mob.hasLineOfSight(target)) {
                    this.charging = 20;
                    this.mob.triggerAnim("action", "special");
                    Vfx.play(
                        level,
                        VfxKind.EYE_BEAM,
                        this.mob.getEyePosition(),
                        target.getBoundingBox().getCenter().subtract(this.mob.getEyePosition()),
                        1.0F,
                        16719920,
                        target.getId(),
                        0
                    );
                    level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.EYE_BEAM.get(), SoundSource.HOSTILE, 1.2F, 1.4F);
                }

                if (this.charging >= 0 && --this.charging == 0) {
                    this.charging = -1;
                    this.cooldown = 60;
                    Vec3 from = this.mob.getEyePosition();
                    Vec3 to = target.getBoundingBox().getCenter();
                    BlockHitResult wall = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this.mob));
                    if (wall.getType() == Type.MISS) {
                        Combat.hurt(target, this.mob, this.mob, 7.0F);
                    } else {
                        to = wall.getLocation();
                    }

                    Vfx.play(level, VfxKind.EYE_BEAM, from, to.subtract(from), 1.0F, 16719920, -1, 1);
                    level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.EYE_BEAM.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
                }
            }
        }
    }
}
