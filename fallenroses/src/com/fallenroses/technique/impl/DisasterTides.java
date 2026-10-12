package com.fallenroses.technique.impl;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DisasterTides {
    static final int SEA = 2788048;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "fish_swarm" -> fish(p, power);
            case "tidal_wave" -> wave(p, power);
            case "whirlpool" -> whirlpool(p, power);
            case "death_swarm" -> deathSwarm(p, power);
            case "horizon_of_skandha" -> Domains.expand(p, d, 4, power);
            default -> false;
        };
    }

    private static boolean fish(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        Vec3 look = p.getViewVector(1.0F);
        Vec3 c = p.getEyePosition();

        for (int i = 0; i < 6; i++) {
            double a = (double)i * Math.PI / 3.0;
            Vec3 at = c.add(Math.cos(a) * 1.2, Math.sin(a) * 0.6 + 0.3, Math.sin(a) * 1.2);
            Vec3 v = look.scale(0.5).add(Math.cos(a) * 0.15, 0.1, Math.sin(a) * 0.15);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.FISH).launch(p, at, v, power);
            Vfx.play(p.serverLevel(), VfxKind.SPLASH, at, Vec3.ZERO, 0.4F, 2788048, -1, 0);
        }

        Combat.sound(p, p.position(), ModSounds.SPLASH, 1.4F, 1.3F);
        return true;
    }

    private static boolean wave(ServerPlayer p, float power) {
        PlayerAnims.play(p, "sweep");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, 0.0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
        Vec3 start = p.position();
        Vfx.play(level, VfxKind.TIDAL_WAVE, start, dir, 14.0F, 2788048, -1, 0);
        Combat.sound(p, start, ModSounds.WAVE, 2.5F, 0.9F);
        Set<Integer> hit = new HashSet<>();
        Vec3 d = dir;

        for (int i = 1; i <= 14; i++) {
            Vec3 front = start.add(d.scale((double)i));
            Scheduler.later(i, () -> {
                for (LivingEntity e : Combat.around(p, front.add(0.0, 1.0, 0.0), 2.6)) {
                    if (hit.add(e.getId())) {
                        Combat.hurt(e, p, p, 7.0F * power);
                        Combat.knock(e, d, 1.8, 0.5);
                        e.clearFire();
                    }
                }
            });
        }

        return true;
    }

    private static boolean whirlpool(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        LivingEntity target = Combat.lookTarget(p, 22.0);
        Vec3 at = target != null ? target.position() : BloodManipulation.groundBelow(p, Combat.lookPoint(p, 20.0));
        Vfx.play(level, VfxKind.WHIRLPOOL, at, Vec3.ZERO, 6.0F, 2788048, -1, 60);
        Combat.sound(p, at, ModSounds.WAVE, 2.0F, 0.6F);

        for (int i = 0; i < 12; i++) {
            int k = i;
            Scheduler.later(i * 5, () -> {
                for (LivingEntity e : Combat.around(p, at.add(0.0, 0.5, 0.0), 6.0)) {
                    Vec3 to = at.subtract(e.position());
                    Vec3 swirl = new Vec3(-to.z, 0.0, to.x).normalize().scale(0.25);
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(to.normalize().scale(0.35)).add(swirl));
                    e.hurtMarked = true;
                    if (k % 2 == 0) {
                        Combat.hurt(e, p, p, 1.8F * power);
                    }

                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2), p);
                }
            });
        }

        return true;
    }

    private static boolean deathSwarm(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        LivingEntity target = Combat.lookTarget(p, 26.0);
        Vec3 at = target != null ? target.position() : BloodManipulation.groundBelow(p, Combat.lookPoint(p, 24.0));
        Vfx.play(level, VfxKind.WHIRLPOOL, at.add(0.0, 9.0, 0.0), new Vec3(0.0, -1.0, 0.0), 4.0F, 2788048, -1, 40);
        Combat.sound(p, at, ModSounds.WAVE, 3.0F, 0.5F);

        for (int i = 0; i < 14; i++) {
            Scheduler.later(6 + i * 2, () -> {
                Vec3 from = at.add(level.random.nextGaussian() * 2.5, 9.0, level.random.nextGaussian() * 2.5);
                new ShotEntity(ModEntities.SHOT.get(), level).kind(ShotEntity.Kind.FISH).launch(p, from, new Vec3(0.0, -0.7, 0.0), power * 1.1F);
            });
        }

        return true;
    }

    private DisasterTides() {
    }
}
