package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Dzhigan: buzz cut and a thick black beard, black shades, a gold chain over a white tank top under an open black
 * leather jacket, tattooed hands, dark jeans and white trainers.
 */
public final class DzhiganSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "dzhigan"), "main");
   static final int[] SHADES = new int[]{64, 0, 8, 2, 1};
   static final int[] PENDANT = new int[]{64, 8, 2, 2, 1};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("head", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HEAD, -4.0F, -8.0F, -4.0F, 0.0F)),
      new BoxSkin.Part("hat", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.HAT, -4.0F, -8.0F, -4.0F, 0.4F)),
      new BoxSkin.Part("shades", "head", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(SHADES, -4.0F, -5.4F, -4.6F, 0.05F)),
      new BoxSkin.Part("body", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PeopleSkins.BODY, -4.0F, 0.0F, -2.0F, 0.0F)),
      new BoxSkin.Part("pendant", "body", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(PENDANT, -1.0F, 3.4F, -2.6F, 0.0F)),
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
   private static final int SKIN = 0xD9A27E;
   private static final int HAIR = 0x1E1814;
   private static final int LEATHER = 0x18181B;
   private static final int TANK = 0xEDEDED;
   private static final int GOLD = 0xF2C230;
   private static final int INK = 0x2E3A55;
   private static final int JEANS = 0x262C38;
   private static final int SHOE_COL = 0xF0F0F0;

   /** A gold chain hanging in a V on the chest. */
   private static boolean chain(int x, int y) {
      return y == Math.min(x, 7 - x) && y >= 1 && y <= 3;
   }

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == TOP) {
            return this.shade(HAIR, 0.9 + this.noise() * 0.3);
         } else if (f == FRONT) {
            if (y == 0) {
               return this.shade(HAIR, 0.9 + this.noise() * 0.2);
            } else if (y == 2 && x >= 1 && x <= 6 && x != 3 && x != 4) {
               return this.shade(HAIR, 0.85);
            } else if (y >= 5) {
               // the beard, with the mouth in it
               return y == 5 && x >= 3 && x <= 4 ? this.shade(0x7A4A3A, 0.9) : this.shade(HAIR, 0.9 + this.noise() * 0.25);
            } else if (y == 4 && (x == 0 || x == 7)) {
               return this.shade(HAIR, 0.9);
            } else {
               return this.shade(SKIN, 0.95 + this.noise() * 0.06);
            }
         } else if (f == BACK || f == BOTTOM) {
            return f == BOTTOM ? this.shade(HAIR, 0.8) : this.shade(HAIR, 0.9 + this.noise() * 0.3);
         } else {
            // sides: hair on top, ear and skin in the middle, beard below
            return y < 2 || y >= 5 && x <= 3 ? this.shade(HAIR, 0.9 + this.noise() * 0.25) : this.shade(SKIN, 0.88 + this.noise() * 0.05);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> CLEAR);
      this.box(SHADES, (f, x, y, w, h) -> {
         if (f == FRONT && y == 0 && (x == 1 || x == 5)) {
            this.glow(0x6A7080);
            return 0x6A7080; // a glint on each lens
         }

         return f == FRONT && y == 1 && (x == 3 || x == 4) ? CLEAR : this.shade(0x0C0C10, 1.0 + this.noise() * 0.3);
      });
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> {
         if (f == FRONT) {
            if (x >= 2 && x <= 5 && y < 11) {
               if (chain(x, y)) {
                  return this.shade(GOLD, 0.9 + this.noise() * 0.25);
               }

               return this.shade(TANK, 0.92 + this.noise() * 0.08);
            } else if (y == 11) {
               return this.shade(0x101010, 1.0); // belt
            } else {
               return x == 1 || x == 6 ? this.shade(LEATHER, 1.35) : this.shade(LEATHER, 1.0 + this.noise() * 0.35);
            }
         } else if (f == BACK && y >= 3 && y <= 6 && x >= 2 && x <= 5) {
            return this.shade(0x8A8A8A, 0.5 + this.noise() * 0.4); // a print on the jacket's back
         } else {
            return y == 11 ? this.shade(0x101010, 1.0) : this.shade(LEATHER, 1.0 + this.noise() * 0.35);
         }
      });
      this.box(PENDANT, (f, x, y, w, h) -> {
         this.glow(GOLD);
         return this.shade(GOLD, 0.85 + this.noise() * 0.35);
      });
      BoxSkin.Painter arm = (f, x, y, w, h) -> {
         if (y >= 9) {
            // tattooed hands
            return this.noise() < 0.3 ? this.shade(INK, 0.9 + this.noise()) : this.shade(SKIN, 0.9 + this.noise() * 0.05);
         } else if (y == 8) {
            return this.shade(LEATHER, 1.4); // the cuff
         } else {
            return this.shade(LEATHER, 1.0 + this.noise() * 0.35);
         }
      };
      this.box(PeopleSkins.ARM, arm);
      this.box(PeopleSkins.ARM_L, arm);
      BoxSkin.Painter leg = (f, x, y, w, h) -> this.shade(JEANS, (y > 9 ? 0.85 : 1.0) + this.noise() * 0.25);
      this.box(PeopleSkins.LEG, leg);
      this.box(PeopleSkins.LEG_L, leg);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> y == h - 1 || f == BOTTOM ? this.shade(0x404040, 1.0) : this.shade(SHOE_COL, 0.95 + this.noise() * 0.08));
   }
}
