package com.fallenroses.technique.impl;

import com.fallenroses.entity.curse.CurseEntity;
import com.fallenroses.entity.curse.CursedSpirit;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class IdleTransfiguration {
    static final int SOUL = 8044728;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "soul_touch" -> touch(p, power);
            case "body_repel" -> repel(p, power);
            case "spike_burst" -> spikes(p, power);
            case "soul_multiplicity" -> multiplicity(p, power);
            case "self_embodiment" -> Domains.expand(p, d, 3, power);
            default -> false;
        };
    }

    private static boolean touch(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 4.5);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            ServerLevel level;
            Vec3 c;
            boolean var10000;
            label41: {
                PlayerAnims.play(p, "touch");
                level = p.serverLevel();
                c = target.getBoundingBox().getCenter();
                if (target instanceof CursedSpirit s && s.grade() <= 0) {
                    var10000 = true;
                    break label41;
                }

                var10000 = false;
            }

            boolean boss = var10000;
            boolean transfigure = !boss && !(target instanceof Player) && target.getHealth() <= Math.max(14.0F * power, target.getMaxHealth() * 0.3F);
            Vfx.play(level, VfxKind.SOUL_TOUCH, c, Vec3.ZERO, target.getBbHeight(), 8044728, target.getId(), transfigure ? 1 : 0);
            Combat.sound(p, c, ModSounds.SOUL_TOUCH, 1.6F, transfigure ? 0.7F : 1.0F);
            if (transfigure) {
                target.hurt(p.damageSources().indirectMagic(p, p), Float.MAX_VALUE);
            } else {
                target.invulnerableTime = 0;
                target.hurt(p.damageSources().indirectMagic(p, p), 10.0F * power);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), p);
            }

            return true;
        }
    }

    private static boolean repel(ServerPlayer p, float power) {
        PlayerAnims.play(p, "sweep");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.REPEL_SWEEP, p.getEyePosition().add(0.0, -0.4, 0.0), look, 5.5F, 8044728, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.SOUL_TOUCH, 1.4F, 0.6F);
        Combat.sound(p, p.position(), ModSounds.SLAM, 1.2F, 1.2F);

        for (LivingEntity e : Combat.cone(p, 6.0, 0.2)) {
            Combat.hurt(e, p, p, 9.0F * power);
            Combat.knock(e, e.position().subtract(p.position()), 2.0, 0.55);
        }

        Combat.shake(level, p.position(), 0.7F, 8, 10.0);
        return true;
    }

    private static boolean spikes(ServerPlayer p, float power) {
        PlayerAnims.play(p, "berserk");
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0.0, 1.0, 0.0);
        Vfx.play(level, VfxKind.SPIKES, c, Vec3.ZERO, 4.2F, 8044728, p.getId(), 0);
        Combat.sound(p, c, ModSounds.CLEAVE, 1.6F, 0.7F);

        for (LivingEntity e : Combat.around(p, c, 4.2)) {
            Combat.hurt(e, p, p, 8.5F * power);
            Combat.knock(e, e.position().subtract(p.position()), 0.9, 0.3);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), p);
        }

        return true;
    }

    private static boolean multiplicity(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Combat.sound(p, p.position(), ModSounds.SOUL_TOUCH, 2.0F, 0.5F);

        for (int i = 0; i < 3; i++) {
            int n = i;
            Scheduler.later(4 + i * 5, () -> {
                double a = Math.toRadians((double)(p.getYRot() + 90.0F + (float)((n - 1) * 50)));
                Vec3 at = p.position().add(Math.cos(a) * 2.2, 0.1, Math.sin(a) * 2.2);
                CurseEntity c = CurseEntity.summon(ModEntities.TRANSFIGURED_HUMAN.get(), p, at, 800);
                if (c != null) {
                    c.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 * (double)power);
                }
            });
        }

        return true;
    }

    private IdleTransfiguration() {
    }
}
