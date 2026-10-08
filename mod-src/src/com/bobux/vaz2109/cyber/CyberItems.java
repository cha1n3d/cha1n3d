package com.bobux.vaz2109.cyber;

import com.bobux.vaz2109.ModRegistry;
import net.minecraft.world.item.Item;

public final class CyberItems {
   private CyberItems() {
   }

   public static Item item(Implant implant) {
      return ModRegistry.IMPLANTS.get(implant).get();
   }
}
