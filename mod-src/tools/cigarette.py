"""16x16 texture of a single lit cigarette lying diagonally: filter, paper, ash, ember and a wisp of smoke."""
import sys
from PIL import Image
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
OUT = (58, 46, 40, 255)
def col(i):  # i = 0 (filter end) .. 11 (lit end)
    if i <= 3: return [(214, 140, 64, 255), (196, 120, 50, 255)][i % 2]       # cork filter
    if i == 4: return (232, 196, 120, 255)                                     # gold ring
    if i <= 9: return (246, 246, 242, 255) if i % 2 else (232, 232, 226, 255)  # paper
    if i == 10: return (150, 150, 150, 255)                                    # ash
    return (255, 110, 30, 255)                                                 # ember
# body: two pixels thick along the diagonal from (2,13) to (13,2)
for i in range(12):
    x, y = 1 + i, 13 - i
    c = col(i)
    img.putpixel((x, y), c)
    img.putpixel((x + 1, y), c if i < 11 else (255, 190, 60, 255))
    img.putpixel((x + 1, y + 1), tuple(int(v * 0.8) for v in c[:3]) + (255,) if i < 11 else (230, 80, 20, 255))
# outline
px = img.load()
filled = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
for x, y in filled:
    for dx, dy in ((0, -1), (-1, 0), (0, 1), (1, 0)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] == 0:
            px[nx, ny] = OUT
# glowing tip and smoke
for (x, y, a) in ((14, 0, 150), (15, 0, 90), (13, 0, 80)):
    px[x, y] = (200, 200, 205, a)
img.save(sys.argv[1])
big = img.resize((160, 160), Image.NEAREST); bg = Image.new('RGBA', (160, 160), (60, 60, 60, 255)); bg.alpha_composite(big); bg.save(sys.argv[2])
