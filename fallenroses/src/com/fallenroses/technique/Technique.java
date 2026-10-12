package com.fallenroses.technique;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public enum Technique {
    LIMITLESS(
        "limitless",
        5941503,
        Technique.Source.SORCERER,
        Technique.DomainType.VOID,
        AbilityDef.cast("blue", 30.0F, 140, "蒼"),
        AbilityDef.cast("red", 45.0F, 180, "赫"),
        AbilityDef.cast("hollow_purple", 120.0F, 900, "茈"),
        AbilityDef.toggle("infinity", 5.0F, 40, "限"),
        AbilityDef.cast("unlimited_void", 250.0F, 3600, "無")
    ),
    SHRINE(
        "shrine",
        14692412,
        Technique.Source.CLASS,
        Technique.DomainType.SHRINE,
        AbilityDef.finger("dismantle", 18.0F, 30, "解", 1),
        AbilityDef.finger("cleave", 40.0F, 140, "捌", 3),
        AbilityDef.finger("spiderweb", 55.0F, 300, "網", 6),
        AbilityDef.finger("fuga", 100.0F, 900, "開", 10),
        AbilityDef.finger("malevolent_shrine", 250.0F, 3600, "廚", 15)
    ),
    CURSED_SPEECH(
        "cursed_speech",
        11894015,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("dont_move", 25.0F, 200, "止"),
        AbilityDef.cast("blast_away", 35.0F, 180, "飛"),
        AbilityDef.cast("twist", 50.0F, 300, "捻"),
        AbilityDef.cast("explode", 80.0F, 600, "爆"),
        AbilityDef.cast("vanish", 200.0F, 3000, "消")
    ),
    BOOGIE_WOOGIE(
        "boogie_woogie",
        16757575,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("swap", 10.0F, 25, "替"),
        AbilityDef.cast("feint", 15.0F, 120, "拍"),
        AbilityDef.cast("clap_barrage", 45.0F, 300, "連"),
        AbilityDef.cast("brother_strike", 40.0F, 400, "兄"),
        AbilityDef.cast("chaos_clap", 180.0F, 2400, "乱")
    ),
    HEAVENLY_BODY(
        "heavenly_body",
        15259808,
        Technique.Source.CLASS,
        Technique.DomainType.NONE,
        AbilityDef.cast("dash", 15.0F, 40, "迅"),
        AbilityDef.cast("ground_slam", 35.0F, 200, "砕"),
        AbilityDef.cast("flurry", 30.0F, 160, "撃"),
        AbilityDef.cast("killer_instinct", 40.0F, 600, "狩"),
        AbilityDef.cast("berserk", 100.0F, 3000, "鬼")
    ),
    BLOOD_MANIPULATION(
        "blood_manipulation",
        13635632,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("piercing_blood", 30.0F, 100, "穿"),
        AbilityDef.cast("slicing_exorcism", 25.0F, 80, "斬"),
        AbilityDef.cast("flowing_red_scale", 35.0F, 500, "赤"),
        AbilityDef.cast("supernova", 60.0F, 300, "星"),
        AbilityDef.cast("blood_rain", 180.0F, 2400, "血")
    ),
    RATIO(
        "ratio",
        15253600,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("ratio_strike", 20.0F, 60, "七"),
        AbilityDef.cast("blunt_combo", 30.0F, 160, "鈍"),
        AbilityDef.cast("collapse", 45.0F, 260, "瓦"),
        AbilityDef.cast("overtime", 40.0F, 1200, "残"),
        AbilityDef.cast("fatal_ratio", 160.0F, 2400, "極")
    ),
    STRAW_DOLL(
        "straw_doll",
        14708784,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("nails", 12.0F, 25, "釘"),
        AbilityDef.cast("resonance", 35.0F, 160, "共"),
        AbilityDef.cast("hairpin", 30.0F, 140, "簪"),
        AbilityDef.cast("nail_storm", 50.0F, 300, "嵐"),
        AbilityDef.cast("black_resonance", 150.0F, 2400, "鳴")
    ),
    DISASTER_FLAMES(
        "disaster_flames",
        16738832,
        Technique.Source.SPIRIT,
        Technique.DomainType.FLAMES,
        AbilityDef.cast("ember_insects", 20.0F, 60, "虫"),
        AbilityDef.cast("flame_breath", 30.0F, 120, "炎"),
        AbilityDef.cast("fire_pillar", 40.0F, 180, "柱"),
        AbilityDef.cast("maximum_meteor", 100.0F, 900, "隕"),
        AbilityDef.cast("coffin_of_iron_mountain", 250.0F, 3600, "棺")
    ),
    IDLE_TRANSFIGURATION(
        "idle_transfiguration",
        8044728,
        Technique.Source.SPIRIT,
        Technique.DomainType.MAHITO,
        AbilityDef.cast("soul_touch", 25.0F, 80, "触"),
        AbilityDef.cast("body_repel", 30.0F, 140, "弾"),
        AbilityDef.cast("spike_burst", 40.0F, 200, "棘"),
        AbilityDef.cast("soul_multiplicity", 45.0F, 600, "多"),
        AbilityDef.cast("self_embodiment", 250.0F, 3600, "掌")
    ),
    DISASTER_PLANTS(
        "disaster_plants",
        5943360,
        Technique.Source.SPIRIT,
        Technique.DomainType.NONE,
        AbilityDef.cast("cursed_buds", 20.0F, 80, "芽"),
        AbilityDef.cast("root_eruption", 40.0F, 160, "根"),
        AbilityDef.cast("flower_field", 35.0F, 500, "花"),
        AbilityDef.cast("wooden_balls", 60.0F, 300, "球"),
        AbilityDef.cast("sea_of_light", 200.0F, 3000, "光")
    ),
    CURSE_MANIPULATION(
        "curse_manipulation",
        3844744,
        Technique.Source.CLASS,
        Technique.DomainType.NONE,
        AbilityDef.cast("absorb", 10.0F, 40, "吸"),
        AbilityDef.cast("release_spirits", 25.0F, 200, "放"),
        AbilityDef.cast("curse_swarm", 35.0F, 160, "群"),
        AbilityDef.cast("summon_grade_two", 45.0F, 600, "召"),
        AbilityDef.cast("uzumaki", 200.0F, 3000, "渦")
    ),
    TEN_SHADOWS(
        "ten_shadows",
        6978248,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("divine_dogs", 30.0F, 400, "犬"),
        AbilityDef.cast("toad", 20.0F, 120, "蛙"),
        AbilityDef.cast("nue", 35.0F, 500, "鵺"),
        AbilityDef.cast("max_elephant", 50.0F, 300, "象"),
        AbilityDef.cast("mahoraga", 220.0F, 3600, "魔")
    ),
    PROJECTION(
        "projection",
        8050920,
        Technique.Source.SORCERER,
        Technique.DomainType.MOON_PALACE,
        AbilityDef.cast("frame_dash", 15.0F, 40, "幀"),
        AbilityDef.cast("freeze_frame", 25.0F, 160, "凍"),
        AbilityDef.cast("frame_barrage", 40.0F, 240, "連"),
        AbilityDef.cast("acceleration", 35.0F, 600, "加"),
        AbilityDef.cast("time_cell_moon_palace", 230.0F, 3600, "宮")
    ),
    ICE_FORMATION(
        "ice_formation",
        11067647,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("ice_shards", 15.0F, 30, "氷"),
        AbilityDef.cast("frost_calm", 25.0F, 100, "霜"),
        AbilityDef.cast("ice_fall", 40.0F, 200, "瀑"),
        AbilityDef.cast("frozen_coffin", 45.0F, 300, "柩"),
        AbilityDef.cast("glacial_slope", 180.0F, 2400, "凍")
    ),
    DISASTER_TIDES(
        "disaster_tides",
        2788048,
        Technique.Source.SPIRIT,
        Technique.DomainType.TIDES,
        AbilityDef.cast("fish_swarm", 20.0F, 60, "魚"),
        AbilityDef.cast("tidal_wave", 35.0F, 160, "波"),
        AbilityDef.cast("whirlpool", 40.0F, 240, "渦"),
        AbilityDef.cast("death_swarm", 60.0F, 400, "鮫"),
        AbilityDef.cast("horizon_of_skandha", 250.0F, 3600, "海")
    ),
    SMALLPOX(
        "smallpox",
        10133600,
        Technique.Source.SPIRIT,
        Technique.DomainType.CEMETERY,
        AbilityDef.cast("pox_cloud", 25.0F, 140, "疱"),
        AbilityDef.cast("grave_marker", 30.0F, 120, "墓"),
        AbilityDef.cast("coffin_trap", 45.0F, 300, "棺"),
        AbilityDef.cast("plague_bloom", 50.0F, 400, "疫"),
        AbilityDef.cast("cemetery", 230.0F, 3600, "葬")
    ),
    IDLE_DEATH_GAMBLE(
        "idle_death_gamble",
        7004282,
        Technique.Source.SORCERER,
        Technique.DomainType.GAMBLE,
        AbilityDef.cast("rough_energy", 15.0F, 30, "粗"),
        AbilityDef.cast("shutter_doors", 25.0F, 140, "扉"),
        AbilityDef.cast("pachinko_balls", 30.0F, 160, "玉"),
        AbilityDef.cast("reach", 35.0F, 600, "確"),
        AbilityDef.cast("jackpot", 220.0F, 3600, "当")
    ),
    LIGHTNING(
        "lightning",
        8048895,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("charge_strike", 15.0F, 30, "電"),
        AbilityDef.cast("static_field", 25.0F, 200, "帯"),
        AbilityDef.cast("staff_rush", 30.0F, 160, "棍"),
        AbilityDef.cast("discharge", 45.0F, 300, "放"),
        AbilityDef.cast("mythical_beast_amber", 160.0F, 3600, "琥")
    ),
    STAR_RAGE(
        "star_rage",
        16762960,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("mass_punch", 20.0F, 60, "質"),
        AbilityDef.cast("garuda", 25.0F, 120, "鳥"),
        AbilityDef.cast("gravity_well", 35.0F, 240, "重"),
        AbilityDef.cast("star_fall", 40.0F, 300, "墜"),
        AbilityDef.cast("bonbaye", 220.0F, 3600, "星")
    ),
    DEATH_PAINTING(
        "death_painting",
        10123824,
        Technique.Source.SPIRIT,
        Technique.DomainType.NONE,
        AbilityDef.cast("rot_blood", 15.0F, 40, "腐"),
        AbilityDef.cast("wing_king", 25.0F, 120, "翼"),
        AbilityDef.cast("decay_mark", 25.0F, 160, "蝕"),
        AbilityDef.cast("rot_mist", 40.0F, 400, "瘴"),
        AbilityDef.cast("decay_bloom", 160.0F, 3000, "壊")
    ),
    VESSEL(
        "vessel",
        14692412,
        Technique.Source.CLASS,
        Technique.DomainType.STATION,
        AbilityDef.finger("vessel_dismantle", 15.0F, 30, "解", 1),
        AbilityDef.finger("vessel_cleave", 30.0F, 120, "捌", 3),
        AbilityDef.painting("blood_spear", 30.0F, 120, "穿", 1),
        AbilityDef.cast("black_flash_rush", 30.0F, 300, "黒"),
        AbilityDef.finger("vessel_domain", 250.0F, 3600, "駅", 10)
    ),
    DEADLY_SENTENCING(
        "deadly_sentencing",
        15259808,
        Technique.Source.CLASS,
        Technique.DomainType.JUDGEMENT,
        AbilityDef.cast("gavel_strike", 15.0F, 30, "槌"),
        AbilityDef.cast("gavel_throw", 20.0F, 80, "投"),
        AbilityDef.cast("objection", 20.0F, 300, "異"),
        AbilityDef.cast("evidence", 25.0F, 200, "証"),
        AbilityDef.cast("deadly_sentencing_domain", 250.0F, 3600, "誅")
    ),
    GRANITE_BLAST(
        "granite_blast",
        15786160,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("granite_shot", 15.0F, 25, "石"),
        AbilityDef.cast("discharge_barrage", 30.0F, 140, "連"),
        AbilityDef.cast("output_ram", 25.0F, 120, "突"),
        AbilityDef.cast("overflow", 20.0F, 900, "溢"),
        AbilityDef.cast("granite_cannon", 200.0F, 3600, "崗")
    ),
    SWARM(
        "swarm",
        9067050,
        Technique.Source.SPIRIT,
        Technique.DomainType.NONE,
        AbilityDef.cast("roach_swarm", 15.0F, 40, "蟲"),
        AbilityDef.cast("devour", 25.0F, 140, "喰"),
        AbilityDef.cast("molt", 25.0F, 400, "脱"),
        AbilityDef.cast("brood", 40.0F, 500, "孵"),
        AbilityDef.cast("infestation", 180.0F, 3000, "災")
    ),
    CONSTRUCTION(
        "construction",
        13158616,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("construct_bullet", 12.0F, 25, "弾"),
        AbilityDef.cast("iron_spikes", 25.0F, 120, "鋲"),
        AbilityDef.cast("liquid_metal", 30.0F, 160, "液"),
        AbilityDef.cast("insect_armor", 35.0F, 600, "鎧"),
        AbilityDef.cast("truesphere", 220.0F, 3600, "真")
    ),
    SKY_MANIPULATION(
        "sky_manipulation",
        9095423,
        Technique.Source.SORCERER,
        Technique.DomainType.NONE,
        AbilityDef.cast("sky_ripple", 15.0F, 40, "波"),
        AbilityDef.cast("sky_shield", 25.0F, 300, "盾"),
        AbilityDef.cast("sky_fold", 30.0F, 160, "折"),
        AbilityDef.cast("sky_slide", 20.0F, 100, "滑"),
        AbilityDef.cast("thin_ice_breaker", 200.0F, 3600, "空")
    ),
    RIKA(
        "rika",
        15764160,
        Technique.Source.SPIRIT,
        Technique.DomainType.NONE,
        AbilityDef.cast("rika_claw", 15.0F, 30, "爪"),
        AbilityDef.cast("rika_grab", 25.0F, 140, "掴"),
        AbilityDef.cast("rika_guard", 25.0F, 400, "守"),
        AbilityDef.cast("rika_manifest", 40.0F, 900, "顕"),
        AbilityDef.cast("pure_love", 230.0F, 3600, "愛")
    );

    public static final int SLOTS = 5;
    public static final int ULTIMATE = 4;
    private final String id;
    private final int color;
    private final Technique.Source source;
    private final Technique.DomainType domain;
    private final AbilityDef[] abilities;

    private Technique(String id, int color, Technique.Source source, Technique.DomainType domain, AbilityDef... abilities) {
        this.id = id;
        this.color = color;
        this.source = source;
        this.domain = domain;
        this.abilities = abilities;
    }

    public String id() {
        return this.id;
    }

    public int color() {
        return this.color;
    }

    public Technique.Source source() {
        return this.source;
    }

    public float balance() {
        return switch (this) {
            case DISASTER_FLAMES -> 0.6F;
            case RATIO, DISASTER_PLANTS, DISASTER_TIDES -> 0.8F;
            case LIMITLESS, SHRINE, CURSED_SPEECH, IDLE_TRANSFIGURATION, BLOOD_MANIPULATION, SMALLPOX -> 0.85F;
            case STRAW_DOLL, CURSE_MANIPULATION, ICE_FORMATION, PROJECTION, IDLE_DEATH_GAMBLE, DEATH_PAINTING -> 0.9F;
            case LIGHTNING, STAR_RAGE, GRANITE_BLAST -> 0.85F;
            case VESSEL, DEADLY_SENTENCING, SWARM, CONSTRUCTION, SKY_MANIPULATION -> 0.9F;
            case RIKA -> 0.85F;
            default -> 1.0F;
        };
    }

    public boolean learnable() {
        return this.source != Technique.Source.CLASS;
    }

    public boolean hasDomain() {
        return this.domain != Technique.DomainType.NONE;
    }

    public Technique.DomainType domain() {
        return this.domain;
    }

    public AbilityDef ability(int slot) {
        return this.abilities[slot];
    }

    public MutableComponent displayName() {
        return Component.translatable("technique.fallenroses." + this.id).withStyle(s -> s.withColor(this.color));
    }

    public MutableComponent description() {
        return Component.translatable("technique.fallenroses." + this.id + ".desc");
    }

    public static List<Technique> pool(Technique.Source source) {
        List<Technique> list = new ArrayList<>();

        for (Technique t : values()) {
            if (t.source == source) {
                list.add(t);
            }
        }

        return list;
    }

    public static Technique random(Technique.Source source, RandomSource random) {
        List<Technique> pool = pool(source);
        return pool.get(random.nextInt(pool.size()));
    }

    @Nullable
    public static Technique byId(String id) {
        for (Technique t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }

        return null;
    }

    @Nullable
    public static Technique byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
    }

    public static enum DomainType {
        NONE(-1),
        VOID(0),
        SHRINE(1),
        FLAMES(2),
        MAHITO(3),
        TIDES(4),
        CEMETERY(5),
        GAMBLE(6),
        JUDGEMENT(7),
        MOON_PALACE(8),
        STATION(9);

        public final int id;

        private DomainType(int id) {
            this.id = id;
        }
    }

    public static enum Source {
        SORCERER,
        SPIRIT,
        CLASS;
    }
}
