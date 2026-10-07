import sys
names, out = sys.argv[1:3]
prim = {'int':'I','long':'J','float':'F','double':'D','boolean':'Z','byte':'B','char':'C','short':'S','void':'V'}
def desc(t):
    arr = ''
    while t.endswith('[]'): arr += '['; t = t[:-2]
    return arr + (prim[t] if t in prim else 'L' + t.replace('.', '/') + ';')
by = {}
for line in open(names):
    k, cls, name, srg, typ = line.rstrip('\n').split(' ', 4)
    by.setdefault(cls, []).append((k, name, srg, typ))
with open(out, 'w') as o:
    for cls, ms in by.items():
        c = cls.replace('.', '/')
        o.write(f'{c} {c}\n')
        for k, name, srg, typ in ms:
            if k == 'F':
                o.write(f'\t{name} {srg}\n')
            else:
                n, params = name.split('(', 1); params = params[:-1]
                d = '(' + ''.join(desc(x) for x in params.split(',') if x) + ')' + desc(typ)
                o.write(f'\t{n} {d} {srg}\n')
