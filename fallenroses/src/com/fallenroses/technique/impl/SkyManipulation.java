package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class SkyManipulation {
    static final int SKY = 9095423;
    private static final String SHIELD = "fr_sky_shield_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "sky_ripple" -> ripple(p, power);
            case "sky_shield" -> shield(p);
            case "sky_fold" -> fold(p, power);
            case "sky_slide" -> slide(p, power);
            case "thin_ice_breaker" -> thinIce(p, power);
            default -> false;
        };
    }

    private static boolean ripple(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.INFINITY_RIPPLE, p.getEyePosition().add(look.scale(2.0)), look, 2.5F, 9095423, -1, 0);
        Combat.sound(p, p.position(), ModSounds.INFINITY_ON, 1.4F, 0.7F);

        for (LivingEntity e : Combat.cone(p, 9.0, 0.6)) {
            Combat.hurt(e, p, p, 5.0F * power);
            Combat.knock(e, look, 1.8, 0.45);
        }

        return true;
    }

    private static boolean shield(ServerPlayer p) {
        PlayerAnims.play(p, "raise");
        p.getPersistentData().putLong("fr_sky_shield_until", p.level().getGameTime() + 80L);
        Vfx.play(p.serverLevel(), VfxKind.STATIC_FIELD, p.position(), Vec3.ZERO, 3.5F, 9095423, p.getId(), 80);
        Combat.sound(p, p.position(), ModSounds.INFINITY_ON, 1.4F, 1.3F);
        p.displayClientMessage(Component.translatable("message.fallenroses.sky_shield").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    public static void tick(ServerPlayer p, PlayerData d) {
        if (p.getPersistentData().getLong("fr_sky_shield_until") > p.level().getGameTime()) {
            for (Projectile pr : p.level().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.5))) {
                Entity o = pr.getOwner();
                if (o != p) {
                    Vec3 v = pr.getDeltaMovement();
                    if (!(v.dot(p.position().subtract(pr.position())) <= 0.0)) {
                        pr.setDeltaMovement(v.scale(-1.1));
                        pr.setOwner(p);
                        pr.hurtMarked = true;
                        Vfx.play(p.serverLevel(), VfxKind.INFINITY_RIPPLE, pr.position(), v.reverse(), 0.8F, 9095423, -1, 0);
                        Combat.sound(p, pr.position(), ModSounds.INFINITY_BLOCK, 0.8F, 1.5F);
                    }
                }
            }
        }
    }

    private static boolean fold(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 18.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "snap");
            ServerLevel level = p.serverLevel();
            target.setDeltaMovement(0.0, 1.5, 0.0);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 20, 0), p);
            Vfx.play(level, VfxKind.INFINITY_RIPPLE, target.position(), new Vec3(0.0, 1.0, 0.0), 2.0F, 9095423, -1, 0);
            Combat.sound(p, target.position(), ModSounds.DASH, 1.2F, 0.6F);
            Scheduler.later(14, () -> {
                if (target.isAlive()) {
                    target.setDeltaMovement(0.0, -2.6, 0.0);
                    target.hurtMarked = true;
                    Scheduler.later(6, () -> {
                        if (target.isAlive()) {
                            Vfx.play(level, VfxKind.SLAM, target.position(), Vec3.ZERO, 3.0F);
                            Combat.sound(p, target.position(), ModSounds.SLAM, 1.6F, 0.8F);
                            Combat.hurt(target, p, p, 12.0F * power);
                            target.fallDistance = 0.0F;
                        }
                    });
                }
            });
            return true;
        }
    }

    private static boolean slide(ServerPlayer p, float power) {
        PlayerAnims.play(p, "dash");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, Math.max(-0.1, Math.min(0.5, look.y)), look.z).normalize();
        Vec3 start = p.position();
        Vec3 end = Projection.safeEnd(p, dir, 12.0);
        if (end.distanceTo(start) < 1.5) {
            return false;
        } else {
            Set<Integer> hit = new HashSet<>();

            for (LivingEntity e : BloodManipulation.along(p, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), 1.5)) {
                if (hit.add(e.getId())) {
                    Combat.hurt(e, p, p, 6.0F * power);
                    Combat.knock(e, dir, 1.0, 0.3);
                }
            }

            Vfx.play(level, VfxKind.DASH, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.0F);
            Vfx.play(level, VfxKind.FRAME_TRAIL, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.0F, 9095423, p.getId(), 0);
            p.teleportTo(end.x, end.y, end.z);
            p.fallDistance = 0.0F;
            Combat.sound(p, end, ModSounds.DASH, 1.2F, 1.3F);
            return true;
        }
    }

    private static boolean thinIce(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        p.displayClientMessage(Component.translatable("message.fallenroses.thin_ice_breaker").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        Combat.sound(p, c, ModSounds.ICE_FORM, 3.0F, 1.4F);
        Scheduler.later(
            14,
            () -> {
                if (p.isAlive()) {
                    Vfx.play(level, VfxKind.GLACIER, c, Vec3.ZERO, 12.0F, 9095423, -1, 0);
                    Combat.sound(p, c, ModSounds.ICE_SHATTER, 4.0F, 0.8F);
                    Combat.flash(level, c, 15267071, 12, 0.8F, 40.0);
                    Combat.shake(level, c, 2.0F, 22, 40.0);

                    for (LivingEntity e : Combat.around(p, c, 12.0)) {
                        Combat.hurt(e, p, p, 26.0F * power);
                        Combat.knock(e, e.position().subtract(c), 1.2, 0.6);
                    }

                    for (int i = 0; i < 16; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0;
                        double r = 2.0 + level.random.nextDouble() * 10.0;
                        Vec3 at = c.add(Math.cos(a) * r, 1.0 + level.random.nextDouble() * 4.0, Math.sin(a) * r);
                        Scheduler.later(
                            level.random.nextInt(8),
                            () -> Vfx.play(
                                    level,
                                    VfxKind.SLASH_HIT,
                                    at,
                                    new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()),
                                    2.0F,
                                    15267071,
                                    -1,
                                    level.random.nextInt(180)
                                )
                        );
                    }
                }
            }
        );
        return true;
    }

    private SkyManipulation() {
    }
}
