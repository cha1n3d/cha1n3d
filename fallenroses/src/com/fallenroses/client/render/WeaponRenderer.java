package com.fallenroses.client.render;

import com.fallenroses.FallenRoses;
import com.fallenroses.item.CursedToolItem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class WeaponRenderer {
    public static GeoItemRenderer<CursedToolItem> create(String id) {
        GeoItemRenderer<CursedToolItem> renderer = new GeoItemRenderer<>(new WeaponRenderer.Model(id));
        renderer.addRenderLayer(new WeaponRenderer.OptionalGlowLayer(renderer, FallenRoses.id("textures/item/model/" + id + "_glowmask.png")));
        return renderer;
    }

    private WeaponRenderer() {
    }

    private static final class Model extends GeoModel<CursedToolItem> {
        private final ResourceLocation model;
        private final ResourceLocation texture;
        private final ResourceLocation animation;

        Model(String id) {
            this.model = FallenRoses.id("geo/item/" + id + ".geo.json");
            this.texture = FallenRoses.id("textures/item/model/" + id + ".png");
            this.animation = FallenRoses.id("animations/item/" + id + ".animation.json");
        }

        public ResourceLocation getModelResource(CursedToolItem item) {
            return this.model;
        }

        public ResourceLocation getTextureResource(CursedToolItem item) {
            return this.texture;
        }

        public ResourceLocation getAnimationResource(CursedToolItem item) {
            return this.animation;
        }
    }

    private static final class OptionalGlowLayer extends AutoGlowingGeoLayer<CursedToolItem> {
        private final ResourceLocation mask;
        private Boolean present;

        OptionalGlowLayer(GeoItemRenderer<CursedToolItem> renderer, ResourceLocation mask) {
            super(renderer);
            this.mask = mask;
        }

        public void render(
            PoseStack poseStack,
            CursedToolItem animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
        ) {
            if (this.present == null) {
                this.present = hasGlowPixels(this.mask);
            }

            if (this.present) {
                super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        }

        private static boolean hasGlowPixels(ResourceLocation mask) {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(mask);
            if (res.isEmpty()) {
                return false;
            } else {
                try (
                    InputStream in = res.get().open();
                    NativeImage img = NativeImage.read(in);
                ) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        for (int x = 0; x < img.getWidth(); x++) {
                            if ((img.getPixelRGBA(x, y) >>> 24 & 0xFF) > 0) {
                                return true;
                            }
                        }
                    }

                    return false;
                } catch (IOException var11) {
                    FallenRoses.LOGGER.warn("Не удалось прочитать {}", mask, var11);
                    return false;
                }
            }
        }
    }
}
