package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Gopnik: shaved head under a flat cap, a dark tracksuit with two white stripes, white trainers. */
public final class GopnikSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "gopnik"), "main");
   static final int[] CAP = new int[]{64, 0, 9, 2, 9};
   static final int[] VISOR = new int[]{64, 16, 8, 1, 3};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("head", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HEAD, -4.0F, -8.0F, -4.0F, 0.0F)),
      new BoxSkin.Part("hat", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HAT, -4.0F, -8.0F, -4.0F, 0.4F)),
      new BoxSkin.Part(
         "cap", "head", 0.0F, 0.0F, 0.0F, -0.08F, 0.0F, 0.0F,
         new BoxSkin.Box(CAP, -4.5F, -9.3F, -4.5F, 0.0F),
         new BoxSkin.Box(VISOR, -4.0F, -8.3F, -7.0F, 0.0F)
      ),
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
   private static final int SKIN = 14335117;
   private static final int STUBBLE = 9013641;
   private static final int SUIT = 1909566;
   private static final int STRIPE = 15329769;
   private static final int CAP_COL = 3289650;
   private static final int SHOE_COL = 15132390;

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == FRONT) {
            if (y == 3 && (x == 2 || x == 5)) {
               return 2236962;
            } else if (y == 2 && x >= 1 && x <= 6 && x != 3 && x != 4) {
               return this.shade(STUBBLE, 0.8);
            } else if (y == 6 && x >= 2 && x <= 5) {
               return this.shade(SKIN, 0.7);
            } else {
               return this.shade(SKIN, 0.95 + this.noise() * 0.06);
            }
         } else if (f == TOP || f == BACK || y < 2) {
            return this.shade(STUBBLE, 0.95 + this.noise() * 0.25);
         } else {
            return this.shade(SKIN, 0.9 + this.noise() * 0.05);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> CLEAR);
      this.box(CAP, (f, x, y, w, h) -> this.shade(CAP_COL, (f == TOP ? 1.0 : 0.85) + this.noise() * 0.2));
      this.box(VISOR, (f, x, y, w, h) -> this.shade(CAP_COL, 0.75 + this.noise() * 0.15));
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> {
         if (f == FRONT && x == 3 && y < 9) {
            return this.shade(12632256, 0.9);
         } else if (y == 0 && (f == FRONT || f == BACK)) {
            return this.shade(STRIPE, 0.9);
         } else {
            return this.shade(SUIT, 1.0 + this.noise() * 0.25);
         }
      });
      BoxSkin.Painter arm = (f, x, y, w, h) -> {
         if (y >= 11) {
            return this.shade(SKIN, 0.9);
         } else if ((f == RIGHT || f == LEFT) && (x == 1 || x == 2)) {
            return this.shade(STRIPE, 0.95);
         } else {
            return this.shade(SUIT, 1.0 + this.noise() * 0.25);
         }
      };
      this.box(PeopleSkins.ARM, arm);
      this.box(PeopleSkins.ARM_L, arm);
      BoxSkin.Painter leg = (f, x, y, w, h) -> (f == RIGHT || f == LEFT) && (x == 1 || x == 2)
         ? this.shade(STRIPE, 0.95)
         : this.shade(SUIT, 1.0 + this.noise() * 0.25);
      this.box(PeopleSkins.LEG, leg);
      this.box(PeopleSkins.LEG_L, leg);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> y == h - 1 || f == BOTTOM ? this.shade(4210752, 1.0) : this.shade(SHOE_COL, 0.95 + this.noise() * 0.08));
   }
}
