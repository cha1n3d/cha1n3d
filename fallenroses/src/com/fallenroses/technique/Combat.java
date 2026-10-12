package com.fallenroses.technique;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.registry.ModDamageTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class Combat {
    @Nullable
    public static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, Block.COLLIDER, Fluid.NONE, player));
        if (wall.getType() != Type.MISS) {
            end = wall.getLocation();
        }

        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box, ex -> valid(ex, player), eye.distanceToSqr(end));
        if (hit != null) {
            return (LivingEntity)hit.getEntity();
        } else {
            LivingEntity best = null;
            double bestDot = 0.985;

            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), ex -> valid(ex, player))) {
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double d = to.length();
                if (!(d > range)) {
                    double dot = to.scale(1.0 / d).dot(look);
                    if (dot > bestDot && player.hasLineOfSight(e)) {
                        bestDot = dot;
                        best = e;
                    }
                }
            }

            return best;
        }
    }

    public static Vec3 lookPoint(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        BlockHitResult wall = player.level().clip(new ClipContext(eye, eye.add(look.scale(range)), Block.COLLIDER, Fluid.NONE, player));
        return wall.getType() == Type.MISS ? eye.add(look.scale(range)) : wall.getLocation().subtract(look.scale(0.8));
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), ex -> valid(ex, player))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist <= range && dist > 1.0E-4 && to.scale(1.0 / dist).dot(look) >= minDot && player.hasLineOfSight(e)) {
                result.add(e);
            }
        }

        return result;
    }

    public static List<LivingEntity> around(Entity center, Vec3 pos, double radius) {
        List<LivingEntity> result = new ArrayList<>();

        for (LivingEntity e : center.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius), ex -> valid(ex, center))) {
            if (e.getBoundingBox().getCenter().distanceTo(pos) <= radius + (double)(e.getBbWidth() / 2.0F)) {
                result.add(e);
            }
        }

        return result;
    }

    public static boolean valid(Entity e, Entity caster) {
        if (e != caster
            && e instanceof LivingEntity living
            && living.isAlive()
            && !e.isSpectator()
            && !(e instanceof ArmorStand)
            && (!(e instanceof ServerPlayer p) || !p.isCreative())
            && !e.isAlliedTo(caster)) {
            return true;
        }

        return false;
    }

    public static void hurt(LivingEntity target, Entity direct, @Nullable Entity owner, float amount) {
        target.invulnerableTime = 0;
        target.hurt(ModDamageTypes.technique(direct, owner), amount);
    }

    public static void knock(LivingEntity e, Vec3 dir, double strength, double up) {
        double k = strength * (1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.6);
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize();
        e.push(d.x * k, up, d.z * k);
        e.hurtMarked = true;
    }

    public static void sound(Entity source, Vec3 at, Supplier<SoundEvent> sound, float volume, float pitch) {
        source.level().playSound(null, at.x, at.y, at.z, sound.get(), SoundSource.PLAYERS, volume, pitch);
    }

    public static void shake(ServerLevel level, Vec3 at, float intensity, int duration, double radius) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (!(d > radius)) {
                float k = (float)(1.0 - d / radius);
                ModNetwork.send(p, new SimplePackets.ScreenFx(1, 0, duration, intensity * k, ""));
            }
        }
    }

    public static void flash(ServerLevel level, Vec3 at, int color, int duration, float intensity, double radius) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(at);
            if (!(d > radius)) {
                ModNetwork.send(p, new SimplePackets.ScreenFx(0, color, duration, intensity * (float)(1.0 - d / radius * 0.7), ""));
            }
        }
    }

    private Combat() {
    }
}
