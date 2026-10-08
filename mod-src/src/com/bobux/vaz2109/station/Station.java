package com.bobux.vaz2109.station;

import com.bobux.vaz2109.fx.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public enum Station {
   CURSED("cursed_altar", -267384300, -266595292, 12593224),
   MASKS("mask_table", -267122148, -266203088, 16727200),
   GARAGE("garage_bench", -266988010, -266066908, 15763998),
   ARMORY("armory_bench", -267250672, -266329060, 10137674),
   /** Ripperdoc's chair: cyberware is made here. */
   RIPPERDOC("ripperdoc", -267645168, -266592988, 3792895);

   public final String id;
   public final int background;
   public final int panel;
   public final int accent;

   private Station(String id, int background, int panel, int accent) {
      this.id = id;
      this.background = background;
      this.panel = panel;
      this.accent = accent;
   }

   public RegistryObject<SoundEvent> craftSound() {
      return switch (this) {
         case CURSED -> ModSounds.STATION_CURSED;
         case MASKS -> ModSounds.STATION_MASK;
         case GARAGE -> ModSounds.STATION_GARAGE;
         case ARMORY, RIPPERDOC -> ModSounds.STATION_ARMORY;
      };
   }

   public static Station byId(String id) {
      for (Station s : values()) {
         if (s.id.equals(id) || s.name().equalsIgnoreCase(id)) {
            return s;
         }
      }

      throw new IllegalArgumentException("unknown station " + id);
   }
}
