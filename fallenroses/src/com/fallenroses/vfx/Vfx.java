package com.fallenroses.vfx;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.VfxPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;

public final class Vfx {
    private static final double RANGE = 96.0;

    public static void play(ServerLevel level, VfxKind kind, Vec3 pos) {
        play(level, kind, pos, Vec3.ZERO, 1.0F, 16777215, -1, 0);
    }

    public static void play(ServerLevel level, VfxKind kind, Vec3 pos, Vec3 dir, float scale) {
        play(level, kind, pos, dir, scale, 16777215, -1, 0);
    }

    public static void at(Entity entity, VfxKind kind) {
        at(entity, kind, 1.0F, 0);
    }

    public static void at(Entity entity, VfxKind kind, float scale, int extra) {
        if (entity.level() instanceof ServerLevel level) {
            play(level, kind, entity.position().add(0.0, (double)(entity.getBbHeight() / 2.0F), 0.0), Vec3.ZERO, scale, 16777215, entity.getId(), extra);
        }
    }

    public static void play(ServerLevel level, VfxKind kind, Vec3 pos, Vec3 dir, float scale, int color, int entity, int extra) {
        ModNetwork.CHANNEL
            .send(
                PacketDistributor.NEAR.with(() -> new TargetPoint(pos.x, pos.y, pos.z, 96.0, level.dimension())),
                new VfxPacket(kind, pos, dir, scale, color, entity, extra)
            );
    }

    private Vfx() {
    }
}
