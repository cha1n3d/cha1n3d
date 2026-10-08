package com.bobux.vaz2109.quest;

import com.bobux.vaz2109.ModRegistry;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public enum BossQuest {
   IVAN("ivan", "ivan_dungeon", "vaz2109_ivan_beaten", ModRegistry.IVAN::get, 15),
   GAISHNIK("gaishnik", "gaishnik_post", "vaz2109_gaishnik_beaten", ModRegistry.GAISHNIK::get, 18),
   JACKET("jacket", "jacket_hideout", "vaz2109_jacket_beaten", ModRegistry.JACKET::get, 20),
   TIMOKHA("timokha", "timokha_dojo", "vaz2109_timokha_beaten", ModRegistry.TIMOKHA::get, 25),
   ANDREY("andrey", "andrey_brewery", "vaz2109_andrey_beaten", ModRegistry.ANDREY::get, 30),
   TOJI("toji", "toji_warehouse", "vaz2109_toji_beaten", ModRegistry.TOJI::get, 35),
   CHOSO("choso", "choso_house", "vaz2109_choso_beaten", ModRegistry.CHOSO::get, 38),
   MAHITO("mahito", "mahito_sewer", "vaz2109_mahito_beaten", ModRegistry.MAHITO::get, 39),
   TIMOKHA_CHAIN("timokha_chain", "chain_arena", "vaz2109_timokha_chain_beaten", ModRegistry.TIMOKHA_CHAIN::get, 40),
   MAHORAGA("mahoraga", "shibuya_ruins", "vaz2109_mahoraga_beaten", ModRegistry.MAHORAGA::get, 45),
   ARTHAS("arthas", "frozen_throne", "vaz2109_arthas_beaten", ModRegistry.ARTHAS::get, 50),
   GOJO("gojo", "jujutsu_high", "vaz2109_gojo_beaten", ModRegistry.GOJO::get, 60);

   public final String id;
   @Nullable
   public final String structure;
   public final String key;
   public final Supplier<EntityType<?>> type;
   public final int levels;

   private BossQuest(String id, @Nullable String structure, String key, Supplier<EntityType<?>> type, int levels) {
      this.id = id;
      this.structure = structure;
      this.key = key;
      this.type = type;
      this.levels = levels;
   }

   @Nullable
   public static BossQuest of(EntityType<?> type) {
      for (BossQuest q : values()) {
         if (q.type.get() == type) {
            return q;
         }
      }

      return null;
   }
}
