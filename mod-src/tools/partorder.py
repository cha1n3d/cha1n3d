"""Every BoxSkin part must come after its parent (ArthasModel.build adds them in order).
usage: partorder.py <src dir>... — checks each PARTS array and each humanoid(...) extra list."""
import re, sys, glob, os
bad = 0; checked = 0
files = {}
for d in sys.argv[1:]:
    for f in glob.glob(os.path.join(d, '**', '*Skin*.java'), recursive=True):
        files[os.path.basename(f) + ':' + f.split('vaz2109/')[-1]] = f
seen_paths = {}
for key, f in files.items():
    rel = f.split('com/bobux/vaz2109/')[-1]
    if rel in seen_paths and 'work/src' not in f:
        continue
    seen_paths[rel] = f
for rel, f in seen_paths.items():
    s = open(f).read()
    for m in re.finditer(r'PARTS\s*=\s*(new BoxSkin\.Part\[\]\{|PeopleSkins\.humanoid\(|humanoid\()', s):
        humanoid = 'humanoid' in m.group(1)
        i = m.end(); depth = 1; j = i
        while depth and j < len(s):
            c = s[j]
            depth += c in '({'
            depth -= c in ')}'
            j += 1
        names = re.findall(r'Part\(\s*"([a-z_0-9]+)",\s*"([a-z_0-9]*)"', s[i:j])
        defined = {'head', 'hat', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg'} if humanoid else set()
        for name, parent in names:
            checked += 1
            if parent and parent not in defined:
                bad += 1
                print(f'BROKEN {rel}: part "{name}" comes before its parent "{parent}"')
            defined.add(name)
print(f'{checked} parts checked, {bad} out of order')
sys.exit(1 if bad else 0)
