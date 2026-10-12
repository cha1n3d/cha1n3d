package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEffects;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Lightning {
    static final int VOLT = 8048895;
    private static final String CHARGE = "fr_volt";
    private static final String CHARGE_UNTIL = "fr_volt_until";
    private static final String AMBER = "fr_amber_until";

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "charge_strike" -> strike(p, power);
            case "static_field" -> field(p, power);
            case "staff_rush" -> rush(p, power);
            case "discharge" -> discharge(p, power);
            case "mythical_beast_amber" -> amber(p);
            default -> false;
        };
    }

    static void charge(LivingEntity e, int n) {
        CompoundTag t = e.getPersistentData();
        long now = e.level().getGameTime();
        int cur = t.getLong("fr_volt_until") > now ? t.getInt("fr_volt") : 0;
        t.putInt("fr_volt", Math.min(5, cur + n));
        t.putLong("fr_volt_until", now + 300L);
    }

    static int charges(LivingEntity e) {
        CompoundTag t = e.getPersistentData();
        return t.getLong("fr_volt_until") > e.level().getGameTime() ? t.getInt("fr_volt") : 0;
    }

    static void arc(ServerPlayer p, Vec3 from, LivingEntity target, float damage) {
        Vec3 to = target.getBoundingBox().getCenter();
        Vfx.play(p.serverLevel(), VfxKind.ELECTRIC_ARC, from, to.subtract(from), 1.0F, 8048895, -1, 0);
        Combat.hurt(target, p, p, damage);
        target.addEffect(new MobEffectInstance(ModEffects.OVERLOAD.get(), 6, 0), p);
        charge(target, 1);
    }

    static void chain(ServerPlayer p, Vec3 from, LivingEntity first, float damage, int jumps) {
        Set<Integer> hit = new HashSet<>();
        LivingEntity cur = first;
        Vec3 at = from;
        float dmg = damage;

        for (int i = 0; i <= jumps && cur != null; i++) {
            hit.add(cur.getId());
            arc(p, at, cur, dmg);
            at = cur.getBoundingBox().getCenter();
            dmg *= 0.7F;
            LivingEntity next = null;
            double best = 7.0;

            for (LivingEntity e : Combat.around(p, at, 7.0)) {
                if (!hit.contains(e.getId())) {
                    double dd = e.getBoundingBox().getCenter().distanceTo(at);
                    if (dd < best) {
                        best = dd;
                        next = e;
                    }
                }
            }

            cur = next;
        }
    }

    private static boolean strike(ServerPlayer p, float power) {
        PlayerAnims.play(p, "snap");
        LivingEntity target = Combat.lookTarget(p, 18.0);
        Vec3 hand = Limitless.hand(p);
        Combat.sound(p, p.position(), ModSounds.THUNDER, 0.9F, 1.8F);
        if (target == null) {
            Vec3 at = Combat.lookPoint(p, 18.0);
            Vfx.play(p.serverLevel(), VfxKind.ELECTRIC_ARC, hand, at.subtract(hand), 1.0F, 8048895, -1, 0);
            return true;
        } else {
            chain(p, hand, target, 6.0F * power, 2);
            return true;
        }
    }

    private static boolean field(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        Vfx.play(level, VfxKind.STATIC_FIELD, p.position(), Vec3.ZERO, 7.0F, 8048895, p.getId(), 100);
        Combat.sound(p, p.position(), ModSounds.THUNDER, 1.0F, 1.4F);

        for (int i = 1; i <= 10; i++) {
            Scheduler.later(i * 10, () -> {
                if (p.isAlive()) {
                    List<LivingEntity> in = Combat.around(p, p.position().add(0.0, 1.0, 0.0), 7.0);
                    if (!in.isEmpty()) {
                        LivingEntity e = in.get(p.getRandom().nextInt(in.size()));
                        arc(p, p.position().add(0.0, 1.2, 0.0), e, 3.0F * power);
                        Combat.sound(p, e.position(), ModSounds.THUNDER, 0.6F, 2.0F);
                    }
                }
            });
        }

        return true;
    }

    private static boolean rush(ServerPlayer p, float power) {
        PlayerAnims.play(p, "dash");
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.3, look.y)), look.z).normalize();
        Vec3 start = p.position();
        Vec3 end = start;

        for (int i = 1; i <= 10; i++) {
            Vec3 next = start.add(dir.scale((double)i));
            if (!level.noCollision(p, p.getBoundingBox().move(next.subtract(p.position())))) {
                break;
            }

            end = next;
        }

        Vfx.play(level, VfxKind.DASH, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.0F);
        Vfx.play(level, VfxKind.ELECTRIC_ARC, start.add(0.0, 1.0, 0.0), end.subtract(start), 1.6F, 8048895, -1, 1);
        Combat.sound(p, start, ModSounds.THUNDER, 1.4F, 1.3F);
        Combat.sound(p, start, ModSounds.DASH, 1.0F, 1.2F);
        p.teleportTo(end.x, end.y, end.z);
        p.hurtMarked = true;
        Vec3 seg = end.subtract(start);
        double len = Math.max(0.1, seg.length());
        List<LivingEntity> hit = new ArrayList<>();

        for (LivingEntity e : Combat.around(p, start.add(seg.scale(0.5)).add(0.0, 1.0, 0.0), len / 2.0 + 2.0)) {
            Vec3 rel = e.position().subtract(start);
            double tt = Math.max(0.0, Math.min(len, rel.dot(seg) / len));
            Vec3 closest = start.add(seg.scale(tt / len));
            if (closest.distanceTo(e.position()) < 2.2) {
                hit.add(e);
            }
        }

        for (LivingEntity ex : hit) {
            Combat.hurt(ex, p, p, 8.0F * power);
            Combat.knock(ex, dir, 0.6, 0.35);
            charge(ex, 1);
        }

        return true;
    }

    private static boolean discharge(ServerPlayer p, float power) {
        PlayerAnims.play(p, "raise");
        ServerLevel level = p.serverLevel();
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity e : Combat.around(p, p.position(), 20.0)) {
            if (charges(e) > 0) {
                targets.add(e);
            }
        }

        if (targets.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.fallenroses.no_charged").withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            Combat.sound(p, p.position(), ModSounds.THUNDER, 2.0F, 0.7F);
            int i = 0;

            for (LivingEntity ex : targets) {
                int n = charges(ex);
                ex.getPersistentData().putInt("fr_volt", 0);
                Scheduler.later(2 + i++ * 3, () -> {
                    if (ex.isAlive()) {
                        Vfx.play(level, VfxKind.LIGHTNING, ex.position(), Vec3.ZERO, 1.0F, 8048895, -1, 0);
                        Combat.sound(p, ex.position(), ModSounds.THUNDER, 2.4F, 1.0F);
                        Combat.hurt(ex, p, p, (5.0F + 4.0F * (float)n) * power);
                        ex.addEffect(new MobEffectInstance(ModEffects.OVERLOAD.get(), 10 + n * 6, 0), p);
                    }
                });
            }

            Combat.flash(level, p.position(), 14742783, 6, 0.4F, 30.0);
            return true;
        }
    }

    private static boolean amber(ServerPlayer p) {
        PlayerAnims.play(p, "berserk");
        ServerLevel level = p.serverLevel();
        p.getPersistentData().putLong("fr_amber_until", level.getGameTime() + 400L);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 2, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 400, 1, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0, false, false, true));
        Vfx.play(level, VfxKind.AMBER_AURA, p.position(), Vec3.ZERO, 1.0F, 8048895, p.getId(), 400);
        Vfx.play(level, VfxKind.LIGHTNING, p.position(), Vec3.ZERO, 1.5F, 8048895, -1, 0);
        Combat.sound(p, p.position(), ModSounds.THUNDER, 3.0F, 0.6F);
        Combat.sound(p, p.position(), ModSounds.BERSERK, 1.5F, 1.3F);
        Combat.flash(level, p.position(), 14742783, 10, 0.7F, 32.0);
        Combat.shake(level, p.position(), 1.4F, 20, 24.0);
        p.displayClientMessage(Component.translatable("message.fallenroses.amber").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);

        for (LivingEntity e : Combat.around(p, p.position().add(0.0, 1.0, 0.0), 6.0)) {
            arc(p, p.position().add(0.0, 1.0, 0.0), e, 6.0F);
            Combat.knock(e, e.position().subtract(p.position()), 1.2, 0.4);
        }

        return true;
    }

    public static boolean inAmber(ServerPlayer p) {
        return p.getPersistentData().getLong("fr_amber_until") > p.level().getGameTime();
    }

    public static void onAmberHit(ServerPlayer p, PlayerData d, LivingEntity target) {
        if (p.getRandom().nextInt(2) == 0) {
            chain(p, p.getEyePosition().add(p.getViewVector(1.0F).scale(0.6)), target, 4.0F * d.power(), 2);
            Combat.sound(p, target.position(), ModSounds.THUNDER, 0.8F, 1.9F);
        }
    }

    public static void tick(ServerPlayer p, PlayerData d) {
        CompoundTag t = p.getPersistentData();
        long until = t.getLong("fr_amber_until");
        if (until != 0L) {
            long now = p.level().getGameTime();
            if (now < until) {
                if (p.tickCount % 3 == 0) {
                    p.serverLevel().sendParticles(ModParticles.spark(8048895, 0.5F, 6), p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.4, 0.8, 0.4, 0.1);
                }
            } else {
                t.remove("fr_amber_until");
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                p.hurt(p.damageSources().magic(), 4.0F);
                p.displayClientMessage(Component.translatable("message.fallenroses.amber_end").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
    }

    private Lightning() {
    }
}
