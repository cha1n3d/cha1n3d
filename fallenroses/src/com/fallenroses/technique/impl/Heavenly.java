package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Stat;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class Heavenly {
    private static final Map<UUID, Heavenly.State> STATES = new HashMap<>();

    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "dash" -> dash(p, power);
            case "ground_slam" -> slam(p, power);
            case "flurry" -> flurry(p, d, power);
            case "killer_instinct" -> instinct(p, d);
            case "berserk" -> berserk(p, d);
            default -> false;
        };
    }

    private static Heavenly.State state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), u -> new Heavenly.State());
    }

    private static boolean dash(ServerPlayer p, float power) {
        Vec3 look = p.getViewVector(1.0F);
        Vec3 dir = new Vec3(look.x, Math.max(-0.2, Math.min(0.25, look.y)), look.z).normalize();
        Vec3 from = p.position();
        p.setDeltaMovement(dir.scale(2.6).add(0.0, 0.12, 0.0));
        p.hurtMarked = true;
        p.invulnerableTime = 12;
        p.fallDistance = 0.0F;
        Heavenly.State s = state(p);
        s.dashTicks = 6;
        s.dashHits.clear();
        s.slamPower = power;
        PlayerAnims.play(p, "dash");
        Vfx.play(p.serverLevel(), VfxKind.DASH, from.add(0.0, 1.0, 0.0), dir.scale(8.0), 1.0F, 15259808, p.getId(), 0);
        Combat.sound(p, from, ModSounds.DASH, 1.4F, 1.0F);
        return true;
    }

    private static boolean slam(ServerPlayer p, float power) {
        Heavenly.State s = state(p);
        if (p.onGround()) {
            Vec3 look = p.getViewVector(1.0F);
            p.setDeltaMovement(look.x * 0.6, 1.15, look.z * 0.6);
        } else {
            p.setDeltaMovement(0.0, -2.6, 0.0);
        }

        p.hurtMarked = true;
        s.slamTicks = 50;
        s.slamPower = power;
        PlayerAnims.play(p, "slam");
        Combat.sound(p, p.position(), ModSounds.DASH, 1.0F, 0.6F);
        return true;
    }

    private static boolean flurry(ServerPlayer p, PlayerData d, float power) {
        LivingEntity target = Combat.lookTarget(p, 4.5);
        if (target == null) {
            p.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            PlayerAnims.play(p, "flurry");
            float perHit = 1.6F * power + (float)d.getStat(Stat.STRENGTH) * 0.2F;

            for (int i = 0; i < 8; i++) {
                boolean last = i == 7;
                Scheduler.later(
                    1 + i * 2,
                    () -> {
                        if (p.isAlive() && target.isAlive() && !(p.distanceTo(target) > 6.0F)) {
                            Combat.hurt(target, p, p, last ? perHit * 2.5F : perHit);
                            Vec3 at = target.getBoundingBox()
                                .getCenter()
                                .add(
                                    (p.getRandom().nextDouble() - 0.5) * 0.6,
                                    (p.getRandom().nextDouble() - 0.5) * 0.8,
                                    (p.getRandom().nextDouble() - 0.5) * 0.6
                                );
                            Vfx.play(p.serverLevel(), VfxKind.FLURRY_HIT, at, target.position().subtract(p.position()), last ? 2.0F : 1.0F);
                            Combat.sound(p, at, ModSounds.FLURRY, last ? 1.6F : 1.0F, last ? 0.7F : 0.9F + p.getRandom().nextFloat() * 0.3F);
                            if (last) {
                                Combat.knock(target, target.position().subtract(p.position()), 1.8, 0.5);
                                Combat.shake(p.serverLevel(), at, 0.9F, 10, 10.0);
                            }
                        }
                    }
                );
            }

            return true;
        }
    }

    private static boolean instinct(ServerPlayer p, PlayerData d) {
        for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32.0), LivingEntity::isAlive)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
        }

        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        d.instinctTicks = 200;
        Vfx.at(p, VfxKind.INSTINCT);
        Combat.sound(p, p.position(), ModSounds.INSTINCT, 1.5F, 1.0F);
        return true;
    }

    private static boolean berserk(ServerPlayer p, PlayerData d) {
        d.berserkTicks = 300;
        d.markUrgent();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
        PlayerAnims.play(p, "berserk");
        Vfx.at(p, VfxKind.BERSERK, 2.0F, 0);
        Combat.sound(p, p.position(), ModSounds.BERSERK, 3.0F, 1.0F);
        Combat.flash(p.serverLevel(), p.position(), 9043984, 12, 0.6F, 24.0);
        Combat.shake(p.serverLevel(), p.position(), 1.5F, 20, 24.0);
        return true;
    }

    public static void tick(ServerPlayer p, PlayerData d) {
        Heavenly.State s = STATES.get(p.getUUID());
        if (d.berserkTicks > 0 && p.tickCount % 10 == 0) {
            Vfx.at(p, VfxKind.BERSERK, 0.6F, 0);
        }

        if (s != null) {
            ServerLevel level = p.serverLevel();
            if (s.dashTicks > 0) {
                s.dashTicks--;
                p.fallDistance = 0.0F;

                for (LivingEntity e : Combat.around(p, p.getBoundingBox().getCenter(), 1.8)) {
                    if (s.dashHits.add(e.getId())) {
                        Combat.hurt(e, p, p, 5.0F * s.slamPower);
                        Combat.knock(e, p.getDeltaMovement(), 0.8, 0.3);
                        Vfx.play(level, VfxKind.FLURRY_HIT, e.getBoundingBox().getCenter(), p.getDeltaMovement(), 1.2F);
                    }
                }
            }

            if (s.slamTicks > 0) {
                s.slamTicks--;
                p.fallDistance = 0.0F;
                if (s.slamTicks < 44 && p.onGround()) {
                    s.slamTicks = 0;
                    Vec3 c = p.position();
                    Vfx.play(level, VfxKind.SLAM, c.add(0.0, 0.1, 0.0), Vec3.ZERO, 5.5F, 15259808, p.getId(), 0);
                    Combat.sound(p, c, ModSounds.SLAM, 3.0F, 1.0F);
                    Combat.shake(level, c, 2.0F, 18, 24.0);

                    for (LivingEntity ex : Combat.around(p, c, 5.5)) {
                        Combat.hurt(ex, p, p, 9.0F * s.slamPower + p.getMaxHealth() * 0.05F);
                        ex.push(0.0, 0.85, 0.0);
                        ex.hurtMarked = true;
                    }
                }
            }
        }
    }

    public static boolean ignoresFall(ServerPlayer p) {
        Heavenly.State s = STATES.get(p.getUUID());
        return s != null && (s.slamTicks > 0 || s.dashTicks > 0);
    }

    public static void forget(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }

    private Heavenly() {
    }

    private static final class State {
        int dashTicks;
        final Set<Integer> dashHits = new HashSet<>();
        int slamTicks;
        float slamPower;
    }
}
