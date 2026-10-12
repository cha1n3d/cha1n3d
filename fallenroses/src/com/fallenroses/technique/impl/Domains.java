package com.fallenroses.technique.impl;

import com.fallenroses.entity.DomainEntity;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.player.FRAdvancements;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public final class Domains {
    private static final String[] KEYS = new String[]{
        "unlimited_void",
        "malevolent_shrine",
        "coffin_of_iron_mountain",
        "self_embodiment",
        "horizon_of_skandha",
        "cemetery",
        "idle_death_gamble",
        "deadly_sentencing",
        "time_cell_moon_palace",
        "vessel_domain"
    };
    private static final int[] COLORS = new int[]{10275839, 14692412, 16738832, 8044728, 5945584, 10133600, 7004282, 15259808, 8050920, 14711360};
    private static final float[] RADIUS = new float[]{14.0F, 18.0F, 16.0F, 14.0F, 16.0F, 16.0F, 15.0F, 14.0F, 15.0F, 15.0F};
    private static final int[] FLASH = new int[]{16777215, 4194312, 4198400, 663584, 402490, 1710608, 670224, 2760720, 662064, 3807760};

    public static boolean expand(ServerPlayer p, PlayerData d, int type, float power) {
        ServerLevel level = p.serverLevel();
        String key = "domain.fallenroses." + KEYS[type];
        int color = COLORS[type];
        float radius = RADIUS[type];
        PlayerAnims.play(p, "domain");
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 4, false, false));
        // the barrier sound's sealing boom lands as the dome closes (20 ticks after it appears)
        Combat.sound(p, p.position(), ModSounds.DOMAIN_SIGN, 4.0F, 1.0F);
        Combat.sound(p, p.position(), ModSounds.DOMAIN_BARRIER, 6.0F, 1.0F);
        Vfx.play(level, VfxKind.DOMAIN_SIGN, p.position(), Vec3.ZERO, radius, DomainEntity.EDGE[type], p.getId(), type);

        for (ServerPlayer other : level.players()) {
            if (other.distanceTo(p) < 48.0F) {
                ModNetwork.send(other, new SimplePackets.ScreenFx(2, color, 70, 1.0F, key));
            }
        }

        Scheduler.later(20, () -> {
            if (p.isAlive() && p.level() == level) {
                DomainEntity domain = new DomainEntity(ModEntities.DOMAIN.get(), level);
                domain.setup(p, type, radius, type == 7 ? 110 : 220, power);
                domain.setPos(p.getX(), p.getY(), p.getZ());
                domain.setYRot(p.getYRot());
                level.addFreshEntity(domain);
                Vfx.play(level, VfxKind.DOMAIN_OPEN, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, radius, color, p.getId(), type);
                Combat.flash(level, p.position(), FLASH[type], 16, 0.9F, 40.0);
                Combat.shake(level, p.position(), 2.0F, 24, 40.0);
            }
        });
        FRAdvancements.award(p, "story/domain_expansion");
        return true;
    }

    public static boolean isInsideOwnDomain(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(DomainEntity.class, p.getBoundingBox().inflate(40.0), e -> p.getUUID().equals(e.ownerUuid())).isEmpty();
    }

    private Domains() {
    }
}
