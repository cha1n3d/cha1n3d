package com.fallenroses.network;

import com.fallenroses.vfx.VfxKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record VfxPacket(VfxKind kind, Vec3 pos, Vec3 dir, float scale, int color, int entity, int extra) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.kind.ordinal());
        buf.writeDouble(this.pos.x);
        buf.writeDouble(this.pos.y);
        buf.writeDouble(this.pos.z);
        buf.writeFloat((float)this.dir.x);
        buf.writeFloat((float)this.dir.y);
        buf.writeFloat((float)this.dir.z);
        buf.writeFloat(this.scale);
        buf.writeInt(this.color);
        buf.writeVarInt(this.entity + 1);
        buf.writeVarInt(this.extra);
    }

    public static VfxPacket decode(FriendlyByteBuf buf) {
        VfxKind kind = VfxKind.byOrdinal(buf.readVarInt());
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3((double)buf.readFloat(), (double)buf.readFloat(), (double)buf.readFloat());
        return new VfxPacket(kind, pos, dir, buf.readFloat(), buf.readInt(), buf.readVarInt() - 1, buf.readVarInt());
    }
}
