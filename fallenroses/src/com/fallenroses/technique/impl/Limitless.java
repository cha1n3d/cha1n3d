package com.fallenroses.technique.impl;

import com.fallenroses.entity.BlueOrbEntity;
import com.fallenroses.entity.HollowPurpleEntity;
import com.fallenroses.entity.RedBlastEntity;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class Limitless {
    private static final String STOPPED_TAG = "fallenroses_infinity_stopped";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "blue" -> blue(p, power);
            case "red" -> red(p, power);
            case "hollow_purple" -> purple(p, power);
            case "infinity" -> infinity(p, d);
            case "unlimited_void" -> Domains.expand(p, d, 0, power);
            default -> false;
        };
    }

    public static Vec3 hand(ServerPlayer p) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        return p.getEyePosition().add(look.scale(0.9)).add(right.scale(0.35)).add(0.0, -0.25, 0.0);
    }

    private static boolean blue(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 26.0);
        Vec3 at = target != null ? target.getBoundingBox().getCenter() : Combat.lookPoint(p, 14.0);
        PlayerAnims.play(p, "blue");
        Vfx.play(p.serverLevel(), VfxKind.BLUE_CAST, hand(p), p.getViewVector(1.0F), 1.0F);
        Combat.sound(p, p.position(), ModSounds.BLUE_CAST, 1.5F, 1.0F);
        BlueOrbEntity orb = new BlueOrbEntity(ModEntities.BLUE_ORB.get(), p.level());
        orb.setup(p, power);
        orb.setPos(at.x, at.y, at.z);
        p.level().addFreshEntity(orb);
        return true;
    }

    private static boolean red(ServerPlayer p, float power) {
        PlayerAnims.play(p, "red");
        Vfx.play(p.serverLevel(), VfxKind.RED_CHARGE, hand(p), p.getViewVector(1.0F), 1.0F, 16722462, p.getId(), 10);
        Combat.sound(p, p.position(), ModSounds.RED_CHARGE, 1.3F, 1.0F);
        Scheduler.later(10, () -> {
            if (p.isAlive()) {
                Vec3 look = p.getViewVector(1.0F);
                new RedBlastEntity(ModEntities.RED_BLAST.get(), p.level()).launch(p, hand(p), look.scale(2.4), power);
                Combat.sound(p, p.position(), ModSounds.RED_FIRE, 2.0F, 1.0F);
                Combat.shake(p.serverLevel(), p.position(), 0.6F, 8, 6.0);
                Combat.knock(p, look.scale(-1.0), 0.35, 0.05);
            }
        });
        return true;
    }

    private static boolean purple(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        PlayerAnims.play(p, "purple");
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34, 3, false, false));
        Vfx.play(level, VfxKind.PURPLE_CHARGE, p.position(), p.getViewVector(1.0F), 1.0F, 10112255, p.getId(), 30);
        Combat.sound(p, p.position(), ModSounds.PURPLE_CHARGE, 2.5F, 1.0F);
        p.displayClientMessage(Component.translatable("message.fallenroses.purple_chant").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        Scheduler.later(30, () -> {
            if (p.isAlive()) {
                Vec3 look = p.getViewVector(1.0F);
                new HollowPurpleEntity(ModEntities.HOLLOW_PURPLE.get(), p.level()).launch(p, p.getEyePosition().add(look.scale(2.5)), look.scale(1.15), power);
                Combat.sound(p, p.position(), ModSounds.PURPLE_FIRE, 6.0F, 1.0F);
                Combat.flash(level, p.position(), 13144319, 14, 0.85F, 48.0);
                Combat.shake(level, p.position(), 2.2F, 26, 40.0);
                Combat.knock(p, look.scale(-1.0), 0.8, 0.1);
            }
        });
        return true;
    }

    private static boolean infinity(ServerPlayer p, PlayerData d) {
        d.setInfinity(true);
        Combat.sound(p, p.position(), ModSounds.INFINITY_ON, 1.2F, 1.0F);
        Vfx.play(p.serverLevel(), VfxKind.INFINITY_RIPPLE, p.getBoundingBox().getCenter(), Vec3.ZERO, 2.5F, 12575999, p.getId(), 0);
        return true;
    }

    public static void tickInfinity(ServerPlayer p, PlayerData d, boolean creative) {
        float drain = 0.15F;
        if (!creative) {
            if (d.getEnergy() < drain) {
                d.setInfinity(false);
                p.displayClientMessage(Component.translatable("message.fallenroses.infinity_broken").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            d.setEnergy(d.getEnergy() - drain);
        }

        Vec3 c = p.getBoundingBox().getCenter();

        for (Projectile proj : p.level()
            .getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.0), e -> e.getOwner() != p && e.getDeltaMovement().lengthSqr() > 1.0E-4)) {
            Vec3 to = c.subtract(proj.position());
            if (!(to.dot(proj.getDeltaMovement()) <= 0.0) || proj.getPersistentData().getBoolean("fallenroses_infinity_stopped")) {
                proj.setDeltaMovement(proj.getDeltaMovement().scale(0.05));
                proj.hurtMarked = true;
                if (!proj.getPersistentData().getBoolean("fallenroses_infinity_stopped")) {
                    proj.getPersistentData().putBoolean("fallenroses_infinity_stopped", true);
                    Vfx.play(p.serverLevel(), VfxKind.INFINITY_RIPPLE, proj.position(), to, 0.8F, 12575999, -1, 0);
                    Combat.sound(p, proj.position(), ModSounds.INFINITY_BLOCK, 0.8F, 1.2F);
                }
            }
        }
    }

    private Limitless() {
    }
}
