package com.bobux.vaz2109.client;

/** Box layout and painted texture of the kick scooter (model space: ground at y = 24, front at -Z). */
public final class ScooterSkin extends BoxSkin {
   static final int[] DECK = new int[]{0, 0, 4, 1, 16};
   static final int[] FENDER = new int[]{0, 20, 4, 1, 4};
   static final int[] WHEEL = new int[]{48, 0, 1, 3, 3};
   static final int[] NECK = new int[]{0, 30, 2, 2, 3};
   static final int[] STEM = new int[]{20, 30, 1, 18, 1};
   static final int[] BAR = new int[]{30, 30, 12, 1, 1};
   static final int[] FORK = new int[]{60, 30, 2, 3, 1};
   public static final BoxSkin.Part[] PARTS = new BoxSkin.Part[]{
      new BoxSkin.Part("deck", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(DECK, -2.0F, 20.5F, -6.0F, 0.0F)),
      new BoxSkin.Part("fender", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(FENDER, -2.0F, 19.9F, 9.0F, 0.0F)),
      new BoxSkin.Part("wheel_back", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(WHEEL, -0.5F, 21.0F, 9.5F, 0.0F)),
      new BoxSkin.Part("wheel_front", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(WHEEL, -0.5F, 21.0F, -10.5F, 0.0F)),
      new BoxSkin.Part("neck", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(NECK, -1.0F, 19.5F, -8.5F, 0.0F)),
      new BoxSkin.Part("fork", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(FORK, -1.0F, 20.0F, -9.5F, 0.0F)),
      new BoxSkin.Part("stem", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(STEM, -0.5F, 2.5F, -9.5F, 0.0F)),
      new BoxSkin.Part("bar", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new BoxSkin.Box(BAR, -6.0F, 2.5F, -9.5F, 0.0F))
   };
   private static final int GRIP = 2302755;
   private static final int RED = 13250593;
   private static final int ALU = 12634325;
   private static final int RUBBER = 1710618;
   private static final int WHEELS = 15263719;

   @Override
   protected void all() {
      this.box(DECK, (f, x, y, w, h) -> f == TOP ? this.shade(GRIP, 0.9 + this.noise() * 0.4) : (f == BOTTOM ? this.shade(ALU, 0.7) : this.shade(RED, 0.95 + this.noise() * 0.1)));
      this.box(FENDER, (f, x, y, w, h) -> this.shade(RUBBER, 1.0 + this.noise() * 0.5));
      this.box(WHEEL, (f, x, y, w, h) -> f == RIGHT || f == LEFT ? (x == 1 && y == 1 ? this.shade(ALU, 0.8) : this.shade(WHEELS, 0.9 + this.noise() * 0.1)) : this.shade(WHEELS, 0.8));
      this.box(NECK, (f, x, y, w, h) -> this.shade(ALU, 0.85 + this.noise() * 0.15));
      this.box(FORK, (f, x, y, w, h) -> this.shade(ALU, 0.75 + this.noise() * 0.15));
      this.box(STEM, (f, x, y, w, h) -> y == 6 || y == 7 ? this.shade(RED, 0.9) : this.shade(ALU, (f == FRONT ? 1.0 : 0.85) + this.noise() * 0.1));
      this.box(BAR, (f, x, y, w, h) -> x <= 2 || x >= w - 3 ? this.shade(RUBBER, 1.1 + this.noise() * 0.5) : this.shade(ALU, 0.9 + this.noise() * 0.1));
   }
}
