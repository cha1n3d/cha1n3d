package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Mahito: shoulder-length blue-grey hair, a face stitched together from patches, mismatched eyes, patched dark clothes. */
public final class MahitoSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "mahito"), "main");
   static final int[] HAIR_BACK = new int[]{64, 0, 8, 6, 2};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("hair_back", "head", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(HAIR_BACK, -4.0F, -4.0F, 3.0F, 0.3F)),
      new BoxSkin.Part("head", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HEAD, -4.0F, -8.0F, -4.0F, 0.0F)),
      new BoxSkin.Part("hat", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HAT, -4.0F, -8.0F, -4.0F, 0.4F)),
      new BoxSkin.Part("body", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.BODY, -4.0F, 0.0F, -2.0F, 0.0F)),
      new BoxSkin.Part("right_arm", "", -5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.ARM, -3.0F, -2.0F, -2.0F, 0.0F)),
      new BoxSkin.Part("left_arm", "", 5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.ARM_L, -1.0F, -2.0F, -2.0F, 0.0F)),
      new BoxSkin.Part(
         "right_leg", "", -1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
         new BoxSkin.Box(PeopleSkins.LEG, -2.0F, 0.0F, -2.0F, 0.0F),
         new BoxSkin.Box(PeopleSkins.SHOE, -2.0F, 9.0F, -3.0F, 0.25F)
      ),
      new BoxSkin.Part(
         "left_leg", "", 1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F,
         new BoxSkin.Box(PeopleSkins.LEG_L, -2.0F, 0.0F, -2.0F, 0.0F),
         new BoxSkin.Box(PeopleSkins.SHOE, -2.0F, 9.0F, -3.0F, 0.25F)
      )
   };
   private static final int SKIN = 14535864;
   private static final int STITCH = 4864312;
   private static final int HAIR = 8229800;
   private static final int CLOTH = 2632764;
   private static final int PATCH = 5130346;

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == FRONT) {
            if (y <= 1 || y == 2 && (x <= 1 || x >= 6)) {
               return this.shade(HAIR, 0.95 + this.noise() * 0.2);
            } else if (y == 3 && x == 2) {
               return 6316128;
            } else if (y == 3 && x == 5) {
               return 3891586;
            } else if (y == 4 && x >= 1 && x <= 4 || x == 5 && y >= 2 && y <= 6) {
               return STITCH;
            } else if (y == 6 && x >= 3 && x <= 4) {
               return 9465936;
            } else {
               return this.shade(SKIN, (x > 5 ? 0.88 : 0.97) + this.noise() * 0.05);
            }
         } else if (f == BOTTOM) {
            return this.shade(SKIN, 0.85);
         } else {
            return y < 5 || f == TOP || f == BACK ? this.shade(HAIR, 0.92 + this.noise() * 0.2) : this.shade(SKIN, 0.9);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> f == TOP || (f == RIGHT || f == LEFT || f == BACK) && y <= 4 ? this.shade(HAIR, 1.0 + this.noise() * 0.25) : CLEAR);
      this.box(HAIR_BACK, (f, x, y, w, h) -> this.shade(HAIR, 0.85 + this.noise() * 0.2));
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> {
         if (f == FRONT && y >= 3 && y <= 6 && x >= 1 && x <= 3) {
            return this.shade(PATCH, 1.0 + this.noise() * 0.3);
         } else if (f == FRONT && (y == 2 && x >= 1 && x <= 3 || x == 4 && y >= 3 && y <= 6)) {
            return STITCH;
         } else {
            return this.shade(CLOTH, 1.0 + this.noise() * 0.35);
         }
      });
      BoxSkin.Painter arm = (f, x, y, w, h) -> y >= 9 ? (y == 10 && x == 1 ? STITCH : this.shade(SKIN, 0.92 + this.noise() * 0.05)) : this.shade(CLOTH, 1.0 + this.noise() * 0.3);
      this.box(PeopleSkins.ARM, arm);
      this.box(PeopleSkins.ARM_L, arm);
      BoxSkin.Painter leg = (f, x, y, w, h) -> f == FRONT && y >= 4 && y <= 6 && x <= 2 ? this.shade(PATCH, 1.0) : this.shade(CLOTH, 0.95 + this.noise() * 0.3);
      this.box(PeopleSkins.LEG, leg);
      this.box(PeopleSkins.LEG_L, leg);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> this.shade(1710618, 1.0 + this.noise() * 0.3));
   }
}
