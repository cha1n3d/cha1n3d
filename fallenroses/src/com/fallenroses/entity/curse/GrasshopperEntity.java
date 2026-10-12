package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GrasshopperEntity extends CurseEntity {
    private boolean leaping;

    public GrasshopperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 18.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, 3.5)
            .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new GrasshopperEntity.LeapGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.leaping && this.onGround() && this.tickCount % 1 == 0 && this.getDeltaMovement().y <= 0.0) {
            this.leaping = false;
            ServerLevel level = (ServerLevel)this.level();
            Vfx.play(level, VfxKind.SLAM, this.position(), Vec3.ZERO, 1.6F);

            for (LivingEntity e : Combat.around(this, this.position().add(0.0, 0.4, 0.0), 1.8)) {
                if (!(e instanceof CurseEntity c) || c.isSummon() || this.isSummon()) {
                    Combat.hurt(e, this, this, (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.4F);
                    Combat.knock(e, e.position().subtract(this.position()), 0.6, 0.3);
                }
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
        return 30;
    }

    @Override
    public float energyReward() {
        return 1.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GRASSHOPPER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SPIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SPIDER_DEATH.get();
    }

    static class LeapGoal extends Goal {
        private final GrasshopperEntity mob;
        private int cooldown = 30;

        LeapGoal(GrasshopperEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.mob.getTarget();
            if (t != null && this.mob.onGround() && --this.cooldown <= 0) {
                double d = (double)this.mob.distanceTo(t);
                return d > 4.0 && d < 12.0;
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            LivingEntity t = this.mob.getTarget();
            if (t != null) {
                this.cooldown = 60;
                Vec3 to = t.position().subtract(this.mob.position());
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                double d = flat.length();
                this.mob.setDeltaMovement(flat.normalize().scale(Math.min(1.6, d * 0.16)).add(0.0, 0.55 + d * 0.02, 0.0));
                this.mob.leaping = true;
                this.mob.triggerAnim("action", "special");
                this.mob
                    .level()
                    .playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.GRASSHOPPER.get(), SoundSource.HOSTILE, 1.2F, 1.3F);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
