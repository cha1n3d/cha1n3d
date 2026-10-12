package com.fallenroses.client.vfx;

import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class VfxTechniques3 {
    private static final int WHITE = 16777215;
    private static final int GREEN = 7004282;
    private static final int VOLT = 8048895;
    private static final int STAR = 16762960;
    private static final int ROT = 10123824;

    static void play(VfxPacket p) {
        Vec3 pos = p.pos();
        Vec3 dir = p.dir();
        float s = p.scale();
        int c = p.color() == 16777215 ? 16777215 : p.color();
        switch (p.kind()) {
            case ROUGH_BLAST:
                rough(pos, dir);
                break;
            case SHUTTER_DOORS:
                VfxEffects.add(new VfxTechniques3.DoorsFx(p.entity(), pos, dir, s, p.extra() == 1));
                break;
            case PACHINKO_REELS:
                reels(p.entity(), pos, s, p.extra());
                break;
            case JACKPOT:
                jackpot(p.entity(), pos, s, p.extra());
                break;
            case ELECTRIC_ARC:
                arc(pos, dir, s, p.extra());
                break;
            case STATIC_FIELD:
                VfxEffects.add(new VfxTechniques3.FieldFx(p.entity(), pos, s, Math.max(10, p.extra())));
                break;
            case AMBER_AURA:
                VfxEffects.add(new VfxTechniques3.AmberFx(p.entity(), pos, Math.max(20, p.extra())));
                VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(p.entity(), pos, 0.5), 0.5F, 5.0F, 16777215, 10));
                VfxEffects.add(
                    new VfxEffects.Ring(VfxClient.follow(p.entity(), pos, 0.05), new Vec3(0.0, 1.0, 0.0), 0.5F, 7.0F, 0.7F, 8048895, 16, FRRenderTypes.SHOCK)
                );
                break;
            case MASS_IMPACT:
                massImpact(pos, dir, s, p.extra());
                break;
            case GRAVITY_WELL:
                VfxEffects.add(new VfxTechniques3.WellFx(pos, s, Math.max(20, p.extra())));
                break;
            case BLACK_HOLE:
                if (p.extra() == 1) {
                    implode(pos, s);
                } else {
                    VfxEffects.add(new VfxTechniques3.HoleFx(pos, s, 122));
                }
                break;
            case GARUDA:
                garuda(pos, dir, p.extra());
                break;
            case ROT_CLOUD:
                VfxEffects.add(new VfxTechniques3.CloudFx(p.entity(), pos, s, Math.max(10, p.extra())));
                break;
            case ROT_BURST:
                rotBurst(pos, s);
                break;
            case WING_FAN:
                wing(pos, dir, s, c);
                break;
            case PRISON_REALM:
                prison(pos, s, p.extra());
                break;
            case CHAIN_PULL:
                chain(pos, dir, p.entity());
                break;
            case ROPE_LASH:
                lash(pos, dir);
                break;
            case VERDICT:
                verdict(pos, s, p.extra());
                break;
            case GRANITE_BEAM:
                graniteBeam(pos, dir, s, c);
                break;
            case JUDGEMAN:
                judgeman(p.entity(), pos, s, p.extra());
                break;
            case SWARM_STORM:
                VfxEffects.add(new VfxTechniques3.SwarmFx(p.entity(), pos, s, Math.max(20, p.extra())));
        }
    }

    private static void graniteBeam(Vec3 from, Vec3 vec, float w, int color) {
        Vec3 to = from.add(vec);
        Vec3 d = VfxClient.dirOr(vec);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(to), w, color, 22));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(to), w * 0.45F, 16777215, 18));
        int n = (int)(vec.length() / 4.0);

        for (int i = 0; i <= n; i++) {
            Vec3 at = from.add(d.scale((double)i * 4.0));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(at), d, w * 0.6F, w * 2.2F, 0.4F, 16773312, 12, FRRenderTypes.SHOCK).delay(i));
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from), 1.0F, w * 2.2F, 16777215, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(to), 1.5F, w * 3.0F, color, 16));
        VfxClient.burst(to, d.reverse(), 60, 0.9, ModParticles.spark(16773312, 0.7F, 18));
        VfxClient.burst(to, Vec3.ZERO, 20, 0.4, ModParticles.smoke(4866096, 2.0F, 30));
    }

    private static void judgeman(int entity, Vec3 pos, float s, int extra) {
        Supplier<Vec3> at = VfxClient.follow(entity, pos, 0.0);
        if (extra == 4) {
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.follow(entity, pos, 0.5), new Vec3(0.0, 1.0, 0.0), 0.4F, 2.4F, 0.35F, 15259808, 14, FRRenderTypes.SHOCK)
            );
            VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(entity, pos, 0.6), 0.3F, 2.0F, 16773312, 10));
        } else {
            if (extra == 0) {
                VfxEffects.add(new VfxTechniques3.JudgemanFx(at, s, 70));
            }

            int c = extra == 1 ? 16719904 : (extra == 2 ? 15259808 : 13158600);
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, (double)(4.0F * s), 0.0)), 0.5F, 4.0F * s, c, 14));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, 6.0F * s, 0.6F, c, 18, FRRenderTypes.SHOCK)
            );
        }
    }

    private static void rough(Vec3 hand, Vec3 dir) {
        Vec3 d = VfxClient.dirOr(dir);
        Vec3[] ab = VfxDraw.basis(d);
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(hand), 0.4F, 2.4F, 16777215, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(hand.add(d.scale(1.5))), 0.6F, 3.2F, 7004282, 9));

        for (int i = 0; i < 7; i++) {
            Vec3 spread = ab[0].scale(VfxClient.random.nextGaussian() * 0.18).add(ab[1].scale(VfxClient.random.nextGaussian() * 0.18));
            Vec3 to = hand.add(d.add(spread).normalize().scale(5.0 + VfxClient.random.nextDouble() * 3.0));
            VfxEffects.add(new VfxEffects.Bolt(hand, to, 0.3F, 0.35F, 6, 7004282, 6 + VfxClient.random.nextInt(3), true));
        }

        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(hand.add(d.scale(1.2))), d, 0.3F, 2.4F, 0.5F, 7004282, 8, FRRenderTypes.SHOCK));
        VfxClient.burst(hand.add(d), d, 24, 0.6, ModParticles.spark(14221280, 0.5F, 10));
        VfxClient.burst(hand.add(d), d, 10, 0.3, ModParticles.smoke(1722912, 0.8F, 14));
    }

    private static void reels(int entity, Vec3 pos, float s, int code) {
        if (code < 0) {
            VfxClient.burst(pos, Vec3.ZERO, 14, 0.25, ModParticles.spark(15266040, 0.35F, 10));
        } else {
            boolean big = code >= 1000;
            int life = big ? 70 : 40;
            Supplier<Vec3> at = VfxClient.follow(entity, pos, 1.0);
            Supplier<Vec3> above = () -> at.get().add(0.0, 0.9 + (double)s * 0.3, 0.0);
            MangaFx.reels(above, code % 1000, big ? new int[]{20, 36, 56} : new int[]{12, 20, 30}, life, s);
            VfxEffects.add(new VfxEffects.Flash(above, 0.4F, 2.4F * s, 7004282, 12));
            VfxEffects.add(new VfxEffects.Ring(at, new Vec3(0.0, 1.0, 0.0), 0.4F, 2.2F * s, 0.25F, 7004282, 18, FRRenderTypes.RING));
        }
    }

    private static void jackpot(int entity, Vec3 pos, float s, int ticks) {
        Supplier<Vec3> feet = VfxClient.follow(entity, pos, 0.0);
        VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, 14.0, 0.0), 1.6F, 7004282, 40));
        VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, 14.0, 0.0), 0.5F, 16777215, 30));

        for (int i = 0; i < 4; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(feet, new Vec3(0.0, 1.0, 0.0), 0.5F, 5.0F + (float)i * 2.0F, 0.6F, i % 2 == 0 ? 7004282 : 16773248, 24, FRRenderTypes.SHOCK)
                    .delay(i * 4)
            );
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(entity, pos, 0.5), 1.0F, 7.0F, 14221248, 16));

        for (int i = 0; i < 120; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double sp = 0.2 + VfxClient.random.nextDouble() * 0.5;
            Vec3 f = feet.get();

            int col = switch (i % 4) {
                case 0 -> 16773248;
                case 1 -> 7004282;
                case 2 -> 16738954;
                default -> 16777215;
            };
            VfxClient.add(ModParticles.star(col, 0.5F, 40), f.x, f.y + 1.0, f.z, Math.cos(a) * sp, 0.4 + VfxClient.random.nextDouble() * 0.5, Math.sin(a) * sp);
        }

        VfxEffects.add(new VfxTechniques3.LuckFx(entity, pos, ticks));
    }

    static void vtx(VertexConsumer vc, Matrix4f m, Vec3 p, int argb) {
        vc.vertex(m, (float)p.x, (float)p.y, (float)p.z).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24 & 0xFF).uv(0.5F, 0.5F).endVertex();
    }

    private static void arc(Vec3 from, Vec3 vec, float s, int extra) {
        Vec3 to = from.add(vec);
        int segs = Math.max(4, (int)(vec.length() * 1.6));
        float jitter = (float)Math.min(1.2, 0.25 + vec.length() * 0.06);
        VfxEffects.add(new VfxEffects.Bolt(from, to, 0.35F * s, jitter, segs, 8048895, 7, true));
        VfxEffects.add(new VfxEffects.Bolt(from, to, 0.12F * s, jitter, segs, 16777215, 6, true));
        if (extra == 1) {
            VfxEffects.add(new VfxEffects.Bolt(from.add(0.0, 0.5, 0.0), to.add(0.0, 0.5, 0.0), 0.25F, jitter * 1.5F, segs, 8048895, 9, true));
            VfxEffects.add(new VfxEffects.Bolt(from.add(0.0, -0.4, 0.0), to.add(0.0, -0.4, 0.0), 0.25F, jitter * 1.5F, segs, 8048895, 9, true));
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(to), 0.4F, 1.8F * s, 8048895, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(to), 0.2F, 0.8F * s, 16777215, 4));
        VfxClient.burst(to, Vec3.ZERO, 14, 0.4, ModParticles.spark(14743807, 0.4F, 8));
    }

    private static void massImpact(Vec3 pos, Vec3 dir, float s, int extra) {
        Vec3 n = extra == 1 ? new Vec3(0.0, 1.0, 0.0) : VfxClient.dirOr(dir);
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.6F * s, 2.6F * s, 16777215, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.8F * s, 3.6F * s, 16762960, 10));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), n, 0.3F, 2.4F * s, 0.6F * s, 16762960, 10, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), n, 0.2F, 1.8F * s, 0.3F * s, 16777215, 7, FRRenderTypes.RING).delay(1));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 0.2F * s, 1.3F * s, -1072691196, 0, 8, false, 1.4F));
        VfxClient.burst(pos, n, (int)(20.0F * s), 0.5 * (double)s, ModParticles.spark(16773312, 0.6F, 12));
        if (extra == 1) {
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F * s, 1.4F * s, 0.0F, -534768632, 60, FRRenderTypes.CRACKS, false
                )
            );

            for (int i = 0; i < 3; i++) {
                VfxEffects.add(
                    new VfxEffects.Ring(
                            VfxClient.fixed(pos.add(0.0, 0.15, 0.0)),
                            new Vec3(0.0, 1.0, 0.0),
                            0.5F,
                            s * (1.3F + (float)i * 0.3F),
                            0.5F,
                            i == 0 ? 16777215 : 16762960,
                            14 + i * 3,
                            FRRenderTypes.SHOCK
                        )
                        .delay(i * 2)
                );
            }

            VfxClient.debris(pos, (int)(20.0F * s));
            VfxClient.burst(pos, new Vec3(0.0, 1.0, 0.0), (int)(10.0F * s), 0.25, ModParticles.smoke(3813408, 1.6F, 30));
        }
    }

    private static void implode(Vec3 c, float r) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(c), 1.0F, r * 1.4F, 16777215, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(c), 2.0F, r * 2.0F, 16762960, 18));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(c), 0.5F, r * 0.8F, 1627386048, -20416, 14, true, 2.0F));

        for (int i = 0; i < 4; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        VfxClient.fixed(c),
                        new Vec3(0.0, 1.0, 0.0),
                        1.0F,
                        r * (1.0F + (float)i * 0.4F),
                        1.2F,
                        i == 0 ? 16777215 : 16762960,
                        18 + i * 4,
                        FRRenderTypes.SHOCK
                    )
                    .delay(i * 2)
            );
        }

        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(c.add(0.0, -2.0, 0.0)), VfxClient.fixed(c.add(0.0, 30.0, 0.0)), 2.5F, 16762960, 20));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(c.add(0.0, -2.0, 0.0)), VfxClient.fixed(c.add(0.0, 30.0, 0.0)), 0.9F, 16777215, 14));
        VfxClient.burst(c, Vec3.ZERO, 160, 1.2, ModParticles.spark(16773312, 0.8F, 20));
        VfxClient.burst(c, Vec3.ZERO, 40, 0.5, ModParticles.smoke(2759176, 2.5F, 40));
    }

    private static void garuda(Vec3 pos, Vec3 dir, int extra) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.4F, 2.2F, 16762960, 10));
        VfxClient.burst(pos, dir, 20, 0.35, ModParticles.star(16773312, 0.4F, 20));
        if (extra == 1) {
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(pos), VfxClient.fixed(pos.add(0.0, 6.0, 0.0)), 0.6F, 16762960, 12));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, 3.0F, 0.5F, 16762960, 12, FRRenderTypes.SHOCK)
            );
            VfxClient.debris(pos, 12);
        }
    }

    private static void rotBurst(Vec3 pos, float s) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.4F * s, 2.0F * s, 13148224, 8));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 0.2F * s, 1.1F * s, -1069929976, 1616527376, 10, false, 1.4F));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.3F, 2.0F * s, 0.4F * s, 10123824, 10, FRRenderTypes.SHOCK));
        VfxClient.burst(pos, Vec3.ZERO, (int)(24.0F * s), 0.4 * (double)s, ModParticles.glow(6967312, 0.45F, 16));
        VfxClient.burst(pos, Vec3.ZERO, (int)(10.0F * s), 0.2 * (double)s, ModParticles.smoke(2760198, 1.2F, 26));
    }

    private static void wing(Vec3 pos, Vec3 dir, float range, int color) {
        Vec3 f = VfxClient.dirOr(new Vec3(dir.x, 0.0, dir.z));
        Vec3 side = f.cross(new Vec3(0.0, 1.0, 0.0)).normalize();

        for (int i = 0; i < 3; i++) {
            Vec3 c = pos.add(0.0, -0.3 + (double)i * 0.35, 0.0);
            VfxEffects.add(new VfxEffects.Slash(c, f, side, range * (0.55F + (float)i * 0.12F), 0.5F + (float)i * 0.1F, -1.0F, 1.0F, color, 9).delay(i));
            VfxEffects.add(new VfxEffects.Slash(c, f, side, range * (0.56F + (float)i * 0.12F), 0.15F, -1.0F, 1.0F, 16728112, 8).delay(i));
        }

        for (int i = 0; i < 30; i++) {
            double a = -1.0 + VfxClient.random.nextDouble() * 2.0;
            double d = (double)range * (0.3 + VfxClient.random.nextDouble() * 0.7);
            Vec3 p = pos.add(f.scale(Math.cos(a) * d)).add(side.scale(Math.sin(a) * d));
            VfxClient.add(ModParticles.glow(i % 2 == 0 ? 6950928 : 9072672, 0.4F, 14), p.x, p.y, p.z, 0.0, -0.03, 0.0);
        }
    }

    private static void prison(Vec3 pos, float s, int extra) {
        int c = extra == 1 ? 15261904 : 6957616;
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.5F, 3.0F * s, 15788256, 10));
        VfxEffects.add(
            new VfxEffects.Decal(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.3F, 2.4F * s, 0.15F, VfxDraw.argb(0.9F, c), 26, FRRenderTypes.SEAL, false)
        );

        for (int i = 0; i < 6; i++) {
            Vec3 n = switch (i) {
                case 0 -> new Vec3(1.0, 0.0, 0.0);
                case 1 -> new Vec3(-1.0, 0.0, 0.0);
                case 2 -> new Vec3(0.0, 1.0, 0.0);
                case 3 -> new Vec3(0.0, -1.0, 0.0);
                case 4 -> new Vec3(0.0, 0.0, 1.0);
                default -> new Vec3(0.0, 0.0, -1.0);
            };
            VfxEffects.add(
                new VfxEffects.Decal(
                        VfxClient.fixed(pos.add(n.scale(0.9 * (double)s))),
                        n,
                        0.2F,
                        0.9F * s,
                        0.05F,
                        VfxDraw.argb(0.8F, 2755088),
                        20,
                        FRRenderTypes.SEAL,
                        false
                    )
                    .delay(i)
            );
        }

        VfxEffects.add(
            new VfxEffects.Ring(
                VfxClient.fixed(pos),
                new Vec3(0.0, 1.0, 0.0),
                extra == 1 ? 0.3F : 3.0F * s,
                extra == 1 ? 3.0F * s : 0.3F,
                0.4F,
                15788256,
                14,
                FRRenderTypes.SHOCK
            )
        );
        VfxClient.burst(pos, Vec3.ZERO, 30, 0.3, ModParticles.smoke(1706512, 1.0F, 24));
    }

    private static void chain(Vec3 from, Vec3 vec, int target) {
        Supplier<Vec3> to = VfxClient.follow(target, from.add(vec), 0.5);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), to, 0.12F, 13158608, 12));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), to, 0.04F, 16777215, 10));
        VfxEffects.add(new VfxEffects.Flash(to, 0.3F, 1.2F, 14737640, 8));
        VfxClient.burst(from.add(vec), Vec3.ZERO, 12, 0.3, ModParticles.spark(14737640, 0.4F, 8));
    }

    private static void lash(Vec3 pos, Vec3 dir) {
        Vec3 d = VfxClient.dirOr(dir);
        Vec3[] ab = VfxDraw.basis(d);
        VfxEffects.add(new VfxEffects.Slash(pos.subtract(d.scale(0.8)), d, ab[0], 1.4F, 0.2F, -1.0F, 1.0F, 6974080, 7));
        VfxClient.burst(pos, d, 20, 0.35, ModParticles.smoke(657936, 0.6F, 14));
        VfxClient.burst(pos, d, 14, 0.5, ModParticles.spark(11579647, 0.35F, 10));
    }

    private static void verdict(Vec3 pos, float s, int extra) {
        if (extra == 1) {
            Vec3 top = pos.add(0.0, 9.0, 0.0);
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(pos), 1.2F, 15790320, 12));
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(pos), 0.4F, 16777215, 10));
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 1.0F, 5.0F, 16777215, 12));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.5F, 5.0F, 0.8F, 14737632, 14, FRRenderTypes.SHOCK));
            VfxClient.burst(pos, Vec3.ZERO, 50, 0.8, ModParticles.spark(16777215, 0.6F, 14));
        } else {
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(pos.add(0.0, 0.05, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, 3.0F * s, 0.05F, -521609008, 50, FRRenderTypes.SEAL, false
                )
            );
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 1.0, 0.0)), 0.4F, 2.0F, 14737632, 14));
        }
    }

    private VfxTechniques3() {
    }

    private static final class AmberFx extends VfxEffects.Fx {
        private final Supplier<Vec3> body;

        AmberFx(int entity, Vec3 pos, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.body = VfxClient.follow(entity, pos, 0.5);
        }

        @Override
        protected void tick() {
            Vec3 c = this.body.get();
            if (this.age % 2 == 0) {
                Vec3 a = c.add(VfxClient.random.nextGaussian() * 0.4, VfxClient.random.nextGaussian() * 0.6, VfxClient.random.nextGaussian() * 0.4);
                Vec3 b = c.add(VfxClient.random.nextGaussian() * 1.1, VfxClient.random.nextGaussian() * 1.1, VfxClient.random.nextGaussian() * 1.1);
                VfxEffects.add(new VfxEffects.Bolt(a, b, 0.1F, 0.3F, 5, this.age % 4 == 0 ? 16777215 : 8048895, 3, true));
            }

            if (this.age % 15 == 0) {
                double ang = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                Vec3 g = c.add(Math.cos(ang) * 3.0, -1.0, Math.sin(ang) * 3.0);
                VfxEffects.add(new VfxEffects.Bolt(g.add(0.0, 8.0, 0.0), g, 0.3F, 0.8F, 10, 8048895, 5, true));
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = this.body.get();
            float a = VfxEffects.fade(t, 0.02F, 0.05F) * (0.7F + 0.3F * VfxClient.random.nextFloat());
            VfxDraw.billboard(m, vc, cam, c, 1.8F, 0.0F, VfxDraw.argb(a * 0.45F, 8048895));
            VfxDraw.billboard(m, vc, cam, c, 0.9F, 0.0F, VfxDraw.argb(a * 0.35F, 16777215));
        }
    }

    private static final class CloudFx extends VfxEffects.Fx {
        private final Supplier<Vec3> feet;
        private final float r;

        CloudFx(int entity, Vec3 pos, float r, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.SEAL));
            this.feet = VfxClient.follow(entity, pos, 0.0);
            this.r = r;
        }

        @Override
        protected void tick() {
            Vec3 f = this.feet.get();
            int n = (int)(this.r * 1.5F);

            for (int i = 0; i < n; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double d = Math.sqrt(VfxClient.random.nextDouble()) * (double)this.r;
                VfxClient.add(
                    ModParticles.smoke(i % 3 == 0 ? 9072672 : 3811848, 1.4F, 30),
                    f.x + Math.cos(a) * d,
                    f.y + 0.2 + VfxClient.random.nextDouble(),
                    f.z + Math.sin(a) * d,
                    0.0,
                    0.01,
                    0.0
                );
            }

            if (this.age % 4 == 0) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double d = VfxClient.random.nextDouble() * (double)this.r;
                VfxClient.add(ModParticles.glow(13148224, 0.35F, 18), f.x + Math.cos(a) * d, f.y + 0.3, f.z + Math.sin(a) * d, 0.0, 0.04, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3[] ab = VfxDraw.basis(new Vec3(0.0, 1.0, 0.0));
            VfxDraw.plane(m, vc, this.feet.get().add(0.0, 0.05, 0.0), ab[0], ab[1], this.r, VfxDraw.argb(0.5F * VfxEffects.fade(t, 0.1F, 0.2F), 2760198));
        }
    }

    private static final class DoorsFx extends VfxEffects.Fx {
        private final Supplier<Vec3> at;
        private final Vec3 side;
        private final float h;
        private final boolean slam;

        DoorsFx(int entity, Vec3 pos, Vec3 side, float h, boolean slam) {
            super(slam ? 10 : 8, FRRenderTypes.translucent(FRRenderTypes.WHITE));
            this.at = VfxClient.follow(entity, pos, 0.0);
            this.side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(side.x, 0.0, side.z).normalize();
            this.h = h;
            this.slam = slam;
            if (slam) {
                Vec3 c = pos.add(0.0, (double)h * 0.5, 0.0);
                VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(c), 0.5F, 3.5F, 16777215, 6));
                VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(c), this.side, 0.3F, 3.0F, 0.5F, 7004282, 10, FRRenderTypes.SHOCK));
                VfxClient.burst(c, Vec3.ZERO, 30, 0.6, ModParticles.spark(14221280, 0.5F, 12));
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 base = this.at.get();
            float open = this.slam ? 0.0F : 1.0F - VfxEffects.easeOut(t);
            float a = this.slam ? VfxEffects.fade(t, 0.0F, 0.5F) : Math.min(1.0F, t * 4.0F);
            float halfW = 0.55F + this.h * 0.25F;
            float height = this.h * 1.3F;
            Vec3 fwd = this.side.cross(new Vec3(0.0, 1.0, 0.0)).normalize();

            for (int k = -1; k <= 1; k += 2) {
                Vec3 center = base.add(this.side.scale((double)((float)k * (halfW + open * 2.2F)) * 0.5));
                door(m, vc, center, this.side, fwd, halfW, height, VfxDraw.argb(a * 0.95F, 1980964), VfxDraw.argb(a, 7004282));
            }
        }

        private static void door(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 side, Vec3 fwd, float halfW, float height, int body, int edge) {
            quad(m, vc, c.add(side.scale((double)(-halfW / 2.0F))), c.add(side.scale((double)(halfW / 2.0F))), height, body);

            for (int i = 1; i < 5; i++) {
                Vec3 y = c.add(0.0, (double)(height * (float)i / 5.0F), 0.0);
                quadStrip(m, vc, y.add(side.scale((double)(-halfW / 2.0F))), y.add(side.scale((double)(halfW / 2.0F))), 0.06F, edge);
            }

            quadStrip(m, vc, c.add(side.scale((double)(-halfW / 2.0F))), c.add(side.scale((double)(halfW / 2.0F))), 0.1F, edge);
            quadStrip(
                m,
                vc,
                c.add(0.0, (double)height - 0.1, 0.0).add(side.scale((double)(-halfW / 2.0F))),
                c.add(0.0, (double)height - 0.1, 0.0).add(side.scale((double)(halfW / 2.0F))),
                0.1F,
                edge
            );
        }

        private static void quad(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float height, int argb) {
            VfxTechniques3.vtx(vc, m, a, argb);
            VfxTechniques3.vtx(vc, m, a.add(0.0, (double)height, 0.0), argb);
            VfxTechniques3.vtx(vc, m, b.add(0.0, (double)height, 0.0), argb);
            VfxTechniques3.vtx(vc, m, b, argb);
        }

        private static void quadStrip(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float th, int argb) {
            quad(m, vc, a, b, th, argb);
        }
    }

    private static final class FieldFx extends VfxEffects.Fx {
        private final Supplier<Vec3> feet;
        private final float r;

        FieldFx(int entity, Vec3 pos, float r, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.RING));
            this.feet = VfxClient.follow(entity, pos, 0.0);
            this.r = r;
        }

        @Override
        protected void tick() {
            if (this.age % 3 == 0) {
                Vec3 f = this.feet.get();
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double d = VfxClient.random.nextDouble() * (double)this.r;
                Vec3 g = f.add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
                VfxEffects.add(new VfxEffects.Bolt(f.add(0.0, 1.2, 0.0), g, 0.12F, 0.5F, 6, 8048895, 4, true));
                VfxClient.add(ModParticles.spark(16777215, 0.4F, 6), g.x, g.y, g.z, 0.0, 0.1, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 f = this.feet.get().add(0.0, 0.06, 0.0);
            float a = VfxEffects.fade(t, 0.05F, 0.15F);
            float flicker = 0.6F + 0.4F * VfxClient.random.nextFloat();
            VfxDraw.ring(m, vc, f, new Vec3(0.0, 1.0, 0.0), this.r, 0.35F, VfxDraw.argb(a * flicker * 0.8F, 8048895), 64);
            VfxDraw.ring(m, vc, f, new Vec3(0.0, 1.0, 0.0), this.r * 0.6F, 0.15F, VfxDraw.argb(a * 0.5F, 16777215), 48);
        }
    }

    private static final class HoleFx extends VfxEffects.Fx {
        private final Vec3 c;
        private final float r;

        HoleFx(Vec3 c, float r, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.WHITE));
            this.c = c;
            this.r = r;
        }

        @Override
        protected void tick() {
            for (int i = 0; i < 8; i++) {
                Vec3 d = new Vec3(VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian() * 0.4, VfxClient.random.nextGaussian())
                    .normalize()
                    .scale((double)this.r * (0.5 + VfxClient.random.nextDouble() * 0.5));
                Vec3 p = this.c.add(d);
                VfxClient.add(ModParticles.spark(i % 3 == 0 ? 16777215 : 16762960, 0.5F, 14), p.x, p.y, p.z, -d.x * 0.08, -d.y * 0.08, -d.z * 0.08);
            }

            if (this.age % 10 == 0) {
                VfxEffects.add(
                    new VfxEffects.Ring(VfxClient.fixed(this.c), new Vec3(0.0, 1.0, 0.0), this.r * 0.9F, 1.2F, 0.6F, 16762960, 14, FRRenderTypes.SHOCK)
                        .alpha(0.7F)
                );
            }

            if (this.age % 4 == 0) {
                VfxClient.add(
                    ModParticles.smoke(394242, 2.5F, 30),
                    this.c.x + VfxClient.random.nextGaussian() * (double)this.r * 0.4,
                    this.c.y,
                    this.c.z + VfxClient.random.nextGaussian() * (double)this.r * 0.4,
                    0.0,
                    0.0,
                    0.0
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float grow = VfxEffects.easeOut(Math.min(1.0F, t * 6.0F));
            float core = 1.8F * grow * (1.0F + 0.05F * Mth.sin(age * 0.8F));
            float a = VfxEffects.fade(t, 0.02F, 0.03F);
            VfxDraw.sphere(m, vc, this.c, core, VfxDraw.argb(a, 0), VfxDraw.argb(a, 1050628), eye, 12, 18, 1.2F);
            Vec3 tilt = new Vec3(Math.sin((double)age * 0.02) * 0.3, 1.0, Math.cos((double)age * 0.03) * 0.3).normalize();

            for (int i = 0; i < 4; i++) {
                float rr = core * (1.4F + (float)i * 0.5F);
                VfxDraw.ring(
                    m, vc, this.c, tilt, rr, 0.25F + (float)i * 0.06F, VfxDraw.argb(a * (0.8F - (float)i * 0.15F), i % 2 == 0 ? 16756800 : 16773312), 56
                );
            }
        }
    }

    private static final class JudgemanFx extends VfxEffects.Fx {
        private final Supplier<Vec3> base;
        private final float s;

        JudgemanFx(Supplier<Vec3> base, float s, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.base = base;
            this.s = s;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 b = this.base.get().add(0.0, 0.0, 0.0);
            float a = VfxEffects.fade(t, 0.1F, 0.15F);
            int black = VfxDraw.argb(0.95F * a, 328965);
            float h = 6.5F * this.s;
            VfxDraw.beam(m, vc, b, b.add(0.0, (double)h * 0.75, 0.0), 1.6F * this.s, 0.8F * this.s, black, black, eye);
            Vec3 head = b.add(0.0, (double)h * 0.82, 0.0);
            VfxDraw.beam(
                m, vc, head.add(0.0, -0.5 * (double)this.s, 0.0), head.add(0.0, 0.6 * (double)this.s, 0.0), 0.55F * this.s, 0.5F * this.s, black, black, eye
            );
            int white = VfxDraw.argb(0.95F * a, 15790320);
            VfxDraw.beam(
                m,
                vc,
                head.add(-0.6 * (double)this.s, 0.1 * (double)this.s, 0.0),
                head.add(0.6 * (double)this.s, 0.1 * (double)this.s, 0.0),
                0.12F * this.s,
                0.12F * this.s,
                white,
                white,
                eye
            );
            float tilt = Mth.sin(age * 0.15F) * 0.4F;
            int gold = VfxDraw.argb(0.95F * a, 15253600);
            Vec3 top = b.add(0.0, (double)h * 1.02, 0.0);
            Vec3 l = top.add(-1.4 * (double)this.s, (double)(tilt * this.s), 0.0);
            Vec3 r = top.add(1.4 * (double)this.s, (double)(-tilt * this.s), 0.0);
            VfxDraw.beam(
                m, vc, top.add(0.0, -0.6 * (double)this.s, 0.0), top.add(0.0, 0.3 * (double)this.s, 0.0), 0.08F * this.s, 0.08F * this.s, gold, gold, eye
            );
            VfxDraw.beam(m, vc, l, r, 0.07F * this.s, 0.07F * this.s, gold, gold, eye);
            VfxDraw.beam(m, vc, l, l.add(0.0, -0.8 * (double)this.s, 0.0), 0.04F * this.s, 0.25F * this.s, gold, gold, eye);
            VfxDraw.beam(m, vc, r, r.add(0.0, -0.8 * (double)this.s, 0.0), 0.04F * this.s, 0.25F * this.s, gold, gold, eye);
        }
    }

    private static final class LuckFx extends VfxEffects.Fx {
        private final Supplier<Vec3> feet;

        LuckFx(int entity, Vec3 pos, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.RING));
            this.feet = VfxClient.follow(entity, pos, 0.0);
        }

        @Override
        protected void tick() {
            Vec3 f = this.feet.get();
            if (this.age % 3 == 0) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                VfxClient.add(
                    ModParticles.glow(this.age % 6 == 0 ? 16773248 : 7004282, 0.5F, 24),
                    f.x + Math.cos(a) * 0.6,
                    f.y + 0.1,
                    f.z + Math.sin(a) * 0.6,
                    0.0,
                    0.08,
                    0.0
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 f = this.feet.get().add(0.0, 0.05, 0.0);
            float pulse = 0.5F + 0.5F * Mth.sin(age * 0.3F);
            VfxDraw.ring(m, vc, f, new Vec3(0.0, 1.0, 0.0), 1.1F + pulse * 0.2F, 0.25F, VfxDraw.argb(0.6F * VfxEffects.fade(t, 0.02F, 0.1F), 7004282), 40);
        }
    }

    private static final class SwarmFx extends VfxEffects.Fx {
        private final Supplier<Vec3> feet;
        private final float r;

        SwarmFx(int entity, Vec3 pos, float r, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.SEAL));
            this.feet = VfxClient.follow(entity, pos, 0.0);
            this.r = r;
        }

        @Override
        protected void tick() {
            Vec3 f = this.feet.get();

            for (int i = 0; i < 14; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double d = VfxClient.random.nextDouble() * (double)this.r;
                double y = VfxClient.random.nextDouble() * 4.0;
                double sp = 0.25 + d * 0.02;
                VfxClient.add(
                    ModParticles.smoke(i % 4 == 0 ? 4860426 : 1182212, 0.5F, 16),
                    f.x + Math.cos(a) * d,
                    f.y + y,
                    f.z + Math.sin(a) * d,
                    -Math.sin(a) * sp,
                    0.02,
                    Math.cos(a) * sp
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3[] ab = VfxDraw.basis(new Vec3(0.0, 1.0, 0.0));
            float rot = age * 0.2F;
            Vec3 ax = ab[0].scale(Math.cos((double)rot)).add(ab[1].scale(Math.sin((double)rot)));
            Vec3 ay = ab[0].scale(-Math.sin((double)rot)).add(ab[1].scale(Math.cos((double)rot)));
            VfxDraw.plane(m, vc, this.feet.get().add(0.0, 0.05, 0.0), ax, ay, this.r, VfxDraw.argb(0.6F * VfxEffects.fade(t, 0.05F, 0.15F), 1708038));
        }
    }

    private static final class WellFx extends VfxEffects.Fx {
        private final Vec3 c;
        private final float r;

        WellFx(Vec3 c, float r, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.SEAL));
            this.c = c;
            this.r = r;
        }

        @Override
        protected void tick() {
            if (this.age % 6 == 0) {
                VfxEffects.add(
                    new VfxEffects.Ring(
                            VfxClient.fixed(this.c.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), this.r, 0.3F, 0.4F, 16762960, 12, FRRenderTypes.SHOCK
                        )
                        .alpha(0.6F)
                );
            }

            for (int i = 0; i < 4; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double d = (double)this.r * (0.6 + VfxClient.random.nextDouble() * 0.4);
                VfxClient.add(
                    ModParticles.smoke(4864544, 0.8F, 16),
                    this.c.x + Math.cos(a) * d,
                    this.c.y + 0.2,
                    this.c.z + Math.sin(a) * d,
                    -Math.cos(a) * d * 0.06,
                    -0.02,
                    -Math.sin(a) * d * 0.06
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3[] ab = VfxDraw.basis(new Vec3(0.0, 1.0, 0.0));
            float rot = age * 0.08F;
            Vec3 ax = ab[0].scale(Math.cos((double)rot)).add(ab[1].scale(Math.sin((double)rot)));
            Vec3 ay = ab[0].scale(-Math.sin((double)rot)).add(ab[1].scale(Math.cos((double)rot)));
            VfxDraw.plane(m, vc, this.c.add(0.0, 0.05, 0.0), ax, ay, this.r, VfxDraw.argb(0.85F * VfxEffects.fade(t, 0.1F, 0.1F), 1708550));
        }
    }
}
