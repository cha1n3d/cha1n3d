"""Base values for the computer market (rubles per item).

SELL: what the computer pays for raw/vanilla goods.  MAT: material cost of intermediates
that the computer does not buy, used only to price crafted mod items from their recipes.
"""
SELL = {
 # ores and metals
 "coal": 3, "charcoal": 2, "copper_ingot": 3, "raw_copper": 2, "iron_ingot": 10, "raw_iron": 8, "iron_nugget": 1,
 "gold_ingot": 25, "raw_gold": 20, "gold_nugget": 2, "redstone": 2, "lapis_lazuli": 4, "quartz": 3, "amethyst_shard": 3,
 "emerald": 40, "diamond": 150, "netherite_scrap": 350, "ancient_debris": 300, "netherite_ingot": 1500,
 "iron_block": 90, "gold_block": 225, "diamond_block": 1350, "emerald_block": 360, "redstone_block": 18,
 "lapis_block": 36, "coal_block": 27, "copper_block": 27, "raw_iron_block": 72, "raw_gold_block": 180, "raw_copper_block": 18,
 "obsidian": 2, "glowstone_dust": 2,
 # wood
 "oak_log": 1, "spruce_log": 1, "birch_log": 1, "jungle_log": 1, "acacia_log": 1, "dark_oak_log": 1, "mangrove_log": 1, "cherry_log": 1,
 # farming
 "wheat": 1, "carrot": 1, "potato": 1, "beetroot": 1, "sugar_cane": 1, "pumpkin": 2, "melon": 2, "cocoa_beans": 1,
 "nether_wart": 2, "egg": 1, "apple": 2, "honey_bottle": 5, "honeycomb": 3,
 # food
 "bread": 3, "beef": 2, "porkchop": 2, "chicken": 2, "mutton": 2, "salmon": 2, "cod": 1,
 "cooked_beef": 4, "cooked_porkchop": 4, "cooked_chicken": 3, "cooked_mutton": 3, "cooked_salmon": 3, "cooked_cod": 2,
 "baked_potato": 2, "pumpkin_pie": 4, "cake": 10, "golden_carrot": 12, "golden_apple": 120, "enchanted_golden_apple": 1500,
 # mob drops
 "leather": 4, "string": 1, "bone": 1, "gunpowder": 4, "spider_eye": 2, "ender_pearl": 12, "blaze_rod": 15, "blaze_powder": 7,
 "ghast_tear": 35, "slime_ball": 4, "magma_cream": 6, "feather": 1, "ink_sac": 1, "glow_ink_sac": 3, "prismarine_shard": 2,
 "prismarine_crystals": 3, "phantom_membrane": 10, "rabbit_hide": 1, "rabbit_foot": 15, "shulker_shell": 120, "scute": 20,
 "nautilus_shell": 30, "heart_of_the_sea": 600, "echo_shard": 40, "ender_eye": 18, "tnt": 20, "firework_rocket": 1,
 # rarities
 "experience_bottle": 6, "name_tag": 40, "saddle": 40, "trident": 600, "elytra": 2500, "totem_of_undying": 800,
 "nether_star": 2500, "dragon_egg": 5000, "dragon_head": 800, "wither_skeleton_skull": 150,
}
# Mod items that cannot be crafted: boss trophies and drops.
SELL_MOD = {
 "sukuna_finger": 300, "death_painting": 1500, "frostmourne": 2500, "haruta_sword": 1000, "timokha_chain": 1000,
 "ivan_watch": 1000, "music_disc_morgen_1": 150, "music_disc_morgen_2": 150, "music_disc_morgen_3": 150,
 "banknote_100": 100, "banknote_1000": 1000,
}
MAT = {
 "planks": 0.25, "wooden_slabs": 0.125, "stick": 0.125, "lever": 0.2, "furnace": 0.0, "crafting_table": 1.0,
 "glass_pane": 0.4, "glass_bottle": 0.4, "paper": 0.35, "sugar": 1.0, "chain": 12.0, "iron_bars": 3.75, "piston": 12.75,
 "note_block": 4.0, "spyglass": 9.0, "compass": 42.0, "map": 45.0, "blue_ice": 3.0, "black_wool": 1.0, "white_wool": 1.0,
 "polished_blackstone": 0.0, "smooth_stone_slab": 0.0, "stone_button": 0.0, "bone_block": 3.0, "fire_charge": 3.7,
 "soul_soil": 0.0, "coals": 2.5, "dirt": 0.0, "snowball": 0.0, "oak_door": 0.5, "iron_pickaxe": 30.0, "shield": 11.0,
 "tropical_fish": 1.0, "cactus": 0.5, "glow_berries": 0.5, "sunflower": 0.5, "clock": 102.0, "logs": 1.0, "dried_kelp": 0.2, "red_wool": 1.0, "rotten_flesh": 0.0, "flint": 0.5, "sweet_berries": 0.5, "dyes": 0.5,
}
