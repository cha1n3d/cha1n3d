package com.bobux.vaz2109.quest;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.BossProgress;
import com.bobux.vaz2109.item.ExplorerCompassItem;
import com.bobux.vaz2109.network.Network;
import com.bobux.vaz2109.network.QuestS2C;
import com.bobux.vaz2109.role.Ability;
import com.bobux.vaz2109.role.Roles;
import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class BossQuests {
   private static final String ACTIVE = "vaz2109_quest";
   private static final String BOOK = "vaz2109_quest_book_given";
   private static final Map<ServerPlayer, Pair<BossQuest, Integer>> COMING = new HashMap<>();

   private BossQuests() {
   }

   private static CompoundTag persisted(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted");
   }

   private static void save(Player player, CompoundTag persisted) {
      player.getPersistentData().put("PlayerPersisted", persisted);
   }

   public static boolean beaten(Player player, BossQuest quest) {
      return BossProgress.has(player, quest.key);
   }

   @Nullable
   public static BossQuest active(Player player) {
      String id = persisted(player).getString("vaz2109_quest");

      for (BossQuest q : BossQuest.values()) {
         if (q.id.equals(id)) {
            return q;
         }
      }

      return null;
   }

   @Nullable
   public static BossQuest next(Player player) {
      for (BossQuest q : BossQuest.values()) {
         if (!beaten(player, q)) {
            return q;
         }
      }

      return null;
   }

   public static boolean mayFight(Player player, BossQuest quest) {
      return player.isCreative() || beaten(player, quest) || active(player) == quest;
   }

   public static boolean refuse(LivingEntity boss, DamageSource source, BossQuest quest) {
      if (source.getEntity() instanceof Player p && !mayFight(p, quest)) {
         if (!boss.level().isClientSide) {
            BossQuest next = next(p);
            p.displayClientMessage(
               Component.translatable(
                     "message.vaz2109.quest.locked", new Object[]{Component.translatable("quest.vaz2109." + (next != null ? next.id : quest.id))}
                  )
                  .withStyle(ChatFormatting.GRAY),
               true
            );
         }

         return true;
      }

      return false;
   }

   public static void sync(ServerPlayer player, boolean open) {
      int mask = 0;

      for (BossQuest q : BossQuest.values()) {
         if (beaten(player, q)) {
            mask |= 1 << q.ordinal();
         }
      }

      BossQuest active = active(player);
      Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuestS2C(open, mask, active == null ? -1 : active.ordinal()));
   }

   public static void take(ServerPlayer player, int index) {
      if (index >= 0 && index < BossQuest.values().length) {
         BossQuest quest = BossQuest.values()[index];
         if (quest == next(player)) {
            CompoundTag persisted = persisted(player);
            persisted.putString("vaz2109_quest", quest.id);
            save(player, persisted);
            ServerLevel level = player.serverLevel();
            Component name = Component.translatable("quest.vaz2109." + quest.id).withStyle(ChatFormatting.GOLD);
            player.sendSystemMessage(Component.translatable("message.vaz2109.quest.taken", new Object[]{name}).withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.translatable("quest.vaz2109." + quest.id + ".hint").withStyle(ChatFormatting.GRAY));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.8F);
            if (quest.structure != null) {
               BlockPos pos = locate(level, new ResourceLocation("vaz2109", quest.structure), player.blockPosition());
               if (pos == null) {
                  player.sendSystemMessage(Component.translatable("message.vaz2109.quest.not_found").withStyle(ChatFormatting.GRAY));
               } else {
                  int dist = (int)Math.sqrt(player.blockPosition().distSqr(new BlockPos(pos.getX(), player.getBlockY(), pos.getZ())));
                  player.sendSystemMessage(
                     Component.translatable(
                           "message.vaz2109.quest.where",
                           new Object[]{Component.translatable(ExplorerCompassItem.direction(player.blockPosition(), pos)), dist, pos.getX(), pos.getZ()}
                        )
                        .withStyle(ChatFormatting.GOLD)
                  );
               }
            } else if (!COMING.containsKey(player)
               && level.getEntities((EntityTypeTest)quest.type.get(), player.getBoundingBox().inflate(64.0), Entity::isAlive).isEmpty()) {
               COMING.put(player, Pair.of(quest, 100));
               player.sendSystemMessage(Component.translatable("quest.vaz2109." + quest.id + ".coming").withStyle(ChatFormatting.RED));
            }

            sync(player, true);
         }
      }
   }

   @Nullable
   private static BlockPos locate(ServerLevel level, ResourceLocation id, BlockPos from) {
      Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
      Optional<Reference<Structure>> holder = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, id));
      if (holder.isEmpty()) {
         return null;
      } else {
         Pair<BlockPos, Holder<Structure>> found = level.getChunkSource()
            .getGenerator()
            .findNearestMapStructure(level, HolderSet.direct(new Holder[]{(Holder)holder.get()}), from, 24, false);
         return found == null ? null : ((BlockPos)found.getFirst()).offset(8, 0, 8);
      }
   }

   private static void arrive(ServerPlayer player, BossQuest quest) {
      ServerLevel level = player.serverLevel();
      Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
      if (look.lengthSqr() < 0.01) {
         look = new Vec3(0.0, 0.0, 1.0);
      }

      BlockPos at = BlockPos.containing(player.position().add(look.scale(12.0)));
      at = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, at);
      if (Math.abs(at.getY() - player.getBlockY()) > 12) {
         at = player.blockPosition().offset((int)(look.x * 6.0), 0, (int)(look.z * 6.0));
      }

      if (quest.type.get().create(level) instanceof Mob boss) {
         boss.moveTo((double)at.getX() + 0.5, (double)at.getY(), (double)at.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
         boss.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
         boss.setPersistenceRequired();
         boss.setTarget(player);
         level.addFreshEntity(boss);
         level.playSound(null, at, (SoundEvent)SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 1.0F);
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END && !COMING.isEmpty()) {
         Iterator<Entry<ServerPlayer, Pair<BossQuest, Integer>>> it = COMING.entrySet().iterator();

         while (it.hasNext()) {
            Entry<ServerPlayer, Pair<BossQuest, Integer>> e = it.next();
            ServerPlayer player = e.getKey();
            int left = (Integer)e.getValue().getSecond() - 1;
            if (player.hasDisconnected() || !player.isAlive()) {
               it.remove();
            } else if (left <= 0) {
               it.remove();
               arrive(player, (BossQuest)e.getValue().getFirst());
            } else {
               e.setValue(Pair.of((BossQuest)e.getValue().getFirst(), left));
            }
         }
      }
   }

   /** A boss's death counts for everyone on the server who has that quest taken, wherever they are. */
   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      LivingEntity dead = event.getEntity();
      if (!dead.level().isClientSide && dead.getServer() != null) {
         BossQuest quest = BossQuest.of(dead.getType());
         if (quest != null) {
            Set<ServerPlayer> fighters = new HashSet<>(dead.getServer().getPlayerList().getPlayers());
            if (event.getSource().getEntity() instanceof ServerPlayer killer) {
               fighters.add(killer);
            }

            for (ServerPlayer p : fighters) {
               if (active(p) == quest) {
                  complete(p, quest);
               }
            }
         }
      }
   }

   /** The boss counts as beaten for this player: marked in the book, levels, unlocked abilities, the next quest. */
   public static void complete(ServerPlayer p, BossQuest quest) {
      BossProgress.mark(p, quest.key);
      CompoundTag persisted = persisted(p);
      if (quest.id.equals(persisted.getString("vaz2109_quest"))) {
         persisted.remove("vaz2109_quest");
         save(p, persisted);
      }

      p.giveExperienceLevels(quest.levels);
      p.sendSystemMessage(
         Component.translatable("message.vaz2109.quest.done", new Object[]{Component.translatable("quest.vaz2109." + quest.id)})
            .withStyle(new ChatFormatting[]{ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD})
      );

      for (Ability a : Ability.values()) {
         if (a.boss == quest && Roles.get(p) == a.role && a.unlocked(p)) {
            p.sendSystemMessage(
               Component.translatable("message.vaz2109.ability.unlocked", new Object[]{Component.translatable("ability.vaz2109." + a.id)})
                  .withStyle(ChatFormatting.AQUA)
            );
         }
      }

      BossQuest next = next(p);
      p.sendSystemMessage(
         next == null
            ? Component.translatable("message.vaz2109.quest.all").withStyle(ChatFormatting.GOLD)
            : Component.translatable("message.vaz2109.quest.next", new Object[]{Component.translatable("quest.vaz2109." + next.id)})
               .withStyle(ChatFormatting.YELLOW)
      );
      p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
      Roles.sync(p);
      sync(p, false);
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      Player player = event.getEntity();
      CompoundTag persisted = persisted(player);
      if (!persisted.getBoolean("vaz2109_quest_book_given")) {
         persisted.putBoolean("vaz2109_quest_book_given", true);
         save(player, persisted);
         ItemStack book = new ItemStack((ItemLike)ModRegistry.QUEST_BOOK.get());
         if (!player.getInventory().add(book)) {
            player.drop(book, false);
         }
      }
   }
}
