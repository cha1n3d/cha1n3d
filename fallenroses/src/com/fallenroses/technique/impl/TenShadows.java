package com.fallenroses.technique.impl;

import com.fallenroses.entity.curse.CurseEntity;
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
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
import net.minecraft.world.phys.Vec3;

public final class TenShadows {
    static final int SHADOW = 6978248;

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "divine_dogs" -> dogs(p, power);
            case "toad" -> toad(p, power);
            case "nue" -> nue(p, power);
            case "max_elephant" -> elephant(p, power);
            case "mahoraga" -> mahoraga(p, power);
            default -> false;
        };
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static boolean dogs(ServerPlayer p, float power) {
        PlayerAnims.play(p, "snap");
        ServerLevel level = p.serverLevel();
        Vec3 fwd = flatLook(p);
        Vec3 side = fwd.cross(new Vec3(0.0, 1.0, 0.0));
        Combat.sound(p, p.position(), ModSounds.SHADOW_SUMMON, 1.6F, 1.1F);

        for (int s = -1; s <= 1; s += 2) {
            Vec3 at = BloodManipulation.groundBelow(p, p.position().add(fwd.scale(1.6)).add(side.scale(1.4 * (double)s)).add(0.0, 1.0, 0.0));
            Vfx.play(level, VfxKind.SHADOW_POOL, at, Vec3.ZERO, 1.2F, 6978248, -1, 0);
            Scheduler.later(8, () -> {
                if (p.isAlive()) {
                    CurseManipulation.buff(CurseEntity.summon(ModEntities.DIVINE_DOG.get(), p, at, 800), power);
                }
            });
        }

        Scheduler.later(10, () -> Combat.sound(p, p.position(), ModSounds.DOG_HOWL, 1.6F, 1.0F));
        return true;
    }

    private static boolean toad(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 18.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "throw");
            ServerLevel level = p.serverLevel();
            Vec3 from = p.position().add(flatLook(p).scale(0.8)).add(0.0, 0.4, 0.0);
            Vfx.play(level, VfxKind.SHADOW_POOL, p.position().add(flatLook(p).scale(0.8)), Vec3.ZERO, 0.8F, 6978248, -1, 0);
            Vfx.play(level, VfxKind.TOAD_TONGUE, from, target.getBoundingBox().getCenter().subtract(from), 1.0F, 14183050, target.getId(), 0);
            Combat.sound(p, p.position(), ModSounds.KAPPA, 1.4F, 1.4F);
            Combat.hurt(target, p, p, 4.0F * power);
            Vec3 pull = p.position().subtract(target.position());
            double d = pull.length();
            Vec3 v = pull.normalize().scale(Math.min(2.2, d * 0.22));
            target.setDeltaMovement(v.x, 0.35 + Math.min(0.4, d * 0.02), v.z);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 25, 0), p);
            return true;
        }
    }

    private static boolean nue(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position().add(0.0, 2.5, 0.0).add(flatLook(p).scale(-1.0));
        Vfx.play(level, VfxKind.SHADOW_POOL, p.position(), Vec3.ZERO, 1.6F, 6978248, -1, 0);
        Combat.sound(p, p.position(), ModSounds.SHADOW_SUMMON, 1.8F, 0.9F);
        Scheduler.later(6, () -> {
            if (p.isAlive()) {
                CurseManipulation.buff(CurseEntity.summon(ModEntities.NUE.get(), p, at, 600), power);
                Combat.sound(p, at, ModSounds.NUE_SCREECH, 2.0F, 1.0F);
            }
        });
        return true;
    }

    private static boolean elephant(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        LivingEntity target = Combat.lookTarget(p, 22.0);
        Vec3 at = target != null ? target.position() : BloodManipulation.groundBelow(p, Combat.lookPoint(p, 18.0));
        Vfx.play(level, VfxKind.SHADOW_POOL, at.add(0.0, 7.0, 0.0), new Vec3(0.0, -1.0, 0.0), 2.6F, 6978248, -1, 1);
        Combat.sound(p, at, ModSounds.SHADOW_SUMMON, 2.2F, 0.6F);
        Scheduler.later(14, () -> {
            Vfx.play(level, VfxKind.FLOOD, at, Vec3.ZERO, 4.5F, 5945584, -1, 0);
            Combat.sound(p, at, ModSounds.WAVE, 3.0F, 0.7F);
            Combat.shake(level, at, 1.6F, 18, 24.0);

            for (LivingEntity e : Combat.around(p, at.add(0.0, 0.5, 0.0), 4.5)) {
                Combat.hurt(e, p, p, 9.0F * power);
                Combat.knock(e, e.position().subtract(at), 1.3, 0.2);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2), p);
                e.clearFire();
            }
        });
        return true;
    }

    private static boolean mahoraga(ServerPlayer p, float power) {
        PlayerAnims.play(p, "domain");
        ServerLevel level = p.serverLevel();
        Vec3 at = BloodManipulation.groundBelow(p, p.position().add(flatLook(p).scale(3.0)).add(0.0, 1.0, 0.0));
        p.displayClientMessage(Component.translatable("message.fallenroses.mahoraga").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, false, false));
        Vfx.play(level, VfxKind.SHADOW_POOL, at, Vec3.ZERO, 3.5F, 6978248, -1, 2);
        Combat.sound(p, p.position(), ModSounds.SHADOW_SUMMON, 4.0F, 0.5F);
        Scheduler.later(24, () -> {
            if (p.isAlive()) {
                CurseManipulation.buff(CurseEntity.summon(ModEntities.MAHORAGA.get(), p, at, 900), power);
                Vfx.play(level, VfxKind.ADAPT_WHEEL, at.add(0.0, 4.6, 0.0), Vec3.ZERO, 1.6F, 15788224, -1, 0);
                Combat.sound(p, at, ModSounds.WHEEL, 3.0F, 0.7F);
                Combat.flash(level, at, 15788224, 14, 0.7F, 40.0);
                Combat.shake(level, at, 2.0F, 26, 40.0);
            }
        });
        return true;
    }

    private TenShadows() {
    }
}
