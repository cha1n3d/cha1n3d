package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class YokaiCurses {
    private YokaiCurses() {
    }

    public static class KasaObake extends CurseEntity {
        private int lick = 40;

        public KasaObake(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 4;
        }

        public static Builder createAttributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
        }

        @Override
        protected void registerGoals() {
            this.standardGoals(1.2);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!this.level().isClientSide) {
                if (this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() > 0.001 && this.tickCount % 10 == 0) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.42, 0.0));
                }

                LivingEntity t = this.getTarget();
                if (t != null && t.isAlive() && --this.lick <= 0 && !((double)this.distanceTo(t) > 4.5)) {
                    this.lick = 50;
                    this.triggerAnim("action", "attack");
                    Vec3 mouth = this.getEyePosition();
                    Vfx.play((ServerLevel)this.level(), VfxKind.TOAD_TONGUE, mouth, t.getEyePosition().subtract(mouth), 1.0F, 15757456, t.getId(), 0);
                    Combat.hurt(t, this, this, 3.0F);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
                }
            }
        }

        @Override
        public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
            return false;
        }

        @Override
        public int grade() {
            return 4;
        }

        @Override
        public int xpReward() {
            return 40;
        }

        @Override
        public float energyReward() {
            return 1.0F;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return ModSounds.DOLL_LAUGH.get();
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.FLY_HURT.get();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return ModSounds.FLY_DEATH.get();
        }
    }

    public static class Nopperabo extends CurseEntity {
        private int scare = 40;

        public Nopperabo(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static Builder createAttributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
        }

        @Override
        protected void registerGoals() {
            this.standardGoals(1.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!this.level().isClientSide && --this.scare <= 0) {
                for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(10.0))) {
                    if (!p.isCreative() && !p.isSpectator()) {
                        Vec3 to = this.getEyePosition().subtract(p.getEyePosition()).normalize();
                        if (!(p.getViewVector(1.0F).dot(to) < 0.93) && p.hasLineOfSight(this)) {
                            this.scare = 160;
                            this.triggerAnim("action", "special");
                            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), this);
                            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this);
                            this.level()
                                .playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.HANGED_AMBIENT.get(), SoundSource.HOSTILE, 1.6F, 0.6F);
                            if (p instanceof ServerPlayer sp) {
                                sp.displayClientMessage(
                                    Component.translatable("message.fallenroses.nopperabo").withStyle(s -> s.withColor(13156536).withItalic(true)), true
                                );
                            }
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public int grade() {
            return 3;
        }

        @Override
        public int xpReward() {
            return 90;
        }

        @Override
        public float energyReward() {
            return 2.0F;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return ModSounds.HUMAN_GROAN.get();
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.HUMAN_GROAN.get();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return ModSounds.HANGED_DEATH.get();
        }
    }

    public static class Tengu extends CurseEntity {
        private int gust = 60;
        private int dive = 100;

        public Tengu(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.moveControl = new FlyingMoveControl(this, 12, true);
            this.xpReward = 30;
        }

        public static Builder createAttributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanFloat(true);
            return nav;
        }

        @Override
        protected void registerGoals() {
            this.goalSelector.addGoal(1, new FloatGoal(this));
            this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
            this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.7));
            this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!this.level().isClientSide) {
                LivingEntity t = this.getTarget();
                if (t != null && t.isAlive()) {
                    ServerLevel level = (ServerLevel)this.level();
                    if (this.distanceTo(t) > 4.0F) {
                        this.getMoveControl().setWantedPosition(t.getX(), t.getY() + 4.0, t.getZ(), 1.0);
                    }

                    if (--this.gust <= 0 && this.distanceTo(t) < 12.0F) {
                        this.gust = 90;
                        this.triggerAnim("action", "special");
                        Vec3 dir = t.position().subtract(this.position()).normalize();
                        Vfx.play(level, VfxKind.REPEL_SWEEP, this.position().add(0.0, 1.0, 0.0), dir, 6.0F, 15266047, -1, 0);
                        Vfx.play(level, VfxKind.DUST_CLOUD, t.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 2.5F, 14211280, -1, 0);
                        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.DASH.get(), SoundSource.HOSTILE, 2.0F, 0.5F);

                        for (LivingEntity e : Combat.around(this, t.position(), 4.0)) {
                            if (!(e instanceof CurseEntity c) || c.isSummon()) {
                                Combat.hurt(e, this, this, 8.0F);
                                Combat.knock(e, dir, 2.0, 0.8);
                            }
                        }
                    } else if (--this.dive <= 0) {
                        this.dive = 120;
                        this.setDeltaMovement(t.position().subtract(this.position()).normalize().scale(1.6));
                        this.hurtMarked = true;
                        this.triggerAnim("action", "attack");
                        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.NUE_SCREECH.get(), SoundSource.HOSTILE, 1.6F, 1.3F);
                    }
                }
            }
        }

        @Override
        public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
            return false;
        }

        @Override
        protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        }

        @Override
        public int grade() {
            return 1;
        }

        @Override
        public int xpReward() {
            return 850;
        }

        @Override
        public float energyReward() {
            return 9.0F;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return ModSounds.NUE_SCREECH.get();
        }

        @Override
        public int getAmbientSoundInterval() {
            return 260;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.MASK_HISS.get();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return ModSounds.BOSS_HURT.get();
        }
    }

    public static class YukiOnna extends CurseEntity {
        private int breath = 60;

        public YukiOnna(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 15;
        }

        public static Builder createAttributes() {
            return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 65.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
        }

        @Override
        protected void registerGoals() {
            this.standardGoals(1.05);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!this.level().isClientSide) {
                LivingEntity t = this.getTarget();
                if (t != null && t.isAlive() && --this.breath <= 0 && !(this.distanceTo(t) > 9.0F) && this.hasLineOfSight(t)) {
                    this.breath = 90;
                    this.triggerAnim("action", "special");
                    ServerLevel level = (ServerLevel)this.level();
                    Vec3 mouth = this.getEyePosition();
                    Vfx.play(level, VfxKind.FROST_CONE, mouth, t.getEyePosition().subtract(mouth).normalize(), 9.0F, 11067647, this.getId(), 0);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.ICE_FORM.get(), SoundSource.HOSTILE, 1.6F, 0.8F);
                    Combat.hurt(t, this, this, 6.0F);
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), t.getTicksRequiredToFreeze() + 120));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
                }
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            if (target instanceof LivingEntity l) {
                l.setTicksFrozen(Math.max(l.getTicksFrozen(), l.getTicksRequiredToFreeze() + 40));
            }

            return super.doHurtTarget(target);
        }

        @Override
        public boolean canFreeze() {
            return false;
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
        protected SoundEvent getHurtSound(DamageSource source) {
            return ModSounds.ICE_SHATTER.get();
        }

        @Override
        protected SoundEvent getDeathSound() {
            return ModSounds.ICE_SHATTER.get();
        }
    }
}
