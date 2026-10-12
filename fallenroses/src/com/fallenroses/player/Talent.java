package com.fallenroses.player;

import com.fallenroses.technique.Technique;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public enum Talent {
    CORE("core", Talent.Branch.CORE, 0, 0, 1, 0, 1, "魂", null, 0, Talent.Gate.ALL),
    TECH_INSIGHT("tech_insight", Talent.Branch.TECHNIQUE, 0, 1, 3, 1, 1, "覚", null, 0, Talent.Gate.ALL, "core"),
    TECH_FOCUS("tech_focus", Talent.Branch.TECHNIQUE, -1, 2, 3, 1, 3, "集", null, 0, Talent.Gate.ALL, "tech_insight"),
    TECH_FLOW("tech_flow", Talent.Branch.TECHNIQUE, 1, 2, 3, 1, 3, "流", null, 0, Talent.Gate.ALL, "tech_insight"),
    TECH_POWER("tech_power", Talent.Branch.TECHNIQUE, 0, 3, 3, 1, 5, "威", null, 0, Talent.Gate.ALL, "tech_focus", "tech_flow"),
    ABILITY_2("ability_2", Talent.Branch.TECHNIQUE, 0, 4, 1, 2, 8, "弐", null, 0, Talent.Gate.ALL, "tech_power"),
    TECH_COOLDOWN("tech_cooldown", Talent.Branch.TECHNIQUE, -1, 5, 3, 1, 10, "瞬", null, 0, Talent.Gate.ALL, "ability_2"),
    TECH_PRECISION("tech_precision", Talent.Branch.TECHNIQUE, 1, 5, 3, 1, 10, "精", null, 0, Talent.Gate.ALL, "ability_2"),
    TECH_RHYTHM("tech_rhythm", Talent.Branch.TECHNIQUE, 0, 6, 3, 1, 13, "律", null, 0, Talent.Gate.ALL, "tech_cooldown", "tech_precision"),
    TECH_DEPTH("tech_depth", Talent.Branch.TECHNIQUE, -1, 7, 2, 2, 15, "深", null, 0, Talent.Gate.ALL, "tech_rhythm"),
    TECH_RESONANCE("tech_resonance", Talent.Branch.TECHNIQUE, 1, 7, 2, 2, 15, "響", null, 0, Talent.Gate.ALL, "tech_rhythm"),
    ABILITY_3("ability_3", Talent.Branch.TECHNIQUE, 0, 8, 1, 3, 18, "参", null, 0, Talent.Gate.ALL, "tech_depth", "tech_resonance"),
    TECH_CHAIN("tech_chain", Talent.Branch.TECHNIQUE, -1, 9, 2, 1, 20, "連", null, 0, Talent.Gate.ALL, "ability_3"),
    TECH_SURGE("tech_surge", Talent.Branch.TECHNIQUE, 1, 9, 3, 1, 20, "迸", null, 0, Talent.Gate.ALL, "ability_3"),
    TECH_INSTINCT("tech_instinct", Talent.Branch.TECHNIQUE, 0, 10, 3, 1, 23, "本", null, 0, Talent.Gate.ALL, "tech_chain", "tech_surge"),
    TECH_EXPANSION("tech_expansion", Talent.Branch.TECHNIQUE, -1, 11, 3, 2, 25, "拡", null, 0, Talent.Gate.ALL, "tech_instinct"),
    TECH_ENDURANCE("tech_endurance", Talent.Branch.TECHNIQUE, 1, 11, 3, 2, 25, "耐", null, 0, Talent.Gate.ALL, "tech_instinct"),
    ABILITY_4("ability_4", Talent.Branch.TECHNIQUE, 0, 12, 1, 4, 28, "四", null, 0, Talent.Gate.ALL, "tech_expansion", "tech_endurance"),
    INNER_WORLD("inner_world", Talent.Branch.TECHNIQUE, -1, 13, 3, 2, 30, "界", null, 0, Talent.Gate.ALL, "ability_4"),
    THRESHOLD("threshold", Talent.Branch.TECHNIQUE, 1, 13, 3, 2, 30, "閾", null, 0, Talent.Gate.ALL, "ability_4"),
    TRANSCEND("transcend", Talent.Branch.TECHNIQUE, 0, 14, 2, 2, 33, "越", null, 0, Talent.Gate.ALL, "inner_world", "threshold"),
    ULT_HEART("ult_heart", Talent.Branch.TECHNIQUE, -1, 15, 2, 3, 36, "心", null, 0, Talent.Gate.ALL, "transcend"),
    ULT_SEAL("ult_seal", Talent.Branch.TECHNIQUE, 1, 15, 2, 3, 36, "印", null, 0, Talent.Gate.ALL, "transcend"),
    ULTIMATE("ultimate", Talent.Branch.TECHNIQUE, 0, 16, 1, 5, 40, "極", null, 0, Talent.Gate.ALL, "ult_heart", "ult_seal"),
    ULT_MASTERY("ult_mastery", Talent.Branch.TECHNIQUE, -1, 17, 1, 2, 42, "域", null, 0, Talent.Gate.ALL, "ultimate"),
    ULT_ECHO("ult_echo", Talent.Branch.TECHNIQUE, 1, 17, 2, 2, 42, "還", null, 0, Talent.Gate.ALL, "ultimate"),
    TECH_PEAK("tech_peak", Talent.Branch.TECHNIQUE, 0, 18, 1, 4, 48, "神", null, 0, Talent.Gate.ALL, "ult_mastery", "ult_echo"),
    STRENGTH("strength", Talent.Branch.BODY, -3, 1, 5, 1, 1, "力", Stat.STRENGTH, 4, Talent.Gate.ALL, "core"),
    VITALITY("vitality", Talent.Branch.BODY, -4, 2, 5, 1, 2, "体", Stat.VITALITY, 4, Talent.Gate.ALL, "strength"),
    AGILITY("agility", Talent.Branch.BODY, -3, 3, 5, 1, 3, "速", Stat.AGILITY, 4, Talent.Gate.ALL, "strength"),
    WEAPON_MASTERY("weapon_mastery", Talent.Branch.BODY, -6, 2, 1, 2, 4, "剣", null, 0, Talent.Gate.HEAVENLY, "vitality"),
    DIVERGENT_FIST("divergent_fist", Talent.Branch.BODY, -6, 2, 1, 2, 4, "拳", null, 0, Talent.Gate.VESSEL, "vitality"),
    REINFORCE_MASTERY("reinforce_mastery", Talent.Branch.BODY, -6, 2, 1, 2, 4, "纏", null, 0, Talent.Gate.SORCERER, "vitality"),
    SPIRIT_BODY("spirit_body", Talent.Branch.BODY, -6, 2, 1, 2, 4, "霊", null, 0, Talent.Gate.CURSED_SPIRIT, "vitality"),
    CURSE_BOND("curse_bond", Talent.Branch.BODY, -6, 2, 1, 2, 4, "縁", null, 0, Talent.Gate.MANIPULATOR, "vitality"),
    TOUGHNESS("toughness", Talent.Branch.BODY, -5, 3, 3, 1, 4, "堅", null, 0, Talent.Gate.ALL, "vitality"),
    THICK_SKIN("thick_skin", Talent.Branch.BODY, -4, 4, 3, 1, 6, "皮", null, 0, Talent.Gate.ALL, "toughness", "agility"),
    IRON_BODY(
        "iron_body",
        Talent.Branch.BODY,
        -6,
        4,
        1,
        2,
        10,
        "鉄",
        null,
        0,
        Talent.Gate.ALL,
        "weapon_mastery",
        "divergent_fist",
        "reinforce_mastery",
        "spirit_body",
        "curse_bond",
        "toughness"
    ),
    MOMENTUM("momentum", Talent.Branch.BODY, -3, 5, 1, 2, 8, "勢", null, 0, Talent.Gate.ALL, "agility", "thick_skin"),
    HEAVY_BLOWS("heavy_blows", Talent.Branch.BODY, -5, 5, 3, 1, 8, "重", null, 0, Talent.Gate.ALL, "thick_skin", "iron_body"),
    BLOODLUST("bloodlust", Talent.Branch.BODY, -7, 5, 3, 1, 12, "渇", null, 0, Talent.Gate.ALL, "iron_body"),
    MIGHT("might", Talent.Branch.BODY, -4, 6, 5, 1, 14, "剛", Stat.STRENGTH, 4, Talent.Gate.ALL, "momentum", "heavy_blows"),
    FORTITUDE("fortitude", Talent.Branch.BODY, -6, 6, 5, 1, 14, "壮", Stat.VITALITY, 4, Talent.Gate.ALL, "heavy_blows", "bloodlust"),
    SWIFTNESS("swiftness", Talent.Branch.BODY, -3, 7, 5, 1, 16, "疾", Stat.AGILITY, 4, Talent.Gate.ALL, "might"),
    LAST_STAND("last_stand", Talent.Branch.BODY, -5, 7, 1, 2, 18, "抗", null, 0, Talent.Gate.ALL, "might", "fortitude"),
    SECOND_WIND("second_wind", Talent.Branch.BODY, -7, 7, 1, 2, 20, "息", null, 0, Talent.Gate.ALL, "fortitude"),
    UNBREAKABLE("unbreakable", Talent.Branch.BODY, -4, 8, 3, 2, 24, "不", null, 0, Talent.Gate.ALL, "swiftness", "last_stand"),
    BATTLE_TRANCE("battle_trance", Talent.Branch.BODY, -6, 8, 3, 2, 24, "闘", null, 0, Talent.Gate.ALL, "last_stand", "second_wind"),
    BODY_PEAK("body_peak", Talent.Branch.BODY, -5, 9, 1, 3, 30, "鬼", null, 0, Talent.Gate.ALL, "unbreakable", "battle_trance"),
    TITAN("titan", Talent.Branch.BODY, -5, 10, 1, 4, 40, "巨", null, 0, Talent.Gate.ALL, "body_peak"),
    ENERGY("energy", Talent.Branch.ENERGY, 3, 1, 5, 1, 1, "気", Stat.ENERGY, 4, Talent.Gate.ALL, "core"),
    CONTROL("control", Talent.Branch.ENERGY, 4, 2, 5, 1, 2, "制", Stat.CONTROL, 4, Talent.Gate.ALL, "energy"),
    FLOW("flow", Talent.Branch.ENERGY, 3, 3, 3, 1, 3, "湧", null, 0, Talent.Gate.ALL, "energy"),
    EFFICIENCY("efficiency", Talent.Branch.ENERGY, 5, 3, 1, 2, 6, "節", null, 0, Talent.Gate.ALL, "control"),
    REGEN_SURGE("regen_surge", Talent.Branch.ENERGY, 4, 4, 3, 1, 6, "潮", null, 0, Talent.Gate.ALL, "flow", "efficiency"),
    VEIL("veil", Talent.Branch.ENERGY, 6, 4, 1, 2, 10, "帳", null, 0, Talent.Gate.ALL, "efficiency"),
    BLACK_FLASH("black_flash", Talent.Branch.ENERGY, 3, 5, 1, 2, 8, "黒", null, 0, Talent.Gate.ENERGY_USERS, "flow", "regen_surge"),
    SENSES("senses", Talent.Branch.ENERGY, 3, 5, 1, 2, 8, "感", null, 0, Talent.Gate.HEAVENLY, "flow", "regen_surge"),
    WELLSPRING("wellspring", Talent.Branch.ENERGY, 5, 5, 5, 1, 14, "泉", Stat.ENERGY, 4, Talent.Gate.ALL, "regen_surge", "veil"),
    FINE_CONTROL("fine_control", Talent.Branch.ENERGY, 4, 6, 5, 1, 14, "細", Stat.CONTROL, 4, Talent.Gate.ALL, "black_flash", "senses", "wellspring"),
    REVERSE_TECHNIQUE("reverse_technique", Talent.Branch.ENERGY, 6, 6, 1, 2, 15, "反", null, 0, Talent.Gate.ENERGY_USERS, "veil", "wellspring"),
    RECOVERY("recovery", Talent.Branch.ENERGY, 6, 6, 1, 2, 15, "癒", null, 0, Talent.Gate.HEAVENLY, "veil", "wellspring"),
    SPARK("spark", Talent.Branch.ENERGY, 3, 7, 3, 1, 18, "閃", null, 0, Talent.Gate.ENERGY_USERS, "black_flash"),
    CE_BARRIER("ce_barrier", Talent.Branch.ENERGY, 5, 7, 3, 2, 20, "障", null, 0, Talent.Gate.ALL, "fine_control", "reverse_technique", "recovery"),
    RCT_MASTERY("rct_mastery", Talent.Branch.ENERGY, 7, 7, 2, 2, 22, "再", null, 0, Talent.Gate.ENERGY_USERS, "reverse_technique"),
    DEEP_RESERVE("deep_reserve", Talent.Branch.ENERGY, 4, 8, 3, 1, 24, "蓄", null, 0, Talent.Gate.ALL, "spark", "ce_barrier", "senses"),
    SIMPLE_DOMAIN("simple_domain", Talent.Branch.ENERGY, 6, 8, 1, 2, 26, "簡", null, 0, Talent.Gate.ALL, "ce_barrier", "rct_mastery", "recovery"),
    ENERGY_PEAK("energy_peak", Talent.Branch.ENERGY, 5, 9, 1, 3, 30, "淵", null, 0, Talent.Gate.ALL, "deep_reserve", "simple_domain"),
    BOUNDLESS_SPRING("boundless_spring", Talent.Branch.ENERGY, 5, 10, 1, 4, 40, "源", null, 0, Talent.Gate.ALL, "energy_peak");

    public static final int LAYOUT_VERSION = 2;
    private final String id;
    public final Talent.Branch branch;
    public final int gx;
    public final int gy;
    public final int maxRank;
    public final int cost;
    public final int requiredLevel;
    public final String glyph;
    @Nullable
    public final Stat stat;
    public final int statPerRank;
    private final Talent.Gate gate;
    private final String[] parentIds;
    private Talent[] parents;

    private Talent(
        String id,
        Talent.Branch branch,
        int gx,
        int gy,
        int maxRank,
        int cost,
        int requiredLevel,
        String glyph,
        @Nullable Stat stat,
        int statPerRank,
        Talent.Gate gate,
        String... parents
    ) {
        this.id = id;
        this.branch = branch;
        this.gx = gx;
        this.gy = gy;
        this.maxRank = maxRank;
        this.cost = cost;
        this.requiredLevel = requiredLevel;
        this.glyph = glyph;
        this.stat = stat;
        this.statPerRank = statPerRank;
        this.gate = gate;
        this.parentIds = parents;
    }

    public String id() {
        return this.id;
    }

    public Talent[] parents() {
        if (this.parents == null) {
            Talent[] p = new Talent[this.parentIds.length];

            for (int i = 0; i < p.length; i++) {
                p[i] = byId(this.parentIds[i]);
            }

            this.parents = p;
        }

        return this.parents;
    }

    public int slot() {
        return switch (this) {
            case ABILITY_2 -> 1;
            case ABILITY_3 -> 2;
            case ABILITY_4 -> 3;
            case ULTIMATE -> 4;
            default -> -1;
        };
    }

    @Nullable
    public static Talent forSlot(int slot) {
        return switch (slot) {
            case 1 -> ABILITY_2;
            case 2 -> ABILITY_3;
            case 3 -> ABILITY_4;
            case 4 -> ULTIMATE;
            default -> null;
        };
    }

    public boolean strict() {
        return this.slot() > 0;
    }

    public boolean availableFor(@Nullable PlayerClass c) {
        return c != null && this.gate.allows(c);
    }

    public boolean isCapstone() {
        return this.cost >= 3;
    }

    @Nullable
    public static Talent byId(String id) {
        for (Talent t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }

        return null;
    }

    public static Set<Talent> visibleFor(@Nullable PlayerClass c) {
        Set<Talent> set = EnumSet.noneOf(Talent.class);

        for (Talent t : values()) {
            if (t.availableFor(c)) {
                set.add(t);
            }
        }

        return set;
    }

    public int fingersNeeded(@Nullable Technique technique) {
        int s = this.slot();
        return s >= 0 && technique != null ? technique.ability(s).fingers() : 0;
    }

    @Nullable
    public String blockReason(PlayerClass c, int level, int points, int[] ranks, @Nullable Technique technique, int fingers) {
        int rank = ranks[this.ordinal()];
        if (!this.availableFor(c)) {
            return "class";
        } else if (rank >= this.maxRank) {
            return "maxed";
        } else if (level < this.requiredLevel) {
            return "level";
        } else if (!this.parentsMet(ranks, c)) {
            return this.strict() ? "parent_full" : "parent";
        } else if (fingers < this.fingersNeeded(technique)) {
            return "fingers";
        } else {
            return points < this.cost ? "points" : null;
        }
    }

    public boolean parentsMet(int[] ranks, @Nullable PlayerClass c) {
        if (this.parentIds.length == 0) {
            return true;
        } else if (!this.strict()) {
            for (Talent p : this.parents()) {
                if (p != null && ranks[p.ordinal()] > 0) {
                    return true;
                }
            }

            return false;
        } else {
            for (Talent px : this.parents()) {
                if (px != null && (c == null || px.availableFor(c)) && ranks[px.ordinal()] < px.maxRank) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean parentsMet(int[] ranks) {
        return this.parentsMet(ranks, null);
    }

    public static int maxGy() {
        int m = 0;

        for (Talent t : values()) {
            m = Math.max(m, t.gy);
        }

        return m;
    }

    public static int maxAbsGx() {
        int m = 0;

        for (Talent t : values()) {
            m = Math.max(m, Math.abs(t.gx));
        }

        return m;
    }

    public MutableComponent displayName() {
        return Component.translatable("talent.fallenroses." + this.id);
    }

    public MutableComponent description() {
        return Component.translatable("talent.fallenroses." + this.id + ".desc");
    }

    public static enum Branch {
        CORE(15261936),
        BODY(14696524),
        TECHNIQUE(10510591),
        ENERGY(4885759);

        public final int color;

        private Branch(int color) {
            this.color = color;
        }
    }

    public static enum Gate {
        ALL,
        SORCERER,
        VESSEL,
        HEAVENLY,
        CURSED_SPIRIT,
        MANIPULATOR,
        ENERGY_USERS;

        boolean allows(PlayerClass c) {
            return switch (this) {
                case ALL -> true;
                case SORCERER -> c == PlayerClass.SORCERER || c == PlayerClass.JUDGE;
                case VESSEL -> c == PlayerClass.VESSEL;
                case HEAVENLY -> c == PlayerClass.HEAVENLY_RESTRICTION;
                case CURSED_SPIRIT -> c == PlayerClass.CURSED_SPIRIT;
                case MANIPULATOR -> c == PlayerClass.CURSE_MANIPULATOR;
                case ENERGY_USERS -> !c.usesStamina();
            };
        }
    }
}
