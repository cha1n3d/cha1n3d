package com.fallenroses.client.vfx;

import com.fallenroses.FallenRoses;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class FRRenderTypes extends RenderType {
    public static final ResourceLocation GLOW = FallenRoses.id("textures/vfx/glow.png");
    public static final ResourceLocation RING = FallenRoses.id("textures/vfx/ring.png");
    public static final ResourceLocation SLASH = FallenRoses.id("textures/vfx/slash.png");
    public static final ResourceLocation BEAM = FallenRoses.id("textures/vfx/beam.png");
    public static final ResourceLocation WHITE = FallenRoses.id("textures/vfx/white.png");
    public static final ResourceLocation STARS = FallenRoses.id("textures/vfx/stars.png");
    public static final ResourceLocation SEAL = FallenRoses.id("textures/vfx/seal.png");
    public static final ResourceLocation CRACKS = FallenRoses.id("textures/vfx/cracks.png");
    public static final ResourceLocation SHOCK = FallenRoses.id("textures/vfx/shock.png");
    public static final ResourceLocation BARRIER = FallenRoses.id("textures/vfx/barrier.png");
    public static final ResourceLocation VEINS = FallenRoses.id("textures/vfx/veins.png");
    public static final ResourceLocation SHARD = FallenRoses.id("textures/vfx/shard.png");
    private static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(
        tex -> create(
                "fallenroses_additive",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                Mode.QUADS,
                4096,
                false,
                true,
                CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TRANSLUCENT = Util.memoize(
        tex -> create(
                "fallenroses_translucent",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                Mode.QUADS,
                4096,
                false,
                true,
                CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
            )
    );
    /** Like translucent but writes depth: a domain barrier hides the clouds, particles and water behind it. */
    private static final Function<ResourceLocation, RenderType> SHELL = Util.memoize(
        tex -> create(
                "fallenroses_shell",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                Mode.QUADS,
                4096,
                false,
                false,
                CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
            )
    );
    public static final RenderType GUI_SHAPES = create(
        "fallenroses_gui_shapes",
        DefaultVertexFormat.POSITION_COLOR,
        Mode.QUADS,
        65536,
        false,
        false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_GUI_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .createCompositeState(false)
    );

    public static RenderType additive(ResourceLocation texture) {
        return ADDITIVE.apply(texture);
    }

    public static RenderType translucent(ResourceLocation texture) {
        return TRANSLUCENT.apply(texture);
    }

    public static RenderType shell(ResourceLocation texture) {
        return SHELL.apply(texture);
    }

    private FRRenderTypes(String name, VertexFormat format, Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
