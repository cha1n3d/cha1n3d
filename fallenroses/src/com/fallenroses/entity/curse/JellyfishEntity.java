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

public class JellyfishEntity extends CurseEntity {
    public JellyfishEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 8, true);
        this.xpReward = 8;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 28.0)
            .add(Attributes.FLYING_SPEED, 0.3)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(2, new JellyfishEntity.StingGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.6));
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
        return 75;
    }

    @Override
    public float energyReward() {
        return 2.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.JELLY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.JELLY.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPLASH.get();
    }

    static class StingGoal extends Goal {
        private final JellyfishEntity mob;
        private int cooldown = 20;

        StingGoal(JellyfishEntity mob) {
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
        public void tick() {
            LivingEntity t = this.mob.getTarget();
            if (t != null) {
                this.mob.getMoveControl().setWantedPosition(t.getX(), t.getY() + 2.2, t.getZ(), 1.0);
                if (--this.cooldown <= 0 && !((double)this.mob.distanceTo(t) > 3.6)) {
                    this.cooldown = 40;
                    ServerLevel level = (ServerLevel)this.mob.level();
                    this.mob.triggerAnim("action", "attack");
                    Vfx.play(
                        level, VfxKind.STING, this.mob.position(), t.getBoundingBox().getCenter().subtract(this.mob.position()), 1.0F, 12099839, t.getId(), 0
                    );
                    level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.JELLY.get(), SoundSource.HOSTILE, 1.2F, 1.2F);

                    for (LivingEntity e : Combat.around(this.mob, this.mob.position().add(0.0, -1.2, 0.0), 2.2)) {
                        if (!(e instanceof CurseEntity c) || c.isSummon() || this.mob.isSummon()) {
                            Combat.hurt(e, this.mob, this.mob, (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this.mob);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), this.mob);
                        }
                    }
                }
            }
        }
    }
}
