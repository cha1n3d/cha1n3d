package com.fallenroses.player;

import com.fallenroses.FallenRoses;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

public final class FRAdvancements {
    public static void award(ServerPlayer player, String path) {
        Advancement adv = player.server.getAdvancements().getAdvancement(FallenRoses.id(path));
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);

            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(adv, criterion);
            }
        }
    }

    public static void onLevel(ServerPlayer player, int level) {
        for (int milestone : new int[]{5, 10, 20, 30, 50}) {
            if (level >= milestone) {
                award(player, "story/level_" + milestone);
            }
        }
    }

    private FRAdvancements() {
    }
}
