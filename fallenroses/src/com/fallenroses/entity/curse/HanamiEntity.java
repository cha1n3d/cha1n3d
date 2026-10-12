package com.fallenroses.entity.curse;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.registry.ModEntities;
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

public class HanamiEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.hanami"), BossBarColor.GREEN, BossBarOverlay.NOTCHED_10
    );

    public HanamiEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 420.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 15.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 44.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new HanamiEntity.Skills(this));
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
            if (this.tickCount % 20 == 0 && this.level().isDay() && this.level().canSeeSky(this.blockPosition()) && this.getHealth() < this.getMaxHealth()) {
                this.heal(1.5F);
            }
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
        return 50;
    }

    @Override
    public int grade() {
        return 0;
    }

    @Override
    public int xpReward() {
        return 2800;
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
        return ModSounds.PLANT_GROW.get();
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
        private final HanamiEntity mob;
        private int cooldown = 60;
        private int step;

        Skills(HanamiEntity mob) {
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
            if (t != null && --this.cooldown <= 0 && !(this.mob.distanceTo(t) > 24.0F)) {
                this.cooldown = 85;
                ServerLevel level = (ServerLevel)this.mob.level();
                this.mob.triggerAnim("action", "special");
                switch (this.step++ % 3) {
                    case 0:
                        Vec3 at = t.position();
                        Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 2.5F, 5943360, -1, 0);
                        level.playSound(null, at.x, at.y, at.z, ModSounds.PLANT_GROW.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
                        Scheduler.later(14, () -> {
                            if (this.mob.isAlive()) {
                                Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 2.5F, 5943360, -1, 1);

                                for (LivingEntity e : Combat.around(this.mob, at.add(0.0, 1.0, 0.0), 3.0)) {
                                    if (!(e instanceof CurseEntity c) || c.isSummon()) {
                                        Combat.hurt(e, this.mob, this.mob, 14.0F);
                                        e.push(0.0, 1.0, 0.0);
                                        e.hurtMarked = true;
                                    }
                                }
                            }
                        });
                        break;
                    case 1:
                        Vfx.play(level, VfxKind.BUDS, t.position(), Vec3.ZERO, 1.5F, 5943360, t.getId(), 0);
                        level.playSound(null, t.getX(), t.getY(), t.getZ(), ModSounds.PLANT_GROW.get(), SoundSource.HOSTILE, 1.6F, 1.3F);

                        for (int i = 0; i < 4; i++) {
                            Scheduler.later(i * 15, () -> {
                                if (this.mob.isAlive() && t.isAlive()) {
                                    Combat.hurt(t, this.mob, this.mob, 4.0F);
                                    this.mob.heal(4.0F);
                                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1), this.mob);
                                }
                            });
                        }
                        break;
                    default:
                        for (int i = 0; i < 3; i++) {
                            Scheduler.later(
                                i * 8,
                                () -> {
                                    if (this.mob.isAlive() && t.isAlive()) {
                                        Vec3 from = this.mob.getEyePosition().add(0.0, 1.0, 0.0);
                                        new ShotEntity(ModEntities.SHOT.get(), level)
                                            .kind(ShotEntity.Kind.WOOD_BALL)
                                            .launch(this.mob, from, t.getEyePosition().subtract(from).normalize().scale(0.9), 1.3F);
                                    }
                                }
                            );
                        }

                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.SLAM.get(), SoundSource.HOSTILE, 1.6F, 1.2F);
                }
            }
        }
    }
}
