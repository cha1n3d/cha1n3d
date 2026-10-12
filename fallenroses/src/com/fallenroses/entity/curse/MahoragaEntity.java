package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MahoragaEntity extends CurseEntity {
    private final Map<String, Integer> adapted = new HashMap<>();
    private final ServerBossEvent bar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.mahoraga"), BossBarColor.WHITE, BossBarOverlay.NOTCHED_10
    );

    public MahoragaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 260.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MahoragaEntity.SwordGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isDeadOrDying()) {
            String key = source.getMsgId();
            int n = this.adapted.getOrDefault(key, 0);
            float reduced = amount * Math.max(0.25F, 1.0F - (float)n * 0.15F);
            boolean hit = super.hurt(source, reduced);
            if (hit && n < 5) {
                this.adapted.put(key, n + 1);
                if (this.level() instanceof ServerLevel level) {
                    Vfx.play(
                        level,
                        VfxKind.ADAPT_WHEEL,
                        this.position().add(0.0, (double)this.getBbHeight() + 0.6, 0.0),
                        Vec3.ZERO,
                        1.0F,
                        15788224,
                        this.getId(),
                        n + 1
                    );
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.WHEEL.get(), SoundSource.HOSTILE, 1.6F, 0.9F + (float)n * 0.05F);
                }
            }

            return hit;
        } else {
            return super.hurt(source, amount);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.bar.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bar.removePlayer(player);
    }

    @Override
    protected int deathTicks() {
        return 40;
    }

    @Override
    public int grade() {
        return 0;
    }

    @Override
    public int xpReward() {
        return 0;
    }

    @Override
    public float energyReward() {
        return 0.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WHEEL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOSS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BOSS_DEATH.get();
    }

    static class SwordGoal extends Goal {
        private final MahoragaEntity mob;
        private int cooldown = 40;

        SwordGoal(MahoragaEntity mob) {
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
            if (target != null && --this.cooldown <= 0 && !(this.mob.distanceTo(target) > 7.0F)) {
                this.cooldown = 80;
                this.mob.triggerAnim("action", "special");
                ServerLevel level = (ServerLevel)this.mob.level();
                Scheduler.later(12, () -> {
                    if (this.mob.isAlive()) {
                        Vec3 at = this.mob.position().add(this.mob.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(3.0));
                        Vfx.play(level, VfxKind.SLAM, at, Vec3.ZERO, 4.5F);
                        Vfx.play(level, VfxKind.CLEAVE, at.add(0.0, 1.0, 0.0), Vec3.ZERO, 3.0F, 16777215, -1, 0);
                        level.playSound(null, at.x, at.y, at.z, ModSounds.SLAM.get(), SoundSource.HOSTILE, 2.5F, 0.6F);
                        Combat.shake(level, at, 1.5F, 14, 22.0);
                        float dmg = (float)this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.6F;

                        for (LivingEntity e : Combat.around(this.mob, at.add(0.0, 0.5, 0.0), 3.8)) {
                            Combat.hurt(e, this.mob, (Entity)(this.mob.summonerPlayer() != null ? this.mob.summonerPlayer() : this.mob), dmg);
                            Combat.knock(e, e.position().subtract(this.mob.position()), 1.4, 0.6);
                        }
                    }
                });
            }
        }
    }
}
