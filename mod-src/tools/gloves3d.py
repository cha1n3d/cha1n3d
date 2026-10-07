"""3D model for Itadori's red gloves (worn over the fist like the brass knuckles) and its 16x16 texture:
quadrants = red leather / dark seams / white strap / knuckle padding."""
import json, random, sys
from PIL import Image
tex_out, model_dir = sys.argv[1], sys.argv[2]
random.seed(7)
img = Image.new('RGBA', (16, 16))
def fill(x0, y0, base, var):
    for y in range(y0, y0 + 8):
        for x in range(x0, x0 + 8):
            n = random.uniform(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, int(c * (1 + n)))) for c in base) + (255,))
fill(0, 0, (196, 22, 28), 0.10)      # leather
fill(8, 0, (125, 10, 14), 0.08)      # seams / shadow
fill(0, 8, (238, 238, 238), 0.04)    # strap
fill(8, 8, (226, 60, 66), 0.10)      # padded knuckles (lighter)
for x in range(0, 8):                 # stitching across the leather
    img.putpixel((x, 3), (110, 8, 12, 255))
for y in range(8, 16):                # strap edge
    img.putpixel((0, y), (190, 190, 190, 255)); img.putpixel((7, y), (190, 190, 190, 255))
img.save(tex_out)

LEATHER, SEAM, STRAP, PAD = [0, 0, 8, 8], [8, 0, 16, 8], [0, 8, 8, 16], [8, 8, 16, 16]
def box(f, t, uv, up=None, down=None):
    faces = {d: {"uv": uv, "texture": "#0"} for d in ("north", "east", "south", "west")}
    faces["up"] = {"uv": up or uv, "texture": "#0"}
    faces["down"] = {"uv": down or uv, "texture": "#0"}
    return {"from": f, "to": t, "faces": faces}
elements = [
    box([5.5, 6.2, 5.5], [10.5, 10.6, 10.5], LEATHER, up=SEAM),          # mitt around the hand
    *[box([5.3, 10.6, z], [8.7, 11.3, z + 1.0], PAD) for z in (5.75, 6.85, 7.95, 9.05)],  # four padded knuckles
    *[box([5.2, 8.4, z], [5.5, 10.6, z + 1.0], SEAM) for z in (5.75, 6.85, 7.95, 9.05)],   # curled fingers
    box([7.4, 7.6, 10.5], [9.8, 9.9, 11.2], LEATHER),                     # thumb
    box([5.25, 5.0, 5.25], [10.75, 6.3, 10.75], STRAP),                   # white wrist strap
    box([5.6, 3.8, 5.6], [10.4, 5.0, 10.4], SEAM),                        # cuff
]
worn = {
    "gui_light": "side",
    "textures": {"0": "vaz2109:item/red_gloves_3d", "particle": "vaz2109:item/red_gloves"},
    "elements": elements,
    "display": {
        "thirdperson_righthand": {"rotation": [-90, 180, 0], "translation": [0, -2, 2], "scale": [1, 1, 1]},
        "thirdperson_lefthand": {"rotation": [-90, -180, 0], "translation": [0, -2, 2], "scale": [1, 1, 1]},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, -2], "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, -2], "scale": [0.6, 0.6, 0.6]},
    },
}
icon = {"parent": "minecraft:item/generated", "textures": {"layer0": "vaz2109:item/red_gloves"}}
top = {
    "loader": "forge:separate_transforms",
    "gui_light": "front",
    "base": {"parent": "vaz2109:item/red_gloves_worn"},
    "perspectives": {
        "gui": {"parent": "vaz2109:item/red_gloves_icon"},
        "ground": {"parent": "vaz2109:item/red_gloves_icon"},
        "fixed": {"parent": "vaz2109:item/red_gloves_icon"},
    },
}
for name, m in (("red_gloves_worn", worn), ("red_gloves_icon", icon), ("red_gloves", top)):
    json.dump(m, open(f"{model_dir}/{name}.json", "w"), indent=1)
print("ok")
