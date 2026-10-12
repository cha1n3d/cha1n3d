package com.fallenroses.client;

import com.fallenroses.client.anim.PlayerAnimClient;
import com.fallenroses.client.screen.IntroScreen;
import com.fallenroses.client.vfx.ScreenFx;
import com.fallenroses.client.vfx.VfxClient;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.network.SyncDataPacket;
import com.fallenroses.network.VfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientPackets {
    private static boolean pendingIntro;
    private static boolean pendingSkipStory;

    public static void sync(SyncDataPacket msg) {
        ClientData.apply(msg);
    }

    public static void vfx(VfxPacket msg) {
        VfxClient.play(msg);
    }

    public static void playerAnim(SimplePackets.PlayerAnim msg) {
        PlayerAnimClient.play(msg.entity(), msg.animation());
    }

    public static void openIntro(SimplePackets.OpenIntro msg) {
        pendingIntro = true;
        pendingSkipStory = msg.skipStory();
    }

    public static void tryOpenIntro(Minecraft mc) {
        if (pendingIntro && mc.player != null && mc.level != null && mc.screen == null && mc.getOverlay() == null) {
            pendingIntro = false;
            mc.setScreen(new IntroScreen(pendingSkipStory));
        }
    }

    public static void levelUp(SimplePackets.LevelUp msg) {
        ScreenFx.levelUp(msg.level(), msg.talentPoints());
    }

    public static void screenFx(SimplePackets.ScreenFx msg) {
        switch (msg.kind()) {
            case 0:
                ScreenFx.flash(msg.color(), msg.duration(), msg.intensity());
                break;
            case 1:
                ScreenFx.shake(msg.intensity(), msg.duration());
                break;
            case 2:
                ScreenFx.domainTitle(Component.translatable(msg.key()), msg.color(), msg.duration());
        }
    }

    public static void reset() {
        pendingIntro = false;
    }

    private ClientPackets() {
    }
}
