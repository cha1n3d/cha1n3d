"""16x16 textures: sunflower seeds bag, shawarma, instant noodles, energy drink can, chocolate Chapman."""
import sys
from PIL import Image
out = sys.argv[1]
def draw(rows, pal, name):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    img.save(f'{out}/{name}.png')
    return img
def h(c): c = c.lstrip('#'); return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
O = h('#2b1d14')
imgs = []
imgs.append(draw([
 '................',
 '.....OOOOOO.....',
 '....OYYYYYYO....',
 '....OYyYYyYO....',
 '...OYYYYYYYYO...',
 '...OBBBBBBBBO...',
 '...OBssBssBBO...',
 '...OBBBBBBBBO...',
 '..OBsBBBBsBBBO..',
 '..OBBBssBBBBBO..',
 '..OBBBBBBBsBBO..',
 '..OBsBBBBBBBBO..',
 '..OBBBBsBBBsBO..',
 '...OBBBBBBBBO...',
 '....OOOOOOOO....',
 '................'], {'O': O, 'Y': h('#f2c230'), 'y': h('#c98f12'), 'B': h('#2f5fbf'), 's': h('#1b1b1b')}, 'sunflower_seeds'))
imgs.append(draw([
 '................',
 '..........OOO...',
 '.........OgLrO..',
 '........OcgLLO..',
 '.......OrLcLgO..',
 '......OLgLrcO...',
 '.....OWWLcLO....',
 '....OWWWWgO.....',
 '...OWwWWWO......',
 '..OWWWwWO.......',
 '..OwWWWO........',
 '..OWWwO.........',
 '..OWWO..........',
 '...OO...........',
 '................',
 '................'], {'O': O, 'W': h('#efe0bf'), 'w': h('#d2bd92'), 'L': h('#b5733a'), 'g': h('#4fa83a'), 'r': h('#c8302a'), 'c': h('#f0f0e8')}, 'shawarma'))
imgs.append(draw([
 '................',
 '...OOOOOOOOOO...',
 '...ORRRRRRRRO...',
 '...ORYYYYYYRO...',
 '...ORYyyyyYRO...',
 '...ORYYYYYYRO...',
 '...ORRRRRRRRO...',
 '...ORrRRRRrRO...',
 '...ORRRwwRRRO...',
 '...ORRwwwwRRO...',
 '...ORRRwwRRRO...',
 '...ORRRRRRRRO...',
 '...ORrRRRRrRO...',
 '...OOOOOOOOOO...',
 '................',
 '................'], {'O': O, 'R': h('#d62b1e'), 'r': h('#a81d14'), 'Y': h('#f4d03f'), 'y': h('#c9a227'), 'w': h('#f6f0dc')}, 'instant_noodles'))
imgs.append(draw([
 '................',
 '.....OOOOOO.....',
 '.....OSSsSO.....',
 '....OGGGGGGO....',
 '....OKKKKKKO....',
 '....OKGKKKKO....',
 '....OKGGKKKO....',
 '....OKKGGKKO....',
 '....OKKKGGKO....',
 '....OKKKKGKO....',
 '....OKKKKKKO....',
 '....OGGGGGGO....',
 '....OSSSSSSO....',
 '.....OOOOOO.....',
 '................',
 '................'], {'O': h('#101010'), 'S': h('#bfc5c9'), 's': h('#8a9196'), 'K': h('#1c1c22'), 'G': h('#7fe03a')}, 'energy_drink'))
# chocolate Chapman: a slim dark-brown cigarette with a gold band, lying diagonally
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = img.load()
for i in range(12):
    x, y = 2 + i, 13 - i
    c = h('#d8b04a') if i in (3, 4) else (h('#4a2a17') if i < 11 else h('#ff7a2a'))
    if i < 3: c = h('#5c3820')
    px[x, y] = c
    px[x + 1, y] = c if i < 11 else h('#ffc060')
body = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
for x, y in body:
    for dx, dy in ((0, 1), (1, 0), (0, -1), (-1, 0)):
        n = (x + dx, y + dy)
        if 0 <= n[0] < 16 and 0 <= n[1] < 16 and n not in body:
            px[n] = h('#24140b')
px[14, 0] = (190, 170, 150, 150); px[15, 1] = (190, 170, 150, 100)
img.save(f'{out}/chapman_chocolate.png'); imgs.append(img)
prev = Image.new('RGBA', (5 * 106, 96), (60, 60, 60, 255))
for i, im in enumerate(imgs):
    b = im.resize((96, 96), Image.NEAREST); prev.alpha_composite(b, (i * 106, 0))
prev.save(sys.argv[2])
