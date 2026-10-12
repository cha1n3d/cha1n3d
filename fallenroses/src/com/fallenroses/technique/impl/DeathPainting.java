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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DeathPainting {
    static final int ROT = 10123824;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "rot_blood" -> spit(p, power);
            case "wing_king" -> wing(p, power);
            case "decay_mark" -> mark(p, power);
            case "rot_mist" -> mist(p, power);
            case "decay_bloom" -> bloom(p, power);
            default -> false;
        };
    }

    public static void rot(LivingEntity e, LivingEntity source, int stacks, int ticks) {
        MobEffectInstance cur = e.getEffect(ModEffects.ROT.get());
        int amp = cur == null ? stacks - 1 : Math.min(3, cur.getAmplifier() + stacks);
        e.addEffect(new MobEffectInstance(ModEffects.ROT.get(), Math.max(ticks, cur == null ? 0 : cur.getDuration()), Math.min(3, amp)), source);
    }

    private static boolean spit(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blood_beam");
        Vec3 look = p.getViewVector(1.0F);

        for (int i = -1; i <= 1; i++) {
            double a = Math.toRadians((double)(i * 6));
            Vec3 v = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a)).normalize().scale(1.4);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.ROT_BLOOD).launch(p, Limitless.hand(p), v, power);
        }

        Combat.sound(p, p.position(), ModSounds.BLOOD_WHOOSH, 1.0F, 0.7F);
        return true;
    }

    private static boolean wing(ServerPlayer p, float power) {
        PlayerAnims.play(p, "sweep");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.WING_FAN, p.position().add(0.0, 1.3, 0.0), look, 9.0F, 10123824, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.BLOOD_BEAM, 1.2F, 0.6F);

        for (LivingEntity e : Combat.cone(p, 9.0, 0.5)) {
            Combat.hurt(e, p, p, 6.0F * power);
            rot(e, p, 1, 140);
            Combat.knock(e, look, 0.6, 0.2);
        }

        return true;
    }

    private static boolean mark(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 18.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            Vfx.play(level, VfxKind.PLAGUE_MARK, target.position(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 10123824, target.getId(), 200);
            Combat.sound(p, target.position(), ModSounds.PLAGUE, 1.2F, 0.8F);
            rot(target, p, 3, 200);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1), p);
            Combat.hurt(target, p, p, 4.0F * power);
            return true;
        }
    }

    private static boolean mist(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vfx.play(level, VfxKind.ROT_CLOUD, p.position(), Vec3.ZERO, 6.0F, 10123824, p.getId(), 100);
        Combat.sound(p, p.position(), ModSounds.PLAGUE, 1.6F, 0.6F);

        for (int i = 1; i <= 5; i++) {
            Scheduler.later(i * 20, () -> {
                if (p.isAlive()) {
                    for (LivingEntity e : Combat.around(p, p.position().add(0.0, 1.0, 0.0), 6.0)) {
                        Combat.hurt(e, p, p, 2.5F * power);
                        rot(e, p, 1, 120);
                    }
                }
            });
        }

        return true;
    }

    private static boolean bloom(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        p.displayClientMessage(Component.translatable("message.fallenroses.decay_bloom").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        Vfx.play(level, VfxKind.ROT_CLOUD, p.position(), Vec3.ZERO, 12.0F, 10123824, p.getId(), 30);
        Combat.sound(p, p.position(), ModSounds.PLAGUE, 2.4F, 0.5F);

        for (LivingEntity e : Combat.around(p, p.position(), 12.0)) {
            rot(e, p, 1, 200);
        }

        Scheduler.later(30, () -> {
            if (p.isAlive()) {
                List<LivingEntity> rotting = new ArrayList<>();

                for (LivingEntity exx : Combat.around(p, p.position(), 20.0)) {
                    if (exx.hasEffect(ModEffects.ROT.get())) {
                        rotting.add(exx);
                    }
                }

                Combat.flash(level, p.position(), 13148240, 8, 0.4F, 30.0);
                Combat.shake(level, p.position(), 1.4F, 20, 30.0);
                int i = 0;

                for (LivingEntity ex : rotting) {
                    Scheduler.later(i++ * 2, () -> {
                        if (ex.isAlive()) {
                            MobEffectInstance r = ex.getEffect(ModEffects.ROT.get());
                            int amp = r == null ? 0 : r.getAmplifier();
                            ex.removeEffect(ModEffects.ROT.get());
                            Vfx.play(level, VfxKind.ROT_BURST, ex.getBoundingBox().getCenter(), Vec3.ZERO, 1.5F + (float)amp * 0.5F, 10123824, -1, 0);
                            Combat.sound(p, ex.position(), ModSounds.BLOOD_BURST, 1.6F, 0.6F);
                            Combat.hurt(ex, p, p, (8.0F + 6.0F * (float)(amp + 1)) * power);
                        }
                    });
                }
            }
        });
        return true;
    }

    private DeathPainting() {
    }
}
