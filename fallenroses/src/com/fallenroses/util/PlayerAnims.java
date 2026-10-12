package com.fallenroses.util;

import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerAnims {
    public static void play(ServerPlayer player, String animation) {
        ModNetwork.sendTracking(player, new SimplePackets.PlayerAnim(player.getId(), animation));
    }

    private PlayerAnims() {
    }
}
