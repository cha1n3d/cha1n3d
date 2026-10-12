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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class StarRage {
    static final int STAR = 16762960;
    private static final String NO_FALL = "fr_star_nofall";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "mass_punch" -> punch(p, power);
            case "garuda" -> garuda(p, power);
            case "gravity_well" -> well(p, power);
            case "star_fall" -> starFall(p, power);
            case "bonbaye" -> blackHole(p, power);
            default -> false;
        };
    }

    public static boolean ignoresFall(ServerPlayer p) {
        return p.getPersistentData().getLong("fr_star_nofall") > p.level().getGameTime();
    }

    private static boolean punch(ServerPlayer p, float power) {
        PlayerAnims.play(p, "punch");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 at = p.getEyePosition().add(look.scale(2.2));
        Vfx.play(level, VfxKind.MASS_IMPACT, at, look, 1.6F, 16762960, -1, 0);
        Combat.sound(p, at, ModSounds.SLAM, 1.6F, 0.6F);
        Combat.shake(level, at, 0.7F, 8, 14.0);

        for (LivingEntity e : Combat.cone(p, 5.0, 0.6)) {
            Combat.hurt(e, p, p, 9.0F * power);
            Combat.knock(e, look, 2.6, 0.5);
        }

        return true;
    }

    private static boolean garuda(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 look = p.getViewVector(1.0F);
        new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.GARUDA).launch(p, Limitless.hand(p), look.scale(1.3), power);
        Vfx.play(p.serverLevel(), VfxKind.GARUDA, Limitless.hand(p), look, 1.0F, 16762960, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.NUE_SCREECH, 1.0F, 1.5F);
        return true;
    }

    private static boolean well(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        ServerLevel level = p.serverLevel();
        Vec3 c = BloodManipulation.groundBelow(p, Combat.lookPoint(p, 22.0)).add(0.0, 0.5, 0.0);
        Vfx.play(level, VfxKind.GRAVITY_WELL, c, Vec3.ZERO, 7.0F, 16762960, -1, 60);
        Combat.sound(p, c, ModSounds.PURPLE_CHARGE, 1.6F, 0.5F);

        for (int i = 1; i <= 30; i++) {
            Scheduler.later(i * 2, () -> {
                for (LivingEntity e : Combat.around(p, c, 7.0)) {
                    Vec3 to = c.subtract(e.position());
                    double d = Math.max(0.5, to.length());
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(to.normalize().scale(Math.min(0.5, 1.6 / d))).add(0.0, -0.08, 0.0));
                    e.hurtMarked = true;
                }
            });
        }

        Scheduler.later(62, () -> {
            Vfx.play(level, VfxKind.MASS_IMPACT, c, new Vec3(0.0, -1.0, 0.0), 4.0F, 16762960, -1, 1);
            Combat.sound(p, c, ModSounds.SLAM, 2.4F, 0.5F);
            Combat.shake(level, c, 1.2F, 14, 24.0);

            for (LivingEntity e : Combat.around(p, c, 7.0)) {
                Combat.hurt(e, p, p, 12.0F * power);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3), p);
            }
        });
        return true;
    }

    private static boolean starFall(ServerPlayer p, float power) {
        PlayerAnims.play(p, "slam");
        ServerLevel level = p.serverLevel();
        p.setDeltaMovement(p.getViewVector(1.0F).multiply(0.6, 0.0, 0.6).add(0.0, 1.5, 0.0));
        p.hurtMarked = true;
        p.getPersistentData().putLong("fr_star_nofall", level.getGameTime() + 100L);
        Combat.sound(p, p.position(), ModSounds.DASH, 1.2F, 0.6F);
        Vfx.play(level, VfxKind.GARUDA, p.position(), new Vec3(0.0, 1.0, 0.0), 1.4F, 16762960, p.getId(), 1);
        pollLanding(p, power, 0);
        return true;
    }

    private static void pollLanding(ServerPlayer p, float power, int t) {
        Scheduler.later(1, () -> {
            if (p.isAlive()) {
                if (t > 8 && p.getDeltaMovement().y < 0.0 && !p.onGround()) {
                    p.setDeltaMovement(p.getDeltaMovement().add(0.0, -0.25, 0.0));
                    p.hurtMarked = true;
                }

                if ((t <= 6 || !p.onGround()) && t <= 80) {
                    pollLanding(p, power, t + 1);
                } else {
                    land(p, power);
                }
            }
        });
    }

    private static void land(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        p.fallDistance = 0.0F;
        Vfx.play(level, VfxKind.MASS_IMPACT, c.add(0.0, 0.2, 0.0), new Vec3(0.0, -1.0, 0.0), 6.0F, 16762960, -1, 1);
        Vfx.play(level, VfxKind.SLAM, c, Vec3.ZERO, 6.0F);
        Combat.sound(p, c, ModSounds.SLAM, 3.0F, 0.45F);
        Combat.sound(p, c, ModSounds.FUGA_IMPACT, 1.5F, 0.6F);
        Combat.shake(level, c, 1.8F, 18, 30.0);

        for (LivingEntity e : Combat.around(p, c, 6.0)) {
            double d = e.position().distanceTo(c);
            float k = (float)Math.max(0.4, 1.0 - d / 7.0);
            Combat.hurt(e, p, p, 14.0F * power * k);
            Combat.knock(e, e.position().subtract(c), 1.6 * (double)k, 0.7);
        }
    }

    private static boolean blackHole(ServerPlayer p, float power) {
        PlayerAnims.play(p, "purple");
        ServerLevel level = p.serverLevel();
        Vec3 c = Combat.lookPoint(p, 26.0);
        p.displayClientMessage(Component.translatable("message.fallenroses.bonbaye").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 2.0F, 0.4F);
        Vfx.play(level, VfxKind.RED_CHARGE, Limitless.hand(p), Vec3.ZERO, 1.6F, 16762960, p.getId(), 20);
        Scheduler.later(20, () -> {
            Vfx.play(level, VfxKind.BLACK_HOLE, c, Vec3.ZERO, 14.0F, 16762960, -1, 0);
            Combat.sound(p, c, ModSounds.DOMAIN_VOID, 3.0F, 0.5F);
            Combat.shake(level, c, 1.0F, 120, 40.0);

            for (int i = 1; i <= 60; i++) {
                int tick = i;
                Scheduler.later(i * 2, () -> {
                    for (LivingEntity e : Combat.around(p, c, 14.0)) {
                        Vec3 to = c.subtract(e.getBoundingBox().getCenter());
                        double d = Math.max(0.6, to.length());
                        e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(to.normalize().scale(Math.min(0.9, 3.0 / d))));
                        e.hurtMarked = true;
                        e.fallDistance = 0.0F;
                        if (tick % 5 == 0 && d < 4.0) {
                            Combat.hurt(e, p, p, 2.5F * power);
                        }
                    }
                });
            }

            Scheduler.later(122, () -> {
                Vfx.play(level, VfxKind.BLACK_HOLE, c, Vec3.ZERO, 14.0F, 16762960, -1, 1);
                Combat.sound(p, c, ModSounds.FUGA_IMPACT, 4.0F, 0.4F);
                Combat.sound(p, c, ModSounds.SLAM, 4.0F, 0.4F);
                Combat.flash(level, c, 16773312, 14, 0.9F, 48.0);
                Combat.shake(level, c, 2.6F, 30, 48.0);

                for (LivingEntity e : Combat.around(p, c, 9.0)) {
                    double d = e.getBoundingBox().getCenter().distanceTo(c);
                    float k = (float)Math.max(0.4, 1.0 - d / 10.0);
                    Combat.hurt(e, p, p, 32.0F * power * k);
                    Combat.knock(e, e.position().subtract(c), 2.2 * (double)k, 0.9);
                }
            });
        });
        return true;
    }

    private StarRage() {
    }
}
