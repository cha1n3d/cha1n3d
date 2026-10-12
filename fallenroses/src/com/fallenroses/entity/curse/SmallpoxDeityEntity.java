package com.fallenroses.entity.curse;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.registry.ModEffects;
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

public class SmallpoxDeityEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.smallpox_deity"), BossBarColor.GREEN, BossBarOverlay.NOTCHED_6
    );

    public SmallpoxDeityEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 260.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SmallpoxDeityEntity.Skills(this));
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
        return 50;
    }

    @Override
    public int grade() {
        return 1;
    }

    @Override
    public int xpReward() {
        return 1100;
    }

    @Override
    public float energyReward() {
        return 14.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.POX_MOAN.get();
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
        return ModSounds.BOSS_DEATH.get();
    }

    static class Skills extends Goal {
        private final SmallpoxDeityEntity mob;
        private int cooldown = 50;
        private int step;

        Skills(SmallpoxDeityEntity mob) {
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
            if (t != null && --this.cooldown <= 0 && !(this.mob.distanceTo(t) > 20.0F)) {
                this.cooldown = 100;
                ServerLevel level = (ServerLevel)this.mob.level();
                this.mob.triggerAnim("action", "special");
                switch (this.step++ % 3) {
                    case 0:
                        for (int i = 0; i < 4; i++) {
                            int k = i;
                            Scheduler.later(
                                i * 5,
                                () -> {
                                    if (this.mob.isAlive() && t.isAlive()) {
                                        Vec3 at = t.position().add(this.mob.getRandom().nextGaussian() * 1.5, 9.0, this.mob.getRandom().nextGaussian() * 1.5);
                                        new ShotEntity(ModEntities.SHOT.get(), level)
                                            .kind(ShotEntity.Kind.GRAVESTONE)
                                            .launch(this.mob, at, new Vec3(0.0, -0.4, 0.0), 1.0F + (float)k * 0.05F);
                                    }
                                }
                            );
                        }

                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.POX_MOAN.get(), SoundSource.HOSTILE, 2.0F, 0.9F);
                        break;
                    case 1:
                        Vec3 trap = t.position();
                        t.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 60, 0), this.mob);
                        Vfx.play(level, VfxKind.COFFIN, trap, Vec3.ZERO, Math.max(1.0F, t.getBbHeight()), 5917232, t.getId(), 60);

                        for (int i = 0; i < 3; i++) {
                            int n = 3 - i;
                            Scheduler.later(
                                i * 20,
                                () -> {
                                    level.playSound(
                                        null, trap.x, trap.y, trap.z, ModSounds.COFFIN_BELL.get(), SoundSource.HOSTILE, 2.0F, 0.7F + (float)(3 - n) * 0.1F
                                    );
                                    if (t instanceof ServerPlayer sp) {
                                        sp.displayClientMessage(
                                            Component.translatable("message.fallenroses.coffin_count", n).withStyle(s -> s.withColor(13156480).withBold(true)),
                                            true
                                        );
                                    }
                                }
                            );
                        }

                        Scheduler.later(60, () -> {
                            if (this.mob.isAlive() && t.isAlive() && !(t.position().distanceTo(trap) > 2.5)) {
                                Vfx.play(level, VfxKind.GRAVE_IMPACT, trap, Vec3.ZERO, 2.2F, 10133600, -1, 1);
                                level.playSound(null, trap.x, trap.y, trap.z, ModSounds.GRAVE_THUD.get(), SoundSource.HOSTILE, 2.5F, 0.6F);
                                Combat.hurt(t, this.mob, this.mob, 16.0F);
                            }
                        });
                        break;
                    default:
                        Vfx.play(level, VfxKind.POX_CLOUD, this.mob.position(), Vec3.ZERO, 5.0F, 10133600, -1, 80);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.PLAGUE.get(), SoundSource.HOSTILE, 2.0F, 0.8F);

                        for (int i = 0; i < 4; i++) {
                            Scheduler.later(i * 20, () -> {
                                if (this.mob.isAlive()) {
                                    for (LivingEntity e : Combat.around(this.mob, this.mob.position(), 5.0)) {
                                        if (!(e instanceof CurseEntity c) || c.isSummon()) {
                                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), this.mob);
                                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this.mob);
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
