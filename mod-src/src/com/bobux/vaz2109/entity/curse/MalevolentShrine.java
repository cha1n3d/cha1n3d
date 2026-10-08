package com.bobux.vaz2109.entity.curse;

import com.bobux.vaz2109.entity.BossResistance;
import com.bobux.vaz2109.fx.ModSounds;
import com.bobux.vaz2109.network.CursedFxS2C;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "vaz2109"
)
public final class MalevolentShrine {
   public static final int DURATION = 300;
   public static final double RADIUS = 16.0;
   private static final List<MalevolentShrine.Domain> DOMAINS = new ArrayList<>();

   private MalevolentShrine() {
   }

   public static boolean isShrineBlock(ServerLevel level, BlockPos pos) {
      for (MalevolentShrine.Domain d : DOMAINS) {
         if (d.level == level && d.placed.containsKey(pos)) {
            return true;
         }
      }

      return false;
   }

   public static void open(ServerPlayer caster, int fingers) {
      ServerLevel level = caster.serverLevel();
      MalevolentShrine.Domain d = new MalevolentShrine.Domain(caster, level, caster.position(), level.getGameTime() + 300L, fingers);
      Vec3 back = caster.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-9.0);
      BlockPos origin = BlockPos.containing(caster.position().add(back));
      origin = level.getHeightmapPos(Types.MOTION_BLOCKING_NO_LEAVES, origin);
      if (Math.abs(origin.getY() - caster.getBlockY()) > 6) {
         origin = new BlockPos(origin.getX(), caster.getBlockY(), origin.getZ());
      }

      build(d, origin);
      DOMAINS.add(d);
      CursedFxS2C.send(level, 14, caster, null, d.center, new Vec3(16.0, 0.0, 0.0), 300.0F);
      ModSounds.play(level, caster.position(), 2.0F, ModSounds.BLACK_FLASH, ModSounds.OVERTIME_BELL);
      level.playSound(null, origin, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 2.0F, 0.6F);
      Component text = Component.translatable("message.vaz2109.sukuna.shrine").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD});

      for (Player p : level.getEntitiesOfClass(Player.class, caster.getBoundingBox().inflate(48.0))) {
         p.sendSystemMessage(text);
      }
   }

   private static void put(MalevolentShrine.Domain d, BlockPos origin, int x, int y, int z, BlockState state) {
      BlockPos p = origin.offset(x, y, z);
      BlockState old = d.level.getBlockState(p);
      if (old.canBeReplaced() && !old.hasBlockEntity() && !d.placed.containsKey(p)) {
         d.level.setBlock(p, state, 2);
         d.placed.put(p.immutable(), new BlockState[]{state, old});
      }
   }

   private static void build(MalevolentShrine.Domain d, BlockPos o) {
      BlockState brick = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
      BlockState chisel = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
      BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
      BlockState plank = Blocks.DARK_OAK_PLANKS.defaultBlockState();
      BlockState red = Blocks.RED_NETHER_BRICKS.defaultBlockState();
      BlockState tile = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
      BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
      BlockState skull = Blocks.SKELETON_SKULL.defaultBlockState();
      BlockState wither = Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
      BlockState lantern = (BlockState)Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

      for (int x = -4; x <= 4; x++) {
         for (int z = -4; z <= 4; z++) {
            boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4;
            put(d, o, x, 0, z, Math.abs(x) == 4 && Math.abs(z) == 4 ? chisel : brick);
            if (edge && Math.abs(x) == 4 && Math.abs(z) == 4) {
               put(d, o, x, 1, z, (BlockState)skull.setValue(SkullBlock.ROTATION, (x > 0 ? 6 : 10) + (z > 0 ? 0 : (x > 0 ? -4 : 4))));
            } else if (edge && (x + z) % 2 == 0) {
               put(d, o, x, 1, z, bone);
            }
         }
      }

      for (int y = 1; y <= 6; y++) {
         for (int sx = -3; sx <= 3; sx += 6) {
            for (int sz = -3; sz <= 3; sz += 6) {
               put(d, o, sx, y, sz, log);
            }
         }
      }

      for (int i = -3; i <= 3; i++) {
         put(d, o, i, 7, -3, plank);
         put(d, o, i, 7, 3, plank);
         put(d, o, -3, 7, i, plank);
         put(d, o, 3, 7, i, plank);
      }

      for (int i = -2; i <= 2; i++) {
         if (i % 2 == 0) {
            put(d, o, i, 6, -3, bone);
            put(d, o, i, 6, 3, bone);
            put(d, o, -3, 6, i, bone);
            put(d, o, 3, 6, i, bone);
         } else {
            put(d, o, i, 1, -3, bone);
            put(d, o, i, 1, 3, bone);
            put(d, o, -3, 1, i, bone);
            put(d, o, 3, 1, i, bone);
         }
      }

      put(d, o, 0, 1, 0, red);
      put(d, o, 0, 2, 0, wither);
      put(d, o, 1, 1, 0, (BlockState)skull.setValue(SkullBlock.ROTATION, 4));
      put(d, o, -1, 1, 0, (BlockState)skull.setValue(SkullBlock.ROTATION, 12));
      put(d, o, 0, 1, 1, skull);
      put(d, o, 0, 1, -1, (BlockState)skull.setValue(SkullBlock.ROTATION, 8));
      int[][] tiers = new int[][]{{8, 5, 0}, {9, 4, 1}, {10, 3, 0}, {11, 2, 1}, {12, 1, 0}};

      for (int[] t : tiers) {
         for (int x = -t[1]; x <= t[1]; x++) {
            for (int zx = -t[1]; zx <= t[1]; zx++) {
               put(d, o, x, t[0], zx, t[2] == 0 ? tile : (Math.abs(x) != t[1] && Math.abs(zx) != t[1] ? plank : red));
            }
         }
      }

      put(d, o, 0, 13, 0, bone);
      put(d, o, 0, 14, 0, bone);
      put(d, o, 0, 15, 0, wither);

      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sz = -1; sz <= 1; sz += 2) {
            put(d, o, 6 * sx, 8, 6 * sz, bone);
            put(d, o, 7 * sx, 9, 7 * sz, bone);
            put(d, o, 7 * sx, 10, 7 * sz, bone);
            put(d, o, 5 * sx, 7, 5 * sz, lantern);
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END && !DOMAINS.isEmpty()) {
         Iterator<MalevolentShrine.Domain> it = DOMAINS.iterator();

         while (it.hasNext()) {
            MalevolentShrine.Domain d = it.next();
            Player caster = d.caster;
            long now = d.level.getGameTime();
            if (now < d.end && !caster.isRemoved() && caster.isAlive()) {
               tick(d, caster, now);
            } else {
               close(d);
               it.remove();
            }
         }
      }
   }

   private static void tick(MalevolentShrine.Domain d, Player caster, long now) {
      if (d.grand) {
         tickGrand(d, caster, now);
         return;
      }

      ServerLevel level = d.level;
      if (now % 5L == 0L) {
         float damage = 2.0F + 0.1F * (float)d.fingers;
         boolean any = false;

         for (LivingEntity e : level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(d.center, d.center).inflate(16.0, 8.0, 16.0),
            ex -> ex != caster && ex.isAlive() && !ex.isSpectator() && (!(ex instanceof Player px) || !px.isCreative())
         )) {
            if (!(e.position().distanceTo(d.center) > 16.0)) {
               e.invulnerableTime = 0;
               if (e.hurt(BossResistance.sorcery(caster), e instanceof Player ? damage * 0.5F : damage)) {
                  any = true;
                  level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + (double)e.getBbHeight() * 0.5, e.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
               }
            }
         }

         if (any) {
            level.playSound(
               null, d.center.x, d.center.y, d.center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.5F + level.random.nextFloat() * 0.6F
            );
         }
      }

      for (int i = 0; i < 3; i++) {
         double ang = level.random.nextDouble() * Math.PI * 2.0;
         double r = Math.sqrt(level.random.nextDouble()) * 16.0;
         int x = (int)Math.floor(d.center.x + Math.cos(ang) * r);
         int z = (int)Math.floor(d.center.z + Math.sin(ang) * r);
         int top = level.getHeight(Types.MOTION_BLOCKING, x, z) - 1;
         if (Math.abs((double)top - d.center.y) < 10.0 && caster instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)caster;
            BlockPos p = new BlockPos(x, top, z);
            if (SukunaVessel.cut(level, p, sp)) {
               level.sendParticles(ParticleTypes.SWEEP_ATTACK, (double)x + 0.5, (double)top + 0.8, (double)z + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private static void close(MalevolentShrine.Domain d) {
      for (Entry<BlockPos, BlockState[]> e : d.placed.entrySet()) {
         if (d.level.getBlockState(e.getKey()) == e.getValue()[0]) {
            d.level.setBlock(e.getKey(), e.getValue()[1], 2);
         }
      }

      d.level.playSound(null, d.center.x, d.center.y, d.center.z, SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 2.0F, 0.6F);
   }

   @SubscribeEvent
   public static void onStopping(ServerStoppingEvent event) {
      for (MalevolentShrine.Domain d : DOMAINS) {
         close(d);
      }

      DOMAINS.clear();
   }

   // ---- the awakened Sukuna's domain: radius 50, cuts everything -------------------------------------

   public static final double GRAND_RADIUS = 50.0;
   /** How far below the shrine's floor the cutting goes. */
   private static final int GRAND_DEPTH = 10;
   private static int[] grandOffsets;

   /** Offsets of the domain sphere (above GRAND_DEPTH below the floor), nearest first, packed 7 bits per axis. */
   private static int[] grandOffsets() {
      if (grandOffsets == null) {
         int r = (int)GRAND_RADIUS;
         int[] count = new int[r * r + 1];
         for (int x = -r; x <= r; x++) {
            for (int y = -GRAND_DEPTH; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  int d = x * x + y * y + z * z;
                  if (d <= r * r) {
                     count[d]++;
                  }
               }
            }
         }

         int[] startAt = new int[count.length];
         int total = 0;
         for (int i = 0; i < count.length; i++) {
            startAt[i] = total;
            total += count[i];
         }

         int[] out = new int[total];
         for (int x = -r; x <= r; x++) {
            for (int y = -GRAND_DEPTH; y <= r; y++) {
               for (int z = -r; z <= r; z++) {
                  int d = x * x + y * y + z * z;
                  if (d <= r * r) {
                     out[startAt[d]++] = (x + 64) << 14 | (y + 64) << 7 | z + 64;
                  }
               }
            }
         }

         grandOffsets = out;
      }

      return grandOffsets;
   }

   public static void openGrand(ServerPlayer caster, int fingers, BlockPos ground, int duration) {
      ServerLevel level = caster.serverLevel();
      Vec3 center = Vec3.atBottomCenterOf(ground);
      MalevolentShrine.Domain d = new MalevolentShrine.Domain(caster, level, center, level.getGameTime() + (long)duration, fingers);
      d.grand = true;
      d.ground = ground;
      d.start = level.getGameTime();
      build(d, ground);
      DOMAINS.add(d);
      CursedFxS2C.send(level, 14, caster, null, center, new Vec3(GRAND_RADIUS, 0.0, 0.0), (float)duration);
      ModSounds.play(level, caster.position(), 4.0F, ModSounds.BLACK_FLASH, ModSounds.OVERTIME_BELL);
      level.playSound(null, ground, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 4.0F, 0.5F);
      level.playSound(null, ground, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 4.0F, 0.6F);
      Component text = Component.translatable("message.vaz2109.sukuna.shrine").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD});

      for (Player p : level.players()) {
         if (p.position().distanceTo(center) < GRAND_RADIUS + 30.0) {
            p.sendSystemMessage(text);
         }
      }
   }

   private static boolean target(LivingEntity e, Player caster) {
      return e != caster && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
   }

   private static List<LivingEntity> inside(MalevolentShrine.Domain d, Player caster) {
      List<LivingEntity> list = new ArrayList<>();
      for (LivingEntity e : d.level.getEntitiesOfClass(LivingEntity.class, new AABB(d.center, d.center).inflate(GRAND_RADIUS), e -> target(e, caster))) {
         if (e.position().distanceTo(d.center) <= GRAND_RADIUS) {
            list.add(e);
         }
      }

      return list;
   }

   /** The toughest thing in the caster's domain (for the closing Fuga). */
   @org.jetbrains.annotations.Nullable
   public static LivingEntity strongestIn(Player caster, double radius) {
      LivingEntity best = null;
      for (LivingEntity e : caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius), e -> target(e, caster))) {
         if (best == null || e.getMaxHealth() > best.getMaxHealth()) {
            best = e;
         }
      }

      return best;
   }

   /** A hit of the music inside the domain: a volley of slashes. */
   public static void beat(ServerPlayer caster, boolean big) {
      for (MalevolentShrine.Domain d : DOMAINS) {
         if (d.grand && d.caster == caster) {
            ServerLevel level = d.level;
            List<LivingEntity> in = inside(d, caster);
            int slashes = big ? 6 : 2;
            for (int i = 0; i < slashes && !in.isEmpty(); i++) {
               LivingEntity e = in.get(level.random.nextInt(in.size()));
               Vec3 c = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
               Vec3 side = new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.6, level.random.nextGaussian()).normalize().scale(2.0 + e.getBbWidth());
               CursedFxS2C.send(level, 11, caster, e, c.add(side), c.subtract(side), big ? 0.45F : 0.3F);
               if (big) {
                  e.invulnerableTime = 0;
                  e.hurt(BossResistance.sorcery(caster), e instanceof Player ? 2.0F : 6.0F + 0.3F * (float)d.fingers);
               }
            }

            for (int i = 0; i < (big ? 8 : 3); i++) {
               double a = level.random.nextDouble() * Math.PI * 2.0;
               double r = Math.sqrt(level.random.nextDouble()) * GRAND_RADIUS;
               Vec3 p = d.center.add(Math.cos(a) * r, 1.0 + level.random.nextDouble() * 12.0, Math.sin(a) * r);
               Vec3 side = new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()).normalize().scale(4.0);
               CursedFxS2C.send(level, 11, caster, null, p.add(side), p.subtract(side), 0.3F);
            }

            level.playSound(null, d.center.x, d.center.y + 4.0, d.center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, big ? 4.0F : 2.0F, 0.5F + level.random.nextFloat() * 0.5F);
            if (big) {
               ModSounds.play(level, d.center, 3.0F, ModSounds.SOUL_CUT);
            }
         }
      }
   }

   private static void tickGrand(MalevolentShrine.Domain d, Player caster, long now) {
      ServerLevel level = d.level;
      if (now % 5L == 0L) {
         boolean players = now % 10L == 0L;
         float damage = 5.0F + 0.25F * (float)d.fingers;
         for (LivingEntity e : inside(d, caster)) {
            if (e instanceof Player && !players) {
               continue;
            }

            e.invulnerableTime = 0;
            if (e.hurt(BossResistance.sorcery(caster), e instanceof Player ? 1.5F : damage)) {
               level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
            }
         }
      }

      if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING) || !(caster instanceof ServerPlayer sp)) {
         return;
      }

      // carve the sphere from the centre outwards, finishing a little before the domain closes
      int[] offsets = grandOffsets();
      long left = Math.max(1L, d.end - now - 40L);
      int examine = (int)Math.min(6000L, Math.max(300L, (offsets.length - d.cursor) / left + 1L));
      int broken = 0;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int i = 0; i < examine && d.cursor < offsets.length && broken < 1500; i++) {
         int o = offsets[d.cursor++];
         int x = (o >> 14 & 127) - 64;
         int y = (o >> 7 & 127) - 64;
         int z = (o & 127) - 64;
         if (y < 0 && Math.abs(x) <= 5 && Math.abs(z) <= 5) {
            continue; // keep the shrine's footing
         }

         pos.set(d.ground.getX() + x, d.ground.getY() + y, d.ground.getZ() + z);
         if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) {
            continue;
         }

         BlockState state = level.getBlockState(pos);
         if (state.isAir() || state.hasBlockEntity() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock || state.getDestroySpeed(level, pos) < 0.0F) {
            continue;
         }

         if (d.placed.containsKey(pos) || net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, pos.immutable(), state, sp))) {
            continue;
         }

         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 50);
         if (++broken % 60 == 0) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static final class Domain {
      final Player caster;
      final ServerLevel level;
      final Vec3 center;
      final long end;
      final int fingers;
      final Map<BlockPos, BlockState[]> placed = new HashMap<>();

      boolean grand;
      BlockPos ground;
      int cursor;
      long start;

      Domain(Player caster, ServerLevel level, Vec3 center, long end, int fingers) {
         this.caster = caster;
         this.level = level;
         this.center = center;
         this.end = end;
         this.fingers = fingers;
      }
   }
}
