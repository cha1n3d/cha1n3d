package com.bobux.vaz2109.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** A cyberpsycho: red mohawk, a glowing red visor, chrome arms and a black jacket full of wiring. */
public final class CyberpsychoSkin extends BoxSkin {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation("vaz2109", "cyberpsycho"), "main");
   static final int[] MOHAWK = new int[]{64, 0, 2, 3, 8};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("mohawk", "head", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(MOHAWK, -1.0F, -11.0F, -4.0F, 0.0F)),
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
   private static final int SKIN = 13544067;
   private static final int CHROME = 11581898;
   private static final int JACKET = 1645083;
   private static final int VISOR = 16721456;
   private static final int WIRE = 3196654;

   @Override
   protected void all() {
      this.box(PeopleSkins.HEAD, (f, x, y, w, h) -> {
         if (f == FRONT && y == 3) {
            return this.shade(VISOR, 1.0 + (x == 2 || x == 5 ? 0.15 : 0.0));
         } else if (f == FRONT && y >= 5 && x >= 5) {
            return this.shade(CHROME, 0.9 + this.noise() * 0.2);
         } else if (f == TOP || y < 2) {
            return this.shade(3092271, 0.9 + this.noise() * 0.2);
         } else {
            return this.shade(SKIN, 0.92 + this.noise() * 0.06);
         }
      });
      this.box(PeopleSkins.HAT, (f, x, y, w, h) -> CLEAR);
      this.box(MOHAWK, (f, x, y, w, h) -> this.shade(14686256, 0.85 + this.noise() * 0.35));
      this.box(PeopleSkins.BODY, (f, x, y, w, h) -> {
         if (f == FRONT && (x == 2 || x == 5) && y % 3 != 0) {
            return this.shade(WIRE, 1.0 + this.noise() * 0.2);
         } else {
            return this.shade(JACKET, 1.0 + this.noise() * 0.4);
         }
      });
      BoxSkin.Painter arm = (f, x, y, w, h) -> y < 3 ? this.shade(JACKET, 1.0 + this.noise() * 0.4)
         : (y % 3 == 0 ? this.shade(CHROME, 0.6) : this.shade(CHROME, (f == FRONT || f == RIGHT ? 1.05 : 0.85) + this.noise() * 0.12));
      this.box(PeopleSkins.ARM, arm);
      this.box(PeopleSkins.ARM_L, arm);
      BoxSkin.Painter leg = (f, x, y, w, h) -> this.shade(2368548, 0.9 + this.noise() * 0.3);
      this.box(PeopleSkins.LEG, leg);
      this.box(PeopleSkins.LEG_L, leg);
      this.box(PeopleSkins.SHOE, (f, x, y, w, h) -> this.shade(CHROME, 0.7 + this.noise() * 0.2));
   }
}
