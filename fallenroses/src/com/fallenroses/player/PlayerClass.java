package com.fallenroses.player;

import com.fallenroses.technique.Technique;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public enum PlayerClass {
    SORCERER("sorcerer", 10255615, "呪", false, 120.0F, new int[]{1, 1, 1, 3, 3}, 0, 0, 0.0F),
    HEAVENLY_RESTRICTION("heavenly_restriction", 15259808, "天", true, 100.0F, new int[]{4, 3, 4, 1, 1}, 8, 2, 0.15F),
    VESSEL("vessel", 14692412, "宿", false, 100.0F, new int[]{3, 3, 2, 2, 1}, 4, 1, 0.05F),
    CURSED_SPIRIT("cursed_spirit", 9059024, "霊", false, 140.0F, new int[]{2, 3, 2, 3, 2}, 6, 1, 0.05F),
    CURSE_MANIPULATOR("curse_manipulator", 3844744, "操", false, 120.0F, new int[]{1, 2, 1, 3, 3}, 2, 0, 0.0F),
    JUDGE("judge", 15259808, "法", false, 130.0F, new int[]{2, 2, 1, 3, 3}, 4, 1, 0.0F);

    private final String id;
    private final int color;
    private final String glyph;
    private final boolean stamina;
    private final float baseEnergy;
    private final int[] startStats;
    private final int bonusHealth;
    private final int bonusDamage;
    private final float bonusSpeed;

    private PlayerClass(
        String id, int color, String glyph, boolean stamina, float baseEnergy, int[] startStats, int bonusHealth, int bonusDamage, float bonusSpeed
    ) {
        this.id = id;
        this.color = color;
        this.glyph = glyph;
        this.stamina = stamina;
        this.baseEnergy = baseEnergy;
        this.startStats = startStats;
        this.bonusHealth = bonusHealth;
        this.bonusDamage = bonusDamage;
        this.bonusSpeed = bonusSpeed;
    }

    public String id() {
        return this.id;
    }

    public int color() {
        return this.color;
    }

    public String glyph() {
        return this.glyph;
    }

    public boolean usesStamina() {
        return this.stamina;
    }

    public float baseEnergy() {
        return this.baseEnergy;
    }

    public int startStat(Stat stat) {
        return this.startStats[stat.ordinal()];
    }

    public int bonusHealth() {
        return this.bonusHealth;
    }

    public int bonusDamage() {
        return this.bonusDamage;
    }

    public float bonusSpeed() {
        return this.bonusSpeed;
    }

    @Nullable
    public Technique fixedTechnique() {
        return switch (this) {
            case HEAVENLY_RESTRICTION -> Technique.HEAVENLY_BODY;
            case VESSEL -> Technique.VESSEL;
            case JUDGE -> Technique.DEADLY_SENTENCING;
            case CURSE_MANIPULATOR -> Technique.CURSE_MANIPULATION;
            case SORCERER, CURSED_SPIRIT -> null;
        };
    }

    @Nullable
    public Technique.Source randomPool() {
        return switch (this) {
            case SORCERER -> Technique.Source.SORCERER;
            case CURSED_SPIRIT -> Technique.Source.SPIRIT;
            default -> null;
        };
    }

    @Nullable
    public Technique rollTechnique(RandomSource random) {
        Technique fixed = this.fixedTechnique();
        if (fixed != null) {
            return fixed;
        } else {
            Technique.Source pool = this.randomPool();
            return pool == null ? null : Technique.random(pool, random);
        }
    }

    public MutableComponent displayName() {
        return Component.translatable("class.fallenroses." + this.id).withStyle(s -> s.withColor(this.color));
    }

    @Nullable
    public static PlayerClass byId(String id) {
        for (PlayerClass c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }

        return null;
    }

    @Nullable
    public static PlayerClass byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }
}
