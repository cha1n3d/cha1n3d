package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class KuchisakeEntity extends CurseEntity {
    private int blinkCooldown = 60;

    public KuchisakeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 70.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            LivingEntity t = this.getTarget();
            if (t != null && --this.blinkCooldown <= 0) {
                double d = (double)this.distanceTo(t);
                if (!(d < 3.0) && !(d > 22.0)) {
                    Vec3 toMe = this.position().add(0.0, 1.5, 0.0).subtract(t.getEyePosition()).normalize();
                    if (!(t.getViewVector(1.0F).dot(toMe) > 0.55)) {
                        this.blinkCooldown = 110;
                        ServerLevel level = (ServerLevel)this.level();
                        Vec3 behind = t.position().subtract(t.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(1.6));
                        Vfx.play(level, VfxKind.EXORCISM, this.position().add(0.0, 1.2, 0.0), Vec3.ZERO, 1.2F, 2755088, this.getId(), 4);
                        this.teleportTo(behind.x, t.getY(), behind.z);
                        this.getLookControl().setLookAt(t, 90.0F, 90.0F);
                        this.triggerAnim("action", "special");
                        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.KUCHISAKE.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
                        if (t instanceof ServerPlayer sp) {
                            sp.displayClientMessage(
                                Component.translatable("message.fallenroses.kuchisake").withStyle(s -> s.withColor(13647952).withItalic(true)), true
                            );
                        }

                        Vfx.play(level, VfxKind.SCISSORS, t.getBoundingBox().getCenter(), t.position().subtract(this.position()), 1.3F, 15263984, t.getId(), 0);
                        Combat.hurt(t, this, this, 8.0F);
                        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0), this);
                    }
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
        return ModSounds.KUCHISAKE.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
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
