package com.fallenroses.technique;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record AbilityDef(String id, float cost, int cooldown, boolean toggle, String glyph, int fingers, int paintings) {
    public static AbilityDef cast(String id, float cost, int cooldown, String glyph) {
        return new AbilityDef(id, cost, cooldown, false, glyph, 0, 0);
    }

    public static AbilityDef toggle(String id, float cost, int cooldown, String glyph) {
        return new AbilityDef(id, cost, cooldown, true, glyph, 0, 0);
    }

    public static AbilityDef finger(String id, float cost, int cooldown, String glyph, int fingers) {
        return new AbilityDef(id, cost, cooldown, false, glyph, fingers, 0);
    }

    public static AbilityDef painting(String id, float cost, int cooldown, String glyph, int paintings) {
        return new AbilityDef(id, cost, cooldown, false, glyph, 0, paintings);
    }

    public MutableComponent displayName() {
        return Component.translatable("ability.fallenroses." + this.id);
    }

    public MutableComponent description() {
        return Component.translatable("ability.fallenroses." + this.id + ".desc");
    }
}
