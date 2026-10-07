package com.bobux.vaz2109.role;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Extra unlock conditions for the third class abilities: counters and one-time feats kept in persisted player data. */
public enum Trial {
   PAINTING("vaz2109_trial_painting", 1, 0),
   FULL_TUNE("vaz2109_trial_full_tune", 1, 0),
   JUMPS("vaz2109_trial_jumps", 300, 50),
   BEERS("vaz2109_trial_beers", 30, 5);

   public final String key;
   public final int goal;
   private final int report;

   private Trial(String key, int goal, int report) {
      this.key = key;
      this.goal = goal;
      this.report = report;
   }

   private static CompoundTag persisted(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted");
   }

   public int progress(Player player) {
      return persisted(player).getInt(this.key);
   }

   public boolean done(Player player) {
      return this.progress(player) >= this.goal;
   }

   public Component describe() {
      return Component.translatable("gui.vaz2109.trial." + this.name().toLowerCase(), new Object[]{this.goal});
   }

   /** Adds to the counter; reports milestones and announces the unlocked ability when the goal is reached. */
   public void add(ServerPlayer player, int amount) {
      int before = this.progress(player);
      if (before < this.goal) {
         int now = Math.min(this.goal, before + amount);
         CompoundTag data = player.getPersistentData();
         CompoundTag persisted = data.getCompound("PlayerPersisted");
         persisted.putInt(this.key, now);
         data.put("PlayerPersisted", persisted);
         Role role = Roles.get(player);
         boolean relevant = false;

         for (Ability a : Ability.values()) {
            if (a.trial == this && a.role == role) {
               relevant = true;
               if (now >= this.goal && a.unlocked(player)) {
                  player.sendSystemMessage(
                     Component.translatable("message.vaz2109.ability.unlocked", new Object[]{Component.translatable("ability.vaz2109." + a.id)})
                        .withStyle(ChatFormatting.AQUA)
                  );
               }
            }
         }

         if (relevant && this.report > 0 && now < this.goal && now / this.report != before / this.report) {
            player.displayClientMessage(
               Component.translatable("message.vaz2109.trial.progress", new Object[]{this.describe(), now, this.goal}).withStyle(ChatFormatting.GRAY), true
            );
         }

         if (now >= this.goal) {
            Roles.sync(player);
         }
      }
   }
}
