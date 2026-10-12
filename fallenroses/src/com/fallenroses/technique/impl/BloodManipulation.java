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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class BloodManipulation {
    static final int RED = 13635632;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "piercing_blood" -> piercingBlood(p, power);
            case "slicing_exorcism" -> slicing(p, power);
            case "flowing_red_scale" -> redScale(p, d);
            case "supernova" -> supernova(p, power);
            case "blood_rain" -> bloodRain(p, power);
            default -> false;
        };
    }

    private static void bleed(ServerPlayer p, float amount) {
        if (!p.getAbilities().instabuild && p.getHealth() > amount + 2.0F) {
            p.hurt(p.damageSources().magic(), amount);
        }
    }

    private static boolean piercingBlood(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blood_beam");
        ServerLevel level = p.serverLevel();
        Vfx.play(level, VfxKind.BLOOD_CHARGE, Limitless.hand(p), p.getViewVector(1.0F), 1.0F, 13635632, p.getId(), 10);
        Combat.sound(p, p.position(), ModSounds.BLOOD_CHARGE, 1.5F, 1.0F);
        bleed(p, 1.0F);
        Scheduler.later(10, () -> {
            if (p.isAlive()) {
                Vec3 from = p.getEyePosition().add(0.0, -0.25, 0.0);
                Vec3 look = p.getViewVector(1.0F);
                double range = 32.0;
                BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(range)), Block.COLLIDER, Fluid.NONE, p));
                Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(range)) : wall.getLocation();

                for (LivingEntity e : along(p, from, end, 0.8)) {
                    Combat.hurt(e, p, p, 10.0F * power);
                    Combat.knock(e, look, 0.5, 0.1);
                }

                Vfx.play(level, VfxKind.BLOOD_BEAM, from, end.subtract(from), 1.0F, 13635632, -1, 0);
                Combat.sound(p, p.position(), ModSounds.BLOOD_BEAM, 2.0F, 1.0F);
                Combat.shake(level, p.position(), 0.5F, 6, 6.0);
            }
        });
        return true;
    }

    static List<LivingEntity> along(ServerPlayer p, Vec3 from, Vec3 to, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.001) {
            return out;
        } else {
            Vec3 dir = d.scale(1.0 / len);

            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(radius + 1.0), ex -> Combat.valid(ex, p))) {
                Vec3 c = e.getBoundingBox().getCenter().subtract(from);
                double t = c.dot(dir);
                if (!(t < 0.0) && !(t > len) && c.subtract(dir.scale(t)).length() <= radius + (double)(e.getBbWidth() / 2.0F)) {
                    out.add(e);
                }
            }

            return out;
        }
    }

    private static boolean slicing(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 look = p.getViewVector(1.0F);
        Vec3 from = p.getEyePosition().add(look.scale(0.8)).add(0.0, -0.3, 0.0);

        for (int side : new int[]{-1, 1}) {
            double a = Math.toRadians((double)(side * 22));
            Vec3 v = new Vec3(look.x * Math.cos(a) - look.z * Math.sin(a), look.y, look.x * Math.sin(a) + look.z * Math.cos(a)).scale(1.15);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.BLOOD_DISC).curve((float)(-side) * 3.2F).launch(p, from, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.BLOOD_WHOOSH, 1.5F, 1.1F);
        bleed(p, 1.0F);
        return true;
    }

    private static boolean redScale(ServerPlayer p, PlayerData d) {
        PlayerAnims.play(p, "berserk");
        d.scaleTicks = 300;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1, false, true));
        Vfx.play(p.serverLevel(), VfxKind.RED_SCALE, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.0F, 13635632, p.getId(), 300);
        Combat.sound(p, p.position(), ModSounds.BLOOD_CHARGE, 1.5F, 0.7F);
        p.displayClientMessage(Component.translatable("message.fallenroses.red_scale").withStyle(ChatFormatting.DARK_RED), true);
        bleed(p, 2.0F);
        return true;
    }

    private static boolean supernova(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");

        for (int i = 0; i < 6; i++) {
            Vec3 at = p.position().add(0.0, 1.4, 0.0);
            new ShotEntity(ModEntities.SHOT.get(), p.level())
                .kind(ShotEntity.Kind.BLOOD_ORB)
                .orbit(22 + i * 3, (float)i * 60.0F)
                .launch(p, at, Vec3.ZERO, power);
        }

        Combat.sound(p, p.position(), ModSounds.BLOOD_CHARGE, 2.0F, 1.3F);
        bleed(p, 2.0F);
        return true;
    }

    private static boolean bloodRain(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        PlayerAnims.play(p, "raise");
        Vec3 c = Combat.lookPoint(p, 24.0);
        Vec3 ground = groundBelow(p, c);
        Vfx.play(level, VfxKind.BLOOD_CLOUD, ground.add(0.0, 9.0, 0.0), Vec3.ZERO, 9.0F, 13635632, -1, 100);
        Combat.sound(p, ground, ModSounds.BLOOD_CHARGE, 3.0F, 0.6F);
        bleed(p, 3.0F);

        for (int tick = 10; tick < 110; tick += 3) {
            Scheduler.later(tick, () -> {
                for (int k = 0; k < 2; k++) {
                    Vec3 at = ground.add(level.random.nextGaussian() * 4.0, 0.0, level.random.nextGaussian() * 4.0);
                    Vfx.play(level, VfxKind.BLOOD_SPIKE, at, Vec3.ZERO, 1.0F, 13635632, -1, 0);

                    for (LivingEntity e : Combat.around(p, at.add(0.0, 1.0, 0.0), 1.6)) {
                        Combat.hurt(e, p, p, 4.0F * power);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), p);
                    }
                }

                if (level.random.nextInt(3) == 0) {
                    Combat.sound(p, ground, ModSounds.NAIL_HIT, 1.5F, 0.6F);
                }
            });
        }

        return true;
    }

    public static Vec3 groundBelow(Entity e, Vec3 at) {
        BlockHitResult hit = e.level().clip(new ClipContext(at.add(0.0, 2.0, 0.0), at.add(0.0, -24.0, 0.0), Block.COLLIDER, Fluid.NONE, e));
        return hit.getType() == Type.MISS ? at : hit.getLocation();
    }

    private BloodManipulation() {
    }
}
