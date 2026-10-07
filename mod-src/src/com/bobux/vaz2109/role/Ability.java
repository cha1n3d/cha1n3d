package com.bobux.vaz2109.role;

import com.bobux.vaz2109.entity.curse.SukunaVessel;
import com.bobux.vaz2109.quest.BossQuest;
import com.bobux.vaz2109.quest.BossQuests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public enum Ability {
   BLUE_FISTS(Role.VESSEL, "blue_fists", 400, 3, BossQuest.IVAN, null),
   DISMANTLE(Role.VESSEL, "dismantle", 60, 8, BossQuest.TIMOKHA, null),
   PIERCING_BLOOD(Role.VESSEL, "piercing_blood", 160, 0, BossQuest.CHOSO, Trial.PAINTING),
   REPAIR(Role.MECHANIC, "repair", 900, 0, BossQuest.IVAN, null),
   SHOCK(Role.MECHANIC, "shock", 160, 0, BossQuest.TIMOKHA, null),
   TURRET(Role.MECHANIC, "turret", 900, 0, BossQuest.TOJI, Trial.FULL_TUNE),
   LEAP(Role.FROG, "leap", 100, 0, BossQuest.IVAN, null),
   TONGUE(Role.FROG, "tongue", 60, 0, BossQuest.TIMOKHA, null),
   SWAMP_PACK(Role.FROG, "swamp_pack", 1200, 0, BossQuest.TIMOKHA_CHAIN, Trial.JUMPS),
   FEAST(Role.BREWER, "feast", 1500, 0, BossQuest.JACKET, null),
   KEG(Role.BREWER, "keg", 200, 0, BossQuest.ANDREY, null),
   FIRE_BREATH(Role.BREWER, "fire_breath", 240, 0, BossQuest.MAHORAGA, Trial.BEERS);

   public static final int BLACK_FLASH_FINGERS = 15;
   public final Role role;
   public final String id;
   public final int cooldown;
   public final int fingers;
   @Nullable
   public final BossQuest boss;
   @Nullable
   public final Trial trial;

   private Ability(Role role, String id, int cooldown, int fingers, @Nullable BossQuest boss, @Nullable Trial trial) {
      this.role = role;
      this.id = id;
      this.cooldown = cooldown;
      this.fingers = fingers;
      this.boss = boss;
      this.trial = trial;
   }

   public boolean chargeable() {
      return this == PIERCING_BLOOD;
   }

   public boolean unlocked(Player player) {
      return (this.boss == null || BossQuests.beaten(player, this.boss))
         && SukunaVessel.fingers(player) >= this.fingers
         && (this.trial == null || this.trial.done(player));
   }

   public Component requirement() {
      Component boss = Component.translatable("quest.vaz2109." + (this.boss != null ? this.boss.id : "ivan"));
      Component base = this.fingers > 0
         ? Component.translatable("gui.vaz2109.ability.need_both", new Object[]{boss, this.fingers})
         : Component.translatable("gui.vaz2109.ability.need_boss", new Object[]{boss});
      return this.trial == null ? base : Component.translatable("gui.vaz2109.ability.need_trial", new Object[]{base, this.trial.describe()});
   }

   public static List<Ability> of(Role role) {
      List<Ability> out = new ArrayList<>();

      for (Ability a : values()) {
         if (a.role == role) {
            out.add(a);
         }
      }

      return out;
   }
}
