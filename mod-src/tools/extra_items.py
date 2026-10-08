"""16x16 textures: firecracker and medkit."""
import sys
from PIL import Image
out = sys.argv[1]
def h(c): c = c.lstrip('#'); return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def make(name, rows, pal, outline='#1a1010'):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal: px[x, y] = h(pal[ch])
    body = {(x, y) for y in range(16) for x in range(16) if px[x, y][3]}
    for x, y in body:
        for dx, dy in ((0, 1), (1, 0), (0, -1), (-1, 0)):
            n = (x + dx, y + dy)
            if 0 <= n[0] < 16 and 0 <= n[1] < 16 and n not in body: px[n] = h(outline)
    img.save(f'{out}/{name}.png'); return img
a = make('firecracker', [
 '................', '...........y....', '..........yo....', '.........ws.....', '........RRw.....', '.......RRRR.....',
 '......RRYRR.....', '.....RRRRRR.....', '....RRYRRR......', '...RRRRRR.......', '...RRYRR........', '...RRRR.........',
 '....RR..........', '................', '................', '................'], {'R': '#d4202a', 'Y': '#f2d23a', 'w': '#d8c8a8', 's': '#8a7a60', 'y': '#ffe070', 'o': '#ff8a20'})
b = make('medkit', [
 '................', '......dddd......', '......d..d......', '..WWWWWWWWWWWW..', '..WWWWWWWWWWWW..', '..WWWWWRRWWWWW..',
 '..WWWWWRRWWWWW..', '..WWWRRRRRRWWW..', '..WWWRRRRRRWWW..', '..WWWWWRRWWWWW..', '..WWWWWRRWWWWW..', '..WWWWWWWWWWWW..',
 '..gggggggggggg..', '................', '................', '................'], {'W': '#f2f2ee', 'R': '#d82020', 'g': '#c0c0b8', 'd': '#6a6a6a'}, outline='#404040')
prev = Image.new('RGBA', (148, 64), (60, 60, 60, 255)); prev.alpha_composite(a.resize((64, 64), Image.NEAREST), (0, 0)); prev.alpha_composite(b.resize((64, 64), Image.NEAREST), (84, 0)); prev.save(sys.argv[2])
