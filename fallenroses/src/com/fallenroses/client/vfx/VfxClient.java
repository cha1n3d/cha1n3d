package com.fallenroses.client.vfx;

import com.fallenroses.network.VfxPacket;
import com.fallenroses.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VfxClient {
    private static final int BLUE = 4885759;
    private static final int BLUE_LIGHT = 12574975;
    private static final int RED = 16722462;
    private static final int RED_LIGHT = 16756896;
    private static final int PURPLE = 10112255;
    private static final int VIOLET = 11894015;
    private static final int GOLD = 16757575;
    private static final int PALE = 15259808;
    private static final int WHITE = 16777215;
    private static final int BLOOD = 10095124;
    private static final int CE = 6970111;
    static ClientLevel level;
    static RandomSource random;

    public static void play(VfxPacket p) {
        Minecraft mc = Minecraft.getInstance();
        level = mc.level;
        if (level != null) {
            random = level.random;
            Vec3 pos = p.pos();
            Vec3 dir = p.dir();
            float s = p.scale();
            MangaFx.onVfx(p);
            switch (p.kind()) {
                case MANGA_CAST:
                    VfxExtra.onCast(p);
                    break;
                case HIT_SPARK:
                    hitSpark(pos, dir, s, p.color());
                    break;
                case REINFORCE:
                    reinforce(p.entity(), pos, p.extra() == 1 ? 15259808 : 6970111);
                    break;
                case BLACK_FLASH:
                    blackFlash(pos, dir);
                    break;
                case LEVEL_UP:
                    levelUp(p.entity(), pos, s, p.extra() != 0 ? p.extra() : 12098815);
                    break;
                case EXORCISM:
                    exorcism(pos, s, p.color(), p.extra());
                    break;
                case RCT_HEAL:
                    rctHeal(p.entity(), pos);
                    break;
                case BLUE_CAST:
                    blueCast(pos);
                    break;
                case RED_CHARGE:
                    charge(p.entity(), pos, p.color(), p.extra(), s);
                    break;
                case RED_IMPACT:
                    impact(pos, dir, s, p.extra() == 1 ? 4885759 : (p.color() == 16777215 ? 16722462 : p.color()), p.extra() == 1 ? 12574975 : 16756896);
                    break;
                case PURPLE_CHARGE:
                    purpleCharge(p.entity(), pos, p.extra());
                    break;
                case PURPLE_IMPACT:
                    purpleImpact(pos, s);
                    break;
                case INFINITY_RIPPLE:
                    infinityRipple(pos, dir, s);
                    break;
                case CLEAVE:
                    cleave(p.entity(), pos, s);
                    break;
                case SPIDERWEB:
                    spiderweb(pos, s);
                    break;
                case SLASH_HIT:
                    slashHit(pos, dir, s, p.extra());
                    break;
                case FUGA_IGNITE:
                    fuga(pos, s);
                    break;
                case SPEECH_WAVE:
                    speechWave(pos, dir, s);
                    break;
                case SPEECH_SEAL:
                    speechSeal(p.entity(), pos, s, p.extra());
                    break;
                case TWIST:
                    twist(p.entity(), pos, s);
                    break;
                case EXPLODE:
                    explode(pos, s, 11894015);
                    break;
                case CLAP:
                    clap(pos, dir);
                    break;
                case SWAP:
                    swap(pos, dir);
                    break;
                case DASH:
                    dash(pos, dir);
                    break;
                case SLAM:
                    slam(pos, s);
                    break;
                case FLURRY_HIT:
                    flurryHit(pos, dir, s);
                    break;
                case INSTINCT:
                    instinct(p.entity(), pos);
                    break;
                case BERSERK:
                    berserk(p.entity(), pos, s);
                    break;
                case DOMAIN_OPEN:
                    domainOpen(pos, s, p.color(), p.extra());
                    break;
                case VANISH:
                    vanish(pos, s);
                    break;
                case CHAOS_CLAP:
                    chaos(pos, s);
                    break;
                case CURSE_ORB_HIT:
                    explode(pos, 1.2F, 6953632);
                    break;
                case ROOT_ERUPT:
                    roots(pos, s, p.extra());
                    break;
                default:
                    VfxTechniques.play(p);
            }
        }
    }

    private static void hitSpark(Vec3 pos, Vec3 dir, float s, int color) {
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.3F * s, 0.9F * s, 16777215, 5));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.6F * s, 1.4F * s, color, 8));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), dirOr(dir), 0.2F, 1.3F * s, 0.25F * s, color, 7, FRRenderTypes.RING));
        burst(pos, dir, 10, 0.35 * (double)s, ModParticles.spark(color, 0.6F, 8));
    }

    private static void reinforce(int entity, Vec3 pos, int color) {
        Supplier<Vec3> feet = follow(entity, pos, 0.02);
        VfxEffects.add(new VfxEffects.Ring(feet, new Vec3(0.0, 1.0, 0.0), 0.3F, 1.8F, 0.4F, color, 14, FRRenderTypes.RING));
        VfxEffects.add(new VfxEffects.Flash(follow(entity, pos, 0.5), 0.5F, 1.6F, color, 10).alpha(0.6F));

        for (int i = 0; i < 24; i++) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            double r = 0.5 + random.nextDouble() * 0.3;
            Vec3 f = feet.get();
            add(
                ModParticles.glow(color, 0.8F, 16),
                f.x + Math.cos(a) * r,
                f.y + random.nextDouble() * 1.8,
                f.z + Math.sin(a) * r,
                0.0,
                0.05 + random.nextDouble() * 0.05,
                0.0
            );
        }
    }

    private static void blackFlash(Vec3 pos, Vec3 dir) {
        for (int i = 0; i < 9; i++) {
            Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.8, random.nextGaussian()).normalize().scale(1.4 + random.nextDouble() * 2.4);
            int life = 8 + random.nextInt(5);
            VfxEffects.add(new VfxEffects.Bolt(pos, pos.add(d), 0.16F, 0.35F, 7, 16715808, life, true));
            VfxEffects.add(new VfxEffects.Bolt(pos, pos.add(d), 0.07F, 0.35F, 7, 0, life, false));
        }

        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.8F, 3.2F, 16717864, 7));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 0.3F, 1.6F, -536870912, 0, 6, false, 1.5F));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), dirOr(dir), 0.3F, 3.5F, 0.6F, 16715808, 10, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), dirOr(dir), 0.2F, 2.4F, 0.3F, 16777215, 6, FRRenderTypes.RING).alpha(0.7F));
        burst(pos, dir, 30, 0.5, ModParticles.spark(16719920, 0.8F, 10));
        burst(pos, dir, 12, 0.25, ModParticles.smoke(328965, 1.2F, 18));
    }

    private static void levelUp(int entity, Vec3 pos, float s, int color) {
        Supplier<Vec3> feet = follow(entity, pos, 0.0);
        VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, (double)(7.0F * s), 0.0), 0.7F * s, color, 30));
        VfxEffects.add(new VfxEffects.Beam(feet, () -> feet.get().add(0.0, (double)(7.0F * s), 0.0), 0.25F * s, 16777215, 24));
        VfxEffects.add(new VfxEffects.Ring(feet, new Vec3(0.0, 1.0, 0.0), 0.5F, 3.2F * s, 0.5F, color, 20, FRRenderTypes.RING));
        VfxEffects.add(new VfxEffects.Ring(feet, new Vec3(0.0, 1.0, 0.0), 0.3F, 2.2F * s, 0.3F, 16777215, 16, FRRenderTypes.RING).delay(4));

        for (int i = 0; i < 50; i++) {
            double a = (double)i * 0.5;
            double r = 0.8 + (double)(i % 5) * 0.08;
            Vec3 f = feet.get();
            add(
                ModParticles.glow(i % 3 == 0 ? 16777215 : color, 0.7F, 30),
                f.x + Math.cos(a) * r,
                f.y + (double)i * 0.04,
                f.z + Math.sin(a) * r,
                -Math.sin(a) * 0.05,
                0.08,
                Math.cos(a) * 0.05
            );
        }
    }

    private static void exorcism(Vec3 pos, float s, int color, int grade) {
        int n = (int)(20.0F + 18.0F * s);

        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.3, random.nextGaussian()).scale(0.08 * (double)s);
            add(ModParticles.smoke(656912, 1.6F + s * 0.5F, 40), pos.x + d.x * 4.0, pos.y + d.y * 4.0, pos.z + d.z * 4.0, d.x, d.y * 0.6, d.z);
        }

        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            double r = random.nextDouble() * (double)s * 0.6;
            add(
                ModParticles.glow(color, 0.6F, 30),
                pos.x + Math.cos(a) * r,
                pos.y + (random.nextDouble() - 0.5) * (double)s,
                pos.z + Math.sin(a) * r,
                -Math.sin(a) * 0.04,
                0.05 + random.nextDouble() * 0.06,
                Math.cos(a) * 0.04
            );
        }

        for (int i = 0; i < n / 2; i++) {
            add(
                ModParticles.petal(1314838, 0.6F, 60),
                pos.x + random.nextGaussian() * 0.6 * (double)s,
                pos.y + random.nextDouble() * (double)s,
                pos.z + random.nextGaussian() * 0.6 * (double)s,
                random.nextGaussian() * 0.05,
                0.05,
                random.nextGaussian() * 0.05
            );
        }

        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.5F * s, 2.0F * s, color, 14).alpha(0.7F));
        if (grade <= 1) {
            VfxEffects.add(new VfxEffects.Ring(fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.5F, 4.0F * s, 0.8F, color, 24, FRRenderTypes.SHOCK));
        }
    }

    private static void rctHeal(int entity, Vec3 pos) {
        Supplier<Vec3> body = follow(entity, pos, 0.5);
        VfxEffects.add(new VfxEffects.Flash(body, 0.6F, 1.4F, 13172712, 16).alpha(0.4F));

        for (int i = 0; i < 12; i++) {
            Vec3 b = body.get();
            add(
                ModParticles.glow(i % 2 == 0 ? 15269876 : 9109448, 0.5F, 24),
                b.x + random.nextGaussian() * 0.35,
                b.y + random.nextGaussian() * 0.5,
                b.z + random.nextGaussian() * 0.35,
                0.0,
                0.04,
                0.0
            );
        }
    }

    private static void blueCast(Vec3 hand) {
        VfxEffects.add(new VfxEffects.Flash(fixed(hand), 0.15F, 0.7F, 4885759, 8));
        VfxEffects.add(new VfxEffects.Flash(fixed(hand), 0.05F, 0.3F, 16777215, 6));
        converge(hand, 14, 1.4, 4885759);
    }

    private static void charge(int entity, Vec3 hand, int color, int duration, float s) {
        Entity e = level.getEntity(entity);
        Supplier<Vec3> at = e != null ? () -> handOf(e) : fixed(hand);
        VfxEffects.add(new VfxClient.ChargeFx(at, color, Math.max(4, duration), s));
    }

    private static void impact(Vec3 pos, Vec3 dir, float radius, int color, int light) {
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.0F, radius * 0.9F, light, 8));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.6F, radius * 0.5F, 16777215, 5));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 0.5F, radius, VfxDraw.argb(0.25F, color), VfxDraw.argb(0.9F, light), 12, true, 2.2F));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.5F, radius * 1.4F, 0.9F, color, 14, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), dirOr(dir), 0.3F, radius, 0.5F, light, 10, FRRenderTypes.RING).delay(1));
        burst(pos, Vec3.ZERO, 50, 0.7, ModParticles.spark(light, 0.9F, 12));
        burst(pos, Vec3.ZERO, 16, 0.18, ModParticles.smoke(1706506, 2.2F, 30));
        burst(pos, Vec3.ZERO, 20, 0.3, ModParticles.ember(color, 0.35F, 26));
    }

    private static void purpleCharge(int entity, Vec3 pos, int duration) {
        Entity e = level.getEntity(entity);
        if (e != null) {
            VfxEffects.add(new VfxClient.PurpleChargeFx(e, Math.max(10, duration)));
        }
    }

    private static void purpleImpact(Vec3 pos, float s) {
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.0F, s * 1.3F, 14199039, 14));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.0F, s * 0.8F, 16777215, 8));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), s * 0.6F, s * 1.4F, VfxDraw.argb(0.2F, 10112255), VfxDraw.argb(1.0F, 14729471), 18, true, 2.5F));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), new Vec3(0.0, 1.0, 0.0), 1.0F, s * 2.2F, 1.2F, 10112255, 20, FRRenderTypes.SHOCK));
        burst(pos, Vec3.ZERO, 80, 1.0, ModParticles.spark(14731519, 1.1F, 16));
    }

    private static void infinityRipple(Vec3 pos, Vec3 dir, float s) {
        Vec3 n = dirOr(dir);
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), n, 0.05F, s, 0.12F * s, 12574975, 10, FRRenderTypes.RING).alpha(0.6F));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), n, 0.05F, s * 0.6F, 0.08F * s, 16777215, 8, FRRenderTypes.RING).alpha(0.5F).delay(2));
    }

    private static void cleave(int entity, Vec3 pos, float h) {
        Supplier<Vec3> c = follow(entity, pos, 0.5);
        Vec3 center = c.get();
        float r = Math.max(0.8F, h * 0.7F);

        for (int i = 0; i < 7; i++) {
            Vec3 axis = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3[] ab = VfxDraw.basis(axis);
            Vec3 off = ab[0].scale((double)(-r) * 0.9);
            VfxEffects.add(new VfxEffects.Slash(center.add(off), ab[0], ab[1], r * 1.6F, 0.22F * r, -0.9F, 0.9F, 16777215, 7).delay(i));
            VfxEffects.add(new VfxEffects.Slash(center.add(off), ab[0], ab[1], r * 1.62F, 0.08F * r, -0.9F, 0.9F, 16719920, 8).delay(i));
        }

        VfxEffects.add(new VfxEffects.Flash(c, 0.5F, 1.6F * r, 16777215, 6).delay(2));
        blood(center, 30, r);
    }

    private static void spiderweb(Vec3 pos, float radius) {
        VfxEffects.add(
            new VfxEffects.Decal(fixed(pos.add(0.0, 0.02, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, radius, 0.0F, -535818224, 60, FRRenderTypes.CRACKS, false)
        );
        VfxEffects.add(
            new VfxEffects.Decal(fixed(pos.add(0.0, 0.04, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, radius, 0.0F, 16724032, 30, FRRenderTypes.CRACKS, true)
        );

        for (int i = 0; i < 10; i++) {
            double a = (double)((float)i * (float) (Math.PI * 2) / 10.0F) + random.nextDouble() * 0.3;
            Vec3 d = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 end = pos.add(d.scale((double)radius)).add(0.0, 0.1, 0.0);
            VfxEffects.add(new VfxEffects.Beam(fixed(pos.add(0.0, 0.1, 0.0)), fixed(end), 0.12F, 16777215, 8).delay(i / 3));
            VfxEffects.add(new VfxEffects.Beam(fixed(pos.add(0.0, 0.1, 0.0)), fixed(end), 0.3F, 16719920, 10).delay(i / 3));

            for (int k = 0; k < 6; k++) {
                debris(pos.add(d.scale((double)(radius * (float)(k + 1)) / 6.0)), 2);
            }
        }

        VfxEffects.add(
            new VfxEffects.Ring(fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, radius, 0.6F, 16777215, 10, FRRenderTypes.SHOCK).alpha(0.7F)
        );
    }

    private static void slashHit(Vec3 pos, Vec3 dir, float s, int rollDeg) {
        Vec3 fwd = dirOr(dir);
        Vec3[] ab = VfxDraw.basis(fwd);
        double roll = Math.toRadians((double)rollDeg);
        Vec3 a = ab[0].scale(Math.cos(roll)).add(ab[1].scale(Math.sin(roll)));
        Vec3 b = fwd.cross(a).normalize();
        float r = 0.9F * s;
        VfxEffects.add(new VfxEffects.Slash(pos.subtract(a.scale((double)r)), a, b, r * 1.6F, 0.2F * r, -1.0F, 1.0F, 16777215, 6));
        VfxEffects.add(new VfxEffects.Slash(pos.subtract(a.scale((double)r)), a, b, r * 1.63F, 0.07F * r, -1.0F, 1.0F, 16719920, 7));
        burst(pos, Vec3.ZERO, 6, 0.3, ModParticles.spark(16777215, 0.5F, 6));
        blood(pos, 6, 0.4F);
    }

    private static void fuga(Vec3 pos, float radius) {
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.5F, radius * 1.4F, 16756800, 16));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.0F, radius * 0.7F, 16773312, 10));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 1.0F, radius, VfxDraw.argb(0.55F, 16740384), VfxDraw.argb(1.0F, 16765056), 20, true, 1.6F));
        VfxEffects.add(new VfxEffects.Beam(fixed(pos), fixed(pos.add(0.0, (double)radius * 2.2, 0.0)), radius * 0.5F, 16744496, 24));
        VfxEffects.add(
            new VfxEffects.Ring(fixed(pos.add(0.0, 0.2, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, radius * 1.6F, 1.2F, 16736272, 18, FRRenderTypes.SHOCK)
        );
        burst(pos, Vec3.ZERO, 90, 0.45, ModParticles.ember(16744480, 0.5F, 40));
        burst(pos, Vec3.ZERO, 30, 0.15, ModParticles.smoke(1707528, 2.6F, 50));

        for (int i = 0; i < 60; i++) {
            add(
                ParticleTypes.FLAME,
                pos.x + random.nextGaussian() * (double)radius * 0.4,
                pos.y + random.nextDouble() * 2.0,
                pos.z + random.nextGaussian() * (double)radius * 0.4,
                random.nextGaussian() * 0.1,
                0.1 + random.nextDouble() * 0.2,
                random.nextGaussian() * 0.1
            );
        }
    }

    private static void speechWave(Vec3 mouth, Vec3 dir, float s) {
        Vec3 d = dirOr(dir);

        for (int i = 0; i < 5; i++) {
            Vec3 at = mouth.add(d.scale(0.6 + (double)i * 1.2 * (double)s));
            VfxEffects.add(
                new VfxEffects.Ring(fixed(at), d, 0.2F, (0.6F + (float)i * 0.45F) * s, 0.25F, 11894015, 10, FRRenderTypes.RING).alpha(0.85F).delay(i)
            );
        }

        VfxEffects.add(new VfxEffects.Flash(fixed(mouth), 0.1F, 0.5F, 11894015, 8));
    }

    private static void speechSeal(int entity, Vec3 pos, float h, int duration) {
        Supplier<Vec3> feet = follow(entity, pos, 0.02);
        VfxEffects.add(
            new VfxEffects.Decal(
                feet, new Vec3(0.0, 1.0, 0.0), 0.3F, Math.max(0.9F, h * 0.7F), 0.04F, 13148415, Math.max(10, duration), FRRenderTypes.SEAL, true
            )
        );
        VfxEffects.add(new VfxEffects.Flash(follow(entity, pos, 0.5), 0.3F, 1.2F * h, 11894015, 10).alpha(0.6F));
    }

    private static void twist(int entity, Vec3 pos, float h) {
        Supplier<Vec3> body = follow(entity, pos, 0.0);
        VfxEffects.add(new VfxClient.TwistFx(body, Math.max(1.0F, h), 16));
        VfxEffects.add(new VfxEffects.Flash(follow(entity, pos, 0.5), 0.4F, 1.8F, 11894015, 10));
        blood(body.get().add(0.0, (double)h * 0.5, 0.0), 14, 0.6F);
    }

    private static void explode(Vec3 pos, float s, int color) {
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 0.3F, s, -804257768, 0, 10, false, 1.2F));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 0.4F, s * 1.1F, VfxDraw.argb(0.1F, color), VfxDraw.argb(0.95F, color), 10, true, 2.5F));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.5F, s * 1.2F, color, 8));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.4F, s * 1.6F, 0.5F, color, 12, FRRenderTypes.SHOCK));
        burst(pos, Vec3.ZERO, 30, 0.45, ModParticles.spark(color, 0.8F, 10));
        burst(pos, Vec3.ZERO, 14, 0.12, ModParticles.smoke(656400, 1.8F, 30));
    }

    private static void clap(Vec3 hand, Vec3 dir) {
        VfxEffects.add(new VfxEffects.Flash(fixed(hand), 0.1F, 0.6F, 16777215, 5));
        VfxEffects.add(new VfxEffects.Ring(fixed(hand), dirOr(dir), 0.1F, 1.4F, 0.2F, 16757575, 7, FRRenderTypes.RING));
        burst(hand, Vec3.ZERO, 8, 0.2, ModParticles.spark(16757575, 0.4F, 6));
    }

    private static void swap(Vec3 from, Vec3 vec) {
        Vec3 to = from.add(vec);
        VfxEffects.add(new VfxEffects.Beam(fixed(from), fixed(to), 0.25F, 16757575, 7));
        VfxEffects.add(new VfxEffects.Beam(fixed(from), fixed(to), 0.08F, 16777215, 5));

        for (Vec3 p : new Vec3[]{from, to}) {
            VfxEffects.add(new VfxEffects.Flash(fixed(p), 0.3F, 1.3F, 16757575, 8));
            burst(p, Vec3.ZERO, 8, 0.08, ModParticles.smoke(14209216, 1.0F, 14));
        }
    }

    private static void dash(Vec3 from, Vec3 vec) {
        Vec3 d = dirOr(vec);
        VfxEffects.add(new VfxEffects.Ring(fixed(from), d, 0.2F, 1.6F, 0.3F, 16777215, 8, FRRenderTypes.RING).alpha(0.7F));

        for (int i = 0; i < 7; i++) {
            Vec3 p = from.add(vec.scale((double)i / 7.0));
            VfxEffects.add(new VfxEffects.Flash(fixed(p), 0.9F, 0.6F, 15259808, 10).alpha(0.45F).delay(i / 2));
        }

        VfxEffects.add(new VfxEffects.Beam(fixed(from), fixed(from.add(vec)), 0.35F, 15259808, 8));

        for (int i = 0; i < 16; i++) {
            Vec3 p = from.add(vec.scale(random.nextDouble()));
            add(ModParticles.spark(16777215, 0.4F, 6), p.x, p.y + random.nextGaussian() * 0.4, p.z, d.x * 0.6, d.y * 0.6, d.z * 0.6);
        }
    }

    private static void slam(Vec3 pos, float radius) {
        VfxEffects.add(
            new VfxEffects.Ring(fixed(pos.add(0.0, 0.15, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, radius * 1.3F, 1.1F, 16777215, 14, FRRenderTypes.SHOCK)
        );
        VfxEffects.add(
            new VfxEffects.Ring(fixed(pos.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.3F, radius, 0.6F, 15259808, 18, FRRenderTypes.RING).delay(2)
        );
        VfxEffects.add(
            new VfxEffects.Decal(
                fixed(pos.add(0.0, 0.02, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, radius * 0.9F, 0.0F, -804253680, 80, FRRenderTypes.CRACKS, false
            )
        );
        VfxEffects.add(new VfxEffects.Flash(fixed(pos.add(0.0, 0.5, 0.0)), 0.5F, 2.5F, 16777215, 6));

        for (int i = 0; i < 40; i++) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            double r = random.nextDouble() * (double)radius;
            debris(pos.add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 1);
        }

        burst(pos.add(0.0, 0.3, 0.0), new Vec3(0.0, 0.3, 0.0), 24, 0.12, ModParticles.smoke(6971992, 2.0F, 30));
    }

    private static void flurryHit(Vec3 pos, Vec3 dir, float s) {
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.2F, 0.7F * s, 16777215, 4, FRRenderTypes.SHOCK, 0.6F));
        VfxEffects.add(new VfxEffects.Ring(fixed(pos), dirOr(dir), 0.1F, 0.9F * s, 0.15F, 15259808, 5, FRRenderTypes.RING));
        burst(pos, dir, (int)(6.0F * s), 0.3, ModParticles.spark(16777215, 0.4F, 5));
    }

    private static void instinct(int entity, Vec3 pos) {
        VfxEffects.add(new VfxEffects.Ring(follow(entity, pos, 0.02), new Vec3(0.0, 1.0, 0.0), 0.4F, 6.0F, 0.3F, 16757575, 20, FRRenderTypes.RING));
        VfxEffects.add(new VfxEffects.Flash(follow(entity, pos, 0.9), 0.2F, 0.9F, 16757575, 14));
    }

    private static void berserk(int entity, Vec3 pos, float s) {
        Supplier<Vec3> feet = follow(entity, pos, 0.02);
        if (s > 1.0F) {
            VfxEffects.add(new VfxEffects.Ring(feet, new Vec3(0.0, 1.0, 0.0), 0.4F, 3.0F * s, 0.6F, 13635612, 18, FRRenderTypes.SHOCK));
            VfxEffects.add(new VfxEffects.Flash(follow(entity, pos, 0.5), 0.6F, 2.5F * s, 16715808, 12));
        }

        Vec3 f = feet.get();

        for (int i = 0; i < (int)(10.0F * s); i++) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            add(ModParticles.glow(14684192, 0.7F, 18), f.x + Math.cos(a) * 0.5, f.y + random.nextDouble() * 1.8, f.z + Math.sin(a) * 0.5, 0.0, 0.06, 0.0);
            add(ModParticles.smoke(2752516, 0.8F, 20), f.x + Math.cos(a) * 0.4, f.y + random.nextDouble() * 1.2, f.z + Math.sin(a) * 0.4, 0.0, 0.03, 0.0);
        }
    }

    /** The moment the barrier appears: a shockwave of ink across the ground (the dome itself is DomainRenderer). */
    private static void domainOpen(Vec3 pos, float radius, int color, int type) {
        Vec3 feet = pos.add(0.0, -0.9, 0.0);
        VfxEffects.add(new VfxEffects.Ring(fixed(feet.add(0.0, 0.1, 0.0)), new Vec3(0.0, 1.0, 0.0), 1.0F, radius * 1.15F, 2.4F, color, 26, FRRenderTypes.SHOCK));
        VfxEffects.add(new VfxEffects.Ring(fixed(feet.add(0.0, 0.12, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.8F, radius, 0.6F, 16777215, 18, FRRenderTypes.RING).delay(2));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 1.0F, radius * 0.5F, type == 0 ? 16777215 : color, 12));

        for (int i = 0; i < 90; i++) {
            double a = random.nextDouble() * (float) (Math.PI * 2);
            double sp = 0.4 + random.nextDouble() * 0.5;
            add(ModParticles.smoke(i % 4 == 0 ? 1315860 : 263172, 2.2F, 30), feet.x, feet.y + 0.3, feet.z, Math.cos(a) * sp, 0.02, Math.sin(a) * sp);
        }

        if (type == 2) {
            burst(pos, Vec3.ZERO, 120, 0.8, ModParticles.ember(16738832, 0.7F, 50));
        }

        if (type == 3) {
            burst(pos, Vec3.ZERO, 60, 0.5, ModParticles.glow(8044728, 0.8F, 40));
        }

        if (type == 4) {
            burst(pos, Vec3.ZERO, 80, 0.6, ModParticles.glow(9099519, 0.8F, 40));
            burst(pos, new Vec3(0.0, 1.0, 0.0), 120, 0.9, ParticleTypes.SPLASH);
        }

        if (type == 1) {
            burst(pos, Vec3.ZERO, 60, 0.6, ModParticles.ember(16722480, 0.5F, 40));
        }
    }

    private static void vanish(Vec3 pos, float radius) {
        for (int i = 0; i < 3; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(fixed(pos), new Vec3(0.0, 1.0, 0.0), 0.5F, radius * (1.0F + (float)i * 0.25F), 1.4F, 11894015, 22, FRRenderTypes.SHOCK)
                    .delay(i * 3)
            );
        }

        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 1.0F, radius, -1073217520, 0, 18, false, 1.2F));
        VfxEffects.add(new VfxEffects.Sphere(fixed(pos), 1.0F, radius * 1.05F, VfxDraw.argb(0.05F, 10112255), VfxDraw.argb(0.9F, 13672703), 18, true, 3.0F));
        VfxEffects.add(new VfxEffects.Flash(fixed(pos), 2.0F, radius * 0.9F, 11894015, 12));
        burst(pos, Vec3.ZERO, 120, 1.2, ModParticles.spark(13676799, 1.0F, 18));
        burst(pos, Vec3.ZERO, 40, 0.3, ModParticles.smoke(525324, 3.0F, 40));
    }

    private static void chaos(Vec3 pos, float radius) {
        for (int i = 0; i < 5; i++) {
            VfxEffects.add(
                new VfxEffects.Ring(fixed(pos.add(0.0, -0.8, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.5F, radius, 0.7F, 16757575, 14, FRRenderTypes.RING).delay(i * 4)
            );
            VfxEffects.add(new VfxEffects.Flash(fixed(pos), 0.3F, 1.6F, 16777215, 6).delay(i * 4));
        }
    }

    private static void roots(Vec3 pos, float s, int phase) {
        if (phase == 0) {
            VfxEffects.add(
                new VfxEffects.Decal(fixed(pos.add(0.0, 0.03, 0.0)), new Vec3(0.0, 1.0, 0.0), 0.4F, 1.8F, 0.02F, -12973536, 24, FRRenderTypes.CRACKS, true)
            );

            for (int i = 0; i < 6; i++) {
                debris(pos.add(random.nextGaussian() * 0.8, 0.0, random.nextGaussian() * 0.8), 1);
            }
        } else {
            for (int i = 0; i < 7; i++) {
                Vec3 base = pos.add(random.nextGaussian() * 0.7, 0.0, random.nextGaussian() * 0.7);
                Vec3 tip = base.add(random.nextGaussian() * 0.6, 2.2 + random.nextDouble() * 1.6 * (double)s, random.nextGaussian() * 0.6);
                VfxEffects.add(new VfxEffects.Bolt(base, tip, 0.35F, 0.25F, 6, -266334200, 22, false));
                VfxEffects.add(new VfxEffects.Bolt(base, tip, 0.12F, 0.25F, 6, 5906448, 14, true));
            }

            for (int i = 0; i < 20; i++) {
                debris(pos.add(random.nextGaussian(), 0.0, random.nextGaussian()), 2);
            }

            for (int i = 0; i < 25; i++) {
                add(
                    ModParticles.petal(i % 3 == 0 ? 1708052 : 16033992, 0.7F, 50),
                    pos.x + random.nextGaussian(),
                    pos.y + random.nextDouble() * 3.0,
                    pos.z + random.nextGaussian(),
                    random.nextGaussian() * 0.08,
                    0.15,
                    random.nextGaussian() * 0.08
                );
            }
        }
    }

    static Supplier<Vec3> fixed(Vec3 p) {
        return () -> p;
    }

    public static Supplier<Vec3> follow(int id, Vec3 fallback, double yFrac) {
        return id < 0
            ? fixed(fallback)
            : () -> {
                Entity e = level == null ? null : level.getEntity(id);
                return e != null && !e.isRemoved()
                    ? e.getPosition(Minecraft.getInstance().getFrameTime()).add(0.0, (double)e.getBbHeight() * yFrac, 0.0)
                    : fallback;
            };
    }

    public static Vec3 handOf(Entity e) {
        float pt = Minecraft.getInstance().getFrameTime();
        Vec3 look = e.getViewVector(pt);
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        return e.getEyePosition(pt).add(look.scale(0.9)).add(right.scale(0.35)).add(0.0, -0.25, 0.0);
    }

    static Vec3 dirOr(Vec3 dir) {
        return dir.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : dir.normalize();
    }

    static void add(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(o, true, x, y, z, vx, vy, vz);
    }

    static void burst(Vec3 pos, Vec3 dir, int n, double speed, ParticleOptions o) {
        Vec3 bias = dir.lengthSqr() < 1.0E-6 ? Vec3.ZERO : dir.normalize().scale(0.6);

        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
                .normalize()
                .add(bias)
                .normalize()
                .scale(speed * (0.4 + random.nextDouble() * 0.8));
            add(o, pos.x, pos.y, pos.z, v.x, v.y, v.z);
        }
    }

    static void converge(Vec3 at, int n, double radius, int color) {
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
                .normalize()
                .scale(radius * (0.6 + random.nextDouble() * 0.6));
            Vec3 p = at.add(d);
            add(ModParticles.spark(i % 3 == 0 ? 16777215 : color, 0.4F, 7), p.x, p.y, p.z, -d.x * 0.25, -d.y * 0.25, -d.z * 0.25);
        }
    }

    static void blood(Vec3 pos, int n, float spread) {
        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3(random.nextGaussian(), random.nextGaussian() + 0.5, random.nextGaussian()).scale(0.12);
            add(
                ModParticles.glow(10095124, 0.35F, 12 + random.nextInt(8)),
                pos.x + random.nextGaussian() * (double)spread * 0.3,
                pos.y,
                pos.z + random.nextGaussian() * (double)spread * 0.3,
                v.x,
                v.y,
                v.z
            );
        }
    }

    static void debris(Vec3 at, int n) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState state = level.getBlockState(below);
        if (!state.isAir()) {
            for (int i = 0; i < n; i++) {
                add(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    at.x + random.nextGaussian() * 0.2,
                    at.y + 0.1,
                    at.z + random.nextGaussian() * 0.2,
                    random.nextGaussian() * 0.15,
                    0.3 + random.nextDouble() * 0.4,
                    random.nextGaussian() * 0.15
                );
            }
        }
    }

    private VfxClient() {
    }

    private static final class ChargeFx extends VfxEffects.Fx {
        private final Supplier<Vec3> at;
        private final int color;
        private final float scale;

        ChargeFx(Supplier<Vec3> at, int color, int life, float scale) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.at = at;
            this.color = color;
            this.scale = scale;
        }

        @Override
        protected void tick() {
            if (VfxClient.level != null) {
                VfxClient.converge(this.at.get(), 3, 1.2 * (double)this.scale, this.color);
                if (this.age == this.life) {
                    VfxEffects.add(new VfxEffects.Flash(this.at, 0.4F * this.scale, 1.4F * this.scale, this.color, 6));
                }
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float pulse = 1.0F + 0.12F * Mth.sin(age * 1.7F);
            VfxDraw.billboard(m, vc, cam, this.at.get(), (0.15F + 0.55F * t) * this.scale * pulse, age * 0.2F, VfxDraw.argb(0.9F, this.color));
            VfxDraw.billboard(m, vc, cam, this.at.get(), (0.06F + 0.22F * t) * this.scale, 0.0F, VfxDraw.argb(1.0F, 16777215));
        }
    }

    private static final class PurpleChargeFx extends VfxEffects.Fx {
        private final Entity caster;

        PurpleChargeFx(Entity caster, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.GLOW));
            this.caster = caster;
        }

        private Vec3[] points(float t) {
            float pt = Minecraft.getInstance().getFrameTime();
            Vec3 look = this.caster.getViewVector(pt);
            Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
            right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
            Vec3 front = this.caster.getEyePosition(pt).add(look.scale(1.3)).add(0.0, -0.2, 0.0);
            float k = 1.0F - VfxEffects.easeOut(Math.min(1.0F, t * 1.15F));
            return new Vec3[]{front.add(right.scale(-1.1 * (double)k)), front.add(right.scale(1.1 * (double)k)), front};
        }

        @Override
        protected void tick() {
            if (VfxClient.level != null && !this.caster.isRemoved()) {
                Vec3[] p = this.points((float)this.age / (float)this.life);
                VfxClient.converge(p[0], 2, 0.8, 4885759);
                VfxClient.converge(p[1], 2, 0.8, 16722462);
                if (this.age == this.life) {
                    VfxEffects.add(new VfxEffects.Flash(() -> p[2], 0.5F, 3.5F, 14199039, 10));
                    VfxEffects.add(new VfxEffects.Flash(() -> p[2], 0.3F, 1.8F, 16777215, 6));
                }
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            if (!this.caster.isRemoved()) {
                Vec3[] p = this.points(t);
                float s = 0.35F + 0.25F * t;
                float merge = Mth.clamp((t - 0.75F) / 0.25F, 0.0F, 1.0F);
                VfxDraw.billboard(m, vc, cam, p[0], s, age * 0.3F, VfxDraw.argb(1.0F - merge * 0.6F, 4885759));
                VfxDraw.billboard(m, vc, cam, p[1], s, -age * 0.3F, VfxDraw.argb(1.0F - merge * 0.6F, 16722462));
                VfxDraw.billboard(m, vc, cam, p[0], s * 0.4F, 0.0F, VfxDraw.argb(1.0F, 12574975));
                VfxDraw.billboard(m, vc, cam, p[1], s * 0.4F, 0.0F, VfxDraw.argb(1.0F, 16756896));
                if (merge > 0.0F) {
                    VfxDraw.billboard(m, vc, cam, p[2], 0.4F + merge * 0.9F, age * 0.1F, VfxDraw.argb(merge, 10112255));
                }
            }
        }
    }

    private static final class TwistFx extends VfxEffects.Fx {
        private final Supplier<Vec3> base;
        private final float height;

        TwistFx(Supplier<Vec3> base, float height, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.base = base;
            this.height = height;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3 b = this.base.get();
            int c = VfxDraw.argb(VfxEffects.fade(t, 0.05F, 0.5F), 11894015);

            for (int k = 0; k < 3; k++) {
                Vec3 prev = null;

                for (int i = 0; i <= 18; i++) {
                    float f = (float)i / 18.0F;
                    double a = (double)((float)k * (float) (Math.PI * 2) / 3.0F + f * 9.0F) + (double)age * 0.5;
                    double r = 0.7 * (1.0 - (double)f * 0.5);
                    Vec3 p = b.add(Math.cos(a) * r, (double)(f * this.height) * 1.1 * Math.min(1.0, (double)t * 2.5), Math.sin(a) * r);
                    if (prev != null) {
                        VfxDraw.beam(m, vc, prev, p, 0.07F, 0.07F, c, c, eye);
                    }

                    prev = p;
                }
            }
        }
    }
}
