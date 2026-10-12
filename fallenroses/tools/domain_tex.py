"""Textures for the 0.9 domain barrier and domain interiors (numpy + PIL).

- vfx/barrier.png, vfx/veins.png: tileable surface of the black barrier and the cursed-energy veins on it.
- vfx/shard.png: a barrier shard for the collapse.
- domain/pano_<type>.png + pano_<type>_glow.png: 360-degree horizon panoramas inside each domain
  (dark silhouettes over a transparent sky, plus an additive layer with lights).
Usage: python3 tools/domain_tex.py res/assets/fallenroses/textures
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

S = 2  # supersampling
W, H = 1024 * S, 256 * S


def periodic_noise(n, seed, power=2.2):
    r = np.random.default_rng(seed)
    f = np.fft.fftfreq(n)
    fx, fy = np.meshgrid(f, f)
    k = np.sqrt(fx ** 2 + fy ** 2) + 1e-6
    spec = (r.standard_normal((n, n)) + 1j * r.standard_normal((n, n))) / k ** power
    spec[0, 0] = 0
    x = np.real(np.fft.ifft2(spec))
    x -= x.min()
    return x / x.max()


def barrier(out):
    n = 256
    a = periodic_noise(n, 1, 2.0)
    b = periodic_noise(n, 2, 1.4)
    v = 0.35 + 0.45 * a + 0.2 * b
    ridge = 1 - np.abs(periodic_noise(n, 3, 1.8) - 0.5) * 2
    v = np.clip(v + 0.35 * ridge ** 8, 0, 1)
    img = (np.stack([v * 0.92, v * 0.92, v], -1) * 255).astype(np.uint8)
    Image.fromarray(img, 'RGB').convert('RGBA').save(os.path.join(out, 'vfx', 'barrier.png'))


def veins(out):
    n = 256
    acc = np.zeros((n, n))
    for seed, w in ((11, 1.0), (12, 0.6), (13, 0.35)):
        x = periodic_noise(n, seed, 1.6)
        line = np.exp(-((x - 0.5) / 0.012) ** 2)
        acc += line * w
    acc = np.clip(acc, 0, 1) ** 0.8
    glow = np.array(Image.fromarray((acc * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2))) / 255
    v = np.clip(acc + glow * 0.5, 0, 1)
    rgba = np.zeros((n, n, 4), np.uint8)
    rgba[..., :3] = 255
    rgba[..., 3] = (v * 255).astype(np.uint8)
    Image.fromarray(rgba, 'RGBA').save(os.path.join(out, 'vfx', 'veins.png'))


def shard(out):
    n = 64 * S
    img = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pts = [(0.5, 0.02), (0.92, 0.38), (0.7, 0.97), (0.18, 0.8), (0.06, 0.3)]
    d.polygon([(x * n, y * n) for x, y in pts], fill=(255, 255, 255, 255))
    img.resize((64, 64), Image.LANCZOS).save(os.path.join(out, 'vfx', 'shard.png'))


# ---------------------------------------------------------------- panoramas

class Pano:
    def __init__(self, seed):
        self.r = np.random.default_rng(seed)
        self.sil = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        self.ds = ImageDraw.Draw(self.sil)
        self.dg = ImageDraw.Draw(self.glow)

    def wrap(self, fn, x, *a, **k):
        """Draw at x and at x +- W so shapes that cross the seam tile around the cylinder."""
        for off in (-W, 0, W):
            fn(x + off, *a, **k)

    def ground(self, y0, col, rough=0.0):
        pts = [(0, H)]
        for i in range(65):
            x = i * W / 64
            pts.append((x, y0 + rough * self.r.uniform(-1, 1)))
        pts.append((W, H))
        self.ds.polygon(pts, fill=col)

    def save(self, out, name, blur_glow=3):
        g = self.glow.filter(ImageFilter.GaussianBlur(blur_glow * S))
        g = Image.alpha_composite(g, self.glow)
        path = os.path.join(out, 'domain')
        os.makedirs(path, exist_ok=True)
        self.sil.resize((W // S, H // S), Image.LANCZOS).save(os.path.join(path, f'pano_{name}.png'))
        g.resize((W // S, H // S), Image.LANCZOS).save(os.path.join(path, f'pano_{name}_glow.png'))


def pano_shrine(out):
    """Malevolent Shrine: the shrine with its fanged gate, piles of skulls and horned skulls around."""
    p = Pano(21)
    col = (22, 6, 8, 255)
    p.ground(H * 0.86, col, 3 * S)
    # skull piles all around
    for i in range(160):
        x = p.r.uniform(0, W)
        y = H * 0.84 + p.r.uniform(-H * 0.05, H * 0.08)
        s = p.r.uniform(6, 14) * S

        def skull(x, y=y, s=s):
            p.ds.ellipse([x - s, y - s, x + s, y + s * 0.9], fill=col)
            p.ds.rectangle([x - s * 0.55, y + s * 0.5, x + s * 0.55, y + s * 1.2], fill=col)
        p.wrap(skull, x)
    for i in range(10):  # ox skulls on poles
        x = p.r.uniform(0, W)
        if abs(x - W / 2) < W * 0.12:
            continue
        h = p.r.uniform(0.25, 0.4) * H

        def pole(x, h=h):
            base = H * 0.86
            p.ds.rectangle([x - 2 * S, base - h, x + 2 * S, base], fill=col)
            p.ds.ellipse([x - 12 * S, base - h - 10 * S, x + 12 * S, base - h + 10 * S], fill=col)
            p.ds.polygon([(x - 10 * S, base - h - 4 * S), (x - 34 * S, base - h - 26 * S), (x - 14 * S, base - h - 8 * S)], fill=col)
            p.ds.polygon([(x + 10 * S, base - h - 4 * S), (x + 34 * S, base - h - 26 * S), (x + 14 * S, base - h - 8 * S)], fill=col)
        p.wrap(pole, x)
    # the shrine (centre of the panorama = behind the caster)
    cx, base = W / 2, H * 0.86
    w = W * 0.2
    p.ds.rectangle([cx - w * 0.6, base - H * 0.08, cx + w * 0.6, base], fill=col)  # platform
    for k in (-0.45, -0.28, 0.28, 0.45):
        p.ds.rectangle([cx + k * w - 4 * S, base - H * 0.36, cx + k * w + 4 * S, base - H * 0.08], fill=col)
    # lower roof with upturned eaves
    def roof(y, half, h):
        p.ds.polygon([(cx - half * 1.15, y + h * 0.15), (cx - half, y + h * 0.55), (cx + half, y + h * 0.55),
                      (cx + half * 1.15, y + h * 0.15), (cx + half * 0.75, y - h * 0.35), (cx - half * 0.75, y - h * 0.35)], fill=col)
    roof(base - H * 0.42, w * 0.62, H * 0.12)
    p.ds.rectangle([cx - w * 0.3, base - H * 0.62, cx + w * 0.3, base - H * 0.42], fill=col)
    roof(base - H * 0.66, w * 0.45, H * 0.11)
    p.ds.polygon([(cx - w * 0.18, base - H * 0.7), (cx, base - H * 0.88), (cx + w * 0.18, base - H * 0.7)], fill=col)
    # fanged mouth gate between the pillars
    mx0, mx1, my0, my1 = cx - w * 0.25, cx + w * 0.25, base - H * 0.33, base - H * 0.08
    p.dg.rectangle([mx0, my0, mx1, my1], fill=(255, 40, 30, 150))
    for i in range(9):
        tx = mx0 + (mx1 - mx0) * (i + 0.5) / 9
        p.ds.polygon([(tx - 8 * S, my0), (tx + 8 * S, my0), (tx, my0 + 22 * S)], fill=col)
        p.ds.polygon([(tx - 8 * S, my1), (tx + 8 * S, my1), (tx, my1 - 18 * S)], fill=col)
    for k in (-1, 1):  # horns on the roof
        p.ds.polygon([(cx + k * w * 0.42, base - H * 0.45), (cx + k * w * 0.7, base - H * 0.78), (cx + k * w * 0.5, base - H * 0.47)], fill=col)
    # red lanterns
    for i in range(30):
        x = p.r.uniform(0, W)
        y = H * p.r.uniform(0.7, 0.82)
        p.wrap(lambda x, y=y: p.dg.ellipse([x - 3 * S, y - 3 * S, x + 3 * S, y + 3 * S], fill=(255, 60, 40, 200)), x)
    p.save(out, 'shrine')


def pano_flames(out):
    """Coffin of the Iron Mountain: a ring of volcanoes with lava running down their slopes."""
    p = Pano(22)
    col = (26, 10, 6, 255)
    for layer, (y0, amp, c) in enumerate(((0.55, 0.25, (40, 16, 10, 255)), (0.66, 0.22, col))):
        pts = [(0, H)]
        for i in range(129):
            x = i * W / 128
            y = H * y0 + H * amp * (0.5 * math.sin(i * 0.31 + layer) + 0.35 * math.sin(i * 0.73 + 2 * layer)) + p.r.uniform(-4, 4) * S
            pts.append((x, y))
        pts.append((W, H))
        p.ds.polygon(pts, fill=c)
    cx = W / 2
    for k, (x, h, w) in enumerate(((cx, 0.78, 0.16), (cx - W * 0.3, 0.6, 0.1), (cx + W * 0.28, 0.66, 0.12))):
        top = H * (1 - h)
        p.ds.polygon([(x - W * w, H * 0.9), (x - W * 0.025, top), (x + W * 0.025, top), (x + W * w, H * 0.9)], fill=col)
        p.dg.ellipse([x - W * 0.03, top - 10 * S, x + W * 0.03, top + 8 * S], fill=(255, 140, 30, 230))
        for j in range(5):  # lava rivers
            pts = []
            xx, yy = x + p.r.uniform(-W * 0.02, W * 0.02), top
            while yy < H * 0.9:
                pts.append((xx, yy))
                yy += 8 * S
                xx += p.r.uniform(-5, 5) * S + (j - 2) * 2.2 * S
            p.dg.line(pts, fill=(255, 110, 20, 220), width=3 * S)
    for i in range(70):  # cracks in the ground
        x, y = p.r.uniform(0, W), H * p.r.uniform(0.88, 0.99)
        p.wrap(lambda x, y=y: p.dg.line([(x, y), (x + p.r.uniform(-30, 30) * S, y + p.r.uniform(-4, 4) * S)],
                                        fill=(255, 90, 10, 200), width=2 * S), x)
    p.ground(H * 0.9, col, 2 * S)
    p.save(out, 'flames')


def pano_flesh(out):
    """Self-Embodiment of Perfection: endless raised hands."""
    p = Pano(23)
    for row, (base, scale, c) in enumerate(((0.75, 0.8, (34, 18, 40, 255)), (0.88, 1.0, (20, 10, 26, 255)))):
        n = 34 if row == 0 else 26
        for i in range(n):
            x = (i + p.r.uniform(-0.3, 0.3)) * W / n
            s = scale * p.r.uniform(0.8, 1.2) * S
            tilt = p.r.uniform(-0.3, 0.3)

            def hand(x, s=s, tilt=tilt, base=base, c=c):
                y = H * base
                palm_w, palm_h = 26 * s, 34 * s
                p.ds.rectangle([x - 9 * s, y - 10 * s, x + 9 * s, H], fill=c)  # arm
                p.ds.ellipse([x - palm_w / 2, y - palm_h, x + palm_w / 2, y], fill=c)
                for f, (dx, ln) in enumerate(((-11, 36), (-4, 46), (4, 48), (11, 40))):
                    fx = x + dx * s
                    tx = fx + tilt * ln * s
                    p.ds.line([(fx, y - palm_h * 0.7), (tx, y - palm_h * 0.7 - ln * s)], fill=c, width=int(7 * s))
                    p.ds.ellipse([tx - 3.5 * s, y - palm_h * 0.7 - ln * s - 3.5 * s, tx + 3.5 * s, y - palm_h * 0.7 - ln * s + 3.5 * s], fill=c)
                p.ds.line([(x - palm_w * 0.45, y - palm_h * 0.4), (x - palm_w * 0.95, y - palm_h * 0.9)], fill=c, width=int(7 * s))
            p.wrap(hand, x)
    for i in range(80):
        x, y = p.r.uniform(0, W), H * p.r.uniform(0.3, 0.8)
        p.wrap(lambda x, y=y: p.dg.ellipse([x - 2 * S, y - 2 * S, x + 2 * S, y + 2 * S], fill=(170, 200, 255, 160)), x)
    p.save(out, 'flesh')


def pano_sea(out):
    """Horizon of the Captivating Skandha: a calm tropical shore."""
    p = Pano(24)
    sea = (16, 60, 72, 230)
    p.ds.rectangle([0, H * 0.72, W, H], fill=sea)
    for i in range(6):  # distant islands
        x, w = p.r.uniform(0, W), p.r.uniform(60, 160) * S
        p.wrap(lambda x, w=w: p.ds.ellipse([x - w, H * 0.69, x + w, H * 0.75], fill=(20, 48, 52, 255)), x)
    p.ds.rectangle([0, H * 0.9, W, H], fill=(70, 62, 40, 255))  # sand
    for i in range(14):  # palms
        x = p.r.uniform(0, W)
        h = p.r.uniform(0.35, 0.55) * H
        lean = p.r.uniform(-40, 40) * S

        def palm(x, h=h, lean=lean):
            col = (14, 26, 22, 255)
            base, top = (x, H * 0.92), (x + lean, H * 0.92 - h)
            p.ds.line([base, ((base[0] + top[0]) / 2 + lean * 0.2, (base[1] + top[1]) / 2), top], fill=col, width=7 * S)
            for a in range(7):
                ang = math.pi * (0.1 + 0.8 * a / 6)
                ex, ey = top[0] + math.cos(ang) * 70 * S * (1 if a % 2 else -1), top[1] + math.sin(ang) * 25 * S
                p.ds.line([top, ((top[0] + ex) / 2, top[1] - 12 * S), (ex, ey)], fill=col, width=5 * S)
        p.wrap(palm, x)
    for i in range(140):  # glints on the water
        x, y = p.r.uniform(0, W), H * p.r.uniform(0.73, 0.88)
        p.wrap(lambda x, y=y: p.dg.line([(x, y), (x + p.r.uniform(6, 20) * S, y)], fill=(255, 250, 220, 140), width=S), x)
    p.save(out, 'sea')


def pano_cemetery(out):
    p = Pano(25)
    col = (18, 20, 18, 255)
    p.ground(H * 0.88, col, 2 * S)
    for i in range(90):
        x = p.r.uniform(0, W)
        y = H * p.r.uniform(0.82, 0.9)
        s = p.r.uniform(0.7, 1.3) * S
        kind = p.r.integers(0, 3)

        def grave(x, y=y, s=s, kind=kind):
            if kind == 0:
                p.ds.rounded_rectangle([x - 12 * s, y - 34 * s, x + 12 * s, y], radius=int(10 * s), fill=col)
            elif kind == 1:
                p.ds.rectangle([x - 3 * s, y - 44 * s, x + 3 * s, y], fill=col)
                p.ds.rectangle([x - 14 * s, y - 34 * s, x + 14 * s, y - 28 * s], fill=col)
            else:
                p.ds.rectangle([x - 8 * s, y - 50 * s, x + 8 * s, y], fill=col)  # sotoba / grave post
                p.ds.polygon([(x - 8 * s, y - 50 * s), (x, y - 58 * s), (x + 8 * s, y - 50 * s)], fill=col)
        p.wrap(grave, x)
    for i in range(8):  # dead trees
        x = p.r.uniform(0, W)

        def tree(x):
            def branch(x0, y0, ang, ln, w):
                if ln < 8 * S:
                    return
                x1, y1 = x0 + math.cos(ang) * ln, y0 - math.sin(ang) * ln
                p.ds.line([(x0, y0), (x1, y1)], fill=col, width=max(1, int(w)))
                for da in (-0.5, 0.45):
                    branch(x1, y1, ang + da + p.r.uniform(-0.2, 0.2), ln * 0.7, w * 0.65)
            branch(x, H * 0.88, math.pi / 2, H * 0.22, 9 * S)
        p.wrap(tree, x)
    for i in range(40):  # will-o'-wisps
        x, y = p.r.uniform(0, W), H * p.r.uniform(0.45, 0.8)
        p.wrap(lambda x, y=y: p.dg.ellipse([x - 4 * S, y - 4 * S, x + 4 * S, y + 4 * S], fill=(150, 255, 190, 170)), x)
    p.save(out, 'cemetery')


def pano_pachinko(out):
    """Idle Death Gamble: rows of pachinko machines under neon."""
    p = Pano(26)
    body = (14, 22, 18, 255)
    n = 28
    mw = W / n
    for i in range(n):
        x0 = i * mw + 4 * S
        x1 = (i + 1) * mw - 4 * S
        top, bot = H * 0.38, H * 0.95
        p.ds.rectangle([x0, top, x1, bot], fill=body)
        cx = (x0 + x1) / 2
        p.dg.ellipse([cx - mw * 0.32, top + 20 * S, cx + mw * 0.32, top + 20 * S + mw * 0.64], outline=(120, 255, 160, 220), width=2 * S)
        for k in range(10):
            col = [(255, 80, 160), (80, 255, 140), (255, 230, 80), (120, 180, 255)][(i + k) % 4]
            y = top + 6 * S + k * 4 * S
            p.dg.ellipse([x0 + 3 * S, y, x0 + 6 * S, y + 3 * S], fill=col + (230,))
            p.dg.ellipse([x1 - 6 * S, y, x1 - 3 * S, y + 3 * S], fill=col + (230,))
        p.dg.rectangle([x0 + 6 * S, bot - 40 * S, x1 - 6 * S, bot - 30 * S], fill=(255, 230, 120, 200))
    p.dg.line([(0, H * 0.33), (W, H * 0.33)], fill=(80, 255, 140, 230), width=4 * S)
    p.dg.line([(0, H * 0.31), (W, H * 0.31)], fill=(255, 80, 160, 200), width=2 * S)
    p.save(out, 'pachinko', blur_glow=4)


def pano_court(out):
    """Deadly Sentencing: the courtroom, judge's bench at the centre."""
    p = Pano(27)
    col = (30, 20, 12, 255)
    p.ground(H * 0.9, col)
    for i in range(24):  # columns
        x = (i + 0.5) * W / 24
        p.ds.rectangle([x - 10 * S, H * 0.3, x + 10 * S, H * 0.9], fill=col)
        p.ds.rectangle([x - 16 * S, H * 0.28, x + 16 * S, H * 0.32], fill=col)
    p.ds.rectangle([0, H * 0.24, W, H * 0.28], fill=col)  # architrave
    for i in range(60):  # gallery benches
        x = p.r.uniform(0, W)
        p.wrap(lambda x: p.ds.rectangle([x - 22 * S, H * 0.8, x + 22 * S, H * 0.86], fill=col), x)
    cx = W / 2
    p.ds.rectangle([cx - W * 0.09, H * 0.6, cx + W * 0.09, H * 0.9], fill=col)  # bench
    p.ds.rectangle([cx - W * 0.1, H * 0.57, cx + W * 0.1, H * 0.61], fill=col)
    p.ds.rectangle([cx - W * 0.02, H * 0.4, cx + W * 0.02, H * 0.6], fill=col)  # chair back
    gold = (255, 210, 120, 230)
    sy = H * 0.12
    p.dg.line([(cx, sy), (cx, sy + 60 * S)], fill=gold, width=3 * S)  # scales of justice
    p.dg.line([(cx - 50 * S, sy + 10 * S), (cx + 50 * S, sy + 10 * S)], fill=gold, width=3 * S)
    for k in (-1, 1):
        x = cx + k * 50 * S
        p.dg.line([(x, sy + 10 * S), (x - 12 * S, sy + 34 * S)], fill=gold, width=2 * S)
        p.dg.line([(x, sy + 10 * S), (x + 12 * S, sy + 34 * S)], fill=gold, width=2 * S)
        p.dg.arc([x - 14 * S, sy + 26 * S, x + 14 * S, sy + 42 * S], 0, 180, fill=gold, width=3 * S)
    for i in range(24):  # lamps between the columns
        x = (i + 1) * W / 24
        p.dg.ellipse([x - 5 * S, H * 0.45, x + 5 * S, H * 0.45 + 10 * S], fill=(255, 220, 150, 220))
    p.save(out, 'court')


def pano_film(out):
    """Time Cell Moon Palace: a palace built of film frames."""
    p = Pano(28)
    col = (10, 12, 26, 255)
    p.ground(H * 0.9, col)
    for i in range(9):  # towers
        x = (i + 0.5) * W / 9
        h = H * p.r.uniform(0.35, 0.6)
        p.ds.rectangle([x - 30 * S, H * 0.9 - h, x + 30 * S, H * 0.9], fill=col)
        p.ds.polygon([(x - 40 * S, H * 0.9 - h), (x, H * 0.9 - h - 40 * S), (x + 40 * S, H * 0.9 - h)], fill=col)
    for band_y in (0.55, 0.72):  # film strips around the palace
        y0, y1 = H * band_y, H * band_y + 40 * S
        p.ds.rectangle([0, y0, W, y1], fill=col)
        for k in range(int(W / (14 * S))):
            x = k * 14 * S
            p.dg.rectangle([x + 3 * S, y0 + 3 * S, x + 9 * S, y0 + 8 * S], fill=(200, 210, 255, 150))
            p.dg.rectangle([x + 3 * S, y1 - 8 * S, x + 9 * S, y1 - 3 * S], fill=(200, 210, 255, 150))
        for k in range(int(W / (48 * S))):
            x = k * 48 * S
            p.dg.rectangle([x + 4 * S, y0 + 11 * S, x + 44 * S, y1 - 11 * S], outline=(140, 160, 255, 160), width=S)
    p.save(out, 'film')


def pano_station(out):
    """Vessel's domain: a night train station."""
    p = Pano(29)
    col = (20, 14, 12, 255)
    p.ds.rectangle([0, H * 0.82, W, H], fill=col)  # platform
    p.ds.rectangle([0, H * 0.25, W, H * 0.3], fill=col)  # canopy
    for i in range(18):
        x = (i + 0.5) * W / 18
        p.ds.rectangle([x - 5 * S, H * 0.3, x + 5 * S, H * 0.82], fill=col)
        p.dg.ellipse([x + 30 * S, H * 0.32, x + 44 * S, H * 0.32 + 10 * S], fill=(255, 230, 170, 230))
    tx0, tx1 = W * 0.3, W * 0.7  # the train
    p.ds.rounded_rectangle([tx0, H * 0.5, tx1, H * 0.84], radius=14 * S, fill=(26, 20, 18, 255))
    for k in range(int((tx1 - tx0) / (34 * S))):
        x = tx0 + 12 * S + k * 34 * S
        p.dg.rectangle([x, H * 0.56, x + 22 * S, H * 0.64], fill=(255, 210, 140, 200))
    for i in range(10):  # signs
        x = p.r.uniform(0, W)
        p.wrap(lambda x: p.dg.rectangle([x - 26 * S, H * 0.34, x + 26 * S, H * 0.38], fill=(120, 200, 255, 200)), x)
    p.save(out, 'station')


def main():
    out = sys.argv[1]
    os.makedirs(os.path.join(out, 'vfx'), exist_ok=True)
    barrier(out)
    veins(out)
    shard(out)
    for fn in (pano_shrine, pano_flames, pano_flesh, pano_sea, pano_cemetery, pano_pachinko, pano_court, pano_film, pano_station):
        fn(out)
        print(fn.__name__)


if __name__ == '__main__':
    main()
