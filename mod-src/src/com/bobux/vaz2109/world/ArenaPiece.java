package com.bobux.vaz2109.world;

import com.bobux.vaz2109.ModRegistry;
import com.bobux.vaz2109.entity.VazEntity;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class ArenaPiece extends StructurePiece {
   public static final ResourceLocation LOOT = new ResourceLocation("vaz2109", "chests/boss_arena");
   private static final int CLEAR = 24;
   private static final int FOUNDATION = 14;
   private final BossArena arena;
   private final BlockPos center;
   private WorldGenLevel level;
   private BoundingBox box;
   private RandomSource random;

   public ArenaPiece(BossArena arena, BlockPos center) {
      super(
         (StructurePieceType)ModStructures.ARENA_PIECE.get(),
         0,
         new BoundingBox(
            center.getX() - arena.radius,
            center.getY() - 14,
            center.getZ() - arena.radius,
            center.getX() + arena.radius,
            center.getY() + 24,
            center.getZ() + arena.radius
         )
      );
      this.arena = arena;
      this.center = center;
      this.setOrientation(null);
   }

   public ArenaPiece(CompoundTag tag) {
      super((StructurePieceType)ModStructures.ARENA_PIECE.get(), tag);
      this.arena = BossArena.values()[Mth.clamp(tag.getInt("Arena"), 0, BossArena.values().length - 1)];
      this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
      tag.putInt("Arena", this.arena.ordinal());
      tag.putInt("CX", this.center.getX());
      tag.putInt("CY", this.center.getY());
      tag.putInt("CZ", this.center.getZ());
   }

   public void postProcess(
      WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos pivot
   ) {
      this.level = level;
      this.box = box;
      this.random = random;
      switch (this.arena) {
         case GAISHNIK_POST:
            this.post();
            break;
         case JACKET_HIDEOUT:
            this.hideout();
            break;
         case TOJI_WAREHOUSE:
            this.warehouse();
            break;
         case SHIBUYA_RUINS:
            this.ruins();
            break;
         case JUJUTSU_HIGH:
            this.college();
            break;
         case CHOSO_HOUSE:
            this.bloodHouse();
            break;
         case MAHITO_SEWER:
            this.sewer();
      }

      this.level = null;
      this.box = null;
      this.random = null;
   }

   private void put(int dx, int dy, int dz, BlockState state) {
      BlockPos pos = this.center.offset(dx, dy, dz);
      if (this.box.isInside(pos)) {
         this.level.setBlock(pos, state, 2);
      }
   }

   private void put(int dx, int dy, int dz, Block block) {
      this.put(dx, dy, dz, block.defaultBlockState());
   }

   private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
      for (int x = x0; x <= x1; x++) {
         for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
               this.put(x, y, z, state);
            }
         }
      }
   }

   private double noise(int dx, int dy, int dz) {
      long s = Mth.getSeed(this.center.getX() + dx, this.center.getY() + dy, this.center.getZ() + dz);
      return (double)(s >>> 16 & 65535L) / 65536.0;
   }

   private static BlockState slab(Block slab, SlabType type) {
      return (BlockState)slab.defaultBlockState().setValue(SlabBlock.TYPE, type);
   }

   private static BlockState pane(Block pane, boolean alongX) {
      return (BlockState)((BlockState)pane.defaultBlockState().setValue(alongX ? CrossCollisionBlock.EAST : CrossCollisionBlock.NORTH, true))
         .setValue(alongX ? CrossCollisionBlock.WEST : CrossCollisionBlock.SOUTH, true);
   }

   private void yard(BiFunction<Integer, Integer, BlockState> top) {
      int r = this.arena.radius;

      for (int x = -r; x <= r; x++) {
         for (int z = -r; z <= r; z++) {
            if (this.box.isInside(this.center.offset(x, 0, z))
               && !((double)(x * x + z * z) > ((double)r + 0.5) * ((double)r + 0.5) - this.noise(x, 0, z) * 30.0)) {
               for (int y = 1; y <= 24; y++) {
                  if (!this.level.getBlockState(this.center.offset(x, y, z)).isAir()) {
                     this.put(x, y, z, Blocks.AIR.defaultBlockState());
                  }
               }

               for (int yx = -1; yx >= -14; yx--) {
                  BlockState below = this.level.getBlockState(this.center.offset(x, yx, z));
                  if (!below.isAir() && !below.canBeReplaced() && below.getFluidState().isEmpty()) {
                     break;
                  }

                  this.put(x, yx, z, yx == -1 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState());
               }

               this.put(x, 0, z, top.apply(x, z));
            }
         }
      }
   }

   private void chest(int dx, int dy, int dz, Direction facing) {
      BlockPos pos = this.center.offset(dx, dy, dz);
      if (this.box.isInside(pos)) {
         this.level.setBlock(pos, (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
         RandomizableContainerBlockEntity.setLootTable(this.level, this.random, pos, LOOT);
      }
   }

   private void sign(int dx, int dy, int dz, BlockState state, String... lines) {
      BlockPos pos = this.center.offset(dx, dy, dz);
      if (this.box.isInside(pos)) {
         this.level.setBlock(pos, state, 2);
         if (this.level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            ListTag messages = new ListTag();

            for (int i = 0; i < 4; i++) {
               messages.add(StringTag.valueOf(Serializer.toJson(Component.literal(i < lines.length ? lines[i] : ""))));
            }

            CompoundTag front = new CompoundTag();
            front.put("messages", messages);
            front.putString("color", "black");
            front.putBoolean("has_glowing_text", false);
            CompoundTag tag = sign.saveWithoutMetadata();
            tag.put("front_text", front);
            sign.load(tag);
         }
      }
   }

   private void lamp(int x, int z, Block post, int height) {
      for (int y = 1; y <= height; y++) {
         this.put(x, y, z, post);
      }

      this.put(x, height + 1, z, Blocks.LANTERN);
   }

   private void spawnBoss(int dx, int dy, int dz, float yaw) {
      BlockPos pos = this.center.offset(dx, dy, dz);
      if (this.box.isInside(pos)) {
         if (this.arena.boss.get().create(this.level.getLevel()) instanceof Mob boss) {
            boss.moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, yaw, 0.0F);
            boss.setYHeadRot(yaw);
            boss.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
            boss.setPersistenceRequired();
            this.level.addFreshEntityWithPassengers(boss);
         }
      }
   }

   private void post() {
      this.yard(
         (x, zx) -> Math.abs(zx) == 4
               ? Blocks.SMOOTH_STONE.defaultBlockState()
               : (Math.abs(zx) == 5 && this.noise(x, 0, zx) < 0.6 ? Blocks.GRAVEL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState())
      );
      int r = this.arena.radius;

      for (int x = -r; x <= r; x++) {
         for (int z = -3; z <= 3; z++) {
            boolean line = z == 0 && Math.floorMod(x, 4) < 2;
            double n = this.noise(x, 0, z);
            this.put(
               x,
               0,
               z,
               line
                  ? Blocks.WHITE_CONCRETE.defaultBlockState()
                  : (n < 0.08 ? Blocks.GRAY_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState())
            );
         }
      }

      for (int x = -7; x <= -2; x++) {
         for (int z = 6; z <= 9; z++) {
            this.put(x, 0, z, Blocks.SMOOTH_STONE);
            boolean edge = x == -7 || x == -2 || z == 6 || z == 9;

            for (int y = 1; y <= 3; y++) {
               BlockState s = Blocks.AIR.defaultBlockState();
               if (edge) {
                  boolean door = z == 6 && x == -4 && y <= 2;
                  boolean window = y == 2 && z == 6 && (x == -6 || x == -5 || x == -3);
                  s = door
                     ? Blocks.AIR.defaultBlockState()
                     : (
                        window
                           ? pane(Blocks.GLASS_PANE, true)
                           : (y == 3 ? Blocks.BLUE_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState())
                     );
               }

               this.put(x, y, z, s);
            }
         }
      }

      this.fill(-8, 4, 5, -1, 4, 10, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
      this.put(-4, 5, 7, Blocks.REDSTONE_LAMP);
      this.sign(-5, 3, 5, (BlockState)Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.NORTH), "", "ПОСТ ДПС");
      this.put(-3, 1, 8, (BlockState)Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH));
      this.chest(-6, 1, 8, Direction.EAST);
      this.put(-6, 3, 7, (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

      for (int[] b : new int[][]{{5, -4, -3, 0}, {-11, 4, 0, 3}}) {
         this.put(b[0], 1, b[1], Blocks.ANDESITE_WALL);
         this.put(b[0], 2, b[1], Blocks.ANDESITE_WALL);

         for (int z = b[2]; z <= b[3]; z++) {
            this.put(b[0], 2, z, z % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
         }
      }

      for (int x = -2; x <= 3; x += 2) {
         this.put(x, 1, -2, Blocks.ORANGE_CANDLE.defaultBlockState());
      }

      this.lamp(9, -5, Blocks.ANDESITE_WALL, 4);
      this.lamp(-13, 5, Blocks.ANDESITE_WALL, 4);
      this.sign(12, 1, -5, (BlockState)Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, 8), "ОГРАНИЧЕНИЕ", "СКОРОСТИ", "40");
      BlockPos car = this.center.offset(3, 1, 7);
      if (this.box.isInside(car)) {
         VazEntity vaz = (VazEntity)((EntityType)ModRegistry.VAZ.get()).create(this.level.getLevel());
         if (vaz != null) {
            vaz.moveTo((double)car.getX() + 0.5, (double)car.getY(), (double)car.getZ() + 0.5, 90.0F, 0.0F);
            vaz.setBodyColor(15921906);
            this.level.addFreshEntity(vaz);
         }
      }

      this.spawnBoss(-4, 1, 3, 180.0F);
   }

   private void hideout() {
      this.yard((xx, zx) -> this.noise(xx, 0, zx) < 0.85 ? Blocks.SAND.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState());
      int x0 = -8;
      int x1 = 8;
      int z0 = -6;
      int z1 = 4;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            this.put(x, 0, z, (x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE);

            for (int y = 1; y <= 4; y++) {
               BlockState s = Blocks.AIR.defaultBlockState();
               boolean inner = x == -2 && z != 0 || x == 3 && z < 0 && z != -3;
               if (edge) {
                  boolean door = z == z1 && (x == 0 || x == 1) && y <= 2;
                  boolean window = (y == 2 || y == 3) && (z != z0 && z != z1 ? Math.floorMod(z, 3) == 1 : Math.floorMod(x, 4) == 2);
                  s = door
                     ? Blocks.AIR.defaultBlockState()
                     : (
                        !window
                           ? (
                              y == 4
                                 ? ((x + z) % 2 == 0 ? Blocks.MAGENTA_CONCRETE : Blocks.CYAN_CONCRETE).defaultBlockState()
                                 : Blocks.WHITE_CONCRETE.defaultBlockState()
                           )
                           : pane(Blocks.GLASS_PANE, z == z0 || z == z1)
                     );
               } else if (inner && y <= 3) {
                  s = Blocks.PINK_CONCRETE.defaultBlockState();
               }

               this.put(x, y, z, s);
            }

            this.put(x, 5, z, edge ? Blocks.PINK_CONCRETE.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState());
         }
      }

      for (int z = -5; z <= -3; z++) {
         this.put(-7, 1, z, Blocks.RED_WOOL);
      }

      this.put(-7, 1, 2, Blocks.BLACK_CONCRETE);
      this.put(-7, 2, 2, Blocks.LIGHT_BLUE_STAINED_GLASS);
      this.put(6, 1, -5, Blocks.JUKEBOX);
      this.chest(6, 1, 2, Direction.WEST);

      for (int x = -1; x <= 2; x++) {
         for (int z = -4; z <= 2; z++) {
            this.put(x, 1, z, Blocks.MAGENTA_CARPET);
         }
      }

      for (int[] l : new int[][]{{-5, -2}, {0, -1}, {6, -3}}) {
         this.put(l[0], 4, l[1], (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }

      for (int x = 2; x <= 8; x++) {
         for (int z = 7; z <= 12; z++) {
            boolean rim = x == 2 || x == 8 || z == 7 || z == 12;
            this.put(x, 0, z, rim ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState() : Blocks.WATER.defaultBlockState());
            if (!rim) {
               this.put(x, -1, z, Blocks.WATER);
               this.put(x, -2, z, Blocks.LIGHT_BLUE_CONCRETE);
            }
         }
      }

      for (int[] p : new int[][]{{-6, 9}, {11, -9}, {-12, -5}, {12, 5}}) {
         this.palm(p[0], p[1]);
      }

      this.spawnBoss(-5, 1, -2, 0.0F);
   }

   private void palm(int x, int z) {
      int h = 6;

      for (int y = 1; y <= h; y++) {
         this.put(x, y, z, Blocks.JUNGLE_LOG);
      }

      BlockState leaves = (BlockState)Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
      this.put(x, h + 1, z, leaves);

      for (Direction d : Plane.HORIZONTAL) {
         for (int i = 1; i <= 3; i++) {
            this.put(x + d.getStepX() * i, h + (i == 3 ? 0 : 1), z + d.getStepZ() * i, leaves);
         }
      }
   }

   private void warehouse() {
      this.yard((xx, zx) -> {
         double n = this.noise(xx, 0, zx);
         return n < 0.5 ? Blocks.COARSE_DIRT.defaultBlockState() : (n < 0.8 ? Blocks.GRAVEL.defaultBlockState() : Blocks.DIRT.defaultBlockState());
      });
      int x0 = -9;
      int x1 = 9;
      int z0 = -6;
      int z1 = 6;
      int top = 7;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            this.put(x, 0, z, this.noise(x, 1, z) < 0.3 ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_ANDESITE);

            for (int y = 1; y <= top; y++) {
               BlockState s = Blocks.AIR.defaultBlockState();
               if (edge) {
                  boolean door = z == z1 && Math.abs(x) <= 2 && y <= 4;
                  boolean pillar = z != z0 && z != z1 ? Math.floorMod(z, 4) == 2 || Math.abs(z) == 6 : Math.floorMod(x, 4) == 1;
                  boolean window = y == 6 && !pillar;
                  if (door) {
                     s = Blocks.AIR.defaultBlockState();
                  } else if (pillar || y == 1) {
                     s = Blocks.GRAY_CONCRETE.defaultBlockState();
                  } else if (window) {
                     s = this.noise(x, y, z) < 0.35 ? Blocks.AIR.defaultBlockState() : pane(Blocks.GLASS_PANE, z == z0 || z == z1);
                  } else {
                     s = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                  }
               }

               this.put(x, y, z, s);
            }

            this.put(x, top + 1, z, this.noise(x, 9, z) < 0.06 && !edge ? Blocks.AIR.defaultBlockState() : slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
         }
      }

      for (int x = x0 + 1; x <= x1 - 1; x++) {
         int stack = (int)(this.noise(x, 2, 0) * 3.0);

         for (int y = 1; y <= stack + 1; y++) {
            this.put(x, y, z0 + 1, (x + y) % 2 == 0 ? Blocks.BARREL.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
         }
      }

      for (int z = z0 + 2; z <= z1 - 3; z += 2) {
         this.put(x0 + 1, 1, z, Blocks.BARREL);
         this.put(x1 - 1, 1, z, Blocks.SPRUCE_PLANKS);
         this.put(x1 - 1, 2, z, Blocks.SCAFFOLDING);
      }

      for (int x : new int[]{-4, 4}) {
         for (int y = 5; y <= top; y++) {
            this.put(x, y, 0, Blocks.CHAIN);
         }

         this.put(x, 4, 0, (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      }

      this.chest(7, 1, -4, Direction.WEST);
      this.lamp(-5, 10, Blocks.COBBLESTONE_WALL, 3);
      this.lamp(5, 10, Blocks.COBBLESTONE_WALL, 3);
      this.spawnBoss(0, 1, 0, 0.0F);
   }

   private void ruins() {
      this.yard(
         (xx, zx) -> {
            double nx = this.noise(xx, 0, zx);
            return nx < 0.55
               ? Blocks.GRAY_CONCRETE.defaultBlockState()
               : (nx < 0.75 ? Blocks.GRAVEL.defaultBlockState() : (nx < 0.9 ? Blocks.ANDESITE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState()));
         }
      );

      for (int x = -7; x <= 7; x++) {
         for (int z = -7; z <= 7; z++) {
            double d = (double)(x * x + z * z);
            if (!(d > 42.0)) {
               int depth = (int)Math.round(3.0 * (1.0 - d / 42.0));

               for (int y = 0; y > -depth; y--) {
                  this.put(x, y, z, Blocks.AIR);
               }

               this.put(x, -depth, z, this.noise(x, -depth, z) < 0.15 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE);
            }
         }
      }

      for (int[] t : new int[][]{{-13, -11}, {10, -13}, {-14, 7}, {11, 9}, {-2, -16}, {14, -3}}) {
         int h = 5 + (int)(this.noise(t[0], 0, t[1]) * 10.0);

         for (int x = 0; x <= 3; x++) {
            for (int zx = 0; zx <= 3; zx++) {
               if (x == 0 || x == 3 || zx == 0 || zx == 3) {
                  int top = h - (int)(this.noise(t[0] + x, 5, t[1] + zx) * 4.0);

                  for (int y = 1; y <= top; y++) {
                     boolean window = y % 3 == 2 && (x == 1 || x == 2 || zx == 1 || zx == 2);
                     this.put(
                        t[0] + x,
                        y,
                        t[1] + zx,
                        window
                           ? Blocks.GRAY_STAINED_GLASS.defaultBlockState()
                           : ((y + x) % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState())
                     );
                  }
               }
            }
         }
      }

      int r = this.arena.radius;

      for (int x = -r; x <= r; x++) {
         for (int zxx = -r; zxx <= r; zxx++) {
            double n = this.noise(x, 3, zxx);
            if (x * x + zxx * zxx > 64 && x * x + zxx * zxx < r * r && n < 0.05) {
               this.put(x, 1, zxx, n < 0.02 ? Blocks.COBBLESTONE_WALL : (n < 0.035 ? Blocks.ANDESITE : Blocks.GRAVEL));
            }
         }
      }

      for (int[] f : new int[][]{{-9, 3}, {7, -6}, {4, 11}}) {
         this.put(f[0], 1, f[1], (BlockState)Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
      }

      this.chest(-11, 1, -9, Direction.SOUTH);
      this.spawnBoss(0, -2, 0, 180.0F);
   }

   private void college() {
      this.yard(
         (xx, zx) -> Math.abs(xx) <= 10 && zx >= -10 && zx <= 6 || Math.abs(xx) <= 1 && zx > 6
               ? (this.noise(xx, 0, zx) < 0.2 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState()
               : Blocks.GRASS_BLOCK.defaultBlockState()
      );

      for (int x : new int[]{-3, 3}) {
         for (int y = 1; y <= 6; y++) {
            this.put(x, y, 13, Blocks.DARK_OAK_LOG);
         }
      }

      this.fill(-4, 6, 13, 4, 6, 13, Blocks.DARK_OAK_PLANKS.defaultBlockState());
      this.fill(-5, 7, 12, 5, 7, 14, slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM));
      this.fill(-4, 7, 13, 4, 7, 13, Blocks.DEEPSLATE_TILES.defaultBlockState());
      int x0 = -7;
      int x1 = 7;
      int z0 = -17;
      int z1 = -11;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            this.put(x, 0, z, Blocks.STONE_BRICKS);
            this.put(x, 1, z, Blocks.DARK_OAK_PLANKS);

            for (int y = 2; y <= 5; y++) {
               BlockState s = Blocks.AIR.defaultBlockState();
               if (edge) {
                  boolean door = z == z1 && Math.abs(x) <= 1 && y <= 4;
                  boolean post = z != z0 && z != z1 ? Math.floorMod(z, 3) == 1 || z == z0 : Math.floorMod(x, 3) == 1 || Math.abs(x) == 7;
                  s = door
                     ? Blocks.AIR.defaultBlockState()
                     : (
                        post
                           ? Blocks.DARK_OAK_LOG.defaultBlockState()
                           : (y != 3 && y != 4 ? Blocks.WHITE_TERRACOTTA.defaultBlockState() : pane(Blocks.WHITE_STAINED_GLASS_PANE, z == z0 || z == z1))
                     );
               }

               this.put(x, y, z, s);
            }
         }
      }

      int mid = (z0 + z1) / 2;
      int half = (z1 - z0) / 2 + 2;

      for (int z = z0 - 2; z <= z1 + 2; z++) {
         int k = half - Math.abs(z - mid);
         int h2 = 12 + k;
         int y = h2 / 2;

         for (int x = x0 - 2; x <= x1 + 2; x++) {
            this.put(
               x, y, z, k == half ? Blocks.DEEPSLATE_TILES.defaultBlockState() : slab(Blocks.DEEPSLATE_TILE_SLAB, h2 % 2 == 1 ? SlabType.TOP : SlabType.BOTTOM)
            );
            if ((x == x0 || x == x1) && z > z0 && z < z1) {
               for (int dy = 6; dy < y; dy++) {
                  this.put(x, dy, z, Blocks.DARK_OAK_PLANKS);
               }
            }

            if ((z == z0 || z == z1) && x >= x0 && x <= x1) {
               for (int dy = 6; dy < y; dy++) {
                  this.put(x, dy, z, Blocks.DARK_OAK_PLANKS);
               }
            }
         }
      }

      this.fill(-2, 1, -10, 2, 1, -10, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
      this.put(0, 3, -16, (BlockState)Blocks.RED_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH));
      this.chest(5, 2, -15, Direction.WEST);
      this.put(-4, 5, -14, (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      this.put(4, 5, -14, (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));

      for (int[] l : new int[][]{{-9, -9}, {9, -9}, {-9, 5}, {9, 5}}) {
         this.put(l[0], 1, l[1], Blocks.STONE_BRICK_WALL);
         this.put(l[0], 2, l[1], Blocks.LANTERN);
         this.put(l[0], 3, l[1], slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
      }

      for (int[] t : new int[][]{{-14, -4}, {14, -6}, {-13, 10}, {13, 9}}) {
         this.cherry(t[0], t[1]);
      }

      this.spawnBoss(0, 1, -2, 0.0F);
   }


   /** Choso's lair: a half-collapsed dark oak house by a stagnant pond, blood soaked into the floor. */
   private void bloodHouse() {
      this.yard((xx, zx) -> {
         double n = this.noise(xx, 0, zx);
         return n < 0.45 ? Blocks.MUD.defaultBlockState() : (n < 0.75 ? Blocks.PODZOL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
      });

      for (int x = 4; x <= 11; x++) {
         for (int z = -10; z <= -4; z++) {
            if ((x - 7.5) * (x - 7.5) / 14.0 + (z + 7.0) * (z + 7.0) / 9.0 <= 1.0) {
               this.put(x, 0, z, Blocks.WATER);
               this.put(x, -1, z, Blocks.MUD);
               if (this.noise(x, 5, z) < 0.12) {
                  this.put(x, 1, z, Blocks.LILY_PAD);
               }
            }
         }
      }

      int x0 = -7;
      int x1 = 5;
      int z0 = -4;
      int z1 = 6;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            double n = this.noise(x, 2, z);
            this.put(x, 0, z, n < 0.18 ? Blocks.RED_TERRACOTTA : (n < 0.3 ? Blocks.MUD_BRICKS : Blocks.DARK_OAK_PLANKS));
            if (!edge && n < 0.08) {
               this.put(x, 1, z, Blocks.RED_CARPET);
            }

            int wall = 4 - (int)(this.noise(x, 3, z) * 3.0 * (x > 1 ? 1.0 : 0.3));

            for (int y = 1; y <= 4; y++) {
               if (edge) {
                  boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                  boolean door = z == z1 && (x == -1 || x == 0) && y <= 2;
                  boolean window = y == 2 && !corner && (z == z0 || z == z1 ? Math.floorMod(x, 4) == 1 : Math.floorMod(z, 4) == 1);
                  BlockState s;
                  if (door || window || y > wall && !corner) {
                     s = Blocks.AIR.defaultBlockState();
                  } else if (corner) {
                     s = Blocks.DARK_OAK_LOG.defaultBlockState();
                  } else {
                     s = this.noise(x, y, z) < 0.2 ? Blocks.MUD_BRICKS.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState();
                  }

                  this.put(x, y, z, s);
               } else {
                  this.put(x, y, z, Blocks.AIR.defaultBlockState());
               }
            }

            if (x < 1 && this.noise(x, 9, z) > 0.12) {
               this.put(x, 5, z, edge ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
            }
         }
      }

      for (int z = z0 + 1; z <= z1 - 1; z++) {
         this.put(x0 + 1, 1, z, z % 2 == 0 ? Blocks.BOOKSHELF : Blocks.DARK_OAK_PLANKS);
      }

      for (int x = -5; x <= -2; x++) {
         this.put(x, 1, z0 + 1, Blocks.DARK_OAK_PLANKS);
         this.put(x, 2, z0 + 1, Blocks.DECORATED_POT);
      }

      this.put(-6, 3, 0, (BlockState)Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
      this.put(3, 1, 4, Blocks.CAULDRON);
      this.put(2, 1, 4, Blocks.REDSTONE_WIRE);
      this.chest(-6, 1, 5, Direction.EAST);

      for (int[] p : new int[][]{{9, 4}, {-11, -6}, {-10, 8}, {11, 9}}) {
         for (int y = 1; y <= 5; y++) {
            this.put(p[0], y, p[1], Blocks.MANGROVE_LOG);
         }

         this.put(p[0], 6, p[1], Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
         this.put(p[0], 1, p[1] + 1, Blocks.MANGROVE_ROOTS);
      }

      this.spawnBoss(-1, 1, 1, 180.0F);
   }


   /** Mahito's lair: a cracked concrete booth over a manhole; below, a sewer hall overgrown with reshaped flesh. */
   private void sewer() {
      this.yard((xx, zx) -> {
         double n = this.noise(xx, 0, zx);
         return n < 0.4 ? Blocks.GRAVEL.defaultBlockState() : (n < 0.7 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
      });
      // the hall below
      for (int x = -7; x <= 7; x++) {
         for (int z = -7; z <= 7; z++) {
            for (int y = -8; y <= -1; y++) {
               boolean shell = Math.abs(x) == 7 || Math.abs(z) == 7 || y == -8 || y == -1;
               double n = this.noise(x, y, z);
               BlockState s;
               if (shell) {
                  s = n < 0.25 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : (n < 0.45 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
                  if ((Math.abs(x) == 7 || Math.abs(z) == 7) && y == -4 && Math.floorMod(x + z, 4) == 0) {
                     s = Blocks.IRON_BARS.defaultBlockState();
                  }
               } else {
                  s = Blocks.AIR.defaultBlockState();
               }

               this.put(x, y, z, s);
            }

            if (z == 0 || z == 1) {
               this.put(x, -8, z, Blocks.WATER);
               this.put(x, -9, z, Blocks.STONE_BRICKS);
            }
         }
      }
      // reshaped flesh on the walls and a few lights
      for (int x = -6; x <= 6; x++) {
         for (int z = -6; z <= 6; z++) {
            double n = this.noise(x, 7, z);
            if (n < 0.1 && z != 0 && z != 1) {
               this.put(x, -7, z, n < 0.05 ? Blocks.NETHER_WART_BLOCK : Blocks.BONE_BLOCK);
            } else if (n > 0.93) {
               this.put(x, -2, z, Blocks.COBWEB);
            }
         }
      }

      for (int[] c : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
         this.put(c[0], -2, c[1], Blocks.SHROOMLIGHT);
         this.put(c[0], -7, c[1], Blocks.NETHER_WART_BLOCK);
         this.put(c[0], -6, c[1], Blocks.NETHER_WART_BLOCK);
      }

      this.put(-5, -7, 5, Blocks.SKELETON_SKULL);
      this.chest(5, -7, -5, Direction.WEST);
      // the booth with the manhole
      for (int x = -2; x <= 2; x++) {
         for (int z = -3; z <= 1; z++) {
            boolean edge = Math.abs(x) == 2 || z == -3 || z == 1;
            this.put(x, 0, z, Blocks.GRAY_CONCRETE);
            for (int y = 1; y <= 3; y++) {
               boolean door = z == 1 && x == 0 && y <= 2;
               boolean broken = y == 3 && this.noise(x, y, z) < 0.4;
               this.put(x, y, z, edge && !door && !broken ? (this.noise(x, y, z) < 0.3 ? Blocks.CRACKED_STONE_BRICKS : Blocks.GRAY_CONCRETE).defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
         }
      }

      for (int y = -7; y <= 0; y++) {
         this.put(0, y, -1, Blocks.AIR.defaultBlockState());
         this.put(0, y, -2, Blocks.STONE_BRICKS);
         this.put(0, y, -1, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
      }

      this.put(1, 1, -2, Blocks.IRON_BARS);
      this.put(-1, 1, -2, Blocks.IRON_BARS);
      this.spawnBoss(0, -7, 4, 180.0F);
   }

   private void cherry(int x, int z) {
      for (int y = 1; y <= 4; y++) {
         this.put(x, y, z, Blocks.CHERRY_LOG);
      }

      BlockState leaves = (BlockState)Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

      for (int dx = -3; dx <= 3; dx++) {
         for (int dy = 3; dy <= 6; dy++) {
            for (int dz = -3; dz <= 3; dz++) {
               double d = (double)(dx * dx + (dy - 5) * (dy - 5) * 2 + dz * dz);
               if (d <= 9.5 + this.noise(x + dx, dy, z + dz) * 3.0 && (dx != 0 || dz != 0 || dy > 4)) {
                  this.put(x + dx, dy, z + dz, leaves);
               }
            }
         }
      }
   }
}
