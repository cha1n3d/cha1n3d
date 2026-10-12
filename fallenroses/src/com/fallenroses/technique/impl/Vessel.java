package com.fallenroses.technique.impl;

import com.fallenroses.entity.SlashEntity;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.player.FRAdvancements;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class Vessel {
    static final int RED = 14692412;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "vessel_dismantle" -> dismantle(p, power);
            case "vessel_cleave" -> cleave(p, power);
            case "blood_spear" -> bloodSpear(p, d, power);
            case "black_flash_rush" -> blackFlash(p, d, power);
            case "vessel_domain" -> Domains.expand(p, d, 9, power);
            default -> false;
        };
    }

    private static boolean dismantle(ServerPlayer p, float power) {
        PlayerAnims.play(p, "punch");
        Combat.sound(p, p.position(), ModSounds.DISMANTLE, 1.4F, 1.1F);
        Vec3 look = p.getViewVector(1.0F);
        Vec3 from = p.getEyePosition().add(look.scale(1.1)).add(0.0, -0.2, 0.0);

        for (float roll : new float[]{35.0F, -35.0F}) {
            new SlashEntity(ModEntities.SLASH.get(), p.level()).setup(roll, 3.0F).launch(p, from, look.scale(2.1), power * 0.8F);
        }

        Vfx.play(p.serverLevel(), VfxKind.HIT_SPARK, from, look, 1.0F, 16777215, -1, 0);
        return true;
    }

    private static boolean cleave(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 8.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "cleave");
            Ratio.dashTo(p, target);
            ServerLevel level = p.serverLevel();
            Vfx.play(level, VfxKind.CLEAVE, target.getBoundingBox().getCenter(), Vec3.ZERO, Math.max(1.0F, target.getBbHeight()), 16777215, target.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.CLEAVE, 1.6F, 1.1F);
            float total = 5.0F * power + Math.min(45.0F, target.getMaxHealth() * 0.22F) * Math.min(1.0F, power);

            for (int i = 0; i < 3; i++) {
                Scheduler.later(
                    i * 3,
                    () -> {
                        if (target.isAlive()) {
                            Combat.hurt(target, p, p, total / 3.0F);
                            Vfx.play(
                                level,
                                VfxKind.SLASH_HIT,
                                target.getBoundingBox().getCenter(),
                                new Vec3(p.getRandom().nextGaussian(), p.getRandom().nextGaussian() * 0.4, p.getRandom().nextGaussian()),
                                1.4F,
                                16777215,
                                -1,
                                p.getRandom().nextInt(180)
                            );
                        }
                    }
                );
            }

            Combat.shake(level, target.position(), 0.7F, 8, 10.0);
            return true;
        }
    }

    private static boolean bloodSpear(ServerPlayer p, PlayerData d, float power) {
        PlayerAnims.play(p, "blood_beam");
        ServerLevel level = p.serverLevel();
        Vfx.play(level, VfxKind.BLOOD_CHARGE, Limitless.hand(p), p.getViewVector(1.0F), 1.0F, 13635632, p.getId(), 8);
        Combat.sound(p, p.position(), ModSounds.BLOOD_CHARGE, 1.5F, 1.1F);
        float bonus = 1.0F + (float)(d.getPaintings() - 1) * 0.06F;
        Scheduler.later(8, () -> {
            if (p.isAlive()) {
                Vec3 from = p.getEyePosition().add(0.0, -0.25, 0.0);
                Vec3 look = p.getViewVector(1.0F);
                BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(30.0)), Block.COLLIDER, Fluid.NONE, p));
                Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(30.0)) : wall.getLocation();

                for (LivingEntity e : BloodManipulation.along(p, from, end, 0.9)) {
                    Combat.hurt(e, p, p, 13.0F * power * bonus);
                    Combat.knock(e, look, 0.6, 0.1);
                }

                Vfx.play(level, VfxKind.BLOOD_BEAM, from, end.subtract(from), 1.2F, 13635632, -1, 0);
                Combat.sound(p, p.position(), ModSounds.BLOOD_BEAM, 2.0F, 0.9F);
                Combat.shake(level, p.position(), 0.6F, 6, 8.0);
            }
        });
        return true;
    }

    private static boolean blackFlash(ServerPlayer p, PlayerData d, float power) {
        LivingEntity target = Combat.lookTarget(p, 10.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "flurry");
            Ratio.dashTo(p, target);
            ServerLevel level = p.serverLevel();
            Combat.hurt(target, p, p, 5.0F * power);
            Vfx.play(level, VfxKind.HIT_SPARK, target.getBoundingBox().getCenter(), target.position().subtract(p.position()), 1.2F, 14692412, -1, 0);
            Scheduler.later(
                5,
                () -> {
                    if (p.isAlive() && target.isAlive()) {
                        float dmg = 7.0F * power * 2.5F;
                        Combat.hurt(target, p, p, dmg);
                        Combat.knock(target, target.position().subtract(p.position()), 1.4, 0.4);
                        d.zoneTicks = 200;
                        d.regen(20.0F);
                        d.markUrgent();
                        Vfx.play(
                            level,
                            VfxKind.BLACK_FLASH,
                            target.getBoundingBox().getCenter(),
                            target.position().subtract(p.position()),
                            1.0F,
                            0,
                            target.getId(),
                            0
                        );
                        Combat.sound(p, target.position(), ModSounds.BLACK_FLASH, 3.0F, 1.0F);
                        Combat.shake(level, target.position(), 1.6F, 14, 20.0);
                        ModNetwork.send(p, new SimplePackets.ScreenFx(0, 1703940, 6, 0.55F, ""));
                        p.displayClientMessage(
                            Component.translatable("message.fallenroses.black_flash").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true
                        );
                        FRAdvancements.award(p, "story/black_flash");
                    }
                }
            );
            return true;
        }
    }

    private Vessel() {
    }
}
