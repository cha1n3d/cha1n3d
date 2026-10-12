package com.fallenroses.registry;

import com.fallenroses.item.CursedObjects;
import com.fallenroses.item.CursedToolItem;
import com.fallenroses.item.RebirthScrollItem;
import com.fallenroses.item.SpecialGradeSealItem;
import com.fallenroses.item.SukunaFingerItem;
import com.fallenroses.item.TechniqueScrollItem;
import com.fallenroses.technique.Technique;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "fallenroses");
    public static final RegistryObject<Item> CURSED_ESSENCE = ITEMS.register("cursed_essence", () -> new Item(new Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SUKUNA_FINGER = ITEMS.register(
        "sukuna_finger",
        () -> new SukunaFingerItem(new Properties().stacksTo(20).rarity(Rarity.EPIC).food(new Builder().nutrition(0).saturationMod(0.0F).alwaysEat().build()))
    );
    public static final RegistryObject<Item> SCROLL_LIMITLESS = scroll("scroll_limitless", Technique.LIMITLESS);
    public static final RegistryObject<Item> SCROLL_CURSED_SPEECH = scroll("scroll_cursed_speech", Technique.CURSED_SPEECH);
    public static final RegistryObject<Item> SCROLL_BOOGIE_WOOGIE = scroll("scroll_boogie_woogie", Technique.BOOGIE_WOOGIE);
    public static final RegistryObject<Item> SCROLL_BLOOD_MANIPULATION = scroll("scroll_blood_manipulation", Technique.BLOOD_MANIPULATION);
    public static final RegistryObject<Item> SCROLL_RATIO = scroll("scroll_ratio", Technique.RATIO);
    public static final RegistryObject<Item> SCROLL_STRAW_DOLL = scroll("scroll_straw_doll", Technique.STRAW_DOLL);
    public static final RegistryObject<Item> SCROLL_TEN_SHADOWS = scroll("scroll_ten_shadows", Technique.TEN_SHADOWS);
    public static final RegistryObject<Item> SCROLL_PROJECTION = scroll("scroll_projection", Technique.PROJECTION);
    public static final RegistryObject<Item> SCROLL_ICE_FORMATION = scroll("scroll_ice_formation", Technique.ICE_FORMATION);
    public static final RegistryObject<Item> SPECIAL_GRADE_SEAL = ITEMS.register(
        "special_grade_seal", () -> new SpecialGradeSealItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> PLAYFUL_CLOUD = tool("playful_cloud", CursedToolItem.Kind.PLAYFUL_CLOUD, 6, -3.0F);
    public static final RegistryObject<Item> INVERTED_SPEAR_OF_HEAVEN = tool("inverted_spear_of_heaven", CursedToolItem.Kind.INVERTED_SPEAR, 3, -2.0F);
    public static final RegistryObject<Item> SPLIT_SOUL_KATANA = tool("split_soul_katana", CursedToolItem.Kind.SPLIT_SOUL, 4, -2.4F);
    public static final RegistryObject<Item> SLAUGHTER_DEMON = tool("slaughter_demon", CursedToolItem.Kind.SLAUGHTER_DEMON, 1, -2.0F);
    public static final RegistryObject<Item> DRAGON_BONE = tool("dragon_bone", CursedToolItem.Kind.DRAGON_BONE, 7, -3.1F);
    public static final RegistryObject<Item> KAMUTOKE = tool("kamutoke", CursedToolItem.Kind.KAMUTOKE, 4, -2.6F);
    public static final RegistryObject<Item> NANAMI_CLEAVER = tool("nanami_cleaver", CursedToolItem.Kind.NANAMI_CLEAVER, 5, -2.5F);
    public static final RegistryObject<Item> CHAIN_OF_THOUSAND_MILES = tool("chain_of_thousand_miles", CursedToolItem.Kind.CHAIN_OF_THOUSAND_MILES, 3, -2.2F);
    public static final RegistryObject<Item> BLACK_ROPE = tool("black_rope", CursedToolItem.Kind.BLACK_ROPE, 2, -2.0F);
    public static final RegistryObject<Item> EXECUTIONER_SWORD = tool("executioner_sword", CursedToolItem.Kind.EXECUTIONER_SWORD, 6, -2.8F);
    public static final RegistryObject<Item> MIWA_KATANA = tool("miwa_katana", CursedToolItem.Kind.MIWA_KATANA, 5, -2.3F);
    public static final RegistryObject<Item> MAI_REVOLVER = tool("mai_revolver", CursedToolItem.Kind.MAI_REVOLVER, 1, -2.0F);
    public static final RegistryObject<Item> MOMO_BROOM = tool("momo_broom", CursedToolItem.Kind.MOMO_BROOM, 2, -2.4F);
    public static final RegistryObject<Item> INVENTORY_CURSE = ITEMS.register(
        "inventory_curse", () -> new CursedObjects.InventoryCurseItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> PRISON_REALM = ITEMS.register(
        "prison_realm", () -> new CursedObjects.PrisonRealmItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
    );
    public static final RegistryObject<Item> DEATH_PAINTING = ITEMS.register(
        "death_painting", () -> new CursedObjects.DeathPaintingItem(new Properties().stacksTo(4).rarity(Rarity.RARE))
    );
    public static final RegistryObject<Item> SIX_EYES_BLINDFOLD = ITEMS.register(
        "six_eyes_blindfold", () -> new CursedObjects.BlindfoldItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> SCROLL_IDLE_DEATH_GAMBLE = scroll("scroll_idle_death_gamble", Technique.IDLE_DEATH_GAMBLE);
    public static final RegistryObject<Item> SCROLL_LIGHTNING = scroll("scroll_lightning", Technique.LIGHTNING);
    public static final RegistryObject<Item> SCROLL_STAR_RAGE = scroll("scroll_star_rage", Technique.STAR_RAGE);
    public static final RegistryObject<Item> SCROLL_GRANITE_BLAST = scroll("scroll_granite_blast", Technique.GRANITE_BLAST);
    public static final RegistryObject<Item> SCROLL_DISASTER_FLAMES = scroll("scroll_disaster_flames", Technique.DISASTER_FLAMES);
    public static final RegistryObject<Item> SCROLL_IDLE_TRANSFIGURATION = scroll("scroll_idle_transfiguration", Technique.IDLE_TRANSFIGURATION);
    public static final RegistryObject<Item> SCROLL_DISASTER_PLANTS = scroll("scroll_disaster_plants", Technique.DISASTER_PLANTS);
    public static final RegistryObject<Item> SCROLL_DISASTER_TIDES = scroll("scroll_disaster_tides", Technique.DISASTER_TIDES);
    public static final RegistryObject<Item> SCROLL_SMALLPOX = scroll("scroll_smallpox", Technique.SMALLPOX);
    public static final RegistryObject<Item> SCROLL_DEATH_PAINTING = scroll("scroll_death_painting", Technique.DEATH_PAINTING);
    public static final RegistryObject<Item> SCROLL_SWARM = scroll("scroll_swarm", Technique.SWARM);
    public static final RegistryObject<Item> SCROLL_CONSTRUCTION = scroll("scroll_construction", Technique.CONSTRUCTION);
    public static final RegistryObject<Item> SCROLL_SKY_MANIPULATION = scroll("scroll_sky_manipulation", Technique.SKY_MANIPULATION);
    public static final RegistryObject<Item> SCROLL_RIKA = scroll("scroll_rika", Technique.RIKA);
    public static final RegistryObject<Item> SCROLL_REBIRTH = ITEMS.register(
        "scroll_rebirth", () -> new RebirthScrollItem(new Properties().stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> FLY_HEAD_SPAWN_EGG = egg("fly_head_spawn_egg", ModEntities.FLY_HEAD, 2828080, 12721978);
    public static final RegistryObject<Item> ROTTEN_MOUTH_SPAWN_EGG = egg("rotten_mouth_spawn_egg", ModEntities.ROTTEN_MOUTH, 9210494, 5901844);
    public static final RegistryObject<Item> HAND_SPIDER_SPAWN_EGG = egg("hand_spider_spawn_egg", ModEntities.HAND_SPIDER, 13219492, 3806228);
    public static final RegistryObject<Item> HANGED_SPAWN_EGG = egg("hanged_spawn_egg", ModEntities.HANGED, 1973796, 14209216);
    public static final RegistryObject<Item> SAKURA_BOSS_SPAWN_EGG = egg("sakura_boss_spawn_egg", ModEntities.SAKURA_BOSS, 2757648, 16033992);
    public static final RegistryObject<Item> TRANSFIGURED_HUMAN_SPAWN_EGG = egg(
        "transfigured_human_spawn_egg", ModEntities.TRANSFIGURED_HUMAN, 9077880, 3820104
    );
    public static final RegistryObject<Item> EYE_CLUSTER_SPAWN_EGG = egg("eye_cluster_spawn_egg", ModEntities.EYE_CLUSTER, 8006192, 15784080);
    public static final RegistryObject<Item> CURSED_WOMB_SPAWN_EGG = egg("cursed_womb_spawn_egg", ModEntities.CURSED_WOMB, 11037290, 3803664);
    public static final RegistryObject<Item> FINGER_BEARER_SPAWN_EGG = egg("finger_bearer_spawn_egg", ModEntities.FINGER_BEARER, 2762800, 10109183);
    public static final RegistryObject<Item> GRASSHOPPER_SPAWN_EGG = egg("grasshopper_spawn_egg", ModEntities.GRASSHOPPER, 4880938, 14207120);
    public static final RegistryObject<Item> CURSED_MOTH_SPAWN_EGG = egg("cursed_moth_spawn_egg", ModEntities.CURSED_MOTH, 5917242, 14729312);
    public static final RegistryObject<Item> JELLYFISH_SPAWN_EGG = egg("jellyfish_spawn_egg", ModEntities.JELLYFISH, 13154559, 6965936);
    public static final RegistryObject<Item> KAPPA_SPAWN_EGG = egg("kappa_spawn_egg", ModEntities.KAPPA, 4876858, 11061464);
    public static final RegistryObject<Item> CURSED_DOLL_SPAWN_EGG = egg("cursed_doll_spawn_egg", ModEntities.CURSED_DOLL, 15788256, 11542576);
    public static final RegistryObject<Item> KUCHISAKE_SPAWN_EGG = egg("kuchisake_spawn_egg", ModEntities.KUCHISAKE, 13154464, 9046554);
    public static final RegistryObject<Item> HANNYA_MASK_SPAWN_EGG = egg("hannya_mask_spawn_egg", ModEntities.HANNYA_MASK, 15261904, 12595232);
    public static final RegistryObject<Item> CURSED_ONI_SPAWN_EGG = egg("cursed_oni_spawn_egg", ModEntities.CURSED_ONI, 10496544, 2763306);
    public static final RegistryObject<Item> RAINBOW_DRAGON_SPAWN_EGG = egg("rainbow_dragon_spawn_egg", ModEntities.RAINBOW_DRAGON, 3836624, 15769824);
    public static final RegistryObject<Item> SMALLPOX_DEITY_SPAWN_EGG = egg("smallpox_deity_spawn_egg", ModEntities.SMALLPOX_DEITY, 6972496, 12103776);
    public static final RegistryObject<Item> KUROURUSHI_SPAWN_EGG = egg("kurourushi_spawn_egg", ModEntities.KUROURUSHI, 3810320, 13144128);
    public static final RegistryObject<Item> ROKUROKUBI_SPAWN_EGG = egg("rokurokubi_spawn_egg", ModEntities.ROKUROKUBI, 15788256, 6953536);
    public static final RegistryObject<Item> NURE_ONNA_SPAWN_EGG = egg("nure_onna_spawn_egg", ModEntities.NURE_ONNA, 2775626, 1053720);
    public static final RegistryObject<Item> ITTAN_MOMEN_SPAWN_EGG = egg("ittan_momen_spawn_egg", ModEntities.ITTAN_MOMEN, 16052972, 11052184);
    public static final RegistryObject<Item> ESO_SPAWN_EGG = egg("eso_spawn_egg", ModEntities.ESO, 4876842, 9048080);
    public static final RegistryObject<Item> GASHADOKURO_SPAWN_EGG = egg("gashadokuro_spawn_egg", ModEntities.GASHADOKURO, 15261896, 2761760);
    public static final RegistryObject<Item> DAGON_SPAWN_EGG = egg("dagon_spawn_egg", ModEntities.DAGON, 6961706, 2788048);
    public static final RegistryObject<Item> HANAMI_SPAWN_EGG = egg("hanami_spawn_egg", ModEntities.HANAMI, 14209208, 3832352);
    public static final RegistryObject<Item> YUKI_ONNA_SPAWN_EGG = egg("yuki_onna_spawn_egg", ModEntities.YUKI_ONNA, 15791359, 9095423);
    public static final RegistryObject<Item> KASA_OBAKE_SPAWN_EGG = egg("kasa_obake_spawn_egg", ModEntities.KASA_OBAKE, 13119536, 15786176);
    public static final RegistryObject<Item> TENGU_SPAWN_EGG = egg("tengu_spawn_egg", ModEntities.TENGU, 12591136, 1710618);
    public static final RegistryObject<Item> NOPPERABO_SPAWN_EGG = egg("nopperabo_spawn_egg", ModEntities.NOPPERABO, 15260872, 3816004);

    private static RegistryObject<Item> scroll(String name, Technique technique) {
        return ITEMS.register(name, () -> new TechniqueScrollItem(technique, new Properties().stacksTo(1).rarity(Rarity.EPIC)));
    }

    private static RegistryObject<Item> tool(String name, CursedToolItem.Kind kind, int damage, float speed) {
        return ITEMS.register(
            name,
            () -> new CursedToolItem(kind, damage, speed, new Properties().rarity(kind == CursedToolItem.Kind.SLAUGHTER_DEMON ? Rarity.RARE : Rarity.EPIC))
        );
    }

    private static RegistryObject<Item> egg(String name, RegistryObject<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        return ITEMS.register(name, () -> new ForgeSpawnEggItem(type, bg, fg, new Properties()));
    }

    private ModItems() {
    }
}
