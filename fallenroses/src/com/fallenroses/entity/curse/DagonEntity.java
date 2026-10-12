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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class DagonEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.dagon"), BossBarColor.BLUE, BossBarOverlay.NOTCHED_10
    );

    public DagonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 380.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 44.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new DagonEntity.Skills(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
            LivingEntity t = this.getTarget();
            if (this.isInWater() && t != null) {
                Vec3 to = t.position().subtract(this.position());
                if (to.lengthSqr() > 4.0) {
                    this.setDeltaMovement(this.getDeltaMovement().add(to.normalize().scale(0.05)));
                }
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
        return ModSounds.WAVE.get();
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
        private final DagonEntity mob;
        private int cooldown = 60;
        private int step;

        Skills(DagonEntity mob) {
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
                this.cooldown = 80;
                ServerLevel level = (ServerLevel)this.mob.level();
                this.mob.triggerAnim("action", "special");
                switch (this.step++ % 3) {
                    case 0:
                        for (int i = 0; i < 6; i++) {
                            Scheduler.later(
                                i * 3,
                                () -> {
                                    if (this.mob.isAlive()) {
                                        Vec3 from = this.mob
                                            .getEyePosition()
                                            .add(this.mob.getRandom().nextGaussian(), 0.5, this.mob.getRandom().nextGaussian());
                                        new ShotEntity(ModEntities.SHOT.get(), level)
                                            .kind(ShotEntity.Kind.FISH)
                                            .launch(this.mob, from, t.getEyePosition().subtract(from).normalize().scale(0.6), 1.4F);
                                    }
                                }
                            );
                        }

                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.SPLASH.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
                        break;
                    case 1:
                        Vec3 dir = t.position().subtract(this.mob.position()).multiply(1.0, 0.0, 1.0).normalize();
                        Vfx.play(level, VfxKind.TIDAL_WAVE, this.mob.position().add(dir.scale(2.0)), dir, 10.0F, 2788048, -1, 0);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.WAVE.get(), SoundSource.HOSTILE, 3.0F, 0.7F);

                        for (LivingEntity e : Combat.around(this.mob, this.mob.position().add(dir.scale(5.0)), 6.0)) {
                            if (!(e instanceof CurseEntity c) || c.isSummon()) {
                                Combat.hurt(e, this.mob, this.mob, 13.0F);
                                Combat.knock(e, dir, 1.8, 0.6);
                            }
                        }
                        break;
                    default:
                        Vfx.play(level, VfxKind.WHIRLPOOL, this.mob.position(), new Vec3(0.0, 1.0, 0.0), 8.0F, 2788048, this.mob.getId(), 50);
                        level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.WAVE.get(), SoundSource.HOSTILE, 2.5F, 0.5F);

                        for (int i = 0; i < 10; i++) {
                            Scheduler.later(i * 5, () -> {
                                if (this.mob.isAlive()) {
                                    for (LivingEntity ex : Combat.around(this.mob, this.mob.position(), 9.0)) {
                                        if (ex instanceof CurseEntity cx && !cx.isSummon()) {
                                            continue;
                                        }

                                        Vec3 pull = this.mob.position().subtract(ex.position());
                                        ex.setDeltaMovement(ex.getDeltaMovement().scale(0.5).add(pull.normalize().scale(0.35)));
                                        ex.hurtMarked = true;
                                        if (pull.length() < 3.0) {
                                            Combat.hurt(ex, this.mob, this.mob, 3.0F);
                                        }

                                        ex.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2), this.mob);
                                    }
                                }
                            });
                        }
                }
            }
        }
    }
}
