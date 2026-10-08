package com.bobux.vaz2109.role;

import com.bobux.vaz2109.entity.BossProgress;
import com.bobux.vaz2109.entity.curse.SukunaVessel;
import com.bobux.vaz2109.item.DeathPaintingItem;
import com.bobux.vaz2109.quest.BossQuest;
import com.bobux.vaz2109.quest.BossQuests;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Test helpers merged into /vazclass (operators only):
 * unlock / lock (every ability regardless of conditions), cooldowns, fingers N, trials, bosses.
 */
@EventBusSubscriber(modid = "vaz2109")
public final class TestCommands {
   public static final String UNLOCK = "vaz2109_test_unlock";

   private TestCommands() {
   }

   public static boolean unlockedForTests(net.minecraft.world.entity.player.Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted").getBoolean(UNLOCK);
   }

   private static void flag(ServerPlayer player, boolean on) {
      CompoundTag data = player.getPersistentData();
      CompoundTag persisted = data.getCompound("PlayerPersisted");
      if (on) {
         persisted.putBoolean(UNLOCK, true);
      } else {
         persisted.remove(UNLOCK);
      }

      data.put("PlayerPersisted", persisted);
   }

   private static void cooldowns(ServerPlayer player) {
      for (Ability a : Ability.values()) {
         player.getPersistentData().remove("vaz2109Cd_" + a.id);
      }
   }

   @SubscribeEvent
   public static void onCommands(RegisterCommandsEvent event) {
      event.getDispatcher()
         .register(
            Commands.literal("vazclass")
               .then(each("unlock", "commands.vaz2109.test.unlock", p -> {
                  flag(p, true);
                  cooldowns(p);
               }))
               .then(each("lock", "commands.vaz2109.test.lock", p -> flag(p, false)))
               .then(each("cooldowns", "commands.vaz2109.test.cooldowns", TestCommands::cooldowns))
               .then(each("trials", "commands.vaz2109.test.trials", p -> {
                  for (Trial t : Trial.values()) {
                     t.add(p, t.goal);
                  }

                  DeathPaintingItem.apply(p);
               }))
               .then(each("bosses", "commands.vaz2109.test.bosses", p -> {
                  for (BossQuest q : BossQuest.values()) {
                     BossProgress.mark(p, q.key);
                  }

                  BossQuests.sync(p, false);
               }))
               .then(each("awaken", "commands.vaz2109.test.awaken", p -> {
                  if (SukunaVessel.fingers(p) < 15) {
                     SukunaVessel.setFingers(p, 15);
                  }

                  com.bobux.vaz2109.entity.curse.SukunaAwakening.start(p);
               }))
               .then(
                  Commands.literal("boss")
                     .requires(s -> s.hasPermission(2))
                     .then(
                        Commands.argument("boss", com.mojang.brigadier.arguments.StringArgumentType.word())
                           .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                              java.util.Arrays.stream(BossQuest.values()).map(q -> q.id), b))
                           .executes(c -> boss(c, c.getSource().getServer().getPlayerList().getPlayers()))
                           .then(Commands.argument("targets", EntityArgument.players()).executes(c -> boss(c, EntityArgument.getPlayers(c, "targets"))))
                     )
               )
               .then(
                  Commands.literal("fingers")
                     .requires(s -> s.hasPermission(2))
                     .then(
                        Commands.argument("count", IntegerArgumentType.integer(0, 20))
                           .executes(c -> fingers(c, List.of(c.getSource().getPlayerOrException())))
                           .then(Commands.argument("targets", EntityArgument.players()).executes(c -> fingers(c, EntityArgument.getPlayers(c, "targets"))))
                     )
               )
         );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> each(String name, String message, Consumer<ServerPlayer> action) {
      return Commands.literal(name)
         .requires(s -> s.hasPermission(2))
         .executes(c -> run(c, List.of(c.getSource().getPlayerOrException()), message, action))
         .then(Commands.argument("targets", EntityArgument.players()).executes(c -> run(c, EntityArgument.getPlayers(c, "targets"), message, action)));
   }

   private static int run(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players, String message, Consumer<ServerPlayer> action) {
      for (ServerPlayer p : players) {
         action.accept(p);
         Roles.sync(p);
      }

      c.getSource().sendSuccess(() -> Component.translatable(message, new Object[]{players.size()}), true);
      return players.size();
   }

   /** The given boss counts as killed for these players (everyone online by default), with the same reward as a real kill. */
   private static int boss(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
      String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "boss");
      BossQuest quest = null;
      for (BossQuest q : BossQuest.values()) {
         if (q.id.equals(id)) {
            quest = q;
         }
      }

      if (quest == null) {
         throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.translatable("commands.vaz2109.test.boss.unknown", new Object[]{id})).create();
      }

      int done = 0;
      int already = 0;
      for (ServerPlayer p : players) {
         if (BossQuests.beaten(p, quest)) {
            already++;
         } else {
            BossQuests.complete(p, quest);
            done++;
         }
      }

      Component name = Component.translatable("quest.vaz2109." + quest.id);
      int n = done;
      int a = already;
      c.getSource().sendSuccess(() -> Component.translatable("commands.vaz2109.test.boss", new Object[]{name, n, a}), true);
      return done;
   }

   private static int fingers(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) throws CommandSyntaxException {
      int n = IntegerArgumentType.getInteger(c, "count");

      for (ServerPlayer p : players) {
         SukunaVessel.setFingers(p, n);
         Roles.sync(p);
      }

      c.getSource().sendSuccess(() -> Component.translatable("commands.vaz2109.test.fingers", new Object[]{n, players.size()}), true);
      return players.size();
   }
}
