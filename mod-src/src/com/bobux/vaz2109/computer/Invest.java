package com.bobux.vaz2109.computer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Investments. Three stocks whose daily price (same for everyone on the server) swings around its fair value:
 * a few slow waves seeded from the world seed plus daily noise, so prices come back and patience pays, but a
 * 3% fee on every trade punishes jumping in and out. And a savings deposit: +1% a day, compounded for at most
 * 30 days between visits, up to 100 000 rubles.
 */
public final class Invest {
   public static final int STOCKS = 3;
   public static final int HISTORY = 8;
   public static final int MAX_SHARES = 60000;
   public static final float FEE = 0.03F;
   public static final long MAX_DEPOSIT = 100000L;
   public static final int DEPOSIT_DAYS = 30;
   private static final double[] BASE = new double[]{120.0, 60.0, 40.0};
   private static final double[] SWING = new double[]{0.25, 0.4, 0.8};
   private static final double[] NOISE = new double[]{0.03, 0.05, 0.12};
   private static final double[] PERIODS = new double[]{11.0, 23.0, 47.0, 97.0};
   private static final double[] WEIGHTS = new double[]{0.15, 0.3, 0.6, 1.0};
   private static final String SHARES = "vaz2109_shares";
   private static final String DEPOSIT = "vaz2109_deposit";
   private static final String DEPOSIT_DAY = "vaz2109_deposit_day";

   private Invest() {
   }

   public static long day(ServerLevel level) {
      return level.getDayTime() / 24000L;
   }

   private static long mix(long x) {
      x = (x ^ x >>> 33) * -49064778989728563L;
      x = (x ^ x >>> 33) * -4265267296055464877L;
      return x ^ x >>> 33;
   }

   private static double unit(long seed) {
      return (double)(mix(seed) >>> 11) * 1.1102230246251565E-16;
   }

   /** Price of one share of stock s on the given day. */
   public static int price(long worldSeed, int s, long day) {
      double log = 0.0;

      for (int k = 0; k < PERIODS.length; k++) {
         double phase = unit(worldSeed ^ (long)(s * 977 + k * 31) * 1442695040888963407L) * Math.PI * 2.0;
         log += Math.sin((double)day * Math.PI * 2.0 / PERIODS[k] + phase) * WEIGHTS[k];
      }

      log = log * SWING[s] / 1.4;
      log += (unit(worldSeed * 31L + (long)s * 7919L + day * 104729L) * 2.0 - 1.0) * NOISE[s];
      return (int)Math.max(1L, Math.min(60000L, Math.round(BASE[s] * Math.exp(log))));
   }

   public static int price(ServerLevel level, int s, long day) {
      return price(level.getServer().overworld().getSeed(), s, Math.max(0L, day));
   }

   private static CompoundTag persisted(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted");
   }

   private static void save(Player player, CompoundTag tag) {
      player.getPersistentData().put("PlayerPersisted", tag);
   }

   public static int shares(Player player, int s) {
      int[] all = persisted(player).getIntArray(SHARES);
      return s < all.length ? all[s] : 0;
   }

   private static void setShares(Player player, int s, int n) {
      CompoundTag tag = persisted(player);
      int[] all = tag.getIntArray(SHARES);
      if (all.length < STOCKS) {
         int[] grown = new int[STOCKS];
         System.arraycopy(all, 0, grown, 0, all.length);
         all = grown;
      }

      all[s] = n;
      tag.putIntArray(SHARES, all);
      save(player, tag);
   }

   public static long fee(long amount) {
      return Math.max(1L, (long)Math.ceil((double)amount * (double)FEE));
   }

   public static boolean buy(ServerLevel level, Player player, int s, int count) {
      int have = shares(player, s);
      count = Math.min(count, MAX_SHARES - have);
      if (s >= 0 && s < STOCKS && count > 0) {
         long cost = (long)price(level, s, day(level)) * (long)count;
         long total = cost + fee(cost);
         if (ComputerShop.balance(player) < total) {
            return false;
         } else {
            ComputerShop.setBalance(player, ComputerShop.balance(player) - total);
            setShares(player, s, have + count);
            return true;
         }
      } else {
         return false;
      }
   }

   /** count 0 sells everything. */
   public static boolean sell(ServerLevel level, Player player, int s, int count) {
      int have = shares(player, s);
      count = count <= 0 ? have : Math.min(count, have);
      if (s >= 0 && s < STOCKS && count > 0) {
         long gross = (long)price(level, s, day(level)) * (long)count;
         ComputerShop.setBalance(player, ComputerShop.balance(player) + gross - fee(gross));
         setShares(player, s, have - count);
         return true;
      } else {
         return false;
      }
   }

   /** Deposit with interest so far. */
   public static long deposit(ServerLevel level, Player player) {
      CompoundTag tag = persisted(player);
      long principal = tag.getLong(DEPOSIT);
      if (principal <= 0L) {
         return 0L;
      } else {
         long days = Math.max(0L, Math.min((long)DEPOSIT_DAYS, day(level) - tag.getLong(DEPOSIT_DAY)));
         return Math.round((double)principal * Math.pow(1.01, (double)days));
      }
   }

   private static void setDeposit(ServerLevel level, Player player, long amount) {
      CompoundTag tag = persisted(player);
      tag.putLong(DEPOSIT, amount);
      tag.putLong(DEPOSIT_DAY, day(level));
      save(player, tag);
   }

   public static boolean putIn(ServerLevel level, Player player, long amount) {
      long now = deposit(level, player);
      amount = Math.min(amount, MAX_DEPOSIT - now);
      if (amount > 0L && ComputerShop.balance(player) >= amount) {
         ComputerShop.setBalance(player, ComputerShop.balance(player) - amount);
         setDeposit(level, player, now + amount);
         return true;
      } else {
         return false;
      }
   }

   public static boolean takeOut(ServerLevel level, Player player) {
      long now = deposit(level, player);
      if (now <= 0L) {
         return false;
      } else {
         ComputerShop.setBalance(player, ComputerShop.balance(player) + now);
         setDeposit(level, player, 0L);
         return true;
      }
   }
}
