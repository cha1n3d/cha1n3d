package com.bobux.vaz2109.computer;

import com.bobux.vaz2109.item.GunItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The computer's built-in market: rubles kept on the player, what things sell for, the shop, the daily deals and
 * the daily orders. Prices come from {@link PriceTable} (generated, checked for arbitrage). Selling a lot of one
 * stackable item in one day lowers its demand; it recovers the next day.
 */
public final class ComputerShop {
   public static final int DEALS = 0;
   public static final int BANK = 8;
   public static final int CATEGORIES = 9;
   public static final int DEAL_COUNT = 4;
   public static final int DEAL_PERCENT = 30;
   public static final int ORDER_COUNT = 3;
   private static final String WALLET = "vaz2109_rubles";
   private static final String SOLD = "vaz2109_sold";
   private static final String DONE = "vaz2109_orders";
   private static final float SATURATION = 128.0F;
   private static final float MIN_DEMAND = 0.25F;
   private static Map<Item, Integer> values;
   private static List<ComputerShop.Offer> offers;
   private static List<Object[]> orderPool;

   private ComputerShop() {
   }

   // ---- wallet ----------------------------------------------------------------------------------

   private static CompoundTag persisted(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted");
   }

   private static void save(Player player, CompoundTag persisted) {
      player.getPersistentData().put("PlayerPersisted", persisted);
   }

   public static long balance(Player player) {
      return persisted(player).getLong(WALLET);
   }

   public static void setBalance(Player player, long rubles) {
      CompoundTag persisted = persisted(player);
      persisted.putLong(WALLET, Math.max(0L, Math.min((long)Integer.MAX_VALUE, rubles)));
      save(player, persisted);
   }

   /** Market day: the deals and orders change with every in-game day (sleeping counts). Kept to 16 bits for syncing. */
   public static int day(Level level) {
      return (int)(level.getDayTime() / 24000L & 65535L);
   }

   // ---- tables ----------------------------------------------------------------------------------

   private static Item item(String id) {
      Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
      return it == null ? Items.AIR : it;
   }

   public static List<ComputerShop.Offer> offers() {
      if (offers == null) {
         List<ComputerShop.Offer> list = new ArrayList<>();

         for (Object[] o : PriceTable.OFFERS) {
            ComputerShop.Offer offer = ComputerShop.Offer.parse((Integer)o[0], (String)o[1], (Integer)o[2], (Integer)o[3]);
            if (offer != null) {
               list.add(offer);
            }
         }

         offers = list;
      }

      return offers;
   }

   private static Map<Item, Integer> values() {
      if (values == null) {
         Map<Item, Integer> map = new HashMap<>();

         for (Object[] o : PriceTable.SELL) {
            Item it = item((String)o[0]);
            if (it != Items.AIR) {
               map.put(it, (Integer)o[1]);
            }
         }

         values = map;
      }

      return values;
   }

   /** Rubles for one item of this stack at full demand; 0 when the computer will not buy it. Worn tools sell for less. */
   public static int value(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      } else {
         int base = values().getOrDefault(stack.getItem(), 0);
         if (base > 0 && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            base = Math.max(1, Math.round(base * (1.0F - (float)stack.getDamageValue() / (float)stack.getMaxDamage())));
         }

         return base;
      }
   }

   // ---- demand ----------------------------------------------------------------------------------

   /** Stackable goods saturate; gear, trophies and banknotes always sell at full price. */
   private static boolean saturates(ItemStack stack) {
      return stack.getMaxStackSize() > 1 && !(stack.getItem() instanceof BanknoteItem);
   }

   private static CompoundTag soldToday(Player player) {
      CompoundTag sold = persisted(player).getCompound(SOLD);
      return sold.getInt("day") == day(player.level()) ? sold.getCompound("items") : new CompoundTag();
   }

   private static String key(ItemStack stack) {
      ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
      return id == null ? "?" : id.toString();
   }

   private static float demandAt(int sold) {
      return Math.max(MIN_DEMAND, 1.0F / (1.0F + (float)sold / SATURATION));
   }

   /** Demand for this item right now, in percent. */
   public static int demand(Player player, ItemStack stack) {
      return saturates(stack) ? Math.round(demandAt(soldToday(player).getInt(key(stack))) * 100.0F) : 100;
   }

   private static long quote(ItemStack stack, int alreadySold) {
      int unit = value(stack);
      if (unit <= 0) {
         return 0L;
      } else if (!saturates(stack)) {
         return (long)unit * (long)stack.getCount();
      } else {
         double sum = 0.0;

         for (int i = 0; i < stack.getCount(); i++) {
            sum += (double)((float)unit * demandAt(alreadySold + i));
         }

         return Math.round(sum);
      }
   }

   /** What the computer would pay for this stack right now. */
   public static long quote(Player player, ItemStack stack) {
      return saturates(stack) ? quote(stack, soldToday(player).getInt(key(stack))) : quote(stack, 0);
   }

   /** Pays for the stack and records it against today's demand. The caller removes the items. */
   public static long sell(Player player, ItemStack stack) {
      long paid = quote(player, stack);
      if (paid > 0L && saturates(stack)) {
         CompoundTag persisted = persisted(player);
         CompoundTag items = soldToday(player).copy();
         items.putInt(key(stack), items.getInt(key(stack)) + stack.getCount());
         CompoundTag sold = new CompoundTag();
         sold.putInt("day", day(player.level()));
         sold.put("items", items);
         persisted.put(SOLD, sold);
         save(player, persisted);
      }

      setBalance(player, balance(player) + paid);
      return paid;
   }

   // ---- deals -----------------------------------------------------------------------------------

   /** Offer indices on sale today (same for everyone). */
   public static int[] deals(int day) {
      List<ComputerShop.Offer> all = offers();
      List<Integer> pool = new ArrayList<>();

      for (int i = 0; i < all.size(); i++) {
         if (all.get(i).category != BANK) {
            pool.add(i);
         }
      }

      Random random = new Random((long)day * 6364136223846793005L + 2109L);
      int[] picked = new int[Math.min(DEAL_COUNT, pool.size())];

      for (int i = 0; i < picked.length; i++) {
         picked[i] = pool.remove(random.nextInt(pool.size()));
      }

      return picked;
   }

   public static boolean onSale(int index, int day) {
      for (int i : deals(day)) {
         if (i == index) {
            return true;
         }
      }

      return false;
   }

   public static int price(int index, int day) {
      int price = offers().get(index).price;
      return onSale(index, day) ? Math.max(1, Math.round((float)price * (float)(100 - DEAL_PERCENT) / 100.0F)) : price;
   }

   // ---- orders ----------------------------------------------------------------------------------

   private static List<Object[]> orderPool() {
      if (orderPool == null) {
         List<Object[]> list = new ArrayList<>();

         for (Object[] o : PriceTable.ORDERS) {
            if (item((String)o[0]) != Items.AIR) {
               list.add(o);
            }
         }

         orderPool = list;
      }

      return orderPool;
   }

   /** Today's three orders for this player: hand in the items, get about twice what they would sell for. */
   public static List<ComputerShop.Order> orders(UUID player, int day) {
      List<Object[]> pool = new ArrayList<>(orderPool());
      Random random = new Random(player.getMostSignificantBits() ^ player.getLeastSignificantBits() * 31L ^ (long)day * 2654435761L);
      List<ComputerShop.Order> list = new ArrayList<>();

      for (int i = 0; i < ORDER_COUNT && !pool.isEmpty(); i++) {
         Object[] o = pool.remove(random.nextInt(pool.size()));
         int min = (Integer)o[1];
         int max = (Integer)o[2];
         int count = min + random.nextInt(max - min + 1);
         if (min >= 8) {
            count = Math.max(min, count / 4 * 4);
         }

         int reward = Math.max(10, Math.round((float)count * (float)(Integer)o[3] / 500.0F) * 5);
         list.add(new ComputerShop.Order(item((String)o[0]), count, reward));
      }

      return list;
   }

   /** Bit i set: order i was handed in today. */
   public static int ordersDone(Player player) {
      CompoundTag done = persisted(player).getCompound(DONE);
      return done.getInt("day") == day(player.level()) ? done.getInt("mask") : 0;
   }

   public static int count(Player player, Item item) {
      int n = 0;

      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.is(item)) {
            n += s.getCount();
         }
      }

      return n;
   }

   /** Hands in order i from the inventory. */
   public static boolean complete(Player player, int index) {
      int day = day(player.level());
      List<ComputerShop.Order> list = orders(player.getUUID(), day);
      int mask = ordersDone(player);
      if (index >= 0 && index < list.size() && (mask & 1 << index) == 0) {
         ComputerShop.Order order = list.get(index);
         if (count(player, order.item) < order.count) {
            return false;
         } else {
            int left = order.count;

            for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
               ItemStack s = player.getInventory().getItem(i);
               if (s.is(order.item)) {
                  int take = Math.min(left, s.getCount());
                  s.shrink(take);
                  left -= take;
               }
            }

            CompoundTag persisted = persisted(player);
            CompoundTag done = new CompoundTag();
            done.putInt("day", day);
            done.putInt("mask", mask | 1 << index);
            persisted.put(DONE, done);
            save(player, persisted);
            setBalance(player, balance(player) + (long)order.reward);
            return true;
         }
      } else {
         return false;
      }
   }

   // ---- types -----------------------------------------------------------------------------------

   public static final class Offer {
      public final int category;
      public final Item item;
      public final int count;
      public final int price;
      @org.jetbrains.annotations.Nullable
      private final Enchantment enchantment;
      private final int level;
      @org.jetbrains.annotations.Nullable
      private final Potion potion;

      private Offer(int category, Item item, int count, int price, Enchantment enchantment, int level, Potion potion) {
         this.category = category;
         this.item = item;
         this.count = count;
         this.price = price;
         this.enchantment = enchantment;
         this.level = level;
         this.potion = potion;
      }

      @org.jetbrains.annotations.Nullable
      static ComputerShop.Offer parse(int category, String id, int count, int price) {
         if (id.startsWith("book:")) {
            String[] p = id.split(":");
            Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(p[1], p[2]));
            return e == null ? null : new ComputerShop.Offer(category, Items.ENCHANTED_BOOK, 1, price, e, Integer.parseInt(p[3]), null);
         } else if (id.startsWith("potion:")) {
            Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(id.substring(7)));
            return potion == null ? null : new ComputerShop.Offer(category, Items.POTION, 1, price, null, 0, potion);
         } else {
            Item it = item(id);
            return it == Items.AIR ? null : new ComputerShop.Offer(category, it, count, price, null, 0, null);
         }
      }

      public ItemStack stack() {
         if (this.enchantment != null) {
            return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(this.enchantment, this.level));
         } else if (this.potion != null) {
            return PotionUtils.setPotion(new ItemStack(Items.POTION), this.potion);
         } else {
            return this.item instanceof GunItem gun ? GunItem.loaded(gun.type) : new ItemStack(this.item, this.count);
         }
      }
   }

   public static final class Order {
      public final Item item;
      public final int count;
      public final int reward;

      Order(Item item, int count, int reward) {
         this.item = item;
         this.count = count;
         this.reward = reward;
      }
   }
}
