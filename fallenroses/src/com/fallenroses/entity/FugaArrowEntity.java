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

public class FugaArrowEntity extends TechniqueProjectile {
    private static final double RADIUS = 6.5;

    public FugaArrowEntity(EntityType<? extends FugaArrowEntity> type, Level level) {
        super(type, level);
        this.maxLife = 40;
        this.hitRadius = 0.5;
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
    }

    @Override
    protected void impact(Vec3 c) {
        ServerLevel level = (ServerLevel)this.level();
        Entity owner = this.getOwner();
        Vfx.play(level, VfxKind.FUGA_IGNITE, c, Vec3.ZERO, 6.5F);
        Combat.sound(this, c, ModSounds.FUGA_IMPACT, 4.0F, 1.0F);
        Combat.shake(level, c, 1.8F, 24, 32.0);
        Combat.flash(level, c, 16742944, 10, 0.55F, 40.0);

        for (LivingEntity e : Combat.around((Entity)(owner == null ? this : owner), c, 6.5)) {
            double dist = e.getBoundingBox().getCenter().distanceTo(c);
            float falloff = (float)Math.max(0.4, 1.0 - dist / 7.5);
            Combat.hurt(e, this, owner, 26.0F * this.power() * falloff);
            e.setSecondsOnFire(10);
            Combat.knock(e, e.position().subtract(c), 1.2 * (double)falloff, 0.5);
        }
    }

    @Override
    protected void clientTick() {
        Vec3 m = this.getDeltaMovement();

        for (int i = 0; i < 4; i++) {
            double f = (double)i / 4.0;
            this.level()
                .addParticle(
                    ModParticles.glow(i % 2 == 0 ? 16747040 : 16769152, 1.4F, 12),
                    this.getX() - m.x * f,
                    this.getY() - m.y * f,
                    this.getZ() - m.z * f,
                    0.0,
                    0.0,
                    0.0
                );
            this.level()
                .addParticle(
                    ModParticles.ember(16736272, 0.35F, 24),
                    this.getX() - m.x * f,
                    this.getY() - m.y * f,
                    this.getZ() - m.z * f,
                    this.random.nextGaussian() * 0.06,
                    this.random.nextGaussian() * 0.06 + 0.03,
                    this.random.nextGaussian() * 0.06
                );
        }
    }
}
