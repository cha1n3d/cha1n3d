package com.fallenroses.entity.curse;

import com.fallenroses.entity.CurseOrbEntity;
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

public class HangedEntity extends CurseEntity {
    public HangedEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 15;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.FLYING_SPEED, 0.35)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(false);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new HangedEntity.CastGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 160;
    }

    @Override
    public float energyReward() {
        return 4.0F;
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
        return ModSounds.HANGED_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HANGED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HANGED_DEATH.get();
    }

    static class CastGoal extends Goal {
        private final HangedEntity mob;
        private int cooldown;

        CastGoal(HangedEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                double dist = (double)this.mob.distanceTo(target);
                Vec3 above = target.position().add(0.0, 3.5, 0.0);
                if (dist > 12.0) {
                    this.mob.getNavigation().moveTo(above.x, above.y, above.z, 1.0);
                } else if (dist < 7.0) {
                    Vec3 away = this.mob.position().subtract(target.position()).normalize().scale(4.0);
                    this.mob.getMoveControl().setWantedPosition(this.mob.getX() + away.x, target.getY() + 3.5, this.mob.getZ() + away.z, 1.0);
                } else {
                    this.mob.getNavigation().stop();
                }

                if (--this.cooldown <= 0 && dist < 20.0 && this.mob.hasLineOfSight(target)) {
                    this.cooldown = 50;
                    this.mob.triggerAnim("action", "special");
                    Vec3 from = this.mob.getEyePosition();
                    Vec3 dir = target.getBoundingBox().getCenter().subtract(from).normalize();
                    new CurseOrbEntity(ModEntities.CURSE_ORB.get(), this.mob.level()).launch(this.mob, from.add(dir), dir.scale(0.55), 1.0F);
                    this.mob
                        .level()
                        .playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.HANGED_SHOOT.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
                }
            }
        }
    }
}
