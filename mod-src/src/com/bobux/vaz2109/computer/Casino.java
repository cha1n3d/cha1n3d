package com.bobux.vaz2109.computer;

import net.minecraft.util.RandomSource;

/**
 * VAZ-OS casino. Slots: three reels of six symbols; three of a kind pays 5x..500x, a pair of the better symbols
 * returns the bet or more. Return to player about 93%. Roulette: a European wheel, red/black pays 2x, zero 36x
 * (97.3%). The house always wins in the long run.
 */
public final class Casino {
   public static final int[] BETS = new int[]{10, 100, 1000};
   public static final int SYMBOLS = 6;
   private static final int[] WEIGHTS = new int[]{32, 26, 18, 12, 8, 4};
   private static final int[] TRIPLE = new int[]{5, 10, 20, 40, 100, 500};
   private static final int[] PAIR = new int[]{0, 1, 1, 1, 2, 4};
   public static final int RED = 0;
   public static final int BLACK = 1;
   public static final int ZERO = 2;
   private static final int[] RED_NUMBERS = new int[]{1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};

   private Casino() {
   }

   public static boolean validBet(int bet) {
      for (int b : BETS) {
         if (b == bet) {
            return true;
         }
      }

      return false;
   }

   private static int symbol(RandomSource random) {
      int r = random.nextInt(100);

      for (int i = 0; i < SYMBOLS; i++) {
         r -= WEIGHTS[i];
         if (r < 0) {
            return i;
         }
      }

      return 0;
   }

   /** Three reels packed as 3 bits each. */
   public static int spin(RandomSource random) {
      return symbol(random) | symbol(random) << 3 | symbol(random) << 6;
   }

   public static int reel(int packed, int i) {
      return packed >> i * 3 & 7;
   }

   /** Multiplier for a spin (0 = lost). */
   public static int slotsMultiplier(int packed) {
      int a = reel(packed, 0);
      int b = reel(packed, 1);
      int c = reel(packed, 2);
      if (a == b && b == c) {
         return TRIPLE[a];
      } else if (a == b || a == c) {
         return PAIR[a];
      } else {
         return b == c ? PAIR[b] : 0;
      }
   }

   public static int roulette(RandomSource random) {
      return random.nextInt(37);
   }

   public static boolean red(int number) {
      for (int n : RED_NUMBERS) {
         if (n == number) {
            return true;
         }
      }

      return false;
   }

   public static int rouletteMultiplier(int number, int choice) {
      if (number == 0) {
         return choice == ZERO ? 36 : 0;
      } else if (choice == RED) {
         return red(number) ? 2 : 0;
      } else {
         return choice == BLACK && !red(number) ? 2 : 0;
      }
   }
}
