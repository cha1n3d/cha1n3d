package com.fallenroses.entity;

import com.fallenroses.registry.ModParticles;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CurseOrbEntity extends TechniqueProjectile {
    public CurseOrbEntity(EntityType<? extends CurseOrbEntity> type, Level level) {
        super(type, level);
        this.maxLife = 80;
        this.hitRadius = 0.35;
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
        Combat.hurt(target, this, this.getOwner(), 5.0F * this.power());
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
    }

    @Override
    protected void serverTick() {
        if (this.getOwner() instanceof Mob mob && mob.getTarget() != null && this.tickCount > 5) {
            Vec3 to = mob.getTarget().getBoundingBox().getCenter().subtract(this.position()).normalize();
            Vec3 m = this.getDeltaMovement();
            double speed = m.length();
            this.setDeltaMovement(m.scale(0.9).add(to.scale(speed * 0.1)).normalize().scale(speed));
        }
    }

    @Override
    protected void impact(Vec3 at) {
        Vfx.play((ServerLevel)this.level(), VfxKind.CURSE_ORB_HIT, at);
    }

    @Override
    protected void clientTick() {
        this.level().addParticle(ModParticles.glow(6953632, 1.3F, 10), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        this.level()
            .addParticle(
                ModParticles.smoke(1378328, 0.6F, 20),
                this.getX(),
                this.getY(),
                this.getZ(),
                this.random.nextGaussian() * 0.02,
                this.random.nextGaussian() * 0.02,
                this.random.nextGaussian() * 0.02
            );
    }
}
