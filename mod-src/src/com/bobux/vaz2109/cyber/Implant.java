package com.bobux.vaz2109.cyber;

import org.jetbrains.annotations.Nullable;

/** Cyberware: one implant per body slot; each one eats into the owner's humanity. */
public enum Implant {
   SANDEVISTAN("sandevistan", Implant.Slot.NERVOUS, 30),
   KERENZIKOV("kerenzikov", Implant.Slot.NERVOUS, 15),
   MANTIS_BLADES("mantis_blades", Implant.Slot.ARMS, 20),
   GORILLA_ARMS("gorilla_arms", Implant.Slot.ARMS, 15),
   KIROSHI_OPTICS("kiroshi_optics", Implant.Slot.EYES, 10),
   REINFORCED_TENDONS("reinforced_tendons", Implant.Slot.LEGS, 10),
   SECOND_HEART("second_heart", Implant.Slot.HEART, 25),
   SUBDERMAL_ARMOR("subdermal_armor", Implant.Slot.SKIN, 15);

   public final String id;
   public final Implant.Slot slot;
   /** Humanity it takes. */
   public final int cost;

   Implant(String id, Implant.Slot slot, int cost) {
      this.id = id;
      this.slot = slot;
      this.cost = cost;
   }

   public int bit() {
      return 1 << this.ordinal();
   }

   @Nullable
   public static Implant inSlot(int mask, Implant.Slot slot) {
      for (Implant i : values()) {
         if (i.slot == slot && (mask & i.bit()) != 0) {
            return i;
         }
      }

      return null;
   }

   public enum Slot {
      NERVOUS,
      ARMS,
      EYES,
      LEGS,
      HEART,
      SKIN;

      public String id() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }
}
