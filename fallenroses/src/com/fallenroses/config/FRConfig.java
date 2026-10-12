package com.fallenroses.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class FRConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final DoubleValue BASE_MAX_ENERGY;
    public static final DoubleValue REGEN_PERCENT;
    public static final DoubleValue REINFORCE_DRAIN;
    public static final DoubleValue REINFORCE_BONUS;
    public static final DoubleValue FINGER_BONUS;
    public static final IntValue MAX_FINGERS;
    public static final DoubleValue KILL_BONUS_CAP;
    public static final DoubleValue NON_CURSED_MULTIPLIER;
    public static final BooleanValue HOLLOW_PURPLE_BREAKS_BLOCKS;
    public static final BooleanValue SHOW_HUD;
    public static final BooleanValue SHOW_TEXT;
    public static final IntValue HUD_X;
    public static final IntValue HUD_Y;
    public static final BooleanValue SHOW_ABILITY_SLOTS;
    public static final DoubleValue CAMERA_SHAKE;
    public static final BooleanValue SCREEN_FLASHES;
    public static final BooleanValue MANGA_FX;
    public static final BooleanValue MANGA_TEXT;

    private FRConfig() {
    }

    static {
        Builder b = new Builder();
        b.push("cursed_energy");
        BASE_MAX_ENERGY = b.comment("Maximum cursed energy before a class is chosen").defineInRange("baseMaxEnergy", 100.0, 1.0, 10000.0);
        REGEN_PERCENT = b.comment("Cursed energy regenerated per second, in percent of the maximum (Control adds more)")
            .defineInRange("regenPercentPerSecond", 2.0, 0.0, 100.0);
        REINFORCE_DRAIN = b.comment("Energy spent per second while reinforcement / focus is active")
            .defineInRange("reinforcementDrainPerSecond", 2.0, 0.0, 1000.0);
        REINFORCE_BONUS = b.comment("Melee damage bonus while reinforced (0.35 = +35%)").defineInRange("reinforcementDamageBonus", 0.35, 0.0, 10.0);
        FINGER_BONUS = b.comment("Maximum cursed energy gained per eaten Sukuna finger (Vessel gets double)")
            .defineInRange("sukunaFingerMaxBonus", 40.0, 0.0, 1000.0);
        MAX_FINGERS = b.comment("How many Sukuna fingers one body can take").defineInRange("maxSukunaFingers", 20, 0, 20);
        KILL_BONUS_CAP = b.comment("Upper limit of maximum cursed energy gained by exorcising cursed spirits")
            .defineInRange("curseKillMaxBonusCap", 300.0, 0.0, 10000.0);
        b.pop();
        b.push("techniques");
        HOLLOW_PURPLE_BREAKS_BLOCKS = b.comment("Hollow Purple erases blocks in its path (griefing!)").define("hollowPurpleBreaksBlocks", false);
        b.pop();
        b.push("cursed_spirits");
        NON_CURSED_MULTIPLIER = b.comment(
                "Damage multiplier for cursed spirits hit without cursed energy", "(no reinforcement, no technique, no cursed tool, not Heavenly Restriction)"
            )
            .defineInRange("nonCursedDamageMultiplier", 0.3, 0.0, 1.0);
        b.pop();
        COMMON_SPEC = b.build();
        Builder c = new Builder();
        c.push("hud");
        SHOW_HUD = c.define("showCursedEnergyBar", true);
        SHOW_TEXT = c.define("showCursedEnergyText", true);
        SHOW_ABILITY_SLOTS = c.define("showAbilitySlots", true);
        HUD_X = c.comment("Left edge of the cursed energy bar, relative to the screen center").defineInRange("barXOffset", -41, -4000, 4000);
        HUD_Y = c.comment(
                "Top edge of the cursed energy bar, relative to the screen bottom", "(-84 keeps it above action bar messages, which are drawn at -68)"
            )
            .defineInRange("barYOffset", -84, -4000, 0);
        c.pop();
        c.push("effects");
        CAMERA_SHAKE = c.comment("Camera shake strength (0 = off)").defineInRange("cameraShake", 1.0, 0.0, 3.0);
        SCREEN_FLASHES = c.comment("Screen flashes from techniques").define("screenFlashes", true);
        MANGA_FX = c.comment("Manga/anime presentation: speed lines, impact frames, cut-ins, screentone, cast stamps").define("mangaEffects", true);
        MANGA_TEXT = c.comment("Manga sound-effect words in the world").define("mangaSoundWords", true);
        c.pop();
        CLIENT_SPEC = c.build();
    }
}
