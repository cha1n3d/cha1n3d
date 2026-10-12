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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RokurokubiEntity extends CurseEntity {
    private int neckCooldown = 40;

    public RokurokubiEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            LivingEntity t = this.getTarget();
            if (t != null && t.isAlive() && --this.neckCooldown <= 0) {
                double d = (double)this.distanceTo(t);
                if (!(d < 2.5) && !(d > 8.0) && this.hasLineOfSight(t)) {
                    this.neckCooldown = 70;
                    ServerLevel level = (ServerLevel)this.level();
                    this.triggerAnim("action", "special");
                    this.getLookControl().setLookAt(t, 90.0F, 90.0F);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.KUCHISAKE.get(), SoundSource.HOSTILE, 1.4F, 1.3F);
                    Scheduler.later(7, () -> {
                        if (this.isAlive() && t.isAlive() && !(this.distanceTo(t) > 9.0F)) {
                            Vec3 head = this.getEyePosition();
                            Vfx.play(level, VfxKind.TOAD_TONGUE, head, t.getEyePosition().subtract(head), 1.0F, 15259848, t.getId(), 0);
                            Vfx.play(level, VfxKind.HIT_SPARK, t.getEyePosition(), t.position().subtract(this.position()), 1.0F, 13647952, -1, 0);
                            Combat.hurt(t, this, this, 7.0F);
                            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
                            Vec3 pull = this.position().subtract(t.position());
                            t.setDeltaMovement(pull.normalize().scale(0.9).add(0.0, 0.25, 0.0));
                            t.hurtMarked = true;
                        }
                    });
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
        return 380;
    }

    @Override
    public float energyReward() {
        return 5.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.KUCHISAKE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WOMB_WAIL.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WOMB_WAIL.get();
    }
}
