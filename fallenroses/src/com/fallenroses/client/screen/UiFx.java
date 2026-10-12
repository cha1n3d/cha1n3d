package com.fallenroses.client.screen;

import com.fallenroses.FallenRoses;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class UiFx {
    public static final ResourceLocation PETAL = FallenRoses.id("textures/gui/petal.png");

    public static float easeOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    public static float easeOutBack(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float)Math.pow((double)(t - 1.0F), 3.0) + c1 * (float)Math.pow((double)(t - 1.0F), 2.0);
    }

    public static int argb(float a, int rgb) {
        return Mth.clamp((int)(a * 255.0F), 0, 255) << 24 | rgb & 16777215;
    }

    public static void sound(Supplier<SoundEvent> sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.get(), pitch, volume));
    }

    public static void centered(GuiGraphics g, Font font, Component text, float cx, float y, float scale, int argb, boolean shadow) {
        if ((argb >>> 24 & 0xFF) >= 4) {
            g.pose().pushPose();
            g.pose().translate(cx, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, text, -font.width(text) / 2, 0, argb, shadow);
            g.pose().popPose();
        }
    }

    public static void left(GuiGraphics g, Font font, Component text, float x, float y, float scale, int argb, boolean shadow) {
        if ((argb >>> 24 & 0xFF) >= 4) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, text, 0, 0, argb, shadow);
            g.pose().popPose();
        }
    }

    public static int paragraph(GuiGraphics g, Font font, Component text, float x, float y, int width, float scale, int argb, boolean center) {
        List<FormattedCharSequence> lines = font.split(text, (int)((float)width / scale));
        float lh = 10.0F * scale;

        for (int i = 0; i < lines.size(); i++) {
            g.pose().pushPose();
            float lx = center ? x + (float)width / 2.0F - (float)font.width(lines.get(i)) * scale / 2.0F : x;
            g.pose().translate(lx, y + (float)i * lh, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            if ((argb >>> 24 & 0xFF) >= 4) {
                g.drawString(font, lines.get(i), 0, 0, argb, false);
            }

            g.pose().popPose();
        }

        return (int)((float)lines.size() * lh);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }

    private UiFx() {
    }

    public static final class Petals {
        private final List<float[]> petals = new ArrayList<>();
        private final RandomSource random = RandomSource.create();
        private final int count;
        private final int[] colors;

        public Petals(int count, int... colors) {
            this.count = count;
            this.colors = colors;
        }

        public void tick(int width, int height) {
            while (this.petals.size() < this.count) {
                this.petals
                    .add(
                        new float[]{
                            this.random.nextFloat() * (float)width,
                            -10.0F - this.random.nextFloat() * (float)height,
                            0.2F + this.random.nextFloat() * 0.5F,
                            0.4F + this.random.nextFloat() * 0.8F,
                            this.random.nextFloat() * 6.28F,
                            (this.random.nextFloat() - 0.5F) * 0.12F,
                            4.0F + this.random.nextFloat() * 6.0F,
                            (float)this.colors[this.random.nextInt(this.colors.length)],
                            0.0F
                        }
                    );
            }

            for (float[] p : this.petals) {
                p[8]++;
                p[0] += p[2] + Mth.sin(p[8] * 0.05F + p[4]) * 0.4F;
                p[1] += p[3];
                p[4] += p[5];
                if (p[1] > (float)(height + 12) || p[0] > (float)(width + 12)) {
                    p[0] = this.random.nextFloat() * (float)width - 40.0F;
                    p[1] = -12.0F;
                }
            }
        }

        public void render(GuiGraphics g, float alpha) {
            for (float[] p : this.petals) {
                int c = (int)p[7];
                g.pose().pushPose();
                g.pose().translate(p[0], p[1], 0.0F);
                g.pose().mulPose(Axis.ZP.rotation(p[4]));
                g.setColor((float)(c >> 16 & 0xFF) / 255.0F, (float)(c >> 8 & 0xFF) / 255.0F, (float)(c & 0xFF) / 255.0F, alpha);
                int s = (int)p[6];
                g.blit(UiFx.PETAL, -s / 2, -s / 2, s, s, 0.0F, 0.0F, 16, 16, 16, 16);
                g.pose().popPose();
            }

            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
