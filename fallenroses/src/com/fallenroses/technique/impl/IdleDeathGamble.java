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
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class IdleDeathGamble {
    static final int GREEN = 7004282;
    private static final String FEVER = "fr_fever";
    private static final String JACKPOT = "fr_jackpot_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "rough_energy" -> rough(p, power);
            case "shutter_doors" -> doors(p, power);
            case "pachinko_balls" -> balls(p, power);
            case "reach" -> reach(p, d);
            case "jackpot" -> jackpot(p, d);
            default -> false;
        };
    }

    static int fever(ServerPlayer p) {
        return p.getPersistentData().getInt("fr_fever");
    }

    static void addFever(ServerPlayer p, int n) {
        CompoundTag t = p.getPersistentData();
        t.putInt("fr_fever", Math.min(10, t.getInt("fr_fever") + n));
    }

    public static boolean inJackpot(ServerPlayer p) {
        return p.getPersistentData().getLong("fr_jackpot_until") > p.level().getGameTime();
    }

    private static boolean rough(ServerPlayer p, float power) {
        PlayerAnims.play(p, "punch");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vfx.play(level, VfxKind.ROUGH_BLAST, Limitless.hand(p), look, 1.0F, 7004282, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.CURSED_HIT, 1.4F, 0.7F);
        int hits = 0;

        for (LivingEntity e : Combat.cone(p, 8.0, 0.86)) {
            Combat.hurt(e, p, p, 6.0F * power);
            Combat.knock(e, look, 1.1, 0.25);
            hits++;
        }

        if (hits > 0) {
            addFever(p, 1);
        }

        return true;
    }

    private static boolean doors(ServerPlayer p, float power) {
        LivingEntity target = Combat.lookTarget(p, 14.0);
        if (target == null) {
            return Ratio.noTarget(p);
        } else {
            PlayerAnims.play(p, "clap");
            ServerLevel level = p.serverLevel();
            Vec3 side = p.getViewVector(1.0F).cross(new Vec3(0.0, 1.0, 0.0));
            Vfx.play(level, VfxKind.SHUTTER_DOORS, target.position(), side, Math.max(1.2F, target.getBbHeight()), 7004282, target.getId(), 0);
            Combat.sound(p, target.position(), ModSounds.FRAME, 1.4F, 0.6F);
            Scheduler.later(7, () -> {
                if (target.isAlive()) {
                    Vfx.play(level, VfxKind.SHUTTER_DOORS, target.position(), side, Math.max(1.2F, target.getBbHeight()), 7004282, target.getId(), 1);
                    Combat.sound(p, target.position(), ModSounds.SLAM, 1.6F, 1.4F);
                    Combat.shake(level, target.position(), 0.6F, 8, 14.0);
                    Combat.hurt(target, p, p, 10.0F * power);
                    target.addEffect(new MobEffectInstance(ModEffects.BOUND.get(), 30, 0), p);
                    addFever(p, 1);
                }
            });
            return true;
        }
    }

    private static boolean balls(ServerPlayer p, float power) {
        PlayerAnims.play(p, "throw");
        Vec3 hand = Limitless.hand(p);
        Vec3 look = p.getViewVector(1.0F);

        for (int i = 0; i < 10; i++) {
            Vec3 v = look.add(p.getRandom().nextGaussian() * 0.12, p.getRandom().nextGaussian() * 0.08 + 0.05, p.getRandom().nextGaussian() * 0.12)
                .normalize()
                .scale(1.5 + p.getRandom().nextDouble() * 0.4);
            new ShotEntity(ModEntities.SHOT.get(), p.level()).kind(ShotEntity.Kind.PACHINKO_BALL).launch(p, hand, v, power);
        }

        Combat.sound(p, p.position(), ModSounds.HAMMER, 1.0F, 1.8F);
        Vfx.play(p.serverLevel(), VfxKind.PACHINKO_REELS, p.getEyePosition().add(0.0, 0.6, 0.0), Vec3.ZERO, 0.6F, 7004282, p.getId(), -1);
        return true;
    }

    private static boolean reach(ServerPlayer p, PlayerData d) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        RandomSource r = p.getRandom();
        int boost = fever(p);
        int a = 1 + r.nextInt(7);
        int b = r.nextInt(10) < 3 + boost / 2 ? a : 1 + r.nextInt(7);
        int c = r.nextInt(10) < 2 + boost / 3 ? b : 1 + r.nextInt(7);
        int code = a * 100 + b * 10 + c;
        Vfx.play(level, VfxKind.PACHINKO_REELS, p.getEyePosition().add(0.0, 1.2, 0.0), Vec3.ZERO, 1.0F, 7004282, p.getId(), code);
        Combat.sound(p, p.position(), ModSounds.FRAME, 1.0F, 1.6F);
        Scheduler.later(
            36,
            () -> {
                if (p.isAlive()) {
                    if (a == b && b == c) {
                        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, true, true));
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1, false, true, true));
                        d.regen(d.getMax() * 0.5F);
                        p.displayClientMessage(
                            Component.translatable("message.fallenroses.reach_triple", a).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true
                        );
                        Combat.sound(p, p.position(), ModSounds.LEVEL_UP, 1.2F, 1.4F);
                        Vfx.at(p, VfxKind.LEVEL_UP, 1.0F, 7004282);
                    } else if (a != b && b != c && a != c) {
                        addFever(p, 2);
                        p.displayClientMessage(Component.translatable("message.fallenroses.reach_miss").withStyle(ChatFormatting.GRAY), true);
                    } else {
                        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0, false, true, true));
                        addFever(p, 3);
                        p.displayClientMessage(Component.translatable("message.fallenroses.reach_pair").withStyle(ChatFormatting.GREEN), true);
                        Combat.sound(p, p.position(), ModSounds.SKILL_UNLOCK, 1.0F, 1.2F);
                    }
                }
            }
        );
        return true;
    }

    public static boolean rollDomain(ServerPlayer p) {
        int fever = fever(p);
        float chance = Math.min(0.6F, 0.25F + (float)fever * 0.04F);
        boolean win = p.getRandom().nextFloat() < chance;
        int n = win ? 7 : 1 + p.getRandom().nextInt(7);
        int code = win ? 777 : n * 100 + (n % 7 + 1) * 10 + (n + 2) % 7 + 1;
        Vfx.play(p.serverLevel(), VfxKind.PACHINKO_REELS, p.getEyePosition().add(0.0, 1.4, 0.0), Vec3.ZERO, 1.6F, 7004282, p.getId(), code + 1000);
        p.displayClientMessage(Component.translatable("message.fallenroses.jackpot_spin", Math.round(chance * 100.0F)).withStyle(ChatFormatting.GREEN), true);

        for (int i = 0; i < 6; i++) {
            Scheduler.later(i * 9, () -> Combat.sound(p, p.position(), ModSounds.FRAME, 0.8F, 1.4F + p.getRandom().nextFloat() * 0.4F));
        }

        if (!win) {
            addFever(p, 2);
        }

        return win;
    }

    public static void win(ServerPlayer p, PlayerData d) {
        ServerLevel level = p.serverLevel();
        p.getPersistentData().putInt("fr_fever", 0);
        p.getPersistentData().putLong("fr_jackpot_until", level.getGameTime() + 600L);
        d.setEnergy(d.getMax());
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2, false, true, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, true, true));
        Vfx.play(level, VfxKind.JACKPOT, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 3.0F, 7004282, p.getId(), 600);
        Combat.sound(p, p.position(), ModSounds.LEVEL_UP, 2.0F, 0.9F);
        Combat.sound(p, p.position(), ModSounds.DOMAIN_EXPAND, 1.4F, 1.6F);
        Combat.flash(level, p.position(), 13172672, 12, 0.6F, 24.0);
        p.displayClientMessage(Component.translatable("message.fallenroses.jackpot_win").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        Combat.shake(level, p.position(), 1.0F, 16, 20.0);
    }

    private static boolean jackpot(ServerPlayer p, PlayerData d) {
        return Domains.expand(p, d, 6, d.power(4));
    }

    public static void tick(ServerPlayer p, PlayerData d) {
        if (inJackpot(p)) {
            d.setEnergy(d.getMax());
            if (p.tickCount % 10 == 0 && p.getHealth() < p.getMaxHealth()) {
                p.heal(1.5F);
            }

            if (p.tickCount % 40 == 0) {
                Vfx.at(p, VfxKind.RCT_HEAL);
            }
        }
    }

    private IdleDeathGamble() {
    }
}
