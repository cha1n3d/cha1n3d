"""Shaded 16x16 item icons (light from the top left, coloured outlines) for the cyberware, the street items,
the firecracker and the first-aid kit. usage: icons2.py <texture dir> <preview.png>"""
import math, sys
from PIL import Image
out = sys.argv[1]

def hx(c, a=255):
    c = c.lstrip('#'); return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)
def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)
def ramp(base, n=5):
    """dark outline .. highlight"""
    b = hx(base); black = (12, 10, 18, 255); white = (255, 255, 255, 255)
    return [mix(b, black, 0.62), mix(b, black, 0.32), b, mix(b, white, 0.35), mix(b, white, 0.7)]

STEEL = ramp('#8a95a5'); DSTEEL = ramp('#55606e'); GOLD = ramp('#d8a531'); RED = ramp('#d0242c'); PINK = ramp('#ff4fc8')
CYAN = ramp('#28d8f0'); YEL = ramp('#f2d43a'); PURP = ramp('#a24cf0'); ORANGE = ramp('#ff8a1e'); GREEN = ramp('#58d048')
WHITE = ramp('#e8ebef'); TAN = ramp('#d9b27a'); BROWN = ramp('#7a4a26'); BLUE = ramp('#3a6ee8'); BLACK = ramp('#2a2c34')

class Icon:
    def __init__(self):
        self.img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); self.px = self.img.load()
    def p(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16 and c: self.px[x, y] = c
    def get(self, x, y):
        return self.px[x, y] if 0 <= x < 16 and 0 <= y < 16 else (0, 0, 0, 0)
    def rect(self, x0, y0, x1, y1, r, outline=True):
        """bevelled box: outline, light top/left edge, dark bottom/right edge, highlight corner"""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                edge = x in (x0, x1) or y in (y0, y1)
                if edge and outline: c = r[0]
                elif x == x0 + 1 or y == y0 + 1: c = r[3]
                elif x == x1 - 1 or y == y1 - 1: c = r[1]
                else: c = r[2]
                self.p(x, y, c)
        if outline and x1 - x0 > 2 and y1 - y0 > 2: self.p(x0 + 1, y0 + 1, r[4])
    def disc(self, cx, cy, rad, r, ring=False):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= rad:
                    if d > rad - 1.0: c = r[0]
                    else:
                        lit = ((cx - x - 0.5) + (cy - y - 0.5)) / (rad * 1.4)
                        c = r[3] if lit > 0.35 else (r[1] if lit < -0.35 else r[2])
                    self.p(x, y, c)
    def outline(self, c):
        body = {(x, y) for y in range(16) for x in range(16) if self.px[x, y][3]}
        for x, y in body:
            for dx, dy in ((0, 1), (1, 0), (0, -1), (-1, 0)):
                n = (x + dx, y + dy)
                if 0 <= n[0] < 16 and 0 <= n[1] < 16 and n not in body: self.px[n] = c
    def save(self, name):
        self.img.save(f'{out}/{name}.png'); return self.img

icons = {}

# Sandevistan: a chrome spine module, vertebrae with glowing discs between them
i = Icon()
for k, y in enumerate((1, 5, 9)):
    i.rect(4 if k else 5, y, 11 if k else 10, y + 2, STEEL)
for k, y in enumerate((4, 8, 12)):
    g = YEL if k % 2 == 0 else PURP
    for x in range(5, 11): i.p(x, y, g[3] if x in (6, 7, 8) else g[2])
    i.p(4, y, g[0]); i.p(11, y, g[0])
for y in (13, 14): i.p(7, y, DSTEEL[1]); i.p(8, y, DSTEEL[2])
i.p(7, 15, DSTEEL[0]); i.p(8, 15, DSTEEL[0])
for y in (2, 6, 10): i.p(3, y, CYAN[2]); i.p(12, y, CYAN[2])
i.outline((20, 22, 30, 255)); icons['sandevistan'] = i.save('sandevistan')

# Kerenzikov: a neural chip with gold pins
i = Icon()
for x in (5, 7, 9): i.p(x + 1, 2, GOLD[3]); i.p(x + 1, 3, GOLD[1]); i.p(x + 1, 12, GOLD[3]); i.p(x + 1, 13, GOLD[1])
for y in (5, 7, 9): i.p(2, y + 1, GOLD[3]); i.p(3, y + 1, GOLD[1]); i.p(12, y + 1, GOLD[3]); i.p(13, y + 1, GOLD[1])
i.rect(4, 4, 11, 11, DSTEEL)
i.rect(6, 6, 9, 9, BLUE)
i.p(7, 7, CYAN[4]); i.p(8, 8, CYAN[3]); i.p(7, 8, CYAN[2]); i.p(8, 7, CYAN[3])
i.p(5, 10, CYAN[2]); i.p(10, 5, CYAN[2])
icons['kerenzikov'] = i.save('kerenzikov')

# Mantis blades: a scythe-like magenta blade rising out of a steel forearm mount and hooking forward
i = Icon()
P0, P1, P2 = (10.0, 11.0), (14.5, 2.0), (2.0, 1.5)
for k in range(60):
    t = k / 59
    x = (1 - t) ** 2 * P0[0] + 2 * (1 - t) * t * P1[0] + t * t * P2[0]
    y = (1 - t) ** 2 * P0[1] + 2 * (1 - t) * t * P1[1] + t * t * P2[1]
    w = 2.2 * (1 - t) + 0.6
    for d in range(int(w) + 1):
        xi, yi = int(x - d * 0.5), int(y + d * 0.8)
        i.p(xi, yi, PINK[4] if d == 0 else (PINK[2] if d < w - 1 else PINK[1]))
i.rect(7, 10, 13, 15, STEEL)
i.p(9, 12, PINK[3]); i.p(10, 12, PINK[3]); i.p(11, 13, DSTEEL[1])
i.outline((40, 6, 34, 255)); icons['mantis_blades'] = i.save('mantis_blades')

# Gorilla arms: a heavy chrome fist — four knuckles, a thumb, orange vents on the wrist
i = Icon()
for k in range(4):
    i.rect(4 + k * 2 + (k > 0) * k, 2, 6 + k * 3, 6, STEEL)
i.rect(3, 5, 14, 11, STEEL)
i.rect(1, 7, 4, 11, STEEL)
for x in range(5, 13, 3): i.p(x, 4, STEEL[4])
i.rect(5, 11, 12, 15, DSTEEL)
for x in range(6, 12, 2): i.p(x, 13, ORANGE[3]); i.p(x, 12, ORANGE[2])
icons['gorilla_arms'] = i.save('gorilla_arms')

# Kiroshi optics: a lens with a red and a yellow ring and a glint
i = Icon()
i.disc(8, 8, 7.2, STEEL)
i.disc(8, 8, 5.6, RED)
i.disc(8, 8, 4.0, YEL)
i.disc(8, 8, 2.6, BLACK)
i.p(6, 6, (255, 255, 255, 255)); i.p(7, 6, (220, 220, 230, 255)); i.p(6, 7, (200, 200, 210, 255))
for a in range(0, 360, 45):
    x = int(round(8 + math.cos(math.radians(a)) * 6.4 - 0.5)); y = int(round(8 + math.sin(math.radians(a)) * 6.4 - 0.5))
    i.p(x, y, DSTEEL[1])
icons['kiroshi_optics'] = i.save('kiroshi_optics')

# Reinforced tendons: a hydraulic leg strut wrapped in a spring, cyan cable through it
i = Icon()
i.rect(5, 0, 10, 2, STEEL); i.rect(5, 13, 10, 15, STEEL)
for y in range(3, 13):
    i.p(7, y, CYAN[2]); i.p(8, y, CYAN[1])
for y in range(3, 13):
    ph = math.sin(y * 1.6)
    x0 = 5 if ph > 0 else 4; x1 = 11 if ph > 0 else 10
    i.p(x0, y, STEEL[3] if ph > 0 else STEEL[1]); i.p(x1, y, STEEL[2] if ph > 0 else STEEL[0])
    if abs(ph) > 0.6:
        for x in range(x0 + 1, x1): i.p(x, y, STEEL[3] if ph > 0 else STEEL[1])
i.outline((22, 24, 32, 255)); icons['reinforced_tendons'] = i.save('reinforced_tendons')

# Second heart: a red heart with chrome valves on top and a cyan circuit
i = Icon()
for y in range(16):
    for x in range(16):
        u = (x + 0.5 - 8) / 6.5; v = (7 - (y + 0.5)) / 6.5
        if (u * u + v * v - 0.6) ** 3 - u * u * v ** 3 * 1.2 <= 0 and y >= 3:
            lit = (8 - x) * 0.12 + (8 - y) * 0.12
            i.p(x, y, RED[4] if lit > 0.9 else (RED[3] if lit > 0.3 else (RED[2] if lit > -0.4 else RED[1])))
i.rect(4, 1, 6, 4, STEEL); i.rect(9, 0, 11, 4, STEEL)
for (x, y) in ((10, 7), (10, 8), (9, 9), (8, 10), (8, 11)): i.p(x, y, CYAN[3])
i.p(10, 6, CYAN[4])
i.outline((50, 6, 14, 255)); icons['second_heart'] = i.save('second_heart')

# Subdermal armor: interlocking hex plates with glowing seams
i = Icon()
for y in range(2, 14):
    for x in range(2, 14):
        i.p(x, y, CYAN[1] if (x + y) % 2 else CYAN[0])
cells = [(4, 4), (8, 4), (12, 4), (6, 8), (10, 8), (4, 12), (8, 12), (12, 12), (2, 8), (14, 8)]
for cx, cy in cells:
    for y in range(cy - 2, cy + 2):
        for x in range(cx - 2, cx + 2):
            if abs(x + 0.5 - cx) + abs(y + 0.5 - cy) * 0.8 <= 2.3 and 2 <= x <= 13 and 2 <= y <= 13:
                lit = (cx - x) + (cy - y)
                i.p(x, y, STEEL[3] if lit > 1 else (STEEL[2] if lit > -1 else STEEL[1]))
i.outline((22, 30, 40, 255)); icons['subdermal_armor'] = i.save('subdermal_armor')

# Neuroblockers: a foil blister with six domed blue-white capsules
i = Icon()
i.rect(1, 3, 14, 12, WHITE)
for cy in (6, 9):
    for cx in (3, 7, 11):
        i.p(cx, cy - 1, BLUE[3]); i.p(cx + 1, cy - 1, (255, 255, 255, 255))
        i.p(cx, cy, BLUE[2]); i.p(cx + 1, cy, WHITE[3])
        i.p(cx, cy + 1, BLUE[1]); i.p(cx + 1, cy + 1, WHITE[1])
for x in range(2, 14, 3): i.p(x, 11, WHITE[1])
icons['neuroblocker'] = i.save('neuroblocker')

# Sunflower seeds: an open paper bag, seeds spilling out
i = Icon()
i.rect(3, 4, 12, 14, BLUE)
for x in range(4, 12): i.p(x, 5, YEL[3] if x % 2 else YEL[2]); i.p(x, 6, YEL[1])
i.disc(8.0, 10.0, 2.6, YEL); i.p(8, 10, BROWN[1]); i.p(7, 9, BROWN[2])
for (x, y) in ((5, 3), (7, 2), (9, 3), (11, 2), (6, 3), (10, 3)):
    i.p(x, y, BLACK[2]); i.p(x, y - 1 if y > 2 else y, BLACK[0])
for (x, y) in ((1, 14), (13, 15), (14, 13)):
    i.p(x, y, BLACK[2]); i.p(x + 1, y, WHITE[2])
icons['sunflower_seeds'] = i.save('sunflower_seeds')

# Shawarma: a lavash roll on the diagonal, paper at the bottom, filling at the top
i = Icon()
for t in range(12):
    cx, cy = 3 + t, 13 - t
    for w in (-2, -1, 0, 1, 2):
        x, y = cx + w // 2 + (w % 2), cy + w // 2
        if t < 4: c = WHITE[3] if w < 0 else (WHITE[2] if w == 0 else WHITE[1])
        elif t < 10: c = TAN[3] if w < 0 else (TAN[2] if w <= 1 else TAN[1])
        else: c = None
        i.p(x, y, c)
    if 4 <= t < 10 and t % 2 == 0: i.p(cx, cy, BROWN[2])
for (x, y, c) in ((12, 2, GREEN[3]), (13, 3, GREEN[2]), (12, 4, RED[2]), (13, 2, BROWN[3]), (14, 3, RED[3]), (11, 3, WHITE[3]), (13, 4, BROWN[2]), (12, 3, BROWN[3]), (14, 2, GREEN[2])):
    i.p(x, y, c)
i.outline((52, 34, 20, 255)); icons['shawarma'] = i.save('shawarma')

# Instant noodles: a crinkled red packet with a yellow banner and a bowl of noodles
i = Icon()
i.rect(2, 2, 13, 13, RED)
for x in range(2, 14, 2): i.p(x, 1, RED[0]); i.p(x + 1, 14, RED[0])
for x in range(3, 13): i.p(x, 4, YEL[3]); i.p(x, 5, YEL[2])
for x in range(5, 11): i.p(x, 9, WHITE[3]); i.p(x, 10, WHITE[2])
for x in range(6, 10): i.p(x, 11, WHITE[1])
for (x, y) in ((5, 8), (6, 7), (7, 8), (8, 7), (9, 8), (10, 7)): i.p(x, y, YEL[3])
i.p(7, 6, (255, 255, 255, 140)); i.p(9, 6, (255, 255, 255, 110))
icons['instant_noodles'] = i.save('instant_noodles')

# Energy drink: a tall black can, cylinder shading, green lightning bolt, silver rim and tab
i = Icon()
for y in range(3, 15):
    for x in range(4, 12):
        t = (x - 4) / 7
        c = BLACK[3] if t < 0.2 else (BLACK[2] if t < 0.6 else BLACK[1])
        i.p(x, y, c)
for x in range(4, 12): i.p(x, 2, STEEL[3] if x < 8 else STEEL[2]); i.p(x, 15, STEEL[1])
i.p(7, 1, STEEL[2]); i.p(8, 1, STEEL[3])
for (x, y) in ((9, 4), (8, 5), (8, 6), (7, 7), (8, 7), (9, 7), (8, 8), (7, 9), (7, 10), (6, 11)):
    i.p(x, y, GREEN[4] if y < 8 else GREEN[3])
i.p(5, 4, (255, 255, 255, 120)); i.p(5, 5, (255, 255, 255, 80))
i.outline((10, 10, 14, 255)); icons['energy_drink'] = i.save('energy_drink')

# Chocolate Chapman: a slim dark-brown cigarette, gold band, glowing tip, a curl of smoke
i = Icon()
for t in range(12):
    x, y = 2 + t, 13 - t
    c = BROWN[2] if t > 4 else BROWN[3]
    if t in (3, 4): c = GOLD[3]
    if t < 3: c = ramp('#9a6a3a')[2]
    if t == 11: c = ORANGE[4]
    i.p(x, y, c); i.p(x + 1, y, (BROWN[1] if 4 < t < 11 else (GOLD[1] if t in (3, 4) else (ORANGE[2] if t == 11 else ramp('#9a6a3a')[1]))))
i.p(14, 1, ORANGE[3])
i.outline((30, 16, 8, 255))
for (x, y, a) in ((15, 0, 120), (13, 0, 90)): i.p(x, y, (210, 200, 190, a))
icons['chapman_chocolate'] = i.save('chapman_chocolate')

# Firecracker: a red tube with gold bands, a fuse and a spark
i = Icon()
for t in range(9):
    cx, cy = 3 + t, 12 - t
    for w in (-1, 0, 1):
        x, y = cx + (1 if w > 0 else 0), cy + (1 if w < 0 else 0) if w else cy
        c = (GOLD[3] if w <= 0 else GOLD[1]) if t in (1, 6) else (RED[3] if w < 0 else (RED[2] if w == 0 else RED[1]))
        i.p(x, y, c)
for (x, y) in ((12, 3), (13, 2)): i.p(x, y, TAN[1])
i.outline((40, 6, 8, 255))
for (x, y, c) in ((14, 1, YEL[4]), (13, 1, YEL[3]), (15, 1, YEL[2]), (14, 0, YEL[3]), (14, 2, ORANGE[3])): i.p(x, y, c)
icons['firecracker'] = i.save('firecracker')

# First-aid kit: a white case with a handle, a red cross, a latch
i = Icon()
i.rect(1, 4, 14, 14, WHITE)
for x in range(5, 11): i.p(x, 2, STEEL[1] if x in (5, 10) else STEEL[2])
i.p(5, 3, STEEL[1]); i.p(10, 3, STEEL[1])
for (x0, y0, x1, y1) in ((6, 6, 9, 12), (4, 8, 11, 10)):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            i.p(x, y, RED[3] if (x == x0 or y == y0) else (RED[1] if (x == x1 or y == y1) else RED[2]))
i.p(7, 13, STEEL[2]); i.p(8, 13, STEEL[2])
i.outline((60, 64, 70, 255)); icons['medkit'] = i.save('medkit')

prev = Image.new('RGBA', (len(icons) * 68, 64), (58, 60, 66, 255))
for k, im in enumerate(icons.values()):
    prev.alpha_composite(im.resize((64, 64), Image.NEAREST), (k * 68, 0))
prev.save(sys.argv[2])
print(len(icons), 'icons')
