package com.fallenroses.client.anim;

import com.fallenroses.FallenRoses;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class PlayerAnimClient {
    public static final ResourceLocation LAYER = FallenRoses.id("cast");

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1500, player -> new ModifierLayer());
    }

    public static void play(int entityId, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            if (mc.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
                KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(FallenRoses.id(name));
                if (anim != null) {
                    if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof ModifierLayer<?> raw) {
                        KeyframeAnimationPlayer playerAnim = new KeyframeAnimationPlayer(anim)
                            .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)
                            .setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
                        ((ModifierLayer<KeyframeAnimationPlayer>)raw).replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(3, Ease.OUTQUAD), playerAnim);
                    }
                }
            }
        }
    }

    private PlayerAnimClient() {
    }
}
