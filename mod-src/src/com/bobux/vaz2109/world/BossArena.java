package com.bobux.vaz2109.world;

import com.bobux.vaz2109.ModRegistry;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

public enum BossArena {
   GAISHNIK_POST("gaishnik_post", ModRegistry.GAISHNIK::get, 16),
   JACKET_HIDEOUT("jacket_hideout", ModRegistry.JACKET::get, 16),
   TOJI_WAREHOUSE("toji_warehouse", ModRegistry.TOJI::get, 16),
   SHIBUYA_RUINS("shibuya_ruins", ModRegistry.MAHORAGA::get, 17),
   JUJUTSU_HIGH("jujutsu_high", ModRegistry.GOJO::get, 18),
   CHOSO_HOUSE("choso_house", ModRegistry.CHOSO::get, 15),
   MAHITO_SEWER("mahito_sewer", ModRegistry.MAHITO::get, 14);

   public final String id;
   public final Supplier<EntityType<?>> boss;
   public final int radius;

   private BossArena(String id, Supplier<EntityType<?>> boss, int radius) {
      this.id = id;
      this.boss = boss;
      this.radius = radius;
   }
}
