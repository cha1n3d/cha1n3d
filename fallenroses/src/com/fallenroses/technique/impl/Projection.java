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
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class Projection {
    static final int CYAN = 8050920;
    public static final String FRAMED = "fr_framed_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        float accel = 1.0F + (float)d.projectionStacks * 0.12F;

        return switch (id) {
            case "frame_dash" -> dash(p, d, power * accel);
            case "freeze_frame" -> freeze(p, power * accel);
            case "frame_barrage" -> barrage(p, d, power * accel);
            case "acceleration" -> accelerate(p, d);
            case "time_cell_moon_palace" -> Domains.expand(p, d, 8, power);
            default -> false;
        };
    }

    public static boolean framed(LivingEntity e) {
        return e.getPersistentData().getLong("fr_framed_until") > e.level().getGameTime();
    }

    static void frame(ServerPlayer p, LivingEntity e, int ticks) {
        ServerLevel level = p.serverLevel();
        e.getPersistentData().putLong("fr_framed_until", level.getGameTime() + (long)ticks);
        e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), ticks, 0), p);
        Vfx.play(level, VfxKind.FRAME_PANEL, e.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, e.getBbHeight()), 8050920, e.getId(), 0);
        Combat.sound(p, e.position(), ModSounds.FRAME, 1.6F, 0.8F);
        Scheduler.later(ticks, () -> {
            if (e.isAlive()) {
                Vfx.play(level, VfxKind.FRAME_PANEL, e.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, e.getBbHeight()), 8050920, e.getId(), 1);
                Combat.sound(p, e.position(), ModSounds.ICE_SHATTER, 1.2F, 1.4F);
            }
        });
    }

    static Vec3 safeEnd(ServerPlayer p, Vec3 dir, double dist) {
        Vec3 d = dir.normalize();
        double best = dist;

        for (double h : new double[]{0.3, 1.5}) {
            Vec3 from = p.position().add(0.0, h, 0.0);
            BlockHitResult hit = p.level().clip(new ClipContext(from, from.add(d.scale(dist)), Block.COLLIDER, Fluid.NONE, p));
            if (hit.getType() != Type.MISS) {
                best = Math.min(best, hit.getLocation().distanceTo(from) - 0.6);
            }
        }

        return p.position().add(d.scale(Math.max(0.0, best)));
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, Math.max(-0.2, Math.min(0.35, look.y)), look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static boolean dash(ServerPlayer p, PlayerData d, float power) {
        PlayerAnims.play(p, "dash");
        ServerLevel level = p.serverLevel();
        boolean mach = d.projectionStacks >= 4;
        Vec3 start = p.position();
        Vec3 dir = flatLook(p);
        Vec3 end = safeEnd(p, dir, mach ? 20.0 : (double)(8 + d.projectionStacks * 2));
        if (end.distanceTo(start) < 1.5) {
            return false;
        } else {
            Set<Integer> hit = new HashSet<>();
            int frames = mach ? 3 : 6;

            for (int i = 1; i <= frames; i++) {
                int k = i;
                Scheduler.later(i, () -> {
                    if (p.isAlive()) {
                        Vec3 from = start.add(end.subtract(start).scale((double)(k - 1) / (double)frames));
                        Vec3 to = start.add(end.subtract(start).scale((double)k / (double)frames));
                        Vfx.play(level, VfxKind.FRAME_TRAIL, from.add(0.0, 1.0, 0.0), to.subtract(from), 0.8F, 8050920, p.getId(), 0);
                        p.teleportTo(to.x, to.y, to.z);
                        p.fallDistance = 0.0F;

                        for (LivingEntity e : BloodManipulation.along(p, from.add(0.0, 1.0, 0.0), to.add(0.0, 1.0, 0.0), mach ? 2.4 : 1.3)) {
                            if (hit.add(e.getId())) {
                                Combat.hurt(e, p, p, (mach ? 18.0F : 5.0F) * power);
                                if (mach) {
                                    Combat.knock(e, dir, 2.0, 0.5);
                                } else {
                                    frame(p, e, 30);
                                }
                            }
                        }

                        Combat.sound(p, to, ModSounds.FRAME, 0.9F, 1.2F + (float)k * 0.08F);
                    }
                });
            }

            if (mach) {
                p.displayClientMessage(Component.translatable("message.fallenroses.mach").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
                Scheduler.later(frames + 1, () -> {
                    Vfx.play(level, VfxKind.MACH_BOOM, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.6F, 8050920, -1, 0);
                    Combat.sound(p, start, ModSounds.SONIC_BOOM, 4.0F, 1.0F);
                    Combat.shake(level, end, 2.0F, 18, 32.0);
                });
                d.projectionStacks = 0;
            }

            return true;
        }
    }

    private static boolean freeze(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 7.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            Ratio.dashTo(p, target);
            Combat.hurt(target, p, p, 3.0F * power);
            frame(p, target, 50);
            Scheduler.later(50, () -> {
                if (target.isAlive()) {
                    Combat.hurt(target, p, p, 8.0F * power);
                }
            });
            return true;
        }
    }

    private static boolean barrage(ServerPlayer p, PlayerData d, float power) {
        LivingEntity target = Combat.lookTarget(p, 12.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "flurry");
            ServerLevel level = p.serverLevel();
            boolean wasFramed = framed(target);
            int hits = 6 + d.projectionStacks;

            for (int i = 0; i < hits; i++) {
                int k = i;
                Scheduler.later(i * 2, () -> {
                    if (p.isAlive() && target.isAlive()) {
                        double a = (double)k * (Math.PI / 3) + p.getRandom().nextDouble() * 0.4;
                        Vec3 spot = target.position().add(Math.cos(a) * 1.6, 0.0, Math.sin(a) * 1.6);
                        Vec3 from = p.position();
                        p.teleportTo(spot.x, Math.max(spot.y, target.getY()), spot.z);
                        p.lookAt(Anchor.EYES, target.getEyePosition());
                        Vfx.play(level, VfxKind.FRAME_TRAIL, from.add(0.0, 1.0, 0.0), p.position().subtract(from), 0.6F, 8050920, p.getId(), 0);
                        Vfx.play(level, VfxKind.FLURRY_HIT, target.getBoundingBox().getCenter(), target.position().subtract(p.position()), 1.0F);
                        Combat.hurt(target, p, p, (wasFramed ? 6.0F : 3.5F) * power);
                        Combat.sound(p, target.position(), ModSounds.FRAME, 1.0F, 1.4F + level.random.nextFloat() * 0.2F);
                    }
                });
            }

            p.fallDistance = 0.0F;
            return true;
        }
    }

    private static boolean accelerate(ServerPlayer p, PlayerData d) {
        PlayerAnims.play(p, "snap");
        d.projectionStacks = Math.min(5, d.projectionStacks + 2);
        d.projectionDecay = 200;
        int amp = Math.min(4, 1 + d.projectionStacks / 2);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, amp, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1, false, false, true));
        Vfx.play(p.serverLevel(), VfxKind.FRAME_PANEL, p.position(), Vec3.ZERO, 200.0F, 8050920, p.getId(), 2);
        Combat.sound(p, p.position(), ModSounds.FRAME, 1.6F, 0.6F);
        p.displayClientMessage(
            Component.translatable("message.fallenroses.acceleration_stacks", d.projectionStacks + 1).withStyle(s -> s.withColor(8050920)), true
        );
        return true;
    }

    private Projection() {
    }
}
