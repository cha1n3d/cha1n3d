package com.fallenroses.entity.curse;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CursedDollEntity extends CurseEntity {
    private int needleCooldown = 40;

    public CursedDollEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.36)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            LivingEntity t = this.getTarget();
            if (t != null && --this.needleCooldown <= 0) {
                double d = (double)this.distanceTo(t);
                if (!(d > 8.0) && this.hasLineOfSight(t)) {
                    this.needleCooldown = 70;
                    this.triggerAnim("action", "special");
                    this.level()
                        .playSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            ModSounds.DOLL_LAUGH.get(),
                            SoundSource.HOSTILE,
                            1.2F,
                            1.1F + this.random.nextFloat() * 0.2F
                        );
                    Vec3 from = this.position().add(0.0, (double)this.getBbHeight() * 0.7, 0.0);
                    Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize();

                    for (int i = 0; i < 4; i++) {
                        Vec3 v = dir.add(this.random.nextGaussian() * 0.08, this.random.nextGaussian() * 0.08, this.random.nextGaussian() * 0.08)
                            .normalize()
                            .scale(1.2);
                        new ShotEntity(ModEntities.SHOT.get(), this.level()).kind(ShotEntity.Kind.NAIL).launch(this, from, v, 0.7F);
                    }
                }
            }
        }
    }

    @Override
    public int grade() {
        return 4;
    }

    @Override
    public int xpReward() {
        return 30;
    }

    @Override
    public float energyReward() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DOLL_LAUGH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ICE_SHATTER.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ICE_SHATTER.get();
    }
}
