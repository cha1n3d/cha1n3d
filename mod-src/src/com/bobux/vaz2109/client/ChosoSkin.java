package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Choso: black hair tied into two short tufts, the dark blood mark across the nose, a cream robe over black. */
public final class ChosoSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "choso"), "main");
   static final int[] TUFT_R = new int[]{64, 0, 3, 3, 3};
   static final int[] TUFT_L = new int[]{80, 0, 3, 3, 3};
   static final int[] COLLAR = new int[]{64, 16, 9, 2, 5};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("head", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HEAD, -4.0F, -8.0F, -4.0F, 0.0F)),
      new BoxSkin.Part("hat", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HAT, -4.0F, -8.0F, -4.0F, 0.4F)),
      new BoxSkin.Part("tuft_r", "head", -3.5F, -8.0F, 1.0F, -0.35F, 0.0F, -0.45F, new BoxSkin.Box(TUFT_R, -1.5F, -2.5F, -1.5F, 0.0F)),
      new BoxSkin.Part("tuft_l", "head", 3.5F, -8.0F, 1.0F, -0.35F, 0.0F, 0.45F, new BoxSkin.Box(TUFT_L, -1.5F, -2.5F, -1.5F, 0.0F)),
      new BoxSkin.Part("body", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.BODY, -4.0F, 0.0F, -2.0F, 0.0F)),
      new BoxSkin.Part("collar", "body", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(COLLAR, -4.5F, -0.5F, -2.5F, 0.0F)),
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
   private static final int SKIN = 14732704;
   private static final int HAIR = 1381912;
   private static final int MARK = 3150874;
   private static final int ROBE = 14341830;
   private static final int TRIM = 7884887;
   private static final int BLACK = 1907997;

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == FRONT) {
            if (y <= 1 || y == 2 && (x == 0 || x == 7 || x == 3 || x == 4)) {
               return this.shade(HAIR, 1.0 + this.noise() * 0.7);
            } else if (y == 3 && (x == 2 || x == 5)) {
               return 4864040;
            } else if (y == 3 && (x == 1 || x == 6)) {
               return this.shade(SKIN, 0.85);
            } else if (y == 4 && x >= 1 && x <= 6) {
               return this.shade(MARK, 0.9 + this.noise() * 0.2);
            } else if (y == 6 && (x == 3 || x == 4)) {
               return 9465936;
            } else {
               return this.shade(SKIN, 0.95 + this.noise() * 0.05);
            }
         } else if (f == BOTTOM) {
            return this.shade(SKIN, 0.85);
         } else {
            return y < 4 || f == TOP || f == BACK ? this.shade(HAIR, 1.0 + this.noise() * 0.7) : this.shade(SKIN, 0.9);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> f == TOP || (f == RIGHT || f == LEFT || f == BACK) && y <= 2 ? this.shade(HAIR, 1.2 + this.noise() * 0.9) : CLEAR);
      BoxSkin.Painter tuft = (f, x, y, w, h) -> this.shade(HAIR, 1.0 + this.noise() * 0.9);
      this.box(TUFT_R, tuft);
      this.box(TUFT_L, tuft);
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> {
         if (f == FRONT && (x == 3 || x == 4) && y < 5) {
            return this.shade(BLACK, 1.0 + this.noise() * 0.4);
         } else if (y == h - 1 || f == FRONT && (x == 2 || x == 5) && y < 6) {
            return this.shade(TRIM, 0.9 + this.noise() * 0.2);
         } else {
            return this.shade(ROBE, 0.9 + this.noise() * 0.1 - (y > 8 ? 0.06 : 0.0));
         }
      });
      this.box(COLLAR, (f, x, y, w, h) -> this.shade(TRIM, 0.8 + this.noise() * 0.3));
      BoxSkin.Painter arm = (f, x, y, w, h) -> y >= 10
         ? this.shade(SKIN, 0.9 + this.noise() * 0.1)
         : (y == 9 ? this.shade(TRIM, 0.9) : this.shade(ROBE, 0.88 + this.noise() * 0.12));
      this.box(PeopleSkins.ARM, arm);
      this.box(PeopleSkins.ARM_L, arm);
      BoxSkin.Painter leg = (f, x, y, w, h) -> this.shade(BLACK, 1.0 + this.noise() * 0.5 - (x == 0 ? 0.1 : 0.0));
      this.box(PeopleSkins.LEG, leg);
      this.box(PeopleSkins.LEG_L, leg);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> this.shade(2763306, 1.0 + this.noise() * 0.4));
   }
}
