package com.fallenroses.client.screen;

import com.fallenroses.client.ClientData;
import com.fallenroses.client.Keybinds;
import com.fallenroses.client.hud.AbilityHud;
import com.fallenroses.client.vfx.FRRenderTypes;
import com.fallenroses.network.ModNetwork;
import com.fallenroses.network.SimplePackets;
import com.fallenroses.player.PlayerClass;
import com.fallenroses.player.PlayerData;
import com.fallenroses.player.Progression;
import com.fallenroses.player.Stat;
import com.fallenroses.player.Talent;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.AbilityDef;
import com.fallenroses.technique.Technique;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class CharacterScreen extends Screen {
    private static final int SX = 66;
    private static final int SY = 64;
    private static final int SIDEBAR = 150;
    private static final int MARGIN = 10;
    private static final int GAP = 8;
    private static final int BARK = 1839638;
    private static final int BARK_LIGHT = 3811884;
    private int ticks;
    private final UiFx.Petals petals = new UiFx.Petals(10, 2755088, 1052692);
    private final RandomSource random = RandomSource.create();
    private final List<float[]> motes = new ArrayList<>();
    @Nullable
    private List<FormattedCharSequence> tooltip;
    private float zoom = -1.0F;
    private float panX;
    private float panY;
    private float tZoom = -1.0F;
    private boolean anchored;
    private boolean homing;
    private float ax;
    private float ay;
    private float wx;
    private float wy;
    private long lastFrame;
    private boolean pressedInCanvas;
    private boolean dragging;
    private double pressX;
    private double pressY;
    @Nullable
    private Talent hovered;
    @Nullable
    private Talent lastHovered;

    public CharacterScreen() {
        super(Component.translatable("screen.fallenroses.character"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        this.ticks++;
        this.petals.tick(this.width, this.height);
        this.tickMotes();
    }

    private void tickMotes() {
        PlayerClass cls = ClientData.playerClass;
        if (cls != null && this.ticks % 3 == 0) {
            List<Talent> owned = new ArrayList<>();

            for (Talent t : Talent.visibleFor(cls)) {
                if (ClientData.rank(t) > 0) {
                    owned.add(t);
                }
            }

            if (!owned.isEmpty() && this.motes.size() < 60) {
                Talent tx = owned.get(this.random.nextInt(owned.size()));
                float x = (float)(tx.gx * 66) + (this.random.nextFloat() - 0.5F) * 16.0F;
                float y = (float)(-tx.gy * 64) + (this.random.nextFloat() - 0.5F) * 10.0F;
                this.motes
                    .add(
                        new float[]{
                            x,
                            y,
                            (this.random.nextFloat() - 0.5F) * 0.5F,
                            -0.25F - this.random.nextFloat() * 0.35F,
                            0.0F,
                            (float)(50 + this.random.nextInt(40)),
                            Float.intBitsToFloat(tx.branch == Talent.Branch.CORE ? cls.color() : tx.branch.color)
                        }
                    );
            }
        }

        this.motes.removeIf(m -> {
            m[0] += m[2] + Mth.sin((m[4] + m[1]) * 0.1F) * 0.25F;
            m[1] += m[3];
            return ++m[4] > m[5];
        });
    }

    private int cx0() {
        return 168;
    }

    private int cy0() {
        return 10;
    }

    private int cx1() {
        return this.width - 10;
    }

    private int cy1() {
        return this.height - 10;
    }

    private float fitZoom() {
        float h = 696.0F;
        float w = (float)((2 * Talent.maxAbsGx() + 2) * 66) + 60.0F;
        return Mth.clamp(Math.min((float)(this.cy1() - this.cy0()) / h, (float)(this.cx1() - this.cx0()) / w), 0.45F, 1.3F);
    }

    private float originX() {
        return (float)(this.cx0() + this.cx1()) / 2.0F + this.panX;
    }

    private float originY() {
        return (float)this.cy1() - 58.0F * this.zoom + this.panY;
    }

    private static int radius(Talent t) {
        if (t == Talent.CORE) {
            return 21;
        } else if (t.isCapstone()) {
            return 22;
        } else {
            return t.stat != null ? 14 : 17;
        }
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= (double)this.cx0() && mx < (double)this.cx1() && my >= (double)this.cy0() && my < (double)this.cy1();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        PlayerClass cls = ClientData.playerClass;
        if (this.zoom < 0.0F) {
            this.zoom = this.tZoom = this.fitZoom();
        }

        this.animateView();
        float t = (float)this.ticks + partial;
        float open = UiFx.easeOut(t / 8.0F);
        g.fillGradient(0, 0, this.width, this.height, UiFx.argb(0.8F * open, 328711), UiFx.argb(0.9F * open, 1311754));
        this.petals.render(g, 0.5F * open);
        this.tooltip = null;
        if (cls == null) {
            UiFx.centered(
                g,
                this.font,
                Component.translatable("message.fallenroses.no_class"),
                (float)this.width / 2.0F,
                (float)this.height / 2.0F,
                1.0F,
                -5199688,
                false
            );
        } else {
            Set<Talent> visible = Talent.visibleFor(cls);
            this.renderSidebar(g, mx, my, cls, open, t);
            this.renderCanvas(g, mx, my, cls, visible, open, t, partial);
            if (this.tooltip != null) {
                g.renderTooltip(this.font, this.tooltip, mx, my);
            }
        }
    }

    private void animateView() {
        long now = System.nanoTime();
        float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
        this.lastFrame = now;
        float k = 1.0F - (float)Math.exp((double)(-dt * 14.0F));
        if (Math.abs(this.tZoom - this.zoom) > 1.0E-4F) {
            this.zoom = this.zoom + (this.tZoom - this.zoom) * k;
            if (Math.abs(this.tZoom - this.zoom) < 1.0E-4F) {
                this.zoom = this.tZoom;
            }

            if (this.anchored) {
                this.panX = this.ax - this.wx * this.zoom - (float)(this.cx0() + this.cx1()) / 2.0F;
                this.panY = this.ay - this.wy * this.zoom - (float)this.cy1() + 58.0F * this.zoom;
                this.clampPan();
            }
        } else {
            this.anchored = false;
        }

        if (this.homing) {
            this.panX = this.panX - this.panX * k;
            this.panY = this.panY - this.panY * k;
            if (Math.abs(this.panX) + Math.abs(this.panY) < 0.3F) {
                this.panX = this.panY = 0.0F;
                this.homing = false;
            }
        }
    }

    private void renderSidebar(GuiGraphics g, int mx, int my, PlayerClass cls, float a, float t) {
        int x0 = 10;
        int y0 = 10;
        int w = 150;
        int h = this.height - 20;
        int color = cls.color();
        g.fill(x0, y0, x0 + w, y0 + h, UiFx.argb(0.9F * a, 789008));
        UiFx.frame(g, x0, y0, w, h, UiFx.argb(a * 0.9F, color));
        g.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + 2, UiFx.argb(0.3F * a, 16777215));
        int x = x0 + 8;
        int y = y0 + 8;
        int iw = w - 16;

        for (int k = 0; k < 6; k++) {
            double ang = (double)k * Math.PI / 3.0 + (double)t * 0.02;
            UiFx.centered(
                g,
                this.font,
                Component.literal(cls.glyph()),
                (float)(x + 14) + (float)Math.cos(ang) * 1.5F,
                (float)y + (float)Math.sin(ang) * 1.5F,
                2.6F,
                UiFx.argb(a * 0.3F, color),
                false
            );
        }

        UiFx.centered(g, this.font, Component.literal(cls.glyph()), (float)(x + 14), (float)y, 2.6F, UiFx.argb(a, 16777215), false);
        UiFx.left(g, this.font, cls.displayName(), (float)(x + 34), (float)(y + 2), 0.95F, UiFx.argb(a, 16777215), true);
        Technique tech = ClientData.technique;
        UiFx.left(
            g,
            this.font,
            tech == null ? Component.translatable("technique.fallenroses.none") : tech.displayName(),
            (float)(x + 34),
            (float)(y + 13),
            0.65F,
            UiFx.argb(a, 13156560),
            false
        );
        y += 30;
        int lvl = ClientData.level;
        int next = Progression.xpForNext(lvl);
        float frac = lvl >= 50 ? 1.0F : Mth.clamp((float)ClientData.xp / (float)next, 0.0F, 1.0F);
        UiFx.left(g, this.font, Component.translatable("screen.fallenroses.level", lvl), (float)x, (float)y, 0.8F, UiFx.argb(a, 15788287), true);
        Component xpText = lvl >= 50 ? Component.translatable("screen.fallenroses.max_level") : Component.literal(ClientData.xp + " / " + next);
        UiFx.left(g, this.font, xpText, (float)(x + iw) - (float)this.font.width(xpText) * 0.6F, (float)(y + 1), 0.6F, UiFx.argb(a, 11050160), false);
        g.fill(x, y + 10, x + iw, y + 13, UiFx.argb(a * 0.7F, 0));
        g.fill(x, y + 10, x + (int)((float)iw * frac), y + 13, UiFx.argb(a, color));
        g.fill(x, y + 10, x + (int)((float)iw * frac), y + 11, UiFx.argb(a * 0.5F, 16777215));
        y += 19;
        int pts = ClientData.talentPoints;
        float pulse = pts > 0 ? 0.75F + 0.25F * Mth.sin(t * 0.25F) : 1.0F;
        g.fill(x, y, x + iw, y + 20, UiFx.argb(a * 0.5F, pts > 0 ? 3812360 : 1709602));
        UiFx.frame(g, x, y, iw, 20, UiFx.argb(a * pulse, pts > 0 ? 16767072 : 4208712));
        UiFx.left(g, this.font, Component.translatable("screen.fallenroses.talent_points"), (float)(x + 5), (float)(y + 6), 0.7F, UiFx.argb(a, 14735552), false);
        Component pv = Component.literal(String.valueOf(pts));
        UiFx.left(
            g,
            this.font,
            pv,
            (float)(x + iw - 6) - (float)this.font.width(pv) * 1.2F,
            (float)(y + 4),
            1.2F,
            UiFx.argb(a * pulse, pts > 0 ? 16767072 : 9076880),
            true
        );
        y += 27;
        boolean stamina = cls.usesStamina();
        UiFx.left(g, this.font, Component.translatable("screen.fallenroses.stats"), (float)x, (float)y, 0.65F, UiFx.argb(a, 9076880), false);
        y += 10;

        for (Stat s : Stat.values()) {
            int total = ClientData.stats[s.ordinal()];
            int bonus = PlayerData.talentStatBonus(ClientData.talents, s);
            boolean over = mx >= x - 2 && mx < x + iw + 2 && my >= y - 2 && my < y + 10;
            if (over) {
                g.fill(x - 2, y - 2, x + iw + 2, y + 10, UiFx.argb(a * 0.25F, color));
                this.tooltip = this.font.split(s.description(stamina), 200);
            }

            UiFx.left(g, this.font, s.displayName(stamina), (float)x, (float)y, 0.7F, UiFx.argb(a, 14735592), false);
            Component val = Component.literal(String.valueOf(total));
            float vx = (float)(x + iw - (bonus > 0 ? 26 : 0)) - (float)this.font.width(val) * 0.8F;
            UiFx.left(g, this.font, val, vx, (float)y - 0.5F, 0.8F, UiFx.argb(a, 16777215), true);
            if (bonus > 0) {
                UiFx.left(g, this.font, Component.literal("+" + bonus), (float)(x + iw - 22), (float)y, 0.65F, UiFx.argb(a, 8183946), false);
            }

            y += 12;
        }

        y += 2;
        Component power = Component.translatable(
            stamina ? "screen.fallenroses.stamina_line" : "screen.fallenroses.energy_line", Math.round(ClientData.max), String.format("%.2f", ClientData.power)
        );
        y += UiFx.paragraph(g, this.font, power, (float)x, (float)y, iw, 0.6F, UiFx.argb(a, 11050160), false) + 2;
        float mastery = PlayerData.mastery(ClientData.level);
        Component mLine = Component.translatable("screen.fallenroses.mastery", Math.round(mastery * 100.0F));
        UiFx.left(g, this.font, mLine, (float)x, (float)y, 0.6F, UiFx.argb(a, 13154544), false);
        y += 8;
        g.fill(x, y, x + iw, y + 3, UiFx.argb(a * 0.6F, 0));
        g.fill(x, y, x + Math.round((float)iw * mastery), y + 3, UiFx.argb(a, tech != null ? tech.color() : 10125567));
        y += 7;
        if (cls == PlayerClass.VESSEL) {
            UiFx.left(
                g, this.font, Component.translatable("screen.fallenroses.fingers", ClientData.fingers), (float)x, (float)y, 0.6F, UiFx.argb(a, 12599376), false
            );
            y += 9;
            UiFx.left(
                g,
                this.font,
                Component.translatable("screen.fallenroses.paintings", ClientData.paintings),
                (float)x,
                (float)y,
                0.6F,
                UiFx.argb(a, 10123824),
                false
            );
            y += 9;
        } else if (cls == PlayerClass.CURSE_MANIPULATOR) {
            UiFx.left(
                g,
                this.font,
                Component.translatable("screen.fallenroses.absorbed", ClientData.absorbed),
                (float)x,
                (float)y,
                0.6F,
                UiFx.argb(a, 3844744),
                false
            );
            y += 9;
        }

        y += 6;
        if (tech != null && y + 40 <= y0 + h) {
            UiFx.left(g, this.font, Component.translatable("screen.fallenroses.tab_technique"), (float)x, (float)y, 0.65F, UiFx.argb(a, 9076880), false);
            y += 10;
            int size = Math.min(24, (iw - 12) / 5);

            for (int slot = 0; slot < 5; slot++) {
                AbilityDef ab = tech.ability(slot);
                int ix = x + slot * (size + 3);
                boolean unlocked = ClientData.isSlotUnlocked(slot);
                boolean ult = slot == 4;
                g.fill(ix - 1, y - 1, ix + size + 1, y + size + 1, UiFx.argb(a, ult ? tech.color() : 3814466));
                g.blit(AbilityHud.icon(ab.id()), ix, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
                if (!unlocked) {
                    g.fill(ix, y, ix + size, y + size, -1073741824);
                    int lx = ix + size / 2;
                    int ly = y + size / 2;
                    UiFx.frame(g, lx - 2, ly - 4, 5, 4, UiFx.argb(a, 10518656));
                    g.fill(lx - 3, ly - 1, lx + 4, ly + 4, UiFx.argb(a, 10518656));
                }

                if (mx >= ix && mx < ix + size && my >= y && my < y + size) {
                    this.tooltip = this.abilityTooltip(tech, slot, stamina);
                }
            }
        }
    }

    private List<FormattedCharSequence> abilityTooltip(Technique tech, int slot, boolean stamina) {
        AbilityDef ab = tech.ability(slot);
        List<FormattedCharSequence> tip = new ArrayList<>();
        tip.add(ab.displayName().withStyle(s -> s.withColor(tech.color()).withBold(true)).getVisualOrderText());
        tip.add(
            Component.translatable(
                    stamina ? "screen.fallenroses.ability_meta_stamina" : "screen.fallenroses.ability_meta",
                    Math.round(ab.cost() * PlayerData.costMultiplier(ClientData.talents)),
                    String.format("%.1f", (float)ab.cooldown() / 20.0F)
                )
                .withStyle(ChatFormatting.GRAY)
                .getVisualOrderText()
        );
        tip.add(Component.literal(Keybinds.SLOTS[slot].getTranslatedKeyMessage().getString()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
        tip.addAll(this.font.split(ab.description(), 210));
        if (!ClientData.isSlotUnlocked(slot)) {
            Talent need = Talent.forSlot(slot);
            if (need != null && ClientData.rank(need) == 0) {
                tip.add(Component.translatable("screen.fallenroses.needs_talent", need.displayName()).withStyle(ChatFormatting.RED).getVisualOrderText());
            }

            if (ab.fingers() > ClientData.fingers) {
                tip.add(
                    Component.translatable("screen.fallenroses.need_fingers", ClientData.fingers, ab.fingers())
                        .withStyle(ChatFormatting.RED)
                        .getVisualOrderText()
                );
            }
        }

        return tip;
    }

    private void renderCanvas(GuiGraphics g, int mx, int my, PlayerClass cls, Set<Talent> visible, float a, float t, float partial) {
        int x0 = this.cx0();
        int y0 = this.cy0();
        int x1 = this.cx1();
        int y1 = this.cy1();
        g.fill(x0, y0, x1, y1, UiFx.argb(0.9F * a, 526091));
        g.fillGradient(x0, y0, x1, (y0 + y1) / 2, UiFx.argb(0.5F * a, 1050650), UiFx.argb(0.0F, 1050650));
        UiFx.frame(g, x0, y0, x1 - x0, y1 - y0, UiFx.argb(a * 0.7F, 3811908));
        g.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        float step = 26.0F * this.zoom;
        float gx0 = this.originX() % step;
        float gy0 = this.originY() % step;

        for (float gx = (float)x0 + gx0; gx < (float)x1; gx += step) {
            for (float gy = (float)y0 + gy0; gy < (float)y1; gy += step) {
                g.fill((int)gx, (int)gy, (int)gx + 1, (int)gy + 1, UiFx.argb(a * 0.08F, 11575504));
            }
        }

        g.pose().pushPose();
        g.pose().translate(this.originX(), this.originY(), 0.0F);
        g.pose().scale(this.zoom, this.zoom, 1.0F);
        Technique tech = ClientData.technique;
        this.branchTitle(g, Talent.Branch.BODY, -5.0F, 11.4F, Component.translatable("talent.branch.fallenroses.body"), "体", a);
        this.branchTitle(
            g,
            Talent.Branch.TECHNIQUE,
            0.0F,
            (float)Talent.maxGy() + 1.4F,
            tech != null
                ? tech.displayName().copy().withStyle(s -> s.withColor(Talent.Branch.TECHNIQUE.color))
                : Component.translatable("talent.branch.fallenroses.technique"),
            "術",
            a
        );
        this.branchTitle(
            g,
            Talent.Branch.ENERGY,
            5.0F,
            11.4F,
            Component.translatable(cls.usesStamina() ? "talent.branch.fallenroses.stamina" : "talent.branch.fallenroses.energy"),
            cls.usesStamina() ? "力" : "気",
            a
        );

        for (int slot = 1; slot < 5; slot++) {
            Talent seal = Talent.forSlot(slot);
            if (seal != null) {
                float y = (float)(-seal.gy * 64);
                int c = ClientData.rank(seal) > 0 ? 16767072 : 5917296;
                g.fill(-171, (int)y, -105, (int)y + 1, UiFx.argb(a * 0.6F, c));
                g.fill(105, (int)y, 171, (int)y + 1, UiFx.argb(a * 0.6F, c));
                UiFx.centered(
                    g,
                    this.font,
                    Component.translatable("screen.fallenroses.seal_level", slot + 1, seal.requiredLevel),
                    138.59999F,
                    y - 9.0F,
                    0.6F,
                    UiFx.argb(a, c),
                    false
                );
            }
        }

        int[] ranks = ClientData.talents;
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(FRRenderTypes.GUI_SHAPES);
        this.roots(m, vc, cls, a, t);

        for (Talent node : visible) {
            for (Talent p : node.parents()) {
                if (p != null && visible.contains(p)) {
                    this.limb(m, vc, p, node, ranks, a, t);
                }
            }
        }

        for (float[] mo : this.motes) {
            float life = mo[4] / mo[5];
            float al = a * Mth.clamp(life * 4.0F, 0.0F, 1.0F) * (1.0F - life);
            diamond(m, vc, mo[0], mo[1], 2.2F, 3.4F, mo[4] * 0.08F, UiFx.argb(al * 0.9F, Float.floatToIntBits(mo[6])));
            disc(m, vc, mo[0], mo[1], 4.5F, 8, UiFx.argb(al * 0.15F, Float.floatToIntBits(mo[6])));
        }

        g.flush();
        float tmx = ((float)mx - this.originX()) / this.zoom;
        float tmy = ((float)my - this.originY()) / this.zoom;
        this.hovered = null;
        boolean mouseInCanvas = this.inCanvas((double)mx, (double)my);

        for (Talent node : visible) {
            float nx = (float)(node.gx * 66);
            float ny = (float)(-node.gy * 64);
            int r = radius(node);
            boolean over = mouseInCanvas && !this.dragging && (tmx - nx) * (tmx - nx) + (tmy - ny) * (tmy - ny) <= (float)((r + 3) * (r + 3));
            if (over) {
                this.hovered = node;
            }

            this.node(g, node, cls, tech, ranks, over, a, t);
        }

        g.pose().popPose();
        UiFx.left(
            g,
            this.font,
            Component.literal("才能 ").append(Component.translatable("screen.fallenroses.talent_tree")),
            (float)(x0 + 8),
            (float)(y0 + 7),
            1.0F,
            UiFx.argb(a, 15788287),
            true
        );
        UiFx.left(
            g, this.font, Component.translatable("screen.fallenroses.tree_hint"), (float)(x0 + 8), (float)(y1 - 11), 0.6F, UiFx.argb(a * 0.8F, 9076880), false
        );
        this.renderResetButton(g, mx, my, a);
        g.disableScissor();
        if (this.hovered != this.lastHovered) {
            if (this.hovered != null) {
                UiFx.sound(ModSounds.CARD_HOVER, 1.4F + (float)this.hovered.gy * 0.05F, 0.25F);
            }

            this.lastHovered = this.hovered;
        }

        if (this.hovered != null) {
            this.tooltip = this.talentTooltip(this.hovered, cls, tech);
        }
    }

    private void branchTitle(GuiGraphics g, Talent.Branch branch, float gx, float topGy, Component title, String kanji, float a) {
        float x = gx * 66.0F;
        float y = -topGy * 64.0F;
        UiFx.centered(g, this.font, Component.literal(kanji), x, -topGy * 0.55F * 64.0F, 9.0F, UiFx.argb(a * 0.045F, branch.color), false);
        UiFx.centered(g, this.font, title, x, y, 1.35F, UiFx.argb(a, branch.color), true);
        int lw = 52;
        g.fill((int)x - lw, (int)y + 14, (int)x + lw, (int)y + 15, UiFx.argb(a * 0.5F, branch.color));
        g.fill((int)x - 3, (int)y + 12, (int)x + 3, (int)y + 17, UiFx.argb(a * 0.8F, branch.color));
    }

    private void limb(Matrix4f m, VertexConsumer vc, Talent from, Talent to, int[] ranks, float a, float t) {
        CharacterScreen.LimbState state = ranks[from.ordinal()] > 0 && ranks[to.ordinal()] > 0
            ? CharacterScreen.LimbState.OWNED
            : (ranks[from.ordinal()] > 0 ? CharacterScreen.LimbState.OPEN : CharacterScreen.LimbState.LOCKED);
        int color = to.branch.color;
        float ax = (float)(from.gx * 66);
        float ay = (float)(-from.gy * 64);
        float bx = (float)(to.gx * 66);
        float by = (float)(-to.gy * 64);
        float dy = by - ay;
        float bend = Math.signum(bx - ax) * 10.0F + (bx == ax ? (from.gx == 0 ? 0.0F : Math.signum((float)from.gx) * 8.0F) : 0.0F);
        float[] p0 = new float[]{ax, ay};
        float[] p1 = new float[]{ax + bend * 0.2F, ay + dy * 0.55F};
        float[] p2 = new float[]{bx - bend * 0.4F, by - dy * 0.45F};
        float[] p3 = new float[]{bx, by};
        int n = 22;
        float[][] pts = new float[n + 1][];

        for (int i = 0; i <= n; i++) {
            pts[i] = bezier(p0, p1, p2, p3, (float)i / (float)n);
        }

        float w0 = Math.max(4.5F, 11.0F - (float)from.gy * 1.2F);
        float w1 = Math.max(3.0F, w0 * 0.55F);
        if (state == CharacterScreen.LimbState.OWNED) {
            strip(m, vc, pts, w0 + 7.0F, w1 + 6.0F, UiFx.argb(a * 0.12F, color), UiFx.argb(a * 0.08F, color));
        }

        int bark = state == CharacterScreen.LimbState.LOCKED ? 1182994 : 1839638;
        strip(m, vc, pts, w0, w1, UiFx.argb(a, bark), UiFx.argb(a, bark));
        float[][] shifted = new float[pts.length][];

        for (int i = 0; i < pts.length; i++) {
            shifted[i] = new float[]{pts[i][0] - 0.8F, pts[i][1] - 0.8F};
        }

        strip(
            m,
            vc,
            shifted,
            w0 * 0.25F,
            w1 * 0.25F,
            UiFx.argb(a * (state == CharacterScreen.LimbState.LOCKED ? 0.25F : 0.5F), 3811884),
            UiFx.argb(a * 0.3F, 3811884)
        );
        switch (state) {
            case OWNED:
                strip(m, vc, pts, w0 * 0.38F, w1 * 0.4F, UiFx.argb(a, color), UiFx.argb(a, color));
                strip(m, vc, pts, w0 * 0.14F, w1 * 0.14F, UiFx.argb(a * 0.9F, 16777215), UiFx.argb(a * 0.6F, 16777215));

                for (int k = 0; k < 2; k++) {
                    float f = (t * 0.018F + (float)k * 0.5F + (float)from.ordinal() * 0.13F) % 1.0F;
                    float[] q = bezier(p0, p1, p2, p3, f);
                    disc(m, vc, q[0], q[1], 3.2F, 8, UiFx.argb(a, 16777215));
                    disc(m, vc, q[0], q[1], 6.5F, 10, UiFx.argb(a * 0.25F, color));
                }

                for (int k = 0; k < 2; k++) {
                    float f = 0.35F + (float)k * 0.3F;
                    float[] q = bezier(p0, p1, p2, p3, f);
                    float[] q2 = bezier(p0, p1, p2, p3, f + 0.02F);
                    float nx = -(q2[1] - q[1]);
                    float ny = q2[0] - q[0];
                    float len = (float)Math.sqrt((double)(nx * nx + ny * ny)) + 1.0E-4F;
                    float side = k == 0 ? 1.0F : -1.0F;
                    float lx = q[0] + nx / len * side * (w0 * 0.6F + 4.0F);
                    float ly = q[1] + ny / len * side * (w0 * 0.6F + 4.0F);
                    float sway = Mth.sin(t * 0.06F + (float)from.ordinal() + (float)k) * 0.25F;
                    diamond(
                        m,
                        vc,
                        lx,
                        ly,
                        3.2F,
                        7.0F,
                        (float)Math.atan2((double)(ny * side), (double)(nx * side)) + sway,
                        UiFx.argb(a * 0.9F, mix(color, 657928, 0.45F))
                    );
                    diamond(
                        m,
                        vc,
                        lx,
                        ly,
                        1.2F,
                        5.0F,
                        (float)Math.atan2((double)(ny * side), (double)(nx * side)) + sway,
                        UiFx.argb(a * 0.6F, mix(color, 16777215, 0.3F))
                    );
                }
                break;
            case OPEN:
                strip(m, vc, pts, w0 * 0.3F, w1 * 0.3F, UiFx.argb(a * (0.3F + 0.2F * Mth.sin(t * 0.15F)), color), UiFx.argb(a * 0.15F, color));
            case LOCKED:
        }
    }

    private void roots(Matrix4f m, VertexConsumer vc, PlayerClass cls, float a, float t) {
        float[][] dirs = new float[][]{{-70.0F, 34.0F}, {-36.0F, 46.0F}, {0.0F, 52.0F}, {38.0F, 44.0F}, {72.0F, 30.0F}};

        for (int i = 0; i < dirs.length; i++) {
            float[] p0 = new float[]{0.0F, 4.0F};
            float[] p3 = new float[]{dirs[i][0], dirs[i][1]};
            float[] p1 = new float[]{dirs[i][0] * 0.2F, 22.0F};
            float[] p2 = new float[]{dirs[i][0] * 0.8F, dirs[i][1] - 6.0F};
            float[][] pts = new float[15][];

            for (int k = 0; k < pts.length; k++) {
                pts[k] = bezier(p0, p1, p2, p3, (float)k / 14.0F);
            }

            strip(m, vc, pts, 8.0F, 1.5F, UiFx.argb(a, 1839638), UiFx.argb(a * 0.6F, 1839638));
            strip(m, vc, pts, 2.0F, 0.5F, UiFx.argb(a * (0.35F + 0.15F * Mth.sin(t * 0.08F + (float)i)), cls.color()), UiFx.argb(0.0F, cls.color()));
        }

        disc(m, vc, 0.0F, 0.0F, 34.0F, 24, UiFx.argb(a * 0.07F, cls.color()));
    }

    private static float[] bezier(float[] p0, float[] p1, float[] p2, float[] p3, float t) {
        float u = 1.0F - t;
        float a = u * u * u;
        float b = 3.0F * u * u * t;
        float c = 3.0F * u * t * t;
        float d = t * t * t;
        return new float[]{a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]};
    }

    private static void strip(Matrix4f m, VertexConsumer vc, float[][] pts, float w0, float w1, int c0, int c1) {
        int n = pts.length - 1;
        float[] prevL = null;
        float[] prevR = null;
        int prevC = c0;

        for (int i = 0; i <= n; i++) {
            float f = (float)i / (float)n;
            int ia = Math.max(0, i - 1);
            int ib = Math.min(n, i + 1);
            float dx = pts[ib][0] - pts[ia][0];
            float dy = pts[ib][1] - pts[ia][1];
            float len = (float)Math.sqrt((double)(dx * dx + dy * dy)) + 1.0E-4F;
            float nx = -dy / len;
            float ny = dx / len;
            float w = Mth.lerp(f, w0, w1) * 0.5F;
            float[] l = new float[]{pts[i][0] + nx * w, pts[i][1] + ny * w};
            float[] r = new float[]{pts[i][0] - nx * w, pts[i][1] - ny * w};
            int c = lerpColor(c0, c1, f);
            if (prevL != null) {
                vc.vertex(m, prevL[0], prevL[1], 0.0F).color(prevC).endVertex();
                vc.vertex(m, prevR[0], prevR[1], 0.0F).color(prevC).endVertex();
                vc.vertex(m, r[0], r[1], 0.0F).color(c).endVertex();
                vc.vertex(m, l[0], l[1], 0.0F).color(c).endVertex();
            }

            prevL = l;
            prevR = r;
            prevC = c;
        }
    }

    private static void polygon(Matrix4f m, VertexConsumer vc, float cx, float cy, float r, int sides, float rot, int argb) {
        if ((argb >>> 24 & 0xFF) >= 3 && !(r <= 0.0F)) {
            for (int i = 0; i < sides; i++) {
                double a0 = (double)rot + (double)i * Math.PI * 2.0 / (double)sides;
                double a1 = (double)rot + (double)(i + 1) * Math.PI * 2.0 / (double)sides;
                vc.vertex(m, cx, cy, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx, cy, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx + (float)Math.cos(a1) * r, cy + (float)Math.sin(a1) * r, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx + (float)Math.cos(a0) * r, cy + (float)Math.sin(a0) * r, 0.0F).color(argb).endVertex();
            }
        }
    }

    private static void disc(Matrix4f m, VertexConsumer vc, float cx, float cy, float r, int sides, int argb) {
        polygon(m, vc, cx, cy, r, sides, 0.0F, argb);
    }

    private static void ring(Matrix4f m, VertexConsumer vc, float cx, float cy, float r0, float r1, int sides, float rot, int argb) {
        if ((argb >>> 24 & 0xFF) >= 3) {
            for (int i = 0; i < sides; i++) {
                double a0 = (double)rot + (double)i * Math.PI * 2.0 / (double)sides;
                double a1 = (double)rot + (double)(i + 1) * Math.PI * 2.0 / (double)sides;
                float c0 = (float)Math.cos(a0);
                float s0 = (float)Math.sin(a0);
                float c1 = (float)Math.cos(a1);
                float s1 = (float)Math.sin(a1);
                vc.vertex(m, cx + c0 * r0, cy + s0 * r0, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx + c1 * r0, cy + s1 * r0, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx + c1 * r1, cy + s1 * r1, 0.0F).color(argb).endVertex();
                vc.vertex(m, cx + c0 * r1, cy + s0 * r1, 0.0F).color(argb).endVertex();
            }
        }
    }

    private static void diamond(Matrix4f m, VertexConsumer vc, float cx, float cy, float w, float h, float angle, int argb) {
        float ca = (float)Math.cos((double)angle);
        float sa = (float)Math.sin((double)angle);
        vc.vertex(m, cx + ca * h, cy + sa * h, 0.0F).color(argb).endVertex();
        vc.vertex(m, cx - sa * w, cy + ca * w, 0.0F).color(argb).endVertex();
        vc.vertex(m, cx - ca * h, cy - sa * h, 0.0F).color(argb).endVertex();
        vc.vertex(m, cx + sa * w, cy - ca * w, 0.0F).color(argb).endVertex();
    }

    private void node(GuiGraphics g, Talent node, PlayerClass cls, @Nullable Technique tech, int[] ranks, boolean over, float a, float t) {
        float cx = (float)(node.gx * 66);
        float cy = (float)(-node.gy * 64);
        int r = radius(node);
        int rank = ranks[node.ordinal()];
        boolean owned = rank > 0;
        boolean maxed = rank >= node.maxRank;
        String block = node.blockReason(cls, ClientData.level, ClientData.talentPoints, ranks, tech, ClientData.fingers);
        boolean buyable = block == null;
        boolean reachable = node.parentsMet(ranks, cls) || node == Talent.CORE;
        int color = node == Talent.CORE ? cls.color() : node.branch.color;
        int sides = node == Talent.CORE ? 12 : (node.isCapstone() ? 4 : (node.stat != null ? 24 : 8));
        float rot = node.isCapstone() ? (float) (Math.PI / 4) : (node.stat == null ? (float) (Math.PI / 8) : 0.0F);
        if (node.isCapstone()) {
            rot = 0.0F;
        }

        float bought = boughtProgress(node);
        float scale = over ? 1.12F : 1.0F;
        if (bought >= 0.0F) {
            scale += 0.35F * (1.0F - UiFx.easeOut(bought)) * Mth.sin(bought * (float) Math.PI);
        }

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        Matrix4f m = g.pose().last().pose();
        VertexConsumer vc = g.bufferSource().getBuffer(FRRenderTypes.GUI_SHAPES);
        if (owned) {
            float glow = 0.2F + 0.07F * Mth.sin(t * 0.12F + (float)node.ordinal());
            polygon(m, vc, 0.0F, 0.0F, (float)(r + 12), sides, rot, UiFx.argb(a * glow * 0.35F, color));
            polygon(m, vc, 0.0F, 0.0F, (float)(r + 6), sides, rot, UiFx.argb(a * glow, color));
        } else if (buyable) {
            float p = 0.5F + 0.5F * Mth.sin(t * 0.3F);
            ring(m, vc, 0.0F, 0.0F, (float)(r + 4), (float)r + 6.5F, Math.max(sides, 16), rot, UiFx.argb(a * (0.35F + 0.45F * p), 16767072));
        }

        if (node.strict()) {
            float spin = -t * 0.01F;
            ring(m, vc, 0.0F, 0.0F, (float)(r + 9), (float)r + 10.5F, 8, spin, UiFx.argb(a * (owned ? 0.9F : 0.45F), owned ? 16767072 : color));
            ring(m, vc, 0.0F, 0.0F, (float)(r + 13), (float)r + 13.8F, 32, 0.0F, UiFx.argb(a * (owned ? 0.6F : 0.25F), owned ? 16767072 : color));
        }

        if (node.isCapstone() || node == Talent.CORE) {
            float spin = t * (node == Talent.CORE ? 0.02F : 0.012F);
            ring(m, vc, 0.0F, 0.0F, (float)(r + 7), (float)r + 8.5F, 32, spin, UiFx.argb(a * (owned ? 0.8F : 0.3F), color));

            for (int k = 0; k < 8; k++) {
                double ang = (double)spin + (double)k * Math.PI / 4.0;
                diamond(
                    m,
                    vc,
                    (float)Math.cos(ang) * (float)(r + 8),
                    (float)Math.sin(ang) * (float)(r + 8),
                    1.6F,
                    3.5F,
                    (float)ang,
                    UiFx.argb(a * (owned ? 0.9F : 0.35F), color)
                );
            }
        }

        int border = maxed ? 16777215 : (owned ? color : (buyable ? 16767072 : (reachable ? mix(color, 3024950, 0.55F) : 3024950)));
        polygon(m, vc, 0.0F, 0.0F, (float)r + 2.5F, sides, rot, UiFx.argb(a, border));
        polygon(m, vc, 0.0F, 0.0F, (float)r, sides, rot, UiFx.argb(a, owned ? mix(color, 657424, 0.62F) : 1052181));
        polygon(m, vc, 0.0F, (float)(-r) * 0.25F, (float)r * 0.62F, sides, rot, UiFx.argb(a * (owned ? 0.16F : 0.05F), 16777215));
        if (node == Talent.CORE) {
            for (int k = 0; k < 12; k++) {
                double ang = (double)k * Math.PI / 6.0 + (double)t * 0.03;
                disc(m, vc, (float)Math.cos(ang) * (float)(r + 13), (float)Math.sin(ang) * (float)(r + 13), 1.3F, 6, UiFx.argb(a * 0.8F, cls.color()));
            }
        }

        g.flush();
        String glyph = node == Talent.CORE ? cls.glyph() : (node.slot() > 0 && tech != null ? tech.ability(node.slot()).glyph() : node.glyph);
        int glyphColor = owned ? 16777215 : (buyable ? 15786160 : (reachable ? 9076880 : 4867154));
        float gs = !node.isCapstone() && node != Talent.CORE ? (node.stat != null ? 1.3F : 1.55F) : 1.8F;
        UiFx.centered(g, this.font, Component.literal(glyph), 0.0F, -4.0F * gs, gs, UiFx.argb(a, glyphColor), owned);
        g.pose().popPose();
        float under = cy + (float)r + 6.0F;
        if (node.maxRank > 1) {
            int pw = 5;
            int gap = 2;
            int total = node.maxRank * pw + (node.maxRank - 1) * gap;
            int px = (int)(cx - (float)total / 2.0F);
            int py = (int)under;

            for (int i = 0; i < node.maxRank; i++) {
                int x = px + i * (pw + gap);
                g.fill(x - 1, py - 1, x + pw + 1, py + 4, UiFx.argb(a, 0));
                g.fill(x, py, x + pw, py + 3, UiFx.argb(a, i < rank ? color : 3024950));
            }

            under += 6.0F;
        }

        if (owned || !"level".equals(block) && (!node.strict() || ClientData.level >= node.requiredLevel)) {
            if (!owned && node.fingersNeeded(tech) > 0) {
                int need = node.fingersNeeded(tech);
                UiFx.centered(
                    g,
                    this.font,
                    Component.literal("指 " + ClientData.fingers + "/" + need),
                    cx,
                    under,
                    0.6F,
                    UiFx.argb(a, ClientData.fingers >= need ? 8183946 : 14696528),
                    false
                );
            }
        } else {
            UiFx.centered(
                g, this.font, Component.translatable("screen.fallenroses.level_short", node.requiredLevel), cx, under, 0.6F, UiFx.argb(a, 12607584), false
            );
        }

        if (node.slot() > 0 && tech != null) {
            UiFx.centered(
                g,
                this.font,
                tech.ability(node.slot()).displayName(),
                cx,
                cy - (float)r - 12.0F,
                0.6F,
                UiFx.argb(a * (owned ? 1.0F : 0.6F), owned ? 16777215 : 11050160),
                false
            );
        }

        if (bought >= 0.0F) {
            float e = UiFx.easeOut(bought);
            Matrix4f m2 = g.pose().last().pose();
            VertexConsumer vc2 = g.bufferSource().getBuffer(FRRenderTypes.GUI_SHAPES);
            float rr = (float)(r + 6) + e * 40.0F;
            ring(m2, vc2, cx, cy, rr - 1.5F, rr, 40, 0.0F, UiFx.argb(a * (1.0F - e), 16777215));
            ring(m2, vc2, cx, cy, rr - 5.0F, rr - 3.0F, 40, 0.0F, UiFx.argb(a * (1.0F - e) * 0.7F, color));

            for (int k = 0; k < 12; k++) {
                double ang = (double)k * Math.PI / 6.0 + (double)node.ordinal();
                float d = (float)r + e * (float)(30 + k % 3 * 10);
                diamond(
                    m2,
                    vc2,
                    cx + (float)Math.cos(ang) * d,
                    cy + (float)Math.sin(ang) * d,
                    1.5F,
                    4.0F,
                    (float)ang,
                    UiFx.argb(a * (1.0F - e), k % 2 == 0 ? 16777215 : color)
                );
            }

            g.flush();
        }
    }

    private static float boughtProgress(Talent node) {
        long since = System.currentTimeMillis() - ClientData.TalentEvents.boughtAt;
        if (since > 900L) {
            return -1.0F;
        } else {
            for (int i : ClientData.TalentEvents.lastBought) {
                if (i == node.ordinal()) {
                    return (float)since / 900.0F;
                }
            }

            return -1.0F;
        }
    }

    private static int mix(int c1, int c2, float k) {
        int r = (int)Mth.lerp(k, (float)(c1 >> 16 & 0xFF), (float)(c2 >> 16 & 0xFF));
        int gg = (int)Mth.lerp(k, (float)(c1 >> 8 & 0xFF), (float)(c2 >> 8 & 0xFF));
        int b = (int)Mth.lerp(k, (float)(c1 & 0xFF), (float)(c2 & 0xFF));
        return r << 16 | gg << 8 | b;
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (int)Mth.lerp(t, (float)(a >>> 24 & 0xFF), (float)(b >>> 24 & 0xFF));
        return aa << 24 | mix(a & 16777215, b & 16777215, t);
    }

    private List<FormattedCharSequence> talentTooltip(Talent node, PlayerClass cls, @Nullable Technique tech) {
        List<FormattedCharSequence> tip = new ArrayList<>();
        int rank = ClientData.rank(node);
        boolean abilityNode = node.slot() > 0 && tech != null;
        MutableComponent title = (node == Talent.CORE ? cls.displayName().copy() : (abilityNode ? tech.ability(node.slot()).displayName() : node.displayName()))
            .withStyle(s -> s.withColor(node == Talent.CORE ? cls.color() : node.branch.color).withBold(true));
        tip.add(title.getVisualOrderText());
        if (node == Talent.CORE) {
            tip.addAll(this.font.split(Component.translatable("talent.fallenroses.core.desc").withStyle(ChatFormatting.GRAY), 230));
            return tip;
        } else {
            if (abilityNode) {
                tip.add(node.displayName().withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            }

            tip.add(
                (node.maxRank > 1
                        ? Component.translatable("screen.fallenroses.rank", rank, node.maxRank)
                        : Component.translatable(rank > 0 ? "screen.fallenroses.learned" : "screen.fallenroses.not_learned"))
                    .withStyle(rank > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .getVisualOrderText()
            );
            tip.addAll(this.font.split((abilityNode ? tech.ability(node.slot()).description() : node.description()).withStyle(ChatFormatting.GRAY), 230));
            if (node.stat != null) {
                tip.add(
                    Component.translatable("screen.fallenroses.stat_bonus", node.statPerRank, node.stat.displayName(cls.usesStamina()), rank * node.statPerRank)
                        .withStyle(ChatFormatting.AQUA)
                        .getVisualOrderText()
                );
            }

            int fingers = node.fingersNeeded(tech);
            if (fingers > 0) {
                tip.add(
                    Component.translatable("screen.fallenroses.need_fingers", ClientData.fingers, fingers)
                        .withStyle(ClientData.fingers >= fingers ? ChatFormatting.GREEN : ChatFormatting.RED)
                        .getVisualOrderText()
                );
            }

            String block = node.blockReason(cls, ClientData.level, ClientData.talentPoints, ClientData.talents, tech, ClientData.fingers);
            if (block == null) {
                tip.add(Component.translatable("screen.fallenroses.click_learn", node.cost).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
            } else if (!"fingers".equals(block)) {
                Component why = switch (block) {
                    case "maxed" -> Component.translatable("screen.fallenroses.maxed").withStyle(ChatFormatting.GOLD);
                    case "level" -> Component.translatable("screen.fallenroses.need_level", node.requiredLevel).withStyle(ChatFormatting.RED);
                    case "parent" -> Component.translatable("screen.fallenroses.need_parent").withStyle(ChatFormatting.RED);
                    case "parent_full" -> Component.translatable("screen.fallenroses.need_parent_full").withStyle(ChatFormatting.RED);
                    case "points" -> Component.translatable("screen.fallenroses.need_points", node.cost).withStyle(ChatFormatting.RED);
                    default -> Component.translatable("screen.fallenroses.not_for_class").withStyle(ChatFormatting.RED);
                };
                tip.add(why.getVisualOrderText());
            }

            if (node.strict()) {
                tip.add(Component.translatable("screen.fallenroses.seal_needs", node.requiredLevel).withStyle(ChatFormatting.GOLD).getVisualOrderText());

                for (Talent p : node.parents()) {
                    if (p != null && p.availableFor(cls)) {
                        int pr = ClientData.rank(p);
                        tip.add(
                            Component.literal(" • ")
                                .append(p.displayName())
                                .append(" " + pr + "/" + p.maxRank)
                                .withStyle(pr >= p.maxRank ? ChatFormatting.GREEN : ChatFormatting.RED)
                                .getVisualOrderText()
                        );
                    }
                }
            }

            return tip;
        }
    }

    private int[] resetRect() {
        int w = 80;
        int h = 16;
        return new int[]{this.cx1() - w - 6, this.cy1() - h - 6, w, h};
    }

    private void renderResetButton(GuiGraphics g, int mx, int my, float a) {
        int[] r = this.resetRect();
        boolean any = false;

        for (Talent t : Talent.values()) {
            if (t != Talent.CORE && ClientData.rank(t) > 0) {
                any = true;
            }
        }

        boolean over = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
        boolean armed = over && any && hasShiftDown();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], UiFx.argb(a * (armed ? 0.85F : (over ? 0.45F : 0.3F)), armed ? 8396848 : 3156024));
        UiFx.frame(g, r[0], r[1], r[2], r[3], UiFx.argb(a * (any ? 0.8F : 0.3F), 12599376));
        UiFx.centered(
            g,
            this.font,
            Component.translatable("screen.fallenroses.reset_talents"),
            (float)r[0] + (float)r[2] / 2.0F,
            (float)r[1] + 4.5F,
            0.7F,
            UiFx.argb(a * (any ? 1.0F : 0.4F), 16777215),
            false
        );
        if (over) {
            this.tooltip = this.font.split(Component.translatable("screen.fallenroses.reset_hint"), 200);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (ClientData.playerClass == null) {
            return super.mouseClicked(mx, my, button);
        } else {
            int[] r = this.resetRect();
            if (button == 0 && mx >= (double)r[0] && mx < (double)(r[0] + r[2]) && my >= (double)r[1] && my < (double)(r[1] + r[3])) {
                if (hasShiftDown()) {
                    ModNetwork.sendToServer(new SimplePackets.ResetTalents());
                } else {
                    UiFx.sound(ModSounds.UI_ERROR, 1.0F, 0.4F);
                }

                return true;
            } else if (this.inCanvas(mx, my)) {
                this.pressedInCanvas = true;
                this.dragging = false;
                this.pressX = mx;
                this.pressY = my;
                return true;
            } else {
                return super.mouseClicked(mx, my, button);
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.pressedInCanvas) {
            if (!this.dragging && Math.abs(mx - this.pressX) + Math.abs(my - this.pressY) > 4.0) {
                this.dragging = true;
            }

            if (this.dragging) {
                this.anchored = this.homing = false;
                this.panX += (float)dx;
                this.panY += (float)dy;
                this.clampPan();
            }

            return true;
        } else {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.pressedInCanvas) {
            this.pressedInCanvas = false;
            PlayerClass cls = ClientData.playerClass;
            if (!this.dragging && button == 0 && this.hovered != null && this.hovered != Talent.CORE && cls != null) {
                String block = this.hovered
                    .blockReason(cls, ClientData.level, ClientData.talentPoints, ClientData.talents, ClientData.technique, ClientData.fingers);
                if (block == null) {
                    ModNetwork.sendToServer(new SimplePackets.UnlockTalent(this.hovered.ordinal()));
                } else {
                    UiFx.sound(ModSounds.UI_ERROR, 1.0F, 0.5F);
                }
            }

            this.dragging = false;
            return true;
        } else {
            return super.mouseReleased(mx, my, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!this.inCanvas(mx, my)) {
            return super.mouseScrolled(mx, my, delta);
        } else {
            this.homing = false;
            this.anchored = true;
            this.ax = (float)mx;
            this.ay = (float)my;
            this.wx = (this.ax - this.originX()) / this.zoom;
            this.wy = (this.ay - this.originY()) / this.zoom;
            this.tZoom = Mth.clamp(this.tZoom * (delta > 0.0 ? 1.15F : 0.86956525F), 0.4F, 2.0F);
            return true;
        }
    }

    private void clampPan() {
        float lim = (float)(Talent.maxAbsGx() * 66 + 80) * this.zoom;
        this.panX = Mth.clamp(this.panX, -lim, lim);
        this.panY = Mth.clamp(this.panY, -60.0F, (float)((Talent.maxGy() + 1) * 64) * this.zoom);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (Keybinds.CHARACTER.matches(key, scan)) {
            this.onClose();
            return true;
        } else if (key == 32) {
            this.tZoom = this.fitZoom();
            this.anchored = false;
            this.homing = true;
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        this.zoom = this.tZoom = -1.0F;
        this.panX = this.panY = 0.0F;
        this.anchored = this.homing = false;
    }

    @Override
    public void renderBackground(GuiGraphics g) {
    }

    private static enum LimbState {
        OWNED,
        OPEN,
        LOCKED;
    }
}
