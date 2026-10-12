package com.fallenroses.network;

import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Stat;
import com.fallenroses.technique.Technique;
import net.minecraft.network.FriendlyByteBuf;

public record SyncDataPacket(
    int playerClass,
    int level,
    int xp,
    int talentPoints,
    int[] stats,
    int[] talents,
    float energy,
    float max,
    int technique,
    boolean reinforced,
    boolean infinity,
    int[] cooldowns,
    int fingers,
    int flags,
    float power,
    int absorbed,
    int paintings
) {
    public static final int ZONE = 1;
    public static final int BERSERK = 2;
    public static final int STRIKE_READY = 4;
    public static final int OVERTIME = 8;

    public static SyncDataPacket of(PlayerData d) {
        PlayerClass c = d.getPlayerClass();
        Technique t = d.getTechnique();
        int[] stats = new int[Stat.values().length];

        for (Stat s : Stat.values()) {
            stats[s.ordinal()] = d.getStat(s);
        }

        int[] cd = new int[5];

        for (int i = 0; i < cd.length; i++) {
            cd[i] = d.getCooldown(i);
        }

        int flags = (d.zoneTicks > 0 ? 1 : 0) | (d.berserkTicks > 0 ? 2 : 0) | (d.strikeReadyTicks > 0 ? 4 : 0) | (d.overtimeTicks > 0 ? 8 : 0);
        return new SyncDataPacket(
            c == null ? -1 : c.ordinal(),
            d.getLevel(),
            d.getXp(),
            d.getTalentPoints(),
            stats,
            (int[])d.talentRanks().clone(),
            d.getEnergy(),
            d.getMax(),
            t == null ? -1 : t.ordinal(),
            d.isReinforced(),
            d.isInfinity(),
            cd,
            d.getFingers(),
            flags,
            d.power(),
            d.getAbsorbed(),
            d.getPaintings()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.playerClass + 1);
        buf.writeVarInt(this.level);
        buf.writeVarInt(this.xp);
        buf.writeVarInt(this.talentPoints);
        buf.writeVarIntArray(this.stats);
        buf.writeVarIntArray(this.talents);
        buf.writeFloat(this.energy);
        buf.writeFloat(this.max);
        buf.writeVarInt(this.technique + 1);
        buf.writeBoolean(this.reinforced);
        buf.writeBoolean(this.infinity);
        buf.writeVarIntArray(this.cooldowns);
        buf.writeVarInt(this.fingers);
        buf.writeVarInt(this.flags);
        buf.writeFloat(this.power);
        buf.writeVarInt(this.absorbed);
        buf.writeVarInt(this.paintings);
    }

    public static SyncDataPacket decode(FriendlyByteBuf buf) {
        return new SyncDataPacket(
            buf.readVarInt() - 1,
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarIntArray(),
            buf.readVarIntArray(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt() - 1,
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readVarIntArray(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }
}
