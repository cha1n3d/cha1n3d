"""Generates computer/PriceTable.java from prices_base.py and the mod's recipes, and checks the market for
arbitrage: nothing may be bought (even at the daily discount) and sold back, or bought and handed in for an
order, for profit; crafted mod items sell for half their material cost.

usage: prices.py <recipe dir>... -o PriceTable.java [--md table.md]
"""
import json, glob, os, sys, math
from collections import Counter
sys.path.insert(0, os.path.dirname(__file__))
from prices_base import SELL, SELL_MOD, MAT

DEAL = 0.7           # daily deals: -30%
CRAFT_SELL = 0.5     # computer pays half the material cost of crafted mod items
CRAFT_BUY = 3.0      # shop sells crafted mod items for 3x materials ...
LABOR = 20           # ... plus 20 rubles of work per lot

args = sys.argv[1:]
out = args[args.index('-o') + 1]
md = args[args.index('--md') + 1] if '--md' in args else None
dirs = [a for i, a in enumerate(args) if not a.startswith('-') and (i == 0 or args[i - 1] not in ('-o', '--md'))]

def cost(n):
    if n in SELL: return SELL[n]
    if n in MAT: return MAT[n]
    if n.endswith('_dye'): return 0.5
    raise SystemExit('no material cost for ' + n)

material = {}
for d_ in dirs:
    for f in sorted(glob.glob(os.path.join(d_, '**', '*.json'), recursive=True)):
        d = json.load(open(f))
        r = d.get('result', {})
        res, cnt = r.get('item', ':').split(':')[1], r.get('count', 1)
        if d.get('type') == 'vaz2109:station':
            items = [(i['count'], (i['ingredient'].get('item') or i['ingredient'].get('tag')).split(':')[1]) for i in d['ingredients']]
        elif 'key' in d:
            c = Counter(ch for row in d['pattern'] for ch in row if ch != ' ')
            items = [(v, (d['key'][k].get('item') or d['key'][k].get('tag')).split(':')[-1]) for k, v in c.items()]
        else:
            c = Counter((i.get('item') or i.get('tag')).split(':')[-1] for i in d['ingredients'])
            items = [(v, k) for k, v in c.items()]
        if any(n == res for _, n in items):
            continue  # recolouring recipes (skateboard + dye)
        unit = sum(c * cost(n) for c, n in items) / cnt
        material[res] = min(material.get(res, 1e9), unit)

def nice(x):
    step = 5 if x < 100 else 10 if x < 1000 else 50 if x < 10000 else 100
    return max(step, int(round(x / step)) * step)

sell = {('minecraft:' + k): v for k, v in SELL.items()}
sell.update({('vaz2109:' + k): v for k, v in SELL_MOD.items()})
for k, m in material.items():
    if 'vaz2109:' + k not in sell:
        v = math.floor(m * CRAFT_SELL)
        if v >= 1:
            sell['vaz2109:' + k] = v

CATS = ['deals', 'food', 'resources', 'weapons', 'gear', 'rides', 'magic', 'bank', 'orders']
C = {n: i for i, n in enumerate(CATS)}

def crafted(id_, lot=1):
    return nice(CRAFT_BUY * material[id_] * lot + LABOR)

# (category, id, lot, price or None = from recipe)
OFFERS = [
 ('food', 'minecraft:bread', 8, 40), ('food', 'minecraft:cooked_beef', 8, 56), ('food', 'minecraft:cooked_salmon', 8, 48),
 ('food', 'minecraft:golden_carrot', 8, 160), ('food', 'minecraft:cake', 1, 40), ('food', 'minecraft:honey_bottle', 4, 40),
 ('food', 'vaz2109:beer', 4, None), ('food', 'minecraft:golden_apple', 1, 350), ('food', 'minecraft:enchanted_golden_apple', 1, 5000),

 ('resources', 'minecraft:iron_ingot', 8, 200), ('resources', 'minecraft:gold_ingot', 4, 250), ('resources', 'minecraft:copper_ingot', 16, 96),
 ('resources', 'minecraft:diamond', 1, 400), ('resources', 'minecraft:emerald', 1, 100), ('resources', 'minecraft:netherite_scrap', 1, 900),
 ('resources', 'minecraft:netherite_ingot', 1, 3800), ('resources', 'minecraft:coal', 16, 96), ('resources', 'minecraft:redstone', 16, 96),
 ('resources', 'minecraft:lapis_lazuli', 8, 80), ('resources', 'minecraft:quartz', 16, 96), ('resources', 'minecraft:amethyst_shard', 8, 48),
 ('resources', 'minecraft:glowstone_dust', 16, 64), ('resources', 'minecraft:oak_log', 32, 80), ('resources', 'minecraft:spruce_log', 32, 80),
 ('resources', 'minecraft:glass', 32, 64), ('resources', 'minecraft:sand', 32, 40), ('resources', 'minecraft:obsidian', 8, 80),
 ('resources', 'minecraft:torch', 32, 32), ('resources', 'minecraft:string', 8, 24), ('resources', 'minecraft:leather', 8, 64),
 ('resources', 'minecraft:gunpowder', 4, 40), ('resources', 'minecraft:slime_ball', 4, 40), ('resources', 'minecraft:ender_pearl', 2, 60),
 ('resources', 'minecraft:blaze_rod', 2, 60), ('resources', 'minecraft:tnt', 4, 140),

 ('weapons', 'vaz2109:pm', 1, None), ('weapons', 'vaz2109:sawed_off', 1, None), ('weapons', 'vaz2109:ak74', 1, None),
 ('weapons', 'vaz2109:svd', 1, None), ('weapons', 'vaz2109:rpg7', 1, None),
 ('weapons', 'vaz2109:ammo_9x18', 32, None), ('weapons', 'vaz2109:ammo_545', 30, None), ('weapons', 'vaz2109:ammo_12ga', 16, None),
 ('weapons', 'vaz2109:ammo_762', 10, None), ('weapons', 'vaz2109:pg7_rocket', 2, None),
 ('weapons', 'vaz2109:grenade', 4, None), ('weapons', 'vaz2109:molotov', 4, None), ('weapons', 'vaz2109:flashbang', 4, None),
 ('weapons', 'vaz2109:bat', 1, None), ('weapons', 'vaz2109:crowbar', 1, None), ('weapons', 'vaz2109:kastet', 1, None),
 ('weapons', 'vaz2109:machete', 1, None), ('weapons', 'vaz2109:chainsaw', 1, None),
 ('weapons', 'vaz2109:nichirin_water', 1, None), ('weapons', 'vaz2109:nichirin_flame', 1, None),
 ('weapons', 'minecraft:bow', 1, 40), ('weapons', 'minecraft:crossbow', 1, 50), ('weapons', 'minecraft:arrow', 32, 40),
 ('weapons', 'minecraft:shield', 1, 50), ('weapons', 'minecraft:diamond_sword', 1, 600),

 ('gear', 'vaz2109:tactical_helmet', 1, None), ('gear', 'vaz2109:tactical_chestplate', 1, None),
 ('gear', 'vaz2109:tactical_leggings', 1, None), ('gear', 'vaz2109:tactical_boots', 1, None),
 ('gear', 'vaz2109:jujutsu_helmet', 1, None), ('gear', 'vaz2109:jujutsu_chestplate', 1, None),
 ('gear', 'vaz2109:jujutsu_leggings', 1, None), ('gear', 'vaz2109:jujutsu_boots', 1, None),
 ('gear', 'vaz2109:narcoleptin', 4, None), ('gear', 'vaz2109:compound_v', 1, None), ('gear', 'vaz2109:explorer_compass', 1, None),
 ('gear', 'vaz2109:fox_mask', 1, None), ('gear', 'vaz2109:mask_richard', 1, None), ('gear', 'vaz2109:mask_rasmus', 1, None),
 ('gear', 'vaz2109:mask_tony', 1, None), ('gear', 'vaz2109:mask_aubrey', 1, None), ('gear', 'vaz2109:mask_don_juan', 1, None),
 ('gear', 'vaz2109:party_hat_red', 1, None), ('gear', 'vaz2109:party_hat_blue', 1, None), ('gear', 'vaz2109:party_hat_green', 1, None),
 ('gear', 'vaz2109:party_hat_yellow', 1, None), ('gear', 'vaz2109:party_hat_pink', 1, None), ('gear', 'vaz2109:party_hat_purple', 1, None),
 ('gear', 'minecraft:name_tag', 1, 150), ('gear', 'vaz2109:morgen_phone', 1, None),

 ('rides', 'vaz2109:vaz2109', 1, None), ('rides', 'vaz2109:skateboard', 1, None), ('rides', 'vaz2109:scooter', 1, None),
 ('rides', 'vaz2109:skate_ramp', 3, None), ('rides', 'vaz2109:skate_kicker', 3, None), ('rides', 'vaz2109:bull_bar', 1, None),
 ('rides', 'vaz2109:turbo', 1, None), ('rides', 'vaz2109:spoiler', 1, None), ('rides', 'vaz2109:radio', 1, None),
 ('rides', 'vaz2109:lowering_kit', 1, None), ('rides', 'vaz2109:spray_can', 1, None), ('rides', 'minecraft:saddle', 1, 150),
 ('rides', 'minecraft:lead', 1, 30), ('rides', 'minecraft:oak_boat', 1, 15), ('rides', 'minecraft:minecart', 1, 60),
 ('rides', 'minecraft:firework_rocket', 16, 48), ('rides', 'minecraft:elytra', 1, 7000),

 ('magic', 'book:minecraft:mending:1', 1, 2500), ('magic', 'book:minecraft:unbreaking:3', 1, 900),
 ('magic', 'book:minecraft:sharpness:5', 1, 1500), ('magic', 'book:minecraft:protection:4', 1, 1200),
 ('magic', 'book:minecraft:efficiency:5', 1, 1200), ('magic', 'book:minecraft:fortune:3', 1, 1800),
 ('magic', 'book:minecraft:looting:3', 1, 1500), ('magic', 'book:minecraft:silk_touch:1', 1, 1000),
 ('magic', 'book:minecraft:feather_falling:4', 1, 700), ('magic', 'book:minecraft:power:5', 1, 1200),
 ('magic', 'book:minecraft:infinity:1', 1, 1200),
 ('magic', 'potion:minecraft:strong_healing', 1, 120), ('magic', 'potion:minecraft:strong_strength', 1, 180),
 ('magic', 'potion:minecraft:strong_regeneration', 1, 200), ('magic', 'potion:minecraft:strong_swiftness', 1, 120),
 ('magic', 'potion:minecraft:long_fire_resistance', 1, 150), ('magic', 'potion:minecraft:long_night_vision', 1, 80),
 ('magic', 'potion:minecraft:long_invisibility', 1, 150), ('magic', 'potion:minecraft:long_water_breathing', 1, 100),
 ('magic', 'potion:minecraft:long_slow_falling', 1, 100),
 ('magic', 'minecraft:experience_bottle', 8, 120), ('magic', 'minecraft:totem_of_undying', 1, 2500),
 ('magic', 'minecraft:ender_chest', 1, 300), ('magic', 'minecraft:shulker_shell', 2, 600),
 ('magic', 'vaz2109:music_disc_morgen_1', 1, 400), ('magic', 'vaz2109:music_disc_morgen_2', 1, 400), ('magic', 'vaz2109:music_disc_morgen_3', 1, 400),

 ('bank', 'vaz2109:banknote_100', 1, 100), ('bank', 'vaz2109:banknote_1000', 1, 1000),
]
# Daily orders: (id, min count, max count, rubles per item)
ORDERS = [
 ('minecraft:wheat', 32, 64, 2), ('minecraft:carrot', 32, 64, 2), ('minecraft:potato', 32, 64, 2), ('minecraft:beetroot', 16, 32, 2),
 ('minecraft:sugar_cane', 24, 48, 2), ('minecraft:pumpkin', 8, 16, 4), ('minecraft:melon_slice', 32, 64, 1), ('minecraft:cocoa_beans', 8, 16, 2),
 ('minecraft:cooked_porkchop', 16, 32, 6), ('minecraft:cooked_chicken', 16, 32, 5), ('minecraft:cooked_mutton', 16, 32, 5),
 ('minecraft:baked_potato', 16, 32, 3), ('minecraft:pumpkin_pie', 8, 16, 7), ('minecraft:cookie', 16, 32, 2),
 ('minecraft:iron_ingot', 16, 32, 16), ('minecraft:gold_ingot', 8, 16, 40), ('minecraft:copper_ingot', 32, 64, 4),
 ('minecraft:coal', 32, 64, 4), ('minecraft:redstone', 32, 64, 4), ('minecraft:lapis_lazuli', 16, 32, 6),
 ('minecraft:quartz', 16, 32, 4), ('minecraft:amethyst_shard', 16, 32, 4), ('minecraft:emerald', 2, 6, 65), ('minecraft:diamond', 2, 4, 250),
 ('minecraft:birch_log', 32, 64, 1.5), ('minecraft:dark_oak_log', 32, 64, 1.5), ('minecraft:jungle_log', 32, 64, 1.5), ('minecraft:cobblestone', 64, 128, 0.5),
 ('minecraft:leather', 8, 16, 5), ('minecraft:string', 16, 32, 1.8), ('minecraft:bone', 16, 32, 2), ('minecraft:gunpowder', 8, 16, 6),
 ('minecraft:ender_pearl', 4, 8, 18), ('minecraft:blaze_rod', 4, 8, 20), ('minecraft:slime_ball', 8, 16, 6), ('minecraft:spider_eye', 8, 16, 4),
 ('minecraft:rotten_flesh', 32, 64, 1), ('minecraft:feather', 16, 32, 2), ('minecraft:egg', 16, 32, 2), ('minecraft:phantom_membrane', 2, 6, 20),
 ('minecraft:ink_sac', 8, 16, 3), ('minecraft:prismarine_shard', 16, 32, 4), ('minecraft:nether_wart', 16, 32, 4), ('minecraft:ghast_tear', 1, 3, 60),
 ('minecraft:honey_bottle', 4, 8, 6), ('vaz2109:beer', 8, 16, 6), ('minecraft:obsidian', 8, 16, 6), ('minecraft:tnt', 4, 8, 24),
 ('minecraft:glowstone_dust', 16, 32, 2.5),
]

rows = []
for cat, id_, lot, price in OFFERS:
    if price is None:
        price = crafted(id_.split(':')[1], lot)
    rows.append((cat, id_, lot, price))

# --- arbitrage checks -------------------------------------------------------------------------
bad = []
buy_unit = {}
for cat, id_, lot, price in rows:
    if id_.startswith(('book:', 'potion:')):
        continue
    unit = price / lot
    buy_unit[id_] = min(buy_unit.get(id_, 1e9), unit)
    s = sell.get(id_, 0)
    if cat == 'bank':
        if s != unit: bad.append(f'{id_}: banknote buy {unit} != sell {s}')
    elif s >= unit * DEAL:
        bad.append(f'{id_}: sells for {s}, deal price {unit * DEAL:.2f}')
for k, m in material.items():
    s = sell.get('vaz2109:' + k, 0)
    if k not in SELL_MOD and s > m + 1e-9:
        bad.append(f'{k}: sells for {s} > material {m:.2f}')

def reward(count, unit):  # same as ComputerShop.orders (Java Math.round: half up)
    return max(10, int(math.floor(count * int(round(unit * 100)) / 500.0 + 0.5)) * 5)

for id_, lo, hi, unit in ORDERS:
    for n in range(lo, hi + 1):
        per = reward(n, unit) / n
        if id_ in buy_unit and per >= buy_unit[id_] * DEAL:
            bad.append(f'order {id_} x{n}: {per:.2f}/item vs deal {buy_unit[id_] * DEAL:.2f}')
if bad:
    print('\n'.join(bad)); sys.exit(1)

# --- emit ------------------------------------------------------------------------------------
J = []
J.append('package com.bobux.vaz2109.computer;\n')
J.append('/** Generated by tools/prices.py from tools/prices_base.py and the mod recipes. Do not edit by hand. */')
J.append('final class PriceTable {')
J.append('   /** Rubles the computer pays per item. */')
J.append('   static final Object[][] SELL = new Object[][]{')
for k in sorted(sell):
    J.append(f'      {{"{k}", {sell[k]}}},')
J.append('   };')
J.append('   /** Shop: category, item ("book:enchantment:level" / "potion:id" for those), items per lot, rubles per lot. */')
J.append('   static final Object[][] OFFERS = new Object[][]{')
for cat, id_, lot, price in rows:
    J.append(f'      {{{C[cat]}, "{id_}", {lot}, {price}}},')
J.append('   };')
J.append('   /** Orders: item, min count, max count, rubles per item x100. */')
J.append('   static final Object[][] ORDERS = new Object[][]{')
for id_, lo, hi, unit in ORDERS:
    J.append(f'      {{"{id_}", {lo}, {hi}, {int(round(unit * 100))}}},')
J.append('   };\n')
J.append('   private PriceTable() {\n   }\n}')
open(out, 'w').write('\n'.join(J) + '\n')

if md:
    L = ['| Категория | Товар | Лот | Цена, ₽ | Выкуп за шт., ₽ |', '|---|---|---|---|---|']
    for cat, id_, lot, price in rows:
        L.append(f'| {cat} | {id_} | {lot} | {price} | {sell.get(id_, "—")} |')
    open(md, 'w').write('\n'.join(L) + '\n')
print(f'{len(sell)} sell prices, {len(rows)} offers, {len(ORDERS)} orders: no arbitrage')
