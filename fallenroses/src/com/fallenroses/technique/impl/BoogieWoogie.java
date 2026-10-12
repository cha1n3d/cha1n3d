package com.fallenroses.technique.impl;

import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Combat;
import com.fallenroses.util.PlayerAnims;
import com.fallenroses.util.Scheduler;
import com.fallenroses.vfx.Vfx;
import com.fallenroses.vfx.VfxKind;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;

public final class BoogieWoogie {
    public static boolean cast(ServerPlayer p, PlayerData d, String id, float power) {
        return switch (id) {
            case "swap" -> swap(p);
            case "feint" -> feint(p);
            case "clap_barrage" -> barrage(p, power);
            case "brother_strike" -> brotherStrike(p, d);
            case "chaos_clap" -> chaos(p, power);
            default -> false;
        };
    }

    private static boolean swap(ServerPlayer p) {
        LivingEntity target = Combat.lookTarget(p, 32.0);
        if (target == null) {
            return noTarget(p);
        } else {
            clap(p);
            swapPositions(p, p, target);
            if (target instanceof Mob mob) {
                mob.getNavigation().stop();
            }

            return true;
        }
    }

    private static boolean feint(ServerPlayer p) {
        List<Mob> mobs = hostiles(p, 10.0);
        if (mobs.isEmpty()) {
            return noTarget(p);
        } else {
            clap(p);
            confuse(p, mobs);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            return true;
        }
    }

    private static boolean barrage(ServerPlayer p, float power) {
        List<Mob> mobs = hostiles(p, 16.0);
        if (mobs.isEmpty()) {
            return noTarget(p);
        } else {
            mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(p)));
            int n = Math.min(5, mobs.size());

            for (int i = 0; i < n; i++) {
                Mob mob = mobs.get(i);
                Scheduler.later(1 + i * 4, () -> {
                    if (p.isAlive() && mob.isAlive()) {
                        clap(p);
                        swapPositions(p, p, mob);
                        Combat.hurt(mob, p, p, 5.0F * power);
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    }
                });
            }

            return true;
        }
    }

    private static boolean brotherStrike(ServerPlayer p, PlayerData d) {
        LivingEntity target = Combat.lookTarget(p, 20.0);
        if (target == null) {
            return noTarget(p);
        } else {
            clap(p);
            Vec3 from = p.position();
            Vec3 facing = Vec3.directionFromRotation(0.0F, target.getYRot());
            Vec3 behind = target.position().subtract(facing.scale(1.6));
            float yaw = (float)(Math.toDegrees(Math.atan2(target.getZ() - behind.z, target.getX() - behind.x)) - 90.0);
            p.connection.teleport(behind.x, target.getY(), behind.z, yaw, 10.0F);
            p.fallDistance = 0.0F;
            Vfx.play(p.serverLevel(), VfxKind.SWAP, from.add(0.0, 1.0, 0.0), behind.subtract(from), 1.0F, 16757575, -1, 0);
            d.strikeReadyTicks = 60;
            d.markUrgent();
            p.displayClientMessage(Component.translatable("message.fallenroses.strike_ready").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            return true;
        }
    }

    private static boolean chaos(ServerPlayer p, float power) {
        ServerLevel level = p.serverLevel();
        List<Mob> mobs = hostiles(p, 16.0);
        if (mobs.size() < 2) {
            return noTarget(p);
        } else {
            if (mobs.size() > 12) {
                mobs = new ArrayList<>(mobs.subList(0, 12));
            }

            List<Mob> group = mobs;
            PlayerAnims.play(p, "clap");
            Vfx.play(level, VfxKind.CHAOS_CLAP, p.position().add(0.0, 1.0, 0.0), Vec3.ZERO, 16.0F, 16757575, p.getId(), 0);
            Combat.sound(p, p.position(), ModSounds.CHAOS, 3.0F, 1.0F);
            Combat.flash(level, p.position(), 16769184, 8, 0.5F, 30.0);

            for (int round = 0; round < 6; round++) {
                Scheduler.later(4 + round * 4, () -> {
                    List<Mob> alive = group.stream().filter(Entity::isAlive).toList();
                    if (alive.size() >= 2) {
                        List<Vec3> spots = alive.stream().map(Entity::position).toList();

                        for (int i = 0; i < alive.size(); i++) {
                            Mob m = alive.get(i);
                            Vec3 to = spots.get((i + 1) % spots.size());
                            Vfx.play(level, VfxKind.SWAP, m.position().add(0.0, 1.0, 0.0), to.subtract(m.position()), 0.6F, 16757575, -1, 0);
                            m.teleportTo(to.x, to.y, to.z);
                            Combat.hurt(m, p, p, 3.0F * power);
                        }

                        Combat.sound(p, p.position(), ModSounds.CLAP, 2.0F, 1.2F + level.random.nextFloat() * 0.4F);
                        confuse(p, alive);
                    }
                });
            }

            return true;
        }
    }

    private static void clap(ServerPlayer p) {
        PlayerAnims.play(p, "clap");
        Vfx.play(p.serverLevel(), VfxKind.CLAP, Limitless.hand(p), p.getViewVector(1.0F), 1.0F, 16757575, p.getId(), 0);
        Combat.sound(p, p.position(), ModSounds.CLAP, 1.6F, 0.95F + p.getRandom().nextFloat() * 0.1F);
    }

    private static void swapPositions(ServerPlayer caster, LivingEntity a, LivingEntity b) {
        Vec3 pa = a.position();
        Vec3 pb = b.position();
        teleport(a, pb);
        teleport(b, pa);
        a.fallDistance = 0.0F;
        b.fallDistance = 0.0F;
        Vfx.play(caster.serverLevel(), VfxKind.SWAP, pa.add(0.0, 1.0, 0.0), pb.subtract(pa), 1.0F, 16757575, -1, 0);
        Combat.sound(caster, pb, ModSounds.SWAP, 1.2F, 1.0F);
    }

    private static void teleport(LivingEntity e, Vec3 to) {
        if (e instanceof ServerPlayer sp) {
            sp.connection.teleport(to.x, to.y, to.z, sp.getYRot(), sp.getXRot());
        } else {
            e.teleportTo(to.x, to.y, to.z);
        }
    }

    private static List<Mob> hostiles(ServerPlayer p, double radius) {
        return new ArrayList<>(
            p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), m -> m.isAlive() && (m.getTarget() == p || m instanceof Enemy))
        );
    }

    private static void confuse(ServerPlayer p, List<Mob> mobs) {
        for (Mob mob : mobs) {
            Mob other = null;
            double best = Double.MAX_VALUE;

            for (Mob m : mobs) {
                double dist = m.distanceToSqr(mob);
                if (m != mob && dist < best) {
                    best = dist;
                    other = m;
                }
            }

            mob.getNavigation().stop();
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), p);
            if (other != null) {
                mob.setTarget(other);
                mob.setLastHurtByMob(other);
            } else {
                mob.setTarget(null);
            }
        }
    }

    private static boolean noTarget(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.fallenroses.no_target").withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    private BoogieWoogie() {
    }
}
