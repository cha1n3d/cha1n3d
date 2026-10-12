package com.fallenroses.client.hud;

import com.fallenroses.FallenRoses;
import com.fallenroses.client.ClientData;
import com.fallenroses.client.Keybinds;
import com.fallenroses.config.FRConfig;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Progression;
import com.fallenroses.technique.AbilityDef;
import com.fallenroses.technique.Technique;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.settings.KeyModifier;
import org.joml.Matrix4f;

public class AbilityHud implements IGuiOverlay {
    private static final ResourceLocation BG = FallenRoses.id("textures/gui/cursed_energy_background.png");
    private static final ResourceLocation BAR = FallenRoses.id("textures/gui/cursed_energy_bar.png");
    private static final ResourceLocation BAR_STAMINA = FallenRoses.id("textures/gui/stamina_bar.png");
    private static final ResourceLocation FRAME = FallenRoses.id("textures/gui/cursed_energy_foreground.png");
    private static final ResourceLocation FRAME_STAMINA = FallenRoses.id("textures/gui/stamina_foreground.png");
    private static final ResourceLocation SLOT = FallenRoses.id("textures/gui/ability_slot.png");
    private static final ResourceLocation SLOT_ULT = FallenRoses.id("textures/gui/ability_slot_ultimate.png");
    private static final ResourceLocation LOCK = FallenRoses.id("textures/gui/lock.png");
    private float shownEnergy = -1.0F;

    public static ResourceLocation icon(String id) {
        return FallenRoses.id("textures/gui/ability/" + id + ".png");
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = gui.getMinecraft();
        if (!mc.options.hideGui && mc.player != null && !mc.player.isSpectator() && ClientData.synced && ClientData.playerClass != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float time = (float)(mc.level != null ? mc.level.getGameTime() : 0L) + partial;
            if (FRConfig.SHOW_HUD.get()) {
                this.renderBar(mc, g, time, width, height);
            }

            if (FRConfig.SHOW_ABILITY_SLOTS.get()) {
                this.renderSlots(mc, g, time, width, height);
            }

            RenderSystem.disableBlend();
        }
    }

    private void renderBar(Minecraft mc, GuiGraphics g, float time, int width, int height) {
        int x = width / 2 + FRConfig.HUD_X.get();
        int y = height + FRConfig.HUD_Y.get();
        boolean stamina = ClientData.usesStamina();
        float max = Math.max(1.0F, ClientData.max);
        float energy = ClientData.energy;
        this.shownEnergy = this.shownEnergy < 0.0F ? energy : Mth.lerp(0.25F, this.shownEnergy, energy);
        g.blit(BG, x, y, 0.0F, 0.0F, 82, 8, 256, 256);
        int fill = Math.round(76.0F * Mth.clamp(this.shownEnergy / max, 0.0F, 1.0F));
        if (fill > 0) {
            int frame = (int)(time / 3.0F % 32.0F);
            g.blit(stamina ? BAR_STAMINA : BAR, x + 2, y + 1, 0.0F, (float)(frame * 32), fill, 6, 256, 1024);
            if ((ClientData.flags & 2) != 0) {
                g.fill(x + 2, y + 1, x + 2 + fill, y + 7, 1893732384);
            }

            if (ClientData.reinforced) {
                float pulse = 0.5F + 0.5F * Mth.sin(time * 0.25F);
                g.fill(x + 2, y + 1, x + 2 + fill, y + 7, (int)(40.0F + 60.0F * pulse) << 24 | (stamina ? 16773312 : 14207231));
            }
        }

        g.blit(stamina ? FRAME_STAMINA : FRAME, x - 15, y - 12, 0.0F, 0.0F, 106, 29, 256, 256);
        Font font = mc.font;
        if (FRConfig.SHOW_TEXT.get()) {
            text(g, font, Math.round(energy) + "/" + Math.round(max), (float)(x + 41), (float)y + 1.5F, 0.75F, 16777215, AbilityHud.Align.CENTER);
        }

        int lvl = ClientData.level;
        float xpFrac = lvl >= 50 ? 1.0F : Mth.clamp((float)ClientData.xp / (float)Progression.xpForNext(lvl), 0.0F, 1.0F);
        int classColor = ClientData.playerClass.color();
        text(
            g,
            font,
            Component.translatable("hud.fallenroses.level_short", lvl).getString(),
            (float)(x + 5),
            (float)y - 9.5F,
            0.6F,
            15261951,
            AbilityHud.Align.LEFT
        );
        int bx0 = x + 30;
        int bx1 = x + 78;
        int by = y - 7;
        g.fill(bx0, by, bx1, by + 2, -1610612736);
        g.fill(bx0, by, bx0 + Math.round((float)(bx1 - bx0) * xpFrac), by + 2, 0xFF000000 | classColor);
        g.fill(bx0, by, bx0 + Math.round((float)(bx1 - bx0) * xpFrac), by + 1, 1627389951);
        if (ClientData.talentPoints > 0) {
            float pulse = 0.6F + 0.4F * Mth.sin(time * 0.2F);
            text(g, font, "+", (float)(x + 81), (float)(y - 10), 0.8F, (int)(255.0F * pulse) << 24 | 16767072, AbilityHud.Align.LEFT);
        }

        if (ClientData.playerClass == PlayerClass.CURSE_MANIPULATOR) {
            text(g, font, "霊 " + ClientData.absorbed, (float)(x - 4), (float)y + 0.5F, 0.7F, 3844744, AbilityHud.Align.RIGHT);
        }

        if ((ClientData.flags & 8) != 0) {
            float pulse = 0.55F + 0.45F * Mth.sin(time * 0.3F);
            text(g, font, "残業", (float)(x + 92), (float)(y + 9), 0.6F, (int)(255.0F * pulse) << 24 | 15253600, AbilityHud.Align.LEFT);
        }

        if ((ClientData.flags & 1) != 0) {
            float pulse = 0.55F + 0.45F * Mth.sin(time * 0.5F);
            text(g, font, "黒閃", (float)(x + 92), (float)y + 0.5F, 0.75F, (int)(255.0F * pulse) << 24 | 16719920, AbilityHud.Align.LEFT);
        }
    }

    private void renderSlots(Minecraft mc, GuiGraphics g, float time, int width, int height) {
        Technique technique = ClientData.technique;
        int x = width / 2 + 91 + 6;
        int y = height - 22;
        Font font = mc.font;
        if (technique != null) {
            for (int slot = 0; slot < 4; slot++) {
                this.drawSlot(mc, g, font, x, y, technique, slot, time);
                x += 23;
            }

            x += 3;
            this.drawUltimate(mc, g, font, x, y - 3, technique, time);
            x += 30;
        }

        boolean stamina = ClientData.usesStamina();
        g.blit(SLOT, x, y, 0.0F, 0.0F, 22, 22, 22, 22);
        g.blit(icon(stamina ? "focus" : "reinforcement"), x + 3, y + 3, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        if (ClientData.reinforced) {
            glowBorder(g, x, y, 22, stamina ? 16771264 : 12098815, time);
        }

        keyLabel(g, font, Keybinds.REINFORCE, (float)(x + 20), (float)(y + 15), ClientData.reinforced);
    }

    private void drawSlot(Minecraft mc, GuiGraphics g, Font font, int x, int y, Technique technique, int slot, float time) {
        AbilityDef ability = technique.ability(slot);
        g.blit(SLOT, x, y, 0.0F, 0.0F, 22, 22, 22, 22);
        boolean unlocked = ClientData.isSlotUnlocked(slot);
        g.blit(icon(ability.id()), x + 3, y + 3, 16, 16, 0.0F, 0.0F, 32, 32, 32, 32);
        if (!unlocked) {
            g.fill(x + 3, y + 3, x + 19, y + 19, -1073741824);
            g.blit(LOCK, x + 7, y + 5, 8, 8, 0.0F, 0.0F, 16, 16, 16, 16);
            fingerReq(g, font, ability, (float)(x + 11), (float)(y + 14));
        } else {
            this.decorate(mc, g, font, x + 11, y + 11, 8, technique, slot, ability, time);
            keyLabel(g, font, Keybinds.SLOTS[slot], (float)(x + 20), (float)(y + 15), false);
        }
    }

    private void drawUltimate(Minecraft mc, GuiGraphics g, Font font, int x, int y, Technique technique, float time) {
        AbilityDef ability = technique.ability(4);
        g.blit(SLOT_ULT, x, y, 0.0F, 0.0F, 28, 28, 28, 28);
        boolean unlocked = ClientData.isSlotUnlocked(4);
        g.blit(icon(ability.id()), x + 4, y + 4, 20, 20, 0.0F, 0.0F, 32, 32, 32, 32);
        if (!unlocked) {
            g.fill(x + 4, y + 4, x + 24, y + 24, -1073741824);
            g.blit(LOCK, x + 9, y + 7, 10, 10, 0.0F, 0.0F, 16, 16, 16, 16);
            fingerReq(g, font, ability, (float)(x + 14), (float)(y + 18));
        } else {
            this.decorate(mc, g, font, x + 14, y + 14, 10, technique, 4, ability, time);
            if (ClientData.cooldowns[4] == 0 && affordable(mc, ability)) {
                glowBorder(g, x, y, 28, technique.color(), time);
            }

            keyLabel(g, font, Keybinds.ULTIMATE, (float)(x + 26), (float)(y + 21), false);
        }
    }

    private static void fingerReq(GuiGraphics g, Font font, AbilityDef ability, float cx, float y) {
        if (ability.fingers() > ClientData.fingers) {
            text(g, font, ClientData.fingers + "/" + ability.fingers() + "指", cx, y, 0.45F, 16736368, AbilityHud.Align.CENTER);
        }
    }

    private void decorate(Minecraft mc, GuiGraphics g, Font font, int cx, int cy, int r, Technique technique, int slot, AbilityDef ability, float time) {
        int cd = ClientData.cooldowns[slot];
        if (!affordable(mc, ability) && cd == 0) {
            g.fill(cx - r, cy - r, cx + r, cy + r, -2142240752);
        }

        if (cd > 0) {
            pie(g, (float)cx, (float)cy, (float)r + 0.5F, Mth.clamp((float)cd / (float)ability.cooldown(), 0.0F, 1.0F), -1342177280);
            String s = cd >= 20 ? String.valueOf(Mth.ceil((float)cd / 20.0F)) : String.format("%.1f", (float)cd / 20.0F);
            text(g, font, s, (float)cx, (float)(cy - 3), r >= 10 ? 0.9F : 0.75F, 16777215, AbilityHud.Align.CENTER);
        }

        if (ability.toggle() && ClientData.infinity) {
            glowBorder(g, cx - r - 3, cy - r - 3, 2 * r + 6, technique.color(), time);
        }

        int flash = ClientData.readyFlash[slot];
        if (flash > 0) {
            int a = (int)((float)(180 * flash) / 12.0F);
            g.fill(cx - r, cy - r, cx + r, cy + r, a << 24 | 16777215);
        }
    }

    private static boolean affordable(Minecraft mc, AbilityDef ability) {
        return mc.player != null && (mc.player.getAbilities().instabuild || ClientData.energy >= ability.cost() * costMultiplier());
    }

    private static float costMultiplier() {
        return PlayerData.costMultiplier(ClientData.talents);
    }

    private static void glowBorder(GuiGraphics g, int x, int y, int size, int rgb, float time) {
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.3F);
        int c = (int)(90.0F + 120.0F * pulse) << 24 | rgb & 16777215;
        g.fill(x, y, x + size, y + 1, c);
        g.fill(x, y + size - 1, x + size, y + size, c);
        g.fill(x, y, x + 1, y + size, c);
        g.fill(x + size - 1, y, x + size, y + size, c);
    }

    private static void keyLabel(GuiGraphics g, Font font, KeyMapping key, float rightX, float y, boolean active) {
        String base = key.getKey().getDisplayName().getString();
        if (base.length() > 3) {
            base = base.substring(0, 3);
        }

        String label = (key.getKeyModifier() == KeyModifier.SHIFT ? "⇧" : (key.getKeyModifier() == KeyModifier.CONTROL ? "^" : "")) + base;
        text(g, font, label, rightX, y, 0.5F, active ? 14733567 : 12105920, AbilityHud.Align.RIGHT);
    }

    private static void pie(GuiGraphics g, float cx, float cy, float r, float frac, int argb) {
        if (!(frac <= 0.0F)) {
            Matrix4f m = g.pose().last().pose();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            int a = argb >>> 24 & 0xFF;
            int rr = argb >> 16 & 0xFF;
            int gg = argb >> 8 & 0xFF;
            int bb = argb & 0xFF;
            b.vertex(m, cx, cy, 0.0F).color(rr, gg, bb, a).endVertex();
            int seg = Math.max(2, (int)(40.0F * frac));

            for (int i = 0; i <= seg; i++) {
                double ang = (-Math.PI / 2) + (double)((float) (Math.PI * 2) * frac * (float)i / (float)seg);
                b.vertex(m, (float)((double)cx + Math.cos(ang) * (double)r), (float)((double)cy + Math.sin(ang) * (double)r), 0.0F)
                    .color(rr, gg, bb, a)
                    .endVertex();
            }

            BufferUploader.drawWithShader(b.end());
        }
    }

    static void text(GuiGraphics g, Font font, String text, float x, float y, float scale, int color, AbilityHud.Align align) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int w = font.width(text);

        int dx = switch (align) {
            case LEFT -> 0;
            case CENTER -> -w / 2;
            case RIGHT -> -w;
        };
        g.drawString(font, text, dx, 0, color, true);
        g.pose().popPose();
    }

    static enum Align {
        LEFT,
        CENTER,
        RIGHT;
    }
}
