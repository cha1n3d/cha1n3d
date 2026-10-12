package com.fallenroses.client.particle;

import com.fallenroses.vfx.FRParticleOptions;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FRParticle extends TextureSheetParticle {
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            builder.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "fallenroses:additive";
        }
    };
    private final FRParticle.Style style;
    private final SpriteSet sprites;
    private final float baseSize;
    private final float baseAlpha;
    private final float spin;

    protected FRParticle(
        ClientLevel level, double x, double y, double z, double vx, double vy, double vz, FRParticleOptions o, FRParticle.Style style, SpriteSet sprites
    ) {
        super(level, x, y, z, vx, vy, vz);
        this.style = style;
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = Math.max(
            1, o.life() + (style != FRParticle.Style.PETAL && style != FRParticle.Style.SMOKE ? 0 : this.random.nextInt(Math.max(1, o.life() / 3)))
        );
        this.rCol = o.r();
        this.gCol = o.g();
        this.bCol = o.b();
        this.baseSize = o.scale() * 0.25F;
        this.quadSize = this.baseSize;
        this.hasPhysics = style == FRParticle.Style.PETAL;

        this.gravity = switch (style) {
            case PETAL -> 0.02F;
            case EMBER -> -0.01F;
            case SMOKE -> -0.004F;
            default -> 0.0F;
        };

        this.friction = switch (style) {
            case PETAL -> 0.96F;
            default -> 0.94F;
            case SPARK -> 0.86F;
            case GLOW -> 0.9F;
        };
        this.baseAlpha = style == FRParticle.Style.SMOKE ? 0.75F : 1.0F;
        this.alpha = this.baseAlpha;
        this.spin = style == FRParticle.Style.PETAL
            ? (this.random.nextFloat() - 0.5F) * 0.3F
            : (style == FRParticle.Style.SMOKE ? (this.random.nextFloat() - 0.5F) * 0.05F : 0.0F);
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2);
        this.oRoll = this.roll;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll = this.roll + this.spin;
        float t = (float)this.age / (float)this.lifetime;
        switch (this.style) {
            case PETAL:
                this.xd = this.xd + Math.sin((double)this.age * 0.15 + (double)this.roll) * 0.004;
                this.zd = this.zd + Math.cos((double)this.age * 0.13 + (double)this.roll) * 0.004;
                this.alpha = Math.min(1.0F, (1.0F - t) * 4.0F);
                break;
            case EMBER:
                this.alpha = (1.0F - t) * (0.7F + 0.3F * this.random.nextFloat());
                this.quadSize = this.baseSize * (1.0F - t * 0.5F);
                break;
            case SMOKE:
                this.quadSize = this.baseSize * (1.0F + t * 1.4F);
                this.alpha = this.baseAlpha * (1.0F - t) * Math.min(1.0F, t * 6.0F);
                break;
            case SPARK:
                this.alpha = 1.0F - t;
                break;
            case GLOW:
                this.quadSize = this.baseSize * (1.0F - t * 0.7F);
                this.alpha = this.baseAlpha * (1.0F - t * t);
                break;
            case STAR:
                this.alpha = (0.55F + 0.45F * Mth.sin((float)this.age * 0.4F + this.roll * 5.0F)) * Math.min(1.0F, (1.0F - t) * 3.0F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return switch (this.style) {
            case PETAL, SMOKE -> ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            default -> ADDITIVE;
        };
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.style != FRParticle.Style.SMOKE && this.style != FRParticle.Style.PETAL ? 15728880 : super.getLightColor(partialTick);
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partial) {
        if (this.style != FRParticle.Style.SPARK) {
            super.render(vc, camera, partial);
        } else {
            Vec3 cam = camera.getPosition();
            float px = (float)(Mth.lerp((double)partial, this.xo, this.x) - cam.x);
            float py = (float)(Mth.lerp((double)partial, this.yo, this.y) - cam.y);
            float pz = (float)(Mth.lerp((double)partial, this.zo, this.z) - cam.z);
            Vector3f vel = new Vector3f((float)this.xd, (float)this.yd, (float)this.zd);
            float speed = vel.length();
            if (speed < 1.0E-4F) {
                super.render(vc, camera, partial);
            } else {
                Vector3f dir = new Vector3f(vel).div(speed);
                Vector3f toCam = new Vector3f(-px, -py, -pz).normalize();
                Vector3f side = new Vector3f(dir).cross(toCam);
                if (side.lengthSquared() < 1.0E-6F) {
                    side.set(0.0F, 1.0F, 0.0F);
                }

                side.normalize().mul(this.quadSize * 0.35F);
                float len = Math.max(this.quadSize, speed * 3.5F);
                Vector3f tail = new Vector3f(dir).mul(-len);
                float u0 = this.getU0();
                float u1 = this.getU1();
                float v0 = this.getV0();
                float v1 = this.getV1();
                int light = this.getLightColor(partial);
                this.put(vc, px - side.x, py - side.y, pz - side.z, u1, v1, light);
                this.put(vc, px + side.x, py + side.y, pz + side.z, u1, v0, light);
                this.put(vc, px + tail.x + side.x, py + tail.y + side.y, pz + tail.z + side.z, u0, v0, light);
                this.put(vc, px + tail.x - side.x, py + tail.y - side.y, pz + tail.z - side.z, u0, v1, light);
            }
        }
    }

    private void put(VertexConsumer vc, float x, float y, float z, float u, float v, int light) {
        vc.vertex((double)x, (double)y, (double)z).uv(u, v).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    public static record Provider(SpriteSet sprites, FRParticle.Style style) implements ParticleProvider<FRParticleOptions> {
        public Particle createParticle(FRParticleOptions o, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new FRParticle(level, x, y, z, vx, vy, vz, o, this.style, this.sprites);
        }
    }

    public static enum Style {
        GLOW,
        SPARK,
        SMOKE,
        PETAL,
        STAR,
        EMBER;
    }
}
