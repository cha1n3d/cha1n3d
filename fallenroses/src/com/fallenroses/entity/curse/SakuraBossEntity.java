package com.fallenroses.entity.curse;

import com.fallenroses.entity.CurseOrbEntity;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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

public class SakuraBossEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.sakura_boss"), BossBarColor.PINK, BossBarOverlay.NOTCHED_10
    );
    private boolean enraged;

    public SakuraBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
        this.setPersistenceRequired();
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 500.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SakuraBossEntity.BossSkills(this));
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
            if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.5F) {
                this.enraged = true;
                this.bossBar.setColor(BossBarColor.RED);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
                Vfx.play((ServerLevel)this.level(), VfxKind.BERSERK, this.getBoundingBox().getCenter(), Vec3.ZERO, 3.0F, 14692412, this.getId(), 0);
                Combat.shake((ServerLevel)this.level(), this.position(), 1.5F, 30, 32.0);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
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
        return 3000;
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
        return ModSounds.BOSS_ROAR.get();
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

    static class BossSkills extends Goal {
        private final SakuraBossEntity boss;
        private int cooldown = 60;
        private int step;

        BossSkills(SakuraBossEntity boss) {
            this.boss = boss;
            this.setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return this.boss.getTarget() != null && this.boss.getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.boss.getTarget();
            if (target != null && --this.cooldown <= 0) {
                ServerLevel level = (ServerLevel)this.boss.level();
                int skill = this.step++ % (this.boss.enraged ? 3 : 2);
                this.cooldown = this.boss.enraged ? 70 : 110;
                this.boss.triggerAnim("action", "special");
                switch (skill) {
                    case 0:
                        this.roots(level, target);
                        break;
                    case 1:
                        this.petals(level, target);
                        break;
                    default:
                        this.summon(level);
                }
            }
        }

        private void roots(ServerLevel level, LivingEntity target) {
            int count = this.boss.enraged ? 5 : 3;

            for (int i = 0; i < count; i++) {
                Vec3 at = i == 0 ? target.position() : target.position().add(this.boss.random.nextGaussian() * 3.0, 0.0, this.boss.random.nextGaussian() * 3.0);
                Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 0.4F, 3807762, -1, 0);
                Scheduler.later(
                    22,
                    () -> {
                        if (this.boss.isAlive()) {
                            Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 2.0F, 3807762, -1, 1);
                            level.playSound(
                                null, at.x, at.y, at.z, ModSounds.BOSS_ROOTS.get(), SoundSource.HOSTILE, 2.0F, 0.9F + this.boss.random.nextFloat() * 0.2F
                            );

                            for (LivingEntity e : Combat.around(this.boss, at.add(0.0, 1.0, 0.0), 2.2)) {
                                Combat.hurt(e, this.boss, this.boss, 12.0F);
                                e.push(0.0, 1.0, 0.0);
                                e.hurtMarked = true;
                            }
                        }
                    }
                );
            }
        }

        private void petals(ServerLevel level, LivingEntity target) {
            Vec3 from = this.boss.getEyePosition();
            Vec3 dir = target.getBoundingBox().getCenter().subtract(from).normalize();
            int n = this.boss.enraged ? 9 : 6;

            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(((double)i - (double)(n - 1) / 2.0) * 9.0);
                Vec3 d = new Vec3(dir.x * Math.cos(a) - dir.z * Math.sin(a), dir.y, dir.x * Math.sin(a) + dir.z * Math.cos(a));
                new CurseOrbEntity(ModEntities.CURSE_ORB.get(), level).launch(this.boss, from.add(d.scale(1.5)), d.scale(0.7), 1.6F);
            }

            level.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(), ModSounds.HANGED_SHOOT.get(), SoundSource.HOSTILE, 2.0F, 0.7F);
        }

        private void summon(ServerLevel level) {
            for (int i = 0; i < 3; i++) {
                FlyHeadEntity fly = ModEntities.FLY_HEAD.get().create(level);
                if (fly != null) {
                    fly.moveTo(
                        this.boss.getX() + this.boss.random.nextGaussian() * 2.0,
                        this.boss.getY() + 2.5,
                        this.boss.getZ() + this.boss.random.nextGaussian() * 2.0,
                        0.0F,
                        0.0F
                    );
                    fly.finalizeSpawn(level, level.getCurrentDifficultyAt(fly.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                    fly.setTarget(this.boss.getTarget());
                    level.addFreshEntity(fly);
                    Vfx.play(level, VfxKind.EXORCISM, fly.position(), Vec3.ZERO, 0.8F, 6957744, -1, 4);
                }
            }

            level.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2.5F, 1.2F);
        }
    }
}
