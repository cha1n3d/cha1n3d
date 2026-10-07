package com.bobux.vaz2109.client;

import com.bobux.vaz2109.entity.ScooterEntity;
import com.bobux.vaz2109.entity.SkateboardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class SkatePose {
   private SkatePose() {
   }

   public static void apply(HumanoidModel<?> model, LivingEntity rider, SkateboardEntity board) {
      if (board instanceof ScooterEntity) {
         scooter(model, rider, board);
         return;
      }

      float pt = Minecraft.getInstance().getFrameTime();
      float t = (float)rider.tickCount + pt;
      boolean air = !board.onGround();
      boolean grind = board.visualGrind;
      float push = air ? 0.0F : Mth.lerp(pt, (float)board.visualPushO, (float)board.visualPush) / 8.0F;
      float lean = Mth.lerp(pt, board.visualLeanO, board.visualLean);
      float sway = Mth.sin(t * 0.13F) * 0.05F;
      model.rightLeg.yRot = 0.0F;
      model.leftLeg.yRot = 0.0F;
      if (push > 0.0F) {
         float stroke = Mth.sin((1.0F - push) * (float) Math.PI);
         model.crouching = false;
         model.leftLeg.xRot = 0.0F;
         model.leftLeg.zRot = -0.1F;
         model.rightLeg.xRot = -0.2F + stroke * 0.25F;
         model.rightLeg.zRot = 0.1F + stroke * 0.8F;
      } else {
         model.crouching = true;
         float tuck = air ? -0.6F : 0.0F;
         model.leftLeg.xRot = tuck;
         model.rightLeg.xRot = tuck;
         model.leftLeg.zRot = -0.3F;
         model.rightLeg.zRot = 0.3F;
      }

      float out = grind ? 1.4F : (air ? 1.05F : (push > 0.0F ? 0.3F : 0.55F));
      float raise = air ? -0.55F : (grind ? -0.1F : -0.15F);
      float swing = push > 0.0F ? Mth.sin((1.0F - push) * (float) Math.PI) * 0.6F : 0.0F;
      model.rightArm.yRot = 0.0F;
      model.leftArm.yRot = 0.0F;
      model.rightArm.xRot = raise + swing;
      model.leftArm.xRot = raise - swing;
      model.rightArm.zRot = out + sway - lean;
      model.leftArm.zRot = -out + sway - lean;
   }

   /** Standing on a kick scooter: both hands on the bar, the back leg kicks on every push. */
   private static void scooter(HumanoidModel<?> model, LivingEntity rider, SkateboardEntity board) {
      float pt = Minecraft.getInstance().getFrameTime();
      float push = board.onGround() ? Mth.lerp(pt, (float)board.visualPushO, (float)board.visualPush) / 8.0F : 0.0F;
      float lean = Mth.lerp(pt, board.visualLeanO, board.visualLean);
      model.crouching = false;
      model.rightArm.xRot = -1.0F;
      model.leftArm.xRot = -1.0F;
      model.rightArm.yRot = -0.25F + lean * 0.5F;
      model.leftArm.yRot = 0.25F + lean * 0.5F;
      model.rightArm.zRot = 0.0F;
      model.leftArm.zRot = 0.0F;
      model.leftLeg.xRot = 0.0F;
      model.leftLeg.yRot = 0.0F;
      model.leftLeg.zRot = -0.03F;
      float stroke = push > 0.0F ? Mth.sin((1.0F - push) * (float) Math.PI) : 0.0F;
      model.rightLeg.xRot = -0.15F + stroke * 0.95F;
      model.rightLeg.yRot = 0.0F;
      model.rightLeg.zRot = 0.04F;
   }
}
