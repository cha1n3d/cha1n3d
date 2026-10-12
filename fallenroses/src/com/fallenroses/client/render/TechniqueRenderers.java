package com.fallenroses.client.render;

import com.fallenroses.client.vfx.FRRenderTypes;
import com.fallenroses.client.vfx.VfxDraw;
import com.fallenroses.client.vfx.VfxEffects;
import com.fallenroses.entity.BlueOrbEntity;
import com.fallenroses.entity.CurseOrbEntity;
import com.fallenroses.entity.DomainEntity;
import com.fallenroses.entity.FugaArrowEntity;
import com.fallenroses.entity.HollowPurpleEntity;
import com.fallenroses.entity.RedBlastEntity;
import com.fallenroses.entity.ShotEntity;
import com.fallenroses.entity.SlashEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TechniqueRenderers {
    private TechniqueRenderers() {
    }

    abstract static class Base<T extends Entity> extends EntityRenderer<T> {
        Base(Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return TextureAtlas.LOCATION_BLOCKS;
        }

        @Override
        public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
            return true;
        }

        Camera cam() {
            return this.entityRenderDispatcher.camera;
        }

        Vec3 eye(T e, float partial) {
            return this.cam().getPosition().subtract(e.getPosition(partial));
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float age = (float)e.tickCount + partial;
            this.draw(e, ps.last().pose(), buf, this.eye(e, partial), age, partial);
        }

        abstract void draw(T var1, Matrix4f var2, MultiBufferSource var3, Vec3 var4, float var5, float var6);
    }

    public static class Blue extends TechniqueRenderers.Base<BlueOrbEntity> {
        public Blue(Context ctx) {
            super(ctx);
        }

        void draw(BlueOrbEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            float grow = Mth.clamp(age / 8.0F, 0.0F, 1.0F) * Mth.clamp((70.0F - age) / 6.0F, 0.0F, 1.0F);
            float pulse = 1.0F + 0.08F * Mth.sin(age * 1.3F);
            Vec3 c = new Vec3(0.0, 0.0, 0.0);
            float r = 0.55F * grow * pulse;
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, r, -268302312, 1073874968, eye, 12, 18, 1.5F);
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, r * 1.15F, 270553343, -8734465, eye, 12, 18, 2.2F);
            VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
            VfxDraw.billboard(m, glow, this.cam(), c, 2.4F * grow, 0.0F, VfxDraw.argb(0.55F, 2777343));
            VfxDraw.billboard(m, glow, this.cam(), c, 0.9F * grow, 0.0F, VfxDraw.argb(0.8F, 10275839));
            VertexConsumer ring = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING));

            for (int i = 0; i < 3; i++) {
                double a = (double)age * (0.09 + (double)i * 0.04) + (double)i * 2.1;
                Vec3 n = new Vec3(Math.cos(a), 0.6 + 0.3 * (double)i, Math.sin(a)).normalize();
                VfxDraw.ring(m, ring, c, n, (0.9F + (float)i * 0.35F) * grow, 0.12F, VfxDraw.argb(0.7F, i == 1 ? 16777215 : 4885759), 40);
            }

            VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.SHOCK)), c, eye, 3.2F * grow, 1.6F, VfxDraw.argb(0.18F, 6987007), 40);
        }
    }

    public static class Domain extends TechniqueRenderers.Base<DomainEntity> {
        public Domain(Context ctx) {
            super(ctx);
        }

        static int edgeColor(int type) {
            return switch (type) {
                case 0 -> 15266047;
                default -> 16719920;
                case 2 -> 16738832;
                case 3 -> 8044728;
                case 4 -> 9099519;
                case 5 -> 10133600;
                case 6 -> 7004282;
                case 7 -> 15259808;
                case 8 -> 8050920;
                case 9 -> 16747072;
            };
        }

        void draw(DomainEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            float open = e.openness(partial);
            if (!(open <= 0.0F)) {
                float r = e.radius() * VfxEffects.easeOut(open);
                Vec3 c = Vec3.ZERO;
                boolean inside = eye.length() < (double)r;
                int type = e.domainType();
                int edge = edgeColor(type);
                VertexConsumer trans = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE));
                VertexConsumer add = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE));
                VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
                if (!inside) {
                    VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.985F * open, 0), VfxDraw.argb(0.97F * open, 328968), eye, 18, 28, 1.0F);
                    VfxDraw.sphere(m, add, c, r * 1.01F, 0, VfxDraw.argb(0.55F * open, edge), eye, 18, 28, 4.0F);
                    VfxDraw.ring(
                        m,
                        buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)),
                        new Vec3(0.0, 0.05, 0.0),
                        new Vec3(0.0, 1.0, 0.0),
                        r,
                        0.8F,
                        VfxDraw.argb(0.5F * open, edge),
                        72
                    );
                } else {
                    switch (type) {
                        case 0:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.93F * open, 5), VfxDraw.argb(0.93F * open, 5), eye, 18, 28, 1.0F);
                            VfxDraw.sphere(
                                m,
                                buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.STARS)),
                                c,
                                r * 0.98F,
                                VfxDraw.argb(open, 16777215),
                                VfxDraw.argb(open, 12572927),
                                eye,
                                18,
                                28,
                                1.0F
                            );
                            break;
                        case 1:
                        default:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.6F * open, 2752516), VfxDraw.argb(0.85F * open, 4194312), eye, 18, 28, 1.4F);
                            break;
                        case 2:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.8F * open, 2099202), VfxDraw.argb(0.95F * open, 5904900), eye, 18, 28, 1.3F);
                            VfxDraw.sphere(m, add, c, r * 0.99F, 0, VfxDraw.argb(0.9F * open, 16738832), eye, 18, 28, 2.6F);
                            VfxDraw.ring(
                                m,
                                buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.SHOCK)),
                                new Vec3(0.0, 0.1, 0.0),
                                new Vec3(0.0, 1.0, 0.0),
                                r * 0.9F,
                                r * 0.5F,
                                VfxDraw.argb(0.35F * open * (0.8F + 0.2F * Mth.sin(age * 0.3F)), 16730632),
                                72
                            );
                            break;
                        case 3:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.8F * open, 660500), VfxDraw.argb(0.95F * open, 1717298), eye, 18, 28, 1.2F);

                            for (int i = 0; i < 24; i++) {
                                double a = (double)i * 0.7 + (double)age * 0.01 * (double)(i % 2 == 0 ? 1 : -1);
                                double b = (double)i * 0.37 % 1.2 + 0.15;
                                Vec3 p = new Vec3(Math.cos(a) * Math.cos(b), Math.sin(b), Math.sin(a) * Math.cos(b)).scale((double)r * 0.96);
                                VfxDraw.billboard(m, glow, this.cam(), p, 1.4F, (float)a, VfxDraw.argb(0.35F * open, 12116184));
                            }
                            break;
                        case 4: {
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.75F * open, 5946600), VfxDraw.argb(0.95F * open, 674426), eye, 18, 28, 1.1F);
                            VertexConsumer tr = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING));
                            VfxDraw.ring(
                                m, tr, new Vec3(0.0, 0.08, 0.0), new Vec3(0.0, 1.0, 0.0), r * 0.45F, r * 0.45F, VfxDraw.argb(0.55F * open, 15259808), 72
                            );
                            VfxDraw.ring(
                                m,
                                tr,
                                new Vec3(0.0, 0.1, 0.0),
                                new Vec3(0.0, 1.0, 0.0),
                                r * 0.92F,
                                r * 0.42F,
                                VfxDraw.argb(0.5F * open * (0.85F + 0.15F * Mth.sin(age * 0.12F)), 1735360),
                                72
                            );
                            VfxDraw.billboard(
                                m, glow, this.cam(), new Vec3(0.0, (double)r * 0.55, (double)(-r) * 0.7), r * 0.35F, 0.0F, VfxDraw.argb(0.55F * open, 16773296)
                            );
                            break;
                        }
                        case 5:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.85F * open, 1316366), VfxDraw.argb(0.97F * open, 2764314), eye, 18, 28, 1.2F);
                            VfxDraw.ring(
                                m,
                                buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING)),
                                new Vec3(0.0, 0.08, 0.0),
                                new Vec3(0.0, 1.0, 0.0),
                                r * 0.9F,
                                r * 0.6F,
                                VfxDraw.argb(0.5F * open, 3815976),
                                72
                            );

                            for (int i = 0; i < 18; i++) {
                                double a = (double)i * 1.1;
                                double d = (double)r * (0.25 + (double)i * 0.37 % 0.65);
                                Vec3 p = new Vec3(Math.cos(a) * d, 1.2 + 0.3 * Math.sin((double)age * 0.1 + (double)i), Math.sin(a) * d);
                                VfxDraw.billboard(m, glow, this.cam(), p, 0.5F, 0.0F, VfxDraw.argb(0.5F * open, 13164704));
                            }
                            break;
                        case 6:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.85F * open, 268810), VfxDraw.argb(0.97F * open, 932376), eye, 18, 28, 1.2F);

                            for (int i = 0; i < 40; i++) {
                                double a = (double)((float)i * (float) (Math.PI * 2) / 40.0F) + (double)age * 0.05;
                                int col = (i + (int)(age * 0.5F)) % 3 == 0 ? 16769088 : (i % 2 == 0 ? 7004282 : 16738970);

                                for (int k = 0; k < 3; k++) {
                                    double h = 1.5 + (double)k * 2.5;
                                    double rr = Math.sqrt(Math.max(0.0, (double)(r * r) * 0.9 - h * h));
                                    VfxDraw.billboard(
                                        m,
                                        glow,
                                        this.cam(),
                                        new Vec3(Math.cos(a + (double)k) * rr, h, Math.sin(a + (double)k) * rr),
                                        0.45F,
                                        0.0F,
                                        VfxDraw.argb(0.8F * open, col)
                                    );
                                }
                            }
                            break;
                        case 7: {
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.9F * open, 919558), VfxDraw.argb(0.98F * open, 2759180), eye, 18, 28, 1.2F);
                            VertexConsumer tr = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING));
                            VfxDraw.ring(m, tr, new Vec3(0.0, 0.08, 0.0), new Vec3(0.0, 1.0, 0.0), r * 0.85F, r * 0.85F, VfxDraw.argb(0.6F * open, 3810322), 72);
                            VfxDraw.ring(
                                m,
                                buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)),
                                new Vec3(0.0, 0.1, 0.0),
                                new Vec3(0.0, 1.0, 0.0),
                                r * 0.35F,
                                0.3F,
                                VfxDraw.argb(0.7F * open, 15259808),
                                64
                            );
                            VfxDraw.billboard(m, glow, this.cam(), new Vec3(0.0, (double)r * 0.75, 0.0), 3.0F, 0.0F, VfxDraw.argb(0.6F * open, 16773312));
                            break;
                        }
                        case 8:
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.88F * open, 132110), VfxDraw.argb(0.97F * open, 660528), eye, 18, 28, 1.2F);
                            VfxDraw.billboard(
                                m,
                                glow,
                                this.cam(),
                                new Vec3((double)r * 0.4, (double)r * 0.6, (double)(-r) * 0.5),
                                r * 0.32F,
                                0.0F,
                                VfxDraw.argb(0.75F * open, 15266047)
                            );
                            VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));

                            for (int i = 0; i < 24; i++) {
                                double a = (double)((float)i * (float) (Math.PI * 2) / 24.0F) + (double)age * 0.01;
                                Vec3 p = new Vec3(Math.cos(a) * (double)r * 0.85, 2.2, Math.sin(a) * (double)r * 0.85);
                                VfxDraw.beam(
                                    m, beam, p, p.add(0.0, 1.6, 0.0), 0.6F, 0.6F, VfxDraw.argb(0.18F * open, 8050920), VfxDraw.argb(0.18F * open, 8050920), eye
                                );
                            }
                            break;
                        case 9: {
                            VfxDraw.sphere(m, trans, c, r, VfxDraw.argb(0.8F * open, 2757672), VfxDraw.argb(0.95F * open, 12607530), eye, 18, 28, 1.6F);
                            VertexConsumer tr = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING));
                            VfxDraw.ring(m, tr, new Vec3(0.0, 0.08, 0.0), new Vec3(0.0, 1.0, 0.0), r * 0.5F, r * 0.2F, VfxDraw.argb(0.7F * open, 9077888), 72);
                            VfxDraw.ring(m, tr, new Vec3(0.0, 0.09, 0.0), new Vec3(0.0, 1.0, 0.0), r * 0.31F, 0.25F, VfxDraw.argb(0.8F * open, 15257664), 72);
                            VfxDraw.billboard(
                                m, glow, this.cam(), new Vec3(0.0, (double)r * 0.12, (double)(-r) * 0.85), r * 0.3F, 0.0F, VfxDraw.argb(0.8F * open, 16756832)
                            );
                        }
                    }

                    VfxDraw.sphere(m, add, c, r * 0.995F, 0, VfxDraw.argb(0.7F * open, edge), eye, 18, 28, 3.0F);
                    VfxDraw.ring(
                        m,
                        buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)),
                        new Vec3(0.0, 0.05, 0.0),
                        new Vec3(0.0, 1.0, 0.0),
                        r,
                        0.8F,
                        VfxDraw.argb(0.6F * open, edge),
                        72
                    );
                }
            }
        }

        protected int getBlockLightLevel(DomainEntity entity, BlockPos pos) {
            return 15;
        }
    }

    public static class Fuga extends TechniqueRenderers.Base<FugaArrowEntity> {
        public Fuga(Context ctx) {
            super(ctx);
        }

        void draw(FugaArrowEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            Vec3 dir = e.getDeltaMovement();
            if (!(dir.lengthSqr() < 1.0E-6)) {
                dir = dir.normalize();
                VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                VfxDraw.beam(m, beam, dir.scale(0.6), dir.scale(-3.2), 0.45F, 0.05F, VfxDraw.argb(1.0F, 16747040), VfxDraw.argb(0.2F, 16723984), eye);
                VfxDraw.beam(m, beam, dir.scale(0.5), dir.scale(-1.6), 0.18F, 0.02F, VfxDraw.argb(1.0F, 16773312), VfxDraw.argb(0.3F, 16756800), eye);
                VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
                VfxDraw.billboard(m, glow, this.cam(), Vec3.ZERO, 1.2F, 0.0F, VfxDraw.argb(0.8F, 16740384));
                VfxDraw.billboard(m, glow, this.cam(), Vec3.ZERO, 0.4F, 0.0F, VfxDraw.argb(1.0F, 16777184));
            }
        }
    }

    public static class Orb extends TechniqueRenderers.Base<CurseOrbEntity> {
        public Orb(Context ctx) {
            super(ctx);
        }

        void draw(CurseOrbEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            Vec3 c = Vec3.ZERO;
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.22F, -267911152, -2145386448, eye, 8, 12, 1.5F);
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 0.27F, 0, -6274817, eye, 8, 12, 2.0F);
            VfxDraw.billboard(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW)), this.cam(), c, 0.7F, 0.0F, VfxDraw.argb(0.5F, 6953632));
        }
    }

    public static class Purple extends TechniqueRenderers.Base<HollowPurpleEntity> {
        public Purple(Context ctx) {
            super(ctx);
        }

        void draw(HollowPurpleEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            float r = 2.6F * Mth.clamp(age / 6.0F, 0.0F, 1.0F);
            float flick = 1.0F + 0.05F * Mth.sin(age * 2.7F);
            Vec3 c = new Vec3(0.0, 0.0, 0.0);
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, r * 0.85F, -535559128, 1613760608, eye, 14, 22, 1.4F);
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, r * flick, 815418623, -993025, eye, 14, 22, 2.6F);
            VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
            VfxDraw.billboard(m, glow, this.cam(), c, r * 2.6F, 0.0F, VfxDraw.argb(0.5F, 9059583));
            VertexConsumer ring = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING));

            for (int i = 0; i < 4; i++) {
                double a = (double)age * 0.23 + (double)i * 1.57;
                Vec3 n = new Vec3(Math.cos(a), Math.sin(a * 0.7 + (double)i), Math.sin(a)).normalize();
                VfxDraw.ring(m, ring, c, n, r * (1.15F + 0.1F * (float)i), 0.18F, VfxDraw.argb(0.8F, i % 2 == 0 ? 16777215 : 13144319), 48);
            }
        }
    }

    public static class Red extends TechniqueRenderers.Base<RedBlastEntity> {
        public Red(Context ctx) {
            super(ctx);
        }

        void draw(RedBlastEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            Vec3 c = new Vec3(0.0, 0.0, 0.0);
            VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
            VfxDraw.billboard(m, glow, this.cam(), c, 1.5F, 0.0F, VfxDraw.argb(0.85F, 16722462));
            VfxDraw.billboard(m, glow, this.cam(), c, 0.55F, 0.0F, VfxDraw.argb(1.0F, 16769232));
            VfxDraw.billboard(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.SHOCK)), this.cam(), c, 1.1F, age * 0.5F, VfxDraw.argb(0.8F, 16728112));
            VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 0.4F, 1627349056, -57328, eye, 8, 12, 1.6F);
        }
    }

    public static class Shot extends TechniqueRenderers.Base<ShotEntity> {
        public Shot(Context ctx) {
            super(ctx);
        }

        public void render(ShotEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            if (e.getKind() == ShotEntity.Kind.GAVEL) {
                ps.pushPose();
                ps.translate(0.0, 0.4, 0.0);
                ps.mulPose(Axis.YP.rotationDegrees(-yaw));
                ps.mulPose(Axis.XP.rotationDegrees(((float)e.tickCount + partial) * 40.0F));
                ps.pushPose();
                ps.scale(1.1F, 0.7F, 0.7F);
                ps.translate(-0.5, -0.5, -0.5);
                Minecraft.getInstance()
                    .getBlockRenderer()
                    .renderSingleBlock(Blocks.DARK_OAK_LOG.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
                ps.scale(0.18F, 1.2F, 0.18F);
                ps.translate(-0.5, -1.2, -0.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.GOLD_BLOCK.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }

            if (e.getKind() == ShotEntity.Kind.GRAVESTONE) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees((float)(e.getId() * 47 % 360)));
                ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(((float)e.tickCount + partial) * 0.4F) * 6.0F));
                ps.pushPose();
                ps.scale(0.9F, 1.3F, 0.3F);
                ps.translate(-0.5, 0.0, -0.5);
                Minecraft.getInstance()
                    .getBlockRenderer()
                    .renderSingleBlock(Blocks.STONE_BRICKS.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
                ps.translate(0.0, 1.3, 0.0);
                ps.scale(1.0F, 0.25F, 0.4F);
                ps.translate(-0.5, 0.0, -0.5);
                Minecraft.getInstance()
                    .getBlockRenderer()
                    .renderSingleBlock(Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }

            super.render(e, yaw, partial, ps, buf, light);
        }

        void draw(ShotEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            Vec3 c = Vec3.ZERO;
            Vec3 vel = e.getDeltaMovement();
            Vec3 dir = vel.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : vel.normalize();
            VertexConsumer glow = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.GLOW));
            switch (e.getKind()) {
                case BLOOD_DISC:
                    Vec3 up = new Vec3(0.0, 1.0, 0.0);
                    VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)), c, up, 0.75F, 0.35F, VfxDraw.argb(0.95F, 13635632), 32);
                    VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.RING)), c, up, 0.85F, 0.08F, VfxDraw.argb(1.0F, 16769256), 32);
                    VfxDraw.ring(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING)), c, up, 0.5F, 0.3F, VfxDraw.argb(0.8F, 3801096), 32);
                    VfxDraw.billboard(m, glow, this.cam(), c, 1.2F, 0.0F, VfxDraw.argb(0.4F, 13635632));
                    break;
                case BLOOD_ORB:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.28F, -265289720, -2141192176, eye, 8, 12, 1.4F);
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 0.32F, 0, -53184, eye, 8, 12, 2.0F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 1.1F, 0.0F, VfxDraw.argb(0.6F, 13635632));
                    break;
                case NAIL: {
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                    VfxDraw.beam(m, beam, dir.scale(0.35), dir.scale(-0.45), 0.05F, 0.03F, VfxDraw.argb(1.0F, 15261904), VfxDraw.argb(0.9F, 9072736), eye);
                    VfxDraw.beam(m, beam, dir.scale(0.3), dir.scale(-1.6), 0.12F, 0.01F, VfxDraw.argb(0.6F, 14708784), VfxDraw.argb(0.0F, 14708784), eye);
                    break;
                }
                case EMBER_INSECT: {
                    float flick = 1.0F + 0.25F * Mth.sin(age * 3.1F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.7F * flick, 0.0F, VfxDraw.argb(0.85F, 16738832));
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.25F, 0.0F, VfxDraw.argb(1.0F, 16773312));
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                    VfxDraw.beam(m, beam, c, dir.scale(-0.9), 0.2F, 0.02F, VfxDraw.argb(0.8F, 16747040), VfxDraw.argb(0.0F, 16730632), eye);
                    break;
                }
                case WOOD_BALL:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.95F, -12966892, -10864096, eye, 10, 16, 1.2F);
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 1.0F, 0, -1604669376, eye, 10, 16, 3.0F);
                    VertexConsumer ring = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.RING));
                    Vec3 axis = dir.cross(new Vec3(0.0, 1.0, 0.0));
                    if (axis.lengthSqr() < 1.0E-4) {
                        axis = new Vec3(1.0, 0.0, 0.0);
                    }

                    VfxDraw.ring(m, ring, c, axis.normalize(), 0.97F, 0.12F, -14674934, 24);
                    break;
                case METEOR: {
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 2.2F, -15070716, -11920888, eye, 12, 18, 1.2F);
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 2.4F, 1090472456, -20416, eye, 12, 18, 2.2F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 8.0F, 0.0F, VfxDraw.argb(0.7F, 16738832));
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                    VfxDraw.beam(m, beam, c, dir.scale(-9.0), 2.2F, 0.4F, VfxDraw.argb(0.8F, 16747040), VfxDraw.argb(0.0F, 16726536), eye);
                    break;
                }
                case SPIRIT_SWARM:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.25F, -268171258, -1609553376, eye, 8, 12, 1.4F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.8F, 0.0F, VfxDraw.argb(0.45F, 3844744));
                    break;
                case ICE_SHARD: {
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                    VfxDraw.beam(m, beam, dir.scale(0.5), dir.scale(-0.5), 0.16F, 0.02F, VfxDraw.argb(1.0F, 13168895), VfxDraw.argb(0.9F, 5941472), eye);
                    VfxDraw.beam(m, beam, dir.scale(0.45), dir.scale(-0.3), 0.06F, 0.01F, VfxDraw.argb(1.0F, 16777215), VfxDraw.argb(1.0F, 14743807), eye);
                    VfxDraw.beam(m, beam, c, dir.scale(-1.8), 0.1F, 0.01F, VfxDraw.argb(0.5F, 11067647), VfxDraw.argb(0.0F, 11067647), eye);
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.7F, 0.0F, VfxDraw.argb(0.4F, 11067647));
                    break;
                }
                case FISH: {
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.BEAM));
                    Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
                    side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
                    Vec3 tail = dir.scale(-0.55).add(side.scale(0.18 * (double)Mth.sin(age * 1.6F)));
                    VfxDraw.beam(m, beam, dir.scale(0.35), tail, 0.24F, 0.05F, -267240870, -534091120, eye);
                    VfxDraw.beam(
                        m,
                        beam,
                        tail,
                        tail.add(dir.scale(-0.25)).add(side.scale(0.2 * (double)Mth.sin(age * 1.6F + 1.0F))),
                        0.22F,
                        0.02F,
                        -802518320,
                        1613400784,
                        eye
                    );
                    VfxDraw.billboard(m, glow, this.cam(), dir.scale(0.3), 0.18F, 0.0F, VfxDraw.argb(1.0F, 14221296));
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.9F, 0.0F, VfxDraw.argb(0.3F, 2788048));
                    break;
                }
                case GRAVESTONE:
                    VfxDraw.billboard(m, glow, this.cam(), new Vec3(0.0, 0.6, 0.0), 1.6F, 0.0F, VfxDraw.argb(0.25F, 10133600));
                    break;
                case PACHINKO_BALL:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.13F, -7696232, -1511176, eye, 6, 10, 1.2F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.35F, 0.0F, VfxDraw.argb(0.5F, 7004282));
                    VfxDraw.beam(
                        m,
                        buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM)),
                        c,
                        dir.scale(-0.8),
                        0.08F,
                        0.01F,
                        VfxDraw.argb(0.6F, 14221280),
                        VfxDraw.argb(0.0F, 7004282),
                        eye
                    );
                    break;
                case GARUDA: {
                    VertexConsumer beam = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM));
                    Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
                    side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
                    Vec3 upv = side.cross(dir).normalize();
                    float flap = Mth.sin(age * 1.4F) * 0.5F;
                    Vec3 wl = side.scale(1.3).add(upv.scale((double)flap)).add(dir.scale(-0.3));
                    Vec3 wr = side.scale(-1.3).add(upv.scale((double)flap)).add(dir.scale(-0.3));
                    VfxDraw.beam(m, beam, c, wl, 0.45F, 0.05F, VfxDraw.argb(0.9F, 16767088), VfxDraw.argb(0.2F, 16748576), eye);
                    VfxDraw.beam(m, beam, c, wr, 0.45F, 0.05F, VfxDraw.argb(0.9F, 16767088), VfxDraw.argb(0.2F, 16748576), eye);
                    VfxDraw.beam(m, beam, dir.scale(0.5), dir.scale(-2.4), 0.3F, 0.02F, VfxDraw.argb(0.8F, 16773312), VfxDraw.argb(0.0F, 16762960), eye);
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 0.4F, -1824, -2130726848, eye, 8, 12, 1.6F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 2.2F, 0.0F, VfxDraw.argb(0.5F, 16762960));
                    break;
                }
                case GAVEL:
                    VfxDraw.billboard(m, glow, this.cam(), new Vec3(0.0, 0.4, 0.0), 1.1F, 0.0F, VfxDraw.argb(0.3F, 15259808));
                    break;
                case TRUESPHERE:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.9F, -10855832, -1513224, eye, 14, 20, 1.8F);
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.WHITE)), c, 0.95F, 0, -1057949441, eye, 14, 20, 3.0F);
                    VfxDraw.billboard(m, glow, this.cam(), new Vec3(-0.3, 0.4, 0.0), 0.35F, 0.0F, VfxDraw.argb(0.9F, 16777215));
                    VfxDraw.billboard(m, glow, this.cam(), c, 2.4F, 0.0F, VfxDraw.argb(0.25F, 13158655));
                    break;
                case ENERGY_SHOT:
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.9F, 0.0F, VfxDraw.argb(0.8F, 15786160));
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.35F, 0.0F, VfxDraw.argb(1.0F, 16777215));
                    VfxDraw.beam(
                        m,
                        buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.BEAM)),
                        c,
                        dir.scale(-1.4),
                        0.25F,
                        0.02F,
                        VfxDraw.argb(0.7F, 16773312),
                        VfxDraw.argb(0.0F, 15786160),
                        eye
                    );
                    break;
                case ROT_BLOOD:
                    VfxDraw.sphere(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.WHITE)), c, 0.22F, -265676284, -1067827184, eye, 8, 12, 1.4F);
                    VfxDraw.billboard(m, glow, this.cam(), c, 0.7F, 0.0F, VfxDraw.argb(0.4F, 10123824));
                    VfxDraw.beam(m, buf.getBuffer(FRRenderTypes.translucent(FRRenderTypes.BEAM)), c, dir.scale(-1.1), 0.16F, 0.02F, -1069929976, 5914640, eye);
            }
        }
    }

    public static class Slash extends TechniqueRenderers.Base<SlashEntity> {
        public Slash(Context ctx) {
            super(ctx);
        }

        void draw(SlashEntity e, Matrix4f m, MultiBufferSource buf, Vec3 eye, float age, float partial) {
            Vec3 fwd = e.getDeltaMovement();
            if (!(fwd.lengthSqr() < 1.0E-6)) {
                fwd = fwd.normalize();
                Vec3[] ab = VfxDraw.basis(fwd);
                double roll = Math.toRadians((double)e.roll());
                Vec3 side = ab[0].scale(Math.cos(roll)).add(ab[1].scale(Math.sin(roll)));
                float r = e.width() * 0.55F;
                Vec3 center = fwd.scale((double)(-r));
                float a = Mth.clamp((14.0F - age) / 5.0F, 0.0F, 1.0F);
                VertexConsumer vc = buf.getBuffer(FRRenderTypes.additive(FRRenderTypes.SLASH));
                VfxDraw.crescent(m, vc, center, fwd, side, r, 0.28F * r, -1.25F, 1.25F, VfxDraw.argb(a, 16777215), 22);
                VfxDraw.crescent(m, vc, center, fwd, side, r * 1.03F, 0.1F * r, -1.25F, 1.25F, VfxDraw.argb(a, 16719920), 22);
            }
        }
    }
}
