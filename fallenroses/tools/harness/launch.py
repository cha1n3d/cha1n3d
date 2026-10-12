"""Dev only: builds the Forge 1.20.1 client command line from the version JSONs and runs it.
Usage: python3 launch.py <WORK> <game dir> [extra game args]"""
import json, os, sys, subprocess
W = sys.argv[1]; game = sys.argv[2]
mc = os.path.join(W, 'forge', 'mc'); lib = os.path.join(mc, 'libraries')
forge = json.load(open(f'{mc}/versions/1.20.1-forge-47.4.9/1.20.1-forge-47.4.9.json'))
van = json.load(open(f'{mc}/versions/1.20.1/1.20.1.json'))
def allowed(l):
    rules = l.get('rules')
    if not rules: return True
    ok = False
    for r in rules:
        if r['action'] == 'allow' and ('os' not in r or r['os'].get('name') == 'linux'): ok = True
        if r['action'] == 'disallow' and r.get('os', {}).get('name') == 'linux': ok = False
    return ok
cp = []
for l in forge['libraries'] + van['libraries']:
    a = l.get('downloads', {}).get('artifact')
    if a and allowed(l):
        p = os.path.join(lib, a['path'])
        if os.path.exists(p) and p not in cp: cp.append(p)
        elif not os.path.exists(p): print('missing', p, file=sys.stderr)
# the vanilla jar must stay off the classpath (Forge builds minecraft from the patched libraries)
sub = {'${library_directory}': lib, '${classpath_separator}': ':', '${version_name}': '1.20.1-forge-47.4.9'}
def s(x):
    for k, v in sub.items(): x = x.replace(k, v)
    return x
jvm = [s(a) for a in forge['arguments']['jvm']]
cmd = ['java', '-Xmx3G', '-Dfrtest.scenario=' + os.environ.get('FRTEST_SCENARIO', 'domains'), '-Djava.library.path=' + os.path.join(game, 'natives'), *jvm, '-cp', ':'.join(cp), forge['mainClass'],
       *forge['arguments']['game'], '--username', 'Tester', '--version', '1.20.1-forge-47.4.9', '--gameDir', game,
       '--assetsDir', os.path.join(mc, 'assets'), '--assetIndex', '5', '--uuid', '00000000000000000000000000000001',
       '--accessToken', '0', '--userType', 'legacy', '--width', '1280', '--height', '720', *sys.argv[3:]]
os.makedirs(game, exist_ok=True)
env = dict(os.environ)
env.pop('JAVA_TOOL_OPTIONS', None)
sys.exit(subprocess.call(cmd, cwd=game, env=env))
