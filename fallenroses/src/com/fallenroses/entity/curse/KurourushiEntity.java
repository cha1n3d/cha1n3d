package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerLevel;
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

public class KurourushiEntity extends CurseEntity {
    private int swarmCooldown = 80;
    private int lungeCooldown = 40;

    public KurourushiEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 110.0)
            .add(Attributes.MOVEMENT_SPEED, 0.33)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
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
            if (t != null && t.isAlive()) {
                ServerLevel level = (ServerLevel)this.level();
                double d = (double)this.distanceTo(t);
                if (--this.swarmCooldown <= 0 && d < 12.0) {
                    this.swarmCooldown = 160;
                    this.triggerAnim("action", "special");
                    Vec3 at = t.position();
                    Vfx.play(level, VfxKind.DUST_CLOUD, at.add(0.0, 0.6, 0.0), Vec3.ZERO, 3.5F, 2759178, -1, 0);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPIDER_AMBIENT.get(), SoundSource.HOSTILE, 2.0F, 1.6F);

                    for (int i = 0; i < 3; i++) {
                        Scheduler.later(i * 15, () -> {
                            if (this.isAlive()) {
                                Vfx.play(level, VfxKind.DUST_CLOUD, at.add(0.0, 0.6, 0.0), Vec3.ZERO, 3.0F, 1708038, -1, 0);

                                for (LivingEntity e : Combat.around(this, at.add(0.0, 1.0, 0.0), 3.2)) {
                                    if (!(e instanceof CurseEntity c) || c.isSummon()) {
                                        Combat.hurt(e, this, this, 3.0F);
                                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
                                        e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1), this);
                                    }
                                }
                            }
                        });
                    }
                } else if (--this.lungeCooldown <= 0 && d > 4.0 && d < 14.0 && this.onGround()) {
                    this.lungeCooldown = 90;
                    Vec3 to = t.position().subtract(this.position());
                    this.setDeltaMovement(to.normalize().scale(1.3).add(0.0, 0.35, 0.0));
                    this.hurtMarked = true;
                    this.triggerAnim("action", "attack");
                    Vfx.play(level, VfxKind.DASH, this.position().add(0.0, 0.6, 0.0), to, 1.0F);
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SPIDER_HURT.get(), SoundSource.HOSTILE, 1.4F, 1.4F);
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
        return 800;
    }

    @Override
    public float energyReward() {
        return 9.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SPIDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SPIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPIDER_DEATH.get();
    }
}
