package com.fallenroses.event;

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
import com.fallenroses.player.PlayerData;
import com.fallenroses.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "fallenroses",
    bus = Bus.MOD
)
public final class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.FLY_HEAD.get(), FlyHeadEntity.createAttributes().build());
        event.put(ModEntities.ROTTEN_MOUTH.get(), RottenMouthEntity.createAttributes().build());
        event.put(ModEntities.HAND_SPIDER.get(), HandSpiderEntity.createAttributes().build());
        event.put(ModEntities.HANGED.get(), HangedEntity.createAttributes().build());
        event.put(ModEntities.SAKURA_BOSS.get(), SakuraBossEntity.createAttributes().build());
        event.put(ModEntities.TRANSFIGURED_HUMAN.get(), TransfiguredHumanEntity.createAttributes().build());
        event.put(ModEntities.EYE_CLUSTER.get(), EyeClusterEntity.createAttributes().build());
        event.put(ModEntities.CURSED_WOMB.get(), CursedWombEntity.createAttributes().build());
        event.put(ModEntities.FINGER_BEARER.get(), FingerBearerEntity.createAttributes().build());
        event.put(ModEntities.GRASSHOPPER.get(), GrasshopperEntity.createAttributes().build());
        event.put(ModEntities.CURSED_MOTH.get(), CursedMothEntity.createAttributes().build());
        event.put(ModEntities.JELLYFISH.get(), JellyfishEntity.createAttributes().build());
        event.put(ModEntities.KAPPA.get(), KappaEntity.createAttributes().build());
        event.put(ModEntities.CURSED_DOLL.get(), CursedDollEntity.createAttributes().build());
        event.put(ModEntities.KUCHISAKE.get(), KuchisakeEntity.createAttributes().build());
        event.put(ModEntities.HANNYA_MASK.get(), HannyaMaskEntity.createAttributes().build());
        event.put(ModEntities.CURSED_ONI.get(), CursedOniEntity.createAttributes().build());
        event.put(ModEntities.RAINBOW_DRAGON.get(), RainbowDragonEntity.createAttributes().build());
        event.put(ModEntities.SMALLPOX_DEITY.get(), SmallpoxDeityEntity.createAttributes().build());
        event.put(ModEntities.DIVINE_DOG.get(), DivineDogEntity.createAttributes().build());
        event.put(ModEntities.NUE.get(), NueEntity.createAttributes().build());
        event.put(ModEntities.MAHORAGA.get(), MahoragaEntity.createAttributes().build());
        event.put(ModEntities.KUROURUSHI.get(), KurourushiEntity.createAttributes().build());
        event.put(ModEntities.ROKUROKUBI.get(), RokurokubiEntity.createAttributes().build());
        event.put(ModEntities.NURE_ONNA.get(), NureOnnaEntity.createAttributes().build());
        event.put(ModEntities.ITTAN_MOMEN.get(), IttanMomenEntity.createAttributes().build());
        event.put(ModEntities.ESO.get(), EsoEntity.createAttributes().build());
        event.put(ModEntities.GASHADOKURO.get(), GashadokuroEntity.createAttributes().build());
        event.put(ModEntities.DAGON.get(), DagonEntity.createAttributes().build());
        event.put(ModEntities.HANAMI.get(), HanamiEntity.createAttributes().build());
        event.put(ModEntities.YUKI_ONNA.get(), YokaiCurses.YukiOnna.createAttributes().build());
        event.put(ModEntities.KASA_OBAKE.get(), YokaiCurses.KasaObake.createAttributes().build());
        event.put(ModEntities.TENGU.get(), YokaiCurses.Tengu.createAttributes().build());
        event.put(ModEntities.NOPPERABO.get(), YokaiCurses.Nopperabo.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(SpawnPlacementRegisterEvent event) {
        ground(event, ModEntities.FLY_HEAD.get());
        ground(event, ModEntities.ROTTEN_MOUTH.get());
        ground(event, ModEntities.HAND_SPIDER.get());
        ground(event, ModEntities.HANGED.get());
        ground(event, ModEntities.SAKURA_BOSS.get());
        ground(event, ModEntities.TRANSFIGURED_HUMAN.get());
        ground(event, ModEntities.EYE_CLUSTER.get());
        ground(event, ModEntities.CURSED_WOMB.get());
        ground(event, ModEntities.FINGER_BEARER.get());
        ground(event, ModEntities.GRASSHOPPER.get());
        ground(event, ModEntities.CURSED_MOTH.get());
        ground(event, ModEntities.JELLYFISH.get());
        ground(event, ModEntities.KAPPA.get());
        ground(event, ModEntities.CURSED_DOLL.get());
        ground(event, ModEntities.KUCHISAKE.get());
        ground(event, ModEntities.HANNYA_MASK.get());
        ground(event, ModEntities.CURSED_ONI.get());
        ground(event, ModEntities.RAINBOW_DRAGON.get());
        ground(event, ModEntities.SMALLPOX_DEITY.get());
        ground(event, ModEntities.KUROURUSHI.get());
        ground(event, ModEntities.ROKUROKUBI.get());
        ground(event, ModEntities.NURE_ONNA.get());
        ground(event, ModEntities.ITTAN_MOMEN.get());
        ground(event, ModEntities.ESO.get());
        ground(event, ModEntities.GASHADOKURO.get());
        ground(event, ModEntities.DAGON.get());
        ground(event, ModEntities.HANAMI.get());
        ground(event, ModEntities.YUKI_ONNA.get());
        ground(event, ModEntities.KASA_OBAKE.get());
        ground(event, ModEntities.TENGU.get());
        ground(event, ModEntities.NOPPERABO.get());
    }

    private static <T extends Monster> void ground(SpawnPlacementRegisterEvent event, EntityType<T> type) {
        event.register(type, Type.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, Operation.REPLACE);
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
    }

    private ModEvents() {
    }
}
