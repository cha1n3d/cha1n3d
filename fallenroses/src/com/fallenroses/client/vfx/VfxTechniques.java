package com.fallenroses.client.vfx;

import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class VfxTechniques {
    private static final int WHITE = 16777215;
    private static final int BLOOD = 13635632;
    private static final int BLOOD_DARK = 4849672;
    private static final int GOLD = 15253600;
    private static final int FIRE = 16738832;
    private static final int FIRE_LIGHT = 16765056;
    private static final int SOUL = 8044728;
    private static final int GREEN = 5943360;
    private static final int JADE = 3844744;

    static void play(VfxPacket p) {
        Vec3 pos = p.pos();
        Vec3 dir = p.dir();
        float s = p.scale();
        int c = p.color();
        switch (p.kind()) {
            case BLOOD_CHARGE:
                bloodCharge(p.entity(), pos, p.extra());
                break;
            case BLOOD_BEAM:
                beam(pos, dir, 0.35F * s, 13635632, 16765144, 14);
                break;
            case BLOOD_BURST:
                bloodBurst(pos, s);
                break;
            case RED_SCALE:
                redScale(p.entity(), pos, p.extra());
                break;
            case BLOOD_CLOUD:
                bloodCloud(pos, s, p.extra());
                break;
            case BLOOD_SPIKE:
                bloodSpike(pos);
                break;
            case RATIO_MARK:
                ratioMark(p.entity(), pos, dir, s, c);
                break;
            case RATIO_CRIT:
                ratioCrit(pos, dir, s, p.extra());
                break;
            case COLLAPSE:
                collapse(pos, dir, s);
                break;
            case OVERTIME:
                overtime(p.entity(), pos, p.extra());
                break;
            case NAIL_HIT:
                nailHit(pos, dir);
                break;
            case RESONANCE_DOLL:
                doll(p.entity(), pos, s, c);
                break;
            case RESONANCE_HIT:
                resonanceHit(p.entity(), pos, dir, s, c, p.extra());
                break;
            case FIRE_PILLAR:
                firePillar(pos, s, p.extra());
                break;
            case FLAME_CONE:
                flameCone(pos, dir, s);
                break;
            case EMBER_BOOM:
                emberBoom(pos, s, c);
                break;
            case METEOR_IMPACT:
                meteorImpact(pos, s);
                break;
            case SOUL_TOUCH:
                soulTouch(p.entity(), pos, s, p.extra());
                break;
            case REPEL_SWEEP:
                repelSweep(pos, dir, s);
                break;
            case SPIKES:
                spikes(pos, s);
                break;
            case BUDS:
                buds(p.entity(), pos, s, p.extra());
                break;
            case FLOWER_FIELD:
                flowerField(pos, s, p.extra());
                break;
            case LIGHT_BEAM:
                beam(pos, dir, 0.6F * s, 13172656, 16777215, 22);
                break;
            case SUMMON:
                summon(pos, s, c);
                break;
            case ABSORB:
                absorb(p.entity(), pos, s, p.extra());
                break;
            case UZUMAKI_CHARGE:
                uzumakiCharge(p.entity(), pos, s, p.extra());
                break;
            case UZUMAKI_BEAM:
                uzumakiBeam(pos, dir, s);
                break;
            case DRAGON_JET:
                dragonJet(pos, dir, s);
                break;
            case LIGHTNING:
                lightning(pos);
                break;
            case EYE_BEAM:
                eyeBeam(p.entity(), pos, dir, p.extra());
                break;
            case BEARER_SCREAM:
                scream(pos, dir);
                break;
            default:
                VfxTechniques2.play(p);
        }
    }

    private static void beam(Vec3 from, Vec3 vec, float w, int color, int core, int life) {
        Vec3 to = from.add(vec);
        Vec3 d = VfxClient.dirOr(vec);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(to), w * 2.4F, color, life));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(to), w, core, life - 4));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from), 0.4F, w * 6.0F, color, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(to), 0.4F, w * 8.0F, color, 12));
        double len = vec.length();

        for (int i = 1; (double)i < Math.min(12.0, len / 2.0); i++) {
            Vec3 at = from.add(d.scale((double)i * 2.0));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(at), d, w * 1.2F, w * 4.0F, w * 0.6F, color, 8, FRRenderTypes.RING).alpha(0.6F).delay(i / 2));
        }

        for (int i = 0; (double)i < len * 2.0; i++) {
            Vec3 at = from.add(d.scale(VfxClient.random.nextDouble() * len));
            VfxClient.add(
                ModParticles.spark(i % 3 == 0 ? 16777215 : color, 0.5F, 8),
                at.x,
                at.y,
                at.z,
                VfxClient.random.nextGaussian() * 0.15,
                VfxClient.random.nextGaussian() * 0.15,
                VfxClient.random.nextGaussian() * 0.15
            );
        }
    }

    private static void bloodCharge(int entity, Vec3 hand, int duration) {
        Entity e = VfxClient.level.getEntity(entity);
        Supplier<Vec3> at = e != null ? () -> VfxClient.handOf(e) : VfxClient.fixed(hand);
        VfxEffects.add(new VfxTechniques.ChargeOrb(at, 13635632, 16756920, Math.max(6, duration), 0.9F));

        for (int i = 0; i < 16; i++) {
            Vec3 h = at.get();
            Vec3 d = new Vec3(VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian()).normalize().scale(1.2);
            VfxClient.add(ModParticles.glow(13635632, 0.4F, 10), h.x + d.x, h.y + d.y, h.z + d.z, -d.x * 0.12, -d.y * 0.12, -d.z * 0.12);
        }
    }

    private static void bloodBurst(Vec3 pos, float s) {
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 0.2F, s, -1070596088, 0, 10, false, 1.2F));
        VfxEffects.add(
            new VfxEffects.Sphere(VfxClient.fixed(pos), 0.3F, s * 1.1F, VfxDraw.argb(0.15F, 13635632), VfxDraw.argb(0.95F, 16728144), 10, true, 2.4F)
        );
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.5F, s * 1.6F, 13635632, 10));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.3F, s * 1.5F, 0.5F, 13635632, 10, FRRenderTypes.SHOCK));
        VfxClient.blood(pos, 40, s);
        VfxClient.burst(pos, Vec3.ZERO, 24, 0.5, ModParticles.spark(16732256, 0.7F, 10));
    }

    private static void redScale(int entity, Vec3 pos, int duration) {
        Supplier<Vec3> body = VfxClient.follow(entity, pos, 0.5);
        VfxEffects.add(new VfxEffects.Ring(VfxClient.follow(entity, pos, 0.02), new Vec3(0.0, 1.0, 0.0), 0.3F, 2.4F, 0.5F, 13635632, 16, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxTechniques.AuraFx(entity, pos, 13635632, 4849672, duration, 0.5F));
        VfxEffects.add(new VfxEffects.Flash(body, 0.6F, 2.2F, 13635632, 14));
    }

    private static void bloodCloud(Vec3 pos, float r, int duration) {
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos), new Vec3(0.0, -1.0, 0.0), r * 0.6F, r, 0.01F, -1069547504, Math.max(20, duration), FRRenderTypes.GLOW, false
            )
        );
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, -0.2, 0.0)),
                new Vec3(0.0, -1.0, 0.0),
                r * 0.5F,
                r * 0.9F,
                -0.015F,
                -7729120,
                Math.max(20, duration),
                FRRenderTypes.CRACKS,
                true
            )
        );

        for (int i = 0; i < 60; i++) {
            VfxClient.add(
                ModParticles.smoke(i % 2 == 0 ? 3801096 : 6949396, 3.0F, 80),
                pos.x + VfxClient.random.nextGaussian() * (double)r * 0.5,
                pos.y + VfxClient.random.nextGaussian() * 0.6,
                pos.z + VfxClient.random.nextGaussian() * (double)r * 0.5,
                0.0,
                0.0,
                0.0
            );
        }
    }

    private static void bloodSpike(Vec3 ground) {
        Vec3 top = ground.add(VfxClient.random.nextGaussian() * 0.3, 8.0, VfxClient.random.nextGaussian() * 0.3);
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(ground), 0.18F, 13635632, 6));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(top), VfxClient.fixed(ground), 0.06F, 16761032, 4));
        VfxEffects.add(
            new VfxEffects.Ring(VfxClient.fixed(ground.add(0.0, 0.05, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.1F, 1.2F, 0.2F, 13635632, 6, FRRenderTypes.RING)
        );
        VfxClient.blood(ground.add(0.0, 0.2, 0.0), 6, 0.6F);
    }

    private static void ratioMark(int entity, Vec3 pos, Vec3 dir, float h, int color) {
        Supplier<Vec3> c = VfxClient.follow(entity, pos, 0.5);
        Vec3 fwd = VfxClient.dirOr(dir);
        Vec3 side = fwd.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 axis = new Vec3(0.0, 1.0, 0.0).scale(0.85).add(side.scale(0.5)).normalize();
        float half = Math.max(0.8F, h * 0.65F);
        VfxEffects.add(new VfxTechniques.RatioLine(c, axis, half, color, 18));
    }

    private static void ratioCrit(Vec3 pos, Vec3 dir, float s, int big) {
        Vec3 d = VfxClient.dirOr(dir);
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.3F, 2.4F * s, 16777215, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.5F, 3.4F * s, 15253600, 10));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), d, 0.2F, 2.6F * s, 0.5F * s, 16777215, 8, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), d, 0.3F, 1.8F * s, 0.25F, 15253600, 10, FRRenderTypes.RING).delay(1));
        VfxClient.burst(pos, d, (int)(20.0F * s), 0.55, ModParticles.spark(15253600, 0.7F, 10));
        if (big == 1) {
            for (int i = 0; i < 4; i++) {
                VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), d, 0.5F, 5.0F * s, 0.8F, 15253600, 14, FRRenderTypes.SHOCK).delay(i * 2));
            }
        }
    }

    private static void collapse(Vec3 pos, Vec3 dir, float s) {
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, s * 1.3F, 0.0F, -804253680, 70, FRRenderTypes.CRACKS, false
            )
        );
        VfxEffects.add(
            new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.3F, s * 1.6F, 0.6F, 15253600, 10, FRRenderTypes.SHOCK)
                .alpha(0.6F)
        );

        for (int i = 0; i < 18; i++) {
            VfxClient.debris(pos.add(VfxClient.random.nextGaussian() * (double)s * 0.5, 0.0, VfxClient.random.nextGaussian() * (double)s * 0.5), 2);
        }

        VfxClient.burst(pos.add(0.0, 0.3, 0.0), new Vec3(0.0, 0.6, 0.0), 10, 0.1, ModParticles.smoke(6971992, 1.8F, 30));
    }

    private static void overtime(int entity, Vec3 pos, int duration) {
        VfxEffects.add(new VfxEffects.Ring(VfxClient.follow(entity, pos, 0.02), new Vec3(0.0, 1.0, 0.0), 0.4F, 3.0F, 0.5F, 15253600, 20, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(entity, pos, 0.5), 0.5F, 2.6F, 15253600, 16));
        VfxEffects.add(new VfxTechniques.AuraFx(entity, pos, 15253600, 6967328, duration, 0.35F));
    }

    private static void nailHit(Vec3 pos, Vec3 dir) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.1F, 0.6F, 16756848, 5));
        VfxClient.burst(pos, VfxClient.dirOr(dir).reverse(), 8, 0.3, ModParticles.spark(16763024, 0.4F, 6));
    }

    private static void doll(int entity, Vec3 pos, float s, int color) {
        Entity e = VfxClient.level.getEntity(entity);
        Supplier<Vec3> at = e != null ? () -> VfxClient.handOf(e) : VfxClient.fixed(pos);
        VfxEffects.add(new VfxTechniques.DollFx(at, color, 16, s));
        VfxEffects.add(
            new VfxEffects.Ring(at, e != null ? e.getViewVector(1.0F) : new Vec3(0.0, 0.0, 1.0), 0.2F, 2.2F * s, 0.4F, color, 10, FRRenderTypes.SHOCK).delay(5)
        );
        VfxEffects.add(new VfxEffects.Flash(at, 0.2F, 1.6F * s, color, 8).delay(5));
    }

    private static void resonanceHit(int entity, Vec3 pos, Vec3 toCaster, float s, int color, int black) {
        Supplier<Vec3> c = VfxClient.follow(entity, pos, 0.5);
        VfxEffects.add(new VfxEffects.Beam(c, VfxClient.fixed(pos.add(toCaster)), 0.05F, color, 8));
        VfxEffects.add(new VfxEffects.Flash(c, 0.3F, 2.0F * s, color, 10));
        VfxEffects.add(new VfxEffects.Flash(c, 0.2F, 1.0F * s, 16777215, 6));
        VfxEffects.add(new VfxEffects.Ring(c, VfxClient.dirOr(toCaster), 0.2F, 1.8F * s, 0.4F, color, 8, FRRenderTypes.SHOCK));
        VfxClient.blood(pos, (int)(16.0F * s), 0.6F);
        if (black == 1) {
            for (int i = 0; i < 6; i++) {
                Vec3 d = new Vec3(VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian())
                    .normalize()
                    .scale(1.5 * (double)s);
                VfxEffects.add(new VfxEffects.Bolt(pos, pos.add(d), 0.12F, 0.3F, 6, 16715808, 9, true));
                VfxEffects.add(new VfxEffects.Bolt(pos, pos.add(d), 0.05F, 0.3F, 6, 0, 9, false));
            }
        }
    }

    private static void firePillar(Vec3 pos, float s, int phase) {
        if (phase == 0) {
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, 1.8F * s, 0.02F, -38384, 20, FRRenderTypes.CRACKS, true
                )
            );

            for (int i = 0; i < 10; i++) {
                VfxClient.add(
                    ModParticles.ember(16738832, 0.4F, 20),
                    pos.x + VfxClient.random.nextGaussian() * (double)s,
                    pos.y + 0.1,
                    pos.z + VfxClient.random.nextGaussian() * (double)s,
                    0.0,
                    0.08 + VfxClient.random.nextDouble() * 0.1,
                    0.0
                );
            }
        } else {
            float h = 5.0F * s;
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(pos), VfxClient.fixed(pos.add(0.0, (double)h, 0.0)), s * 1.1F, 16738832, 18));
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(pos), VfxClient.fixed(pos.add(0.0, (double)h * 0.8, 0.0)), s * 0.45F, 16765056, 14));
            VfxEffects.add(
                new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, s * 2.2F, 0.7F, 16738832, 14, FRRenderTypes.SHOCK)
            );
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 1.0, 0.0)), 1.0F, s * 3.0F, 16738832, 12));
            VfxEffects.add(
                new VfxEffects.Decal(
                    VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), s, s * 1.8F, 0.0F, -535820284, 80, FRRenderTypes.CRACKS, false
                )
            );

            for (int i = 0; (float)i < 40.0F * s; i++) {
                VfxClient.add(
                    ParticleTypes.FLAME,
                    pos.x + VfxClient.random.nextGaussian() * (double)s * 0.4,
                    pos.y + VfxClient.random.nextDouble() * (double)h * 0.5,
                    pos.z + VfxClient.random.nextGaussian() * (double)s * 0.4,
                    VfxClient.random.nextGaussian() * 0.05,
                    0.25 + VfxClient.random.nextDouble() * 0.3,
                    VfxClient.random.nextGaussian() * 0.05
                );
            }

            VfxClient.burst(pos.add(0.0, 0.5, 0.0), new Vec3(0.0, 1.0, 0.0), (int)(30.0F * s), 0.6, ModParticles.ember(16738832, 0.5F, 30));
            VfxClient.burst(pos.add(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0), (int)(10.0F * s), 0.15, ModParticles.smoke(1707014, 2.5F, 50));

            for (int i = 0; i < 8; i++) {
                VfxClient.debris(pos.add(VfxClient.random.nextGaussian(), 0.0, VfxClient.random.nextGaussian()), 2);
            }
        }
    }

    private static void flameCone(Vec3 mouth, Vec3 dir, float len) {
        Vec3 d = VfxClient.dirOr(dir);

        for (int i = 0; i < 26; i++) {
            Vec3 v = d.add(VfxClient.random.nextGaussian() * 0.22, VfxClient.random.nextGaussian() * 0.22, VfxClient.random.nextGaussian() * 0.22)
                .normalize()
                .scale((double)len / 14.0 * (0.6 + VfxClient.random.nextDouble() * 0.6));
            VfxClient.add(i % 3 == 0 ? ModParticles.glow(16765056, 1.2F, 12) : ModParticles.ember(16738832, 0.9F, 14), mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
            if (i % 2 == 0) {
                VfxClient.add(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, v.x * 0.8, v.y * 0.8, v.z * 0.8);
            }
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(mouth), 0.2F, 1.2F, 16738832, 5));
    }

    private static void emberBoom(Vec3 pos, float s, int color) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.3F, 2.2F * s, color, 10));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.2F, 1.0F * s, 16765056, 6));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 0.2F, s, VfxDraw.argb(0.3F, color), VfxDraw.argb(0.9F, 16765056), 9, true, 2.0F));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.2F, s * 1.5F, 0.4F, color, 9, FRRenderTypes.SHOCK));
        VfxClient.burst(pos, Vec3.ZERO, (int)(20.0F * s), 0.35, ModParticles.ember(color, 0.4F, 20));
        VfxClient.burst(pos, Vec3.ZERO, (int)(6.0F * s), 0.08, ModParticles.smoke(1707014, 1.4F, 26));
    }

    private static void meteorImpact(Vec3 pos, float r) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 2.0F, r * 1.8F, 16738832, 18));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 1.0F, r, 16777215, 10));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 1.0F, r, VfxDraw.argb(0.6F, 16730632), VfxDraw.argb(1.0F, 16765056), 22, true, 1.6F));
        VfxEffects.add(new VfxEffects.Sphere(VfxClient.fixed(pos), 1.0F, r * 0.9F, -1071640570, 0, 30, false, 1.2F).fades(0.2F, 0.6F));

        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        VfxClient.fixed(pos.add(0.0, 0.3, 0.0)),
                        new Vec3(0.0, 1.0, 0.0),
                        1.0F,
                        r * (1.6F + (float)i * 0.4F),
                        1.4F,
                        i == 0 ? 16777215 : 16738832,
                        18 + i * 4,
                        FRRenderTypes.SHOCK
                    )
                    .delay(i * 2)
            );
        }

        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), r, r * 1.4F, 0.0F, -267384828, 140, FRRenderTypes.CRACKS, false
            )
        );
        VfxEffects.add(
            new VfxEffects.Decal(VfxClient.fixed(pos.add(0.0, 0.05, 0.0)), new Vec3(0.0, 1.0, 0.0), r, r * 1.3F, 0.0F, -42480, 60, FRRenderTypes.CRACKS, true)
        );
        VfxClient.burst(pos.add(0.0, 1.0, 0.0), new Vec3(0.0, 1.0, 0.0), 160, 1.2, ModParticles.ember(16738832, 0.7F, 50));
        VfxClient.burst(pos.add(0.0, 1.0, 0.0), new Vec3(0.0, 0.5, 0.0), 50, 0.3, ModParticles.smoke(1312772, 4.0F, 70));

        for (int i = 0; i < 40; i++) {
            double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
            double d = VfxClient.random.nextDouble() * (double)r;
            VfxClient.debris(pos.add(Math.cos(a) * d, 0.0, Math.sin(a) * d), 3);
        }
    }

    private static void soulTouch(int entity, Vec3 pos, float h, int transfigure) {
        Supplier<Vec3> c = VfxClient.follow(entity, pos, 0.5);
        VfxEffects.add(new VfxEffects.Flash(c, 0.3F, 1.8F * Math.max(1.0F, h), 8044728, 12));

        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        c,
                        new Vec3(0.0, 1.0, 0.0),
                        0.2F,
                        (1.2F + (float)i * 0.5F) * Math.max(1.0F, h * 0.6F),
                        0.2F,
                        i == 0 ? 16777215 : 8044728,
                        12,
                        FRRenderTypes.RING
                    )
                    .alpha(0.8F)
                    .delay(i * 2)
            );
        }

        if (transfigure == 1) {
            VfxEffects.add(new VfxTechniques.SpiralFx(VfxClient.follow(entity, pos, 0.0), Math.max(1.0F, h), 8044728, 18));
            VfxClient.burst(pos, Vec3.ZERO, 30, 0.3, ModParticles.smoke(1714728, 1.6F, 40));
            VfxClient.blood(pos, 20, 0.8F);
        } else {
            VfxClient.burst(pos, Vec3.ZERO, 12, 0.25, ModParticles.glow(8044728, 0.5F, 14));
        }
    }

    private static void repelSweep(Vec3 pos, Vec3 dir, float r) {
        Vec3 fwd = new Vec3(dir.x, 0.0, dir.z);
        fwd = fwd.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : fwd.normalize();
        Vec3 side = fwd.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 center = pos.add(fwd.scale((double)(-r) * 0.2));
        VfxEffects.add(new VfxEffects.Slash(center, side, fwd, r, r * 0.35F, -1.3F, 1.3F, 5270120, 9));
        VfxEffects.add(new VfxEffects.Slash(center, side, fwd, r * 1.02F, r * 0.12F, -1.3F, 1.3F, 16777215, 7));
        VfxEffects.add(new VfxEffects.Slash(center.add(up.scale(0.2)), side, fwd, r * 0.9F, r * 0.2F, -1.2F, 1.2F, 8044728, 10));
        VfxClient.burst(pos.add(fwd.scale((double)r * 0.6)), fwd, 18, 0.5, ModParticles.smoke(3819590, 1.2F, 18));
    }

    private static void spikes(Vec3 pos, float r) {
        for (int i = 0; i < 22; i++) {
            Vec3 d = new Vec3(VfxClient.random.nextGaussian(), VfxClient.random.nextGaussian() * 0.6, VfxClient.random.nextGaussian()).normalize();
            Vec3 tip = pos.add(d.scale((double)r * (0.7 + VfxClient.random.nextDouble() * 0.4)));
            VfxEffects.add(new VfxTechniques.SpikeFx(pos, tip, 0.22F, 13160648, 16));
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.4F, r * 1.2F, 8044728, 10));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.4F, r * 1.4F, 0.4F, 16777215, 10, FRRenderTypes.SHOCK));
        VfxClient.blood(pos, 10, 1.2F);
    }

    private static void buds(int entity, Vec3 pos, float h, int duration) {
        VfxEffects.add(new VfxTechniques.BudsFx(entity, pos, Math.max(1.0F, h), Math.max(20, duration)));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.follow(entity, pos, 0.5), 0.3F, 1.4F, 5943360, 10));
    }

    private static void flowerField(Vec3 pos, float r, int duration) {
        int life = Math.max(40, duration);
        VfxEffects.add(new VfxEffects.Decal(VfxClient.fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.5F, r, 0.002F, -1873756096, life, FRRenderTypes.SEAL, true));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, r, 0.4F, 5943360, 20, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxTechniques.FieldFx(pos, r, life));
    }

    private static void summon(Vec3 pos, float s, int color) {
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.04, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.3F, 1.4F * s, 0.25F, -268105208, 24, FRRenderTypes.SEAL, false
            )
        );
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(pos.add(0.0, 0.06, 0.0)),
                new Vec3(0.0, 1.0, 0.0),
                0.3F,
                1.3F * s,
                -0.3F,
                VfxDraw.argb(1.0F, color),
                24,
                FRRenderTypes.RING,
                true
            )
        );
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos.add(0.0, 0.8 * (double)s, 0.0)), 0.5F, 2.0F * s, color, 12));
        VfxClient.burst(pos.add(0.0, 0.5, 0.0), new Vec3(0.0, 1.0, 0.0), 24, 0.2, ModParticles.smoke(529423, 1.6F, 30));
    }

    private static void absorb(int caster, Vec3 pos, float s, int strong) {
        Entity e = VfxClient.level.getEntity(caster);
        Supplier<Vec3> hand = e != null ? () -> VfxClient.handOf(e) : VfxClient.fixed(pos);
        VfxEffects.add(new VfxTechniques.AbsorbFx(pos, hand, s, 16));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.3F, 2.0F * s, 3844744, 8));
        VfxClient.burst(pos, Vec3.ZERO, 16, 0.2, ModParticles.smoke(330248, 1.2F, 16));
    }

    private static void uzumakiCharge(int entity, Vec3 hand, float s, int duration) {
        Entity e = VfxClient.level.getEntity(entity);
        Supplier<Vec3> at = e != null ? () -> VfxClient.handOf(e).add(e.getViewVector(1.0F).scale(0.6)) : VfxClient.fixed(hand);
        VfxEffects.add(new VfxTechniques.VortexFx(at, s, Math.max(10, duration)));
    }

    private static void uzumakiBeam(Vec3 from, Vec3 vec, float w) {
        beam(from, vec, 0.45F * w, 3844744, 14221296, 22);
        Vec3 d = VfxClient.dirOr(vec);
        double len = vec.length();
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), VfxClient.fixed(from.add(vec)), w * 1.6F, 398352, 16));

        for (int i = 0; (double)i < len; i++) {
            Vec3 at = from.add(d.scale((double)i));
            VfxClient.add(
                ModParticles.smoke(397324, 1.6F, 30),
                at.x,
                at.y,
                at.z,
                VfxClient.random.nextGaussian() * 0.08,
                VfxClient.random.nextGaussian() * 0.08,
                VfxClient.random.nextGaussian() * 0.08
            );
        }
    }

    private static void dragonJet(Vec3 pos, Vec3 dir, float s) {
        Vec3 d = VfxClient.dirOr(dir);
        VfxEffects.add(
            new VfxEffects.Beam(VfxClient.fixed(pos.subtract(d.scale(0.5))), VfxClient.fixed(pos.add(d.scale((double)(4.0F * s)))), 0.7F * s, 9099519, 10)
        );
        VfxEffects.add(
            new VfxEffects.Beam(VfxClient.fixed(pos.subtract(d.scale(0.5))), VfxClient.fixed(pos.add(d.scale((double)(3.0F * s)))), 0.25F * s, 16777215, 7)
        );
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.4F, 2.6F * s, 9099519, 8));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos), d, 0.3F, 2.4F * s, 0.5F, 16777215, 8, FRRenderTypes.SHOCK));
        VfxClient.burst(pos, d, 30, 0.8, ModParticles.spark(12577023, 0.7F, 10));
    }

    private static void lightning(Vec3 ground) {
        Vec3 top = ground.add(VfxClient.random.nextGaussian() * 2.0, 24.0, VfxClient.random.nextGaussian() * 2.0);
        VfxEffects.add(new VfxEffects.Bolt(top, ground, 0.55F, 2.2F, 14, 16773242, 10, true));
        VfxEffects.add(new VfxEffects.Bolt(top, ground, 0.2F, 2.2F, 14, 16777215, 8, true));

        for (int i = 0; i < 3; i++) {
            Vec3 mid = top.lerp(ground, 0.3 + (double)i * 0.2);
            VfxEffects.add(
                new VfxEffects.Bolt(
                    mid, mid.add(VfxClient.random.nextGaussian() * 3.0, -3.0, VfxClient.random.nextGaussian() * 3.0), 0.15F, 0.8F, 6, 16773242, 8, true
                )
            );
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(ground.add(0.0, 0.5, 0.0)), 1.0F, 4.0F, 16774336, 8));
        VfxEffects.add(
            new VfxEffects.Ring(VfxClient.fixed(ground.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.3F, 3.0F, 0.5F, 16777215, 10, FRRenderTypes.SHOCK)
        );
        VfxEffects.add(
            new VfxEffects.Decal(
                VfxClient.fixed(ground.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, 1.8F, 0.0F, -804253680, 60, FRRenderTypes.CRACKS, false
            )
        );
        VfxClient.burst(ground.add(0.0, 0.3, 0.0), new Vec3(0.0, 1.0, 0.0), 30, 0.6, ModParticles.spark(16774832, 0.6F, 10));
    }

    private static void eyeBeam(int target, Vec3 from, Vec3 vec, int phase) {
        if (phase == 0) {
            Supplier<Vec3> to = VfxClient.follow(target, from.add(vec), 0.5);
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(from), to, 0.03F, 16719920, 20));
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(from), 0.1F, 0.6F, 16719920, 20).fades(0.6F, 0.1F));
        } else {
            beam(from, vec, 0.18F, 16719920, 16769248, 10);
        }
    }

    private static void scream(Vec3 mouth, Vec3 dir) {
        Vec3 d = VfxClient.dirOr(dir);

        for (int i = 0; i < 6; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(
                        VfxClient.fixed(mouth.add(d.scale(0.8 + (double)i * 1.3))), d, 0.3F, 1.0F + (float)i * 0.8F, 0.4F, 10109183, 12, FRRenderTypes.SHOCK
                    )
                    .alpha(0.7F)
                    .delay(i)
            );
        }

        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(mouth), 0.3F, 1.6F, 10109183, 10));
    }

    private VfxTechniques() {
    }

    private static final class AbsorbFx extends VfxEffects.Fx {
        private final Vec3 from;
        private final Supplier<Vec3> hand;
        private final float scale;

        AbsorbFx(Vec3 from, Supplier<Vec3> hand, float scale, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.GLOW));
            this.from = from;
            this.hand = hand;
            this.scale = scale;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float k = VfxEffects.easeOut(t);
            Vec3 p = this.from.lerp(this.hand.get(), (double)k);
            float s = (0.9F - 0.6F * k) * this.scale;
            VfxDraw.billboard(m, vc, cam, p, s, 0.0F, -268171258);
            VfxDraw.billboard(m, vc, cam, p, s * 1.3F, 0.0F, VfxDraw.argb(0.4F, 3844744));
        }
    }

    private static final class AuraFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final int color;
        private final int dark;
        private final float density;

        AuraFx(int entity, Vec3 fallback, int color, int dark, int life, float density) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.entity = entity;
            this.fallback = fallback;
            this.color = color;
            this.dark = dark;
            this.density = density;
        }

        @Override
        protected void tick() {
            if (!(VfxClient.random.nextFloat() > this.density)) {
                Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                VfxClient.add(
                    ModParticles.glow(this.color, 0.45F, 16),
                    f.x + Math.cos(a) * 0.45,
                    f.y + VfxClient.random.nextDouble() * 1.8,
                    f.z + Math.sin(a) * 0.45,
                    0.0,
                    0.05,
                    0.0
                );
                if (VfxClient.random.nextBoolean()) {
                    VfxClient.add(
                        ModParticles.smoke(this.dark, 0.6F, 18),
                        f.x + Math.cos(a) * 0.4,
                        f.y + VfxClient.random.nextDouble() * 1.4,
                        f.z + Math.sin(a) * 0.4,
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

    private static final class BudsFx extends VfxEffects.Fx {
        private final int entity;
        private final Vec3 fallback;
        private final float height;
        private final Vec3[] offsets = new Vec3[6];

        BudsFx(int entity, Vec3 fallback, float height, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.entity = entity;
            this.fallback = fallback;
            this.height = height;

            for (int i = 0; i < this.offsets.length; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                this.offsets[i] = new Vec3(Math.cos(a) * 0.35, (0.2 + VfxClient.random.nextDouble() * 0.7) * (double)height, Math.sin(a) * 0.35);
            }
        }

        @Override
        protected void tick() {
            if (this.age % 6 == 0) {
                Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
                Vec3 o = this.offsets[VfxClient.random.nextInt(this.offsets.length)];
                VfxClient.add(ModParticles.glow(5943360, 0.3F, 14), f.x + o.x, f.y + o.y, f.z + o.z, 0.0, 0.03, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 f = VfxClient.follow(this.entity, this.fallback, 0.0).get();
            float grow = Mth.clamp(age / 10.0F, 0.0F, 1.0F) * VfxEffects.fade(t, 0.0F, 0.15F);

            for (int i = 0; i < this.offsets.length; i++) {
                float pulse = 0.8F + 0.2F * Mth.sin(age * 0.3F + (float)i);
                VfxDraw.billboard(m, vc, cam, f.add(this.offsets[i]), 0.22F * grow * pulse, 0.0F, VfxDraw.argb(0.9F * grow, i % 2 == 0 ? 5943360 : 14741664));
            }
        }
    }

    private static final class ChargeOrb extends VfxEffects.Fx {
        private final Supplier<Vec3> at;
        private final int color;
        private final int light;
        private final float scale;

        ChargeOrb(Supplier<Vec3> at, int color, int light, int life, float scale) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.at = at;
            this.color = color;
            this.light = light;
            this.scale = scale;
        }

        @Override
        protected void tick() {
            VfxClient.converge(this.at.get(), 2, 0.9 * (double)this.scale, this.color);
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float pulse = 1.0F + 0.15F * Mth.sin(age * 2.1F);
            VfxDraw.billboard(m, vc, cam, this.at.get(), (0.2F + 0.6F * t) * this.scale * pulse, age * 0.3F, VfxDraw.argb(0.9F, this.color));
            VfxDraw.billboard(m, vc, cam, this.at.get(), (0.08F + 0.25F * t) * this.scale, 0.0F, VfxDraw.argb(1.0F, this.light));
        }
    }

    private static final class DollFx extends VfxEffects.Fx {
        private final Supplier<Vec3> at;
        private final int color;
        private final float scale;

        DollFx(Supplier<Vec3> at, int color, int life, float scale) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.at = at;
            this.color = color;
            this.scale = scale;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = this.at.get();
            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            Vec3 side = up.cross(eye.subtract(c)).normalize();
            float s = 0.35F * this.scale;
            int col = VfxDraw.argb(VfxEffects.fade(t, 0.1F, 0.4F), 15257744);
            VfxDraw.beam(m, vc, c.add(up.scale((double)(-s))), c.add(up.scale((double)s * 0.7)), 0.06F, 0.06F, col, col, eye);
            VfxDraw.beam(
                m,
                vc,
                c.add(side.scale((double)(-s) * 0.7)).add(up.scale((double)s * 0.3)),
                c.add(side.scale((double)s * 0.7)).add(up.scale((double)s * 0.3)),
                0.05F,
                0.05F,
                col,
                col,
                eye
            );
            VfxDraw.beam(
                m, vc, c.add(up.scale((double)(-s))), c.add(up.scale((double)(-s) * 1.6)).add(side.scale((double)(-s) * 0.4)), 0.05F, 0.05F, col, col, eye
            );
            VfxDraw.beam(
                m, vc, c.add(up.scale((double)(-s))), c.add(up.scale((double)(-s) * 1.6)).add(side.scale((double)s * 0.4)), 0.05F, 0.05F, col, col, eye
            );
            VfxDraw.beam(m, vc, c.add(up.scale((double)s * 0.7)), c.add(up.scale((double)s * 1.05)), 0.12F, 0.12F, col, col, eye);
            int nail = VfxDraw.argb(VfxEffects.fade(t, 0.2F, 0.4F), this.color);
            VfxDraw.beam(
                m, vc, c.add(side.scale((double)(-s) * 0.9)).add(up.scale((double)s * 0.1)), c.add(up.scale((double)s * 0.1)), 0.04F, 0.02F, nail, nail, eye
            );
        }
    }

    private static final class FieldFx extends VfxEffects.Fx {
        private final Vec3 center;
        private final float radius;
        private final Vec3[] flowers = new Vec3[28];

        FieldFx(Vec3 center, float radius, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.center = center;
            this.radius = radius;

            for (int i = 0; i < this.flowers.length; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double r = Math.sqrt(VfxClient.random.nextDouble()) * (double)radius;
                this.flowers[i] = center.add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
            }
        }

        @Override
        protected void tick() {
            if (this.age % 3 == 0) {
                Vec3 f = this.flowers[VfxClient.random.nextInt(this.flowers.length)];
                VfxClient.add(
                    ModParticles.petal(VfxClient.random.nextBoolean() ? 16040152 : 16773280, 0.5F, 40),
                    f.x,
                    f.y + 0.2,
                    f.z,
                    VfxClient.random.nextGaussian() * 0.02,
                    0.06,
                    VfxClient.random.nextGaussian() * 0.02
                );
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float a = VfxEffects.fade(t, 0.1F, 0.15F);

            for (int i = 0; i < this.flowers.length; i++) {
                float bloom = Mth.clamp((age - (float)i * 0.6F) / 12.0F, 0.0F, 1.0F);
                float s = 0.35F * bloom * (0.85F + 0.15F * Mth.sin(age * 0.2F + (float)i));
                VfxDraw.billboard(m, vc, cam, this.flowers[i], s, (float)i, VfxDraw.argb(0.85F * a, i % 3 == 0 ? 16773280 : (i % 3 == 1 ? 16033992 : 5943360)));
            }
        }
    }

    private static final class RatioLine extends VfxEffects.Fx {
        private final Supplier<Vec3> center;
        private final Vec3 axis;
        private final float half;
        private final int color;

        RatioLine(Supplier<Vec3> center, Vec3 axis, float half, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.center = center;
            this.axis = axis;
            this.half = half;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = this.center.get();
            float reveal = VfxEffects.easeOut(Mth.clamp(t * 3.0F, 0.0F, 1.0F));
            float a = VfxEffects.fade(t, 0.0F, 0.5F);
            Vec3 from = c.subtract(this.axis.scale((double)this.half));
            Vec3 to = from.add(this.axis.scale((double)(this.half * 2.0F * reveal)));
            VfxDraw.beam(m, vc, from, to, 0.05F, 0.05F, VfxDraw.argb(a, 16777215), VfxDraw.argb(a, 16777215), eye);
            if (reveal > 0.69F) {
                Vec3 p = from.add(this.axis.scale((double)(this.half * 2.0F) * 0.7));
                Vec3 side = this.axis.cross(eye.subtract(p)).normalize().scale(0.35);
                VfxDraw.beam(m, vc, p.subtract(side), p.add(side), 0.09F, 0.09F, VfxDraw.argb(a, this.color), VfxDraw.argb(a, this.color), eye);
                Vec3 side2 = this.axis.scale(0.35);
                VfxDraw.beam(m, vc, p.subtract(side2), p.add(side2), 0.09F, 0.09F, VfxDraw.argb(a, this.color), VfxDraw.argb(a, this.color), eye);
            }
        }
    }

    private static final class SpikeFx extends VfxEffects.Fx {
        private final Vec3 from;
        private final Vec3 tip;
        private final float width;
        private final int color;

        SpikeFx(Vec3 from, Vec3 tip, float width, int color, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.from = from;
            this.tip = tip;
            this.width = width;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float grow = Mth.clamp(age / 3.0F, 0.0F, 1.0F);
            Vec3 end = this.from.lerp(this.tip, (double)grow);
            int c = VfxDraw.argb(VfxEffects.fade(t, 0.0F, 0.4F), this.color);
            VfxDraw.beam(m, vc, this.from, end, this.width, 0.01F, c, c, eye);
        }
    }

    private static final class SpiralFx extends VfxEffects.Fx {
        private final Supplier<Vec3> base;
        private final float height;
        private final int color;

        SpiralFx(Supplier<Vec3> base, float height, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.base = base;
            this.height = height;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 b = this.base.get();
            int c = VfxDraw.argb(VfxEffects.fade(t, 0.05F, 0.5F), this.color);

            for (int k = 0; k < 2; k++) {
                Vec3 prev = null;

                for (int i = 0; i <= 24; i++) {
                    float f = (float)i / 24.0F;
                    double a = (double)k * Math.PI + (double)(f * 14.0F) + (double)age * 0.8;
                    double r = 0.8 * (1.0 - (double)t * 0.8) * (1.0 - (double)f * 0.4);
                    Vec3 p = b.add(Math.cos(a) * r, (double)(f * this.height) * 1.1, Math.sin(a) * r);
                    if (prev != null) {
                        VfxDraw.beam(m, vc, prev, p, 0.08F, 0.08F, c, c, eye);
                    }

                    prev = p;
                }
            }
        }
    }

    private static final class VortexFx extends VfxEffects.Fx {
        private final Supplier<Vec3> at;
        private final float scale;

        VortexFx(Supplier<Vec3> at, float scale, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.GLOW));
            this.at = at;
            this.scale = scale;
        }

        @Override
        protected void tick() {
            Vec3 c = this.at.get();

            for (int i = 0; i < 3; i++) {
                double a = VfxClient.random.nextDouble() * (float) (Math.PI * 2);
                double r = 2.5 * (double)this.scale;
                VfxClient.add(
                    ModParticles.smoke(264198, 0.9F, 14),
                    c.x + Math.cos(a) * r,
                    c.y + VfxClient.random.nextGaussian() * 0.6,
                    c.z + Math.sin(a) * r,
                    -Math.cos(a) * 0.18 - Math.sin(a) * 0.15,
                    0.0,
                    -Math.sin(a) * 0.18 + Math.cos(a) * 0.15
                );
            }

            if (this.age == this.life) {
                VfxEffects.add(new VfxEffects.Flash(this.at, 0.5F, 3.0F * this.scale, 3844744, 10));
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = this.at.get();
            int n = 18;

            for (int i = 0; i < n; i++) {
                double a = (double)((float)i * (float) (Math.PI * 2) / (float)n) + (double)age * 0.35;
                double r = (1.8 - 1.4 * (double)t) * (double)this.scale * (0.6 + 0.4 * ((double)(i % 3) / 2.0));
                Vec3 p = c.add(Math.cos(a) * r, Math.sin(a * 2.0 + (double)i) * 0.3 * (double)this.scale, Math.sin(a) * r);
                VfxDraw.billboard(m, vc, cam, p, 0.22F * this.scale, 0.0F, -536606714);
            }

            VfxDraw.billboard(m, vc, cam, c, (0.3F + 0.7F * t) * this.scale, 0.0F, -268303357);
        }
    }
}
