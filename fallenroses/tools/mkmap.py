import sys,re
moj, tsrg, out = sys.argv[1:4]
# mojmap: named -> obf
cls_n2o = {}; cls_o2n = {}
fields = {}   # (obfcls, obfname) -> named
methods = []  # (namedcls, obfcls, ret, name, params, obfname)
cur = None
for line in open(moj, encoding='utf-8'):
    if line.startswith('#'): continue
    if not line.startswith(' '):
        m = re.match(r'(\S+) -> (\S+):', line)
        if m: cur = (m.group(1), m.group(2)); cls_n2o[cur[0]] = cur[1]; cls_o2n[cur[1]] = cur[0]
        continue
    s = line.strip()
    m = re.match(r'(?:\d+:\d+:)?(\S+) (\S+)\((.*)\)(?::\d+:\d+)? -> (\S+)', s)
    if m:
        methods.append((cur[0], cur[1], m.group(1), m.group(2), m.group(3), m.group(4))); continue
    m = re.match(r'(\S+) (\S+) -> (\S+)', s)
    if m: fields[(cur[1], m.group(3))] = (cur[0], m.group(2), m.group(1))
prim = {'int':'I','long':'J','float':'F','double':'D','boolean':'Z','byte':'B','char':'C','short':'S','void':'V'}
def desc(t):
    arr = ''
    while t.endswith('[]'): arr += '['; t = t[:-2]
    if t in prim: return arr + prim[t]
    return arr + 'L' + cls_n2o.get(t, t).replace('.', '/') + ';'
# tsrg: obf -> srg
srg_f = {}; srg_m = {}
c = None
for line in open(tsrg, encoding='utf-8'):
    if line.startswith('tsrg2'): continue
    if not line.startswith('\t'):
        c = line.split()[0]; continue
    if line.startswith('\t\t'): continue
    p = line.split()
    if len(p) == 2: srg_f[(c, p[0])] = p[1]
    elif len(p) == 3: srg_m[(c, p[0], p[1])] = p[2]
with open(out, 'w') as o:
    for (oc, of), (nc, nf, t) in fields.items():
        s = srg_f.get((oc.replace('.', '/'), of))
        if s: o.write(f'F {nc} {nf} {s} {t}\n')
    for nc, oc, ret, name, params, on in methods:
        ps = [x for x in params.split(',') if x]
        d = '(' + ''.join(desc(x) for x in ps) + ')' + desc(ret)
        s = srg_m.get((oc.replace('.', '/'), on, d))
        if s and s.startswith('m_'): o.write(f'M {nc} {name}({params}) {s} {ret}\n')
