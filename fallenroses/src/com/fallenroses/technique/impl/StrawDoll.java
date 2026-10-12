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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class StrawDoll {
    static final int COPPER = 14708784;
    private static final String NAILS = "fr_nails";
    private static final String NAIL_TIME = "fr_nails_time";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "nails" -> nails(p, power, 3, 7.0);
            case "resonance" -> resonance(p, power, false);
            case "hairpin" -> hairpin(p, power);
            case "nail_storm" -> nailStorm(p, power);
            case "black_resonance" -> resonance(p, power, true);
            default -> false;
        };
    }

    public static void addNail(LivingEntity e) {
        CompoundTag tag = e.getPersistentData();
        tag.putInt("fr_nails", Math.min(8, nails(e) + 1));
        tag.putLong("fr_nails_time", e.level().getGameTime());
    }

    public static int nails(LivingEntity e) {
        CompoundTag tag = e.getPersistentData();
        if (!tag.contains("fr_nails")) {
            return 0;
        } else {
            return e.level().getGameTime() - tag.getLong("fr_nails_time") > 600L ? 0 : tag.getInt("fr_nails");
        }
    }

    private static void clearNails(LivingEntity e) {
        e.getPersistentData().remove("fr_nails");
    }

    private static boolean nails(ServerPlayer p, float power, int count, double spreadDeg) {
        PlayerAnims.play(p, "hammer");
        Vec3 look = p.getViewVector(1.0F);
        Vec3 from = p.getEyePosition().add(look.scale(0.7)).add(0.0, -0.2, 0.0);

        for (int i = 0; i < count; i++) {
            double a = Math.toRadians(((double)i - (double)(count - 1) / 2.0) * spreadDeg);
            Vec3 v = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a)).scale(2.2);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.NAIL).launch(p, from, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.HAMMER, 1.4F, 1.0F + p.getRandom().nextFloat() * 0.15F);
        return true;
    }

    private static List<LivingEntity> marked(ServerPlayer p) {
        List<LivingEntity> out = new ArrayList<>();

        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32.0), ex -> Combat.valid(ex, p))) {
            if (nails(e) > 0) {
                out.add(e);
            }
        }

        return out;
    }

    private static boolean resonance(ServerPlayer p, float power, boolean black) {
        List<LivingEntity> targets = marked(p);
        if (black) {
            for (LivingEntity e : Combat.around(p, p.position(), 14.0)) {
                if (!targets.contains(e) && p.hasLineOfSight(e)) {
                    targets.add(e);
                }
            }
        }

        LivingEntity aim = Combat.lookTarget(p, 24.0);
        if (aim != null && !targets.contains(aim)) {
            targets.add(aim);
        }

        if (targets.isEmpty()) {
            return Ratio.noTarget(p);
        } else {
            ServerLevel level = p.serverLevel();
            PlayerAnims.play(p, "hammer");
            Vfx.play(
                level,
                VfxKind.RESONANCE_DOLL,
                p.position().add(0.0, 1.0, 0.0),
                p.getViewVector(1.0F),
                black ? 1.6F : 1.0F,
                black ? 16715808 : 14708784,
                p.getId(),
                0
            );
            Combat.sound(p, p.position(), ModSounds.HAMMER, 2.0F, black ? 0.6F : 0.8F);
            Scheduler.later(
                6,
                () -> {
                    for (LivingEntity ex : targets) {
                        if (ex.isAlive()) {
                            int n = Math.max(1, Math.min(5, nails(ex)));
                            float dmg = black ? 20.0F * power + (float)n * 3.0F * power : 7.0F * power * (float)n;
                            Combat.hurt(ex, p, p, dmg);
                            clearNails(ex);
                            Vec3 c = ex.getBoundingBox().getCenter();
                            Vfx.play(
                                level,
                                VfxKind.RESONANCE_HIT,
                                c,
                                p.getEyePosition().subtract(c),
                                black ? 2.0F : 1.0F + (float)n * 0.15F,
                                black ? 16715808 : 14708784,
                                ex.getId(),
                                black ? 1 : 0
                            );
                            if (black) {
                                Vfx.play(level, VfxKind.BLACK_FLASH, c, Vec3.ZERO, 1.2F);
                            }
                        }
                    }

                    Combat.sound(p, p.position(), black ? ModSounds.BLACK_FLASH : ModSounds.RESONANCE, black ? 3.0F : 2.0F, 1.0F);
                    if (black) {
                        Combat.flash(level, p.position(), 2097152, 10, 0.6F, 32.0);
                        Combat.shake(level, p.position(), 1.6F, 20, 32.0);
                    }
                }
            );
            return true;
        }
    }

    private static boolean hairpin(ServerPlayer p, float power) {
        List<LivingEntity> targets = marked(p);
        if (targets.isEmpty()) {
            return Ratio.noTarget(p);
        } else {
            ServerLevel level = p.serverLevel();
            PlayerAnims.play(p, "snap");
            Combat.sound(p, p.position(), ModSounds.CLAP, 1.2F, 1.4F);

            for (LivingEntity e : targets) {
                int n = Math.min(5, nails(e));
                Vec3 c = e.getBoundingBox().getCenter();
                clearNails(e);
                Vfx.play(level, VfxKind.EMBER_BOOM, c, Vec3.ZERO, 1.6F + (float)n * 0.3F, 14708784, -1, 0);
                Combat.sound(p, c, ModSounds.FUGA_IMPACT, 1.0F, 1.6F);

                for (LivingEntity o : Combat.around(p, c, 2.5)) {
                    Combat.hurt(o, p, p, (o == e ? 5.0F * (float)n : 4.0F) * power);
                }
            }

            return true;
        }
    }

    private static boolean nailStorm(ServerPlayer p, float power) {
        for (int i = 0; i < 5; i++) {
            Scheduler.later(i * 4, () -> {
                if (p.isAlive()) {
                    nails(p, power * 0.8F, 2, 4.0);
                }
            });
        }

        return true;
    }

    private StrawDoll() {
    }
}
