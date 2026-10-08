package com.bobux.vaz2109.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/** Dzhigan's pose: in a fight both arms are straight out, a Makarov in each hand, following his eyes. */
public class DzhiganModel<T extends Mob> extends HumanoidModel<T> {
   public DzhiganModel(ModelPart root) {
      super(root);
   }

   @Override
   public void setupAnim(T entity, float limb, float limbSpeed, float age, float headYaw, float headPitch) {
      super.setupAnim(entity, limb, limbSpeed, age, headYaw, headPitch);
      if (!entity.isAggressive()) {
         return;
      }

      float pitch = headPitch * ((float)Math.PI / 180.0F);
      float yaw = headYaw * ((float)Math.PI / 180.0F);
      // the last shot kicks the arm that fired it up a little
      float kick = this.attackTime > 0.0F ? Mth.sin(this.attackTime * (float)Math.PI) * 0.35F : 0.0F;
      boolean rightFired = entity.swingingArm != net.minecraft.world.InteractionHand.OFF_HAND;
      this.rightArm.xRot = -(float)Math.PI / 2.0F + pitch - (rightFired ? kick : 0.0F);
      this.rightArm.yRot = yaw - 0.12F;
      this.rightArm.zRot = 0.0F;
      this.leftArm.xRot = -(float)Math.PI / 2.0F + pitch - (rightFired ? 0.0F : kick);
      this.leftArm.yRot = yaw + 0.12F;
      this.leftArm.zRot = 0.0F;
   }
}
