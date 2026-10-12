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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DisasterFlames {
    static final int FIRE = 16738832;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "ember_insects" -> insects(p, power);
            case "flame_breath" -> breath(p, power);
            case "fire_pillar" -> pillar(p, power);
            case "maximum_meteor" -> meteor(p, power);
            case "coffin_of_iron_mountain" -> Domains.expand(p, d, 2, power);
            default -> false;
        };
    }

    private static boolean insects(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        Vec3 hand = Limitless.hand(p);
        Vec3 look = p.getViewVector(1.0F);

        for (int i = 0; i < 3; i++) {
            Vec3 v = look.add(p.getRandom().nextGaussian() * 0.35, 0.25 + p.getRandom().nextDouble() * 0.3, p.getRandom().nextGaussian() * 0.35)
                .normalize()
                .scale(0.5);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.EMBER_INSECT).launch(p, hand, v, power);
        }

        Vfx.play(p.serverLevel(), VfxKind.EMBER_BOOM, hand, Vec3.ZERO, 0.6F, 16738832, -1, 0);
        Combat.sound(p, p.position(), ModSounds.FIRE_ROAR, 1.0F, 1.6F);
        return true;
    }

    private static boolean breath(ServerPlayer p, float power) {
        PlayerAnims.play(p, "speech");
        ServerLevel level = p.serverLevel();
        Combat.sound(p, p.position(), ModSounds.FIRE_ROAR, 2.0F, 0.9F);

        for (int i = 0; i < 8; i++) {
            Scheduler.later(i * 4, () -> {
                if (p.isAlive()) {
                    Vec3 look = p.getViewVector(1.0F);
                    Vfx.play(level, VfxKind.FLAME_CONE, p.getEyePosition().add(look.scale(0.6)).add(0.0, -0.2, 0.0), look, 10.0F, 16738832, p.getId(), 0);

                    for (LivingEntity e : Combat.cone(p, 10.0, 0.75)) {
                        Combat.hurt(e, p, p, 2.0F * power);
                        e.setSecondsOnFire(5);
                    }
                }
            });
        }

        return true;
    }

    private static boolean pillar(ServerPlayer p, float power) {
        PlayerAnims.play(p, "slam");
        ServerLevel level = p.serverLevel();
        LivingEntity target = Combat.lookTarget(p, 24.0);
        Vec3 at = target != null ? target.position() : BloodManipulation.groundBelow(p, Combat.lookPoint(p, 24.0));
        Vfx.play(level, VfxKind.FIRE_PILLAR, at, Vec3.ZERO, 1.0F, 16738832, -1, 0);
        Combat.sound(p, at, ModSounds.BOSS_ROOTS, 1.5F, 0.6F);
        Scheduler.later(14, () -> {
            Vfx.play(level, VfxKind.FIRE_PILLAR, at, Vec3.ZERO, 2.6F, 16738832, -1, 1);
            Combat.sound(p, at, ModSounds.FIRE_ROAR, 3.0F, 0.7F);
            Combat.shake(level, at, 1.2F, 14, 20.0);

            for (LivingEntity e : Combat.around(p, at.add(0.0, 1.5, 0.0), 2.8)) {
                Combat.hurt(e, p, p, 11.0F * power);
                e.setSecondsOnFire(8);
                e.push(0.0, 1.1, 0.0);
                e.hurtMarked = true;
            }
        });
        return true;
    }

    private static boolean meteor(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vec3 ground = BloodManipulation.groundBelow(p, Combat.lookPoint(p, 32.0));
        Vec3 sky = ground.add(-6.0, 28.0, -6.0);
        p.displayClientMessage(Component.translatable("message.fallenroses.meteor").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        Vfx.play(level, VfxKind.FIRE_PILLAR, ground, Vec3.ZERO, 2.5F, 16738832, -1, 0);
        Combat.sound(p, p.position(), ModSounds.FUGA_CHARGE, 3.0F, 0.5F);
        Scheduler.later(12, () -> {
            Vec3 v = ground.subtract(sky).normalize().scale(1.25);
            new ShotEntity(ModEntities.SHOT.get(), level).kind(ShotEntity.Kind.METEOR).launch(p, sky, v, power);
            Combat.sound(p, ground, ModSounds.FIRE_ROAR, 4.0F, 0.5F);
        });
        return true;
    }

    private DisasterFlames() {
    }
}
