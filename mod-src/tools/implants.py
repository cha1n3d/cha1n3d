"""16x16 textures for the cyberware implants and the neuroblocker pills (pixel maps, outlined automatically)."""
import sys
from PIL import Image
out = sys.argv[1]
def h(c): c = c.lstrip('#'); return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def make(name, rows, pal, outline='#101418'):
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
M = {'m': '#9aa3ad', 'M': '#c9d0d8', 'd': '#5b636c'}
imgs = [
 make('sandevistan', [
  '................', '.......MM.......', '......mYYm......', '.......dd.......', '......mPPm......', '.......dd.......',
  '......mYYm......', '.......dd.......', '......mPPm......', '.......dd.......', '......mYYm......', '.......dd.......',
  '......mPPm......', '.......MM.......', '................', '................'], {**M, 'Y': '#f5e33a', 'P': '#b04cff'}),
 make('kerenzikov', [
  '................', '................', '....dddddddd....', '...dMMMMMMMMd...', '...dMBBMMBBMd...', '...dMBccccBMd...',
  '...dMMcBBcMMd...', '...dMMcBBcMMd...', '...dMBccccBMd...', '...dMBBMMBBMd...', '...dMMMMMMMMd...', '....dddddddd....',
  '....m.m..m.m....', '....m.m..m.m....', '................', '................'], {**M, 'B': '#2a6cff', 'c': '#7fd8ff'}),
 make('mantis_blades', [
  '................', '..PP............', '...PPp..........', '....PPp.........', '.....PPp........', '......PPp.......',
  '.......PPp......', '.......PPp......', '......PPp.......', '.....mmm........', '....mMMMm.......', '....mMdMm.......',
  '.....mMm........', '......m.........', '................', '................'], {**M, 'P': '#ff4fd8', 'p': '#ffb0f0'}),
 make('gorilla_arms', [
  '................', '................', '...MMMMMMMMM....', '..MmMmMmMmMmM...', '..MmMmMmMmMmM...', '..MMMMMMMMMMM...',
  '..mmmmmmmmmmmM..', '..mOOmmmmmmmmM..', '..mmmmmmmmmmm...', '...mdddddddm....', '....dddddddd....', '....dMMMMMMd....',
  '....dMOOOOMd....', '....dddddddd....', '................', '................'], {**M, 'O': '#ff8a1e'}),
 make('kiroshi_optics', [
  '................', '................', '.....mmmmmm.....', '....mMMMMMMm....', '...mMRRRRRRMm...', '..mMRYYYYYYRMm..',
  '..mMRYKKKKYRMm..', '..mMRYKwKKYRMm..', '..mMRYKKKKYRMm..', '..mMRYYYYYYRMm..', '...mMRRRRRRMm...', '....mMMMMMMm....',
  '.....mmmmmm.....', '................', '................', '................'], {**M, 'R': '#e02a2a', 'Y': '#ffd23a', 'K': '#141414', 'w': '#ffffff'}),
 make('reinforced_tendons', [
  '................', '......MMMM......', '......mddm......', '.....cMMMMc.....', '......mMMm......', '.....cMMMMc.....',
  '......mMMm......', '.....cMMMMc.....', '......mMMm......', '.....cMMMMc.....', '......mMMm......', '.....cMMMMc.....',
  '......mddm......', '......MMMM......', '................', '................'], {**M, 'c': '#39e0ff'}),
 make('second_heart', [
  '................', '................', '...MMM...MMM....', '..MRRRM.MRRRM...', '.MRRRRRMRRRRRM..', '.MRrRRRRRRRRRM..',
  '.MRRRRRRRRcRRM..', '..MRRRRRRccRM...', '...MRRRRccRM....', '....MRRRRRM.....', '.....MRRRM......', '......MRM.......',
  '.......M........', '................', '................', '................'], {**M, 'R': '#b81c2a', 'r': '#ff6070', 'c': '#3ce0ff'}),
 make('subdermal_armor', [
  '................', '................', '...MmMmMmMmM....', '..MmdmdmdmdmM...', '..mdMmMmMmMdm...', '..MmdmdmdmdmM...',
  '..mdMmMmMmMdm...', '..MmdmdmdmdmM...', '..mdMmMmMmMdm...', '..MmdmdmdmdmM...', '..mdMmMmMmMdm...', '...MmMmMmMmM....',
  '................', '................', '................', '................'], {**M}),
 make('neuroblocker', [
  '................', '................', '..SSSSSSSSSSSS..', '..SwwSwwSwwSwS..', '..SwBSwBSwBSwS..', '..SSSSSSSSSSSS..',
  '..SwwSwwSwwSwS..', '..SwBSwBSwBSwS..', '..SSSSSSSSSSSS..', '..SwwSwwSSSSSS..', '..SwBSwBSSSSSS..', '..SSSSSSSSSSSS..',
  '................', '................', '................', '................'], {'S': '#c8ced6', 'w': '#ffffff', 'B': '#3a7bff'}, outline='#6a7078'),
]
prev = Image.new('RGBA', (len(imgs) * 74, 64), (60, 60, 60, 255))
for i, im in enumerate(imgs):
    prev.alpha_composite(im.resize((64, 64), Image.NEAREST), (i * 74, 0))
prev.save(sys.argv[2])
