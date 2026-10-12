package com.fallenroses.client.render;

import com.fallenroses.FallenRoses;
import com.fallenroses.client.vfx.FRRenderTypes;
import com.fallenroses.client.vfx.VfxDraw;
import com.fallenroses.client.vfx.VfxEffects;
import com.fallenroses.entity.DomainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Domain expansion: a black barrier that rises out of the ground like ink and seals overhead, cursed energy
 * flowing through it; inside, the domain's own sky and horizon. When the domain ends the barrier cracks and
 * the shards are thrown off (the shards are a client effect sent by the server, see VfxKind.DOMAIN_BREAK).
 *
 * Every non-fixed render type shares one BufferBuilder and switching types flushes the previous batch,
 * so each layer fetches its buffer right before drawing.
 */
public class DomainRenderer extends EntityRenderer<DomainEntity> {
    public static final int FORM_TICKS = 20;
    private static final int LON = 56;
    private static final int LAT = 28;
    private static final Vec3 C = Vec3.ZERO;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final String[] PANO = {null, "shrine", "flames", "flesh", "sea", "cemetery", "pachinko", "court", "film", "station"};

    /** Per type: sky top, sky horizon, sky below the horizon (barrier colours are DomainEntity.EDGE). */
    private static final int[][] SKY = {
        {5, 393226, 0},
        {1704452, 10120720, 2097152},
        {1705474, 11812876, 2754560},
        {1708588, 6961786, 656912},
        {2781360, 11066606, 667712},
        {1053714, 5002832, 658442},
        {133638, 801314, 132612},
        {656900, 3942420, 525314},
        {132110, 1450062, 131594},
        {656912, 5910074, 525316}
    };

    public DomainRenderer(Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DomainEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public boolean shouldRender(DomainEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(DomainEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        float age = (float)e.tickCount + partial;
        float life = (float)e.life();
        float form = VfxEffects.easeOut(Mth.clamp(age / FORM_TICKS, 0.0F, 1.0F));
        float close = Mth.clamp((life - age) / 15.0F, 0.0F, 1.0F);
        float shell = Mth.clamp((close - 0.45F) / 0.25F, 0.0F, 1.0F);
        if (form <= 0.0F || shell <= 0.0F) {
            return;
        }

        int type = Mth.clamp(e.domainType(), 0, SKY.length - 1);
        int[] pal = SKY[type];
        int edge = DomainEntity.EDGE[type];
        float r = e.radius();
        Matrix4f m = ps.last().pose();
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(e.getPosition(partial));
        boolean inside = eye.length() < (double)r;
        double reach = form * Math.PI / 2;
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.15F);
        float crack = 1.0F - close;

        // the barrier itself: near-opaque black with a faint moving sheen
        int black = VfxDraw.argb(0.98F * shell, 1579040);
        VfxDraw.sphereBand(
            m, buf.getBuffer(FRRenderTypes.shell(FRRenderTypes.BARRIER)), C, r, -reach, reach, LON, LAT, age * 0.0015F, age * 0.004F, 6.0F, 3.0F, (x, y, z) -> black
        );

        if (inside) {
            this.interior(e, type, pal, m, buf, eye, r, age, shell * Mth.clamp((age - 16.0F) / 10.0F, 0.0F, 1.0F), partial);
        }

        // cursed energy running through the barrier
        int veins = VfxDraw.argb((inside ? 0.18F : 0.42F) * shell * pulse, edge);
        float skin = inside ? 0.985F : 1.0F;
        VfxDraw.sphereBand(
            m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.VEINS)), C, r * 1.002F * skin, -reach, reach, LON, LAT, -age * 0.003F, age * 0.006F, 5.0F, 2.5F, (x, y, z) -> veins
        );
        VfxDraw.sphereBand(
            m,
            buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)),
            C,
            r * 1.004F * skin,
            -reach,
            reach,
            LON,
            LAT,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            VfxDraw.fresnel(C, r, eye, 0, VfxDraw.argb((inside ? 0.35F : 0.6F) * shell, edge), inside ? 4.0F : 3.0F)
        );

        // the rising edge of the ink while the barrier forms
        if (form < 1.0F) {
            float seamA = 1.0F - form;
            float sy = (float)Math.sin(reach) * r;
            float sr = (float)Math.cos(reach) * r;

            for (int k = -1; k <= 1; k += 2) {
                Vec3 at = new Vec3(0.0, (double)(k * sy), 0.0);
                VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)), at, UP, sr + 0.6F, 1.6F, VfxDraw.argb(0.9F * seamA, edge), 96);
                VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)), at, UP, sr + 0.25F, 0.5F, VfxDraw.argb(seamA, 16777215), 96);
            }
        }

        // ink spilt around the base and a glowing seam where the barrier meets the ground
        VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), new Vec3(0.0, 0.04, 0.0), UP, r * 1.12F, r * 0.13F, VfxDraw.argb(0.8F * shell, 0), 96);
        VfxDraw.ring(
            m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)), new Vec3(0.0, 0.06, 0.0), UP, r + 0.5F, 1.0F, VfxDraw.argb(0.65F * shell * pulse, edge), 96
        );

        // cracks spreading before the barrier breaks
        if (crack > 0.0F) {
            int cc = VfxDraw.argb(Mth.clamp(crack * 2.2F, 0.0F, 1.0F), 16777215);
            VfxDraw.sphereBand(
                m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.CRACKS)), C, r * 1.006F * skin, -reach, reach, LON, LAT, 0.0F, 0.0F, 7.0F, 3.5F, (x, y, z) -> cc
            );
            VfxDraw.sphereBand(
                m,
                buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)),
                C,
                r * 1.008F * skin,
                -reach,
                reach,
                LON,
                LAT,
                0.0F,
                0.0F,
                1.0F,
                1.0F,
                VfxDraw.fresnel(C, r, eye, VfxDraw.argb(crack * 0.25F, edge), VfxDraw.argb(crack * 0.8F, 16777215), 2.0F)
            );
        }
    }

    private void interior(DomainEntity e, int type, int[] pal, Matrix4f m, MultiBufferSource buf, Vec3 eye, float r, float age, float env, float partial) {
        if (env <= 0.0F) {
            return;
        }

        // the domain's sky
        int top = VfxDraw.argb(env, pal[0]);
        int hor = VfxDraw.argb(env, pal[1]);
        int low = VfxDraw.argb(env, pal[2]);
        VfxDraw.sphereBand(
            m,
            buf.getBuffer(FRRenderTypes.shell(FRRenderTypes.WHITE)),
            C,
            r * 0.99F,
            -Math.PI / 2,
            Math.PI / 2,
            LON,
            LAT,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            VfxDraw.gradient(top, hor, low)
        );
        float yaw = e.getYRot();

        switch (type) {
            case 0: {
                int stars = VfxDraw.argb(env, 16777215);
                VfxDraw.sphereBand(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.STARS)),
                    C,
                    r * 0.98F,
                    -Math.PI / 2,
                    Math.PI / 2,
                    LON,
                    LAT,
                    age * 0.0008F,
                    0.0F,
                    2.0F,
                    1.0F,
                    (x, y, z) -> stars
                );
                // the endless flow of information: tilted streams of light around the victim's head
                for (int i = 0; i < 5; i++) {
                    double a = (double)i * 1.3 + (double)age * 0.004 * (i % 2 == 0 ? 1 : -1);
                    Vec3 n = new Vec3(Math.cos(a) * 0.5, 1.0, Math.sin(a) * 0.5).normalize();
                    VfxDraw.ring(
                        m,
                        buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)),
                        new Vec3(0.0, (double)r * 0.3, 0.0),
                        n,
                        r * (0.55F + (float)i * 0.08F),
                        0.35F,
                        VfxDraw.argb(env * 0.35F, i % 2 == 0 ? 15266047 : 12572927),
                        96
                    );
                }

                VfxDraw.billboard(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW)),
                    this.entityRenderDispatcher.camera,
                    behind(yaw, r * 0.6F, r * 0.3F),
                    r * 0.22F,
                    age * 0.01F,
                    VfxDraw.argb(env * 0.6F, 15266047)
                );
                break;
            }
            case 2:
                VfxDraw.ring(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.SHOCK)),
                    new Vec3(0.0, 0.1, 0.0),
                    UP,
                    r * 0.92F,
                    r * 0.35F,
                    VfxDraw.argb(0.4F * env * (0.8F + 0.2F * Mth.sin(age * 0.3F)), 16730632),
                    96
                );
                break;
            case 4:
                VfxDraw.billboard(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW)),
                    this.entityRenderDispatcher.camera,
                    behind(yaw, r * 0.62F, r * 0.26F),
                    r * 0.18F,
                    0.0F,
                    VfxDraw.argb(0.8F * env, 16773296)
                );
                break;
            case 8:
                VfxDraw.billboard(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW)),
                    this.entityRenderDispatcher.camera,
                    behind(yaw + 35.0F, r * 0.55F, r * 0.42F),
                    r * 0.2F,
                    0.0F,
                    VfxDraw.argb(0.85F * env, 15266047)
                );
                break;
            case 6:
                VfxDraw.ring(
                    m,
                    buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)),
                    new Vec3(0.0, 0.08, 0.0),
                    UP,
                    r * 0.6F,
                    0.6F,
                    VfxDraw.argb(env * (0.5F + 0.5F * Mth.sin(age * 0.6F)), (int)(age / 4.0F) % 2 == 0 ? 6987898 : 16769088),
                    96
                );
        }

        String pano = PANO[type];
        if (pano != null) {
            float pr = r * 0.93F;
            float y1 = r * 0.42F;
            VfxDraw.cylinder(m, buf.getBuffer(FRRenderTypes.translucent(panorama(pano, false))), C, pr, -0.8F, y1, 72, yaw, VfxDraw.argb(env, 16777215));
            float flicker = type == 6 ? 0.75F + 0.25F * Mth.sin(age * 0.9F) : 0.9F + 0.1F * Mth.sin(age * 0.2F);
            VfxDraw.cylinder(
                m, buf.getBuffer(FRRenderTypes.additive(panorama(pano, true))), C, pr * 0.995F, -0.8F, y1, 72, yaw, VfxDraw.argb(env * flicker, 16777215)
            );
        }
    }

    /** A point on the horizon behind a caster facing yaw. */
    private static Vec3 behind(float yawDeg, float dist, float height) {
        double a = Math.toRadians((double)yawDeg);
        return new Vec3(Math.sin(a) * (double)dist, (double)height, -Math.cos(a) * (double)dist);
    }

    private static ResourceLocation panorama(String name, boolean glow) {
        return FallenRoses.id("textures/domain/pano_" + name + (glow ? "_glow" : "") + ".png");
    }
}
