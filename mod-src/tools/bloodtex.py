import sys, math, random
from PIL import Image
out = sys.argv[1]; random.seed(7)

def clamp(v): return max(0, min(255, int(v)))

def stream(w, h, core):
    """Liquid blood jet: dark maroon rim, saturated crimson body, glossy highlight off-centre,
    pressure bands and fine streaks along the flow. Tiles seamlessly along v."""
    im = Image.new('RGBA', (w, h)); px = im.load()
    streaks = [(random.uniform(0, 6.28), random.choice([2, 3, 4, 6]), random.uniform(0.4, 1.0), random.uniform(-0.8, 0.8)) for _ in range(9)]
    for y in range(h):
        t = y / h * 2 * math.pi
        band = 0.5 + 0.5 * math.sin(t * 2)
        for x in range(w):
            u = (x + 0.5) / w * 2 - 1
            body = math.sqrt(max(0.0, 1 - u * u))
            fine = sum(a * math.exp(-((u - c) ** 2) / 0.01) * (0.5 + 0.5 * math.sin(k * t + ph)) for ph, k, a, c in streaks) / 3.0
            gloss = math.exp(-((u + 0.35) ** 2) / (0.008 if core else 0.02)) * (0.6 + 0.4 * band)
            if core:
                r = 150 + 90 * body + 60 * gloss + 20 * fine
                g = 6 + 18 * body + 120 * gloss
                b = 10 + 18 * body + 110 * gloss
                a = 255 * min(1.0, body * 1.8)
            else:
                rim = 1 - body
                r = 55 + 125 * body + 40 * fine + 35 * band * body + 80 * gloss
                g = 2 + 10 * body + 60 * gloss
                b = 6 + 12 * body + 55 * gloss
                r *= 1 - 0.45 * rim
                a = 255 * min(1.0, body * 2.4) * (0.9 + 0.1 * band)
            px[x, y] = (clamp(r), clamp(g), clamp(b), clamp(a))
    return im

def orb(n):
    im = Image.new('RGBA', (n, n)); px = im.load(); c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            dx, dy = (x - c) / c, (y - c) / c
            d = math.hypot(dx, dy)
            if d > 1.0: continue
            z = math.sqrt(max(0.0, 1 - d * d))
            light = max(0.0, (-0.4 * dx - 0.5 * dy + 0.75 * z))
            spec = max(0.0, light) ** 18
            r = 70 + 120 * light + 140 * spec; g = 4 + 10 * light + 150 * spec; b = 8 + 14 * light + 150 * spec
            a = 255 * min(1.0, (1.0 - d) * 6.0)
            px[x, y] = (clamp(r), clamp(g), clamp(b), clamp(a))
    return im

def splash(n):
    im = Image.new('RGBA', (n, n)); px = im.load(); c = (n - 1) / 2
    blobs = [(c, c, n * 0.22)] + [(c + math.cos(a) * r, c + math.sin(a) * r, n * random.uniform(0.04, 0.09))
             for a, r in ((random.uniform(0, 6.28), random.uniform(n * 0.18, n * 0.44)) for _ in range(14))]
    for y in range(n):
        for x in range(n):
            f = 0.0
            for bx, by, br in blobs:
                f += (br * br) / ((x - bx) ** 2 + (y - by) ** 2 + 1.0)
            if f > 1.0:
                k = min(1.0, (f - 1.0) / 2.0)
                px[x, y] = (clamp(100 + 70 * k), clamp(5 + 10 * k), clamp(10 + 10 * k), clamp(150 + 105 * k))
    return im

stream(16, 64, False).save(f'{out}/blood_beam.png')
stream(16, 64, True).save(f'{out}/blood_beam_core.png')
orb(32).save(f'{out}/blood_orb.png')
splash(32).save(f'{out}/blood_splash.png')
prev = Image.new('RGBA', (4 * 136, 136), (60, 70, 60, 255))
for i, nme in enumerate(['blood_beam', 'blood_beam_core', 'blood_orb', 'blood_splash']):
    t = Image.open(f'{out}/{nme}.png')
    t = t.resize((32, 128) if 'beam' in nme else (128, 128), Image.NEAREST)
    prev.alpha_composite(t, (i * 136 + (48 if 'beam' in nme else 4), 4))
prev.save(sys.argv[2])
