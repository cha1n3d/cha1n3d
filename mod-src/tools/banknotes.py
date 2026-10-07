"""16x16 item textures for the 100 and 1000 ruble banknotes (two notes fanned, the top one with a medallion)."""
import sys
from PIL import Image
out = sys.argv[1]

def hexc(h, a=255):
    h = h.lstrip('#'); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (a,)

def note(img, x0, y0, body, dark, light, ink, medallion=True):
    w, h = 14, 8
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or y in (0, h - 1)
            c = dark if edge else body
            if not edge and (x + y) % 5 == 0:
                c = light  # guilloche stripes
            img.putpixel((x0 + x, y0 + y), c)
    if medallion:
        for (x, y) in [(5, 2), (6, 2), (7, 2), (4, 3), (8, 3), (4, 4), (8, 4), (5, 5), (6, 5), (7, 5)]:
            img.putpixel((x0 + x, y0 + y), ink)
        for (x, y) in [(5, 3), (6, 3), (7, 3), (5, 4), (6, 4), (7, 4)]:
            img.putpixel((x0 + x, y0 + y), light)
        for (x, y) in [(10, 2), (11, 2), (10, 5), (11, 5), (2, 2), (2, 5)]:  # corner numerals
            img.putpixel((x0 + x, y0 + y), ink)

for name, body, dark, light, ink in [
    ('banknote_100', '#c9a46a', '#7a5a2e', '#e3c993', '#5b3f1c'),
    ('banknote_1000', '#6fb3a8', '#2f6e66', '#a7d8cf', '#1d4a44'),
]:
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    b, d, l, k = hexc(body), hexc(dark), hexc(light), hexc(ink)
    shade = tuple(int(v * 0.8) for v in b[:3]) + (255,)
    note(img, 1, 3, shade, d, b, d, medallion=False)
    note(img, 1, 6, b, d, l, k)
    img.save(f'{out}/{name}.png')
print('ok')
