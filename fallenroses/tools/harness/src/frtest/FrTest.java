package frtest;

import com.fallenroses.player.ClassSelection;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.PlayerDataCap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Dev-only test director: makes a flat creative world, runs a scripted scenario and saves screenshots to
 * screenshots/, then quits. Scenario = -Dfrtest.scenario (see Scenarios).
 */
@Mod("frtest")
public class FrTest {
    public FrTest() {
        if (FMLEnvironment.dist.isClient()) {
            MinecraftForge.EVENT_BUS.register(new FrTest.Director());
        }
    }

    public static final class Step {
        final int tick;
        final Consumer<Minecraft> action;

        Step(int tick, Consumer<Minecraft> action) {
            this.tick = tick;
            this.action = action;
        }
    }

    /** Builder of a scenario timeline. */
    public static final class Script {
        final List<Step> steps = new ArrayList<>();
        int end;

        public Script client(int tick, Consumer<Minecraft> a) {
            this.steps.add(new Step(tick, a));
            this.end = Math.max(this.end, tick);
            return this;
        }

        public Script server(int tick, Consumer<ServerPlayer> a) {
            return this.client(tick, mc -> {
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> a.accept(server.getPlayerList().getPlayers().get(0)));
            });
        }

        public Script shot(int tick, String name) {
            return this.client(tick, mc -> Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {}));
        }

        public Script tp(int tick, double x, double y, double z, float yaw, float pitch) {
            return this.server(tick, sp -> sp.teleportTo(sp.serverLevel(), x, y, z, yaw, pitch));
        }

        public Script cmd(int tick, String command) {
            return this.server(tick, sp -> sp.server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4), command));
        }
    }

    static final class Director {
        private int phase;
        private int t;
        private Script script;

        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            mc.options.pauseOnLostFocus = false;
            if (this.phase == 0 && mc.getOverlay() == null && mc.screen != null && mc.level == null) {
                // any first screen (title or the 1.20 accessibility onboarding) once resources are loaded
                this.phase = 1;
                LevelSettings settings = new LevelSettings(
                    "frtest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT
                );
                mc.createWorldOpenFlows()
                    .createFreshLevel(
                        "frtest",
                        settings,
                        new WorldOptions(20261012L, false, false),
                        reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()
                    );
            } else if (this.phase == 1 && mc.player != null && mc.level != null && mc.getSingleplayerServer() != null) {
                this.phase = 2;
                this.t = 0;
                this.script = Scenarios.build(System.getProperty("frtest.scenario", "domains"));
                System.out.println("[frtest] world ready, running " + this.script.steps.size() + " steps until tick " + this.script.end);
            } else if (this.phase == 2) {
                if (mc.screen != null) {
                    mc.setScreen(null);
                }

                if (this.t == 0) {
                    mc.options.hideGui = true;
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }

                this.t++;

                for (Step s : this.script.steps) {
                    if (s.tick == this.t) {
                        try {
                            s.action.accept(mc);
                        } catch (Throwable ex) {
                            System.out.println("[frtest] step at " + s.tick + " failed: " + ex);
                            ex.printStackTrace();
                        }
                    }
                }

                if (this.t > this.script.end + 20) {
                    System.out.println("[frtest] done");
                    this.phase = 3;
                    mc.stop();
                }
            }
        }
    }

    /** Server side: a class, a technique and full cursed energy. */
    static void prepare(ServerPlayer sp, PlayerClass cls) {
        PlayerData d = PlayerDataCap.get(sp);
        if (d != null && d.getPlayerClass() == null) {
            ClassSelection.choose(sp, cls.ordinal(), 0);
        }

        sp.server.overworld().setDayTime(6000L);
        sp.server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, sp.server);
        sp.server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, sp.server);
    }
}
