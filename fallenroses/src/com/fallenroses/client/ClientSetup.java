package com.fallenroses.client;

import com.fallenroses.client.anim.PlayerAnimClient;
import com.fallenroses.client.hud.AbilityHud;
import com.fallenroses.client.particle.FRParticle;
import com.fallenroses.client.render.CurseRenderer;
import com.fallenroses.client.render.DomainRenderer;
import com.fallenroses.client.render.TechniqueRenderers;
import com.fallenroses.client.vfx.MangaFx;
import com.fallenroses.client.vfx.ScreenFx;
import com.fallenroses.registry.ModEntities;
import com.fallenroses.registry.ModParticles;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
    modid = "fallenroses",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class ClientSetup {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(PlayerAnimClient::register);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : Keybinds.SLOTS) {
            event.register(key);
        }

        event.register(Keybinds.REINFORCE);
        event.register(Keybinds.CHARACTER);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "abilities", new AbilityHud());
        event.registerAboveAll("manga_fx", MangaFx.OVERLAY);
        event.registerAboveAll("screen_fx", ScreenFx.OVERLAY);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GLOW.get(), s -> new FRParticle.Provider(s, FRParticle.Style.GLOW));
        event.registerSpriteSet(ModParticles.SPARK.get(), s -> new FRParticle.Provider(s, FRParticle.Style.SPARK));
        event.registerSpriteSet(ModParticles.SMOKE.get(), s -> new FRParticle.Provider(s, FRParticle.Style.SMOKE));
        event.registerSpriteSet(ModParticles.PETAL.get(), s -> new FRParticle.Provider(s, FRParticle.Style.PETAL));
        event.registerSpriteSet(ModParticles.STAR.get(), s -> new FRParticle.Provider(s, FRParticle.Style.STAR));
        event.registerSpriteSet(ModParticles.EMBER.get(), s -> new FRParticle.Provider(s, FRParticle.Style.EMBER));
    }

    @SubscribeEvent
    public static void renderers(RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLY_HEAD.get(), ctx -> new CurseRenderer<>(ctx, "fly_head", 0.35F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.ROTTEN_MOUTH.get(), ctx -> new CurseRenderer<>(ctx, "rotten_mouth", 0.6F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.HAND_SPIDER.get(), ctx -> new CurseRenderer<>(ctx, "hand_spider", 0.9F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.HANGED.get(), ctx -> new CurseRenderer<>(ctx, "hanged", 0.4F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.SAKURA_BOSS.get(), ctx -> new CurseRenderer<>(ctx, "sakura_boss", 1.4F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.TRANSFIGURED_HUMAN.get(), ctx -> new CurseRenderer<>(ctx, "transfigured_human", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.EYE_CLUSTER.get(), ctx -> new CurseRenderer<>(ctx, "eye_cluster", 0.5F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.CURSED_WOMB.get(), ctx -> new CurseRenderer<>(ctx, "cursed_womb", 0.7F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.FINGER_BEARER.get(), ctx -> new CurseRenderer<>(ctx, "finger_bearer", 0.8F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.GRASSHOPPER.get(), ctx -> new CurseRenderer<>(ctx, "grasshopper", 0.5F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.CURSED_MOTH.get(), ctx -> new CurseRenderer<>(ctx, "cursed_moth", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.JELLYFISH.get(), ctx -> new CurseRenderer<>(ctx, "jellyfish", 0.4F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.KAPPA.get(), ctx -> new CurseRenderer<>(ctx, "kappa", 0.5F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.CURSED_DOLL.get(), ctx -> new CurseRenderer<>(ctx, "cursed_doll", 0.3F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.KUCHISAKE.get(), ctx -> new CurseRenderer<>(ctx, "kuchisake", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.HANNYA_MASK.get(), ctx -> new CurseRenderer<>(ctx, "hannya_mask", 0.4F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.CURSED_ONI.get(), ctx -> new CurseRenderer<>(ctx, "cursed_oni", 0.9F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.RAINBOW_DRAGON.get(), ctx -> new CurseRenderer<>(ctx, "rainbow_dragon", 0.8F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.SMALLPOX_DEITY.get(), ctx -> new CurseRenderer<>(ctx, "smallpox_deity", 0.9F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.DIVINE_DOG.get(), ctx -> new CurseRenderer<>(ctx, "divine_dog", 0.5F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.NUE.get(), ctx -> new CurseRenderer<>(ctx, "nue", 0.6F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.MAHORAGA.get(), ctx -> new CurseRenderer<>(ctx, "mahoraga", 1.2F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.KUROURUSHI.get(), ctx -> new CurseRenderer<>(ctx, "kurourushi", 0.9F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.ROKUROKUBI.get(), ctx -> new CurseRenderer<>(ctx, "rokurokubi", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.NURE_ONNA.get(), ctx -> new CurseRenderer<>(ctx, "nure_onna", 0.7F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.ITTAN_MOMEN.get(), ctx -> new CurseRenderer<>(ctx, "ittan_momen", 0.4F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.ESO.get(), ctx -> new CurseRenderer<>(ctx, "eso", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.GASHADOKURO.get(), ctx -> new CurseRenderer<>(ctx, "gashadokuro", 1.8F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.DAGON.get(), ctx -> new CurseRenderer<>(ctx, "dagon", 1.0F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.HANAMI.get(), ctx -> new CurseRenderer<>(ctx, "hanami", 1.0F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.YUKI_ONNA.get(), ctx -> new CurseRenderer<>(ctx, "yuki_onna", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.KASA_OBAKE.get(), ctx -> new CurseRenderer<>(ctx, "kasa_obake", 0.4F, 1.0F, false));
        event.registerEntityRenderer(ModEntities.TENGU.get(), ctx -> new CurseRenderer<>(ctx, "tengu", 0.6F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.NOPPERABO.get(), ctx -> new CurseRenderer<>(ctx, "nopperabo", 0.5F, 1.0F, true));
        event.registerEntityRenderer(ModEntities.SHOT.get(), TechniqueRenderers.Shot::new);
        event.registerEntityRenderer(ModEntities.BLUE_ORB.get(), TechniqueRenderers.Blue::new);
        event.registerEntityRenderer(ModEntities.RED_BLAST.get(), TechniqueRenderers.Red::new);
        event.registerEntityRenderer(ModEntities.HOLLOW_PURPLE.get(), TechniqueRenderers.Purple::new);
        event.registerEntityRenderer(ModEntities.SLASH.get(), TechniqueRenderers.Slash::new);
        event.registerEntityRenderer(ModEntities.FUGA_ARROW.get(), TechniqueRenderers.Fuga::new);
        event.registerEntityRenderer(ModEntities.CURSE_ORB.get(), TechniqueRenderers.Orb::new);
        event.registerEntityRenderer(ModEntities.DOMAIN.get(), DomainRenderer::new);
    }

    private ClientSetup() {
    }
}
