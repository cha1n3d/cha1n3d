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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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

public class IttanMomenEntity extends CurseEntity {
    public IttanMomenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 8;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.FLYING_SPEED, 0.5)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 28.0);
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
        this.goalSelector.addGoal(2, new IttanMomenEntity.WrapGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.7));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
        return 3;
    }

    @Override
    public int xpReward() {
        return 80;
    }

    @Override
    public float energyReward() {
        return 2.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MOTH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HANGED_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HANGED_DEATH.get();
    }

    static class WrapGoal extends Goal {
        private final IttanMomenEntity mob;
        private int wrapTicks;
        private int cooldown = 20;

        WrapGoal(IttanMomenEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
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
        public void stop() {
            this.wrapTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = this.mob.getTarget();
            if (t != null) {
                if (this.wrapTicks > 0) {
                    this.wrapTicks--;
                    this.mob.setPos(t.getX(), t.getEyeY() - 0.3, t.getZ());
                    this.mob.setDeltaMovement(Vec3.ZERO);
                    if (this.wrapTicks % 20 == 0) {
                        Combat.hurt(t, this.mob, this.mob, (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
                        t.setAirSupply(Math.max(0, t.getAirSupply() - 60));
                    }

                    if (this.mob.hurtTime > 0 || this.wrapTicks == 0) {
                        this.wrapTicks = 0;
                        this.cooldown = 100;
                        this.mob.setDeltaMovement(new Vec3(this.mob.getRandom().nextGaussian() * 0.4, 0.6, this.mob.getRandom().nextGaussian() * 0.4));
                    }
                } else {
                    this.mob.getMoveControl().setWantedPosition(t.getX(), t.getEyeY(), t.getZ(), 1.2);
                    if (--this.cooldown <= 0 && !((double)this.mob.distanceTo(t) > 1.8)) {
                        this.wrapTicks = 60;
                        this.mob.triggerAnim("action", "special");
                        ServerLevel level = (ServerLevel)this.mob.level();
                        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0), this.mob);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this.mob);
                        Vfx.play(level, VfxKind.TWIST, t.getEyePosition(), Vec3.ZERO, 0.8F, 15789800, t.getId(), 0);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.MOTH.get(), SoundSource.HOSTILE, 1.2F, 1.4F);
                    }
                }
            }
        }
    }
}
