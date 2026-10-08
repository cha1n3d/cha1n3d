package com.bobux.vaz2109.entity.curse;

import com.bobux.vaz2109.network.AwakeningS2C;
import com.bobux.vaz2109.network.Network;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

/**
 * The 15th finger: Sukuna fully awakens for two minutes, set to the awakening track. He is nearly untouchable,
 * every hit of the music pulses through the area, then he flies into the sky (cutscene), opens the Malevolent
 * Shrine on the drop (radius 50, cuts everything), fires a full-power Fuga near the end of the domain and leaves.
 * Afterwards the Vessel keeps a passive Speed I. Timeline in ticks from the start of the track.
 */
@EventBusSubscriber(modid = "vaz2109")
public final class SukunaAwakening {
   public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "vaz2109");
   public static final RegistryObject<SoundEvent> TRACK = SOUNDS.register(
      "music.sukuna_awakening", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("vaz2109", "music.sukuna_awakening"))
   );
   public static final String AWAKENED = "vaz2109_sukuna_awakened";
   /** The cutscene: rising into the sky (124 ticks before the drop). */
   public static final int RISE = 900;
   public static final int HOVER = 960;
   /** The drop of the track: Domain Expansion. */
   public static final int DOMAIN = 1024;
   public static final int FUGA_CHARGE = 2250;
   public static final int FUGA = 2300;
   public static final int DOMAIN_END = 2360;
   /** Two minutes of possession, ending with the music. */
   public static final int LENGTH = 2440;
   public static final double HEARING = 96.0;
   private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.75F, 0.02F, 0.04F), 2.0F);
   private static final Map<UUID, SukunaAwakening.State> ACTIVE = new HashMap<>();

   private SukunaAwakening() {
   }

   private static CompoundTag persisted(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted");
   }

   /** The Vessel has gone through the awakening (passive Speed I from then on). */
   public static boolean awakened(Player player) {
      return persisted(player).getBoolean(AWAKENED);
   }

   public static boolean active(Player player) {
      return ACTIVE.containsKey(player.getUUID());
   }

   /** True while the cutscene runs, so the normal possession brain holds still. */
   public static boolean busy(Player player) {
      SukunaAwakening.State s = ACTIVE.get(player.getUUID());
      return s != null && s.t >= RISE && s.t < DOMAIN + 20;
   }

   public static void start(ServerPlayer player) {
      if (active(player)) {
         return;
      }

      CompoundTag tag = persisted(player);
      tag.putBoolean(AWAKENED, true);
      player.getPersistentData().put("PlayerPersisted", tag);
      SukunaAwakening.State s = new SukunaAwakening.State();
      ACTIVE.put(player.getUUID(), s);
      SukunaVessel.possess(player, LENGTH);
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, LENGTH, 1, false, false));
      ServerLevel level = player.serverLevel();

      for (ServerPlayer p : level.players()) {
         if (p.distanceTo(player) < HEARING) {
            s.listeners.add(p.getUUID());
            p.connection.send(new ClientboundSoundEntityPacket(ForgeRegistries.SOUND_EVENTS.getHolder(TRACK.get()).orElseThrow(), SoundSource.RECORDS, p, 1.0F, 1.0F, 0L));
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new AwakeningS2C(AwakeningS2C.TRACK, player.getId(), 0.0F));
            p.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.awakening", new Object[]{player.getDisplayName()}).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
         }
      }
   }

   private static void stop(ServerPlayer player, SukunaAwakening.State s) {
      ACTIVE.remove(player.getUUID());
      player.removeEffect(MobEffects.LEVITATION);
      player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
      if (player.getServer() != null) {
         for (UUID id : s.listeners) {
            ServerPlayer p = player.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
               p.connection.send(new ClientboundStopSoundPacket(TRACK.getId(), SoundSource.RECORDS));
            }
         }
      }
   }

   private static boolean contains(int[] ticks, int t) {
      for (int x : ticks) {
         if (x == t) {
            return true;
         }

         if (x > t) {
            return false;
         }
      }

      return false;
   }

   /** A hit of the music: the shockwave goes out from Sukuna, stronger once the domain is open. */
   private static void pulse(ServerPlayer player, ServerLevel level, SukunaAwakening.State s, boolean big) {
      boolean domain = s.t >= DOMAIN && s.t < DOMAIN_END;
      float strength = (big ? 0.7F : 0.3F) * (domain ? 1.6F : 1.0F);

      for (ServerPlayer p : level.players()) {
         double d = p.distanceTo(player);
         if (d < 64.0) {
            float near = (float)Math.max(0.15, 1.0 - d / 64.0);
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new AwakeningS2C(AwakeningS2C.PULSE, player.getId(), strength * near));
         }
      }

      Vec3 c = player.position().add(0.0, 1.0, 0.0);
      int ring = big ? 36 : 14;

      for (int i = 0; i < ring; i++) {
         double a = Math.PI * 2.0 * (double)i / (double)ring;
         double r = big ? 2.5 : 1.5;
         level.sendParticles(RED, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
      }

      if (big) {
         level.sendParticles(CurseEntity.CURSED_ENERGY_DARK, c.x, c.y, c.z, 20, 0.6, 0.8, 0.6, 0.02);
      }

      if (domain) {
         MalevolentShrine.beat(player, big);
      }
   }

   private static void cutscene(ServerPlayer player, ServerLevel level) {
      for (ServerPlayer p : level.players()) {
         if (p.distanceTo(player) < 64.0) {
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new AwakeningS2C(AwakeningS2C.CUTSCENE, player.getId(), (float)(DOMAIN - RISE)));
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         SukunaAwakening.State s = ACTIVE.get(player.getUUID());
         if (s != null) {
            if (!player.isAlive() || !SukunaVessel.possessed(player)) {
               stop(player, s);
               if (player.isAlive()) {
                  player.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.awakening_end").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
               }
            } else {
               ServerLevel level = player.serverLevel();
               int t = s.t++;
               if (contains(SukunaTrack.BIG, t)) {
                  pulse(player, level, s, true);
               } else if (contains(SukunaTrack.SMALL, t)) {
                  pulse(player, level, s, false);
               }

               if (t == RISE) {
                  cutscene(player, level);
                  player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, HOVER - RISE, 6, false, false));
                  SukunaVessel.charge(player, DOMAIN - RISE);
               } else if (t == HOVER) {
                  // amplifier 255 reaches the client as -1: levitation that only holds the height
                  player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DOMAIN_END - HOVER, 255, false, false));
               } else if (t == DOMAIN) {
                  SukunaVessel.cancelCharge(player);
                  BlockPos ground = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
                  MalevolentShrine.openGrand(player, SukunaVessel.fingers(player), ground, DOMAIN_END - DOMAIN);
               } else if (t == FUGA_CHARGE) {
                  SukunaVessel.charge(player, FUGA - FUGA_CHARGE);
                  for (ServerPlayer p : level.players()) {
                     if (p.distanceTo(player) < 64.0) {
                        p.sendSystemMessage(Component.translatable("message.vaz2109.sukuna.fuga_open").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
                     }
                  }
               } else if (t == FUGA) {
                  Vec3 eye = player.getEyePosition();
                  LivingEntity target = MalevolentShrine.strongestIn(player, 50.0);
                  Vec3 aim = target != null
                     ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
                     : Vec3.atCenterOf(level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition().offset(6, 0, 6)));
                  SukunaVessel.fuga(player, eye, aim.subtract(eye), 3.0F, SukunaVessel.fingers(player));
               } else if (t == DOMAIN_END) {
                  player.removeEffect(MobEffects.LEVITATION);
                  player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
               }
            }
         }
      }
   }

   /** Almost nothing gets through to Sukuna while he is awake. */
   @SubscribeEvent(priority = EventPriority.LOW)
   public static void onHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && active(player) && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         event.setAmount(event.getAmount() * 0.08F);
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         SukunaAwakening.State s = ACTIVE.get(player.getUUID());
         if (s != null) {
            stop(player, s);
         }
      }
   }

   private static final class State {
      int t;
      final List<UUID> listeners = new ArrayList<>();
   }
}
