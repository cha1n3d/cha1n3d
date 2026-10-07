"""16x16 item textures: six drinks, the cigarette pack and Yuji's red gloves."""
import sys
from PIL import Image
out = sys.argv[1]

def c(h, a=255):
    h = h.lstrip('#'); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)

def shade(col, f):
    return tuple(max(0, min(255, int(v * f))) for v in col[:3]) + (col[3],)

def bottle(glass, liquid, label, cap, *, body_w=6, body_top=6, neck=(7, 9), level=7, label_rows=(9, 12), foil=None, flat=False):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    x0 = 8 - body_w // 2
    x1 = x0 + body_w - 1
    # body
    for y in range(body_top, 15):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), liquid if y >= level else glass)
    # shoulders
    if not flat:
        for x in range(x0 + 1, x1):
            img.putpixel((x, body_top - 1), glass)
    # neck
    top = 1
    for y in range(top + 1, body_top - 1):
        for x in range(neck[0], neck[1]):
            img.putpixel((x, y), foil if foil and y < body_top - 2 else glass)
    for x in range(neck[0], neck[1]):
        img.putpixel((x, top), cap)
        img.putpixel((x, top + 1), cap)
    # label
    for y in range(label_rows[0], label_rows[1] + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), label if y not in (label_rows[0], label_rows[1]) else shade(label, 0.75))
    # outline + highlight
    px = img.load()
    edge = []
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] > 0 and px[x + dx, y + dy] != (0, 0, 0, 0) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for x, y in edge:
        px[x, y] = shade(glass, 0.35)[:3] + (255,)
    for y in range(body_top, 14):
        if y not in range(label_rows[0], label_rows[1] + 1):
            px[x0 + 1, y] = shade(px[x0 + 1, y], 1.45)
    return img

drinks = {
    'kvass': bottle(c('#5a3a1a'), c('#6b3f12'), c('#e08a1e'), c('#c43c1c'), body_w=6, body_top=5, level=5, label_rows=(8, 11)),
    'wine': bottle(c('#1f4d2a'), c('#3a0a14'), c('#e9dcc0'), c('#8a5a2b'), label_rows=(10, 12)),
    'champagne': bottle(c('#173d22'), c('#1f4a28'), c('#f1e6c8'), c('#d9b13b'), foil=c('#d9b13b'), label_rows=(10, 12)),
    'vodka': bottle(c('#cfe6ef', 255), c('#e4f2f7'), c('#f4f4f4'), c('#c8202a'), label_rows=(8, 11)),
    'cognac': bottle(c('#a0601c'), c('#8a4a10'), c('#e3c25a'), c('#3b2412'), body_w=8, body_top=7, level=8, label_rows=(9, 12), flat=True),
    'moonshine': bottle(c('#d8e6e0'), c('#efe6b8'), c('#d8e6e0'), c('#9a9a9a'), body_w=8, body_top=5, neck=(5, 11), level=7, label_rows=(13, 13), flat=True),
}
for k, im in drinks.items():
    im.save(f'{out}/{k}.png')

# cigarette pack: white box, red lid, three cigarettes sticking out
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = img.load()
for y in range(5, 15):
    for x in range(4, 12):
        px[x, y] = c('#f2f2f2') if y >= 8 else c('#c8202a')
for x in range(4, 12):
    px[x, 11] = c('#c8202a'); px[x, 12] = c('#c8202a')
for i, (x, h) in enumerate(((5, 2), (7, 3), (9, 1))):
    for y in range(5 - h, 5):
        px[x, y] = c('#f6f6f6'); px[x + 1, y] = c('#e0e0e0')
    px[x, 5 - h] = c('#d98a3a'); px[x + 1, 5 - h] = c('#c27530')
for y in range(16):
    for x in range(16):
        if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = c('#3a3a3a')
img.save(f'{out}/cigarettes.png')

# red glove: a clenched fist, knuckles up, white wrist strap
G = ['................',
     '................',
     '.....XXXXXX.....',
     '....XRRrRRrX....',
     '...XRRRRRRRRX...',
     '..XRrRRrRRrRRX..',
     '..XRRRRRRRRRRX..',
     '..XRRRRRRRRRdX..',
     '..XdRRRRRRRddX..',
     '...XRRRRRRRdX...',
     '...XdRRRRRddX...',
     '....XWWWWWWX....',
     '....XwWWWWwX....',
     '....XddddddX....',
     '.....XXXXXX.....',
     '................']
pal = {'X': c('#2a0506'), 'R': c('#c4161c'), 'r': c('#e8474c'), 'd': c('#7d0a0e'), 'W': c('#efefef'), 'w': c('#c9c9c9')}
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for y, row in enumerate(G):
    for x, ch in enumerate(row):
        if ch in pal:
            img.putpixel((x, y), pal[ch])
img.save(f'{out}/red_gloves.png')

prev = Image.new('RGBA', (16 * 8 * 6 + 70, 96), (60, 60, 60, 255))
for i, k in enumerate(list(drinks) + ['cigarettes', 'red_gloves']):
    im = Image.open(f'{out}/{k}.png').resize((96, 96), Image.NEAREST)
    prev.paste(im, (i * 106, 0), im)
prev.save(sys.argv[2])
print('ok')
