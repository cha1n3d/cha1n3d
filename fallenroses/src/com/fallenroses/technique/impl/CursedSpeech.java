package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class CursedSpeech {
    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "dont_move" -> dontMove(p, power);
            case "blast_away" -> blastAway(p, power);
            case "twist" -> twist(p, power);
            case "explode" -> explode(p, power);
            case "vanish" -> vanish(p, power);
            default -> false;
        };
    }

    private static void speak(ServerPlayer p, String id, float scale, float throat) {
        ServerLevel level = p.serverLevel();
        PlayerAnims.play(p, "speech");
        Component word = Component.translatable("speech.fallenroses." + id).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);

        for (ServerPlayer other : level.players()) {
            if (other.distanceTo(p) < 24.0F) {
                other.displayClientMessage(word, true);
            }
        }

        Vfx.play(level, VfxKind.SPEECH_WAVE, p.getEyePosition().add(0.0, -0.15, 0.0), p.getViewVector(1.0F), scale, 11894015, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.SPEECH, 2.0F, 0.85F + p.getRandom().nextFloat() * 0.15F);
        if (!p.getAbilities().instabuild && p.getHealth() > throat + 1.0F) {
            p.hurt(p.damageSources().magic(), throat);
        }
    }

    private static boolean dontMove(ServerPlayer p, float power) {
        List<LivingEntity> targets = Combat.cone(p, 14.0, 0.5);
        speak(p, "dont_move", 1.0F, 1.0F);
        int duration = Math.round(60.0F * Math.min(2.0F, power));

        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), duration, 0), p);
            if (e instanceof Mob mob) {
                mob.getNavigation().stop();
            }

            e.setDeltaMovement(0.0, Math.min(0.0, e.getDeltaMovement().y), 0.0);
            e.hurtMarked = true;
            Vfx.play(p.serverLevel(), VfxKind.SPEECH_SEAL, e.getBoundingBox().getCenter(), Vec3.ZERO, e.getBbHeight(), 11894015, e.getId(), duration);
        }

        return true;
    }

    private static boolean blastAway(ServerPlayer p, float power) {
        List<LivingEntity> targets = Combat.cone(p, 12.0, 0.55);
        speak(p, "blast_away", 1.6F, 1.0F);
        Combat.sound(p, p.position(), ModSounds.SPEECH_EXPLODE, 1.5F, 1.4F);

        for (LivingEntity e : targets) {
            Combat.hurt(e, p, p, 6.0F * power);
            Combat.knock(e, e.position().subtract(p.position()), 2.4, 0.65);
        }

        Combat.shake(p.serverLevel(), p.position(), 0.8F, 10, 14.0);
        return true;
    }

    private static boolean twist(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 16.0);
        if (target == null) {
            return noTarget(p);
        } else {
            speak(p, "twist", 1.0F, 2.0F);
            Combat.hurt(target, p, p, 14.0F * power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), p);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2), p);
            Vfx.play(p.serverLevel(), VfxKind.TWIST, target.getBoundingBox().getCenter(), Vec3.ZERO, target.getBbHeight(), 11894015, target.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.SPEECH_TWIST, 1.5F, 1.0F);
            return true;
        }
    }

    private static boolean explode(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 18.0);
        if (target == null) {
            return noTarget(p);
        } else {
            speak(p, "explode", 1.2F, 3.0F);
            ServerLevel level = p.serverLevel();
            Vec3 c = target.getBoundingBox().getCenter();
            Combat.hurt(target, p, p, 18.0F * power);

            for (LivingEntity e : Combat.around(p, c, 3.5)) {
                if (e != target) {
                    Combat.hurt(e, p, p, 8.0F * power);
                }

                Combat.knock(e, e.position().subtract(c), 1.4, 0.6);
            }

            Vfx.play(level, VfxKind.EXPLODE, c, Vec3.ZERO, 3.5F, 11894015, -1, 0);
            Combat.sound(p, c, ModSounds.SPEECH_EXPLODE, 2.5F, 0.9F);
            Combat.shake(level, c, 1.2F, 14, 20.0);
            return true;
        }
    }

    private static boolean vanish(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        speak(p, "vanish", 3.0F, 0.0F);
        Vec3 c = p.position();

        for (LivingEntity e : Combat.around(p, c, 14.0)) {
            if (p.hasLineOfSight(e)) {
                Combat.hurt(e, p, p, 30.0F * power);
                Combat.knock(e, e.position().subtract(c), 3.0, 0.9);
            }
        }

        Vfx.play(level, VfxKind.VANISH, c.add(0.0, 1.0, 0.0), Vec3.ZERO, 14.0F, 11894015, p.getId(), 0);
        Combat.sound(p, c, ModSounds.VANISH, 5.0F, 1.0F);
        Combat.flash(level, c, 2752576, 18, 0.8F, 40.0);
        Combat.shake(level, c, 2.5F, 30, 40.0);
        if (!p.getAbilities().instabuild) {
            float cost = p.getMaxHealth() * 0.4F;
            p.hurt(p.damageSources().magic(), Math.min(cost, p.getHealth() - 1.0F));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        }

        return true;
    }

    private static boolean noTarget(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    private CursedSpeech() {
    }
}
