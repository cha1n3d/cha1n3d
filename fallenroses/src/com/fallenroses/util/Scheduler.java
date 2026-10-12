package com.fallenroses.util;

import com.fallenroses.FallenRoses;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "fallenroses"
)
public final class Scheduler {
    private static final List<Scheduler.Task> TASKS = new ArrayList<>();
    private static long tick;

    public static void later(int ticks, Runnable action) {
        TASKS.add(new Scheduler.Task(tick + (long)Math.max(1, ticks), action));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            tick++;
            if (!TASKS.isEmpty()) {
                List<Scheduler.Task> due = new ArrayList<>();
                TASKS.removeIf(tx -> {
                    if (tx.due <= tick) {
                        due.add(tx);
                        return true;
                    } else {
                        return false;
                    }
                });

                for (Scheduler.Task t : due) {
                    try {
                        t.action.run();
                    } catch (RuntimeException var5) {
                        FallenRoses.LOGGER.error("Scheduled task failed", (Throwable)var5);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }

    private Scheduler() {
    }

    private static record Task(long due, Runnable action) {
    }
}
