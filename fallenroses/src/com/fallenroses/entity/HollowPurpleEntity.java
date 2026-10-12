package com.fallenroses.entity;

import com.fallenroses.config.FRConfig;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HollowPurpleEntity extends TechniqueProjectile {
    public static final float RADIUS = 2.6F;

    public HollowPurpleEntity(EntityType<? extends HollowPurpleEntity> type, Level level) {
        super(type, level);
        this.maxLife = 70;
        this.pierce = true;
        this.passBlocks = true;
        this.hitRadius = 2.6F;
    }

    @Override
    protected void onHitLiving(LivingEntity target) {
        Entity owner = this.getOwner();
        Combat.hurt(target, this, owner, 40.0F * this.power());
        Combat.knock(target, this.getDeltaMovement(), 1.5, 0.3);
    }

    @Override
    protected void serverTick() {
        ServerLevel level = (ServerLevel)this.level();
        if (this.tickCount % 4 == 0) {
            Combat.shake(level, this.position(), 0.7F, 6, 20.0);
        }

        if (FRConfig.HOLLOW_PURPLE_BREAKS_BLOCKS.get() && this.tickCount > 2) {
            BlockPos c = this.blockPosition();
            int r = (int)Math.ceil(2.6F);

            for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
                if (!(p.distSqr(c) > 6.7599993F)) {
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getDestroySpeed(level, p) >= 0.0F && s.getDestroySpeed(level, p) < 50.0F) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
    }

    @Override
    protected void impact(Vec3 at) {
        Vfx.play((ServerLevel)this.level(), VfxKind.PURPLE_IMPACT, at, this.getDeltaMovement(), 5.2F);
        Combat.sound(this, at, ModSounds.RED_IMPACT, 4.0F, 0.5F);
    }

    @Override
    protected void clientTick() {
        for (int i = 0; i < 6; i++) {
            Vec3 d = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).normalize().scale(2.7299998998641968);
            this.level()
                .addParticle(
                    ModParticles.spark(i % 2 == 0 ? 14199039 : 16777215, 0.7F, 8),
                    this.getX() + d.x,
                    this.getY() + d.y,
                    this.getZ() + d.z,
                    d.x * 0.15,
                    d.y * 0.15,
                    d.z * 0.15
                );
        }

        Vec3 m = this.getDeltaMovement();
        this.level()
            .addParticle(ModParticles.glow(9059583, 4.5F, 14), this.getX() - m.x * 2.0, this.getY() - m.y * 2.0, this.getZ() - m.z * 2.0, 0.0, 0.0, 0.0);
    }
}
