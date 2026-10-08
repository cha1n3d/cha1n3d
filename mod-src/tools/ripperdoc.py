"""Ripperdoc station: 32x32 texture atlas and a block model (surgical table, robot arm with a scalpel, monitor)."""
import json, random, sys
from PIL import Image
tex, model_dir = sys.argv[1], sys.argv[2]
random.seed(3)
img = Image.new('RGBA', (32, 32))
def region(u0, v0, u1, v1, fn):
    for y in range(v0 * 2, v1 * 2):
        for x in range(u0 * 2, u1 * 2):
            img.putpixel((x, y), fn(x - u0 * 2, y - v0 * 2))
def noisy(c, n):
    return lambda x, y: tuple(max(0, min(255, int(v * (1 + random.uniform(-n, n))))) for v in c) + (255,)
region(0, 0, 8, 8, lambda x, y: (52, 56, 64, 255) if x in (0, 15) or y in (0, 15) else noisy((74, 80, 90), 0.06)(x, y))   # dark metal panel
region(8, 0, 16, 8, lambda x, y: (120, 128, 138, 255) if (x + y) % 7 == 0 else noisy((168, 176, 186), 0.05)(x, y))         # brushed steel
region(0, 8, 8, 16, lambda x, y: (18, 70, 76, 255) if y % 5 == 0 else noisy((26, 98, 104), 0.08)(x, y))                    # teal padding
region(8, 8, 12, 12, lambda x, y: (60, 255, 230, 255) if y % 3 == 1 and 1 <= x <= 6 - (y % 2) * 2 else (8, 40, 48, 255))   # monitor
region(12, 8, 14, 10, lambda x, y: (255, 40, 50, 255) if 1 <= x <= 2 and 1 <= y <= 2 else (150, 10, 20, 255))             # red light
region(14, 8, 16, 10, lambda x, y: (60, 255, 120, 255) if 1 <= x <= 2 and 1 <= y <= 2 else (10, 120, 50, 255))            # green light
region(12, 10, 16, 12, lambda x, y: (20, 20, 24, 255) if y % 2 else (36, 36, 42, 255))                                       # cables
region(12, 12, 16, 16, lambda x, y: (235, 240, 245, 255) if (x + y) % 4 == 0 else (190, 198, 208, 255))                    # chrome
region(8, 12, 12, 16, lambda x, y: (240, 200, 30, 255) if ((x + y) // 2) % 2 == 0 else (24, 24, 24, 255))                  # hazard stripes
img.save(tex)

DARK, STEEL, PAD, SCREEN, RED, GREEN, CABLE, CHROME, HAZARD = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 12, 12], [12, 8, 14, 10], [14, 8, 16, 10], [12, 10, 16, 12], [12, 12, 16, 16], [8, 12, 12, 16]
def el(f, t, side, up=None, down=None, north=None):
    faces = {d: {"uv": side, "texture": "#t"} for d in ("north", "east", "south", "west")}
    faces["up"] = {"uv": up or side, "texture": "#t"}
    faces["down"] = {"uv": down or side, "texture": "#t"}
    if north:
        faces["north"] = {"uv": north, "texture": "#t"}
    return {"from": f, "to": t, "faces": faces}
elements = [
    el([1, 0, 1], [15, 8, 15], DARK),                                   # cabinet
    el([0.8, 8, 0.8], [15.2, 9, 15.2], HAZARD, up=STEEL),               # hazard band
    el([0, 9, 0], [16, 11, 16], STEEL, up=PAD),                         # table with padding
    el([4.5, 11, 0.5], [11.5, 12.2, 4], PAD),                           # headrest
    el([3, 4.5, 0.6], [4.4, 5.9, 1], RED),                              # status lights
    el([5, 4.5, 0.6], [6.4, 5.9, 1], GREEN),
    el([13, 11, 13], [15, 23, 15], STEEL),                              # robot arm: column
    el([6, 22, 13], [15, 23.5, 15], STEEL),                             #            boom
    el([6.4, 17.5, 13.3], [8.1, 22, 14.7], CHROME),                     #            wrist
    el([6.9, 16.3, 13.7], [7.6, 17.5, 14.3], RED),                      #            laser scalpel
    el([14.2, 9, 14.2], [15.2, 22, 15.2], CABLE),                       # cable down the column
    el([1, 11, 12.5], [7.5, 17, 13.6], DARK, north=SCREEN),             # monitor
    el([3.7, 11, 13.6], [4.8, 12.5, 14.6], DARK),                       # monitor stand
]
model = {"parent": "minecraft:block/block", "textures": {"t": "vaz2109:block/ripperdoc", "particle": "vaz2109:block/ripperdoc"}, "elements": elements}
json.dump(model, open(f'{model_dir}/block/ripperdoc.json', 'w'), indent=1)
json.dump({"parent": "vaz2109:block/ripperdoc"}, open(f'{model_dir}/item/ripperdoc.json', 'w'))
print('ok')
