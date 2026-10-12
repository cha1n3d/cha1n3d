package com.fallenroses.technique.impl;

import com.fallenroses.entity.FugaArrowEntity;
import com.fallenroses.entity.SlashEntity;
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
import net.minecraft.world.phys.Vec3;

public final class Shrine {
    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "dismantle" -> dismantle(p, power);
            case "cleave" -> cleave(p, power);
            case "spiderweb" -> spiderweb(p, power);
            case "fuga" -> fuga(p, power);
            case "malevolent_shrine" -> Domains.expand(p, d, 1, power);
            default -> false;
        };
    }

    private static boolean dismantle(ServerPlayer p, float power) {
        PlayerAnims.play(p, "dismantle");
        Combat.sound(p, p.position(), ModSounds.DISMANTLE, 1.4F, 0.9F + p.getRandom().nextFloat() * 0.2F);
        Vec3 look = p.getViewVector(1.0F);
        float roll = (float)(p.getRandom().nextBoolean() ? 1 : -1) * (10.0F + p.getRandom().nextFloat() * 30.0F);
        SlashEntity slash = new SlashEntity(ModEntities.SLASH.get(), p.level()).setup(roll, 3.4F);
        slash.launch(p, p.getEyePosition().add(look.scale(1.2)).add(0.0, -0.2, 0.0), look.scale(1.9), power);
        return true;
    }

    private static boolean cleave(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 6.0);
        if (target == null) {
            p.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            PlayerAnims.play(p, "cleave");
            float damage = 6.0F * power + Math.min(40.0F, target.getMaxHealth() * 0.2F) * Math.min(1.0F, power);
            Combat.hurt(target, p, p, damage);
            Vfx.play(
                p.serverLevel(),
                VfxKind.CLEAVE,
                target.getBoundingBox().getCenter(),
                Vec3.ZERO,
                Math.max(1.0F, target.getBbHeight()),
                16777215,
                target.getId(),
                0
            );
            Combat.sound(p, target.position(), ModSounds.CLEAVE, 1.6F, 1.0F);
            Combat.shake(p.serverLevel(), target.position(), 0.6F, 8, 10.0);
            return true;
        }
    }

    private static boolean spiderweb(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        PlayerAnims.play(p, "spiderweb");
        Vec3 c = p.position();
        Vfx.play(level, VfxKind.SPIDERWEB, c.add(0.0, 0.05, 0.0), Vec3.ZERO, 8.5F);
        Combat.sound(p, c, ModSounds.SPIDERWEB, 2.5F, 1.0F);
        Combat.shake(level, c, 1.2F, 16, 20.0);

        for (LivingEntity e : Combat.around(p, c, 8.5)) {
            if (!(Math.abs(e.getY() - c.y) > 3.0)) {
                Combat.hurt(e, p, p, 10.0F * power);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                Combat.knock(e, e.position().subtract(c), 0.4, 0.5);
            }
        }

        return true;
    }

    private static boolean fuga(ServerPlayer p, float power) {
        PlayerAnims.play(p, "fuga");
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 2, false, false));
        Vfx.play(p.serverLevel(), VfxKind.RED_CHARGE, Limitless.hand(p), p.getViewVector(1.0F), 1.4F, 16742944, p.getId(), 24);
        Combat.sound(p, p.position(), ModSounds.FUGA_CHARGE, 2.0F, 1.0F);
        p.displayClientMessage(Component.translatable("message.fallenroses.fuga_chant").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        Scheduler.later(24, () -> {
            if (p.isAlive()) {
                Vec3 look = p.getViewVector(1.0F);
                new FugaArrowEntity(ModEntities.FUGA_ARROW.get(), p.level()).launch(p, Limitless.hand(p), look.scale(1.8), power);
                Combat.sound(p, p.position(), ModSounds.FUGA_FIRE, 3.0F, 1.0F);
                Combat.shake(p.serverLevel(), p.position(), 0.8F, 10, 8.0);
            }
        });
        return true;
    }

    private Shrine() {
    }
}
