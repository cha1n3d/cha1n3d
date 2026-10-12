package com.fallenroses.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VfxEffects {
    private static final List<VfxEffects.Fx> ACTIVE = new ArrayList<>();
    private static final List<VfxEffects.Fx> PENDING = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static boolean ticking;

    public static <T extends VfxEffects.Fx> T add(T fx) {
        if (ACTIVE.size() + PENDING.size() >= 600) {
            return fx;
        } else {
            if (ticking) {
                PENDING.add(fx);
            } else {
                ACTIVE.add(fx);
            }

            return fx;
        }
    }

    public static void tick() {
        ticking = true;

        try {
            ACTIVE.removeIf(fx -> {
                fx.age++;
                if (fx.age >= 0) {
                    fx.tick();
                }

                return fx.age > fx.life;
            });
        } finally {
            ticking = false;
        }

        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
    }

    public static void clear() {
        ACTIVE.clear();
        PENDING.clear();
    }

    public static void render(PoseStack poseStack, Camera camera, float partial) {
        if (!ACTIVE.isEmpty()) {
            Vec3 cam = camera.getPosition();
            BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            Matrix4f m = poseStack.last().pose();

            for (VfxEffects.Fx fx : ACTIVE) {
                if (fx.age >= 0) {
                    float age = (float)fx.age + partial;
                    float t = Mth.clamp(age / (float)fx.life, 0.0F, 1.0F);
                    fx.draw(m, buffers.getBuffer(fx.type), camera, cam, t, age);
                }
            }

            poseStack.popPose();
            buffers.endBatch();
        }
    }

    public static float fade(float t, float in, float out) {
        float a = in <= 0.0F ? 1.0F : Mth.clamp(t / in, 0.0F, 1.0F);
        float b = out <= 0.0F ? 1.0F : Mth.clamp((1.0F - t) / out, 0.0F, 1.0F);
        return Math.min(a, b);
    }

    public static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    public static int scaleAlpha(int argb, float k) {
        int a = (int)((float)(argb >>> 24 & 0xFF) * Mth.clamp(k, 0.0F, 1.0F));
        return a << 24 | argb & 16777215;
    }

    public static RandomSource random() {
        return RANDOM;
    }

    private VfxEffects() {
    }

    public static class Beam extends VfxEffects.Fx {
        final Supplier<Vec3> from;
        final Supplier<Vec3> to;
        final float width;
        final int color;

        public Beam(Supplier<Vec3> from, Supplier<Vec3> to, float width, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.BEAM));
            this.from = from;
            this.to = to;
            this.width = width;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            int c = VfxDraw.argb(VfxEffects.fade(t, 0.05F, 0.6F), this.color);
            VfxDraw.beam(m, vc, this.from.get(), this.to.get(), this.width * (1.0F - t * 0.6F), this.width * (1.0F - t * 0.6F), c, c, eye);
        }
    }

    public static class Bolt extends VfxEffects.Fx {
        final Vec3 from;
        final Vec3 to;
        final float width;
        final float jitter;
        final int color;
        final int segments;
        List<Vec3> points = new ArrayList<>();

        public Bolt(Vec3 from, Vec3 to, float width, float jitter, int segments, int color, int life, boolean additive) {
            super(life, additive ? FRRenderTypes.additive(FRRenderTypes.BEAM) : FRRenderTypes.translucent(FRRenderTypes.BEAM));
            this.from = from;
            this.to = to;
            this.width = width;
            this.jitter = jitter;
            this.segments = segments;
            this.color = color;
            this.rebuild();
        }

        private void rebuild() {
            this.points = new ArrayList<>();
            Vec3 d = this.to.subtract(this.from);

            for (int i = 0; i <= this.segments; i++) {
                float f = (float)i / (float)this.segments;
                Vec3 p = this.from.add(d.scale((double)f));
                if (i > 0 && i < this.segments) {
                    float k = this.jitter * Mth.sin(f * (float) Math.PI);
                    p = p.add(
                        VfxEffects.RANDOM.nextGaussian() * (double)k,
                        VfxEffects.RANDOM.nextGaussian() * (double)k,
                        VfxEffects.RANDOM.nextGaussian() * (double)k
                    );
                }

                this.points.add(p);
            }
        }

        @Override
        protected void tick() {
            if (this.age % 2 == 0) {
                this.rebuild();
            }
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            VfxDraw.bolt(m, vc, this.points, this.width, VfxDraw.argb(VfxEffects.fade(t, 0.0F, 0.5F), this.color), eye);
        }
    }

    public static class Decal extends VfxEffects.Fx {
        final Supplier<Vec3> center;
        final Vec3 normal;
        final float s0;
        final float s1;
        final float spin;
        final int color;

        public Decal(Supplier<Vec3> center, Vec3 normal, float s0, float s1, float spin, int color, int life, ResourceLocation tex, boolean additive) {
            super(life, additive ? FRRenderTypes.additive(tex) : FRRenderTypes.translucent(tex));
            this.center = center;
            this.normal = normal;
            this.s0 = s0;
            this.s1 = s1;
            this.spin = spin;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            Vec3[] ab = VfxDraw.basis(this.normal);
            float rot = age * this.spin;
            Vec3 ax = ab[0].scale(Math.cos((double)rot)).add(ab[1].scale(Math.sin((double)rot)));
            Vec3 ay = ab[0].scale(-Math.sin((double)rot)).add(ab[1].scale(Math.cos((double)rot)));
            float s = Mth.lerp(VfxEffects.easeOut(Math.min(1.0F, t * 3.0F)), this.s0, this.s1);
            VfxDraw.plane(m, vc, this.center.get(), ax, ay, s, VfxDraw.argb(VfxEffects.fade(t, 0.1F, 0.4F), this.color));
        }
    }

    public static class Flash extends VfxEffects.Fx {
        final Supplier<Vec3> pos;
        final float s0;
        final float s1;
        final int color;
        final float spin;
        float alpha = 1.0F;
        float fadeIn = 0.05F;
        float fadeOut = 0.7F;

        public Flash(Supplier<Vec3> pos, float s0, float s1, int color, int life) {
            this(pos, s0, s1, color, life, FRRenderTypes.GLOW, 0.0F);
        }

        public Flash(Supplier<Vec3> pos, float s0, float s1, int color, int life, ResourceLocation tex, float spin) {
            super(life, FRRenderTypes.additive(tex));
            this.pos = pos;
            this.s0 = s0;
            this.s1 = s1;
            this.color = color;
            this.spin = spin;
        }

        public VfxEffects.Flash fades(float in, float out) {
            this.fadeIn = in;
            this.fadeOut = out;
            return this;
        }

        public VfxEffects.Flash alpha(float a) {
            this.alpha = a;
            return this;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float s = Mth.lerp(VfxEffects.easeOut(t), this.s0, this.s1);
            VfxDraw.billboard(
                m, vc, cam, this.pos.get(), s, age * this.spin, VfxDraw.argb(this.alpha * VfxEffects.fade(t, this.fadeIn, this.fadeOut), this.color)
            );
        }
    }

    public abstract static class Fx {
        int age;
        final int life;
        final RenderType type;

        protected Fx(int life, RenderType type) {
            this.life = Math.max(1, life);
            this.type = type;
        }

        public <T extends VfxEffects.Fx> T delay(int ticks) {
            this.age = -ticks;
            return (T)this;
        }

        public int age() {
            return this.age;
        }

        protected void tick() {
        }

        protected abstract void draw(Matrix4f var1, VertexConsumer var2, Camera var3, Vec3 var4, float var5, float var6);
    }

    public static class Ring extends VfxEffects.Fx {
        final Supplier<Vec3> center;
        final Vec3 normal;
        final float r0;
        final float r1;
        final float width;
        final int color;
        float alpha = 1.0F;

        public Ring(Supplier<Vec3> center, Vec3 normal, float r0, float r1, float width, int color, int life, ResourceLocation tex) {
            super(life, FRRenderTypes.additive(tex));
            this.center = center;
            this.normal = normal;
            this.r0 = r0;
            this.r1 = r1;
            this.width = width;
            this.color = color;
        }

        public VfxEffects.Ring alpha(float a) {
            this.alpha = a;
            return this;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float r = Mth.lerp(VfxEffects.easeOut(t), this.r0, this.r1);
            float a = this.alpha * VfxEffects.fade(t, 0.05F, 0.7F);
            VfxDraw.ring(m, vc, this.center.get(), this.normal, r, this.width * (1.0F - t * 0.5F), VfxDraw.argb(a, this.color), 56);
        }
    }

    public static class Slash extends VfxEffects.Fx {
        final Vec3 center;
        final Vec3 a;
        final Vec3 b;
        final float radius;
        final float width;
        final float arc0;
        final float arc1;
        final int color;

        public Slash(Vec3 center, Vec3 a, Vec3 b, float radius, float width, float arc0, float arc1, int color, int life) {
            super(life, FRRenderTypes.additive(FRRenderTypes.SLASH));
            this.center = center;
            this.a = a;
            this.b = b;
            this.radius = radius;
            this.width = width;
            this.arc0 = arc0;
            this.arc1 = arc1;
            this.color = color;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float reveal = Mth.clamp(t / 0.3F, 0.0F, 1.0F);
            float end = Mth.lerp(VfxEffects.easeOut(reveal), this.arc0, this.arc1);
            float alpha = VfxEffects.fade(t, 0.0F, 0.6F);
            VfxDraw.crescent(m, vc, this.center, this.a, this.b, this.radius, this.width, this.arc0, end, VfxDraw.argb(alpha, this.color), 20);
        }
    }

    public static class Sphere extends VfxEffects.Fx {
        final Supplier<Vec3> center;
        final float r0;
        final float r1;
        final int core;
        final int rim;
        final float rimPower;
        float alpha = 1.0F;
        float fadeIn = 0.1F;
        float fadeOut = 0.4F;

        public Sphere(Supplier<Vec3> center, float r0, float r1, int core, int rim, int life, boolean additive, float rimPower) {
            super(life, additive ? FRRenderTypes.additive(FRRenderTypes.WHITE) : FRRenderTypes.translucent(FRRenderTypes.WHITE));
            this.center = center;
            this.r0 = r0;
            this.r1 = r1;
            this.core = core;
            this.rim = rim;
            this.rimPower = rimPower;
        }

        public VfxEffects.Sphere fades(float in, float out) {
            this.fadeIn = in;
            this.fadeOut = out;
            return this;
        }

        public VfxEffects.Sphere alpha(float a) {
            this.alpha = a;
            return this;
        }

        @Override
        protected void draw(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 eye, float t, float age) {
            float r = Mth.lerp(VfxEffects.easeOut(t), this.r0, this.r1);
            float a = this.alpha * VfxEffects.fade(t, this.fadeIn, this.fadeOut);
            int c = VfxEffects.scaleAlpha(this.core, a);
            int e = VfxEffects.scaleAlpha(this.rim, a);
            VfxDraw.sphere(m, vc, this.center.get(), r, c, e, eye, 14, 22, this.rimPower);
        }
    }
}
