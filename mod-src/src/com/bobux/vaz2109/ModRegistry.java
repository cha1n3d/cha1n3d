package com.bobux.vaz2109;

import com.bobux.vaz2109.block.SkateRampBlock;
import com.bobux.vaz2109.car.CarPart;
import com.bobux.vaz2109.car.Palette;
import com.bobux.vaz2109.computer.ComputerBlock;
import com.bobux.vaz2109.computer.ComputerMenu;
import com.bobux.vaz2109.effect.NarcolepsyEffect;
import com.bobux.vaz2109.effect.PowerEffect;
import com.bobux.vaz2109.entity.AllyFrog;
import com.bobux.vaz2109.entity.BloodBeamEntity;
import com.bobux.vaz2109.entity.ScooterEntity;
import com.bobux.vaz2109.item.ScooterItem;
import com.bobux.vaz2109.entity.AndreyEntity;
import com.bobux.vaz2109.entity.ChosoEntity;
import com.bobux.vaz2109.entity.TurretEntity;
import com.bobux.vaz2109.item.DeathPaintingItem;
import com.bobux.vaz2109.entity.ArthasEntity;
import com.bobux.vaz2109.entity.ChainHookEntity;
import com.bobux.vaz2109.entity.FlashbangEntity;
import com.bobux.vaz2109.entity.GaishnikEntity;
import com.bobux.vaz2109.entity.GojoEntity;
import com.bobux.vaz2109.entity.GrenadeEntity;
import com.bobux.vaz2109.entity.IvanEntity;
import com.bobux.vaz2109.entity.JacketEntity;
import com.bobux.vaz2109.entity.MahoragaEntity;
import com.bobux.vaz2109.entity.MelonProjectile;
import com.bobux.vaz2109.entity.MolotovEntity;
import com.bobux.vaz2109.entity.MorgenshternEntity;
import com.bobux.vaz2109.entity.RocketEntity;
import com.bobux.vaz2109.entity.SkateboardEntity;
import com.bobux.vaz2109.entity.TimokhaChainEntity;
import com.bobux.vaz2109.entity.TimokhaEntity;
import com.bobux.vaz2109.entity.TimokhaFrog;
import com.bobux.vaz2109.entity.TimokhaParrot;
import com.bobux.vaz2109.entity.TojiEntity;
import com.bobux.vaz2109.entity.VazEntity;
import com.bobux.vaz2109.entity.curse.BriukhozevEntity;
import com.bobux.vaz2109.entity.curse.DlinnorukEntity;
import com.bobux.vaz2109.entity.curse.FlyheadEntity;
import com.bobux.vaz2109.entity.curse.JogoEntity;
import com.bobux.vaz2109.entity.curse.LupoglazEntity;
import com.bobux.vaz2109.fx.ModSounds;
import com.bobux.vaz2109.item.BeerItem;
import com.bobux.vaz2109.item.ChainItem;
import com.bobux.vaz2109.item.ChainsawItem;
import com.bobux.vaz2109.item.CompoundVItem;
import com.bobux.vaz2109.item.DragonBoneItem;
import com.bobux.vaz2109.item.ExplorerCompassItem;
import com.bobux.vaz2109.item.FoxMaskItem;
import com.bobux.vaz2109.item.FrostmourneItem;
import com.bobux.vaz2109.item.GunItem;
import com.bobux.vaz2109.item.HarutaSwordItem;
import com.bobux.vaz2109.item.InvertedSpearItem;
import com.bobux.vaz2109.item.IvanWatchItem;
import com.bobux.vaz2109.item.KatanaItem;
import com.bobux.vaz2109.item.MaskItem;
import com.bobux.vaz2109.item.MeleeItem;
import com.bobux.vaz2109.item.MorgenPhoneItem;
import com.bobux.vaz2109.item.NanamiBladeItem;
import com.bobux.vaz2109.item.NarcoleptinItem;
import com.bobux.vaz2109.item.PartItem;
import com.bobux.vaz2109.item.PartyHatItem;
import com.bobux.vaz2109.item.PlayfulCloudItem;
import com.bobux.vaz2109.item.QuestBookItem;
import com.bobux.vaz2109.item.SkateboardItem;
import com.bobux.vaz2109.item.SlaughterDemonItem;
import com.bobux.vaz2109.item.SplitSoulKatanaItem;
import com.bobux.vaz2109.item.SprayCanItem;
import com.bobux.vaz2109.item.SukunaFingerItem;
import com.bobux.vaz2109.item.ThrowableWeaponItem;
import com.bobux.vaz2109.item.VazArmorItem;
import com.bobux.vaz2109.item.VazArmorMaterial;
import com.bobux.vaz2109.item.VazItem;
import com.bobux.vaz2109.power.Power;
import com.bobux.vaz2109.station.Station;
import com.bobux.vaz2109.station.StationBlock;
import com.bobux.vaz2109.station.StationMenu;
import com.bobux.vaz2109.station.StationRecipe;
import com.bobux.vaz2109.weapon.Breathing;
import com.bobux.vaz2109.weapon.GunType;
import com.bobux.vaz2109.weapon.Mask;
import com.bobux.vaz2109.world.ModStructures;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
   public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "vaz2109");
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "vaz2109");
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "vaz2109");
   public static final RegistryObject<Block> SKATE_RAMP = BLOCKS.register(
      "skate_ramp", () -> new SkateRampBlock(1.0F, Properties.copy(Blocks.OAK_PLANKS).noOcclusion())
   );
   public static final RegistryObject<Block> SKATE_KICKER = BLOCKS.register(
      "skate_kicker", () -> new SkateRampBlock(0.5F, Properties.copy(Blocks.OAK_PLANKS).noOcclusion())
   );
   public static final RegistryObject<Item> SKATE_RAMP_ITEM = ITEMS.register(
      "skate_ramp", () -> new BlockItem((Block)SKATE_RAMP.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<Item> SKATE_KICKER_ITEM = ITEMS.register(
      "skate_kicker", () -> new BlockItem((Block)SKATE_KICKER.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "vaz2109");
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "vaz2109");
   public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "vaz2109");
   public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "vaz2109");
   public static final RegistryObject<RecipeType<StationRecipe>> STATION_RECIPE = RECIPE_TYPES.register(
      "station", () -> RecipeType.simple(new ResourceLocation("vaz2109", "station"))
   );
   public static final RegistryObject<RecipeSerializer<StationRecipe>> STATION_SERIALIZER = RECIPE_SERIALIZERS.register(
      "station", StationRecipe.Serializer::new
   );
   public static final RegistryObject<MenuType<StationMenu>> STATION_MENU = MENUS.register("station", () -> IForgeMenuType.create(StationMenu::new));
   public static final RegistryObject<MenuType<ComputerMenu>> COMPUTER_MENU = MENUS.register("computer", () -> IForgeMenuType.create(ComputerMenu::new));
   public static final RegistryObject<Block> COMPUTER = BLOCKS.register(
      "computer", () -> new ComputerBlock(Properties.copy(Blocks.IRON_BLOCK).strength(2.5F).noOcclusion().lightLevel(s -> 7))
   );
   public static final RegistryObject<Item> COMPUTER_ITEM = ITEMS.register(
      "computer", () -> new BlockItem((Block)COMPUTER.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final Map<Station, RegistryObject<Block>> STATION_BLOCKS = new EnumMap<>(Station.class);
   public static final Map<Station, RegistryObject<Item>> STATION_ITEMS = new EnumMap<>(Station.class);
   public static final ResourceKey<DamageType> VAZ_DAMAGE;
   public static final RegistryObject<EntityType<VazEntity>> VAZ;
   public static final RegistryObject<EntityType<ArthasEntity>> ARTHAS;
   public static final RegistryObject<EntityType<IvanEntity>> IVAN;
   public static final RegistryObject<EntityType<MelonProjectile>> MELON_ENTITY;
   public static final RegistryObject<EntityType<TimokhaEntity>> TIMOKHA;
   public static final RegistryObject<EntityType<TimokhaFrog>> TIMOKHA_FROG;
   public static final RegistryObject<EntityType<TimokhaParrot>> TIMOKHA_PARROT;
   public static final RegistryObject<EntityType<TimokhaChainEntity>> TIMOKHA_CHAIN;
   public static final RegistryObject<EntityType<ChainHookEntity>> CHAIN_HOOK;
   public static final RegistryObject<EntityType<FlyheadEntity>> CURSE_FLYHEAD;
   public static final RegistryObject<EntityType<LupoglazEntity>> CURSE_LUPOGLAZ;
   public static final RegistryObject<EntityType<DlinnorukEntity>> CURSE_DLINNORUK;
   public static final RegistryObject<EntityType<BriukhozevEntity>> CURSE_BRIUKHOZEV;
   public static final RegistryObject<EntityType<JogoEntity>> JOGO;
   public static final RegistryObject<Item> VAZ_ITEM;
   public static final RegistryObject<EntityType<SkateboardEntity>> SKATEBOARD;
   public static final RegistryObject<Item> SKATEBOARD_ITEM;
   public static final RegistryObject<Item> SPRAY_CAN;
   public static final Map<CarPart, RegistryObject<Item>> PARTS;
   public static final ResourceKey<DamageType> BULLET_DAMAGE;
   public static final Map<String, RegistryObject<Item>> ITEMS_BY_ID;
   public static final Map<GunType, RegistryObject<Item>> GUNS;
   public static final DeferredRegister<MobEffect> EFFECTS;
   public static final RegistryObject<MobEffect> NARCOLEPSY;
   public static final Map<Power, RegistryObject<MobEffect>> POWERS;
   public static final RegistryObject<Item> FOX_MASK;
   public static final RegistryObject<Item> FROSTMOURNE;
   public static final RegistryObject<Item> COMPOUND_V;
   public static final RegistryObject<Item> IVAN_WATCH;
   public static final RegistryObject<Item> EXPLORER_COMPASS;
   public static final RegistryObject<Item> IVAN_SPAWN_EGG;
   public static final RegistryObject<Item> ARTHAS_SPAWN_EGG;
   public static final RegistryObject<Item> HARUTA_SWORD;
   public static final RegistryObject<Item> TIMOKHA_SPAWN_EGG;
   public static final RegistryObject<Item> TIMOKHA_FROG_SPAWN_EGG;
   public static final RegistryObject<Item> TIMOKHA_PARROT_SPAWN_EGG;
   public static final RegistryObject<Item> TIMOKHA_CHAIN_ITEM;
   public static final RegistryObject<Item> TIMOKHA_CHAIN_SPAWN_EGG;
   public static final RegistryObject<Item> NANAMI_BLADE;
   public static final RegistryObject<Item> PLAYFUL_CLOUD;
   public static final RegistryObject<Item> INVERTED_SPEAR;
   public static final RegistryObject<Item> SPLIT_SOUL_KATANA;
   public static final RegistryObject<Item> DRAGON_BONE;
   public static final RegistryObject<Item> SLAUGHTER_DEMON;
   public static final RegistryObject<Item> SUKUNA_FINGER;
   public static final RegistryObject<Item> CURSE_FLYHEAD_SPAWN_EGG;
   public static final RegistryObject<Item> CURSE_LUPOGLAZ_SPAWN_EGG;
   public static final RegistryObject<Item> CURSE_DLINNORUK_SPAWN_EGG;
   public static final RegistryObject<Item> CURSE_BRIUKHOZEV_SPAWN_EGG;
   public static final RegistryObject<Item> JOGO_SPAWN_EGG;
   public static final RegistryObject<EntityType<JacketEntity>> JACKET;
   public static final RegistryObject<EntityType<AndreyEntity>> ANDREY;
   public static final RegistryObject<EntityType<GaishnikEntity>> GAISHNIK;
   public static final RegistryObject<EntityType<TojiEntity>> TOJI;
   public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA;
   public static final RegistryObject<EntityType<GojoEntity>> GOJO;
   public static final RegistryObject<EntityType<MorgenshternEntity>> MORGENSHTERN;
   public static final RegistryObject<Item> JACKET_SPAWN_EGG;
   public static final RegistryObject<Item> ANDREY_SPAWN_EGG;
   public static final RegistryObject<Item> GAISHNIK_SPAWN_EGG;
   public static final RegistryObject<Item> TOJI_SPAWN_EGG;
   public static final RegistryObject<Item> MAHORAGA_SPAWN_EGG;
   public static final RegistryObject<Item> GOJO_SPAWN_EGG;
   public static final RegistryObject<Item> MORGENSHTERN_SPAWN_EGG;
   public static final RegistryObject<EntityType<ChosoEntity>> CHOSO;
   public static final RegistryObject<EntityType<AllyFrog>> ALLY_FROG;
   public static final RegistryObject<EntityType<TurretEntity>> TURRET;
   public static final RegistryObject<EntityType<BloodBeamEntity>> BLOOD_BEAM;
   public static final RegistryObject<EntityType<ScooterEntity>> SCOOTER;
   public static final RegistryObject<Item> SCOOTER_ITEM;
   public static final RegistryObject<Item> CHOSO_SPAWN_EGG;
   public static final RegistryObject<Item> DEATH_PAINTING;
   public static final RegistryObject<Item> QUEST_BOOK;
   public static final RegistryObject<Item> MORGEN_PHONE;
   public static final RegistryObject<Item> BEER;
   public static final List<RegistryObject<Item>> ARMOR;
   public static final List<RegistryObject<Item>> PARTY_HATS;
   public static final List<RegistryObject<Item>> MORGEN_DISCS;
   public static final Map<Mask, RegistryObject<Item>> MASKS;
   public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE_ENTITY;
   public static final RegistryObject<EntityType<FlashbangEntity>> FLASHBANG_ENTITY;
   public static final RegistryObject<EntityType<MolotovEntity>> MOLOTOV_ENTITY;
   public static final RegistryObject<EntityType<RocketEntity>> ROCKET_ENTITY;
   public static final RegistryObject<CreativeModeTab> WEAPONS_TAB;
   public static final RegistryObject<CreativeModeTab> HOTLINE_TAB;
   public static final RegistryObject<CreativeModeTab> TAB;

   private static RegistryObject<Item> item(String id, Supplier<Item> factory) {
      RegistryObject<Item> obj = ITEMS.register(id, factory);
      ITEMS_BY_ID.put(id, obj);
      return obj;
   }

   private ModRegistry() {
   }

   public static void register(IEventBus bus) {
      BLOCKS.register(bus);
      ENTITIES.register(bus);
      ITEMS.register(bus);
      EFFECTS.register(bus);
      MENUS.register(bus);
      RECIPE_TYPES.register(bus);
      RECIPE_SERIALIZERS.register(bus);
      ModStructures.register(bus);
      TABS.register(bus);
   }

   static {
      for (Station st : Station.values()) {
         RegistryObject<Block> block = BLOCKS.register(
            st.id,
            () -> new StationBlock(
                  st,
                  Properties.copy(st == Station.CURSED ? Blocks.POLISHED_BLACKSTONE : (st == Station.MASKS ? Blocks.OAK_PLANKS : Blocks.IRON_BLOCK))
                     .strength(2.5F)
                     .noOcclusion()
                     .lightLevel(s -> st == Station.CURSED ? 6 : 0)
               )
         );
         STATION_BLOCKS.put(st, block);
         STATION_ITEMS.put(st, ITEMS.register(st.id, () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties())));
      }

      VAZ_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("vaz2109", "vaz"));
      VAZ = ENTITIES.register("vaz2109", () -> Builder.of(VazEntity::new, MobCategory.MISC).sized(2.0F, 1.78F).clientTrackingRange(10).build("vaz2109"));
      ARTHAS = ENTITIES.register("arthas", () -> Builder.of(ArthasEntity::new, MobCategory.MONSTER).sized(0.9F, 2.6F).clientTrackingRange(10).build("arthas"));
      IVAN = ENTITIES.register(
         "ivan_parkour", () -> Builder.of(IvanEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(10).build("ivan_parkour")
      );
      MELON_ENTITY = ENTITIES.register(
         "melon", () -> Builder.<MelonProjectile>of((t, l) -> new MelonProjectile(t, l), MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(2).build("melon")
      );
      TIMOKHA = ENTITIES.register(
         "timokha", () -> Builder.of(TimokhaEntity::new, MobCategory.MONSTER).sized(0.7F, 1.9F).clientTrackingRange(10).build("timokha")
      );
      TIMOKHA_FROG = ENTITIES.register(
         "timokha_frog", () -> Builder.of(TimokhaFrog::new, MobCategory.MONSTER).sized(0.5F, 0.5F).clientTrackingRange(10).build("timokha_frog")
      );
      TIMOKHA_PARROT = ENTITIES.register(
         "timokha_parrot", () -> Builder.of(TimokhaParrot::new, MobCategory.MONSTER).sized(0.5F, 0.9F).clientTrackingRange(8).build("timokha_parrot")
      );
      TIMOKHA_CHAIN = ENTITIES.register(
         "timokha_chain", () -> Builder.of(TimokhaChainEntity::new, MobCategory.MONSTER).sized(0.75F, 2.1F).clientTrackingRange(10).build("timokha_chain")
      );
      CHAIN_HOOK = ENTITIES.register(
         "chain_hook", () -> Builder.<ChainHookEntity>of((t, l) -> new ChainHookEntity(t, l), MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(1).build("chain_hook")
      );
      CURSE_FLYHEAD = ENTITIES.register(
         "curse_flyhead", () -> Builder.of(FlyheadEntity::new, MobCategory.MONSTER).sized(0.6F, 0.6F).clientTrackingRange(8).build("curse_flyhead")
      );
      CURSE_LUPOGLAZ = ENTITIES.register(
         "curse_lupoglaz", () -> Builder.of(LupoglazEntity::new, MobCategory.MONSTER).sized(0.9F, 1.3F).clientTrackingRange(8).build("curse_lupoglaz")
      );
      CURSE_DLINNORUK = ENTITIES.register(
         "curse_dlinnoruk", () -> Builder.of(DlinnorukEntity::new, MobCategory.MONSTER).sized(0.8F, 2.3F).clientTrackingRange(10).build("curse_dlinnoruk")
      );
      CURSE_BRIUKHOZEV = ENTITIES.register(
         "curse_briukhozev", () -> Builder.of(BriukhozevEntity::new, MobCategory.MONSTER).sized(1.4F, 2.8F).clientTrackingRange(10).build("curse_briukhozev")
      );
      JOGO = ENTITIES.register(
         "jogo", () -> Builder.of(JogoEntity::new, MobCategory.MONSTER).sized(0.7F, 1.9F).fireImmune().clientTrackingRange(10).build("jogo")
      );
      VAZ_ITEM = ITEMS.register("vaz2109", () -> new VazItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
      SKATEBOARD = ENTITIES.register(
         "skateboard", () -> Builder.of(SkateboardEntity::new, MobCategory.MISC).sized(0.7F, 0.18F).clientTrackingRange(8).build("skateboard")
      );
      SKATEBOARD_ITEM = ITEMS.register("skateboard", () -> new SkateboardItem(new net.minecraft.world.item.Item.Properties()));
      SPRAY_CAN = ITEMS.register("spray_can", () -> new SprayCanItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
      PARTS = new EnumMap<>(CarPart.class);

      for (CarPart part : CarPart.values()) {
         PARTS.put(part, ITEMS.register(part.id, () -> new PartItem(part, new net.minecraft.world.item.Item.Properties().stacksTo(16))));
      }

      BULLET_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("vaz2109", "bullet"));
      ITEMS_BY_ID = new LinkedHashMap<>();
      GUNS = new EnumMap<>(GunType.class);

      for (GunType type : GunType.values()) {
         RegistryObject<Item> gun = ITEMS.register(type.id, () -> new GunItem(type, new net.minecraft.world.item.Item.Properties().stacksTo(1)));
         GUNS.put(type, gun);
         ITEMS_BY_ID.put(type.id, gun);
      }

      for (GunType type : GunType.values()) {
         int stack = type == GunType.RPG7 ? 8 : 64;
         ITEMS_BY_ID.put(type.ammoId, ITEMS.register(type.ammoId, () -> new Item(new net.minecraft.world.item.Item.Properties().stacksTo(stack))));
      }

      item("bat", () -> new MeleeItem(MeleeItem.Kind.BAT, Tiers.WOOD, 4, -2.8F, new net.minecraft.world.item.Item.Properties()));
      item("crowbar", () -> new MeleeItem(MeleeItem.Kind.CROWBAR, Tiers.IRON, 3, -2.6F, new net.minecraft.world.item.Item.Properties()));
      item("kastet", () -> new MeleeItem(MeleeItem.Kind.KASTET, Tiers.IRON, 1, -0.5F, new net.minecraft.world.item.Item.Properties()));
      item("machete", () -> new MeleeItem(MeleeItem.Kind.MACHETE, Tiers.IRON, 4, -2.5F, new net.minecraft.world.item.Item.Properties()));
      item("grenade", () -> new ThrowableWeaponItem(ThrowableWeaponItem.Kind.GRENADE, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
      item("molotov", () -> new ThrowableWeaponItem(ThrowableWeaponItem.Kind.MOLOTOV, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
      item("flashbang", () -> new ThrowableWeaponItem(ThrowableWeaponItem.Kind.FLASHBANG, new net.minecraft.world.item.Item.Properties().stacksTo(16)));
      item("nichirin_water", () -> new KatanaItem(Breathing.WATER, new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE)));
      item("nichirin_flame", () -> new KatanaItem(Breathing.FLAME, new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE)));
      item("chainsaw", () -> new ChainsawItem(new net.minecraft.world.item.Item.Properties().durability(1200).rarity(Rarity.UNCOMMON)));
      item("narcoleptin", () -> new NarcoleptinItem(new net.minecraft.world.item.Item.Properties()));
      EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "vaz2109");
      NARCOLEPSY = EFFECTS.register("narcolepsy", NarcolepsyEffect::new);
      POWERS = new EnumMap<>(Power.class);

      for (Power power : Power.values()) {
         POWERS.put(power, EFFECTS.register("v_" + power.id, () -> new PowerEffect(power)));
      }

      FOX_MASK = item("fox_mask", () -> new FoxMaskItem(new net.minecraft.world.item.Item.Properties()));
      FROSTMOURNE = item("frostmourne", () -> new FrostmourneItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      COMPOUND_V = item("compound_v", () -> new CompoundVItem(new net.minecraft.world.item.Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
      IVAN_WATCH = item("ivan_watch", () -> new IvanWatchItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
      EXPLORER_COMPASS = item("explorer_compass", () -> new ExplorerCompassItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
      IVAN_SPAWN_EGG = item("ivan_parkour_spawn_egg", () -> new ForgeSpawnEggItem(IVAN, 1447448, 3967534, new net.minecraft.world.item.Item.Properties()));
      ARTHAS_SPAWN_EGG = item("arthas_spawn_egg", () -> new ForgeSpawnEggItem(ARTHAS, 1976888, 9431295, new net.minecraft.world.item.Item.Properties()));
      HARUTA_SWORD = item("haruta_sword", () -> new HarutaSwordItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      TIMOKHA_SPAWN_EGG = item("timokha_spawn_egg", () -> new ForgeSpawnEggItem(TIMOKHA, 14275526, 9334348, new net.minecraft.world.item.Item.Properties()));
      TIMOKHA_FROG_SPAWN_EGG = item(
         "timokha_frog_spawn_egg", () -> new ForgeSpawnEggItem(TIMOKHA_FROG, 6195770, 14729312, new net.minecraft.world.item.Item.Properties())
      );
      TIMOKHA_PARROT_SPAWN_EGG = item(
         "timokha_parrot_spawn_egg", () -> new ForgeSpawnEggItem(TIMOKHA_PARROT, 13642272, 2783952, new net.minecraft.world.item.Item.Properties())
      );
      TIMOKHA_CHAIN_ITEM = item("timokha_chain", () -> new ChainItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      TIMOKHA_CHAIN_SPAWN_EGG = item(
         "timokha_chain_spawn_egg", () -> new ForgeSpawnEggItem(TIMOKHA_CHAIN, 1710621, 16726564, new net.minecraft.world.item.Item.Properties())
      );
      NANAMI_BLADE = item("nanami_blade", () -> new NanamiBladeItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      PLAYFUL_CLOUD = item("playful_cloud", () -> new PlayfulCloudItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      INVERTED_SPEAR = item("inverted_spear", () -> new InvertedSpearItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      SPLIT_SOUL_KATANA = item(
         "split_soul_katana", () -> new SplitSoulKatanaItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant())
      );
      DRAGON_BONE = item("dragon_bone", () -> new DragonBoneItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.EPIC).fireResistant()));
      SLAUGHTER_DEMON = item("slaughter_demon", () -> new SlaughterDemonItem(new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE)));
      SUKUNA_FINGER = item(
         "sukuna_finger", () -> new SukunaFingerItem(new net.minecraft.world.item.Item.Properties().stacksTo(20).rarity(Rarity.EPIC).fireResistant())
      );
      CURSE_FLYHEAD_SPAWN_EGG = item(
         "curse_flyhead_spawn_egg", () -> new ForgeSpawnEggItem(CURSE_FLYHEAD, 2827312, 12592170, new net.minecraft.world.item.Item.Properties())
      );
      CURSE_LUPOGLAZ_SPAWN_EGG = item(
         "curse_lupoglaz_spawn_egg", () -> new ForgeSpawnEggItem(CURSE_LUPOGLAZ, 7240277, 15913530, new net.minecraft.world.item.Item.Properties())
      );
      CURSE_DLINNORUK_SPAWN_EGG = item(
         "curse_dlinnoruk_spawn_egg", () -> new ForgeSpawnEggItem(CURSE_DLINNORUK, 14209732, 1710618, new net.minecraft.world.item.Item.Properties())
      );
      CURSE_BRIUKHOZEV_SPAWN_EGG = item(
         "curse_briukhozev_spawn_egg", () -> new ForgeSpawnEggItem(CURSE_BRIUKHOZEV, 2762032, 10173695, new net.minecraft.world.item.Item.Properties())
      );
      JOGO_SPAWN_EGG = item("jogo_spawn_egg", () -> new ForgeSpawnEggItem(JOGO, 15262422, 16738842, new net.minecraft.world.item.Item.Properties()));
      JACKET = ENTITIES.register("jacket", () -> Builder.of(JacketEntity::new, MobCategory.MONSTER).sized(0.6F, 1.9F).clientTrackingRange(10).build("jacket"));
      ANDREY = ENTITIES.register(
         "andrey_brewer", () -> Builder.of(AndreyEntity::new, MobCategory.MONSTER).sized(0.8F, 2.15F).clientTrackingRange(10).build("andrey_brewer")
      );
      GAISHNIK = ENTITIES.register(
         "gaishnik", () -> Builder.of(GaishnikEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10).build("gaishnik")
      );
      TOJI = ENTITIES.register("toji", () -> Builder.of(TojiEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10).build("toji"));
      MAHORAGA = ENTITIES.register(
         "mahoraga", () -> Builder.of(MahoragaEntity::new, MobCategory.MONSTER).sized(0.9F, 3.0F).fireImmune().clientTrackingRange(12).build("mahoraga")
      );
      GOJO = ENTITIES.register(
         "gojo", () -> Builder.of(GojoEntity::new, MobCategory.MONSTER).sized(0.6F, 2.0F).fireImmune().clientTrackingRange(12).build("gojo")
      );
      MORGENSHTERN = ENTITIES.register(
         "morgenshtern", () -> Builder.of(MorgenshternEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).build("morgenshtern")
      );
      JACKET_SPAWN_EGG = item("jacket_spawn_egg", () -> new ForgeSpawnEggItem(JACKET, 15261896, 14164000, new net.minecraft.world.item.Item.Properties()));
      ANDREY_SPAWN_EGG = item("andrey_brewer_spawn_egg", () -> new ForgeSpawnEggItem(ANDREY, 6964260, 14721072, new net.minecraft.world.item.Item.Properties()));
      GAISHNIK_SPAWN_EGG = item("gaishnik_spawn_egg", () -> new ForgeSpawnEggItem(GAISHNIK, 3820138, 13168698, new net.minecraft.world.item.Item.Properties()));
      TOJI_SPAWN_EGG = item("toji_spawn_egg", () -> new ForgeSpawnEggItem(TOJI, 1447448, 14211280, new net.minecraft.world.item.Item.Properties()));
      MAHORAGA_SPAWN_EGG = item("mahoraga_spawn_egg", () -> new ForgeSpawnEggItem(MAHORAGA, 15592160, 13148224, new net.minecraft.world.item.Item.Properties()));
      GOJO_SPAWN_EGG = item("gojo_spawn_egg", () -> new ForgeSpawnEggItem(GOJO, 1711146, 15791359, new net.minecraft.world.item.Item.Properties()));
      MORGENSHTERN_SPAWN_EGG = item(
         "morgenshtern_spawn_egg", () -> new ForgeSpawnEggItem(MORGENSHTERN, 1447446, 14200880, new net.minecraft.world.item.Item.Properties())
      );
      CHOSO = ENTITIES.register(
         "choso", () -> Builder.<ChosoEntity>of((t, l) -> new ChosoEntity(t, l), MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(10).build("choso")
      );
      ALLY_FROG = ENTITIES.register(
         "ally_frog", () -> Builder.<AllyFrog>of((t, l) -> new AllyFrog(t, l), MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(10).build("ally_frog")
      );
      TURRET = ENTITIES.register(
         "turret",
         () -> Builder.<TurretEntity>of((t, l) -> new TurretEntity(t, l), MobCategory.MISC).sized(0.8F, 1.1F).clientTrackingRange(10).updateInterval(2).build("turret")
      );
      BLOOD_BEAM = ENTITIES.register(
         "blood_beam",
         () -> Builder.<BloodBeamEntity>of((t, l) -> new BloodBeamEntity(t, l), MobCategory.MISC).sized(0.2F, 0.2F).noSave().clientTrackingRange(10).updateInterval(1).build("blood_beam")
      );
      SCOOTER = ENTITIES.register(
         "scooter", () -> Builder.<ScooterEntity>of((t, l) -> new ScooterEntity(t, l), MobCategory.MISC).sized(0.6F, 0.25F).clientTrackingRange(8).build("scooter")
      );
      SCOOTER_ITEM = ITEMS.register("scooter", () -> new ScooterItem(new net.minecraft.world.item.Item.Properties()));
      CHOSO_SPAWN_EGG = item("choso_spawn_egg", () -> new ForgeSpawnEggItem(CHOSO, 2759184, 9442331, new net.minecraft.world.item.Item.Properties()));
      DEATH_PAINTING = item(
         "death_painting", () -> new DeathPaintingItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
      );
      QUEST_BOOK = item("quest_book", () -> new QuestBookItem(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
      MORGEN_PHONE = item("morgen_phone", () -> new MorgenPhoneItem(new net.minecraft.world.item.Item.Properties().durability(3).rarity(Rarity.RARE)));
      BEER = item("beer", () -> new BeerItem(new net.minecraft.world.item.Item.Properties().stacksTo(16)));
      ARMOR = new ArrayList<>();
      PARTY_HATS = new ArrayList<>();

      for (VazArmorMaterial set : VazArmorMaterial.values()) {
         Rarity rarity = set == VazArmorMaterial.SARONITE ? Rarity.EPIC : (set == VazArmorMaterial.JUJUTSU ? Rarity.RARE : Rarity.UNCOMMON);

         for (Type type : new Type[]{Type.HELMET, Type.CHESTPLATE, Type.LEGGINGS, Type.BOOTS}) {
            ARMOR.add(
               item(
                  set.id() + "_" + type.getName(),
                  () -> new VazArmorItem(
                        set,
                        type,
                        set == VazArmorMaterial.SARONITE
                           ? new net.minecraft.world.item.Item.Properties().rarity(rarity).fireResistant()
                           : new net.minecraft.world.item.Item.Properties().rarity(rarity)
                     )
               )
            );
         }
      }

      for (int i = 0; i < PartyHatItem.COLORS.length; i++) {
         int hat = i;
         PARTY_HATS.add(item("party_hat_" + PartyHatItem.COLORS[i], () -> new PartyHatItem(hat, new net.minecraft.world.item.Item.Properties())));
      }

      MORGEN_DISCS = new ArrayList<>();

      for (int i = 1; i <= 3; i++) {
         int n = i;
         MORGEN_DISCS.add(
            item(
               "music_disc_morgen_" + n,
               () -> new RecordItem(
                     10 + n,
                     () -> (SoundEvent)ModSounds.MORGEN_DISCS.get(n - 1).get(),
                     new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                     ModSounds.MORGEN_LENGTHS[n - 1] * 20
                  )
            )
         );
      }

      MASKS = new EnumMap<>(Mask.class);

      for (Mask mask : Mask.values()) {
         MASKS.put(mask, ITEMS.register(mask.itemId(), () -> new MaskItem(mask, new net.minecraft.world.item.Item.Properties().rarity(Rarity.UNCOMMON))));
      }

      GRENADE_ENTITY = ENTITIES.register(
         "grenade", () -> Builder.<GrenadeEntity>of((t, l) -> new GrenadeEntity(t, l), MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5).build("grenade")
      );
      FLASHBANG_ENTITY = ENTITIES.register(
         "flashbang", () -> Builder.<FlashbangEntity>of((t, l) -> new FlashbangEntity(t, l), MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5).build("flashbang")
      );
      MOLOTOV_ENTITY = ENTITIES.register(
         "molotov", () -> Builder.<MolotovEntity>of((t, l) -> new MolotovEntity(t, l), MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("molotov")
      );
      ROCKET_ENTITY = ENTITIES.register(
         "rocket", () -> Builder.<RocketEntity>of((t, l) -> new RocketEntity(t, l), MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(2).build("rocket")
      );
      WEAPONS_TAB = TABS.register(
         "weapons",
         () -> CreativeModeTab.builder()
               .title(Component.translatable("itemGroup.vaz2109.weapons"))
               .icon(() -> GunItem.loaded(GunType.AK74))
               .withTabsBefore(new ResourceLocation[]{new ResourceLocation("vaz2109", "main")})
               .displayItems((params, out) -> {
                  for (Entry<String, RegistryObject<Item>> e : ITEMS_BY_ID.entrySet()) {
                     Item item = (Item)e.getValue().get();
                     out.accept(item instanceof GunItem gun ? GunItem.loaded(gun.type) : new ItemStack(item));
                  }
               })
               .build()
      );
      HOTLINE_TAB = TABS.register(
         "hotline",
         () -> CreativeModeTab.builder()
               .title(Component.translatable("itemGroup.vaz2109.hotline"))
               .icon(() -> new ItemStack((ItemLike)MASKS.get(Mask.RICHARD).get()))
               .withTabsBefore(new ResourceLocation[]{new ResourceLocation("vaz2109", "weapons")})
               .displayItems((params, out) -> {
                  for (RegistryObject<Item> maskx : MASKS.values()) {
                     out.accept((ItemLike)maskx.get());
                  }
               })
               .build()
      );
      TAB = TABS.register(
         "main",
         () -> CreativeModeTab.builder()
               .title(Component.translatable("itemGroup.vaz2109"))
               .icon(() -> new ItemStack((ItemLike)VAZ_ITEM.get()))
               .displayItems((params, out) -> {
                  out.accept((ItemLike)VAZ_ITEM.get());

                  for (int color : Palette.FACTORY) {
                     out.accept(VazItem.withColor(color));
                  }

                  out.accept(VazItem.tuned(Palette.fromDye(DyeColor.BLACK), CarPart.allMask()));

                  for (CarPart part : CarPart.values()) {
                     out.accept((ItemLike)PARTS.get(part).get());
                  }

                  out.accept((ItemLike)SPRAY_CAN.get());
                  out.accept(SprayCanItem.preset(Palette.COUNT, 4));
                  out.accept(SprayCanItem.preset(0, 3));
                  out.accept((ItemLike)SKATEBOARD_ITEM.get());
                  out.accept((ItemLike)SCOOTER_ITEM.get());
                  out.accept((ItemLike)COMPUTER_ITEM.get());

                  for (int color : new int[]{14, 1, 4, 5, 3, 11, 10, 6, 15, 0}) {
                     out.accept(SkateboardItem.withColor(color));
                  }

                  out.accept((ItemLike)SKATE_RAMP_ITEM.get());
                  out.accept((ItemLike)SKATE_KICKER_ITEM.get());

                  for (RegistryObject<Item> bench : STATION_ITEMS.values()) {
                     out.accept((ItemLike)bench.get());
                  }
               })
               .build()
      );
   }
}
