package com.fallenroses.entity;

import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RedBlastEntity extends TechniqueProjectile {
    private static final double RADIUS = 5.5;

    public RedBlastEntity(EntityType<? extends RedBlastEntity> type, Level level) {
        super(type, level);
        this.maxLife = 26;
        this.hitRadius = 0.4;
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
    }

    @Override
    protected void impact(Vec3 c) {
        ServerLevel level = (ServerLevel)this.level();
        Entity owner = this.getOwner();
        Vfx.play(level, VfxKind.RED_IMPACT, c, this.getDeltaMovement().normalize(), 5.5F);
        Combat.sound(this, c, ModSounds.RED_IMPACT, 3.0F, 1.0F);
        Combat.shake(level, c, 1.4F, 14, 24.0);

        for (LivingEntity e : Combat.around((Entity)(owner == null ? this : owner), c, 5.5)) {
            Vec3 away = e.getBoundingBox().getCenter().subtract(c);
            double dist = away.length();
            float falloff = (float)Math.max(0.3, 1.0 - dist / 6.5);
            Combat.hurt(e, this, owner, 12.0F * this.power() * falloff);
            Vec3 dir = dist < 0.001 ? this.getDeltaMovement() : away;
            Combat.knock(e, dir, 2.6 * (double)falloff, 0.55 + 0.4 * (double)falloff);
        }
    }

    @Override
    protected void clientTick() {
        Vec3 m = this.getDeltaMovement();

        for (int i = 0; i < 3; i++) {
            double f = (double)i / 3.0;
            this.level().addParticle(ModParticles.glow(16722462, 1.6F, 10), this.getX() - m.x * f, this.getY() - m.y * f, this.getZ() - m.z * f, 0.0, 0.0, 0.0);
        }

        this.level()
            .addParticle(
                ModParticles.spark(16756896, 0.5F, 6),
                this.getX(),
                this.getY(),
                this.getZ(),
                -m.x * 0.3 + this.random.nextGaussian() * 0.08,
                -m.y * 0.3 + this.random.nextGaussian() * 0.08,
                -m.z * 0.3 + this.random.nextGaussian() * 0.08
            );
    }
}
