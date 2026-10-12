#!/bin/bash
# One-time toolchain: Forge 1.20.1 + vanilla libraries, Mojang<->SRG map, Mojang-named jars of
# Minecraft/Forge/GeckoLib/PlayerAnimator for compiling, Vineflower for decompiling.
set -e
W=${WORK:?set WORK}; T=$(cd "$(dirname "$0")" && pwd)
mkdir -p $W/forge/mc $W/tools $W/deps $W/moj
cd $W/forge
[ -f installer.jar ] || curl -sSfL -o installer.jar https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.9/forge-1.20.1-47.4.9-installer.jar
echo '{"profiles":{}}' > mc/launcher_profiles.json
[ -d mc/versions/1.20.1-forge-47.4.9 ] || java -jar installer.jar --installClient mc > install.log
python3 - <<'PY'
import json, urllib.request, os
os.chdir('mc')
m = json.load(urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
u = [v['url'] for v in m['versions'] if v['id'] == '1.20.1'][0]
d = json.loads(urllib.request.urlopen(u).read())
for lib in d['libraries']:
    a = lib.get('downloads', {}).get('artifact')
    if not a or 'natives' in lib['name']: continue
    p = 'libraries/' + a['path']
    if not os.path.exists(p):
        os.makedirs(os.path.dirname(p), exist_ok=True); urllib.request.urlretrieve(a['url'], p)
PY
cd $W
get() { [ -f "$2" ] || curl -sSfL -o "$2" "$1"; }
get https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/software/bernie/geckolib/geckolib-forge-1.20.1/4.4.9/geckolib-forge-1.20.1-4.4.9.jar deps/geckolib.jar
get "https://maven.kosmx.dev/dev/kosmx/player-anim/player-animation-lib-forge/1.0.2-rc1%2B1.20/player-animation-lib-forge-1.0.2-rc1%2B1.20.jar" deps/playeranim.jar
get https://repo1.maven.org/maven2/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar tools/vineflower-1.10.1.jar
get https://repo1.maven.org/maven2/org/ow2/asm/asm/9.7/asm-9.7.jar tools/asm-9.7.jar
get https://repo1.maven.org/maven2/org/jetbrains/annotations/24.0.1/annotations-24.0.1.jar tools/annotations-24.0.1.jar
L=$W/forge/mc/libraries; MC=$L/net/minecraft/client/1.20.1-20230612.114412; MCP=$L/de/oceanlabs/mcp/mcp_config/1.20.1-20230612.114412
python3 $T/mkmap.py $MC/client-1.20.1-20230612.114412-mappings.txt $MCP/mcp_config-1.20.1-20230612.114412-mappings-merged.txt names.txt
python3 $T/mktsrg.py names.txt moj2srg.tsrg
ART=$L/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar
FC=$L/net/minecraftforge/forge/1.20.1-47.4.9/forge-1.20.1-47.4.9-client.jar
FU=$L/net/minecraftforge/forge/1.20.1-47.4.9/forge-1.20.1-47.4.9-universal.jar
LIBS="--lib $MC/client-1.20.1-20230612.114412-srg.jar --lib $FC --lib $FU --lib deps/geckolib.jar --lib deps/playeranim.jar"
for j in $(find $L -name '*.jar' ! -path '*forge/1.20.1*' ! -path '*net/minecraft/client*' ! -path '*ForgeAutoRenaming*'); do LIBS="$LIBS --lib $j"; done
rev() { java -jar $ART --input "$1" --output "$2" --map moj2srg.tsrg --reverse $LIBS > /dev/null; }
rev $MC/client-1.20.1-20230612.114412-srg.jar moj/mc.jar
rev $FC moj/forge-client.jar
rev $FU moj/forge-universal.jar
rev deps/geckolib.jar moj/geckolib.jar
rev deps/playeranim.jar moj/playeranim.jar
mkdir -p tools/cls && javac -nowarn -d tools/cls -cp tools/asm-9.7.jar $T/RefCheck.java $T/MemberDiff.java
echo "toolchain ready in $W"
