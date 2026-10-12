package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
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

public class CursedWombEntity extends CurseEntity {
    private boolean birthed;

    public CursedWombEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 70.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 3.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.5F) {
            @Override
            public void start() {
                super.start();
                CursedWombEntity.this.triggerAnim("action", "attack");
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
        }

        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && !this.birthed && !this.isSummon() && !(this.getHealth() > this.getMaxHealth() * 0.5F) && !this.isDeadOrDying()) {
            this.birthed = true;
            ServerLevel level = (ServerLevel)this.level();
            this.triggerAnim("action", "special");
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.WOMB_WAIL.get(), SoundSource.HOSTILE, 2.5F, 1.0F);

            for (int i = 0; i < 2; i++) {
                FlyHeadEntity fly = ModEntities.FLY_HEAD.get().create(level);
                if (fly != null) {
                    fly.moveTo(this.getX() + this.random.nextGaussian(), this.getY() + 1.2, this.getZ() + this.random.nextGaussian(), 0.0F, 0.0F);
                    fly.finalizeSpawn(level, level.getCurrentDifficultyAt(fly.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    fly.setTarget(this.getTarget());
                    level.addFreshEntity(fly);
                    Vfx.play(level, VfxKind.EXORCISM, fly.position(), Vec3.ZERO, 0.7F, 9048096, -1, 4);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Birthed", this.birthed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.birthed = tag.getBoolean("Birthed");
    }

    @Override
    public int grade() {
        return 2;
    }

    @Override
    public int xpReward() {
        return 200;
    }

    @Override
    public float energyReward() {
        return 5.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WOMB_WAIL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOUTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOUTH_DEATH.get();
    }
}
