package com.fallenroses.entity.curse;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.technique.impl.DeathPainting;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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

public class EsoEntity extends CurseEntity {
    private int spitCooldown = 40;
    private int wingCooldown = 100;

    public EsoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 4.0)
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
            if (t != null && t.isAlive()) {
                ServerLevel level = (ServerLevel)this.level();
                double d = (double)this.distanceTo(t);
                if (--this.wingCooldown <= 0 && d < 7.0) {
                    this.wingCooldown = 140;
                    this.triggerAnim("action", "special");
                    Vec3 look = t.position().subtract(this.position()).normalize();
                    Vfx.play(level, VfxKind.WING_FAN, this.position().add(0.0, 1.3, 0.0), look, 7.0F, 10123824, this.getId(), 0);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BLOOD_BEAM.get(), SoundSource.HOSTILE, 1.4F, 0.6F);

                    for (LivingEntity e : Combat.around(this, this.position().add(look.scale(3.0)).add(0.0, 1.0, 0.0), 4.0)) {
                        if (!(e instanceof CurseEntity c) || c.isSummon()) {
                            Combat.hurt(e, this, this, 9.0F);
                            DeathPainting.rot(e, this, 1, 140);
                        }
                    }
                } else if (--this.spitCooldown <= 0 && d > 3.0 && d < 16.0 && this.hasLineOfSight(t)) {
                    this.spitCooldown = 60;
                    this.triggerAnim("action", "attack");
                    Vec3 mouth = this.getEyePosition();
                    Vec3 aim = t.getEyePosition().subtract(mouth).normalize();

                    for (int i = -1; i <= 1; i++) {
                        double a = Math.toRadians((double)(i * 7));
                        Vec3 v = new Vec3(aim.x * Math.cos(a) - aim.z * Math.sin(a), aim.y + 0.05, aim.x * Math.sin(a) + aim.z * Math.cos(a))
                            .normalize()
                            .scale(1.1);
                        new ShotEntity(ModEntities.SHOT.get(), level).kind(ShotEntity.Kind.ROT_BLOOD).launch(this, mouth, v, 1.0F);
                    }

                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.BLOOD_WHOOSH.get(), SoundSource.HOSTILE, 1.2F, 0.7F);
                }
            }
        }
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
        return 10.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WOMB_WAIL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HUMAN_GROAN.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WOMB_WAIL.get();
    }
}
