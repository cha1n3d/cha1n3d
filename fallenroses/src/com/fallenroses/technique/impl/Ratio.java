package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
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

public final class Ratio {
    static final int GOLD = 15253600;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "ratio_strike" -> ratioStrike(p, power);
            case "blunt_combo" -> bluntCombo(p, power);
            case "collapse" -> collapse(p, power);
            case "overtime" -> overtime(p, d);
            case "fatal_ratio" -> fatalRatio(p, power);
            default -> false;
        };
    }

    private static boolean ratioStrike(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 7.0);
        if (target == null) {
            return noTarget(p);
        } else {
            PlayerAnims.play(p, "punch");
            dashTo(p, target);
            ServerLevel level = p.serverLevel();
            Vec3 c = target.getBoundingBox().getCenter();
            Vfx.play(level, VfxKind.RATIO_MARK, c, target.position().subtract(p.position()), target.getBbHeight(), 15253600, target.getId(), 0);
            Scheduler.later(4, () -> {
                if (target.isAlive()) {
                    Combat.hurt(target, p, p, 7.0F * power * 2.0F);
                    Combat.knock(target, target.position().subtract(p.position()), 0.9, 0.3);
                    Vfx.play(level, VfxKind.RATIO_CRIT, target.getBoundingBox().getCenter(), target.position().subtract(p.position()), 1.2F, 15253600, -1, 0);
                    Combat.sound(p, target.position(), ModSounds.RATIO_CRIT, 1.6F, 1.0F);
                    Combat.shake(level, target.position(), 0.6F, 8, 10.0);
                }
            });
            return true;
        }
    }

    private static boolean bluntCombo(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 5.0);
        if (target == null) {
            return noTarget(p);
        } else {
            PlayerAnims.play(p, "flurry");
            dashTo(p, target);

            for (int i = 0; i < 4; i++) {
                int n = i;
                Scheduler.later(2 + i * 4, () -> {
                    if (target.isAlive() && p.isAlive()) {
                        boolean last = n == 3;
                        Combat.hurt(target, p, p, (last ? 7.0F : 4.0F) * power);
                        Vec3 dir = target.position().subtract(p.position());
                        if (last) {
                            Combat.knock(target, dir, 1.6, 0.45);
                        }

                        Vfx.play(p.serverLevel(), VfxKind.FLURRY_HIT, target.getBoundingBox().getCenter(), dir, last ? 2.0F : 1.2F, 15253600, -1, 0);
                        Combat.sound(p, target.position(), ModSounds.FLURRY, 1.2F, last ? 0.7F : 1.0F);
                    }
                });
            }

            return true;
        }
    }

    private static boolean collapse(ServerPlayer p, float power) {
        PlayerAnims.play(p, "cleave");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 origin = p.position();
        Scheduler.later(5, () -> {
            for (int i = 1; i <= 5; i++) {
                int step = i;
                Scheduler.later(i * 2, () -> {
                    Vec3 at = origin.add(flat.scale((double)step * 1.8));
                    Vfx.play(level, VfxKind.COLLAPSE, at, flat, 1.4F + (float)step * 0.25F, 15253600, -1, 0);

                    for (LivingEntity e : Combat.around(p, at.add(0.0, 0.8, 0.0), 1.6 + (double)step * 0.25)) {
                        Combat.hurt(e, p, p, 9.0F * power);
                        Combat.knock(e, flat, 0.4, 0.75);
                    }

                    if (step % 2 == 1) {
                        Combat.sound(p, at, ModSounds.SLAM, 1.4F, 1.1F);
                    }
                });
            }

            Combat.shake(level, origin, 1.0F, 14, 16.0);
        });
        return true;
    }

    private static boolean overtime(ServerPlayer p, PlayerData d) {
        PlayerAnims.play(p, "berserk");
        d.overtimeTicks = 400;
        d.markUrgent();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0, false, true));
        Vfx.play(p.serverLevel(), VfxKind.OVERTIME, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.0F, 15253600, p.getId(), 400);
        Combat.sound(p, p.position(), ModSounds.OVERTIME, 1.5F, 1.0F);
        p.displayClientMessage(Component.translatable("message.fallenroses.overtime").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private static boolean fatalRatio(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 10.0);
        if (target == null) {
            return noTarget(p);
        } else {
            ServerLevel level = p.serverLevel();
            PlayerAnims.play(p, "fuga");
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
            target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 24, 0), p);

            for (int i = 0; i < 6; i++) {
                Scheduler.later(
                    i * 3,
                    () -> {
                        if (target.isAlive()) {
                            Vfx.play(
                                level,
                                VfxKind.RATIO_MARK,
                                target.getBoundingBox().getCenter(),
                                new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.5, level.random.nextGaussian()),
                                target.getBbHeight() * 1.2F,
                                15253600,
                                target.getId(),
                                0
                            );
                        }
                    }
                );
            }

            Combat.sound(p, p.position(), ModSounds.RATIO_CRIT, 2.0F, 0.6F);
            Scheduler.later(22, () -> {
                if (target.isAlive() && p.isAlive()) {
                    dashTo(p, target);
                    Combat.hurt(target, p, p, 42.0F * power);
                    Combat.knock(target, target.position().subtract(p.position()), 2.2, 0.6);
                    Vec3 c = target.getBoundingBox().getCenter();
                    Vfx.play(level, VfxKind.RATIO_CRIT, c, target.position().subtract(p.position()), 3.0F, 15253600, -1, 1);
                    Vfx.play(level, VfxKind.BLACK_FLASH, c, target.position().subtract(p.position()), 1.5F);
                    Combat.sound(p, c, ModSounds.BLACK_FLASH, 3.0F, 0.8F);
                    Combat.flash(level, c, 16771232, 8, 0.5F, 24.0);
                    Combat.shake(level, c, 2.0F, 18, 24.0);
                }
            });
            return true;
        }
    }

    static void dashTo(ServerPlayer p, LivingEntity target) {
        Vec3 to = target.position().subtract(p.position());
        double dist = to.horizontalDistance();
        if (!(dist < 2.2)) {
            Vec3 dest = target.position().subtract(new Vec3(to.x, 0.0, to.z).normalize().scale(1.4));
            Vfx.play(p.serverLevel(), VfxKind.DASH, p.position().add(0.0, 1.0, 0.0), dest.subtract(p.position()), 1.0F);
            p.teleportTo(dest.x, Math.max(dest.y, p.getY()), dest.z);
            p.hurtMarked = true;
        }
    }

    static boolean noTarget(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    private Ratio() {
    }
}
