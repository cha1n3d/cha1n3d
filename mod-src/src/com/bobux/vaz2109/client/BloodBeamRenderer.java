package com.bobux.vaz2109.client;

import com.bobux.vaz2109.entity.BloodBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws Piercing Blood with its own textures: the compressed blood ball while charging, then a thin
 * high-pressure jet (translucent sheath + dense core) with blood flowing along it and a splash at the impact.
 */
public class BloodBeamRenderer extends EntityRenderer<BloodBeamEntity> {
   private static final ResourceLocation SHEATH = new ResourceLocation("vaz2109", "textures/entity/blood_beam.png");
   private static final ResourceLocation CORE = new ResourceLocation("vaz2109", "textures/entity/blood_beam_core.png");
   private static final ResourceLocation ORB = new ResourceLocation("vaz2109", "textures/entity/blood_orb.png");
   private static final ResourceLocation SPLASH = new ResourceLocation("vaz2109", "textures/entity/blood_splash.png");

   public BloodBeamRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(BloodBeamEntity beam, Frustum frustum, double x, double y, double z) {
      return true;
   }

   public void render(BloodBeamEntity beam, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      LivingEntity owner = beam.owner();
      Vec3 base = new Vec3(Mth.lerp(partialTick, beam.xo, beam.getX()), Mth.lerp(partialTick, beam.yo, beam.getY()), Mth.lerp(partialTick, beam.zo, beam.getZ()));
      Vec3 start = owner != null ? BloodBeamEntity.muzzle(owner, partialTick).subtract(base) : Vec3.ZERO;
      Camera camera = this.entityRenderDispatcher.camera;
      Vec3 cam = camera.getPosition().subtract(base);
      float time = (float)beam.tickCount + partialTick;
      int bright = Math.max(light, 0xA000A0);
      pose.pushPose();
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();
      if (!beam.firing()) {
         float progress = Mth.clamp(time / Math.max(1, beam.chargeTicks()), 0.0F, 1.0F);
         float size = 0.42F - 0.24F * progress + 0.03F * Mth.sin(time * 1.7F);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(ORB));
         this.billboard(vc, m, n, start, cam, size, time * 9.0F, 255, bright);

         for (int i = 0; i < 4; i++) {
            float a = time * 0.35F + i * (float)Math.PI / 2.0F;
            float r = 0.55F * (1.0F - progress) + 0.12F;
            Vec3 drop = start.add(Mth.cos(a) * r, Mth.sin(a * 1.3F) * r * 0.6F, Mth.sin(a) * r);
            this.billboard(vc, m, n, drop, cam, 0.06F, a * 40.0F, 220, bright);
         }
      } else {
         float yaw = Mth.rotLerp(partialTick, beam.yawO, beam.beamYaw());
         float pitch = Mth.lerp(partialTick, beam.pitchO, beam.beamPitch());
         float length = Mth.lerp(partialTick, beam.lengthO, beam.length());
         Vec3 dir = BloodBeamEntity.direction(yaw, pitch);
         this.jet(buffers.getBuffer(RenderType.entityTranslucent(SHEATH)), m, n, start, dir, length, cam, 0.11F, time, 1.6F, 200, bright);
         this.jet(buffers.getBuffer(RenderType.entityTranslucent(CORE)), m, n, start, dir, length, cam, 0.045F, time, 2.6F, 255, bright);
         this.billboard(buffers.getBuffer(RenderType.entityTranslucent(ORB)), m, n, start.add(dir.scale(0.1)), cam, 0.16F + 0.03F * Mth.sin(time * 2.1F), time * 13.0F, 255, bright);
         Vec3 hit = start.add(dir.scale(length));
         float splash = 0.45F + 0.12F * Mth.sin(time * 1.9F);
         this.billboard(buffers.getBuffer(RenderType.entityTranslucent(SPLASH)), m, n, hit, cam, splash, time * 23.0F, 235, light);
      }

      pose.popPose();
      super.render(beam, entityYaw, partialTick, pose, buffers, light);
   }

   /** Camera-facing ribbon split into short segments that ripple slightly, texture scrolled along the flow. */
   private void jet(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 start, Vec3 dir, float length, Vec3 cam, float width, float time, float flow, int alpha, int light) {
      float step = 0.5F;
      Vec3 prev = start;
      Vec3 prevSide = this.side(start, dir, cam, width);
      float v0 = -time * 0.25F * flow;

      for (float s = step; s < length + step; s += step) {
         float d = Math.min(s, length);
         float ripple = 0.012F * Mth.sin(time * 1.4F + d * 2.3F);
         Vec3 p = start.add(dir.scale(d)).add(0.0, ripple, 0.0);
         Vec3 side = this.side(p, dir, cam, width * (1.0F + 0.15F * Mth.sin(time * 2.0F + d * 1.7F)));
         float v1 = v0 + (d - (s - step)) * 0.6F;
         this.vertex(vc, m, n, prev.add(prevSide), 0.0F, v0, alpha, light);
         this.vertex(vc, m, n, prev.subtract(prevSide), 1.0F, v0, alpha, light);
         this.vertex(vc, m, n, p.subtract(side), 1.0F, v1, alpha, light);
         this.vertex(vc, m, n, p.add(side), 0.0F, v1, alpha, light);
         prev = p;
         prevSide = side;
         v0 = v1;
         if (d >= length) {
            break;
         }
      }
   }

   private Vec3 side(Vec3 p, Vec3 dir, Vec3 cam, float width) {
      Vec3 s = dir.cross(p.subtract(cam));
      return s.lengthSqr() < 1.0E-6 ? new Vec3(width, 0.0, 0.0) : s.normalize().scale(width);
   }

   private void billboard(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 c, Vec3 cam, float size, float spin, int alpha, int light) {
      Vec3 view = c.subtract(cam).normalize();
      Vec3 up0 = Math.abs(view.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 right = view.cross(up0).normalize();
      Vec3 up = right.cross(view).normalize();
      float a = spin * (float)(Math.PI / 180.0);
      Vec3 r = right.scale(Mth.cos(a)).add(up.scale(Mth.sin(a))).scale(size);
      Vec3 u = up.scale(Mth.cos(a)).subtract(right.scale(Mth.sin(a))).scale(size);
      this.vertex(vc, m, n, c.subtract(r).subtract(u), 0.0F, 1.0F, alpha, light);
      this.vertex(vc, m, n, c.add(r).subtract(u), 1.0F, 1.0F, alpha, light);
      this.vertex(vc, m, n, c.add(r).add(u), 1.0F, 0.0F, alpha, light);
      this.vertex(vc, m, n, c.subtract(r).add(u), 0.0F, 0.0F, alpha, light);
   }

   private void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 p, float u, float v, int alpha, int light) {
      vc.vertex(m, (float)p.x, (float)p.y, (float)p.z)
         .color(255, 255, 255, alpha)
         .uv(u, v)
         .overlayCoords(OverlayTexture.NO_OVERLAY)
         .uv2(light)
         .normal(n, 0.0F, 1.0F, 0.0F)
         .endVertex();
   }

   public ResourceLocation getTextureLocation(BloodBeamEntity beam) {
      return SHEATH;
   }
}
