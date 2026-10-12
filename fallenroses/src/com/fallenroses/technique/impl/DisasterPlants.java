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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class DisasterPlants {
    static final int GREEN = 5943360;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "cursed_buds" -> buds(p, power);
            case "root_eruption" -> roots(p, power);
            case "flower_field" -> field(p, power);
            case "wooden_balls" -> balls(p, power);
            case "sea_of_light" -> seaOfLight(p, power);
            default -> false;
        };
    }

    private static boolean buds(ServerPlayer p, float power) {
        List<LivingEntity> targets = Combat.cone(p, 12.0, 0.6);
        if (targets.isEmpty()) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "blue");

            for (LivingEntity e : targets) {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1), p);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), p);
                Combat.hurt(e, p, p, 3.0F * power);
                Vfx.play(p.serverLevel(), VfxKind.BUDS, e.getBoundingBox().getCenter(), Vec3.ZERO, e.getBbHeight(), 5943360, e.getId(), 120);
                p.heal(1.5F);
            }

            Combat.sound(p, p.position(), ModSounds.PLANT_GROW, 1.4F, 1.2F);
            return true;
        }
    }

    private static boolean roots(ServerPlayer p, float power) {
        PlayerAnims.play(p, "slam");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();

        for (int i = 1; i <= 4; i++) {
            Vec3 at = BloodManipulation.groundBelow(p, p.position().add(flat.scale((double)i * 3.0)).add(0.0, 1.0, 0.0));
            Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 0.4F, 2771472, -1, 0);
            Scheduler.later(6 + i * 4, () -> {
                Vfx.play(level, VfxKind.ROOT_ERUPT, at, Vec3.ZERO, 2.0F, 2771472, -1, 1);
                Combat.sound(p, at, ModSounds.BOSS_ROOTS, 1.6F, 1.1F);

                for (LivingEntity e : Combat.around(p, at.add(0.0, 1.0, 0.0), 2.3)) {
                    Combat.hurt(e, p, p, 9.0F * power);
                    e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 40, 0), p);
                    e.push(0.0, 0.7, 0.0);
                    e.hurtMarked = true;
                }
            });
        }

        return true;
    }

    private static boolean field(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        Vfx.play(level, VfxKind.FLOWER_FIELD, c.add(0.0, 0.05, 0.0), Vec3.ZERO, 6.0F, 5943360, -1, 160);
        Combat.sound(p, c, ModSounds.PLANT_GROW, 2.0F, 0.8F);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, true));

        for (int t = 0; t < 160; t += 20) {
            Scheduler.later(t, () -> {
                for (LivingEntity e : Combat.around(p, c.add(0.0, 0.5, 0.0), 6.0)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), p);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2), p);
                    Combat.hurt(e, p, p, 1.5F * power);
                }
            });
        }

        return true;
    }

    private static boolean balls(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 look = p.getViewVector(1.0F);
        Vec3 from = p.getEyePosition().add(look.scale(1.5)).add(0.0, -0.2, 0.0);

        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians((double)((i - 1) * 12));
            Vec3 v = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y + 0.15, look.x * Math.sin(a) + look.z * Math.cos(a)).scale(0.9);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.WOOD_BALL).launch(p, from, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.BOSS_ROOTS, 1.5F, 0.8F);
        return true;
    }

    private static boolean seaOfLight(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        PlayerAnims.play(p, "purple");
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34, 4, false, false));
        p.displayClientMessage(Component.translatable("message.fallenroses.sea_of_light").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC), true);
        Vfx.play(level, VfxKind.RED_CHARGE, Limitless.hand(p), p.getViewVector(1.0F), 2.2F, 14221248, p.getId(), 30);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 2.5F, 1.3F);
        Scheduler.later(30, () -> {
            if (p.isAlive()) {
                Vec3 from = p.getEyePosition().add(0.0, -0.3, 0.0);
                Vec3 look = p.getViewVector(1.0F);
                BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(48.0)), Block.COLLIDER, Fluid.NONE, p));
                Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(48.0)) : wall.getLocation();

                for (LivingEntity e : BloodManipulation.along(p, from, end, 2.4)) {
                    Combat.hurt(e, p, p, 34.0F * power);
                    Combat.knock(e, look, 1.5, 0.3);
                }

                Vfx.play(level, VfxKind.LIGHT_BEAM, from, end.subtract(from), 2.4F, 14221248, -1, 0);
                Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 4.0F, 1.2F);
                Combat.flash(level, p.position(), 15269848, 12, 0.6F, 48.0);
                Combat.shake(level, p.position(), 2.0F, 20, 32.0);
            }
        });
        return true;
    }

    private DisasterPlants() {
    }
}
