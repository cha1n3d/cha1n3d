package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.impl.BloodManipulation;
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

public class GashadokuroEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.gashadokuro"), BossBarColor.WHITE, BossBarOverlay.NOTCHED_10
    );

    public GashadokuroEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        this.setMaxUpStep(1.5F);
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 420.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 16.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GashadokuroEntity.Skills(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected int deathTicks() {
        return 60;
    }

    @Override
    public int grade() {
        return 0;
    }

    @Override
    public int xpReward() {
        return 2600;
    }

    @Override
    public float energyReward() {
        return 25.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GRAVE_THUD.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOSS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BOSS_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.5F;
    }

    static class Skills extends Goal {
        private final GashadokuroEntity mob;
        private int cooldown = 60;
        private int step;

        Skills(GashadokuroEntity mob) {
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
            LivingEntity t = this.mob.getTarget();
            if (t != null && --this.cooldown <= 0 && !(this.mob.distanceTo(t) > 22.0F)) {
                this.cooldown = 90;
                ServerLevel level = (ServerLevel)this.mob.level();
                this.mob.triggerAnim("action", "special");
                switch (this.step++ % 3) {
                    case 0:
                        Scheduler.later(12, () -> {
                            if (this.mob.isAlive()) {
                                Vec3 c = this.mob.position().add(this.mob.getLookAngle().multiply(2.5, 0.0, 2.5));
                                Vfx.play(level, VfxKind.SLAM, c, Vec3.ZERO, 7.0F);
                                level.playSound(null, c.x, c.y, c.z, ModSounds.SLAM.get(), SoundSource.HOSTILE, 3.0F, 0.5F);
                                Combat.shake(level, c, 1.6F, 16, 28.0);

                                for (LivingEntity e : Combat.around(this.mob, c, 7.0)) {
                                    if (!(e instanceof CurseEntity cu) || cu.isSummon()) {
                                        Combat.hurt(e, this.mob, this.mob, 14.0F);
                                        Combat.knock(e, e.position().subtract(c), 1.4, 0.7);
                                    }
                                }
                            }
                        });
                        break;
                    case 1:
                        if (this.mob.distanceTo(t) > 9.0F) {
                            this.cooldown = 10;
                            return;
                        }

                        t.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 36, 0), this.mob);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2.5F, 0.6F);

                        for (int i = 1; i <= 15; i++) {
                            Scheduler.later(i, () -> {
                                if (this.mob.isAlive() && t.isAlive()) {
                                    Vec3 hand = this.mob.position().add(this.mob.getLookAngle().multiply(1.6, 0.0, 1.6)).add(0.0, 5.5, 0.0);
                                    t.teleportTo(hand.x, hand.y, hand.z);
                                    t.setDeltaMovement(Vec3.ZERO);
                                    t.fallDistance = 0.0F;
                                }
                            });
                        }

                        Scheduler.later(18, () -> {
                            if (this.mob.isAlive() && t.isAlive()) {
                                t.setDeltaMovement(this.mob.getLookAngle().multiply(1.2, 0.0, 1.2).add(0.0, -2.2, 0.0));
                                t.hurtMarked = true;
                                Scheduler.later(5, () -> {
                                    Vec3 g = t.position();
                                    Vfx.play(level, VfxKind.GRAVE_IMPACT, g, Vec3.ZERO, 2.4F, 15261896, -1, 1);
                                    level.playSound(null, g.x, g.y, g.z, ModSounds.GRAVE_THUD.get(), SoundSource.HOSTILE, 2.5F, 0.6F);
                                    Combat.hurt(t, this.mob, this.mob, 18.0F);
                                });
                            }
                        });
                        break;
                    default:
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.GRAVE_THUD.get(), SoundSource.HOSTILE, 2.5F, 0.5F);
                        Vec3 dir = t.position().subtract(this.mob.position()).multiply(1.0, 0.0, 1.0).normalize();

                        for (int i = 1; i <= 9; i++) {
                            int k = i;
                            Scheduler.later(i * 2, () -> {
                                if (this.mob.isAlive()) {
                                    Vec3 at = BloodManipulation.groundBelow(this.mob, this.mob.position().add(dir.scale((double)k * 1.8)).add(0.0, 2.0, 0.0));
                                    Vfx.play(level, VfxKind.SPIKES, at, new Vec3(0.0, 1.0, 0.0), 1.4F, 15261896, -1, 0);

                                    for (LivingEntity e : Combat.around(this.mob, at.add(0.0, 1.0, 0.0), 1.8)) {
                                        if (!(e instanceof CurseEntity cu) || cu.isSummon()) {
                                            Combat.hurt(e, this.mob, this.mob, 10.0F);
                                            e.push(0.0, 0.7, 0.0);
                                            e.hurtMarked = true;
                                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), this.mob);
                                        }
                                    }
                                }
                            });
                        }
                }
            }
        }
    }
}
