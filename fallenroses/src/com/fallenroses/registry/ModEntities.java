package com.fallenroses.registry;

import com.fallenroses.entity.BlueOrbEntity;
import com.fallenroses.entity.CurseOrbEntity;
import com.fallenroses.entity.DomainEntity;
import com.fallenroses.entity.FugaArrowEntity;
import com.fallenroses.entity.HollowPurpleEntity;
import com.fallenroses.entity.RedBlastEntity;
import com.fallenroses.entity.ShotEntity;
import com.fallenroses.entity.SlashEntity;
import com.fallenroses.entity.curse.CursedDollEntity;
import com.fallenroses.entity.curse.CursedMothEntity;
import com.fallenroses.entity.curse.CursedOniEntity;
import com.fallenroses.entity.curse.CursedWombEntity;
import com.fallenroses.entity.curse.DagonEntity;
import com.fallenroses.entity.curse.DivineDogEntity;
import com.fallenroses.entity.curse.EsoEntity;
import com.fallenroses.entity.curse.EyeClusterEntity;
import com.fallenroses.entity.curse.FingerBearerEntity;
import com.fallenroses.entity.curse.FlyHeadEntity;
import com.fallenroses.entity.curse.GashadokuroEntity;
import com.fallenroses.entity.curse.GrasshopperEntity;
import com.fallenroses.entity.curse.HanamiEntity;
import com.fallenroses.entity.curse.HandSpiderEntity;
import com.fallenroses.entity.curse.HangedEntity;
import com.fallenroses.entity.curse.HannyaMaskEntity;
import com.fallenroses.entity.curse.IttanMomenEntity;
import com.fallenroses.entity.curse.JellyfishEntity;
import com.fallenroses.entity.curse.KappaEntity;
import com.fallenroses.entity.curse.KuchisakeEntity;
import com.fallenroses.entity.curse.KurourushiEntity;
import com.fallenroses.entity.curse.MahoragaEntity;
import com.fallenroses.entity.curse.NueEntity;
import com.fallenroses.entity.curse.NureOnnaEntity;
import com.fallenroses.entity.curse.RainbowDragonEntity;
import com.fallenroses.entity.curse.RokurokubiEntity;
import com.fallenroses.entity.curse.RottenMouthEntity;
import com.fallenroses.entity.curse.SakuraBossEntity;
import com.fallenroses.entity.curse.SmallpoxDeityEntity;
import com.fallenroses.entity.curse.TransfiguredHumanEntity;
import com.fallenroses.entity.curse.YokaiCurses;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "fallenroses");
    public static final RegistryObject<EntityType<FlyHeadEntity>> FLY_HEAD = monster("fly_head", FlyHeadEntity::new, 0.7F, 0.7F);
    public static final RegistryObject<EntityType<RottenMouthEntity>> ROTTEN_MOUTH = monster("rotten_mouth", RottenMouthEntity::new, 0.9F, 2.0F);
    public static final RegistryObject<EntityType<HandSpiderEntity>> HAND_SPIDER = monster("hand_spider", HandSpiderEntity::new, 1.6F, 1.0F);
    public static final RegistryObject<EntityType<HangedEntity>> HANGED = monster("hanged", HangedEntity::new, 0.8F, 2.4F);
    public static final RegistryObject<EntityType<SakuraBossEntity>> SAKURA_BOSS = ENTITIES.register(
        "sakura_boss", () -> Builder.of(SakuraBossEntity::new, MobCategory.MONSTER).sized(1.6F, 4.2F).fireImmune().clientTrackingRange(10).build("sakura_boss")
    );
    public static final RegistryObject<EntityType<TransfiguredHumanEntity>> TRANSFIGURED_HUMAN = monster(
        "transfigured_human", TransfiguredHumanEntity::new, 0.8F, 1.9F
    );
    public static final RegistryObject<EntityType<EyeClusterEntity>> EYE_CLUSTER = monster("eye_cluster", EyeClusterEntity::new, 1.1F, 1.1F);
    public static final RegistryObject<EntityType<CursedWombEntity>> CURSED_WOMB = monster("cursed_womb", CursedWombEntity::new, 1.3F, 1.2F);
    public static final RegistryObject<EntityType<FingerBearerEntity>> FINGER_BEARER = monster("finger_bearer", FingerBearerEntity::new, 1.0F, 3.3F);
    public static final RegistryObject<EntityType<GrasshopperEntity>> GRASSHOPPER = monster("grasshopper", GrasshopperEntity::new, 0.9F, 0.9F);
    public static final RegistryObject<EntityType<CursedMothEntity>> CURSED_MOTH = monster("cursed_moth", CursedMothEntity::new, 1.2F, 1.0F);
    public static final RegistryObject<EntityType<JellyfishEntity>> JELLYFISH = monster("jellyfish", JellyfishEntity::new, 1.0F, 1.6F);
    public static final RegistryObject<EntityType<KappaEntity>> KAPPA = monster("kappa", KappaEntity::new, 0.8F, 1.6F);
    public static final RegistryObject<EntityType<CursedDollEntity>> CURSED_DOLL = monster("cursed_doll", CursedDollEntity::new, 0.5F, 0.9F);
    public static final RegistryObject<EntityType<KuchisakeEntity>> KUCHISAKE = monster("kuchisake", KuchisakeEntity::new, 0.7F, 2.3F);
    public static final RegistryObject<EntityType<HannyaMaskEntity>> HANNYA_MASK = ENTITIES.register(
        "hannya_mask", () -> Builder.of(HannyaMaskEntity::new, MobCategory.MONSTER).sized(0.9F, 1.0F).fireImmune().clientTrackingRange(8).build("hannya_mask")
    );
    public static final RegistryObject<EntityType<CursedOniEntity>> CURSED_ONI = monster("cursed_oni", CursedOniEntity::new, 1.2F, 3.0F);
    public static final RegistryObject<EntityType<RainbowDragonEntity>> RAINBOW_DRAGON = ENTITIES.register(
        "rainbow_dragon", () -> Builder.of(RainbowDragonEntity::new, MobCategory.MONSTER).sized(1.6F, 1.4F).clientTrackingRange(10).build("rainbow_dragon")
    );
    public static final RegistryObject<EntityType<SmallpoxDeityEntity>> SMALLPOX_DEITY = monster("smallpox_deity", SmallpoxDeityEntity::new, 1.2F, 3.2F);
    public static final RegistryObject<EntityType<KurourushiEntity>> KUROURUSHI = monster("kurourushi", KurourushiEntity::new, 1.6F, 1.2F);
    public static final RegistryObject<EntityType<RokurokubiEntity>> ROKUROKUBI = monster("rokurokubi", RokurokubiEntity::new, 0.7F, 2.2F);
    public static final RegistryObject<EntityType<NureOnnaEntity>> NURE_ONNA = monster("nure_onna", NureOnnaEntity::new, 1.0F, 1.7F);
    public static final RegistryObject<EntityType<IttanMomenEntity>> ITTAN_MOMEN = monster("ittan_momen", IttanMomenEntity::new, 1.2F, 0.5F);
    public static final RegistryObject<EntityType<EsoEntity>> ESO = monster("eso", EsoEntity::new, 0.8F, 2.1F);
    public static final RegistryObject<EntityType<GashadokuroEntity>> GASHADOKURO = ENTITIES.register(
        "gashadokuro",
        () -> Builder.of(GashadokuroEntity::new, MobCategory.MONSTER).sized(2.6F, 7.0F).fireImmune().clientTrackingRange(12).build("gashadokuro")
    );
    public static final RegistryObject<EntityType<DagonEntity>> DAGON = ENTITIES.register(
        "dagon", () -> Builder.of(DagonEntity::new, MobCategory.MONSTER).sized(1.4F, 3.2F).clientTrackingRange(10).build("dagon")
    );
    public static final RegistryObject<EntityType<HanamiEntity>> HANAMI = ENTITIES.register(
        "hanami", () -> Builder.of(HanamiEntity::new, MobCategory.MONSTER).sized(1.3F, 3.4F).clientTrackingRange(10).build("hanami")
    );
    public static final RegistryObject<EntityType<YokaiCurses.YukiOnna>> YUKI_ONNA = monster("yuki_onna", YokaiCurses.YukiOnna::new, 0.7F, 2.1F);
    public static final RegistryObject<EntityType<YokaiCurses.KasaObake>> KASA_OBAKE = monster("kasa_obake", YokaiCurses.KasaObake::new, 0.8F, 1.3F);
    public static final RegistryObject<EntityType<YokaiCurses.Tengu>> TENGU = monster("tengu", YokaiCurses.Tengu::new, 0.9F, 2.2F);
    public static final RegistryObject<EntityType<YokaiCurses.Nopperabo>> NOPPERABO = monster("nopperabo", YokaiCurses.Nopperabo::new, 0.6F, 1.9F);
    public static final RegistryObject<EntityType<DivineDogEntity>> DIVINE_DOG = monster("divine_dog", DivineDogEntity::new, 0.8F, 1.0F);
    public static final RegistryObject<EntityType<NueEntity>> NUE = monster("nue", NueEntity::new, 1.4F, 1.2F);
    public static final RegistryObject<EntityType<MahoragaEntity>> MAHORAGA = ENTITIES.register(
        "mahoraga", () -> Builder.of(MahoragaEntity::new, MobCategory.MONSTER).sized(1.4F, 4.0F).fireImmune().clientTrackingRange(10).build("mahoraga")
    );
    public static final RegistryObject<EntityType<BlueOrbEntity>> BLUE_ORB = misc("blue_orb", BlueOrbEntity::new, 0.5F, 2);
    public static final RegistryObject<EntityType<RedBlastEntity>> RED_BLAST = misc("red_blast", RedBlastEntity::new, 0.6F, 1);
    public static final RegistryObject<EntityType<HollowPurpleEntity>> HOLLOW_PURPLE = misc("hollow_purple", HollowPurpleEntity::new, 1.0F, 1);
    public static final RegistryObject<EntityType<SlashEntity>> SLASH = misc("slash", SlashEntity::new, 0.8F, 1);
    public static final RegistryObject<EntityType<FugaArrowEntity>> FUGA_ARROW = misc("fuga_arrow", FugaArrowEntity::new, 0.5F, 1);
    public static final RegistryObject<EntityType<CurseOrbEntity>> CURSE_ORB = misc("curse_orb", CurseOrbEntity::new, 0.4F, 1);
    public static final RegistryObject<EntityType<DomainEntity>> DOMAIN = misc("domain", DomainEntity::new, 0.5F, 20);
    public static final RegistryObject<EntityType<ShotEntity>> SHOT = misc("shot", ShotEntity::new, 0.5F, 1);

    private static <T extends Mob> RegistryObject<EntityType<T>> monster(String name, EntityFactory<T> factory, float w, float h) {
        return ENTITIES.register(name, () -> Builder.of(factory, MobCategory.MONSTER).sized(w, h).clientTrackingRange(8).build(name));
    }

    private static <T extends Entity> RegistryObject<EntityType<T>> misc(String name, EntityFactory<T> factory, float size, int interval) {
        return ENTITIES.register(
            name, () -> Builder.of(factory, MobCategory.MISC).sized(size, size).clientTrackingRange(10).updateInterval(interval).fireImmune().build(name)
        );
    }

    private ModEntities() {
    }
}
