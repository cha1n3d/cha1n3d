"""Rough isometric preview of a block-model JSON (painter's algorithm, flat-shaded faces, texture averaged per face)."""
import json, math, sys
from PIL import Image, ImageDraw
model, tex, out = sys.argv[1], sys.argv[2], sys.argv[3]
arm = '--arm' in sys.argv
m = json.load(open(model)); t = Image.open(tex).convert('RGBA')
def avg(uv):
    x0, y0, x1, y1 = [int(v * t.width / 16) for v in uv]
    px = [t.getpixel((x, y)) for x in range(min(x0, x1), max(x0, x1)) for y in range(min(y0, y1), max(y0, y1))] or [t.getpixel((x0, y0))]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3))
els = [(e['from'], e['to'], {k: avg(v['uv']) for k, v in e['faces'].items()}) for e in m['elements']]
if arm:
    els.insert(0, ([6, -6, 6], [10, 10.4, 10], {k: (200, 160, 130) for k in ('north', 'south', 'east', 'west', 'up', 'down')}))
S = 22; W = 520
def proj(x, y, z, view):
    a = math.radians(view)
    xr, zr = x * math.cos(a) - z * math.sin(a), x * math.sin(a) + z * math.cos(a)
    return (W / 2 + (xr - zr) * S * 0.87, W * 0.62 - y * S + (xr + zr) * S * 0.5 - 0 , xr + zr - y * 0.01)
img = Image.new('RGB', (W * 2, W), (40, 40, 48)); d = ImageDraw.Draw(img)
for k, view in enumerate((0, 180)):
    faces = []
    for f, to, col in els:
        x0, y0, z0 = [v - 8 for v in f]; x1, y1, z1 = [v - 8 for v in to]
        quads = {'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], 'down': [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)],
                 'north': [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], 'south': [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
                 'west': [(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], 'east': [(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)]}
        light = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.65, 'east': 0.65}
        for n, q in quads.items():
            p = [proj(*v, view) for v in q]
            depth = sum(v[2] for v in p) / 4
            c = tuple(int(ch * light[n]) for ch in col.get(n, (255, 0, 255)))
            faces.append((depth, [(v[0] + k * W, v[1]) for v in p], c))
    for _, poly, c in sorted(faces, key=lambda f: f[0]):
        d.polygon(poly, fill=c, outline=(20, 5, 5))
img.save(out)
