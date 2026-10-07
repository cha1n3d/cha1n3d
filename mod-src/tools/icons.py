import sys, math
from PIL import Image, ImageDraw
out = sys.argv[1]
OUT = (24, 16, 20, 255)

def outline(im, col=OUT):
    px = im.load(); w, h = im.size
    o = im.copy(); op = o.load()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    nx, ny = x+dx, y+dy
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny][3] > 0 and px[nx, ny][:3] != col[:3]:
                        op[x, y] = col; break
    return o

def new(n=32):
    return Image.new('RGBA', (n, n), (0, 0, 0, 0))

# Piercing Blood: a red spear-beam crossing the icon with droplets
im = new(); d = ImageDraw.Draw(im)
for i in range(4):
    d.line([(4+i, 27), (27, 4+i)], fill=[(110,6,12,255),(170,14,22,255),(220,40,40,255),(150,10,18,255)][i], width=1)
d.polygon([(22, 3), (29, 2), (28, 9)], fill=(235, 60, 55, 255))
d.line([(9, 25), (25, 9)], fill=(255, 140, 130, 255))
for cx, cy in ((7, 15), (12, 9), (19, 26), (25, 20)):
    d.ellipse([cx-1, cy-1, cx+1, cy+2], fill=(180, 12, 20, 255))
im = outline(im); im.save(f'{out}/gui/ability/piercing_blood.png')

# Turret: grey box base, dark barrel and a pistol muzzle flash
im = new(); d = ImageDraw.Draw(im)
d.rectangle([7, 22, 24, 27], fill=(120, 124, 132, 255)); d.rectangle([7, 22, 24, 23], fill=(170, 174, 182, 255))
d.rectangle([13, 16, 18, 21], fill=(90, 94, 100, 255))
d.rectangle([9, 9, 20, 16], fill=(150, 112, 70, 255)); d.rectangle([9, 9, 20, 10], fill=(190, 150, 100, 255))
d.rectangle([20, 11, 28, 13], fill=(60, 62, 70, 255))
d.polygon([(28, 12), (31, 9), (31, 15)], fill=(255, 210, 80, 255))
d.point([(12, 12), (15, 12)], fill=(60, 40, 30, 255))
im = outline(im); im.save(f'{out}/gui/ability/turret.png')

# Swamp pack: three frog heads
im = new(); d = ImageDraw.Draw(im)
def frog(cx, cy, s, col):
    d.ellipse([cx-s, cy-s*0.6, cx+s, cy+s*0.7], fill=col)
    for ex in (cx - s*0.55, cx + s*0.55):
        d.ellipse([ex-s*0.32, cy-s*0.95, ex+s*0.32, cy-s*0.35], fill=(240, 240, 230, 255))
        d.point([(round(ex), round(cy - s*0.65))], fill=(20, 20, 20, 255))
    d.line([(cx - s*0.5, cy + s*0.2), (cx + s*0.5, cy + s*0.2)], fill=(40, 90, 40, 255))
frog(9, 21, 6, (110, 170, 70, 255)); frog(23, 21, 6, (200, 140, 60, 255)); frog(16, 13, 7, (90, 160, 90, 255))
im = outline(im); im.save(f'{out}/gui/ability/swamp_pack.png')

# Fire breath: a bottle on the left spraying a cone of flame
im = new(); d = ImageDraw.Draw(im)
d.polygon([(10, 16), (31, 5), (31, 27)], fill=(240, 120, 30, 255))
d.polygon([(10, 16), (27, 9), (27, 23)], fill=(250, 190, 50, 255))
d.polygon([(10, 16), (21, 13), (21, 19)], fill=(255, 240, 160, 255))
d.rectangle([2, 12, 8, 24], fill=(110, 160, 90, 255)); d.rectangle([3, 9, 6, 12], fill=(110, 160, 90, 255))
d.rectangle([3, 15, 7, 19], fill=(230, 220, 180, 255))
im = outline(im); im.save(f'{out}/gui/ability/fire_breath.png')

# Death Painting item: a dark, veined womb with a red core
im = new(16); d = ImageDraw.Draw(im)
d.ellipse([3, 2, 12, 13], fill=(88, 32, 44, 255))
d.ellipse([4, 3, 10, 9], fill=(120, 46, 58, 255))
d.ellipse([6, 6, 10, 10], fill=(170, 20, 30, 255))
d.point([(7, 7)], fill=(240, 80, 80, 255))
d.line([(4, 10), (7, 12)], fill=(60, 16, 26, 255)); d.line([(11, 5), (12, 8)], fill=(60, 16, 26, 255))
d.line([(8, 13), (8, 15)], fill=(150, 20, 28, 255))
im = outline(im); im.save(f'{out}/item/death_painting.png')

prev = Image.new('RGBA', (5*136, 136), (40, 40, 40, 255))
for i, n in enumerate(['piercing_blood', 'turret', 'swamp_pack', 'fire_breath']):
    prev.paste(Image.open(f'{out}/gui/ability/{n}.png').resize((128, 128), Image.NEAREST), (i*136, 4))
prev.paste(Image.open(f'{out}/item/death_painting.png').resize((128, 128), Image.NEAREST), (4*136, 4))
prev.save(sys.argv[2])
