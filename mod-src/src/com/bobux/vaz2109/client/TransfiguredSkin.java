package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** A transfigured human: swollen grey-green flesh, eyes in the wrong places, a gaping mouth, rags. */
public final class TransfiguredSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "transfigured"), "main");
   static final int[] HUMP = new int[]{64, 0, 6, 4, 3};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
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
      ),
      new BoxSkin.Part("hump", "body", 0.0F, 0.0F, 0.0F, 0.3F, 0.0F, 0.0F, new BoxSkin.Box(HUMP, -3.0F, 0.5F, 1.5F, 0.0F))
   };
   private static final int FLESH = 9479557;
   private static final int DARK = 5263951;
   private static final int RAG = 7038025;

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == FRONT) {
            if (y == 2 && x == 1 || y == 4 && x == 6 || y == 1 && x == 4) {
               return 15724527;
            } else if (y >= 5 && y <= 6 && x >= 2 && x <= 5) {
               return y == 5 ? 3014656 : 12632256;
            } else {
               return this.shade(FLESH, 0.85 + this.noise() * 0.35);
            }
         } else {
            return this.shade(FLESH, 0.8 + this.noise() * 0.4);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> CLEAR);
      this.box(HUMP, (f, x, y, w, h) -> this.shade(FLESH, 0.8 + this.noise() * 0.4));
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> y < 6 ? this.shade(RAG, 0.8 + this.noise() * 0.5) : this.shade(FLESH, 0.8 + this.noise() * 0.35));
      BoxSkin.Painter limb = (f, x, y, w, h) -> this.shade((x + y) % 5 == 0 ? DARK : FLESH, 0.8 + this.noise() * 0.4);
      this.box(PeopleSkins.ARM, limb);
      this.box(PeopleSkins.ARM_L, limb);
      this.box(PeopleSkins.LEG, limb);
      this.box(PeopleSkins.LEG_L, limb);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> this.shade(DARK, 0.9 + this.noise() * 0.3));
   }
}
