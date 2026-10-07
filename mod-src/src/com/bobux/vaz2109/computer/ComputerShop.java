package com.bobux.vaz2109.computer;

import com.bobux.vaz2109.item.GunItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;

/** The computer's built-in market: rubles kept on the player, what things sell for and what the shop offers. */
public final class ComputerShop {
   private static final String WALLET = "vaz2109_rubles";
   private static final float RESALE = 0.4F;
   private static final Object[][] SELL = new Object[][]{
      {"minecraft:diamond", 200}, {"minecraft:emerald", 50}, {"minecraft:gold_ingot", 25}, {"minecraft:iron_ingot", 8},
      {"minecraft:copper_ingot", 2}, {"minecraft:netherite_ingot", 2000}, {"minecraft:netherite_scrap", 400}, {"minecraft:ancient_debris", 400},
      {"minecraft:raw_iron", 6}, {"minecraft:raw_gold", 18}, {"minecraft:raw_copper", 1}, {"minecraft:gold_nugget", 2}, {"minecraft:iron_nugget", 1},
      {"minecraft:diamond_block", 1800}, {"minecraft:emerald_block", 450}, {"minecraft:gold_block", 225}, {"minecraft:iron_block", 72},
      {"minecraft:coal", 2}, {"minecraft:redstone", 1}, {"minecraft:lapis_lazuli", 2}, {"minecraft:quartz", 2}, {"minecraft:amethyst_shard", 3},
      {"minecraft:wheat", 1}, {"minecraft:bread", 1}, {"minecraft:carrot", 1}, {"minecraft:potato", 1}, {"minecraft:sugar_cane", 1},
      {"minecraft:beef", 2}, {"minecraft:porkchop", 2}, {"minecraft:chicken", 2}, {"minecraft:mutton", 2}, {"minecraft:cooked_beef", 2},
      {"minecraft:cooked_porkchop", 2}, {"minecraft:cooked_chicken", 2}, {"minecraft:leather", 3}, {"minecraft:string", 1}, {"minecraft:bone", 1},
      {"minecraft:gunpowder", 3}, {"minecraft:ender_pearl", 15}, {"minecraft:blaze_rod", 12}, {"minecraft:ghast_tear", 30}, {"minecraft:slime_ball", 3},
      {"minecraft:spider_eye", 2}, {"minecraft:feather", 1}, {"minecraft:honey_bottle", 4}, {"minecraft:golden_apple", 100},
      {"minecraft:enchanted_golden_apple", 2000}, {"minecraft:totem_of_undying", 1000}, {"minecraft:nether_star", 3000},
      {"minecraft:experience_bottle", 10}, {"minecraft:name_tag", 50}, {"minecraft:saddle", 40}, {"minecraft:trident", 600},
      {"minecraft:elytra", 3000}, {"minecraft:heart_of_the_sea", 800}, {"minecraft:nautilus_shell", 30}, {"minecraft:shulker_shell", 120},
      {"minecraft:phantom_membrane", 10}, {"minecraft:rabbit_foot", 15}, {"minecraft:echo_shard", 50},
      {"vaz2109:sukuna_finger", 500}, {"vaz2109:death_painting", 3000}, {"vaz2109:frostmourne", 5000}, {"vaz2109:haruta_sword", 1200},
      {"vaz2109:timokha_chain", 1500}, {"vaz2109:nanami_blade", 1200}, {"vaz2109:playful_cloud", 1200}, {"vaz2109:inverted_spear", 1500},
      {"vaz2109:split_soul_katana", 1500}, {"vaz2109:dragon_bone", 1200}, {"vaz2109:slaughter_demon", 800}, {"vaz2109:ivan_watch", 1500},
      {"vaz2109:music_disc_morgen_1", 200}, {"vaz2109:music_disc_morgen_2", 200}, {"vaz2109:music_disc_morgen_3", 200}
   };
   private static final Object[][] OFFERS = new Object[][]{
      {"minecraft:bread", 8, 20}, {"minecraft:cooked_beef", 8, 40}, {"vaz2109:beer", 4, 40}, {"minecraft:golden_apple", 1, 300},
      {"minecraft:iron_ingot", 8, 80}, {"minecraft:gold_ingot", 4, 120}, {"minecraft:diamond", 1, 450}, {"minecraft:ender_pearl", 4, 160},
      {"minecraft:experience_bottle", 8, 200}, {"minecraft:firework_rocket", 16, 120},
      {"vaz2109:ammo_9x18", 32, 60}, {"vaz2109:ammo_545", 30, 120}, {"vaz2109:ammo_12ga", 16, 90}, {"vaz2109:ammo_762", 10, 150},
      {"vaz2109:pg7_rocket", 1, 200}, {"vaz2109:pm", 1, 600}, {"vaz2109:sawed_off", 1, 1500}, {"vaz2109:ak74", 1, 2500}, {"vaz2109:svd", 1, 4000},
      {"vaz2109:rpg7", 1, 6000}, {"vaz2109:grenade", 4, 300}, {"vaz2109:molotov", 4, 200}, {"vaz2109:flashbang", 4, 250},
      {"vaz2109:bat", 1, 120}, {"vaz2109:crowbar", 1, 150}, {"vaz2109:kastet", 1, 150}, {"vaz2109:machete", 1, 200}, {"vaz2109:chainsaw", 1, 900},
      {"vaz2109:tactical_helmet", 1, 500}, {"vaz2109:tactical_chestplate", 1, 900}, {"vaz2109:tactical_leggings", 1, 700}, {"vaz2109:tactical_boots", 1, 400},
      {"vaz2109:narcoleptin", 4, 400}, {"vaz2109:compound_v", 1, 3000}, {"vaz2109:explorer_compass", 1, 1200},
      {"vaz2109:spray_can", 1, 150}, {"vaz2109:skateboard", 1, 300}, {"vaz2109:scooter", 1, 450},
      {"vaz2109:bull_bar", 1, 900}, {"vaz2109:turbo", 1, 1500}, {"vaz2109:spoiler", 1, 600}, {"vaz2109:radio", 1, 700},
      {"vaz2109:lowering_kit", 1, 500}, {"vaz2109:vaz2109", 1, 10000}
   };
   private static Map<Item, Integer> values;
   private static List<ComputerShop.Offer> offers;

   private ComputerShop() {
   }

   public static long balance(Player player) {
      return player.getPersistentData().getCompound("PlayerPersisted").getLong(WALLET);
   }

   public static void setBalance(Player player, long rubles) {
      CompoundTag data = player.getPersistentData();
      CompoundTag persisted = data.getCompound("PlayerPersisted");
      persisted.putLong(WALLET, Math.max(0L, Math.min((long)Integer.MAX_VALUE, rubles)));
      data.put("PlayerPersisted", persisted);
   }

   private static Item item(String id) {
      return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
   }

   public static List<ComputerShop.Offer> offers() {
      if (offers == null) {
         List<ComputerShop.Offer> list = new ArrayList<>();

         for (Object[] o : OFFERS) {
            Item it = item((String)o[0]);
            if (it != null && it != Items.AIR) {
               list.add(new ComputerShop.Offer(it, (Integer)o[1], (Integer)o[2]));
            }
         }

         offers = list;
      }

      return offers;
   }

   private static Map<Item, Integer> values() {
      if (values == null) {
         Map<Item, Integer> map = new HashMap<>();

         for (ComputerShop.Offer o : offers()) {
            map.put(o.item, Math.max(1, Math.round((float)o.price / (float)o.count * RESALE)));
         }

         for (Object[] o : SELL) {
            Item it = item((String)o[0]);
            if (it != null && it != Items.AIR) {
               map.put(it, (Integer)o[1]);
            }
         }

         values = map;
      }

      return values;
   }

   /** Rubles for one item of this stack; 0 when the computer will not buy it. Worn tools sell for less. */
   public static int value(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0;
      } else {
         Integer known = values().get(stack.getItem());
         int base;
         if (known != null) {
            base = known;
         } else {
            Rarity rarity = stack.getRarity();
            base = rarity == Rarity.EPIC ? 150 : (rarity == Rarity.RARE ? 60 : (rarity == Rarity.UNCOMMON ? 20 : (stack.getMaxStackSize() == 1 ? 5 : 0)));
         }

         if (base > 0 && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            base = Math.max(1, Math.round(base * (1.0F - (float)stack.getDamageValue() / (float)stack.getMaxDamage())));
         }

         return base;
      }
   }

   public static final class Offer {
      public final Item item;
      public final int count;
      public final int price;

      Offer(Item item, int count, int price) {
         this.item = item;
         this.count = count;
         this.price = price;
      }

      public ItemStack stack() {
         return this.item instanceof GunItem gun ? GunItem.loaded(gun.type) : new ItemStack(this.item, this.count);
      }
   }
}
