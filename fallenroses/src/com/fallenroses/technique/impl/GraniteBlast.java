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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class GraniteBlast {
    static final int GRANITE = 15786160;
    private static final String OVERFLOW = "fr_overflow_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        if (inOverflow(p)) {
            power *= 1.25F;
        }
        return switch (id) {
            case "granite_shot" -> shot(p, power);
            case "discharge_barrage" -> barrage(p, power);
            case "output_ram" -> ram(p, power);
            case "overflow" -> overflow(p);
            case "granite_cannon" -> cannon(p, power);
            default -> false;
        };
    }

    public static boolean inOverflow(ServerPlayer p) {
        return p.getPersistentData().getLong("fr_overflow_until") > p.level().getGameTime();
    }

    private static boolean shot(ServerPlayer p, float power) {
        PlayerAnims.play(p, "blue");
        new ShotEntity(ModEntities.SHOT.get(), p.level())
            .kind(ShotEntity.Kind.ENERGY_SHOT)
            .launch(p, Limitless.hand(p), p.getViewVector(1.0F).scale(1.8), power);
        Combat.sound(p, p.position(), ModSounds.RED_FIRE, 1.0F, 1.5F);
        return true;
    }

    private static boolean barrage(ServerPlayer p, float power) {
        PlayerAnims.play(p, "flurry");

        for (int i = 0; i < 8; i++) {
            Scheduler.later(
                i * 2,
                () -> {
                    if (p.isAlive()) {
                        Vec3 look = p.getViewVector(1.0F)
                            .add(p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.05, p.getRandom().nextGaussian() * 0.08);
                        new ShotEntity(ModEntities.SHOT.get(), p.level())
                            .kind(ShotEntity.Kind.ENERGY_SHOT)
                            .launch(p, Limitless.hand(p), look.normalize().scale(1.9), power * 0.7F);
                        Combat.sound(p, p.position(), ModSounds.RED_FIRE, 0.7F, 1.7F + p.getRandom().nextFloat() * 0.2F);
                    }
                }
            );
        }

        return true;
    }

    private static boolean ram(ServerPlayer p, float power) {
        PlayerAnims.play(p, "dash");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.3, look.y)), look.z).normalize();
        Vec3 start = p.position();
        Vec3 end = Projection.safeEnd(p, dir, 10.0);
        Set<Integer> hit = new HashSet<>();

        for (LivingEntity e : BloodManipulation.along(p, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), 1.6)) {
            if (hit.add(e.getId())) {
                Combat.hurt(e, p, p, 9.0F * power);
                Combat.knock(e, dir, 2.0, 0.5);
            }
        }

        Vfx.play(level, VfxKind.DASH, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.0F);
        Vfx.play(level, VfxKind.MASS_IMPACT, end.add(0.0, 1.0, 0.0), dir, 1.4F, 15786160, -1, 0);
        p.teleportTo(end.x, end.y, end.z);
        p.fallDistance = 0.0F;
        Combat.sound(p, end, ModSounds.SLAM, 1.6F, 0.9F);
        return true;
    }

    private static boolean overflow(ServerPlayer p) {
        PlayerAnims.play(p, "raise");
        p.getPersistentData().putLong("fr_overflow_until", p.level().getGameTime() + 200L);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, false, true, true));
        Vfx.play(p.serverLevel(), VfxKind.REINFORCE, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.0F, 15786160, p.getId(), 0);
        Vfx.play(p.serverLevel(), VfxKind.LEVEL_UP, p.position(), Vec3.ZERO, 1.0F, 16777215, p.getId(), 15786160);
        Combat.sound(p, p.position(), ModSounds.REINFORCE, 1.6F, 0.6F);
        p.displayClientMessage(Component.translatable("message.fallenroses.overflow").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static boolean cannon(ServerPlayer p, float power) {
        PlayerAnims.play(p, "purple");
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 4, false, false));
        Vfx.play(level, VfxKind.RED_CHARGE, Limitless.hand(p), Vec3.ZERO, 2.0F, 15786160, p.getId(), 20);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 2.0F, 1.3F);
        p.displayClientMessage(Component.translatable("message.fallenroses.granite_blast").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), true);
        Scheduler.later(20, () -> {
            if (p.isAlive()) {
                Vec3 from = p.getEyePosition().add(0.0, -0.3, 0.0);
                Vec3 look = p.getViewVector(1.0F);
                BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(40.0)), Block.COLLIDER, Fluid.NONE, p));
                Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(40.0)) : wall.getLocation();

                for (LivingEntity e : BloodManipulation.along(p, from, end, 2.6)) {
                    Combat.hurt(e, p, p, 34.0F * power);
                    Combat.knock(e, look, 2.4, 0.6);
                }

                Vfx.play(level, VfxKind.GRANITE_BEAM, from, end.subtract(from), 2.6F, 15786160, -1, 0);
                Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 4.0F, 0.8F);
                Combat.shake(level, p.position(), 2.4F, 26, 40.0);
                Combat.flash(level, p.position(), 16775392, 10, 0.7F, 40.0);
            }
        });
        return true;
    }

    public static void tick(ServerPlayer p, PlayerData d) {
        if (p.tickCount % 10 == 0 && inOverflow(p)) {
            d.regen(d.getMax() * 0.02F);
        }
    }

    private GraniteBlast() {
    }
}
