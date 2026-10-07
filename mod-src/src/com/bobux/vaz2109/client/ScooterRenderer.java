package com.bobux.vaz2109.client;

import com.bobux.vaz2109.entity.ScooterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Kick scooter model: deck with grip tape, fork, T-bar with grips and two small urethane wheels. */
public class ScooterRenderer extends EntityRenderer<ScooterEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "scooter"), "main");
   private final ModelPart root;
   private ResourceLocation texture;

   public ScooterRenderer(Context context) {
      super(context);
      this.root = context.bakeLayer(LAYER);
      this.shadowRadius = 0.3F;
   }

   public void render(ScooterEntity scooter, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      float yaw = Mth.rotLerp(partialTick, scooter.yRotO, scooter.getYRot());
      pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
      float pitch = Mth.lerp(partialTick, scooter.visualPitchO, scooter.visualPitch);
      if (pitch != 0.0F) {
         pose.mulPose(Axis.XP.rotationDegrees(-pitch));
      }

      float lean = Mth.lerp(partialTick, scooter.visualLeanO, scooter.visualLean);
      pose.mulPose(Axis.ZP.rotationDegrees(lean * 30.0F));
      pose.scale(-1.0F, -1.0F, 1.0F);
      pose.translate(0.0F, -1.501F, 0.0F);
      this.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.getTextureLocation(scooter))), light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
      super.render(scooter, entityYaw, partialTick, pose, buffers, light);
   }

   public ResourceLocation getTextureLocation(ScooterEntity scooter) {
      if (this.texture == null) {
         this.texture = ArthasRenderer.upload("vaz2109_scooter", new ScooterSkin().render()[0]);
      }

      return this.texture;
   }
}
