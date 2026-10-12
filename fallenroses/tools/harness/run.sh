#!/bin/bash
# Dev only: build the test director, launch the client under Xvfb with the mod and collect screenshots.
# Usage: WORK=... GAME=<dir> tools/harness/run.sh <mod.jar> <scenario> [timeout-seconds]
set -e
W=${WORK:?set WORK}; G=${GAME:?set GAME}; H=$(cd "$(dirname "$0")" && pwd)
L=$W/forge/mc/libraries
ART=$L/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar
OTHERS=$(find $L -name '*.jar' ! -path '*forge/1.20.1*' ! -path '*net/minecraft/client*' ! -path '*ForgeAutoRenaming*' | tr '\n' ':')
MOJ="$W/moj/forge-client.jar:$W/moj/mc.jar:$W/moj/forge-universal.jar:$W/moj/geckolib.jar:$W/moj/playeranim.jar:$W/build/moj.jar"
B=$W/harness; rm -rf $B; mkdir -p $B/classes $B/merge
javac -nowarn -proc:none -encoding UTF-8 --release 17 -cp "$MOJ:${OTHERS}" -d $B/classes $(find $H/src -name '*.java')
(cd $B/classes && jar cf $B/moj.jar .)
LIBS=""; for j in $(echo "$MOJ:$OTHERS" | tr ':' ' '); do LIBS="$LIBS --lib $j"; done
java -jar $ART --input $B/moj.jar --output $B/srg.jar --map $W/moj2srg.tsrg $LIBS > /dev/null
cp -r $H/res/. $B/merge/ && (cd $B/merge && unzip -q $B/srg.jar && zip -qr $B/frtest.jar .)
mkdir -p $G/mods && rm -f $G/mods/*.jar && rm -rf $G/saves/frtest $G/screenshots
cp $W/deps/geckolib.jar $W/deps/playeranim.jar "$1" $B/frtest.jar $G/mods/
cd $G && FRTEST_SCENARIO=$2 timeout ${3:-900} xvfb-run -a -s "-screen 0 1280x720x24" python3 $H/launch.py $W $G > $G/client.log 2>&1 || true
grep -E "\[frtest\]|Exception|ERROR.*fallenroses" $G/client.log | grep -v "assets/objects" | head -40
ls $G/screenshots 2>/dev/null | head -100
