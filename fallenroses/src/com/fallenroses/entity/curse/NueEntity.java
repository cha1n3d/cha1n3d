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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NueEntity extends CurseEntity {
    public NueEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.FLYING_SPEED, 0.7)
            .add(Attributes.MOVEMENT_SPEED, 0.35)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
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
        this.goalSelector.addGoal(2, new NueEntity.DiveGoal(this));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected int deathTicks() {
        return 20;
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 0;
    }

    @Override
    public float energyReward() {
        return 0.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.NUE_SCREECH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.NUE_SCREECH.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHADOW_SUMMON.get();
    }

    static class DiveGoal extends Goal {
        private final NueEntity mob;
        private int cooldown = 30;
        private int diving = -1;
        private float angle;

        DiveGoal(NueEntity mob) {
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
                if (this.diving < 0) {
                    this.angle += 9.0F;
                    double a = Math.toRadians((double)this.angle);
                    this.mob.getMoveControl().setWantedPosition(target.getX() + Math.cos(a) * 4.0, target.getY() + 5.0, target.getZ() + Math.sin(a) * 4.0, 1.2);
                    if (--this.cooldown <= 0) {
                        this.diving = 14;
                        this.mob.triggerAnim("action", "special");
                        this.mob
                            .level()
                            .playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.NUE_SCREECH.get(), SoundSource.HOSTILE, 1.6F, 1.1F);
                    }
                } else {
                    this.mob.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.0, target.getZ(), 2.0);
                    if (--this.diving <= 0 || (double)this.mob.distanceTo(target) < 2.2) {
                        this.diving = -1;
                        this.cooldown = 50;
                        ServerLevel level = (ServerLevel)this.mob.level();
                        Vec3 at = target.position();
                        Vfx.play(level, VfxKind.LIGHTNING, at);
                        level.playSound(null, at.x, at.y, at.z, ModSounds.THUNDER.get(), SoundSource.HOSTILE, 1.4F, 1.3F);
                        float dmg = (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F;

                        for (LivingEntity e : Combat.around(this.mob, at.add(0.0, 0.8, 0.0), 2.4)) {
                            Combat.hurt(e, this.mob, (Entity)(this.mob.summonerPlayer() != null ? this.mob.summonerPlayer() : this.mob), dmg);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), this.mob);
                        }

                        this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.8, 0.0));
                    }
                }
            }
        }
    }
}
