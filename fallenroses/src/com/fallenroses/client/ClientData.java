package com.fallenroses.client;

import com.fallenroses.network.SyncDataPacket;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Stat;
import com.fallenroses.player.Talent;
import com.fallenroses.technique.Technique;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class ClientData {
    public static boolean synced;
    @Nullable
    public static PlayerClass playerClass;
    public static int level = 1;
    public static int xp;
    public static int talentPoints;
    public static int fingers;
    public static int flags;
    public static int absorbed;
    public static int paintings;
    public static final int[] stats = new int[Stat.values().length];
    public static int[] talents = new int[Talent.values().length];
    public static float energy;
    public static float max = 100.0F;
    public static float power = 1.0F;
    @Nullable
    public static Technique technique;
    public static boolean reinforced;
    public static boolean infinity;
    public static final int[] cooldowns = new int[5];
    public static final int[] readyFlash = new int[5];

    public static void apply(SyncDataPacket p) {
        synced = true;
        playerClass = PlayerClass.byOrdinal(p.playerClass());
        level = p.level();
        xp = p.xp();
        talentPoints = p.talentPoints();

        for (int i = 0; i < stats.length && i < p.stats().length; i++) {
            stats[i] = p.stats()[i];
        }

        int[] previous = talents;
        talents = new int[Talent.values().length];

        for (int i = 0; i < talents.length && i < p.talents().length; i++) {
            talents[i] = p.talents()[i];
        }

        ClientData.TalentEvents.onSync(previous, talents);
        energy = p.energy();
        max = p.max();
        technique = Technique.byOrdinal(p.technique());
        reinforced = p.reinforced();
        infinity = p.infinity();

        for (int i = 0; i < cooldowns.length && i < p.cooldowns().length; i++) {
            cooldowns[i] = p.cooldowns()[i];
        }

        fingers = p.fingers();
        flags = p.flags();
        power = p.power();
        absorbed = p.absorbed();
        paintings = p.paintings();
    }

    public static void tick() {
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0 && --cooldowns[i] == 0) {
                readyFlash[i] = 12;
            }

            if (readyFlash[i] > 0) {
                readyFlash[i]--;
            }
        }
    }

    public static boolean hasTalent(Talent t) {
        return talents[t.ordinal()] > 0;
    }

    public static int rank(Talent t) {
        return talents[t.ordinal()];
    }

    public static boolean isSlotUnlocked(int slot) {
        return PlayerData.isSlotUnlocked(slot, talents, technique, fingers, paintings);
    }

    public static boolean usesStamina() {
        return playerClass != null && playerClass.usesStamina();
    }

    public static void reset() {
        synced = false;
        playerClass = null;
        technique = null;
        reinforced = false;
        infinity = false;

        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = 0;
        }
    }

    private ClientData() {
    }

    public static final class TalentEvents {
        public static int[] lastBought = new int[0];
        public static long boughtAt;

        static void onSync(int[] before, int[] after) {
            if (before.length == after.length) {
                List<Integer> bought = new ArrayList<>();

                for (int i = 0; i < after.length; i++) {
                    if (after[i] > before[i]) {
                        bought.add(i);
                    }
                }

                if (!bought.isEmpty()) {
                    lastBought = bought.stream().mapToInt(Integer::intValue).toArray();
                    boughtAt = System.currentTimeMillis();
                }
            }
        }

        private TalentEvents() {
        }
    }
}
