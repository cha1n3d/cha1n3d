package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CursedOniEntity extends CurseEntity {
    private int slamCooldown = 60;
    private boolean enraged;

    public CursedOniEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            ServerLevel level = (ServerLevel)this.level();
            if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.4F) {
                this.enraged = true;
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
                Vfx.at(this, VfxKind.BERSERK, 1.4F, 0);
                level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ONI_ROAR.get(), SoundSource.HOSTILE, 2.5F, 0.8F);
            }

            LivingEntity t = this.getTarget();
            if (t != null && --this.slamCooldown <= 0 && !(this.distanceTo(t) > 6.0F)) {
                this.slamCooldown = this.enraged ? 70 : 110;
                this.triggerAnim("action", "special");
                Scheduler.later(14, () -> {
                    if (this.isAlive()) {
                        Vec3 at = this.position().add(this.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(2.0));
                        Vfx.play(level, VfxKind.SLAM, at, Vec3.ZERO, 4.0F);
                        level.playSound(null, at.x, at.y, at.z, ModSounds.SLAM.get(), SoundSource.HOSTILE, 2.2F, 0.7F);
                        Combat.shake(level, at, 1.2F, 12, 18.0);

                        for (LivingEntity e : Combat.around(this, at.add(0.0, 0.5, 0.0), 4.0)) {
                            if (!(e instanceof CurseEntity c) || c.isSummon() || this.isSummon()) {
                                Combat.hurt(e, this, this, 10.0F);
                                e.push(0.0, 0.8, 0.0);
                                e.hurtMarked = true;
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 500;
    }

    @Override
    public float energyReward() {
        return 6.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ONI_ROAR.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOSS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ONI_ROAR.get();
    }
}
