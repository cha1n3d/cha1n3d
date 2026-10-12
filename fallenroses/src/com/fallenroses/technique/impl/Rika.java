package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
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

public final class Rika {
    static final int PINK = 15764160;
    public static final String MANIFEST = "fr_rika_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "rika_claw" -> claw(p, power);
            case "rika_grab" -> grab(p, power);
            case "rika_guard" -> guard(p);
            case "rika_manifest" -> manifest(p);
            case "pure_love" -> pureLove(p, power);
            default -> false;
        };
    }

    public static boolean manifested(ServerPlayer p) {
        return p.getPersistentData().getLong("fr_rika_until") > p.level().getGameTime();
    }

    private static boolean claw(ServerPlayer p, float power) {
        PlayerAnims.play(p, "sweep");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.WING_FAN, p.position().add(0.0, 1.4, 0.0), look, 5.5F, 15764160, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.CLEAVE, 1.4F, 0.7F);
        Combat.sound(p, p.position(), ModSounds.WOMB_WAIL, 0.8F, 1.4F);

        for (LivingEntity e : Combat.cone(p, 5.5, 0.45)) {
            Combat.hurt(e, p, p, 9.0F * power);
            Combat.knock(e, look, 0.9, 0.25);
        }

        return true;
    }

    private static boolean grab(ServerPlayer p, float power) {
        LivingEntity t = Combat.lookTarget(p, 14.0);
        if (t == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "touch");
            ServerLevel level = p.serverLevel();
            Vfx.play(level, VfxKind.TOAD_TONGUE, p.getEyePosition(), t.getBoundingBox().getCenter().subtract(p.getEyePosition()), 1.0F, 15764160, t.getId(), 0);
            t.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 20, 0), p);
            Vec3 pull = p.position().add(p.getViewVector(1.0F).multiply(2.0, 0.0, 2.0)).subtract(t.position());
            t.setDeltaMovement(pull.scale(0.35).add(0.0, 0.6, 0.0));
            t.hurtMarked = true;
            Scheduler.later(10, () -> {
                if (t.isAlive()) {
                    t.setDeltaMovement(0.0, -2.0, 0.0);
                    t.hurtMarked = true;
                    Combat.hurt(t, p, p, 11.0F * power);
                    Vfx.play(level, VfxKind.SLAM, t.position(), Vec3.ZERO, 2.5F);
                    Combat.sound(p, t.position(), ModSounds.SLAM, 1.4F, 0.9F);
                }
            });
            return true;
        }
    }

    private static boolean guard(ServerPlayer p) {
        PlayerAnims.play(p, "raise");
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1, false, true, true));
        Vfx.play(p.serverLevel(), VfxKind.REINFORCE, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.0F, 15764160, p.getId(), 0);

        for (LivingEntity e : Combat.around(p, p.position(), 4.0)) {
            Combat.knock(e, e.position().subtract(p.position()), 1.2, 0.4);
        }

        Combat.sound(p, p.position(), ModSounds.WOMB_WAIL, 1.2F, 0.9F);
        return true;
    }

    private static boolean manifest(ServerPlayer p) {
        PlayerAnims.play(p, "berserk");
        ServerLevel level = p.serverLevel();
        p.getPersistentData().putLong("fr_rika_until", level.getGameTime() + 300L);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1, false, true, true));
        Vfx.play(level, VfxKind.SUMMON, p.position(), Vec3.ZERO, 3.0F, 15764160, p.getId(), 0);
        Vfx.play(level, VfxKind.BERSERK, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 1.2F, 15764160, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.BOSS_ROAR, 2.0F, 1.3F);
        Combat.shake(level, p.position(), 1.2F, 16, 20.0);
        p.displayClientMessage(Component.translatable("message.fallenroses.rika_manifest").withStyle(s -> s.withColor(15764160).withBold(true)), true);
        return true;
    }

    public static void onHit(ServerPlayer p, PlayerData d, LivingEntity target) {
        Combat.hurt(target, p, p, 4.0F * d.power());
        Vfx.play(
            p.serverLevel(),
            VfxKind.SLASH_HIT,
            target.getBoundingBox().getCenter(),
            target.position().subtract(p.position()),
            1.2F,
            15764160,
            -1,
            p.getRandom().nextInt(180)
        );
    }

    private static boolean pureLove(ServerPlayer p, float power) {
        PlayerAnims.play(p, "purple");
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 32, 4, false, false));
        p.displayClientMessage(Component.translatable("message.fallenroses.pure_love").withStyle(s -> s.withColor(15764160).withBold(true)), true);
        Vfx.play(level, VfxKind.RED_CHARGE, Limitless.hand(p), Vec3.ZERO, 2.2F, 15764160, p.getId(), 30);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 2.4F, 1.1F);
        Combat.sound(p, p.position(), ModSounds.WOMB_WAIL, 2.0F, 0.8F);
        Scheduler.later(30, () -> {
            if (p.isAlive()) {
                Vec3 from = p.getEyePosition().add(0.0, -0.3, 0.0);
                Vec3 look = p.getViewVector(1.0F);
                BlockHitResult wall = level.clip(new ClipContext(from, from.add(look.scale(50.0)), Block.COLLIDER, Fluid.NONE, p));
                Vec3 end = wall.getType() == Type.MISS ? from.add(look.scale(50.0)) : wall.getLocation();

                for (LivingEntity e : BloodManipulation.along(p, from, end, 3.2)) {
                    Combat.hurt(e, p, p, 40.0F * power);
                    Combat.knock(e, look, 2.6, 0.7);
                }

                Vfx.play(level, VfxKind.GRANITE_BEAM, from, end.subtract(from), 3.2F, 15764160, -1, 0);
                Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 4.0F, 1.1F);
                Combat.shake(level, p.position(), 2.6F, 28, 44.0);
                Combat.flash(level, p.position(), 16769264, 12, 0.8F, 44.0);
            }
        });
        return true;
    }

    private Rika() {
    }
}
