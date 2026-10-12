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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Smallpox {
    static final int POX = 10133600;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "pox_cloud" -> cloud(p, power);
            case "grave_marker" -> graves(p, power);
            case "coffin_trap" -> coffin(p, power);
            case "plague_bloom" -> plague(p, power);
            case "cemetery" -> Domains.expand(p, d, 5, power);
            default -> false;
        };
    }

    private static Vec3 aim(ServerPlayer p, double range) {
        LivingEntity t = Combat.lookTarget(p, range);
        return t != null ? t.position() : BloodManipulation.groundBelow(p, Combat.lookPoint(p, range));
    }

    private static boolean cloud(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 18.0);
        Vfx.play(level, VfxKind.POX_CLOUD, at, Vec3.ZERO, 3.5F, 10133600, -1, 100);
        Combat.sound(p, at, ModSounds.PLAGUE, 1.8F, 1.0F);

        for (int i = 0; i < 10; i++) {
            Scheduler.later(i * 10, () -> {
                for (LivingEntity e : Combat.around(p, at.add(0.0, 0.8, 0.0), 3.5)) {
                    Combat.hurt(e, p, p, 1.5F * power);
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), p);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), p);
                }
            });
        }

        return true;
    }

    private static boolean graves(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 22.0);

        for (int i = 0; i < 3; i++) {
            Scheduler.later(i * 5, () -> {
                Vec3 from = at.add(level.random.nextGaussian() * 0.8, 9.0, level.random.nextGaussian() * 0.8);
                new ShotEntity(ModEntities.SHOT.get(), level).kind(ShotEntity.Kind.GRAVESTONE).launch(p, from, new Vec3(0.0, -0.5, 0.0), power);
            });
        }

        Combat.sound(p, p.position(), ModSounds.POX_MOAN, 1.4F, 1.2F);
        return true;
    }

    private static boolean coffin(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 16.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            Vec3 trap = target.position();
            target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 60, 0), p);
            Vfx.play(level, VfxKind.COFFIN, trap, Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 5917232, target.getId(), 60);

            for (int i = 0; i < 3; i++) {
                int n = 3 - i;
                Scheduler.later(
                    i * 20,
                    () -> {
                        Combat.sound(p, trap, ModSounds.COFFIN_BELL, 2.0F, 0.7F + (float)(3 - n) * 0.1F);
                        p.displayClientMessage(
                            Component.translatable("message.fallenroses.coffin_count", n).withStyle(s -> s.withColor(13156480).withBold(true)), true
                        );
                    }
                );
            }

            Scheduler.later(60, () -> {
                if (target.isAlive() && !(target.position().distanceTo(trap) > 2.5)) {
                    Vfx.play(level, VfxKind.GRAVE_IMPACT, trap, Vec3.ZERO, 2.4F, 10133600, -1, 1);
                    Combat.sound(p, trap, ModSounds.GRAVE_THUD, 2.5F, 0.6F);
                    Combat.shake(level, trap, 1.2F, 12, 18.0);
                    Combat.hurt(target, p, p, 20.0F * power);
                }
            });
            return true;
        }
    }

    private static boolean plague(ServerPlayer p, float power) {
        List<LivingEntity> victims = Combat.around(p, p.position(), 12.0);
        if (victims.isEmpty()) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "domain");
            ServerLevel level = p.serverLevel();
            Combat.sound(p, p.position(), ModSounds.PLAGUE, 2.5F, 0.7F);
            Vfx.play(level, VfxKind.POX_CLOUD, p.position(), Vec3.ZERO, 6.0F, 10133600, -1, 30);

            for (LivingEntity e : victims) {
                Vfx.play(level, VfxKind.PLAGUE_MARK, e.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, e.getBbHeight()), 10133600, e.getId(), 100);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0), p);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), p);

                for (int i = 1; i <= 5; i++) {
                    Scheduler.later(i * 20, () -> {
                        if (e.isAlive()) {
                            Combat.hurt(e, p, p, 2.2F * power);
                        }
                    });
                }
            }

            return true;
        }
    }

    private static boolean cemetery(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        p.displayClientMessage(Component.translatable("message.fallenroses.cemetery").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), true);
        Combat.sound(p, c, ModSounds.COFFIN_BELL, 4.0F, 0.5F);
        Vfx.play(level, VfxKind.POX_CLOUD, c, Vec3.ZERO, 12.0F, 6974032, -1, 90);
        Combat.flash(level, c, 1710608, 16, 0.6F, 40.0);

        for (int i = 0; i < 20; i++) {
            int k = i;
            Scheduler.later(
                8 + i * 4,
                () -> {
                    if (p.isAlive()) {
                        List<LivingEntity> foes = Combat.around(p, c, 13.0);
                        Vec3 at;
                        if (k % 2 == 0 && !foes.isEmpty()) {
                            at = foes.get(level.random.nextInt(foes.size())).position();
                        } else {
                            double a = level.random.nextDouble() * (float) (Math.PI * 2);
                            double r = 2.0 + level.random.nextDouble() * 11.0;
                            at = BloodManipulation.groundBelow(p, c.add(Math.cos(a) * r, 2.0, Math.sin(a) * r));
                        }

                        new ShotEntity(ModEntities.SHOT.get(), level)
                            .kind(ShotEntity.Kind.GRAVESTONE)
                            .launch(
                                p, at.add(level.random.nextGaussian() * 0.5, 11.0, level.random.nextGaussian() * 0.5), new Vec3(0.0, -0.6, 0.0), power * 1.3F
                            );
                        if (k % 5 == 0) {
                            Combat.sound(p, c, ModSounds.COFFIN_BELL, 2.5F, 0.6F);
                        }
                    }
                }
            );
        }

        return true;
    }

    private Smallpox() {
    }
}
