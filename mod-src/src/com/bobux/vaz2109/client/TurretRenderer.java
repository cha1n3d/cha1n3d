package com.bobux.vaz2109.client;

import com.bobux.vaz2109.entity.TurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The mechanic's sentry drawn from block models: an anvil-iron base, a turning dispenser head and a chain barrel. */
public class TurretRenderer extends EntityRenderer<TurretEntity> {
   private final BlockRenderDispatcher blocks;

   public TurretRenderer(Context context) {
      super(context);
      this.blocks = context.getBlockRenderDispatcher();
      this.shadowRadius = 0.4F;
   }

   public void render(TurretEntity turret, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      this.block(Blocks.IRON_BLOCK.defaultBlockState(), pose, buffers, light, -0.35F, 0.0F, -0.35F, 0.7F, 0.25F, 0.7F);
      this.block(Blocks.IRON_BARS.defaultBlockState(), pose, buffers, light, -0.15F, 0.25F, -0.15F, 0.3F, 0.35F, 0.3F);
      pose.translate(0.0F, 0.85F, 0.0F);
      pose.mulPose(Axis.YP.rotationDegrees(-turret.aim()));
      BlockState head = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH);
      this.block(head, pose, buffers, light, -0.25F, -0.25F, -0.25F, 0.5F, 0.5F, 0.5F);
      this.block(Blocks.CHAIN.defaultBlockState().setValue(net.minecraft.world.level.block.ChainBlock.AXIS, Direction.Axis.Z), pose, buffers, light, -0.2F, -0.2F, 0.2F, 0.4F, 0.4F, 0.5F);
      pose.popPose();
      super.render(turret, yaw, partialTick, pose, buffers, light);
   }

   private void block(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, float x, float y, float z, float sx, float sy, float sz) {
      pose.pushPose();
      pose.translate(x, y, z);
      pose.scale(sx, sy, sz);
      this.blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
   }

   public ResourceLocation getTextureLocation(TurretEntity turret) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
