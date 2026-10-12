package com.fallenroses.entity.curse;

import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class RainbowDragonEntity extends CurseEntity {
    private final ServerBossEvent bossBar = new ServerBossEvent(
        Component.translatable("entity.fallenroses.rainbow_dragon"), BossBarColor.BLUE, BossBarOverlay.NOTCHED_6
    );

    public RainbowDragonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 40;
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 180.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.FOLLOW_RANGE, 48.0);
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
        this.goalSelector.addGoal(2, new RainbowDragonEntity.SkyGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.9));
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
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected int deathTicks() {
        return 45;
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
        return 10.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DRAGON_ROAR.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BOSS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BOSS_DEATH.get();
    }

    static class SkyGoal extends Goal {
        private final RainbowDragonEntity mob;
        private int cooldown = 50;
        private int step;
        private int diving = -1;
        private Vec3 diveTo = Vec3.ZERO;
        private float angle;

        SkyGoal(RainbowDragonEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
            if (t != null) {
                ServerLevel level = (ServerLevel)this.mob.level();
                this.mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
                if (this.diving >= 0) {
                    this.mob.getMoveControl().setWantedPosition(this.diveTo.x, this.diveTo.y, this.diveTo.z, 2.4);

                    for (LivingEntity e : Combat.around(this.mob, this.mob.position().add(0.0, 0.8, 0.0), 2.2)) {
                        if ((!(e instanceof CurseEntity c) || c.isSummon() || this.mob.isSummon()) && e.invulnerableTime <= 0) {
                            Combat.hurt(e, this.mob, this.mob, 12.0F);
                            Combat.knock(e, this.mob.getDeltaMovement(), 1.4, 0.6);
                        }
                    }

                    if (--this.diving <= 0 || this.mob.position().distanceTo(this.diveTo) < 1.5) {
                        this.diving = -1;
                    }
                } else {
                    this.angle += 6.0F;
                    double a = Math.toRadians((double)this.angle);
                    this.mob.getMoveControl().setWantedPosition(t.getX() + Math.cos(a) * 9.0, t.getY() + 7.0, t.getZ() + Math.sin(a) * 9.0, 1.1);
                    if (--this.cooldown <= 0) {
                        this.cooldown = 70;
                        this.mob.triggerAnim("action", "special");
                        if (this.step++ % 2 == 0) {
                            Vec3 from = this.mob.getEyePosition();
                            Vec3 to = t.getBoundingBox().getCenter();
                            BlockHitResult hit = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this.mob));
                            if (hit.getType() != Type.MISS) {
                                to = hit.getLocation();
                            }

                            Vfx.play(level, VfxKind.RAINBOW_BREATH, from, to.subtract(from), 1.0F, 16777215, -1, 0);
                            level.playSound(
                                null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.DRAGON_ROAR.get(), SoundSource.HOSTILE, 2.5F, 1.2F
                            );
                            Vec3 d = to.subtract(from);
                            double len = d.length();
                            Vec3 n = d.normalize();

                            for (LivingEntity ex : level.getEntitiesOfClass(
                                LivingEntity.class, this.mob.getBoundingBox().expandTowards(d).inflate(1.5), exx -> Combat.valid(exx, this.mob)
                            )) {
                                Vec3 rel = ex.getBoundingBox().getCenter().subtract(from);
                                double along = rel.dot(n);
                                if (!(along < 0.0) && !(along > len + 1.0) && !(rel.subtract(n.scale(along)).length() > 1.4)) {
                                    if (ex instanceof CurseEntity) {
                                        CurseEntity c = (CurseEntity)ex;
                                        if (!c.isSummon() && !this.mob.isSummon()) {
                                            continue;
                                        }
                                    }

                                    Combat.hurt(ex, this.mob, this.mob, 9.0F);
                                    ex.setSecondsOnFire(3);
                                }
                            }
                        } else {
                            this.diveTo = t.position()
                                .add(t.position().subtract(this.mob.position()).multiply(1.0, 0.0, 1.0).normalize().scale(4.0))
                                .add(0.0, 1.0, 0.0);
                            this.diving = 24;
                            Vfx.play(level, VfxKind.DASH, this.mob.position().add(0.0, 1.0, 0.0), this.diveTo.subtract(this.mob.position()), 2.0F);
                            level.playSound(
                                null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), ModSounds.DRAGON_ROAR.get(), SoundSource.HOSTILE, 3.0F, 0.8F
                            );
                        }
                    }
                }
            }
        }
    }
}
