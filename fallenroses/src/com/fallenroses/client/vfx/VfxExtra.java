package com.fallenroses.client.vfx;

import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModParticles;
import com.fallenroses.technique.Technique;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** 0.9 effects: domain hand sign and barrier collapse, the cast flourish of every ability, weapons. */
final class VfxExtra {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    static void play(VfxPacket p) {
        Vec3 pos = p.pos();
        Vec3 dir = p.dir();
        float s = p.scale();
        switch (p.kind()) {
            case DOMAIN_SIGN:
                domainSign(p.entity(), pos, s, p.color());
                break;
            case DOMAIN_BREAK:
                domainBreak(pos, s, p.color());
                break;
            case MUZZLE_FLASH:
                muzzle(pos, dir, p.extra());
                break;
            case ROPE_WHIP:
                VfxEffects.add(new VfxExtra.RopeFx(p.entity(), pos, dir, 9));
                break;
            case ROPE_SPIN:
                VfxEffects.add(new VfxExtra.SpinFx(p.entity(), pos, Math.max(4, p.extra())));
                break;
            case UNRAVEL:
                unravel(p.entity(), pos, s);
                break;
            case BROOM_TRAIL:
                broom(pos, dir, s);
                break;
            case IMPACT_HEAVY:
                impactHeavy(pos, dir, s, p.color());
        }
    }

    // ------------------------------------------------------------------ domain

    private static void domainSign(int entity, Vec3 pos, float radius, int color) {
        Supplier<Vec3> feet = VfxClient.follow(entity, pos, 0.0);
        Entity e = VfxClient.level.getEntity(entity);
        Supplier<Vec3> hands = e != null ? () -> VfxClient.handOf(e).add(e.getViewVector(1.0F).scale(-0.4)) : VfxClient.follow(entity, pos, 0.75);
        // the clap of the hand sign
        VfxEffects.add(new VfxEffects.Flash(hands, 0.1F, 1.6F, 16777215, 6));
        VfxEffects.add(new VfxEffects.Ring(hands, e != null ? e.getViewVector(1.0F) : UP, 0.1F, 2.2F, 0.3F, color, 9, FRRenderTypes.RING));
        // ink seeping out from under the caster, cursed energy running through it
        Supplier<Vec3> floor = VfxClient.fixed(feet.get().add(0.0, 0.03, 0.0));
        VfxEffects.add(new VfxEffects.Decal(floor, UP, 0.4F, Math.min(5.0F, radius * 0.3F), 0.0F, VfxDraw.argb(0.95F, 263176), 42, FRRenderTypes.GLOW, false));
        VfxEffects.add(new VfxEffects.Decal(floor, UP, 0.4F, Math.min(4.6F, radius * 0.28F), 0.03F, VfxDraw.argb(0.8F, color), 40, FRRenderTypes.VEINS, true));
        VfxEffects.add(new VfxExtra.SignFx(feet, hands, color, 20));
    }

    private static void domainBreak(Vec3 c, float r, int color) {
        RandomSource rnd = VfxClient.random;
        VfxExtra.Shards shards = new VfxExtra.Shards(c, r, rnd);
        VfxEffects.add(new VfxExtra.ShardFx(shards, false, color));
        VfxEffects.add(new VfxExtra.ShardFx(shards, true, color));
        Vec3 mid = c.add(0.0, (double)r * 0.4, 0.0);
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(mid), r * 0.3F, r * 1.3F, 16777215, 12));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(mid), r * 0.4F, r * 1.6F, color, 18).alpha(0.7F));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(c.add(0.0, 0.2, 0.0)), UP, r * 0.8F, r * 1.6F, 1.6F, color, 22, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(c.add(0.0, 0.2, 0.0)), UP, r * 0.9F, r * 1.4F, 0.6F, 16777215, 14, FRRenderTypes.RING).delay(2));

        for (int i = 0; i < 12; i++) {
            Vec3 d = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()) * 0.8 + 0.2, rnd.nextGaussian()).normalize();
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(mid), VfxClient.fixed(mid.add(d.scale((double)r * 1.3))), 0.5F, i % 2 == 0 ? 16777215 : color, 10).delay(i / 4));
        }

        for (int i = 0; i < 90; i++) {
            Vec3 n = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()), rnd.nextGaussian()).normalize();
            Vec3 at = c.add(n.scale((double)r));
            VfxClient.add(ModParticles.spark(i % 3 == 0 ? 16777215 : color, 0.6F, 20 + rnd.nextInt(10)), at.x, at.y, at.z, n.x * 0.4, n.y * 0.4, n.z * 0.4);
            if (i % 2 == 0) {
                VfxClient.add(ModParticles.smoke(526344, 2.4F, 40), at.x, at.y, at.z, n.x * 0.08, n.y * 0.05, n.z * 0.08);
            }
        }
    }

    // ------------------------------------------------------------------ cast flourish

    /** Called for MANGA_CAST, which every successful ability sends (scale = slot, extra = technique ordinal). */
    static void onCast(VfxPacket p) {
        cast(p.entity(), p.pos(), p.dir(), (int)p.scale(), p.color(), p.extra());
    }

    /**
     * Every successful ability: the caster's cursed energy flares in the technique's colour, flavoured per
     * technique; the ultimate (slot 4) adds a pillar and a shockwave.
     */
    private static void cast(int entity, Vec3 eyePos, Vec3 look, int slot, int color, int tech) {
        Entity e = VfxClient.level.getEntity(entity);
        if (e != null) {
            RandomSource rnd = VfxClient.random;
            Technique t = Technique.byOrdinal(tech);
            boolean ult = slot >= 4;
            float k = 1.0F + (float)slot * 0.18F;
            Supplier<Vec3> feet = VfxClient.follow(entity, e.position(), 0.0);
            Supplier<Vec3> body = VfxClient.follow(entity, e.position(), 0.55);
            Supplier<Vec3> hand = () -> VfxClient.handOf(e);
            boolean energy = t != Technique.HEAVENLY_BODY;
            if (energy) {
                VfxEffects.add(new VfxEffects.Flash(hand, 0.2F, 1.1F * k, color, 8));
                VfxEffects.add(new VfxEffects.Flash(hand, 0.1F, 0.45F * k, 16777215, 5));
                VfxEffects.add(new VfxEffects.Flash(body, 0.6F, 1.6F * k, color, 12).alpha(0.35F));
                VfxEffects.add(new VfxEffects.Ring(() -> feet.get().add(0.0, 0.05, 0.0), UP, 0.3F, 1.4F + (float)slot * 0.45F, 0.35F, color, 12, FRRenderTypes.SHOCK).alpha(0.8F));
                Vec3 f = feet.get();
                int n = 10 + slot * 5;

                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2.0;
                    double rr = 0.45 + rnd.nextDouble() * 0.35;
                    VfxClient.add(
                        ModParticles.glow(i % 4 == 0 ? 16777215 : color, 0.45F, 14 + rnd.nextInt(10)),
                        f.x + Math.cos(a) * rr,
                        f.y + rnd.nextDouble() * 1.6,
                        f.z + Math.sin(a) * rr,
                        Math.cos(a) * 0.03,
                        0.07 + rnd.nextDouble() * 0.06,
                        Math.sin(a) * 0.03
                    );
                }
            }

            if (ult) {
                VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, 9.0, 0.0), 1.1F, color, 26));
                VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, 9.0, 0.0), 0.35F, 16777215, 20));
                VfxEffects.add(new VfxEffects.Ring(feet, UP, 0.5F, 6.5F, 0.9F, color, 22, FRRenderTypes.SHOCK).delay(2));
                VfxEffects.add(new VfxEffects.Ring(feet, UP, 0.4F, 4.5F, 0.4F, 16777215, 16, FRRenderTypes.RING).delay(4));
                VfxEffects.add(new VfxEffects.Flash(body, 1.0F, 4.0F, color, 16).alpha(0.6F));
                VfxClient.burst(body.get(), UP, 40, 0.45, ModParticles.spark(color, 0.6F, 18));
            }

            if (t != null) {
                flavor(t, e, feet, body, hand, look, slot, color, rnd);
            }
        }
    }

    private static void flavor(Technique t, Entity e, Supplier<Vec3> feet, Supplier<Vec3> body, Supplier<Vec3> hand, Vec3 look, int slot, int color, RandomSource rnd) {
        Vec3 h = hand.get();
        Vec3 b = body.get();
        Vec3 f = feet.get();
        Vec3 d = VfxClient.dirOr(look);
        switch (t) {
            case LIMITLESS:
                VfxEffects.add(new VfxEffects.Ring(hand, d, 0.05F, 1.4F, 0.12F, 12574975, 10, FRRenderTypes.RING).alpha(0.7F));
                VfxEffects.add(new VfxEffects.Ring(hand, d, 0.05F, 0.9F, 0.08F, 16777215, 8, FRRenderTypes.RING).delay(2));
                VfxClient.converge(h, 12, 1.2, slot == 1 ? 16722462 : 4885759);
                break;
            case SHRINE:
            case VESSEL:
                for (int i = 0; i < 2 + slot; i++) {
                    Vec3 axis = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
                    Vec3[] ab = VfxDraw.basis(axis);
                    Vec3 c = b.add(rnd.nextGaussian() * 0.8, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.8);
                    VfxEffects.add(new VfxEffects.Slash(c, ab[0], ab[1], 0.8F, 0.1F, -0.8F, 0.8F, 16719920, 6).delay(i));
                }

                VfxClient.burst(h, d, 10, 0.3, ModParticles.spark(16719920, 0.4F, 8));
                break;
            case CURSED_SPEECH: {
                Vec3 mouth = e.getEyePosition().add(d.scale(0.5)).add(0.0, -0.15, 0.0);
                for (int i = 0; i < 3; i++) {
                    VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(mouth.add(d.scale(0.4 * i))), d, 0.1F, 0.6F + 0.3F * i, 0.12F, 11894015, 8, FRRenderTypes.RING).delay(i));
                }
                break;
            }
            case BOOGIE_WOOGIE:
                VfxEffects.add(new VfxEffects.Flash(hand, 0.1F, 0.9F, 16757575, 5));
                VfxClient.burst(h, Vec3.ZERO, 12, 0.25, ModParticles.star(16757575, 0.35F, 10));
                break;
            case HEAVENLY_BODY:
                VfxClient.debris(f, 6 + slot * 2);
                VfxEffects.add(new VfxEffects.Ring(() -> feet.get().add(0.0, 0.08, 0.0), UP, 0.3F, 1.8F, 0.4F, 15259808, 8, FRRenderTypes.SHOCK).alpha(0.6F));
                VfxClient.burst(f.add(0.0, 0.2, 0.0), UP, 10, 0.15, ModParticles.smoke(9079416, 1.2F, 18));
                break;
            case BLOOD_MANIPULATION:
            case DEATH_PAINTING:
                VfxClient.blood(h, 14 + slot * 4, 0.6F);
                VfxClient.burst(h, Vec3.ZERO, 6, 0.12, ModParticles.smoke(t == Technique.DEATH_PAINTING ? 3811848 : 4194312, 0.7F, 16));
                break;
            case RATIO: {
                Vec3[] ab = VfxDraw.basis(d);
                Vec3 at = h.add(d.scale(0.6));
                VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(at.add(ab[0].scale(-0.9)).add(ab[1].scale(0.5))), VfxClient.fixed(at.add(ab[0].scale(0.9)).add(ab[1].scale(-0.5))), 0.12F, 15253600, 10));
                VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(at.add(ab[0].scale(0.18)).add(ab[1].scale(-0.1))), 0.1F, 0.5F, 16777215, 8));
                break;
            }
            case STRAW_DOLL:
                VfxClient.burst(h, d, 14, 0.35, ModParticles.spark(14708784, 0.4F, 9));
                VfxEffects.add(new VfxEffects.Flash(hand, 0.1F, 0.8F, 16752700, 6));
                break;
            case DISASTER_FLAMES:
                VfxClient.burst(h, UP, 16 + slot * 4, 0.2, ModParticles.ember(16738832, 0.45F, 24));
                for (int i = 0; i < 12 + slot * 4; i++) {
                    VfxClient.add(ParticleTypes.FLAME, f.x + rnd.nextGaussian() * 0.5, f.y + rnd.nextDouble() * 0.5, f.z + rnd.nextGaussian() * 0.5, 0.0, 0.06 + rnd.nextDouble() * 0.08, 0.0);
                }
                break;
            case IDLE_TRANSFIGURATION:
                for (int i = 0; i < 10 + slot * 3; i++) {
                    VfxClient.add(ModParticles.glow(i % 2 == 0 ? 11192575 : 8044728, 0.4F, 30), b.x + rnd.nextGaussian() * 0.5, b.y + rnd.nextGaussian() * 0.6, b.z + rnd.nextGaussian() * 0.5, 0.0, 0.03, 0.0);
                }
                break;
            case DISASTER_PLANTS:
                for (int i = 0; i < 12 + slot * 4; i++) {
                    VfxClient.add(ModParticles.petal(i % 3 == 0 ? 1708052 : 16033992, 0.6F, 40), f.x + rnd.nextGaussian() * 0.8, f.y + rnd.nextDouble() * 1.5, f.z + rnd.nextGaussian() * 0.8, rnd.nextGaussian() * 0.04, 0.06, rnd.nextGaussian() * 0.04);
                }
                break;
            case CURSE_MANIPULATION:
            case SWARM:
                VfxClient.burst(b, Vec3.ZERO, 14 + slot * 4, 0.2, ModParticles.smoke(t == Technique.SWARM ? 1182212 : 1185544, 0.9F, 22));
                VfxClient.converge(h, 10, 1.6, color);
                break;
            case TEN_SHADOWS: {
                Supplier<Vec3> floor = () -> feet.get().add(0.0, 0.03, 0.0);
                VfxEffects.add(new VfxEffects.Decal(floor, UP, 0.3F, 1.6F + slot * 0.3F, 0.0F, VfxDraw.argb(0.95F, 0), 30, FRRenderTypes.GLOW, false));
                VfxClient.burst(f.add(0.0, 0.1, 0.0), UP, 12, 0.12, ModParticles.smoke(65793, 1.0F, 20));
                break;
            }
            case PROJECTION:
                for (int i = 1; i <= 3; i++) {
                    VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(b.subtract(d.scale(0.4 * i))), 0.9F, 0.8F, 8050920, 8).alpha(0.4F / i).delay(i));
                }
                break;
            case ICE_FORMATION:
                VfxClient.burst(h, Vec3.ZERO, 16, 0.2, ModParticles.star(14743807, 0.35F, 20));
                VfxClient.add(ParticleTypes.SNOWFLAKE, h.x, h.y, h.z, 0.0, 0.02, 0.0);
                break;
            case DISASTER_TIDES:
                VfxClient.burst(f.add(0.0, 0.2, 0.0), UP, 20 + slot * 6, 0.35, ParticleTypes.SPLASH);
                VfxClient.burst(h, Vec3.ZERO, 10, 0.12, ModParticles.glow(9099519, 0.4F, 18));
                break;
            case SMALLPOX:
                VfxClient.burst(f.add(0.0, 0.3, 0.0), UP, 12, 0.1, ModParticles.smoke(6973536, 1.4F, 30));
                break;
            case IDLE_DEATH_GAMBLE:
                VfxClient.burst(b, UP, 14 + slot * 3, 0.3, ModParticles.star(rnd.nextBoolean() ? 16769088 : 7004282, 0.4F, 20));
                break;
            case LIGHTNING:
                for (int i = 0; i < 3 + slot; i++) {
                    Vec3 a = b.add(rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.5, rnd.nextGaussian() * 0.3);
                    VfxEffects.add(new VfxEffects.Bolt(a, a.add(rnd.nextGaussian() * 1.2, rnd.nextGaussian() * 1.2, rnd.nextGaussian() * 1.2), 0.08F, 0.3F, 5, i % 2 == 0 ? 16777215 : 8048895, 4, true).delay(i));
                }
                break;
            case STAR_RAGE:
                VfxClient.burst(h, Vec3.ZERO, 14, 0.25, ModParticles.star(16762960, 0.45F, 18));
                VfxEffects.add(new VfxEffects.Ring(hand, d, 0.1F, 1.2F, 0.3F, 16773312, 8, FRRenderTypes.SHOCK));
                break;
            case DEADLY_SENTENCING:
                VfxEffects.add(new VfxEffects.Decal(() -> feet.get().add(0.0, 0.04, 0.0), UP, 0.3F, 1.5F, 0.05F, VfxDraw.argb(0.7F, 15259808), 18, FRRenderTypes.SEAL, true));
                break;
            case GRANITE_BLAST:
                VfxClient.burst(h, d, 16, 0.4, ModParticles.spark(16773312, 0.45F, 10));
                VfxClient.debris(f, 4);
                break;
            case CONSTRUCTION:
                for (int i = 0; i < 3; i++) {
                    Vec3 n = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
                    VfxEffects.add(new VfxEffects.Ring(hand, n, 0.6F, 0.15F, 0.08F, 14737663, 10, FRRenderTypes.RING).delay(i * 2));
                }
                VfxClient.converge(h, 10, 0.9, 13158655);
                break;
            case SKY_MANIPULATION:
                for (int i = 0; i < 3; i++) {
                    VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(h.add(d.scale(0.6 + i * 0.5))), d, 0.2F, 1.0F + i * 0.4F, 0.1F, 12572927, 10, FRRenderTypes.RING).alpha(0.6F).delay(i));
                }
                break;
            case RIKA: {
                Vec3 back = b.subtract(new Vec3(d.x, 0.0, d.z).normalize().scale(1.1)).add(0.0, 0.8, 0.0);
                VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(back), 1.0F, 2.6F + slot * 0.4F, 15764160, 16).alpha(0.45F));
                VfxClient.burst(back, Vec3.ZERO, 16, 0.15, ModParticles.smoke(2759208, 1.4F, 24));
            }
        }
    }

    // ------------------------------------------------------------------ weapons

    /** Revolver: muzzle flash and the tracer to what it hit (extra 1: Mai's constructed bullet). */
    private static void muzzle(Vec3 barrel, Vec3 shot, int extra) {
        boolean construct = extra == 1;
        Vec3 d = VfxClient.dirOr(shot);
        Vec3 hit = barrel.add(shot);
        int core = construct ? 13421823 : 16769184;
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(barrel), 0.15F, construct ? 1.4F : 0.7F, 16777215, 3));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(barrel.add(d.scale(0.2))), 0.3F, construct ? 2.0F : 1.0F, core, 4));
        Vec3[] ab = VfxDraw.basis(d);

        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2.0 + 0.4;
            Vec3 spike = d.scale(0.5).add(ab[0].scale(Math.cos(a) * 0.25)).add(ab[1].scale(Math.sin(a) * 0.25));
            VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(barrel), VfxClient.fixed(barrel.add(spike)), 0.08F, 16769184, 3));
        }

        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(barrel), VfxClient.fixed(hit), construct ? 0.16F : 0.05F, core, construct ? 9 : 4));
        VfxEffects.add(new VfxEffects.Beam(VfxClient.fixed(barrel), VfxClient.fixed(hit), construct ? 0.05F : 0.02F, 16777215, construct ? 7 : 3));
        VfxClient.burst(barrel, d, 6, 0.08, ModParticles.smoke(10526880, 0.6F, 16));
        VfxClient.add(ModParticles.spark(14725216, 0.3F, 14), barrel.x, barrel.y, barrel.z, ab[0].x * 0.15, 0.12, ab[0].z * 0.15);
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(hit), 0.2F, construct ? 1.6F : 0.7F, core, 5));
        VfxClient.burst(hit, d.reverse(), construct ? 24 : 8, construct ? 0.45 : 0.3, ModParticles.spark(core, 0.4F, 8));
        VfxClient.debris(hit.add(0.0, 0.5, 0.0), construct ? 6 : 2);
        if (construct) {
            for (int i = 0; i < 3; i++) {
                VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(barrel.add(d.scale(0.3 + i * 0.35))), d, 0.6F, 0.1F, 0.08F, 14737663, 6, FRRenderTypes.RING).delay(i));
            }

            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(hit), d, 0.2F, 2.2F, 0.4F, 13421823, 10, FRRenderTypes.SHOCK));
        }
    }

    private static void unravel(int entity, Vec3 pos, float s) {
        Supplier<Vec3> body = VfxClient.follow(entity, pos, 0.5);
        Vec3 c = body.get();
        RandomSource rnd = VfxClient.random;
        VfxEffects.add(new VfxEffects.Flash(body, 0.3F, 1.6F * s, 13158655, 10));
        VfxEffects.add(new VfxEffects.Decal(VfxClient.follow(entity, pos, 0.02), UP, 0.4F, 1.4F * s, 0.2F, VfxDraw.argb(0.8F, 11579647), 14, FRRenderTypes.SEAL, true));

        for (int i = 0; i < 9; i++) {
            Vec3 a = c.add(rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.25);
            Vec3 to = a.add(new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6 + 0.4, rnd.nextGaussian()).normalize().scale(1.2 + rnd.nextDouble() * 1.4));
            VfxEffects.add(new VfxEffects.Bolt(a, to, 0.05F, 0.25F, 6, i % 3 == 0 ? 16777215 : 9408511, 8 + rnd.nextInt(5), true));
        }

        VfxClient.burst(c, Vec3.ZERO, 20, 0.3, ModParticles.spark(13158655, 0.35F, 12));
        VfxClient.burst(c, Vec3.ZERO, 10, 0.08, ModParticles.smoke(1052688, 1.0F, 20));
    }

    private static void broom(Vec3 pos, Vec3 vel, float speed) {
        Vec3 back = VfxClient.dirOr(vel).reverse();
        RandomSource rnd = VfxClient.random;

        for (int i = 0; i < 3; i++) {
            VfxClient.add(
                ModParticles.glow(i == 0 ? 16777215 : 13168848, 0.3F + speed * 0.2F, 10),
                pos.x + rnd.nextGaussian() * 0.15,
                pos.y + rnd.nextGaussian() * 0.1,
                pos.z + rnd.nextGaussian() * 0.15,
                back.x * 0.05,
                -0.01,
                back.z * 0.05
            );
        }

        if (speed > 0.4F && rnd.nextInt(3) == 0) {
            VfxClient.add(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, back.x * 0.1, 0.0, back.z * 0.1);
        }
    }

    private static void impactHeavy(Vec3 pos, Vec3 dir, float s, int color) {
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.5F * s, 2.2F * s, 16777215, 6));
        VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(pos), 0.7F * s, 3.0F * s, color, 10));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), UP, 0.4F, 3.0F * s, 0.8F, color, 14, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(pos.add(0.0, 0.1, 0.0)), UP, 0.3F, 2.0F * s, 0.3F, 16777215, 10, FRRenderTypes.RING).delay(2));
        VfxEffects.add(new VfxEffects.Decal(VfxClient.fixed(pos.add(0.0, 0.03, 0.0)), UP, 0.5F * s, 1.6F * s, 0.0F, -804253680, 60, FRRenderTypes.CRACKS, false));
        VfxClient.debris(pos.add(0.0, 0.4, 0.0), (int)(14.0F * s));
        VfxClient.burst(pos, UP, (int)(24.0F * s), 0.45, ModParticles.spark(color, 0.55F, 12));
        VfxClient.burst(pos.add(0.0, 0.3, 0.0), UP, (int)(10.0F * s), 0.15, ModParticles.smoke(6971992, 1.8F, 30));
    }

    private VfxExtra() {
    }

    // ------------------------------------------------------------------ effect classes

    /** The seconds of the hand sign: black wisps rising, cursed energy gathering into the hands. */
    private static final class SignFx extends VfxEffects.Fx {
        private final Supplier<Vec3> feet;
        private final Supplier<Vec3> hands;
        private final int color;

        SignFx(Supplier<Vec3> feet, Supplier<Vec3> hands, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.feet = feet;
            this.hands = hands;
            this.color = color;
        }

        @Override
        protected void tick() {
            Vec3 f = this.feet.get();
            RandomSource rnd = VfxClient.random;

            for (int i = 0; i < 4; i++) {
                double a = rnd.nextDouble() * Math.PI * 2.0;
                double r = 0.6 + rnd.nextDouble() * 1.6;
                VfxClient.add(ModParticles.smoke(i == 0 ? 1315860 : 263172, 1.3F, 26), f.x + Math.cos(a) * r, f.y + 0.1, f.z + Math.sin(a) * r, 0.0, 0.07 + rnd.nextDouble() * 0.05, 0.0);
            }

            VfxClient.converge(this.hands.get(), 3, 1.5, this.color);
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float pulse = 0.8F + 0.2F * Mth.sin(age * 1.3F);
            VfxDraw.billboard(m, vc, cam, this.hands.get(), (0.3F + 0.5F * t) * pulse, age * 0.1F, VfxDraw.argb(0.8F, this.color));
            VfxDraw.billboard(m, vc, cam, this.hands.get(), 0.15F + 0.15F * t, 0.0F, VfxDraw.argb(1.0F, 16777215));
        }
    }

    /** Barrier fragments: thrown outward from the dome, tumbling and falling. Positions are a function of age. */
    private static final class Shards {
        final int n;
        final Vec3[] p0;
        final Vec3[] v;
        final Vec3[] axis;
        final Vec3[] normal;
        final float[] spin;
        final float[] size;

        Shards(Vec3 c, float r, RandomSource rnd) {
            this.n = Mth.clamp((int)(r * 7.0F), 50, 140);
            this.p0 = new Vec3[this.n];
            this.v = new Vec3[this.n];
            this.axis = new Vec3[this.n];
            this.normal = new Vec3[this.n];
            this.spin = new float[this.n];
            this.size = new float[this.n];

            for (int i = 0; i < this.n; i++) {
                Vec3 nrm = new Vec3(rnd.nextGaussian(), Math.abs(rnd.nextGaussian()) * 0.9 + 0.05, rnd.nextGaussian()).normalize();
                this.p0[i] = c.add(nrm.scale((double)r));
                this.v[i] = nrm.scale(0.12 + rnd.nextDouble() * 0.3).add(0.0, 0.05 + rnd.nextDouble() * 0.12, 0.0);
                this.axis[i] = new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
                this.normal[i] = nrm;
                this.spin[i] = (float)(rnd.nextGaussian() * 0.25);
                this.size[i] = (0.4F + rnd.nextFloat() * 0.9F) * Math.max(1.0F, r / 12.0F);
            }
        }

        Vec3 at(int i, float age) {
            return this.p0[i].add(this.v[i].scale((double)age)).add(0.0, -0.022 * (double)age * (double)age, 0.0);
        }

        /** Rodrigues rotation of the shard's surface normal around its tumble axis. */
        Vec3 facing(int i, float age) {
            double a = (double)(this.spin[i] * age);
            Vec3 k = this.axis[i];
            Vec3 n = this.normal[i];
            return n.scale(Math.cos(a)).add(k.cross(n).scale(Math.sin(a))).add(k.scale(k.dot(n) * (1.0 - Math.cos(a))));
        }
    }

    private static final class ShardFx extends VfxEffects.Fx {
        private final VfxExtra.Shards shards;
        private final boolean glow;
        private final int color;

        ShardFx(VfxExtra.Shards shards, boolean glow, int color) {
            super(40, glow ? FRRenderTypes.additive(FRRenderTypes.SHARD) : FRRenderTypes.translucent(FRRenderTypes.SHARD));
            this.shards = shards;
            this.glow = glow;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float a = VfxEffects.fade(t, 0.0F, 0.45F);
            int argb = this.glow ? VfxDraw.argb(0.22F * a, this.color) : VfxDraw.argb(a, 328968);

            for (int i = 0; i < this.shards.n; i++) {
                Vec3 nrm = this.shards.facing(i, age);
                Vec3[] ab = VfxDraw.basis(nrm);
                float s = this.shards.size[i] * (this.glow ? 1.08F : 1.0F) * (1.0F - t * 0.4F);
                VfxDraw.plane(m, vc, this.shards.at(i, age), ab[0], ab[1], s, argb);
            }
        }
    }

    static void chainLinks(Supplier<Vec3> from, Supplier<Vec3> to, int life) {
        VfxEffects.add(new VfxExtra.ChainFx(from, to, life, false));
        VfxEffects.add(new VfxExtra.ChainFx(from, to, life, true));
    }

    /** Chain of a Thousand Miles: alternating links strung from the hand to the hooked target. */
    private static final class ChainFx extends VfxEffects.Fx {
        private final Supplier<Vec3> from;
        private final Supplier<Vec3> to;
        private final boolean shine;

        ChainFx(Supplier<Vec3> from, Supplier<Vec3> to, int life, boolean shine) {
            super(life, shine ? FRRenderTypes.additive(FRRenderTypes.RING) : FRRenderTypes.translucent(FRRenderTypes.RING));
            this.from = from;
            this.to = to;
            this.shine = shine;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 a = this.from.get();
            Vec3 b = this.to.get();
            Vec3 d = b.subtract(a);
            double len = d.length();
            if (!(len < 0.1)) {
                Vec3 dir = d.scale(1.0 / len);
                Vec3[] ab = VfxDraw.basis(dir);
                float alpha = VfxEffects.fade(t, 0.0F, 0.4F);
                int col = this.shine ? VfxDraw.argb(0.35F * alpha, 15263992) : VfxDraw.argb(0.95F * alpha, 8421512);
                int n = Math.min(120, (int)(len / 0.3));

                for (int i = 0; i <= n; i++) {
                    double f = (double)i / Math.max(1, n);
                    double sag = Math.sin(f * Math.PI) * Math.min(1.2, len * 0.04) * (1.0F - t);
                    Vec3 p = a.add(d.scale(f)).add(0.0, -sag, 0.0);
                    VfxDraw.ring(m, vc, p, i % 2 == 0 ? ab[0] : ab[1], 0.13F, 0.05F, col, 10);
                }
            }
        }
    }

    /** Black Rope lash: the rope flies out along a curve, cracks at the tip and is drawn back. */
    private static final class RopeFx extends VfxEffects.Fx {
        private final Supplier<Vec3> hand;
        private final Vec3 reach;
        private final Vec3 side;

        RopeFx(int entity, Vec3 pos, Vec3 reach, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            Entity e = VfxClient.level.getEntity(entity);
            this.hand = e != null ? () -> VfxClient.handOf(e) : VfxClient.fixed(pos);
            this.reach = reach;
            Vec3 s = reach.cross(UP);
            this.side = s.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : s.normalize();
            Vec3 tip = pos.add(reach);
            VfxEffects.add(new VfxEffects.Flash(VfxClient.fixed(tip), 0.1F, 1.0F, 16777215, 4).delay(3));
            VfxEffects.add(new VfxEffects.Ring(VfxClient.fixed(tip), VfxClient.dirOr(reach), 0.1F, 1.2F, 0.2F, 11579647, 6, FRRenderTypes.RING).delay(3));
        }

        private Vec3 point(Vec3 h, float f, float ext, float age) {
            float wave = Mth.sin(f * 7.0F - age * 1.6F) * 0.35F * (1.0F - f) * f * 4.0F * (1.0F - ext * 0.5F);
            float sweep = Mth.sin(f * (float)Math.PI) * 1.2F * (1.0F - ext);
            return h.add(this.reach.scale((double)(f * ext))).add(this.side.scale((double)(sweep + wave))).add(0.0, (double)(Mth.sin(f * (float)Math.PI) * 0.4F * ext), 0.0);
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float ext = t < 0.4F ? VfxEffects.easeOut(t / 0.4F) : 1.0F - (t - 0.4F) / 0.6F;
            Vec3 h = this.hand.get();
            Vec3 prev = h;
            int col = VfxDraw.argb(0.95F, 1052696);

            for (int i = 1; i <= 16; i++) {
                float f = (float)i / 16.0F;
                Vec3 p = this.point(h, f, ext, age);
                float w = 0.07F * (1.0F - f * 0.5F);
                VfxDraw.beam(m, vc, prev, p, w, w, col, col, eye);
                prev = p;
            }
        }
    }

    /** Black Rope spin: a loop of rope whirling around the holder. */
    private static final class SpinFx extends VfxEffects.Fx {
        private final Supplier<Vec3> center;

        SpinFx(int entity, Vec3 pos, int life) {
            super(life, FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.center = VfxClient.follow(entity, pos, 0.55);
        }

        @Override
        protected void tick() {
            if (this.age % 3 == 0) {
                Vec3 c = this.center.get();
                double a = (double)this.age * 0.9;
                VfxClient.add(ModParticles.smoke(1052688, 0.5F, 10), c.x + Math.cos(a) * 2.6, c.y, c.z + Math.sin(a) * 2.6, 0.0, 0.0, 0.0);
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 c = this.center.get();
            int col = VfxDraw.argb(0.9F * VfxEffects.fade(t, 0.05F, 0.2F), 1052696);
            double base = (double)age * 0.9;
            Vec3 prev = null;

            for (int i = 0; i <= 20; i++) {
                double a = base + (double)i * 0.26;
                double r = 0.4 + 2.2 * Math.min(1.0, (double)i / 6.0);
                Vec3 p = c.add(Math.cos(a) * r, Math.sin(a * 2.0) * 0.15, Math.sin(a) * r);
                if (prev != null) {
                    VfxDraw.beam(m, vc, prev, p, 0.06F, 0.06F, col, col, eye);
                }

                prev = p;
            }
        }
    }
}
