package com.fallenroses.client.render;

import com.fallenroses.FallenRoses;
import com.fallenroses.entity.curse.CurseEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class CurseRenderer<T extends CurseEntity> extends GeoEntityRenderer<T> {
    private final boolean translucent;

    public CurseRenderer(Context context, String id, float shadow, float scale, boolean translucent) {
        super(context, new DefaultedEntityGeoModel<>(FallenRoses.id(id), true));
        this.shadowRadius = shadow;
        this.translucent = translucent;
        this.withScale(scale);
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource buffer, float partialTick) {
        return this.translucent ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
    }

    protected float getDeathMaxRotation(T animatable) {
        return 0.0F;
    }
}
