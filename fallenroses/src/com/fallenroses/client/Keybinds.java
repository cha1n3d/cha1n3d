package com.fallenroses.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public final class Keybinds {
    public static final String CATEGORY = "key.categories.fallenroses";
    public static final KeyMapping ABILITY_1 = key("ability_1", KeyModifier.NONE, 88);
    public static final KeyMapping ABILITY_2 = key("ability_2", KeyModifier.NONE, 67);
    public static final KeyMapping ABILITY_3 = key("ability_3", KeyModifier.SHIFT, 88);
    public static final KeyMapping ABILITY_4 = key("ability_4", KeyModifier.SHIFT, 67);
    public static final KeyMapping ULTIMATE = key("ultimate", KeyModifier.SHIFT, 86);
    public static final KeyMapping REINFORCE = key("reinforce", KeyModifier.NONE, 86);
    public static final KeyMapping CHARACTER = key("character", KeyModifier.NONE, 75);
    public static final KeyMapping[] SLOTS = new KeyMapping[]{ABILITY_1, ABILITY_2, ABILITY_3, ABILITY_4, ULTIMATE};

    private static KeyMapping key(String id, KeyModifier modifier, int code) {
        return new KeyMapping("key.fallenroses." + id, KeyConflictContext.IN_GAME, modifier, Type.KEYSYM, code, "key.categories.fallenroses");
    }

    private Keybinds() {
    }
}
