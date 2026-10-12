package com.fallenroses.technique.impl;

import com.fallenroses.entity.ShotEntity;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModEntities;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class IceFormation {
    static final int ICE = 11067647;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "ice_shards" -> shards(p, power);
            case "frost_calm" -> frost(p, power);
            case "ice_fall" -> iceFall(p, power);
            case "frozen_coffin" -> coffin(p, power);
            case "glacial_slope" -> glacier(p, power);
            default -> false;
        };
    }

    static void freeze(LivingEntity e, int ticks) {
        e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + ticks));
    }

    private static boolean shards(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 hand = Limitless.hand(p);
        Vec3 look = p.getViewVector(1.0F);

        for (int i = -2; i <= 2; i++) {
            double a = Math.toRadians((double)(i * 7));
            Vec3 v = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a)).normalize().scale(1.7);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.ICE_SHARD).launch(p, hand, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.ICE_FORM, 1.0F, 1.6F);
        return true;
    }

    private static boolean frost(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.FROST_CONE, Limitless.hand(p), look, 10.0F, 11067647, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.ICE_FORM, 2.0F, 0.7F);

        for (LivingEntity e : Combat.cone(p, 10.0, 0.72)) {
            Combat.hurt(e, p, p, 5.0F * power);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), p);
            freeze(e, 140);
        }

        return true;
    }

    private static boolean iceFall(ServerPlayer p, float power) {
        PlayerAnims.play(p, "slam");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, 0.0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
        Set<Integer> hit = new HashSet<>();

        for (int i = 1; i <= 7; i++) {
            Vec3 at = BloodManipulation.groundBelow(p, p.position().add(dir.scale((double)i * 1.7)).add(0.0, 1.5, 0.0));
            Scheduler.later(i * 2, () -> {
                Vfx.play(level, VfxKind.ICE_SPIKE, at, new Vec3(0.0, 1.0, 0.0), 1.6F, 11067647, -1, 0);
                Combat.sound(p, at, ModSounds.ICE_FORM, 1.2F, 0.8F + level.random.nextFloat() * 0.3F);

                for (LivingEntity e : Combat.around(p, at.add(0.0, 1.0, 0.0), 1.8)) {
                    if (hit.add(e.getId())) {
                        Combat.hurt(e, p, p, 8.0F * power);
                        e.push(0.0, 0.8, 0.0);
                        e.hurtMarked = true;
                        freeze(e, 60);
                    }
                }
            });
        }

        return true;
    }

    private static boolean coffin(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 16.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 60, 0), p);
            freeze(target, 100);
            Vfx.play(level, VfxKind.ICE_COFFIN, target.position(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 11067647, target.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.ICE_FORM, 2.0F, 0.6F);
            Scheduler.later(60, () -> {
                if (target.isAlive()) {
                    Vfx.play(level, VfxKind.ICE_COFFIN, target.position(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 11067647, target.getId(), 1);
                    Combat.sound(p, target.position(), ModSounds.ICE_SHATTER, 2.2F, 0.9F);
                    Combat.hurt(target, p, p, 14.0F * power);

                    for (LivingEntity e : Combat.around(p, target.position().add(0.0, 1.0, 0.0), 3.0)) {
                        if (e != target) {
                            Combat.hurt(e, p, p, 5.0F * power);
                        }
                    }
                }
            });
            return true;
        }
    }

    private static boolean glacier(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        p.displayClientMessage(Component.translatable("message.fallenroses.glacial_slope").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 4, false, false));
        Combat.sound(p, p.position(), ModSounds.ICE_FORM, 3.0F, 0.5F);
        Vec3 c = p.position();
        Scheduler.later(
            12,
            () -> {
                if (p.isAlive()) {
                    Vfx.play(level, VfxKind.GLACIER, c, Vec3.ZERO, 14.0F, 11067647, -1, 0);
                    Combat.sound(p, c, ModSounds.ICE_SHATTER, 4.0F, 0.5F);
                    Combat.flash(level, c, 15268095, 14, 0.8F, 40.0);
                    Combat.shake(level, c, 2.0F, 24, 40.0);

                    for (LivingEntity e : Combat.around(p, c, 14.0)) {
                        Combat.hurt(e, p, p, 22.0F * power);
                        freeze(e, 200);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4), p);
                        e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 40, 0), p);
                    }

                    for (int i = 0; i < 18; i++) {
                        double a = level.random.nextDouble() * (float) (Math.PI * 2);
                        double r = 2.0 + level.random.nextDouble() * 12.0;
                        Vec3 at = BloodManipulation.groundBelow(p, c.add(Math.cos(a) * r, 2.0, Math.sin(a) * r));
                        Scheduler.later(
                            level.random.nextInt(10),
                            () -> Vfx.play(level, VfxKind.ICE_SPIKE, at, new Vec3(0.0, 1.0, 0.0), 1.4F + level.random.nextFloat(), 11067647, -1, 0)
                        );
                    }
                }
            }
        );
        return true;
    }

    private IceFormation() {
    }
}
