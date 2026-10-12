package com.fallenroses.client;

import com.fallenroses.client.screen.CharacterScreen;
import com.fallenroses.client.vfx.MangaFx;
import com.fallenroses.client.vfx.ScreenFx;
import com.fallenroses.client.vfx.VfxEffects;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "fallenroses",
    value = {Dist.CLIENT}
)
public final class ClientEvents {
    @SubscribeEvent
    public static void tick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                if (!mc.isPaused()) {
                    ClientData.tick();
                    VfxEffects.tick();
                    ScreenFx.tick();
                    MangaFx.tick();
                }

                ClientPackets.tryOpenIntro(mc);
                if (mc.screen == null) {
                    for (int i = 0; i < Keybinds.SLOTS.length; i++) {
                        while (Keybinds.SLOTS[i].consumeClick()) {
                            ModNetwork.sendToServer(new SimplePackets.AbilityKey(i));
                        }
                    }

                    while (Keybinds.REINFORCE.consumeClick()) {
                        ModNetwork.sendToServer(new SimplePackets.AbilityKey(5));
                    }

                    while (Keybinds.CHARACTER.consumeClick()) {
                        if (ClientData.playerClass != null) {
                            mc.setScreen(new CharacterScreen());
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_PARTICLES) {
            VfxEffects.render(event.getPoseStack(), event.getCamera(), event.getPartialTick());
            MangaFx.renderWorld(event.getPoseStack(), event.getCamera(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void fov(ComputeFov event) {
        double k = MangaFx.fovScale((float)event.getPartialTick());
        if (k != 1.0) {
            event.setFOV(event.getFOV() * k);
        }
    }

    @SubscribeEvent
    public static void cameraAngles(ComputeCameraAngles event) {
        ScreenFx.onCameraAngles(event);
    }

    @SubscribeEvent
    public static void loggedOut(LoggingOut event) {
        ClientData.reset();
        ClientPackets.reset();
        VfxEffects.clear();
        ScreenFx.reset();
        MangaFx.reset();
    }

    private ClientEvents() {
    }
}
