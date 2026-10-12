package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FingerBearerEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.finger_bearer"), BossBarColor.PURPLE, BossBarOverlay.NOTCHED_6
    );

    public FingerBearerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 220.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FingerBearerEntity.Skills(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isSummon()) {
            this.bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected int deathTicks() {
        return 45;
    }

    @Override
    public int grade() {
        return 1;
    }

    @Override
    public int xpReward() {
        return 900;
    }

    @Override
    public float energyReward() {
        return 12.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BOSS_ROAR.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    public float getVoicePitch() {
        return 1.35F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOSS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BOSS_DEATH.get();
    }

    static class Skills extends Goal {
        private final FingerBearerEntity mob;
        private int cooldown = 60;
        private int step;

        Skills(FingerBearerEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.noneOf(Flag.class));
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
            if (target != null && --this.cooldown <= 0) {
                ServerLevel level = (ServerLevel)this.mob.level();
                double dist = (double)this.mob.distanceTo(target);
                int skill = this.step++ % 3;
                this.cooldown = 90;
                this.mob.triggerAnim("action", "special");
                switch (skill) {
                    case 0:
                        Scheduler.later(10, () -> {
                            if (this.mob.isAlive()) {
                                Vec3 c = this.mob.position();
                                Vfx.play(level, VfxKind.SLAM, c, Vec3.ZERO, 5.0F);
                                level.playSound(null, c.x, c.y, c.z, ModSounds.SLAM.get(), SoundSource.HOSTILE, 2.5F, 0.7F);
                                Combat.shake(level, c, 1.4F, 16, 20.0);

                                for (LivingEntity e : Combat.around(this.mob, c.add(0.0, 0.5, 0.0), 5.0)) {
                                    Combat.hurt(e, this.mob, this.mob, 10.0F);
                                    e.push(0.0, 0.9, 0.0);
                                    e.hurtMarked = true;
                                }
                            }
                        });
                        break;
                    case 1:
                        Vfx.play(level, VfxKind.BEARER_SCREAM, this.mob.getEyePosition(), this.mob.getViewVector(1.0F), 1.0F, 10109183, this.mob.getId(), 0);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 3.0F, 1.4F);

                        for (LivingEntity e : Combat.around(this.mob, this.mob.position(), 9.0)) {
                            Vec3 to = e.position().subtract(this.mob.position());
                            if (!(to.normalize().dot(this.mob.getViewVector(1.0F)) < 0.3)) {
                                Combat.hurt(e, this.mob, this.mob, 5.0F);
                                Combat.knock(e, to, 1.8, 0.5);
                                e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 30, 0), this.mob);
                                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), this.mob);
                            }
                        }
                        break;
                    default:
                        if (dist > 3.0 && dist < 16.0) {
                            Vec3 to = target.position().subtract(this.mob.position());
                            Vec3 dest = target.position().subtract(new Vec3(to.x, 0.0, to.z).normalize().scale(1.6));
                            Vfx.play(level, VfxKind.DASH, this.mob.position().add(0.0, 1.4, 0.0), dest.subtract(this.mob.position()), 1.5F);
                            this.mob.teleportTo(dest.x, dest.y, dest.z);
                            level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.DASH.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
                        }

                        if (this.mob.distanceTo(target) < 4.0F) {
                            Combat.hurt(target, this.mob, this.mob, 12.0F);
                            Vfx.play(level, VfxKind.CLEAVE, target.getBoundingBox().getCenter(), Vec3.ZERO, target.getBbHeight(), 16777215, target.getId(), 0);
                        }
                }
            }
        }
    }
}
