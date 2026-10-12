package com.fallenroses.entity.curse;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HannyaMaskEntity extends CurseEntity {
    public HannyaMaskEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        this.xpReward = 15;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.FLYING_SPEED, 0.45)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
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
        this.goalSelector.addGoal(2, new HannyaMaskEntity.BreathGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 380;
    }

    @Override
    public float energyReward() {
        return 5.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MASK_HISS.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HANGED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HANGED_DEATH.get();
    }

    static class BreathGoal extends Goal {
        private final HannyaMaskEntity mob;
        private int cooldown = 30;

        BreathGoal(HannyaMaskEntity mob) {
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
            LivingEntity t = this.mob.getTarget();
            if (t != null) {
                this.mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
                double d = (double)this.mob.distanceTo(t);
                if (d > 11.0) {
                    this.mob.getNavigation().moveTo(t.getX(), t.getY() + 3.0, t.getZ(), 1.0);
                } else if (d < 5.0) {
                    Vec3 away = this.mob.position().subtract(t.position()).normalize().scale(4.0);
                    this.mob.getMoveControl().setWantedPosition(this.mob.getX() + away.x, t.getY() + 3.0, this.mob.getZ() + away.z, 1.0);
                } else {
                    this.mob.getNavigation().stop();
                }

                if (--this.cooldown <= 0 && !(d > 18.0) && this.mob.hasLineOfSight(t)) {
                    this.cooldown = 55;
                    this.mob.triggerAnim("action", "attack");
                    this.mob
                        .level()
                        .playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.MASK_HISS.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
                    Vec3 from = this.mob.getEyePosition();
                    Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize();

                    for (int i = 0; i < 2; i++) {
                        Vec3 v = dir.add(this.mob.getRandom().nextGaussian() * 0.25, 0.15, this.mob.getRandom().nextGaussian() * 0.25).normalize().scale(0.5);
                        new ShotEntity(ModEntities.SHOT.get(), this.mob.level()).kind(ShotEntity.Kind.EMBER_INSECT).launch(this.mob, from.add(dir), v, 0.9F);
                    }
                }
            }
        }
    }
}
