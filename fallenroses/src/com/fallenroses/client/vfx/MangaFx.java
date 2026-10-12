package com.fallenroses.client.vfx;

import com.fallenroses.config.FRConfig;
import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModSounds;
import com.fallenroses.technique.AbilityDef;
import com.fallenroses.technique.Technique;
import com.fallenroses.vfx.VfxKind;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MangaFx {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final int MAX_WORDS = 24;
    private static final List<MangaFx.Word> WORDS = new ArrayList<>();
    private static final List<MangaFx.Reel> REELS = new ArrayList<>();
    private static final int[] KIND_COOLDOWN = new int[256];
    private static int linesTicks;
    private static int linesTotal;
    private static float linesStrength;
    private static long linesSeed;
    private static int frameTicks = -1;
    private static int frameColor;
    private static int toneTicks;
    private static int toneTotal;
    private static float punch;
    private static float prevPunch;
    private static int cutTicks;
    private static int cutTotal;
    private static String cutGlyph = "";
    private static Component cutName = Component.empty();
    private static Component cutTech = Component.empty();
    private static int cutColor;
    private static int stampTicks;
    private static String stampGlyph = "";
    private static int stampColor;
    private static final int STAMP_LIFE = 18;
    public static final IGuiOverlay OVERLAY = (gui, g, partial, width, height) -> {
        if (enabled()) {
            Minecraft mc = gui.getMinecraft();
            if (!mc.options.hideGui || cutTicks > 0) {
                g.flush();
                if (toneTicks > 0) {
                    drawTone(g, partial, width, height);
                }

                if (linesTicks > 0) {
                    drawLines(g, partial, width, height);
                }

                if (cutTicks > 0) {
                    drawCutIn(mc, g, partial, width, height);
                }

                if (stampTicks > 0) {
                    drawStamp(mc, g, partial, width, height);
                }

                if (frameTicks >= 0) {
                    drawFrame(g, width, height);
                }
            }
        }
    };

    public static void reels(Supplier<Vec3> pos, int code, int[] stops, int life, float size) {
        if (REELS.size() > 4) {
            REELS.remove(0);
        }

        REELS.add(new MangaFx.Reel(pos, code, stops, life, size));
    }

    public static boolean enabled() {
        return FRConfig.MANGA_FX.get();
    }

    public static void onVfx(VfxPacket p) {
        if (enabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 pos = p.pos();
                double dist = mc.gameRenderer.getMainCamera().getPosition().distanceTo(pos);
                boolean self = p.entity() == mc.player.getId();
                int ord = p.kind().ordinal();
                if (ord >= KIND_COOLDOWN.length || KIND_COOLDOWN[ord] <= 0 || p.kind() == VfxKind.MANGA_CAST) {
                    if (ord < KIND_COOLDOWN.length) {
                        KIND_COOLDOWN[ord] = 3;
                    }

                    float s = Math.max(0.6F, Math.min(p.scale(), 6.0F));
                    int c = p.color() == 16777215 ? 16777215 : p.color();
                    Vec3 up = pos.add(0.0, 0.6, 0.0);
                    switch (p.kind()) {
                        case CHAOS_CLAP:
                        case JACKPOT:
                        case FUGA_IGNITE:
                        case METEOR_IMPACT:
                            String w = switch (p.kind()) {
                                case CHAOS_CLAP -> "パァン!!";
                                case JACKPOT -> "大当たり!!";
                                case VERDICT -> "死刑";
                                case FUGA_IGNITE -> "開";
                                default -> "ドゴォォン!!";
                            };
                            word(up, w, p.kind() == VfxKind.VERDICT ? 657930 : 16773312, p.kind() == VfxKind.VERDICT ? 14737632 : 2755072, 3.0F, 34, dist);
                            star(pos, Math.min(s, 5.0F) * 0.7F, c);
                            big(dist, c, 1.1F);
                            break;
                        case VERDICT:
                            if (p.extra() == 1) {
                                word(up.add(0.0, 1.0, 0.0), "死刑", 657930, 16719904, 3.0F, 34, dist);
                                big(dist, 16719904, 1.0F);
                            } else {
                                word(up.add(0.0, 1.5, 0.0), "被告", 15788240, 2759180, 1.6F, 30, dist);
                            }
                            break;
                        case MANGA_CAST:
                            cast(p, self, dist);
                            break;
                        case BLACK_FLASH:
                            word(up, "黒閃", 657930, 16717864, 2.6F, 30, dist);
                            star(pos, 2.4F, 16717864);
                            big(dist, 16715808, 1.2F);
                            break;
                        case PURPLE_IMPACT:
                            word(up, pick("ドォォン!!", "ズドォン!!"), 15257855, 1703984, 3.0F, 32, dist);
                            star(pos, s * 0.8F, 11894015);
                            big(dist, 10112255, 1.3F);
                            break;
                        case MACH_BOOM:
                            word(up, "ドンッ!!", 16777215, 666160, 2.6F, 26, dist);
                            star(pos, 2.0F, 8050920);
                            big(dist, 8050920, 1.0F);
                            break;
                        case DOMAIN_OPEN:
                            word(pos.add(0.0, 3.0, 0.0), "ゴゴゴゴ", 16777215, 0, 3.2F, 50, dist);
                            word(pos.add(2.5, 1.5, 0.0), "ゴゴ", 16777215, 0, 2.0F, 44, dist);
                            word(pos.add(-2.5, 2.0, 0.0), "ゴゴゴ", 16777215, 0, 2.2F, 46, dist);
                            if (dist < 40.0) {
                                frame(c);
                                lines(1.2F, 40);
                                tone(50);
                            }
                            break;
                        case GLACIER:
                            word(up, "パキィィン!!", 15268095, 666176, 3.0F, 30, dist);
                            big(dist, 11067647, 1.0F);
                            break;
                        case BLACK_HOLE:
                            if (p.extra() == 1) {
                                word(up, "ズドォォン!!", 16771248, 0, 3.2F, 34, dist);
                                star(pos, 4.0F, 16765024);
                                big(dist, 16765024, 1.4F);
                            } else {
                                word(up.add(0.0, 1.5, 0.0), "ゴゴゴゴ", 16777215, 0, 2.6F, 60, dist);
                                if (dist < 30.0) {
                                    lines(0.9F, 50);
                                    tone(60);
                                }
                            }
                            break;
                        case ROT_BURST:
                            word(up, "グシャァッ!!", 14205024, 1706496, 2.6F, 28, dist);
                            star(pos, s * 0.6F, 9071136);
                            hard(dist, 0.8F);
                            break;
                        case RED_IMPACT:
                            word(up, p.extra() == 1 ? "ギュン!!" : "ドォン!!", 16777215, p.extra() == 1 ? 663648 : 6291464, 2.4F, 26, dist);
                            star(pos, s * 0.6F, p.extra() == 1 ? 4885759 : 16722462);
                            hard(dist, 1.0F);
                            break;
                        case EXPLODE:
                            word(up, "ドガァン!!", 16777215, 2752576, 2.6F, 26, dist);
                            star(pos, s * 0.7F, 11894015);
                            hard(dist, 1.0F);
                            break;
                        case SLAM:
                        case MASS_IMPACT:
                            word(up, p.kind() == VfxKind.MASS_IMPACT ? "ドゴォッ!!" : "ズドン!!", 16777215, 0, 2.4F, 24, dist);
                            star(pos, Math.min(s, 4.0F) * 0.6F, 16773312);
                            hard(dist, 0.9F);
                            break;
                        case COLLAPSE:
                            word(up, "ガラガラ", 15253600, 0, 2.0F, 26, dist);
                            break;
                        case EMBER_BOOM:
                            word(up, "ボンッ", 16765024, 3803648, 1.4F, 16, dist);
                            break;
                        case GRAVE_IMPACT:
                            word(up, "ドスン", 14737600, 0, 1.8F, 20, dist);
                            hard(dist, 0.5F);
                            break;
                        case BLOOD_BURST:
                            word(up, "バシャッ", 16765144, 5898248, 1.9F, 20, dist);
                            star(pos, s * 0.5F, 13635632);
                            hard(dist, 0.6F);
                            break;
                        case ICE_COFFIN:
                            if (p.extra() == 1) {
                                word(up, "パリィン!!", 15268095, 666176, 2.2F, 24, dist);
                                hard(dist, 0.7F);
                            } else {
                                word(up, "ピキピキ", 15268095, 666176, 1.6F, 20, dist);
                            }
                            break;
                        case RATIO_CRIT:
                            word(up, "バキィッ!!", 16773312, 0, 2.2F, 22, dist);
                            star(pos, 1.4F, 15253600);
                            hard(dist, 0.8F);
                            break;
                        case ROUGH_BLAST:
                            word(up, "ドンッ!", 14221280, 670224, 2.0F, 20, dist);
                            star(pos, 1.2F, 7004282);
                            hard(dist, 0.6F);
                            break;
                        case SHUTTER_DOORS:
                            word(up, "バタンッ!!", 16777215, 0, 2.4F, 22, dist);
                            hard(dist, 0.7F);
                            break;
                        case HIT_SPARK:
                            if (RANDOM.nextInt(3) == 0) {
                                word(up, pick("バキッ", "ドッ", "ガッ", "ゴッ"), 16777215, 0, 1.1F, 12, dist);
                            }
                            break;
                        case FLURRY_HIT:
                            word(up, "ドドドド", 16777215, 0, 1.6F, 16, dist);
                            break;
                        case NAIL_HIT:
                            word(up, "カンッ", 16769216, 3807744, 1.0F, 12, dist);
                            break;
                        case RESONANCE_HIT:
                            word(up, "キィィン", 16769216, 3807744, 1.6F, 18, dist);
                            break;
                        case STING:
                            word(up, "プスッ", 16777215, 0, 1.0F, 12, dist);
                            break;
                        case CLEAVE:
                            word(up, "ザンッ", 16777215, 5898248, 2.0F, 18, dist);
                            break;
                        case SLASH_HIT:
                            word(up, "ズバッ", 16777215, 5898248, 1.5F, 14, dist);
                            break;
                        case SPIDERWEB:
                            word(up, "ズババババ", 16777215, 5898248, 2.2F, 24, dist);
                            hard(dist, 0.6F);
                            break;
                        case SCISSORS:
                            word(up, "チョキン", 16777215, 5898248, 1.6F, 18, dist);
                            break;
                        case REPEL_SWEEP:
                            word(up, "ブンッ", 16777215, 670256, 1.6F, 14, dist);
                            break;
                        case WING_FAN:
                            word(up, "シャッ", 15259808, 2759168, 1.6F, 16, dist);
                            break;
                        case ROPE_LASH:
                            word(up, "ビシッ", 16777215, 0, 1.4F, 14, dist);
                            break;
                        case RED_CHARGE:
                        case PURPLE_CHARGE:
                        case BLOOD_CHARGE:
                        case UZUMAKI_CHARGE:
                            word(up.add(0.0, 1.0, 0.0), p.kind() == VfxKind.BLOOD_CHARGE ? "ズズズ" : "ゴゴゴ", 16777215, 0, 1.8F, 30, dist);
                            if (self) {
                                tone(Math.max(20, p.extra()));
                                lines(0.45F, Math.max(16, p.extra()));
                            }
                            break;
                        case BERSERK:
                            word(up.add(0.0, 1.0, 0.0), "ドクンッ", 16728128, 0, 2.0F, 26, dist);
                            if (self) {
                                frame(16719904);
                                lines(0.8F, 20);
                            }
                            break;
                        case AMBER_AURA:
                            word(up.add(0.0, 1.0, 0.0), "バチバチッ", 14743807, 666192, 2.2F, 26, dist);
                            if (self) {
                                frame(8048895);
                                lines(0.8F, 24);
                            }
                            break;
                        case INSTINCT:
                            word(up.add(0.0, 1.0, 0.0), "ギンッ", 16777215, 0, 1.6F, 18, dist);
                            break;
                        case OVERTIME:
                            word(up.add(0.0, 1.0, 0.0), "カチッ", 15253600, 0, 1.6F, 22, dist);
                            break;
                        case REINFORCE:
                            if (self) {
                                lines(0.3F, 10);
                            }
                            break;
                        case DASH:
                        case FRAME_TRAIL:
                            word(pos, pick("シュンッ", "ヒュッ", "ダッ"), 16777215, 0, 1.5F, 14, dist);
                            if (self) {
                                lines(0.7F, 10);
                            }
                            break;
                        case SWAP:
                            word(pos, "パッ", 16769184, 3809280, 1.4F, 12, dist);
                            break;
                        case CLAP:
                            word(up, "パンッ!", 16769184, 3809280, 1.8F, 16, dist);
                            break;
                        case VANISH:
                            word(up, "スッ…", 16777215, 0, 1.6F, 22, dist);
                            break;
                        case FIRE_PILLAR:
                            word(up.add(0.0, 2.0, 0.0), "ゴオォォ", 16769184, 3803648, 2.2F, 26, dist);
                            break;
                        case FLAME_CONE:
                            word(up, "ボォォッ", 16769184, 3803648, 1.8F, 20, dist);
                            break;
                        case LIGHTNING:
                            word(up.add(0.0, 2.0, 0.0), "ゴロゴロ", 16775360, 0, 2.2F, 24, dist);
                            if (dist < 20.0) {
                                frame(16773242);
                            }
                            break;
                        case ELECTRIC_ARC:
                            word(up, "バリバリッ", 14743807, 666192, 1.8F, 18, dist);
                            hard(dist, 0.5F);
                            break;
                        case STATIC_FIELD:
                            word(up, "ビリビリ", 14743807, 666192, 1.8F, 24, dist);
                            break;
                        case TIDAL_WAVE:
                            word(up.add(0.0, 2.0, 0.0), "ザバァァン!!", 13693183, 8256, 2.6F, 28, dist);
                            hard(dist, 0.7F);
                            break;
                        case SPLASH:
                            word(up, "バシャ", 13693183, 8256, 1.0F, 12, dist);
                            break;
                        case WHIRLPOOL:
                            word(up, "ゴボゴボ", 13693183, 8256, 1.8F, 24, dist);
                            break;
                        case FROST_CONE:
                            word(up, "ヒュオォ", 15268095, 666176, 1.8F, 20, dist);
                            break;
                        case ICE_SPIKE:
                            if (p.extra() == 0) {
                                word(up, "ピキッ", 15268095, 666176, 1.2F, 14, dist);
                            }
                            break;
                        case SPEECH_WAVE:
                            word(up, "ーーッ!!", 15257855, 2097200, 2.2F, 18, dist);
                            if (self) {
                                lines(0.5F, 10);
                            }
                            break;
                        case SPEECH_SEAL:
                            word(up.add(0.0, 0.8, 0.0), "止", 657930, 11894015, 2.2F, 26, dist);
                            break;
                        case TWIST:
                            word(up, "ギュルッ", 15257855, 2097200, 1.8F, 18, dist);
                            break;
                        case ROOT_ERUPT:
                            word(up, "メキメキッ", 14745552, 666112, 1.8F, 20, dist);
                            break;
                        case BUDS:
                            word(up, "ポンッ", 16769264, 2755104, 1.2F, 14, dist);
                            break;
                        case SPIKES:
                            word(up, "ズブッ", 16777215, 666152, 1.6F, 16, dist);
                            break;
                        case SOUL_TOUCH:
                            word(up, "ペタ…", 13697016, 666152, 1.4F, 20, dist);
                            break;
                        case POX_CLOUD:
                            word(up.add(0.0, 1.0, 0.0), "ジュワァ", 14737584, 2105344, 1.8F, 24, dist);
                            break;
                        case COFFIN:
                            word(up, "ギィィ…", 14737600, 0, 1.8F, 24, dist);
                            break;
                        case SHADOW_POOL:
                            word(pos.add(0.0, 1.0, 0.0), "ズルッ", 13687039, 16, 1.6F, 18, dist);
                            break;
                        case TOAD_TONGUE:
                            word(up, "ベロンッ", 16769256, 2097160, 1.6F, 16, dist);
                            break;
                        case ADAPT_WHEEL:
                            word(up.add(0.0, 1.5, 0.0), "ガコン", 16773312, 0, 2.2F, 24, dist);
                            break;
                        case SUMMON:
                            word(up, "ズォッ", 13697008, 8216, 1.8F, 20, dist);
                            break;
                        case ABSORB:
                            word(up, "ゴクン", 13697008, 8216, 1.6F, 20, dist);
                            break;
                        case EXORCISM:
                            if (p.extra() < 4) {
                                word(up, "祓", 16777215, 2755136, 1.6F + (float)p.extra() * 0.3F, 26, dist);
                            }
                            break;
                        case RCT_HEAL:
                            if (self || RANDOM.nextInt(2) == 0) {
                                word(up, "シュゥゥ", 15790335, 2105408, 1.2F, 18, dist);
                            }
                            break;
                        case GRAVITY_WELL:
                            word(up, "ズゥゥン", 16771248, 0, 2.2F, 26, dist);
                            hard(dist, 0.5F);
                            break;
                        case GARUDA:
                            word(up, "キィィッ", 16771248, 2759168, 1.6F, 18, dist);
                            break;
                        case ROT_CLOUD:
                            word(up.add(0.0, 1.0, 0.0), "ジュクジュク", 14205024, 1706496, 1.8F, 26, dist);
                            break;
                        case PACHINKO_REELS:
                            word(up.add(0.0, 1.5, 0.0), "ジャラジャラ", 16773248, 670224, 2.0F, 30, dist);
                            break;
                        case PRISON_REALM:
                            word(up.add(0.0, 1.0, 0.0), "獄門疆", 15788256, 0, 2.6F, 36, dist);
                            if (self) {
                                frame(16777215);
                            }
                            break;
                        case CHAIN_PULL:
                            word(up, "ジャラッ", 14737640, 0, 1.6F, 16, dist);
                            break;
                        case DRAGON_JET:
                            word(up, "ドシュッ!!", 14743807, 8256, 2.0F, 18, dist);
                            hard(dist, 0.6F);
                            break;
                        case EYE_BEAM:
                            word(up, "ビーッ", 16773312, 3803648, 1.6F, 16, dist);
                            break;
                        case BEARER_SCREAM:
                            word(up.add(0.0, 1.0, 0.0), "アアアア!!", 15257855, 0, 2.4F, 26, dist);
                            break;
                        case RAINBOW_BREATH:
                            word(up, "ゴアァァ", 16777215, 0, 2.2F, 22, dist);
                            break;
                        case DUST_CLOUD:
                            word(up, "モワッ", 14735552, 0, 1.4F, 18, dist);
                            break;
                        case JUDGEMAN:
                            String jw = switch (p.extra()) {
                                case 1 -> "死刑!!";
                                case 2 -> "没収";
                                case 3 -> "無罪";
                                case 4 -> "異議あり!";
                                default -> "開廷";
                            };
                            int fill = p.extra() == 1 ? 16724016 : (p.extra() == 3 ? 14211288 : 15786144);
                            word(pos.add(0.0, p.extra() == 4 ? 2.4 : 7.0, 0.0), jw, fill, 656900, p.extra() == 4 ? 2.4F : 3.4F, 44, dist);
                            if (p.extra() == 1 && dist < 40.0) {
                                big(dist, 16719904, 1.2F);
                            } else if (p.extra() != 4 && dist < 30.0) {
                                lines(0.9F, 24);
                            }
                            break;
                        case GRANITE_BEAM:
                            word(up, "ドォォォン!!", 16773312, 2759168, 3.4F, 34, dist);
                            star(pos, 3.0F, 15786160);
                            big(dist, 15786160, 1.3F);
                            break;
                        case SWARM_STORM:
                            word(up.add(0.0, 2.0, 0.0), "ザワザワザワ", 14200960, 1706496, 2.4F, 50, dist);
                    }
                }
            }
        }
    }

    private static void cast(VfxPacket p, boolean self, double dist) {
        Technique t = Technique.byOrdinal(p.extra());
        int slot = Mth.clamp(Math.round(p.scale()), 0, 4);
        if (t != null) {
            AbilityDef a = t.ability(slot);
            int color = t.color();
            boolean ult = slot == 4;
            if (self) {
                if (ult) {
                    cutGlyph = a.glyph();
                    cutName = a.displayName();
                    cutTech = t.displayName();
                    cutColor = color;
                    cutTotal = cutTicks = t.hasDomain() ? 26 : 38;
                    frame(color);
                    lines(1.0F, 30);
                    punch(0.1F);
                } else {
                    stampGlyph = a.glyph();
                    stampColor = color;
                    stampTicks = 18;
                }
            } else {
                word(p.pos().add(0.0, 0.9, 0.0), a.glyph(), 16777215, darken(color), ult ? 3.0F : 1.8F, ult ? 40 : 22, dist);
                if (ult && dist < 24.0) {
                    lines(0.6F, 20);
                }
            }
        }
    }

    private static String pick(String... options) {
        return options[RANDOM.nextInt(options.length)];
    }

    private static int darken(int rgb) {
        int r = (rgb >> 16 & 0xFF) / 5;
        int g = (rgb >> 8 & 0xFF) / 5;
        int b = (rgb & 0xFF) / 5;
        return r << 16 | g << 8 | b;
    }

    public static void word(Vec3 pos, String text, int fill, int outline, float size, int life, double dist) {
        if (FRConfig.MANGA_TEXT.get() && !(dist > 48.0)) {
            if (WORDS.size() >= 24) {
                WORDS.remove(0);
            }

            Vec3 jitter = new Vec3(RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.3);
            WORDS.add(new MangaFx.Word(pos.add(jitter), text, fill, outline, size, life));
        }
    }

    private static void star(Vec3 pos, float size, int tint) {
        VfxEffects.add(new MangaFx.StarFx(pos, Math.max(0.8F, size), tint, 7));
    }

    private static void hard(double dist, float k) {
        if (!(dist > 26.0)) {
            float f = (float)(1.0 - dist / 26.0);
            lines(0.7F * k * (0.5F + f), 9);
            punch(0.05F * k * f);
        }
    }

    private static void big(double dist, int color, float k) {
        if (!(dist > 40.0)) {
            float f = (float)(1.0 - dist / 40.0);
            frame(color);
            lines(1.1F * k, 16);
            punch(0.12F * k * (0.4F + f));
        }
    }

    public static void lines(float strength, int ticks) {
        if (strength >= linesStrength * (float)linesTicks / Math.max(1.0F, (float)linesTotal) - 0.01F) {
            linesStrength = Math.min(strength, 1.5F);
            linesTicks = ticks;
            linesTotal = ticks;
        }
    }

    public static void frame(int color) {
        if (FRConfig.SCREEN_FLASHES.get()) {
            frameTicks = 0;
            frameColor = color;
        }
    }

    public static void tone(int ticks) {
        if (ticks > toneTicks) {
            toneTicks = ticks;
            toneTotal = ticks;
        }
    }

    public static void punch(float k) {
        punch = Math.min(0.25F, Math.max(punch, k));
    }

    public static void tick() {
        for (int i = 0; i < KIND_COOLDOWN.length; i++) {
            if (KIND_COOLDOWN[i] > 0) {
                KIND_COOLDOWN[i]--;
            }
        }

        WORDS.removeIf(w -> {
            w.age++;
            if (w.age < 8) {
                w.jx = (RANDOM.nextFloat() - 0.5F) * 0.08F * (float)(8 - w.age);
                w.jy = (RANDOM.nextFloat() - 0.5F) * 0.08F * (float)(8 - w.age);
            } else {
                w.jx = w.jy = 0.0F;
            }

            return w.age > w.life;
        });
        REELS.removeIf(
            r -> {
                r.age++;

                for (int ix = 0; ix < 3; ix++) {
                    if (r.age == r.stops[ix]) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.level != null && mc.player != null) {
                            mc.level
                                .playLocalSound(
                                    r.pos.get().x,
                                    r.pos.get().y,
                                    r.pos.get().z,
                                    ModSounds.NAIL_HIT.get(),
                                    SoundSource.PLAYERS,
                                    0.8F,
                                    1.2F + (float)ix * 0.2F,
                                    false
                                );
                        }
                    }
                }

                return r.age > r.life;
            }
        );
        if (linesTicks > 0) {
            linesTicks--;
        }

        if (linesTicks % 2 == 0) {
            linesSeed = RANDOM.nextLong();
        }

        if (frameTicks >= 0 && ++frameTicks > 4) {
            frameTicks = -1;
        }

        if (toneTicks > 0) {
            toneTicks--;
        }

        prevPunch = punch;
        punch *= 0.72F;
        if (punch < 0.002F) {
            punch = 0.0F;
        }

        if (cutTicks > 0) {
            cutTicks--;
        }

        if (stampTicks > 0) {
            stampTicks--;
        }
    }

    public static void reset() {
        WORDS.clear();
        REELS.clear();
        stampTicks = 0;
        cutTicks = 0;
        toneTicks = 0;
        linesTicks = 0;
        frameTicks = -1;
        prevPunch = 0.0F;
        punch = 0.0F;
    }

    public static double fovScale(float partial) {
        float k = Mth.lerp(partial, prevPunch, punch);
        return 1.0 - (double)k;
    }

    public static void renderWorld(PoseStack ps, Camera camera, float partial) {
        if (!WORDS.isEmpty() || !REELS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            BufferSource buffers = mc.renderBuffers().bufferSource();
            Vec3 cam = camera.getPosition();

            for (MangaFx.Reel r : REELS) {
                float age = (float)r.age + partial;
                float alpha = Mth.clamp(((float)r.life - age) / 8.0F, 0.0F, 1.0F);
                if (!(alpha <= 0.03F)) {
                    Vec3 p = r.pos.get();
                    boolean all = r.age >= r.stops[2];
                    boolean triple = all && r.digits[0] == r.digits[1] && r.digits[1] == r.digits[2];
                    float k = 0.045F * r.size;
                    ps.pushPose();
                    ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
                    ps.mulPose(camera.rotation());
                    ps.scale(-k, -k, k);
                    Matrix4f m = ps.last().pose();
                    int a = (int)(alpha * 255.0F) << 24;

                    for (int i = 0; i < 3; i++) {
                        boolean stopped = r.age >= r.stops[i];
                        int digit = stopped ? r.digits[i] : 1 + (int)((age * 3.0F + (float)(i * 5)) % 7.0F);
                        String txt = String.valueOf(digit);
                        int fill = stopped ? (digit == 7 ? 16724032 : 16777215) : 13158600;
                        float x = (float)(i - 1) * 14.0F - (float)font.width(txt) / 2.0F;
                        float y = stopped ? -4.0F : -4.0F + (age * 7.0F + (float)(i * 3)) % 6.0F - 3.0F;
                        font.drawInBatch8xOutline(
                            Component.literal(txt).withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(), x, y, a | fill, a | 670224, m, buffers, 15728880
                        );
                    }

                    if (triple) {
                        FormattedCharSequence jp = Component.literal("JACKPOT!!").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText();
                        float pulse = 1.0F + 0.08F * Mth.sin(age * 0.8F);
                        ps.scale(pulse, pulse, 1.0F);
                        font.drawInBatch8xOutline(jp, (float)(-font.width(jp)) / 2.0F, -18.0F, a | 16769088, a | 3807744, ps.last().pose(), buffers, 15728880);
                    }

                    ps.popPose();
                }
            }

            for (MangaFx.Word w : WORDS) {
                float age = (float)w.age + partial;
                float t = age / (float)w.life;
                float pop = age < 3.0F ? Mth.lerp(age / 3.0F, 1.9F, 1.0F) : 1.0F + 0.04F * Mth.sin(age * 0.9F);
                float alpha = t > 0.75F ? 1.0F - (t - 0.75F) / 0.25F : 1.0F;
                if (!(alpha <= 0.03F)) {
                    double dist = cam.distanceTo(w.pos);
                    float k = 0.03F * w.size * pop * (float)Math.max(1.0, dist / 16.0);
                    ps.pushPose();
                    ps.translate(w.pos.x - cam.x + (double)w.jx, w.pos.y - cam.y + (double)w.jy + (double)age * 0.012, w.pos.z - cam.z);
                    ps.mulPose(camera.rotation());
                    ps.mulPose(Axis.ZP.rotationDegrees(w.roll));
                    ps.scale(-k, -k, k);
                    Matrix4f m = ps.last().pose();
                    int a = (int)(alpha * 255.0F) << 24;
                    font.drawInBatch(
                        w.text, (float)(-w.width) / 2.0F + 1.5F, -2.5F, a | w.outline & 16777215, false, m, buffers, DisplayMode.NORMAL, 0, 15728880
                    );
                    font.drawInBatch8xOutline(w.text, (float)(-w.width) / 2.0F, -4.0F, a | w.fill & 16777215, a | w.outline & 16777215, m, buffers, 15728880);
                    ps.popPose();
                }
            }

            buffers.endBatch();
        }
    }

    private static BufferBuilder begin(Mode mode) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return bb;
    }

    private static void end(BufferBuilder bb) {
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void tri(BufferBuilder bb, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
        int a = argb >>> 24;
        int r = argb >> 16 & 0xFF;
        int gg = argb >> 8 & 0xFF;
        int b = argb & 0xFF;
        bb.vertex(m, x0, y0, 0.0F).color(r, gg, b, a).endVertex();
        bb.vertex(m, x1, y1, 0.0F).color(r, gg, b, a).endVertex();
        bb.vertex(m, x2, y2, 0.0F).color(r, gg, b, a).endVertex();
    }

    private static void quad(BufferBuilder bb, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        tri(bb, m, x0, y0, x1, y1, x2, y2, argb);
        tri(bb, m, x0, y0, x2, y2, x3, y3, argb);
    }

    private static void drawLines(GuiGraphics g, float partial, int w, int h) {
        float life = ((float)linesTicks - partial) / Math.max(1.0F, (float)linesTotal);
        float a = linesStrength * Mth.clamp(life * 1.6F, 0.0F, 1.0F);
        if (!(a <= 0.02F)) {
            RandomSource r = RandomSource.create(linesSeed);
            Matrix4f m = g.pose().last().pose();
            float cx = (float)w / 2.0F;
            float cy = (float)h / 2.0F;
            float far = (float)Math.hypot((double)w, (double)h) * 0.6F;
            int n = (int)(60.0F + 50.0F * Math.min(1.0F, linesStrength));
            BufferBuilder bb = begin(Mode.TRIANGLES);

            for (int i = 0; i < n; i++) {
                float ang = r.nextFloat() * (float) (Math.PI * 2);
                float inner = far * (0.42F + r.nextFloat() * 0.3F - 0.12F * Math.min(1.0F, linesStrength));
                float half = 0.004F + r.nextFloat() * 0.012F;
                float ca = Mth.cos(ang);
                float sa = Mth.sin(ang);
                float ca0 = Mth.cos(ang - half);
                float sa0 = Mth.sin(ang - half);
                float ca1 = Mth.cos(ang + half);
                float sa1 = Mth.sin(ang + half);
                int col = VfxDraw.argb(Math.min(0.85F, a * (0.5F + r.nextFloat() * 0.5F)), 0);
                tri(bb, m, cx + ca * inner, cy + sa * inner, cx + ca0 * far, cy + sa0 * far, cx + ca1 * far, cy + sa1 * far, col);
            }

            end(bb);
        }
    }

    private static void drawTone(GuiGraphics g, float partial, int w, int h) {
        float life = ((float)toneTicks - partial) / Math.max(1.0F, (float)toneTotal);
        float a = Mth.clamp(Math.min(life * 3.0F, (1.0F - life) * 6.0F + 0.3F), 0.0F, 1.0F) * 0.55F;
        if (!(a <= 0.02F)) {
            Matrix4f m = g.pose().last().pose();
            float cx = (float)w / 2.0F;
            float cy = (float)h / 2.0F;
            int step = 6;
            BufferBuilder bb = begin(Mode.TRIANGLES);
            int col = VfxDraw.argb(a, 0);

            for (int y = 0; y <= h + step; y += step) {
                float off = y / step % 2 == 0 ? 0.0F : (float)step / 2.0F;

                for (int x = 0; x <= w + step; x += step) {
                    float px = (float)x + off;
                    float py = (float)y;
                    float dx = (px - cx) / cx;
                    float dy = (py - cy) / cy;
                    float e = (float)Math.sqrt((double)(dx * dx + dy * dy)) - 0.62F;
                    if (!(e <= 0.0F)) {
                        float rr = Math.min((float)step * 0.6F, e * (float)step * 1.6F);
                        if (!(rr < 0.35F)) {
                            quad(bb, m, px, py - rr, px + rr, py, px, py + rr, px - rr, py, col);
                        }
                    }
                }
            }

            end(bb);
        }
    }

    private static void drawFrame(GuiGraphics g, int w, int h) {
        Matrix4f m = g.pose().last().pose();
        int stage = frameTicks;
        if (stage == 0 || stage == 2) {
            BufferBuilder bb = begin(Mode.TRIANGLES);
            RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE);
            quad(bb, m, 0.0F, 0.0F, (float)w, 0.0F, (float)w, (float)h, 0.0F, (float)h, -1);
            end(bb);
            RenderSystem.defaultBlendFunc();
        } else if (stage == 1) {
            BufferBuilder bb = begin(Mode.TRIANGLES);
            RenderSystem.blendFuncSeparate(SourceFactor.DST_COLOR, DestFactor.ZERO, SourceFactor.ZERO, DestFactor.ONE);
            int c = frameColor;
            int r = Math.min(255, (c >> 16 & 0xFF) + 60);
            int gg = Math.min(255, (c >> 8 & 0xFF) + 20);
            int b = Math.min(255, (c & 0xFF) + 20);
            quad(bb, m, 0.0F, 0.0F, (float)w, 0.0F, (float)w, (float)h, 0.0F, (float)h, 0xFF000000 | r << 16 | gg << 8 | b);
            end(bb);
            RenderSystem.defaultBlendFunc();
        } else {
            g.fill(0, 0, w, h, VfxDraw.argb(stage == 3 ? 0.35F : 0.15F, 16777215));
        }
    }

    private static void drawCutIn(Minecraft mc, GuiGraphics g, float partial, int w, int h) {
        float age = (float)(cutTotal - cutTicks) + partial;
        float t = age / (float)cutTotal;
        float in = VfxEffects.easeOut(Mth.clamp(age / 5.0F, 0.0F, 1.0F));
        float out = Mth.clamp(((float)cutTicks - partial) / 6.0F, 0.0F, 1.0F);
        float bh = (float)h * 0.3F * Math.min(1.0F, out * 1.2F);
        if (!(bh < 1.0F)) {
            float cy = (float)h * 0.5F;
            float skew = bh * 0.45F;
            float right = (float)w + skew;
            float left = -skew + (1.0F - in) * ((float)w + skew * 2.0F);
            Matrix4f m = g.pose().last().pose();
            BufferBuilder bb = begin(Mode.TRIANGLES);
            float top = cy - bh / 2.0F;
            float bot = cy + bh / 2.0F;
            quad(bb, m, left + skew - 2.0F, top - 4.0F, right, top - 4.0F, right - skew, bot + 4.0F, left - 2.0F, bot + 4.0F, VfxDraw.argb(0.95F, cutColor));
            quad(bb, m, left + skew, top - 1.5F, right, top - 1.5F, right - skew, bot + 1.5F, left, bot + 1.5F, -1);
            quad(bb, m, left + skew, top, right, top, right - skew, bot, left, bot, -184220408);
            RandomSource r = RandomSource.create((long)cutGlyph.hashCode() * 31L);

            for (int i = 0; i < 46; i++) {
                float y = top + 2.0F + r.nextFloat() * (bh - 4.0F);
                float len = 30.0F + r.nextFloat() * (float)w * 0.35F;
                float speed = 18.0F + r.nextFloat() * 24.0F;
                float x = (r.nextFloat() * (float)(w + 400) - age * speed * 2.0F) % (float)(w + 400);
                if (x < 0.0F) {
                    x += (float)(w + 400);
                }

                x -= 200.0F;
                float th = 0.5F + r.nextFloat() * 1.3F;
                float k = (y - top) / bh;
                float sx = x + skew * (1.0F - k) - skew * 0.5F;
                int col = VfxDraw.argb(0.25F + r.nextFloat() * 0.5F, i % 5 == 0 ? cutColor : 16777215);
                quad(bb, m, sx, y, sx + len, y, sx + len, y + th, sx, y + th, col);
            }

            end(bb);
            Font font = mc.font;
            float shakeX = age < 6.0F ? (RANDOM.nextFloat() - 0.5F) * 6.0F : 0.0F;
            float shakeY = age < 6.0F ? (RANDOM.nextFloat() - 0.5F) * 6.0F : 0.0F;
            float glyphScale = bh / 9.0F * (age < 4.0F ? Mth.lerp(age / 4.0F, 1.6F, 1.0F) : 1.0F + t * 0.06F);
            float gx = (float)w * 0.27F + (1.0F - in) * (float)w * 0.5F + shakeX;
            float gy = cy + shakeY;
            outlined(
                g,
                font,
                Component.literal(cutGlyph).withStyle(Style.EMPTY.withBold(true)),
                gx,
                gy,
                glyphScale,
                16777215,
                cutColor,
                Math.max(1.5F, glyphScale * 0.06F)
            );
            float nx = (float)w * 0.62F + (1.0F - in) * (float)w * 0.4F;
            outlined(g, font, cutTech, nx, cy - bh * 0.22F, 1.2F, 16777215, 0, 1.0F);
            outlined(g, font, cutName.copy().withStyle(Style.EMPTY.withBold(true)), nx, cy + bh * 0.06F, Math.min(3.2F, bh / 22.0F), 16777215, cutColor, 1.2F);
        }
    }

    private static void drawStamp(Minecraft mc, GuiGraphics g, float partial, int w, int h) {
        float age = (float)(18 - stampTicks) + partial;
        float pop = age < 3.0F ? Mth.lerp(age / 3.0F, 1.7F, 1.0F) : 1.0F;
        float a = Mth.clamp(((float)stampTicks - partial) / 6.0F, 0.0F, 1.0F);
        if (!(a <= 0.02F)) {
            float cx = (float)w / 2.0F + 30.0F;
            float cy = (float)h / 2.0F + 22.0F;
            float half = 11.0F * pop;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(-9.0F));
            Matrix4f m = g.pose().last().pose();
            BufferBuilder bb = begin(Mode.TRIANGLES);
            int ink = VfxDraw.argb(a * 0.95F, stampColor);
            float b = 2.2F;
            quad(bb, m, -half, -half, half, -half, half, -half + b, -half, -half + b, ink);
            quad(bb, m, -half, half - b, half, half - b, half, half, -half, half, ink);
            quad(bb, m, -half, -half, -half + b, -half, -half + b, half, -half, half, ink);
            quad(bb, m, half - b, -half, half, -half, half, half, half - b, half, ink);
            quad(bb, m, -half + b, -half + b, half - b, -half + b, half - b, half - b, -half + b, half - b, VfxDraw.argb(a * 0.35F, 0));
            end(bb);
            Font font = mc.font;
            Component glyph = Component.literal(stampGlyph);
            g.pose().scale(1.8F * pop, 1.8F * pop, 1.0F);
            g.drawString(font, glyph, -font.width(glyph) / 2, -4, VfxDraw.argb(a, lighten(stampColor)), false);
            g.pose().popPose();
        }
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, (rgb >> 16 & 0xFF) + 70);
        int gg = Math.min(255, (rgb >> 8 & 0xFF) + 70);
        int b = Math.min(255, (rgb & 0xFF) + 70);
        return r << 16 | gg << 8 | b;
    }

    private static void outlined(GuiGraphics g, Font font, Component text, float cx, float cy, float scale, int fill, int outline, float px) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        int x = -font.width(text) / 2;
        float o = px / scale * 1.4F;

        for (int i = 0; i < 8; i++) {
            float ang = (float)i * (float) Math.PI / 4.0F;
            g.pose().pushPose();
            g.pose().translate(Mth.cos(ang) * o * 1.6F, Mth.sin(ang) * o * 1.6F, 0.0F);
            g.drawString(font, text.copy().withStyle(s -> s.withColor(outline)), x, -4, 0xFF000000 | outline, false);
            g.pose().popPose();
        }

        g.drawString(font, text.copy().withStyle(s -> s.withColor(fill)), x, -4, 0xFF000000 | fill, false);
        g.pose().popPose();
    }

    private MangaFx() {
    }

    private static final class Reel {
        final Supplier<Vec3> pos;
        final int[] digits;
        final int[] stops;
        final int life;
        final float size;
        int age;

        Reel(Supplier<Vec3> pos, int code, int[] stops, int life, float size) {
            this.pos = pos;
            this.digits = new int[]{Math.max(1, code / 100 % 10), Math.max(1, code / 10 % 10), Math.max(1, code % 10)};
            this.stops = stops;
            this.life = life;
            this.size = size;
        }
    }

    static final class StarFx extends VfxEffects.Fx {
        private final Vec3 pos;
        private final float size;
        private final int tint;
        private final float[] spikes = new float[18];
        private final float spin;

        StarFx(Vec3 pos, float size, int tint, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.WHITE));
            this.pos = pos;
            this.size = size;
            this.tint = tint;

            for (int i = 0; i < this.spikes.length; i++) {
                this.spikes[i] = 0.55F + MangaFx.RANDOM.nextFloat() * 0.75F;
            }

            this.spin = MangaFx.RANDOM.nextFloat() * (float) (Math.PI * 2);
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float grow = VfxEffects.easeOut(Math.min(1.0F, t * 3.0F));
            float a = t < 0.6F ? 1.0F : 1.0F - (t - 0.6F) / 0.4F;
            Vector3f left = new Vector3f(cam.getLeftVector());
            Vector3f up = new Vector3f(cam.getUpVector());
            float r = this.size * (0.5F + 0.6F * grow);
            this.fan(m, vc, left, up, r * 1.14F, 0.42F, VfxDraw.argb(a, 0));
            this.fan(m, vc, left, up, r, 0.38F, VfxDraw.argb(a, 16777215));
            this.fan(m, vc, left, up, r * 0.45F, 0.5F, VfxDraw.argb(a * 0.9F, this.tint));
        }

        private void fan(Matrix4f m, VertexConsumer vc, Vector3f left, Vector3f up, float r, float inner, int argb) {
            int n = this.spikes.length;

            for (int i = 0; i < n; i++) {
                float a0 = this.spin + (float) (Math.PI * 2) * (float)i / (float)n;
                float a1 = this.spin + (float) (Math.PI * 2) * ((float)i + 0.5F) / (float)n;
                float a2 = this.spin + (float) (Math.PI * 2) * (float)(i + 1) / (float)n;
                this.vtx(vc, m, left, up, 0.0F, 0.0F, argb);
                this.vtx(vc, m, left, up, Mth.cos(a0) * r * inner, Mth.sin(a0) * r * inner, argb);
                this.vtx(vc, m, left, up, Mth.cos(a1) * r * this.spikes[i], Mth.sin(a1) * r * this.spikes[i], argb);
                this.vtx(vc, m, left, up, Mth.cos(a2) * r * inner, Mth.sin(a2) * r * inner, argb);
            }
        }

        private void vtx(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, float x, float y, int argb) {
            vc.vertex(
                    m,
                    (float)(this.pos.x + (double)(left.x * x) + (double)(up.x * y)),
                    (float)(this.pos.y + (double)(left.y * x) + (double)(up.y * y)),
                    (float)(this.pos.z + (double)(left.z * x) + (double)(up.z * y))
                )
                .color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24 & 0xFF)
                .uv(0.5F, 0.5F)
                .endVertex();
        }
    }

    private static final class Word {
        final Vec3 pos;
        final FormattedCharSequence text;
        final int width;
        final int fill;
        final int outline;
        final float size;
        final float roll;
        final int life;
        int age;
        float jx;
        float jy;

        Word(Vec3 pos, String text, int fill, int outline, float size, int life) {
            this.pos = pos;
            this.text = Component.literal(text).withStyle(Style.EMPTY.withBold(true)).getVisualOrderText();
            this.width = Minecraft.getInstance().font.width(this.text);
            this.fill = fill;
            this.outline = outline;
            this.size = size;
            this.roll = (MangaFx.RANDOM.nextFloat() - 0.5F) * 30.0F;
            this.life = life;
        }
    }
}
