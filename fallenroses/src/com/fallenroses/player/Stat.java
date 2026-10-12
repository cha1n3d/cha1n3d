package com.fallenroses.player;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Stat {
    STRENGTH("strength"),
    VITALITY("vitality"),
    AGILITY("agility"),
    ENERGY("energy"),
    CONTROL("control");

    public static final int MAX = 50;
    private final String id;

    private Stat(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public MutableComponent displayName(boolean stamina) {
        return Component.translatable("stat.fallenroses." + this.id + (!stamina || this != ENERGY && this != CONTROL ? "" : ".stamina"));
    }

    public MutableComponent description(boolean stamina) {
        return Component.translatable("stat.fallenroses." + this.id + (!stamina || this != ENERGY && this != CONTROL ? "" : ".stamina") + ".desc");
    }
}
