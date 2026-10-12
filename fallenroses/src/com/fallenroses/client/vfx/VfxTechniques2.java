package com.fallenroses.client.vfx;

import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class VfxTechniques2 {
    private static final int WHITE = 16777215;
    private static final int SHADOW = 6978248;
    private static final int CYAN = 8050920;
    private static final int ICE = 11067647;
    private static final int SEA = 2788048;
    private static final int POX = 10133600;

    static void play(VfxPacket p) {
        Vec3 pos = p.pos();
        Vec3 dir = p.dir();
        float s = p.scale();
        int c = p.color();
        switch (p.kind()) {
            case SHADOW_POOL:
                shadowPool(pos, dir, s, p.extra());
                break;
            case TOAD_TONGUE:
                tongue(pos, dir, p.entity(), c);
                break;
            case FLOOD:
                flood(pos, s);
                break;
            case ADAPT_WHEEL:
                VfxEffects.add(new VfxTechniques2.WheelFx(p.entity(), pos, s, p.extra(), 34));
                break;
            case FRAME_TRAIL:
                frameTrail(pos, dir, s, c);
                break;
            case FRAME_PANEL:
                framePanel(p.entity(), pos, s, c, p.extra());
                break;
            case MACH_BOOM:
                machBoom(pos, dir, s);
                break;
            case FROST_CONE:
                frostCone(pos, dir, s);
                break;
            case ICE_SPIKE:
                iceSpike(pos, s, p.extra());
                break;
            case ICE_COFFIN:
                iceCoffin(p.entity(), pos, s, p.extra());
                break;
            case GLACIER:
                glacier(pos, s);
                break;
            case TIDAL_WAVE:
                VfxEffects.add(new VfxTechniques2.WaveFx(pos, VfxClient.dirOr(dir), s, (int)s + 6));
                break;
            case WHIRLPOOL:
                whirlpool(pos, dir, s, p.extra());
                break;
            case SPLASH:
                splash(pos, s);
                break;
            case POX_CLOUD:
                VfxEffects.add(new VfxTechniques2.PoxFx(pos, s, Math.max(20, p.extra())));
                break;
            case GRAVE_IMPACT:
                graveImpact(pos, s, p.extra());
                break;
            case COFFIN:
                VfxEffects.add(new VfxTechniques2.CoffinFx(p.entity(), pos, s, Math.max(20, p.extra())));
                break;
            case PLAGUE_MARK:
                VfxEffects.add(new VfxTechniques2.SoresFx(p.entity(), pos, s, Math.max(20, p.extra())));
                break;
            case DUST_CLOUD:
                dustCloud(pos, s, c);
                break;
            case STING:
                sting(pos, dir, p.entity(), c);
                break;
            case SCISSORS:
                scissors(pos, dir, s);
                break;
            case RAINBOW_BREATH:
                rainbow(pos, dir);
                break;
            default:
                VfxTechniques3.play(p);
        }
    }

    private static void shadowPool(Vec3 pos, Vec3 dir, float s, int extra) {
        Vec3 n = extra == 1 ? new Vec3(0.0, -1.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 at = pos.add(n.scale(0.04));
        int life = extra == 2 ? 40 : 26;
        VfxEffects.add(new VfxEffects.Decal(VfxClient.fixed(at), n, 0.2F, 1.3F * s, 0.15F, -134086138, life, FRRenderTypes.SEAL, false));
        VfxEffects.add(
            new VfxEffects.Decal(VfxClient.fixed(at.add(n.scale(0.01))), n, 0.2F, 1.2F * s, -0.2F, VfxDraw.argb(0.8F, 6978248), life, FRRenderTypes.RING, true)
        );
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(at), n, 0.2F, 1.6F * s, 0.25F, 6978248, 14, FRRenderTypes.SHOCK).alpha(0.7F));

        for (int i = 0; (float)i < 18.0F * s; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double r = VfxClient.random.nextDouble() * (double)s;
            VfxClient.add(
                ModParticles.smoke(i % 3 == 0 ? 1712208 : 131590, 0.9F, 30),
                at.x + Math.cos(a) * r,
                at.y,
                at.z + Math.sin(a) * r,
                0.0,
                n.y * (0.05 + VfxClient.random.nextDouble() * 0.1),
                0.0
            );
        }

        if (extra == 2) {
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(at), VfxClient.fixed(at.add(0.0, 6.0, 0.0)), 1.6F, 657944, 30));
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(at), VfxClient.fixed(at.add(0.0, 5.0, 0.0)), 0.5F, 6978248, 24));

            for (int i = 0; i < 4; i++) {
                VfxEffects.add(
                    new VfxEffects.Ring(
                            VfxClient.fixed(at.add(0.0, 0.2 + (double)i * 1.2, 0.0)),
                            new Vec3(0.0, 1.0, 0.0),
                            0.5F,
                            3.0F * s * 0.5F,
                            0.6F,
                            6978248,
                            20,
                            FRRenderTypes.SHOCK
                        )
                        .delay(i * 3)
                );
            }
        }
    }

    private static void tongue(Vec3 from, Vec3 vec, int target, int color) {
        Supplier<Vec3> to = VfxClient.follow(target, from.add(vec), 0.5);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), to, 0.22F, color, 10));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), to, 0.08F, 16767200, 8));
        VfxEffects.add(new VfxEffects.Flash(to, 0.2F, 1.2F, color, 8));
        VfxClient.burst(from.add(vec), Vec3.ZERO, 10, 0.25, ParticleTypes.SPLASH);
    }

    private static void flood(Vec3 pos, float r) {
        Vec3 top = pos.add(0.0, 8.0, 0.0);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(pos), r * 0.55F, 2783944, 16));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(pos), r * 0.25F, 13168895, 12));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 0.5, 0.0)), 0.5F, r * 1.3F, 9097471, 12));

        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        VfxClient.fixed(pos.add(0.0, 0.15, 0.0)),
                        new Vec3(0.0, 1.0, 0.0),
                        0.5F,
                        r * (1.2F + (float)i * 0.35F),
                        0.7F,
                        i == 0 ? 16777215 : 5945584,
                        16 + i * 3,
                        FRRenderTypes.SHOCK
                    )
                    .delay(i * 2)
            );
        }

        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), r * 0.5F, r * 1.2F, 0.05F, -1609549216, 60, FRRenderTypes.SEAL, false
            )
        );

        for (int i = 0; i < 160; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double sp = 0.2 + VfxClient.random.nextDouble() * 0.6;
            VfxClient.add(
                i % 4 == 0 ? ParticleTypes.BUBBLE_POP : ParticleTypes.SPLASH,
                pos.x,
                pos.y + 0.3,
                pos.z,
                Math.cos(a) * sp,
                0.3 + VfxClient.random.nextDouble() * 0.4,
                Math.sin(a) * sp
            );
        }

        VfxClient.burst(pos.add(0.0, 2.0, 0.0), new Vec3(0.0, -1.0, 0.0), 40, 0.4, ModParticles.glow(9097471, 0.6F, 14));
    }

    private static void frameTrail(Vec3 from, Vec3 vec, float s, int color) {
        VfxEffects.add(new VfxTechniques2.FramesFx(from, vec, s, color, 14));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(from.add(vec)), 0.06F * s, 16777215, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from.add(vec)), 0.3F, 1.4F * s, color, 8));
        VfxClient.burst(from.add(vec), VfxClient.dirOr(vec), 10, 0.3, ModParticles.spark(color, 0.4F, 8));
    }

    private static void framePanel(int entity, Vec3 pos, float s, int color, int extra) {
        if (extra == 2) {
            VfxEffects.add(new VfxTechniques2.SpeedAuraFx(entity, pos, color, Math.max(10, (int)s)));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.follow(entity, pos, 0.02), new Vec3(0.0, 1.0, 0.0), 0.4F, 2.4F, 0.35F, color, 14, FRRenderTypes.SHOCK));
        } else if (extra == 1) {
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.4F, 2.2F * s, 16777215, 8));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.3F, 2.4F * s, 0.4F, color, 10, FRRenderTypes.SHOCK));
            VfxClient.burst(pos, Vec3.ZERO, (int)(40.0F * s), 0.5, ModParticles.spark(15269119, 0.5F, 14));
            VfxClient.blood(pos, 10, 0.6F);
        } else {
            VfxEffects.add(new VfxTechniques2.PanelFx(entity, pos, s, color, 50));
            VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(entity, pos, 0.5), 0.3F, 1.6F * s, color, 8));
        }
    }

    private static void machBoom(Vec3 from, Vec3 vec, float s) {
        Vec3 d = VfxClient.dirOr(vec);
        double len = vec.length();
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(from.add(vec)), 0.9F * s, 8050920, 14));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(from.add(vec)), 0.3F * s, 16777215, 10));

        for (int i = 0; (double)i < len; i += 2) {
            Vec3 at = from.add(d.scale((double)i));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(at), d, 0.4F, 2.6F * s, 0.6F, i % 4 == 0 ? 16777215 : 8050920, 12, FRRenderTypes.SHOCK)
                    .alpha(0.8F)
                    .delay(i / 3)
            );
        }

        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(from), d, 0.5F, (4.0F + (float)i * 2.0F) * s, 1.0F, 16777215, 14, FRRenderTypes.SHOCK).delay(i * 2)
            );
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from), 1.0F, 4.0F * s, 16777215, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from.add(vec)), 1.0F, 3.0F * s, 8050920, 12));

        for (int i = 0; (double)i < len * 3.0; i++) {
            Vec3 at = from.add(d.scale(VfxClient.random.nextDouble() * len)).add(0.0, -0.8, 0.0);
            VfxClient.add(
                ModParticles.smoke(9077880, 1.4F, 26), at.x, at.y, at.z, VfxClient.random.nextGaussian() * 0.12, 0.05, VfxClient.random.nextGaussian() * 0.12
            );
        }
    }

    private static void frostCone(Vec3 mouth, Vec3 dir, float len) {
        Vec3 d = VfxClient.dirOr(dir);

        for (int i = 0; i < 70; i++) {
            Vec3 v = d.add(VfxClient.random.nextGaussian() * 0.24, VfxClient.random.nextGaussian() * 0.2, VfxClient.random.nextGaussian() * 0.24)
                .normalize()
                .scale((double)len / 16.0 * (0.5 + VfxClient.random.nextDouble() * 0.7));
            VfxClient.add(
                i % 3 == 0 ? ModParticles.spark(15269119, 0.35F, 18) : ModParticles.smoke(i % 2 == 0 ? 14217471 : 11065584, 1.4F, 24),
                mouth.x,
                mouth.y,
                mouth.z,
                v.x,
                v.y,
                v.z
            );
            if (i % 4 == 0) {
                VfxClient.add(ParticleTypes.SNOWFLAKE, mouth.x, mouth.y, mouth.z, v.x * 0.9, v.y * 0.9, v.z * 0.9);
            }
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(mouth), 0.2F, 1.4F, 11067647, 8));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(mouth.add(d.scale(1.5))), d, 0.3F, 2.4F, 0.4F, 16777215, 10, FRRenderTypes.SHOCK).alpha(0.6F));
    }

    private static void iceSpike(Vec3 pos, float s, int extra) {
        if (extra == 1) {
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.1F, 0.8F * s + 0.4F, 11067647, 6));
            VfxClient.burst(pos, Vec3.ZERO, 12, 0.3, ModParticles.spark(15269119, 0.35F, 10));
        } else {
            for (int i = 0; i < 5; i++) {
                Vec3 base = pos.add(VfxClient.random.nextGaussian() * 0.35 * (double)s, 0.0, VfxClient.random.nextGaussian() * 0.35 * (double)s);
                Vec3 tip = base.add(
                    VfxClient.random.nextGaussian() * 0.4,
                    (1.2 + VfxClient.random.nextDouble() * 1.3) * (double)s * (i == 0 ? 1.4 : 1.0),
                    VfxClient.random.nextGaussian() * 0.4
                );
                VfxEffects.add(new VfxTechniques2.CrystalFx(base, tip, (i == 0 ? 0.42F : 0.26F) * s, 46 + i * 3));
            }

            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.2F, 1.6F * s, 0.3F, 16777215, 10, FRRenderTypes.SHOCK)
                    .alpha(0.7F)
            );
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F * s, 1.3F * s, 0.0F, -1327958785, 60, FRRenderTypes.CRACKS, true
                )
            );
            VfxClient.burst(pos.add(0.0, 0.4, 0.0), new Vec3(0.0, 1.0, 0.0), (int)(16.0F * s), 0.35, ModParticles.spark(15269119, 0.4F, 14));
            VfxClient.burst(pos.add(0.0, 0.3, 0.0), Vec3.ZERO, (int)(6.0F * s), 0.08, ModParticles.smoke(14742783, 1.2F, 30));
        }
    }

    private static void iceCoffin(int entity, Vec3 pos, float h, int extra) {
        if (extra != 1) {
            VfxEffects.add(new VfxTechniques2.IceBlockFx(entity, pos, h, 60));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.follow(entity, pos, 0.02), new Vec3(0.0, 1.0, 0.0), 0.3F, 1.8F * h, 0.35F, 16777215, 10, FRRenderTypes.SHOCK)
            );
            VfxClient.burst(pos.add(0.0, (double)h * 0.5, 0.0), Vec3.ZERO, 24, 0.25, ModParticles.smoke(14742783, 1.0F, 26));
        } else {
            Vec3 c = pos.add(0.0, (double)h * 0.5, 0.0);
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(c), 0.5F, 3.0F * h, 16777215, 10));
            VfxEffects.add(
                new VfxEffects.Sphere(VfxClient.fixed(c), 0.3F, 2.0F * h, VfxDraw.argb(0.2F, 11067647), VfxDraw.argb(0.9F, 16777215), 10, true, 2.0F)
            );
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(c), new Vec3(0.0, 1.0, 0.0), 0.4F, 3.2F * h, 0.6F, 11067647, 12, FRRenderTypes.SHOCK));

            for (int i = 0; i < 14; i++) {
                Vec3 d = new Vec3(VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian() * 0.5 + 0.3, VfxClient.random.nextGaussian()).normalize();
                VfxEffects.add(new VfxTechniques2.CrystalFx(c, c.add(d.scale(0.8 + VfxClient.random.nextDouble() * 1.2 * (double)h)), 0.18F, 14));
            }

            VfxClient.burst(c, Vec3.ZERO, 60, 0.6, ModParticles.spark(15269119, 0.5F, 16));
        }
    }

    private static void glacier(Vec3 pos, float r) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 1.0, 0.0)), 2.0F, r * 0.9F, 16777215, 14));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 1.0F, r, VfxDraw.argb(0.15F, 11067647), VfxDraw.argb(0.85F, 15269119), 22, true, 2.4F));

        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        VfxClient.fixed(pos.add(0.0, 0.2, 0.0)),
                        new Vec3(0.0, 1.0, 0.0),
                        1.0F,
                        r * (1.0F + (float)i * 0.15F),
                        1.2F,
                        i == 0 ? 16777215 : 11067647,
                        20 + i * 4,
                        FRRenderTypes.SHOCK
                    )
                    .delay(i * 3)
            );
        }

        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.04, 0.0)), new Vec3(0.0, 1.0, 0.0), r * 0.6F, r * 1.05F, 0.0F, -1058473729, 160, FRRenderTypes.CRACKS, true
            )
        );
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), r * 0.8F, r, 0.02F, -2130706433, 160, FRRenderTypes.SEAL, false
            )
        );

        for (int i = 0; i < 260; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double d = VfxClient.random.nextDouble() * (double)r;
            VfxClient.add(
                (ParticleOptions)(i % 3 == 0 ? ParticleTypes.SNOWFLAKE : ModParticles.smoke(15268095, 1.6F, 50)),
                pos.x + Math.cos(a) * d,
                pos.y + 0.3 + VfxClient.random.nextDouble() * 3.0,
                pos.z + Math.sin(a) * d,
                -Math.sin(a) * 0.12,
                0.02,
                Math.cos(a) * 0.12
            );
        }
    }

    private static void whirlpool(Vec3 pos, Vec3 dir, float r, int duration) {
        Vec3 n = dir.y < -0.5 ? new Vec3(0.0, -1.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        int life = Math.max(30, duration);
        VfxEffects.add(new VfxEffects.Decal(VfxClient.fixed(pos.add(n.scale(0.05))), n, r * 0.3F, r, 0.35F, -804636054, life, FRRenderTypes.SEAL, false));
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(n.scale(0.07))), n, r * 0.2F, r * 0.9F, -0.45F, VfxDraw.argb(0.9F, 5945584), life, FRRenderTypes.RING, true
            )
        );
        VfxEffects.add(new VfxTechniques2.SwirlFx(pos, n, r, life));
    }

    private static void splash(Vec3 pos, float s) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.2F, 1.2F * s, 9097471, 6));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.2F, 1.4F * s, 0.3F, 16777215, 8, FRRenderTypes.SHOCK).alpha(0.6F));

        for (int i = 0; (float)i < 24.0F * s; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double sp = 0.1 + VfxClient.random.nextDouble() * 0.3 * (double)s;
            VfxClient.add(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, Math.cos(a) * sp, 0.2 + VfxClient.random.nextDouble() * 0.3, Math.sin(a) * sp);
        }

        VfxClient.burst(pos, Vec3.ZERO, (int)(6.0F * s), 0.15, ParticleTypes.BUBBLE_POP);
    }

    private static void graveImpact(Vec3 pos, float s, int extra) {
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, 1.3F * s, 0.0F, -803990512, 70, FRRenderTypes.CRACKS, false
            )
        );
        VfxEffects.add(
            new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.3F, 1.6F * s, 0.5F, 9077360, 12, FRRenderTypes.SHOCK)
                .alpha(0.7F)
        );
        VfxClient.burst(pos.add(0.0, 0.3, 0.0), new Vec3(0.0, 0.5, 0.0), (int)(14.0F * s), 0.12, ModParticles.smoke(5919816, 1.6F, 30));

        for (int i = 0; (float)i < 6.0F * s; i++) {
            VfxClient.debris(pos.add(VfxClient.random.nextGaussian() * 0.5, 0.0, VfxClient.random.nextGaussian() * 0.5), 2);
        }

        if (extra == 1) {
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 1.0, 0.0)), 0.5F, 3.0F * s, 2763288, 12));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, 2.6F * s, 0.8F, 10133600, 16, FRRenderTypes.SHOCK)
            );
            VfxClient.burst(pos.add(0.0, 1.0, 0.0), Vec3.ZERO, 30, 0.3, ModParticles.glow(10133600, 0.5F, 20));
        }
    }

    private static void dustCloud(Vec3 pos, float r, int color) {
        for (int i = 0; i < 90; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double sp = VfxClient.random.nextDouble() * 0.18 * (double)r;
            VfxClient.add(
                i % 3 == 0 ? ModParticles.glow(color, 0.3F, 30) : ModParticles.smoke(i % 2 == 0 ? 13152368 : 9075272, 0.9F, 40),
                pos.x,
                pos.y,
                pos.z,
                Math.cos(a) * sp,
                VfxClient.random.nextGaussian() * 0.05,
                Math.sin(a) * sp
            );
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.3F, r * 0.8F, color, 10));
    }

    private static void sting(Vec3 from, Vec3 vec, int target, int color) {
        Supplier<Vec3> to = VfxClient.follow(target, from.add(vec), 0.5);

        for (int i = 0; i < 3; i++) {
            Vec3 off = new Vec3((double)(i - 1) * 0.3, -0.2, VfxClient.random.nextGaussian() * 0.2);
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from.add(off)), to, 0.06F, color, 8 + i));
        }

        VfxEffects.add(new VfxEffects.Flash(to, 0.2F, 1.2F, color, 8));
        VfxClient.burst(from.add(vec), Vec3.ZERO, 10, 0.2, ModParticles.spark(color, 0.35F, 8));
    }

    private static void scissors(Vec3 pos, Vec3 dir, float s) {
        Vec3 fwd = VfxClient.dirOr(dir);
        Vec3 side = fwd.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 a = side.scale(0.7).add(up.scale(0.7)).normalize();
        Vec3 b = side.scale(-0.7).add(up.scale(0.7)).normalize();
        VfxEffects.add(new VfxEffects.Slash(pos, a, fwd, 1.1F * s, 0.18F, -1.2F, 1.2F, 16777215, 8));
        VfxEffects.add(new VfxEffects.Slash(pos, b, fwd, 1.1F * s, 0.18F, -1.2F, 1.2F, 15263984, 8));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.2F, 1.4F * s, 16724032, 6));
        VfxClient.blood(pos, 24, 0.8F);
    }

    private static void rainbow(Vec3 from, Vec3 vec) {
        int[] cols = new int[]{16728128, 16752704, 16773216, 6356864, 6340863, 11563263};
        Vec3 d = VfxClient.dirOr(vec);
        Vec3[] ab = VfxDraw.basis(d);

        for (int i = 0; i < cols.length; i++) {
            double a = (double)((float)i * (float) (Math.PI * 2) / (float)cols.length);
            Vec3 off = ab[0].scale(Math.cos(a) * 0.25).add(ab[1].scale(Math.sin(a) * 0.25));
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from.add(off)), VfxClient.fixed(from.add(vec).add(off)), 0.35F, cols[i], 14));
        }

        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(from.add(vec)), 0.25F, 16777215, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from.add(vec)), 0.5F, 3.0F, 15786239, 12));
        double len = vec.length();

        for (int i = 0; (double)i < len * 3.0; i++) {
            Vec3 at = from.add(d.scale(VfxClient.random.nextDouble() * len));
            VfxClient.add(
                ModParticles.spark(cols[i % cols.length], 0.5F, 12),
                at.x,
                at.y,
                at.z,
                VfxClient.random.nextGaussian() * 0.1,
                VfxClient.random.nextGaussian() * 0.1,
                VfxClient.random.nextGaussian() * 0.1
            );
        }
    }

    private VfxTechniques2() {
    }

    private static final class CoffinFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final float height;

        CoffinFx(int entity, Vec3 fallback, float height, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.entity = entity;
            this.fallback = fallback;
            this.height = height;
        }

        @Override
        protected void tick() {
            if (this.age % 20 == 0) {
                Vec3 c = VfxClient.follow(this.entity, this.fallback, 0.5).get();
                VfxEffects.add(
                    new VfxEffects.Ring(VfxClient.fixed(c), new Vec3(0.0, 1.0, 0.0), 0.4F, 2.2F * this.height, 0.35F, 13156480, 12, FRRenderTypes.SHOCK)
                        .alpha(0.7F)
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 b = VfxClient.follow(this.entity, this.fallback, 0.0).get();
            float close = VfxEffects.easeOut(Mth.clamp(age / 10.0F, 0.0F, 1.0F));
            float a = VfxEffects.fade(t, 0.02F, 0.1F);
            double h = (double)this.height * 1.15 + 0.2;
            double[] xs = new double[]{-0.35, 0.35, 0.55, 0.3, -0.3, -0.55};
            double[] zs = new double[]{-0.9, -0.9, -0.2, 0.9, 0.9, -0.2};
            int wood = VfxDraw.argb(0.9F * a, 9071168);
            int dark = VfxDraw.argb(0.8F * a, 3811864);

            for (int i = 0; i < 6; i++) {
                int j = (i + 1) % 6;
                double k = 1.6 - 0.6 * (double)close;
                Vec3 p0 = b.add(xs[i] * k, 0.0, zs[i] * 0.6 * k);
                Vec3 p1 = b.add(xs[j] * k, 0.0, zs[j] * 0.6 * k);
                VfxDraw.beam(m, vc, p0, p1, 0.1F, 0.1F, wood, wood, eye);
                VfxDraw.beam(m, vc, p0, p0.add(0.0, h * (double)close, 0.0), 0.1F, 0.1F, dark, dark, eye);
                VfxDraw.beam(m, vc, p0.add(0.0, h * (double)close, 0.0), p1.add(0.0, h * (double)close, 0.0), 0.1F, 0.1F, wood, wood, eye);
            }
        }
    }

    private static final class CrystalFx extends VfxEffects.Fx {
        private final Vec3 from;
        private final Vec3 tip;
        private final float width;

        CrystalFx(Vec3 from, Vec3 tip, float width, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.from = from;
            this.tip = tip;
            this.width = width;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float grow = VfxEffects.easeOut(Mth.clamp(age / 3.0F, 0.0F, 1.0F));
            Vec3 end = this.from.lerp(this.tip, (double)grow);
            float a = VfxEffects.fade(t, 0.0F, 0.3F);
            VfxDraw.beam(m, vc, this.from, end, this.width, 0.01F, VfxDraw.argb(0.85F * a, 10147060), VfxDraw.argb(0.95F * a, 15793407), eye);
            VfxDraw.beam(m, vc, this.from, end, this.width * 0.4F, 0.005F, VfxDraw.argb(a, 16777215), VfxDraw.argb(a, 16777215), eye);
        }
    }

    private static final class FramesFx extends VfxEffects.Fx {
        private final Vec3 from;
        private final Vec3 vec;
        private final float scale;
        private final int color;

        FramesFx(Vec3 from, Vec3 vec, float scale, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.from = from;
            this.vec = vec;
            this.scale = scale;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            int n = Math.max(3, (int)(this.vec.length() / 1.2));
            Vec3 d = VfxClient.dirOr(this.vec);
            Vec3 side = d.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = new Vec3(0.0, 1.0, 0.0);

            for (int i = 0; i <= n; i++) {
                float f = (float)i / (float)n;
                float a = Mth.clamp(1.0F - t * 1.6F + f * 0.6F, 0.0F, 1.0F) * 0.8F;
                if (!(a <= 0.02F)) {
                    Vec3 c = this.from.add(this.vec.scale((double)f));
                    Vec3 w = side.scale(0.45 * (double)this.scale);
                    Vec3 h = up.scale(0.95 * (double)this.scale);
                    Vec3 p1 = c.subtract(w).subtract(h);
                    Vec3 p2 = c.add(w).subtract(h);
                    Vec3 p3 = c.add(w).add(h);
                    Vec3 p4 = c.subtract(w).add(h);
                    int col = VfxDraw.argb(a, i == n ? 16777215 : this.color);
                    VfxDraw.beam(m, vc, p1, p2, 0.03F, 0.03F, col, col, eye);
                    VfxDraw.beam(m, vc, p2, p3, 0.03F, 0.03F, col, col, eye);
                    VfxDraw.beam(m, vc, p3, p4, 0.03F, 0.03F, col, col, eye);
                    VfxDraw.beam(m, vc, p4, p1, 0.03F, 0.03F, col, col, eye);
                }
            }
        }
    }

    private static final class IceBlockFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final float height;

        IceBlockFx(int entity, Vec3 fallback, float height, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.WHITE));
            this.entity = entity;
            this.fallback = fallback;
            this.height = height;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 base = VfxClient.follow(this.entity, this.fallback, 0.0).get();
            float grow = VfxEffects.easeOut(Mth.clamp(age / 6.0F, 0.0F, 1.0F));
            float a = VfxEffects.fade(t, 0.0F, 0.08F);
            float hw = 0.5F * Math.max(0.8F, this.height * 0.55F);
            float hh = this.height * 0.6F * grow;
            Vec3 c = base.add(0.0, (double)hh, 0.0);
            int col = VfxDraw.argb(0.45F * a, 12117247);
            Vec3 X = new Vec3(1.0, 0.0, 0.0);
            Vec3 Y = new Vec3(0.0, 1.0, 0.0);
            Vec3 Z = new Vec3(0.0, 0.0, 1.0);
            VfxDraw.plane(m, vc, c.add(0.0, 0.0, (double)hw), X.scale((double)hw), Y.scale((double)hh), 1.0F, col);
            VfxDraw.plane(m, vc, c.add(0.0, 0.0, (double)(-hw)), X.scale((double)hw), Y.scale((double)hh), 1.0F, col);
            VfxDraw.plane(m, vc, c.add((double)hw, 0.0, 0.0), Z.scale((double)hw), Y.scale((double)hh), 1.0F, col);
            VfxDraw.plane(m, vc, c.add((double)(-hw), 0.0, 0.0), Z.scale((double)hw), Y.scale((double)hh), 1.0F, col);
            VfxDraw.plane(m, vc, c.add(0.0, (double)hh, 0.0), X.scale((double)hw), Z.scale((double)hw), 1.0F, VfxDraw.argb(0.6F * a, 14743295));
        }
    }

    private static final class PanelFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final float height;
        private final int color;

        PanelFx(int entity, Vec3 fallback, float height, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.entity = entity;
            this.fallback = fallback;
            this.height = height;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = VfxClient.follow(this.entity, this.fallback, 0.5).get();
            Vec3 toEye = eye.subtract(c);
            Vec3 side = new Vec3(toEye.z, 0.0, -toEye.x);
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            float jitter = 0.03F * Mth.sin(age * 2.7F);
            float a = VfxEffects.fade(t, 0.05F, 0.1F);
            Vec3 w = side.scale(0.75 * (double)this.height * 0.6 + (double)jitter);
            Vec3 h = up.scale(0.65 * (double)this.height + (double)jitter);
            Vec3 p1 = c.subtract(w).subtract(h);
            Vec3 p2 = c.add(w).subtract(h);
            Vec3 p3 = c.add(w).add(h);
            Vec3 p4 = c.subtract(w).add(h);
            int col = VfxDraw.argb(a, this.color);
            int white = VfxDraw.argb(a, 16777215);
            VfxDraw.beam(m, vc, p1, p2, 0.07F, 0.07F, col, col, eye);
            VfxDraw.beam(m, vc, p2, p3, 0.07F, 0.07F, white, white, eye);
            VfxDraw.beam(m, vc, p3, p4, 0.07F, 0.07F, col, col, eye);
            VfxDraw.beam(m, vc, p4, p1, 0.07F, 0.07F, white, white, eye);

            for (int i = 0; i < 6; i++) {
                float f = ((float)i + 0.5F) / 6.0F;
                Vec3 l = p1.lerp(p4, (double)f).add(side.scale(-0.12));
                Vec3 r = p2.lerp(p3, (double)f).add(side.scale(0.12));
                VfxDraw.beam(m, vc, l, l.add(up.scale(0.08)), 0.06F, 0.06F, col, col, eye);
                VfxDraw.beam(m, vc, r, r.add(up.scale(0.08)), 0.06F, 0.06F, col, col, eye);
            }
        }
    }

    private static final class PoxFx extends VfxEffects.Fx {
        private final Vec3 center;
        private final float radius;

        PoxFx(Vec3 center, float radius, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.center = center;
            this.radius = radius;
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(center.add(0.0, 0.04, 0.0)),
                    new Vec3(0.0, 1.0, 0.0),
                    radius * 0.6F,
                    radius,
                    0.01F,
                    -1873126864,
                    life,
                    FRRenderTypes.SEAL,
                    false
                )
            );
        }

        @Override
        protected void tick() {
            int n = Math.max(2, (int)((double)this.radius * 1.2));

            for (int i = 0; i < n; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double r = Math.sqrt(VfxClient.random.nextDouble()) * (double)this.radius;
                VfxClient.add(
                    ModParticles.smoke(i % 3 == 0 ? 9079360 : (i % 3 == 1 ? 6972472 : 4868650), 1.8F, 40),
                    this.center.x + Math.cos(a) * r,
                    this.center.y + 0.2 + VfxClient.random.nextDouble() * 1.2,
                    this.center.z + Math.sin(a) * r,
                    VfxClient.random.nextGaussian() * 0.01,
                    0.015,
                    VfxClient.random.nextGaussian() * 0.01
                );
                if (i == 0 && this.age % 3 == 0) {
                    VfxClient.add(
                        ModParticles.glow(10133600, 0.3F, 20),
                        this.center.x + Math.cos(a) * r,
                        this.center.y + 0.5,
                        this.center.z + Math.sin(a) * r,
                        0.0,
                        0.03,
                        0.0
                    );
                }
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
        }
    }

    private static final class SoresFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final Vec3[] spots = new Vec3[7];

        SoresFx(int entity, Vec3 fallback, float height, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.entity = entity;
            this.fallback = fallback;

            for (int i = 0; i < this.spots.length; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                this.spots[i] = new Vec3(Math.cos(a) * 0.38, (0.15 + VfxClient.random.nextDouble() * 0.75) * (double)height, Math.sin(a) * 0.38);
            }
        }

        @Override
        protected void tick() {
            if (this.age % 8 == 0) {
                Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
                Vec3 o = this.spots[VfxClient.random.nextInt(this.spots.length)];
                VfxClient.add(ModParticles.smoke(6974000, 0.5F, 20), f.x + o.x, f.y + o.y, f.z + o.z, 0.0, 0.02, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
            float grow = Mth.clamp(age / 12.0F, 0.0F, 1.0F) * VfxEffects.fade(t, 0.0F, 0.15F);

            for (int i = 0; i < this.spots.length; i++) {
                float pulse = 0.75F + 0.25F * Mth.sin(age * 0.25F + (float)i * 1.3F);
                VfxDraw.billboard(m, vc, cam, f.add(this.spots[i]), 0.16F * grow * pulse, 0.0F, VfxDraw.argb(0.85F * grow, i % 2 == 0 ? 13156416 : 10133600));
            }
        }
    }

    private static final class SpeedAuraFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final int color;

        SpeedAuraFx(int entity, Vec3 fallback, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.entity = entity;
            this.fallback = fallback;
            this.color = color;
        }

        @Override
        protected void tick() {
            Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
            if (this.age % 2 == 0) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                VfxClient.add(
                    ModParticles.spark(this.color, 0.3F, 8),
                    f.x + Math.cos(a) * 0.5,
                    f.y + VfxClient.random.nextDouble() * 1.8,
                    f.z + Math.sin(a) * 0.5,
                    0.0,
                    0.02,
                    0.0
                );
            }

            if (this.age % 6 == 0) {
                VfxClient.add(ModParticles.glow(this.color, 0.6F, 10), f.x, f.y + 1.0, f.z, 0.0, 0.0, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
        }
    }

    private static final class SwirlFx extends VfxEffects.Fx {
        private final Vec3 center;
        private final Vec3 normal;
        private final float radius;

        SwirlFx(Vec3 center, Vec3 normal, float radius, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.center = center;
            this.normal = normal;
            this.radius = radius;
        }

        @Override
        protected void tick() {
            for (int i = 0; i < 4; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double r = (double)this.radius * (0.5 + VfxClient.random.nextDouble() * 0.5);
                double x = this.center.x + Math.cos(a) * r;
                double z = this.center.z + Math.sin(a) * r;
                VfxClient.add(
                    ParticleTypes.SPLASH,
                    x,
                    this.center.y + 0.2 * this.normal.y,
                    z,
                    -Math.sin(a) * 0.4 - Math.cos(a) * 0.2,
                    0.15 * this.normal.y,
                    Math.cos(a) * 0.4 - Math.sin(a) * 0.2
                );
                if (i == 0) {
                    VfxClient.add(
                        ModParticles.glow(2788048, 0.5F, 14), x, this.center.y + 0.3 * this.normal.y, z, -Math.cos(a) * 0.15, 0.0, -Math.sin(a) * 0.15
                    );
                }
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            VfxDraw.billboard(
                m,
                vc,
                cam,
                this.center.add(this.normal.scale(0.4)),
                this.radius * 0.35F,
                age * 0.2F,
                VfxDraw.argb(0.35F * VfxEffects.fade(t, 0.1F, 0.2F), 9099519)
            );
        }
    }

    private static final class WaveFx extends VfxEffects.Fx {
        private final Vec3 start;
        private final Vec3 dir;
        private final float length;

        WaveFx(Vec3 start, Vec3 dir, float length, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.start = start;
            this.dir = new Vec3(dir.x, 0.0, dir.z).normalize();
            this.length = length;
        }

        private Vec3 front(float t) {
            return this.start.add(this.dir.scale((double)Math.min(this.length, this.length * t * 1.1F)));
        }

        @Override
        protected void tick() {
            Vec3 f = this.front((float)this.age / (float)this.life);
            Vec3 side = this.dir.cross(new Vec3(0.0, 1.0, 0.0));

            for (int i = 0; i < 8; i++) {
                double k = VfxClient.random.nextDouble() * 2.0 - 1.0;
                Vec3 p = f.add(side.scale(k * 2.6)).add(0.0, 1.6 + VfxClient.random.nextDouble() * 0.6, 0.0);
                VfxClient.add(ParticleTypes.SPLASH, p.x, p.y, p.z, this.dir.x * 0.3, 0.2, this.dir.z * 0.3);
                if (i % 3 == 0) {
                    VfxClient.add(ModParticles.smoke(15268095, 1.0F, 18), p.x, p.y, p.z, this.dir.x * 0.2, 0.05, this.dir.z * 0.2);
                }
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 f = this.front(t);
            Vec3 side = this.dir.cross(new Vec3(0.0, 1.0, 0.0));
            float a = VfxEffects.fade(t, 0.05F, 0.2F);

            for (int i = -6; i <= 6; i++) {
                double k = (double)i / 6.0;
                Vec3 base = f.add(side.scale(k * 2.8)).subtract(this.dir.scale(k * k * 0.9));
                float h = (float)(2.4 * (1.0 - k * k * 0.5));
                VfxDraw.beam(
                    m,
                    vc,
                    base,
                    base.add(this.dir.scale(0.4)).add(0.0, (double)h, 0.0),
                    0.6F,
                    0.25F,
                    VfxDraw.argb(0.75F * a, 1731240),
                    VfxDraw.argb(0.9F * a, 14219519),
                    eye
                );
            }
        }
    }

    private static final class WheelFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final float scale;
        private final int turns;

        WheelFx(int entity, Vec3 fallback, float scale, int turns, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.entity = entity;
            this.fallback = fallback;
            this.scale = scale;
            this.turns = turns;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Entity e = VfxClient.level.getEntity(this.entity);
            Vec3 c = e != null ? e.position().add(0.0, (double)e.getBbHeight() + 0.6, 0.0) : this.fallback;
            float a = VfxEffects.fade(t, 0.05F, 0.4F);
            float spin = (float)Math.toRadians(
                (double)((float)(45 * Math.max(0, this.turns - 1)) + 45.0F * VfxEffects.easeOut(Mth.clamp(t * 2.5F, 0.0F, 1.0F)))
            );
            int col = VfxDraw.argb(a, 15788224);
            double R = 1.1 * (double)this.scale;

            for (int i = 0; i < 8; i++) {
                double ang = (double)spin + (double)i * Math.PI / 4.0;
                Vec3 tip = c.add(Math.cos(ang) * R, 0.0, Math.sin(ang) * R);
                VfxDraw.beam(m, vc, c, tip, 0.08F, 0.08F, col, col, eye);
                VfxDraw.beam(m, vc, tip, tip.add(0.0, 0.25 * (double)this.scale, 0.0), 0.12F, 0.04F, col, col, eye);
            }

            Vec3 prev = null;

            for (int i = 0; i <= 24; i++) {
                double ang = (double)(spin + (float)i * (float) (Math.PI * 2) / 24.0F);
                Vec3 p = c.add(Math.cos(ang) * R * 0.75, 0.0, Math.sin(ang) * R * 0.75);
                if (prev != null) {
                    VfxDraw.beam(m, vc, prev, p, 0.07F, 0.07F, col, col, eye);
                }

                prev = p;
            }
        }
    }
}
