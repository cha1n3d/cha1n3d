package com.fallenroses.client.screen;

import com.fallenroses.FallenRoses;
import com.fallenroses.client.ClientData;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.Stat;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.Technique;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class IntroScreen extends Screen {
    private static final ResourceLocation CARD = FallenRoses.id("textures/gui/class_card.png");
    private static final int LINES = 5;
    private static final int PANEL = 130;
    private static final int SPIN = 70;
    private IntroScreen.Phase phase;
    private int ticks;
    private int storyLine;
    private int typed;
    private int selected = -1;
    private int hovered = -1;
    private int lastHoverSound = -1;
    private final float[] lift = new float[PlayerClass.values().length];
    private final float[] prevLift = new float[PlayerClass.values().length];
    private float partialTick;
    private PlayerClass chosenClass;
    private Technique chosenTechnique;
    private List<Technique> pool = List.of();
    private float spinPos;
    private float spinSpeed;
    private int landedAt = -1;
    private int lastTick = -1;
    private boolean landing;
    private float landFrom;
    private float landGoal;
    private float landDur;
    private int landStart;
    private final UiFx.Petals petals = new UiFx.Petals(26, 1052692, 2755088, 5901848);
    private final UiFx.Petals whitePetals = new UiFx.Petals(14, 1314840, 2760752);

    public IntroScreen(boolean skipStory) {
        super(Component.translatable("intro.fallenroses.title"));
        this.phase = skipStory ? IntroScreen.Phase.CLASS : IntroScreen.Phase.STORY;
    }

    @Override
    protected void init() {
        if (this.ticks == 0) {
            UiFx.sound(ModSounds.INTRO_DRONE, 1.0F, 0.8F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return this.phase == IntroScreen.Phase.STORY || this.phase == IntroScreen.Phase.CLASS;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void go(IntroScreen.Phase next) {
        this.phase = next;
        this.ticks = 0;
        this.selected = -1;
        this.hovered = -1;

        for (int i = 0; i < this.lift.length; i++) {
            this.lift[i] = this.prevLift[i] = 0.0F;
        }
    }

    @Override
    public void tick() {
        this.ticks++;
        this.petals.tick(this.width, this.height);
        this.whitePetals.tick(this.width, this.height);
        if (this.phase == IntroScreen.Phase.STORY) {
            int local = this.ticks;
            if (local == 1) {
                UiFx.sound(ModSounds.INTRO_FLASH, 1.0F, 0.5F);
            }

            Component line = Component.translatable("intro.fallenroses.line" + (this.storyLine + 1));
            int len = line.getString().length();
            int target = Mth.clamp((local - 34) / 2, 0, len);
            if (target > this.typed) {
                this.typed = target;
                if (this.typed % 3 == 0) {
                    UiFx.sound(ModSounds.INTRO_TEXT, 0.9F + (float)(this.typed % 7) * 0.03F, 0.25F);
                }
            }

            if (local >= 130) {
                this.storyLine++;
                this.typed = 0;
                this.ticks = 0;
                if (this.storyLine >= 5) {
                    this.go(IntroScreen.Phase.CLASS);
                    UiFx.sound(ModSounds.INTRO_DRONE, 0.9F, 0.7F);
                }
            }
        } else if (this.phase == IntroScreen.Phase.REVEAL) {
            this.tickReveal();
        } else if (this.phase == IntroScreen.Phase.OUTRO && this.ticks > 60) {
            this.onClose();
        }

        for (int i = 0; i < this.lift.length; i++) {
            this.prevLift[i] = this.lift[i];
            float goal = i != this.hovered && i != this.selected ? 0.0F : 1.0F;
            this.lift[i] = Mth.lerp(0.3F, this.lift[i], goal);
        }
    }

    private void tickReveal() {
        if (!this.pool.isEmpty() && this.landedAt < 0) {
            Technique result = ClientData.technique;
            if (!this.landing) {
                this.spinSpeed = Math.max(0.5F, 1.6F - (float)this.ticks * 0.012F);
                this.spinPos = this.spinPos + this.spinSpeed;
                if (this.ticks >= 70 && result != null && this.pool.contains(result)) {
                    this.planLanding(result);
                } else if (this.ticks >= 110 && this.ticks % 60 == 0 && this.chosenClass != null) {
                    ModNetwork.sendToServer(new SimplePackets.ChooseClass(this.chosenClass.ordinal(), -1));
                }
            } else {
                float before = this.spinPos;
                float u = Math.min(1.0F, (float)(this.ticks - this.landStart) / this.landDur);
                this.spinPos = this.landPos(u);
                this.spinSpeed = this.spinPos - before;
                if (u >= 1.0F) {
                    this.spinPos = this.landGoal;
                    this.landedAt = this.ticks;
                    this.chosenTechnique = result != null ? result : this.pool.get(Math.floorMod(Math.round(this.landGoal), this.pool.size()));
                    UiFx.sound(ModSounds.CLASS_CONFIRM, 1.1F, 0.9F);
                    UiFx.sound(ModSounds.SKILL_UNLOCK, 1.0F, 1.0F);
                }
            }

            int cur = Math.round(this.spinPos);
            if (cur != this.lastTick && this.landedAt < 0) {
                this.lastTick = cur;
                UiFx.sound(ModSounds.CARD_HOVER, 0.8F + Math.min(0.6F, this.spinSpeed * 0.3F), 0.45F);
            }
        }
    }

    private void planLanding(Technique result) {
        int n = this.pool.size();
        float speed = Math.max(0.05F, this.spinSpeed);
        float goal = (float)Math.floor((double)(this.spinPos / (float)n)) * (float)n + (float)this.pool.indexOf(result);

        while (goal < this.spinPos + speed * 16.0F) {
            goal += (float)n;
        }

        this.landing = true;
        this.landFrom = this.spinPos;
        this.landGoal = goal;
        this.landDur = 3.0F * (goal - this.spinPos) / speed;
        this.landStart = this.ticks;
    }

    private float landPos(float u) {
        float v = 1.0F - Mth.clamp(u, 0.0F, 1.0F);
        return this.landFrom + (this.landGoal - this.landFrom) * (1.0F - v * v * v);
    }

    private float revealPos(float partial) {
        if (this.landedAt >= 0) {
            return this.spinPos;
        } else {
            return this.landing ? this.landPos(((float)(this.ticks - this.landStart) + partial) / this.landDur) : this.spinPos + this.spinSpeed * partial;
        }
    }

    @Override
    public void onClose() {
        if (this.phase == IntroScreen.Phase.OUTRO) {
            super.onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        float t = (float)this.ticks + partial;
        this.partialTick = partial;
        switch (this.phase) {
            case STORY:
                this.renderStory(g, t);
                break;
            case CLASS:
                this.renderChoice(g, mouseX, mouseY, t);
                break;
            case REVEAL:
                this.renderReveal(g, t, partial);
                break;
            case OUTRO:
                this.renderOutro(g, t);
        }
    }

    private void renderStory(GuiGraphics g, float t) {
        g.fill(0, 0, this.width, this.height, -16777216);
        float in = UiFx.easeOut(t / 30.0F);
        float out = Mth.clamp((130.0F - t) / 22.0F, 0.0F, 1.0F);
        float white = Math.min(in, out);
        int pad = (int)((float)this.width * 0.05F * (1.0F - in));
        g.fill(pad, pad, this.width - pad, this.height - pad, UiFx.argb(white, 16052716));
        g.fillGradient(0, 0, this.width, this.height / 6, UiFx.argb(white * 0.25F, 0), UiFx.argb(0.0F, 0));
        g.fillGradient(0, this.height - this.height / 6, this.width, this.height, UiFx.argb(0.0F, 0), UiFx.argb(white * 0.25F, 0));
        String[] glyphs = new String[]{"日", "呪", "怨", "眼", "薔"};
        UiFx.centered(
            g,
            this.font,
            Component.literal(glyphs[this.storyLine % glyphs.length]),
            (float)this.width / 2.0F,
            (float)this.height * 0.5F - 60.0F,
            9.0F,
            UiFx.argb(white * 0.07F, 0),
            false
        );
        this.whitePetals.render(g, white * 0.8F);
        String full = Component.translatable("intro.fallenroses.line" + (this.storyLine + 1)).getString();
        String shown = full.substring(0, Math.min(this.typed, full.length()));
        UiFx.paragraph(
            g,
            this.font,
            Component.literal(shown),
            (float)this.width * 0.15F,
            (float)this.height * 0.44F,
            (int)((float)this.width * 0.7F),
            1.5F,
            UiFx.argb(white, 1380890),
            true
        );
        UiFx.centered(
            g,
            this.font,
            Component.translatable("intro.fallenroses.skip"),
            (float)this.width / 2.0F,
            (float)(this.height - 18),
            0.75F,
            UiFx.argb(white * 0.6F, 5920096),
            false
        );
    }

    private void renderChoice(GuiGraphics g, int mouseX, int mouseY, float t) {
        g.fillGradient(0, 0, this.width, this.height, -16448505, -15071992);
        float fade = UiFx.easeOut(t / 20.0F);
        this.petals.render(g, 0.85F * fade);
        UiFx.centered(
            g,
            this.font,
            Component.translatable("intro.fallenroses.choose_path"),
            (float)this.width / 2.0F,
            (float)this.height * 0.05F,
            2.0F,
            UiFx.argb(fade, 15788276),
            true
        );
        UiFx.centered(
            g,
            this.font,
            Component.translatable("intro.fallenroses.choose_path_hint"),
            (float)this.width / 2.0F,
            (float)this.height * 0.05F + 20.0F,
            0.8F,
            UiFx.argb(fade * 0.8F, 11050160),
            false
        );
        int n = PlayerClass.values().length;
        this.hovered = -1;

        for (int i = 0; i < n; i++) {
            int[] b = this.cardBox(i, n);
            float enter = UiFx.easeOutBack((t - 8.0F - (float)(i * 5)) / 16.0F);
            if (!(enter <= 0.0F)) {
                int x = b[0];
                float fy = (float)b[1] + (1.0F - enter) * (float)this.height * 0.6F - this.lf(i) * 8.0F;
                int y = (int)fy;
                boolean over = mouseX >= x && mouseX < x + b[2] && mouseY >= y && mouseY < y + b[3] && enter >= 1.0F;
                if (over) {
                    this.hovered = i;
                }

                g.pose().pushPose();
                g.pose().translate((float)x, fy, 0.0F);
                this.drawCard(g, PlayerClass.values()[i], i, 0, 0, b[2], b[3], enter);
                g.pose().popPose();
            }
        }

        if (this.hovered >= 0 && this.hovered != this.lastHoverSound) {
            UiFx.sound(ModSounds.CARD_HOVER, 1.0F + (float)this.hovered * 0.05F, 0.4F);
        }

        this.lastHoverSound = this.hovered;
        if (this.selected >= 0) {
            int[] btn = this.confirmBox();
            boolean over = mouseX >= btn[0] && mouseX < btn[0] + btn[2] && mouseY >= btn[1] && mouseY < btn[1] + btn[3];
            int color = PlayerClass.values()[this.selected].color();
            g.fill(btn[0], btn[1], btn[0] + btn[2], btn[1] + btn[3], over ? -533721064 : -1072429040);
            UiFx.frame(g, btn[0], btn[1], btn[2], btn[3], 0xFF000000 | color);
            UiFx.centered(
                g,
                this.font,
                Component.translatable("intro.fallenroses.confirm"),
                (float)btn[0] + (float)btn[2] / 2.0F,
                (float)btn[1] + (float)btn[3] / 2.0F - 4.0F,
                1.0F,
                over ? -1 : -2041624,
                true
            );
        }
    }

    private float lf(int i) {
        return Mth.lerp(this.partialTick, this.prevLift[i], this.lift[i]);
    }

    private void drawCard(GuiGraphics g, PlayerClass c, int i, int x, int y, int w, int h, float enter) {
        int color = c.color();
        float lift = this.lf(i);
        boolean dim = this.selected >= 0 && this.selected != i;
        float a = Mth.clamp(enter, 0.0F, 1.0F) * (dim ? 0.55F : 1.0F);
        if (lift > 0.02F) {
            int glow = (int)(6.0F * lift);
            g.fill(x - glow, y - glow, x + w + glow, y + h + glow, UiFx.argb(0.25F * lift, color));
        }

        g.setColor(1.0F, 1.0F, 1.0F, a);
        g.blit(CARD, x, y, w, h, 0.0F, 0.0F, 128, 192, 128, 192);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        UiFx.frame(g, x, y, w, h, UiFx.argb(a * (0.4F + 0.6F * lift), color));
        float gs = (float)w / 30.0F;

        for (int k = 0; k < 6; k++) {
            double ang = (double)k * Math.PI / 3.0;
            UiFx.centered(
                g,
                this.font,
                Component.literal(c.glyph()),
                (float)x + (float)w / 2.0F + (float)Math.cos(ang) * 1.6F,
                (float)y + (float)h * 0.08F + (float)Math.sin(ang) * 1.6F,
                gs,
                UiFx.argb(a * 0.3F, color),
                false
            );
        }

        UiFx.centered(g, this.font, Component.literal(c.glyph()), (float)x + (float)w / 2.0F, (float)y + (float)h * 0.08F, gs, UiFx.argb(a, 16777215), false);
        UiFx.centered(
            g,
            this.font,
            c.displayName(),
            (float)x + (float)w / 2.0F,
            (float)y + (float)h * 0.36F,
            Math.min(1.05F, (float)w / 100.0F),
            UiFx.argb(a, 16777215),
            true
        );
        Technique fixed = c.fixedTechnique();
        Component tech = fixed != null
            ? fixed.displayName()
            : Component.translatable(c.randomPool() == Technique.Source.SPIRIT ? "intro.fallenroses.random_spirit" : "intro.fallenroses.random_sorcerer");
        UiFx.centered(
            g,
            this.font,
            tech,
            (float)x + (float)w / 2.0F,
            (float)y + (float)h * 0.36F + 11.0F,
            Math.min(0.62F, (float)w / 160.0F),
            UiFx.argb(a, 14207200),
            false
        );
        Component desc = Component.translatable("class.fallenroses." + c.id() + ".desc");
        float scale = Math.min(0.62F, (float)w / 175.0F);
        int used = UiFx.paragraph(g, this.font, desc, (float)(x + 6), (float)y + (float)h * 0.36F + 22.0F, w - 12, scale, UiFx.argb(a * 0.9F, 14209248), true);
        float sy = (float)y + (float)h * 0.36F + 26.0F + (float)used;

        for (Stat s : Stat.values()) {
            if (sy > (float)(y + h - 8)) {
                break;
            }

            UiFx.left(g, this.font, s.displayName(c.usesStamina()), (float)(x + 6), sy, 0.5F, UiFx.argb(a, 11051184), false);
            int bx = x + w / 2;
            int bw = w / 2 - 8;
            g.fill(bx, (int)sy + 1, bx + bw, (int)sy + 3, UiFx.argb(a * 0.6F, 0));
            g.fill(bx, (int)sy + 1, bx + bw * Math.min(5, c.startStat(s)) / 5, (int)sy + 3, UiFx.argb(a, color));
            sy += 6.5F;
        }
    }

    private void renderReveal(GuiGraphics g, float t, float partial) {
        g.fillGradient(0, 0, this.width, this.height, -16448505, -15596534);
        this.petals.render(g, 0.7F);
        boolean landed = this.landedAt >= 0;
        float since = landed ? (float)(this.ticks - this.landedAt) + partial : 0.0F;
        UiFx.centered(
            g,
            this.font,
            Component.translatable(landed ? "intro.fallenroses.technique_awakened" : "intro.fallenroses.technique_rolling"),
            (float)this.width / 2.0F,
            (float)this.height * 0.08F,
            1.6F,
            UiFx.argb(1.0F, 15788276),
            true
        );
        if (!this.pool.isEmpty()) {
            float pos = this.revealPos(partial);
            int cardW = Math.min(150, this.width / 4);
            int cardH = (int)((float)cardW * 1.35F);
            float midY = (float)this.height / 2.0F + 6.0F;
            int base = (int)Math.floor((double)pos);
            float frac = pos - (float)base;
            Integer[] order = new Integer[]{-2, 2, -1, 1, 0, 3};
            Arrays.sort(order, (p, q) -> Float.compare(Math.abs((float)q.intValue() - frac), Math.abs((float)p.intValue() - frac)));
            Integer[] a = order;
            int var14 = order.length;

            for (int var15 = 0; var15 < var14; var15++) {
                int k = a[var15];
                int idx = Math.floorMod(base + k, this.pool.size());
                float off = (float)k - frac;
                float dist = Math.abs(off);
                float sc = 1.0F - Math.min(0.55F, dist * 0.28F);
                boolean winner = landed && k == 0;
                float alpha = Mth.clamp(1.15F - dist * 0.45F, 0.0F, 1.0F) * (landed && k != 0 ? Math.max(0.0F, 1.0F - since / 12.0F) : 1.0F);
                if (!(alpha <= 0.02F)) {
                    float pop = winner ? 1.0F + 0.06F * (float)Math.exp((double)(-since / 5.0F)) * Mth.sin(since * 0.9F) : 1.0F;
                    g.pose().pushPose();
                    g.pose().translate((float)this.width / 2.0F + off * (float)cardW * 0.78F, midY, 0.0F);
                    g.pose().scale(sc * pop, sc * pop, 1.0F);
                    g.pose().translate((float)(-cardW) / 2.0F, (float)(-cardH) / 2.0F, 0.0F);
                    this.drawTechniqueCard(g, this.pool.get(idx), 0, 0, cardW, cardH, alpha, winner, since);
                    g.pose().popPose();
                }
            }

            if (landed && since > 25.0F) {
                float ax = Mth.clamp((since - 25.0F) / 15.0F, 0.0F, 1.0F) * (0.6F + 0.4F * Mth.sin(t * 0.15F));
                UiFx.centered(
                    g,
                    this.font,
                    Component.translatable("intro.fallenroses.click_continue"),
                    (float)this.width / 2.0F,
                    (float)(this.height - 22),
                    0.85F,
                    UiFx.argb(ax, 13156560),
                    false
                );
            }

            if (landed && since < 10.0F) {
                g.fill(0, 0, this.width, this.height, UiFx.argb(0.7F * (1.0F - since / 10.0F), 16777215));
            }

            if (t < 10.0F) {
                g.fill(0, 0, this.width, this.height, UiFx.argb(1.0F - UiFx.easeOut(t / 10.0F), 328711));
            }
        }
    }

    private void drawTechniqueCard(GuiGraphics g, Technique tech, int x, int y, int w, int h, float a, boolean winner, float since) {
        int color = tech.color();
        if (winner) {
            int glow = (int)(4.0F + 6.0F * (0.5F + 0.5F * Mth.sin(since * 0.2F)));
            g.fill(x - glow, y - glow, x + w + glow, y + h + glow, UiFx.argb(0.3F * a, color));
        }

        g.setColor(1.0F, 1.0F, 1.0F, a);
        g.blit(CARD, x, y, w, h, 0.0F, 0.0F, 128, 192, 128, 192);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        UiFx.frame(g, x, y, w, h, UiFx.argb(a, color));
        String glyph = tech.ability(0).glyph();
        float gs = (float)w / 26.0F;
        UiFx.centered(g, this.font, Component.literal(glyph), (float)x + (float)w / 2.0F, (float)y + (float)h * 0.1F, gs, UiFx.argb(a, 16777215), false);
        UiFx.centered(
            g,
            this.font,
            tech.displayName(),
            (float)x + (float)w / 2.0F,
            (float)y + (float)h * 0.42F,
            Math.min(1.2F, (float)w / 110.0F),
            UiFx.argb(a, 16777215),
            true
        );
        if (winner && !(since < 10.0F)) {
            float da = a * Mth.clamp((since - 10.0F) / 12.0F, 0.0F, 1.0F);
            int used = UiFx.paragraph(
                g,
                this.font,
                tech.description(),
                (float)(x + 8),
                (float)y + (float)h * 0.52F,
                w - 16,
                Math.min(0.7F, (float)w / 190.0F),
                UiFx.argb(da * 0.9F, 14209248),
                true
            );
            float sy = (float)y + (float)h * 0.52F + (float)used + 5.0F;

            for (int k = 0; k < 5 && !(sy > (float)(y + h - 9)); k++) {
                Component ab = Component.literal(tech.ability(k).glyph() + "  ").append(tech.ability(k).displayName());
                UiFx.left(g, this.font, ab, (float)(x + 10), sy, 0.6F, UiFx.argb(da, k == 4 ? color : 13156560), false);
                sy += 8.0F;
            }
        }
    }

    private void renderOutro(GuiGraphics g, float t) {
        g.fill(0, 0, this.width, this.height, -16777216);
        float white = t < 15.0F ? UiFx.easeOut(t / 15.0F) : Mth.clamp((60.0F - t) / 25.0F, 0.0F, 1.0F);
        g.fill(0, 0, this.width, this.height, UiFx.argb(white, 16777215));
        Technique tech = this.chosenTechnique != null ? this.chosenTechnique : this.chosenClass.fixedTechnique();
        Component chosen = tech != null
            ? Component.translatable("intro.fallenroses.chosen_with", this.chosenClass.displayName(), tech.displayName())
            : Component.translatable("intro.fallenroses.chosen", this.chosenClass.displayName());
        float textA = Mth.clamp((t - 10.0F) / 10.0F, 0.0F, 1.0F) * Mth.clamp((58.0F - t) / 10.0F, 0.0F, 1.0F);
        UiFx.centered(g, this.font, chosen, (float)this.width / 2.0F, (float)this.height / 2.0F - 8.0F, 1.6F, UiFx.argb(textA, 1380890), false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.phase != IntroScreen.Phase.STORY || key != 32 && key != 256 && key != 257) {
            if (this.phase == IntroScreen.Phase.CLASS && this.selected >= 0 && key == 257) {
                this.confirm();
                return true;
            } else if (this.phase != IntroScreen.Phase.REVEAL || this.landedAt < 0 || this.ticks - this.landedAt <= 20 || key != 257 && key != 32) {
                return super.keyPressed(key, scan, mods);
            } else {
                this.go(IntroScreen.Phase.OUTRO);
                return true;
            }
        } else {
            this.go(IntroScreen.Phase.CLASS);
            UiFx.sound(ModSounds.INTRO_FLASH, 1.2F, 0.6F);
            return true;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.phase == IntroScreen.Phase.STORY) {
            this.go(IntroScreen.Phase.CLASS);
            return true;
        } else if (this.phase == IntroScreen.Phase.REVEAL) {
            if (this.landedAt >= 0 && this.ticks - this.landedAt > 20) {
                this.go(IntroScreen.Phase.OUTRO);
            }

            return true;
        } else if (this.phase != IntroScreen.Phase.CLASS) {
            return false;
        } else {
            if (this.selected >= 0) {
                int[] btn = this.confirmBox();
                if (mx >= (double)btn[0] && mx < (double)(btn[0] + btn[2]) && my >= (double)btn[1] && my < (double)(btn[1] + btn[3])) {
                    this.confirm();
                    return true;
                }
            }

            if (this.hovered >= 0) {
                if (this.selected == this.hovered) {
                    this.confirm();
                } else {
                    this.selected = this.hovered;
                    UiFx.sound(ModSounds.CARD_SELECT, 1.0F, 0.8F);
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private void confirm() {
        this.chosenClass = PlayerClass.values()[this.selected];
        ModNetwork.sendToServer(new SimplePackets.ChooseClass(this.chosenClass.ordinal(), -1));
        UiFx.sound(ModSounds.CLASS_CONFIRM, 1.0F, 1.0F);
        Technique.Source source = this.chosenClass.randomPool();
        if (source != null) {
            this.pool = Technique.pool(source);
            ClientData.technique = null;
            this.spinPos = (float)(Math.random() * (double)this.pool.size());
            this.spinSpeed = 1.6F;
            this.landedAt = -1;
            this.landing = false;
            this.lastTick = Math.round(this.spinPos);
            this.go(IntroScreen.Phase.REVEAL);
            UiFx.sound(ModSounds.INTRO_FLASH, 0.8F, 0.7F);
        } else {
            this.go(IntroScreen.Phase.OUTRO);
        }
    }

    private int[] cardBox(int i, int n) {
        int gap = 8;
        int w = Math.min(118, (this.width - 30 - gap * (n - 1)) / n);
        int h = Math.min((int)((float)w * 1.62F), this.height - 96);
        int total = w * n + gap * (n - 1);
        int x = (this.width - total) / 2 + i * (w + gap);
        int y = (int)((float)this.height * 0.05F) + 36;
        return new int[]{x, y, w, h};
    }

    private int[] confirmBox() {
        int[] c = this.cardBox(0, PlayerClass.values().length);
        int w = 140;
        int h = 20;
        return new int[]{this.width / 2 - w / 2, Math.min(this.height - h - 4, c[1] + c[3] + 10), w, h};
    }

    @Override
    public void renderBackground(GuiGraphics g) {
    }

    private static enum Phase {
        STORY,
        CLASS,
        REVEAL,
        OUTRO;
    }
}
