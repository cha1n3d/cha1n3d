package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class KappaEntity extends CurseEntity {
    private int grabCooldown = 40;

    public KappaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 34.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (this.isInWaterOrRain() && this.tickCount % 20 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(1.5F);
            }

            LivingEntity t = this.getTarget();
            if (t != null && --this.grabCooldown <= 0) {
                double d = (double)this.distanceTo(t);
                if (!(d < 3.5) && !(d > 9.0) && this.hasLineOfSight(t)) {
                    this.grabCooldown = 90;
                    ServerLevel level = (ServerLevel)this.level();
                    this.triggerAnim("action", "special");
                    Vec3 from = this.position().add(0.0, 1.1, 0.0);
                    Vfx.play(level, VfxKind.TOAD_TONGUE, from, t.getBoundingBox().getCenter().subtract(from), 1.0F, 5933642, t.getId(), 0);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.KAPPA.get(), SoundSource.HOSTILE, 1.4F, 0.9F);
                    Vec3 pull = this.position().subtract(t.position()).normalize().scale(Math.min(1.6, d * 0.2));
                    t.setDeltaMovement(pull.x, 0.35, pull.z);
                    t.hurtMarked = true;
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                }
            }
        }
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
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
        return ModSounds.KAPPA.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOUTH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPLASH.get();
    }
}
