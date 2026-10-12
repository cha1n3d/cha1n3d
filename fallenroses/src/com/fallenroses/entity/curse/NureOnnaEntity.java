package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class NureOnnaEntity extends CurseEntity {
    private int spitCooldown = 50;
    private int coilCooldown = 60;

    public NureOnnaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 70.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, this.getTarget() != null && this.getTarget().getY() > this.getY() ? 0.03 : 0.01, 0.0));
            }

            LivingEntity t = this.getTarget();
            if (t != null && t.isAlive()) {
                ServerLevel level = (ServerLevel)this.level();
                double d = (double)this.distanceTo(t);
                if (--this.coilCooldown <= 0 && d < 2.6) {
                    this.coilCooldown = 120;
                    this.triggerAnim("action", "special");
                    t.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 30, 0), this);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
                    Combat.hurt(t, this, this, 6.0F);
                    Vfx.play(level, VfxKind.WHIRLPOOL, t.position(), new Vec3(0.0, 1.0, 0.0), 1.4F, 3828314, t.getId(), 30);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MASK_HISS.get(), SoundSource.HOSTILE, 1.4F, 0.8F);
                } else if (--this.spitCooldown <= 0 && d > 3.0 && d < 12.0 && this.hasLineOfSight(t)) {
                    this.spitCooldown = 70;
                    this.triggerAnim("action", "attack");
                    Vec3 mouth = this.getEyePosition();
                    Vfx.play(level, VfxKind.TOAD_TONGUE, mouth, t.getEyePosition().subtract(mouth), 1.0F, 4892880, t.getId(), 0);
                    Vfx.play(level, VfxKind.SPLASH, t.getEyePosition(), Vec3.ZERO, 1.0F, 2788048, -1, 0);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPLASH.get(), SoundSource.HOSTILE, 1.4F, 0.9F);
                    Combat.hurt(t, this, this, 5.0F);
                    Combat.knock(t, t.position().subtract(this.position()), 0.6, 0.2);
                    t.clearFire();
                }
            }
        }
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 400;
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
        return ModSounds.SPLASH.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WOMB_WAIL.get();
    }
}
