package com.fallenroses.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VfxDraw {
    public static int argb(float alpha, int rgb) {
        return Mth.clamp((int)(alpha * 255.0F), 0, 255) << 24 | rgb & 16777215;
    }

    private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, int argb, float u, float v) {
        vc.vertex(m, (float)x, (float)y, (float)z).color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24 & 0xFF).uv(u, v).endVertex();
    }

    public static void billboard(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 p, float size, float rotation, int argb) {
        Quaternionf q = new Quaternionf(cam.rotation()).rotateZ(rotation);
        Vector3f a = new Vector3f(-size, -size, 0.0F).rotate(q);
        Vector3f b = new Vector3f(-size, size, 0.0F).rotate(q);
        Vector3f c = new Vector3f(size, size, 0.0F).rotate(q);
        Vector3f d = new Vector3f(size, -size, 0.0F).rotate(q);
        v(vc, m, p.x + (double)a.x, p.y + (double)a.y, p.z + (double)a.z, argb, 0.0F, 1.0F);
        v(vc, m, p.x + (double)b.x, p.y + (double)b.y, p.z + (double)b.z, argb, 0.0F, 0.0F);
        v(vc, m, p.x + (double)c.x, p.y + (double)c.y, p.z + (double)c.z, argb, 1.0F, 0.0F);
        v(vc, m, p.x + (double)d.x, p.y + (double)d.y, p.z + (double)d.z, argb, 1.0F, 1.0F);
    }

    public static void plane(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 ax, Vec3 ay, float size, int argb) {
        Vec3 x = ax.scale((double)size);
        Vec3 y = ay.scale((double)size);
        v(vc, m, c.x - x.x - y.x, c.y - x.y - y.y, c.z - x.z - y.z, argb, 0.0F, 1.0F);
        v(vc, m, c.x - x.x + y.x, c.y - x.y + y.y, c.z - x.z + y.z, argb, 0.0F, 0.0F);
        v(vc, m, c.x + x.x + y.x, c.y + x.y + y.y, c.z + x.z + y.z, argb, 1.0F, 0.0F);
        v(vc, m, c.x + x.x - y.x, c.y + x.y - y.y, c.z + x.z - y.z, argb, 1.0F, 1.0F);
    }

    public static Vec3[] basis(Vec3 normal) {
        Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : normal.normalize();
        Vec3 helper = Math.abs(n.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 a = n.cross(helper).normalize();
        Vec3 b = n.cross(a).normalize();
        return new Vec3[]{a, b};
    }

    public static void ring(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 normal, float r, float w, int argb, int segments) {
        Vec3[] ab = basis(normal);
        float inner = Math.max(0.0F, r - w);

        for (int i = 0; i < segments; i++) {
            double t0 = (double)((float) (Math.PI * 2) * (float)i / (float)segments);
            double t1 = (double)((float) (Math.PI * 2) * (float)(i + 1) / (float)segments);
            Vec3 d0 = ab[0].scale(Math.cos(t0)).add(ab[1].scale(Math.sin(t0)));
            Vec3 d1 = ab[0].scale(Math.cos(t1)).add(ab[1].scale(Math.sin(t1)));
            float u0 = (float)i / (float)segments;
            float u1 = (float)(i + 1) / (float)segments;
            v(vc, m, c.x + d0.x * (double)inner, c.y + d0.y * (double)inner, c.z + d0.z * (double)inner, argb, u0, 0.0F);
            v(vc, m, c.x + d0.x * (double)r, c.y + d0.y * (double)r, c.z + d0.z * (double)r, argb, u0, 1.0F);
            v(vc, m, c.x + d1.x * (double)r, c.y + d1.y * (double)r, c.z + d1.z * (double)r, argb, u1, 1.0F);
            v(vc, m, c.x + d1.x * (double)inner, c.y + d1.y * (double)inner, c.z + d1.z * (double)inner, argb, u1, 0.0F);
        }
    }

    public static void sphere(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int core, int rim, Vec3 eye, int lat, int lon, float rimPower) {
        for (int i = 0; i < lat; i++) {
            double th0 = Math.PI * (double)i / (double)lat;
            double th1 = Math.PI * (double)(i + 1) / (double)lat;

            for (int j = 0; j < lon; j++) {
                double ph0 = (double)((float) (Math.PI * 2) * (float)j / (float)lon);
                double ph1 = (double)((float) (Math.PI * 2) * (float)(j + 1) / (float)lon);
                sv(vc, m, c, r, th0, ph0, core, rim, eye, rimPower, (float)j / (float)lon, (float)i / (float)lat);
                sv(vc, m, c, r, th1, ph0, core, rim, eye, rimPower, (float)j / (float)lon, (float)(i + 1) / (float)lat);
                sv(vc, m, c, r, th1, ph1, core, rim, eye, rimPower, (float)(j + 1) / (float)lon, (float)(i + 1) / (float)lat);
                sv(vc, m, c, r, th0, ph1, core, rim, eye, rimPower, (float)(j + 1) / (float)lon, (float)i / (float)lat);
            }
        }
    }

    private static void sv(VertexConsumer vc, Matrix4f m, Vec3 c, float r, double th, double ph, int core, int rim, Vec3 eye, float rimPower, float u, float vv) {
        double nx = Math.sin(th) * Math.cos(ph);
        double ny = Math.cos(th);
        double nz = Math.sin(th) * Math.sin(ph);
        double px = c.x + nx * (double)r;
        double py = c.y + ny * (double)r;
        double pz = c.z + nz * (double)r;
        double vx = eye.x - px;
        double vy = eye.y - py;
        double vz = eye.z - pz;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz) + 1.0E-6;
        double facing = Math.abs((nx * vx + ny * vy + nz * vz) / vl);
        float k = (float)Math.pow(1.0 - facing, (double)rimPower);
        v(vc, m, px, py, pz, lerpColor(core, rim, k), u, vv);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24 & 0xFF;
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int ba = b >>> 24 & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        return (int)((float)aa + (float)(ba - aa) * t) << 24
            | (int)((float)ar + (float)(br - ar) * t) << 16
            | (int)((float)ag + (float)(bg - ag) * t) << 8
            | (int)((float)ab + (float)(bb - ab) * t);
    }

    public static void crescent(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 a, Vec3 b, float r, float w, float angle0, float angle1, int argb, int segments) {
        for (int i = 0; i < segments; i++) {
            float f0 = (float)i / (float)segments;
            float f1 = (float)(i + 1) / (float)segments;
            float t0 = Mth.lerp(f0, angle0, angle1);
            float t1 = Mth.lerp(f1, angle0, angle1);
            float w0 = w * Mth.sin(f0 * (float) Math.PI);
            float w1 = w * Mth.sin(f1 * (float) Math.PI);
            Vec3 d0 = a.scale(Math.cos((double)t0)).add(b.scale(Math.sin((double)t0)));
            Vec3 d1 = a.scale(Math.cos((double)t1)).add(b.scale(Math.sin((double)t1)));
            v(vc, m, c.x + d0.x * (double)(r - w0), c.y + d0.y * (double)(r - w0), c.z + d0.z * (double)(r - w0), argb, f0, 0.0F);
            v(vc, m, c.x + d0.x * (double)r, c.y + d0.y * (double)r, c.z + d0.z * (double)r, argb, f0, 1.0F);
            v(vc, m, c.x + d1.x * (double)r, c.y + d1.y * (double)r, c.z + d1.z * (double)r, argb, f1, 1.0F);
            v(vc, m, c.x + d1.x * (double)(r - w1), c.y + d1.y * (double)(r - w1), c.z + d1.z * (double)(r - w1), argb, f1, 0.0F);
        }
    }

    public static void beam(Matrix4f m, VertexConsumer vc, Vec3 from, Vec3 to, float w0, float w1, int argb0, int argb1, Vec3 eye) {
        Vec3 dir = to.subtract(from);
        Vec3 mid = from.add(dir.scale(0.5));
        Vec3 side = dir.cross(eye.subtract(mid));
        if (side.lengthSqr() < 1.0E-8) {
            side = new Vec3(0.0, 1.0, 0.0);
        }

        side = side.normalize();
        Vec3 s0 = side.scale((double)w0);
        Vec3 s1 = side.scale((double)w1);
        v(vc, m, from.x - s0.x, from.y - s0.y, from.z - s0.z, argb0, 0.0F, 0.0F);
        v(vc, m, from.x + s0.x, from.y + s0.y, from.z + s0.z, argb0, 0.0F, 1.0F);
        v(vc, m, to.x + s1.x, to.y + s1.y, to.z + s1.z, argb1, 1.0F, 1.0F);
        v(vc, m, to.x - s1.x, to.y - s1.y, to.z - s1.z, argb1, 1.0F, 0.0F);
    }

    /** Per-vertex colour of a sphere from its outward normal. */
    public interface Shade {
        int color(double nx, double ny, double nz);
    }

    /**
     * Sphere band between latitudes lat0..lat1 (radians: -PI/2 bottom, PI/2 top). UVs wrap uRepeat times around and
     * vRepeat times pole to pole, shifted by uOff/vOff, so tiling textures can flow over the surface.
     */
    public static void sphereBand(
        Matrix4f m, VertexConsumer vc, Vec3 c, float r, double lat0, double lat1, int lon, int lat, float uOff, float vOff, float uRepeat, float vRepeat, Shade shade
    ) {
        if (lat1 <= lat0) {
            return;
        }

        for (int i = 0; i < lat; i++) {
            double p0 = lat0 + (lat1 - lat0) * i / lat;
            double p1 = lat0 + (lat1 - lat0) * (i + 1) / lat;

            for (int j = 0; j < lon; j++) {
                double t0 = Math.PI * 2 * j / lon;
                double t1 = Math.PI * 2 * (j + 1) / lon;
                bandVertex(m, vc, c, r, p0, t0, uOff, vOff, uRepeat, vRepeat, shade);
                bandVertex(m, vc, c, r, p1, t0, uOff, vOff, uRepeat, vRepeat, shade);
                bandVertex(m, vc, c, r, p1, t1, uOff, vOff, uRepeat, vRepeat, shade);
                bandVertex(m, vc, c, r, p0, t1, uOff, vOff, uRepeat, vRepeat, shade);
            }
        }
    }

    private static void bandVertex(
        Matrix4f m, VertexConsumer vc, Vec3 c, float r, double phi, double theta, float uOff, float vOff, float uRepeat, float vRepeat, Shade shade
    ) {
        double nx = Math.cos(phi) * Math.cos(theta);
        double ny = Math.sin(phi);
        double nz = Math.cos(phi) * Math.sin(theta);
        float u = (float)(theta / (Math.PI * 2)) * uRepeat + uOff;
        float vv = (float)((Math.PI / 2 - phi) / Math.PI) * vRepeat + vOff;
        v(vc, m, c.x + nx * r, c.y + ny * r, c.z + nz * r, shade.color(nx, ny, nz), u, vv);
    }

    /** Rim-lit shade: core colour facing the eye, rim colour at grazing angles. */
    public static Shade fresnel(Vec3 c, float r, Vec3 eye, int core, int rim, float power) {
        return (nx, ny, nz) -> {
            double vx = eye.x - (c.x + nx * r);
            double vy = eye.y - (c.y + ny * r);
            double vz = eye.z - (c.z + nz * r);
            double vl = Math.sqrt(vx * vx + vy * vy + vz * vz) + 1.0E-6;
            double facing = Math.abs((nx * vx + ny * vy + nz * vz) / vl);
            return lerpColor(core, rim, (float)Math.pow(1.0 - facing, power));
        };
    }

    /** Vertical gradient: horizon colour at ny = 0, blending to top above and to bottom below. */
    public static Shade gradient(int top, int horizon, int bottom) {
        return (nx, ny, nz) -> ny >= 0.0 ? lerpColor(horizon, top, (float)Math.pow(ny, 0.6)) : lerpColor(horizon, bottom, (float)Math.pow(-ny, 0.5));
    }

    /**
     * Textured cylinder wall around c (u = 0.5 faces the yaw direction + 180 degrees, i.e. behind a caster with that yaw;
     * v = 0 at y1, 1 at y0), seen from inside without mirroring.
     */
    public static void cylinder(Matrix4f m, VertexConsumer vc, Vec3 c, float r, float y0, float y1, int segments, float yawDeg, int argb) {
        double base = Math.toRadians(yawDeg) + Math.PI;

        for (int i = 0; i < segments; i++) {
            float u0 = (float)i / segments;
            float u1 = (float)(i + 1) / segments;
            double a0 = base + (u0 - 0.5) * Math.PI * 2;
            double a1 = base + (u1 - 0.5) * Math.PI * 2;
            double x0 = -Math.sin(a0) * r;
            double z0 = Math.cos(a0) * r;
            double x1 = -Math.sin(a1) * r;
            double z1 = Math.cos(a1) * r;
            v(vc, m, c.x + x0, c.y + y0, c.z + z0, argb, u0, 1.0F);
            v(vc, m, c.x + x0, c.y + y1, c.z + z0, argb, u0, 0.0F);
            v(vc, m, c.x + x1, c.y + y1, c.z + z1, argb, u1, 0.0F);
            v(vc, m, c.x + x1, c.y + y0, c.z + z1, argb, u1, 1.0F);
        }
    }

    public static void bolt(Matrix4f m, VertexConsumer vc, List<Vec3> points, float width, int argb, Vec3 eye) {
        int n = points.size();

        for (int i = 0; i < n - 1; i++) {
            float k0 = Mth.sin((float)i / (float)(n - 1) * (float) Math.PI) * 0.8F + 0.2F;
            float k1 = Mth.sin((float)(i + 1) / (float)(n - 1) * (float) Math.PI) * 0.8F + 0.2F;
            beam(m, vc, points.get(i), points.get(i + 1), width * k0, width * k1, argb, argb, eye);
        }
    }

    private VfxDraw() {
    }
}
