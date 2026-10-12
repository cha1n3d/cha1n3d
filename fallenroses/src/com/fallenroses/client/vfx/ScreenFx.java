package com.fallenroses.client.vfx;

import com.fallenroses.config.FRConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class ScreenFx {
    private static final RandomSource RANDOM = RandomSource.create();
    private static float shake;
    private static int shakeTicks;
    private static int shakeTotal;
    private static final List<ScreenFx.Flash> FLASHES = new ArrayList<>();
    private static Component domainName;
    private static int domainColor;
    private static int domainTicks;
    private static int domainTotal;
    private static int levelUp;
    private static int levelTicks;
    private static int levelPoints;
    public static final IGuiOverlay OVERLAY = (gui, g, partial, width, height) -> {
        for (ScreenFx.Flash f : FLASHES) {
            float t = ((float)f.age[0] + partial) / (float)f.duration;
            float a = f.intensity * (t < 0.15F ? t / 0.15F : 1.0F - (t - 0.15F) / 0.85F);
            if (a > 0.0F) {
                g.fill(0, 0, width, height, VfxDraw.argb(Mth.clamp(a, 0.0F, 1.0F), f.color));
            }
        }

        if (domainTicks > 0 && domainName != null) {
            drawDomainTitle(gui.getMinecraft(), g, partial, width, height);
        }

        if (levelTicks > 0) {
            drawLevelUp(gui.getMinecraft(), g, partial, width, height);
        }
    };

    public static void shake(float intensity, int duration) {
        float scaled = intensity * FRConfig.CAMERA_SHAKE.get().floatValue();
        if (!(scaled <= 0.0F)) {
            if (scaled >= shake * (float)shakeTicks / Math.max(1.0F, (float)shakeTotal)) {
                shake = scaled;
                shakeTicks = duration;
                shakeTotal = duration;
            }
        }
    }

    public static void flash(int color, int duration, float intensity) {
        if (FRConfig.SCREEN_FLASHES.get()) {
            FLASHES.add(new ScreenFx.Flash(color, Math.max(1, duration), intensity, new int[1]));
        }
    }

    public static void domainTitle(Component name, int color, int duration) {
        domainName = name;
        domainColor = color;
        domainTicks = duration;
        domainTotal = duration;
    }

    public static void levelUp(int level, int talentPoints) {
        levelUp = level;
        levelPoints = talentPoints;
        levelTicks = 90;
    }

    public static void tick() {
        if (shakeTicks > 0) {
            shakeTicks--;
        }

        FLASHES.removeIf(f -> ++f.age[0] > f.duration);
        if (domainTicks > 0) {
            domainTicks--;
        }

        if (levelTicks > 0) {
            levelTicks--;
        }
    }

    public static void reset() {
        shakeTicks = 0;
        FLASHES.clear();
        domainTicks = 0;
        levelTicks = 0;
    }

    public static void onCameraAngles(ComputeCameraAngles event) {
        if (shakeTicks > 0) {
            float k = shake * ((float)shakeTicks / (float)shakeTotal);
            k *= k > 0.0F ? 1.0F : 0.0F;
            event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * k * 2.2F);
            event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * k * 2.2F);
            event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * k * 1.6F);
        }
    }

    private static void drawDomainTitle(Minecraft mc, GuiGraphics g, float partial, int width, int height) {
        float age = (float)(domainTotal - domainTicks) + partial;
        float in = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
        float out = Mth.clamp(((float)domainTicks - partial) / 15.0F, 0.0F, 1.0F);
        float a = Math.min(in, out);
        int bar = (int)((float)height * 0.12F * VfxEffects.easeOut(a));
        g.fill(0, 0, width, bar, -268435456);
        g.fill(0, height - bar, width, height, -268435456);
        if (!(a <= 0.02F)) {
            Font font = mc.font;
            String jp = "領域展開";
            drawScaled(g, font, Component.literal(jp), (float)width / 2.0F, (float)height * 0.36F, 1.6F, VfxDraw.argb(a * 0.9F, 16777215), 0);
            float grow = 2.4F + 0.2F * VfxEffects.easeOut(Mth.clamp(age / 40.0F, 0.0F, 1.0F));
            Component name = domainName;

            for (int i = 0; i < 6; i++) {
                double ang = (double)i * Math.PI / 3.0;
                drawScaled(
                    g,
                    font,
                    name,
                    (float)width / 2.0F + (float)Math.cos(ang) * 1.5F,
                    (float)height * 0.45F + (float)Math.sin(ang) * 1.5F,
                    grow,
                    VfxDraw.argb(a * 0.35F, domainColor),
                    0
                );
            }

            drawScaled(g, font, name, (float)width / 2.0F, (float)height * 0.45F, grow, VfxDraw.argb(a, 16777215), 0);
            int lw = (int)((float)width * 0.32F * in);
            int ly = (int)((float)height * 0.45F + 12.0F * grow);
            g.fill(width / 2 - lw, ly, width / 2 + lw, ly + 1, VfxDraw.argb(a * 0.8F, domainColor));
        }
    }

    private static void drawLevelUp(Minecraft mc, GuiGraphics g, float partial, int width, int height) {
        float age = (float)(90 - levelTicks) + partial;
        float a = Math.min(Mth.clamp(age / 8.0F, 0.0F, 1.0F), Mth.clamp(((float)levelTicks - partial) / 20.0F, 0.0F, 1.0F));
        float y = (float)height * 0.22F - (1.0F - VfxEffects.easeOut(Mth.clamp(age / 12.0F, 0.0F, 1.0F))) * 12.0F;
        Font font = mc.font;
        int w = (int)((float)width * 0.22F);
        g.fillGradient(width / 2 - w, (int)y - 4, width / 2, (int)y + 30, VfxDraw.argb(0.0F, 2756688), VfxDraw.argb(a * 0.6F, 2756688));
        g.fillGradient(width / 2, (int)y - 4, width / 2 + w, (int)y + 30, VfxDraw.argb(a * 0.6F, 2756688), VfxDraw.argb(0.0F, 2756688));
        drawScaled(g, font, Component.translatable("hud.fallenroses.level_up", levelUp), (float)width / 2.0F, y, 2.0F, VfxDraw.argb(a, 15786239), 0);
        Component sub = Component.translatable("hud.fallenroses.level_up_talents", levelPoints);
        drawScaled(g, font, sub, (float)width / 2.0F, y + 20.0F, 1.0F, VfxDraw.argb(a, 13153535), 0);
    }

    public static void drawScaled(GuiGraphics g, Font font, Component text, float cx, float cy, float scale, int argb, int unused) {
        if ((argb >>> 24 & 0xFF) >= 5) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, text, -font.width(text) / 2, -4, argb, false);
            g.pose().popPose();
        }
    }

    private ScreenFx() {
    }

    private static record Flash(int color, int duration, float intensity, int[] age) {
    }
}
