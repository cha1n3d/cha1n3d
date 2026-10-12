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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Construction {
    static final int METAL = 13158616;
    public static final String ARMOR = "fr_insect_armor_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "construct_bullet" -> bullet(p, power);
            case "iron_spikes" -> spikes(p, power);
            case "liquid_metal" -> liquidMetal(p, power);
            case "insect_armor" -> armor(p);
            case "truesphere" -> truesphere(p, power);
            default -> false;
        };
    }

    public static boolean armored(LivingEntity e) {
        return e.getPersistentData().getLong("fr_insect_armor_until") > e.level().getGameTime();
    }

    private static boolean bullet(ServerPlayer p, float power) {
        PlayerAnims.play(p, "snap");
        new ShotEntity(ModEntities.SHOT.get(), p.level())
            .kind(ShotEntity.Kind.ENERGY_SHOT)
            .launch(p, Limitless.hand(p), p.getViewVector(1.0F).scale(2.4), power * 1.1F);
        Combat.sound(p, p.position(), ModSounds.THUNDER, 0.7F, 2.0F);
        Vfx.play(p.serverLevel(), VfxKind.HIT_SPARK, Limitless.hand(p), p.getViewVector(1.0F), 0.6F, 16773312, -1, 0);
        return true;
    }

    private static boolean spikes(ServerPlayer p, float power) {
        PlayerAnims.play(p, "slam");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, 0.0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
        Set<Integer> hit = new HashSet<>();

        for (int i = 1; i <= 8; i++) {
            Vec3 at = BloodManipulation.groundBelow(p, p.position().add(dir.scale((double)i * 1.6)).add(0.0, 1.5, 0.0));
            Scheduler.later(i * 2, () -> {
                Vfx.play(level, VfxKind.SPIKES, at, new Vec3(0.0, 1.0, 0.0), 1.2F, 13158616, -1, 0);
                Combat.sound(p, at, ModSounds.NAIL_HIT, 1.2F, 0.6F);

                for (LivingEntity e : Combat.around(p, at.add(0.0, 1.0, 0.0), 1.6)) {
                    if (hit.add(e.getId())) {
                        Combat.hurt(e, p, p, 8.0F * power);
                        e.push(0.0, 0.6, 0.0);
                        e.hurtMarked = true;
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), p);
                    }
                }
            });
        }

        return true;
    }

    private static boolean liquidMetal(ServerPlayer p, float power) {
        PlayerAnims.play(p, "sweep");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.WING_FAN, p.position().add(0.0, 1.0, 0.0), look, 8.0F, 13158616, p.getId(), 0);
        Vfx.play(level, VfxKind.DUST_CLOUD, p.getEyePosition().add(look.scale(4.0)), Vec3.ZERO, 2.5F, 9079450, -1, 0);
        Combat.sound(p, p.position(), ModSounds.SPLASH, 1.4F, 0.6F);

        for (LivingEntity e : Combat.cone(p, 8.0, 0.55)) {
            Combat.hurt(e, p, p, 7.0F * power);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), p);
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1), p);
            Combat.knock(e, look, 0.5, 0.1);
        }

        return true;
    }

    private static boolean armor(ServerPlayer p) {
        PlayerAnims.play(p, "raise");
        p.getPersistentData().putLong("fr_insect_armor_until", p.level().getGameTime() + 300L);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, true, true));
        Vfx.play(p.serverLevel(), VfxKind.REINFORCE, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.0F, 13158616, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.ICE_FORM, 1.2F, 0.6F);
        p.displayClientMessage(Component.translatable("message.fallenroses.insect_armor").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    public static void thorns(ServerPlayer wearer, LivingEntity attacker) {
        if (armored(wearer)) {
            Combat.hurt(attacker, wearer, wearer, 4.0F);
            Vfx.play(
                wearer.serverLevel(),
                VfxKind.HIT_SPARK,
                attacker.getBoundingBox().getCenter(),
                attacker.position().subtract(wearer.position()),
                0.7F,
                13158616,
                -1,
                0
            );
        }
    }

    private static boolean truesphere(ServerPlayer p, float power) {
        PlayerAnims.play(p, "purple");
        ServerLevel level = p.serverLevel();
        p.displayClientMessage(Component.translatable("message.fallenroses.truesphere").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
        Vfx.play(level, VfxKind.RED_CHARGE, Limitless.hand(p), Vec3.ZERO, 1.8F, 15790335, p.getId(), 24);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 1.8F, 1.6F);
        Scheduler.later(
            24,
            () -> {
                if (p.isAlive()) {
                    new ShotEntity(ModEntities.SHOT.get(), level)
                        .kind(ShotEntity.Kind.TRUESPHERE)
                        .launch(p, p.getEyePosition().add(p.getViewVector(1.0F).scale(1.5)), p.getViewVector(1.0F).scale(0.7), power);
                    Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 2.5F, 1.4F);
                }
            }
        );
        return true;
    }

    private Construction() {
    }
}
