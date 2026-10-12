#!/bin/bash
# Build the mod from src/ (Mojang names) + res/: javac -> ForgeAutoRenamingTool to SRG -> jar.
# Usage: WORK=<dir prepared by setup.sh> tools/build.sh out.jar
set -e
W=${WORK:?set WORK}; D=$(cd "$(dirname "$0")/.." && pwd); OUT=$(realpath -m "$1")
L=$W/forge/mc/libraries
ART=$L/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar
OTHERS=$(find $L -name '*.jar' ! -path '*forge/1.20.1*' ! -path '*net/minecraft/client*' ! -path '*ForgeAutoRenaming*' | tr '\n' ':')
MOJ="$W/moj/forge-client.jar:$W/moj/mc.jar:$W/moj/forge-universal.jar:$W/moj/geckolib.jar:$W/moj/playeranim.jar"
B=$W/build; rm -rf $B; mkdir -p $B/classes $B/merge
javac -nowarn -proc:none -encoding UTF-8 --release 17 -g -cp "$MOJ:${OTHERS}$W/tools/annotations-24.0.1.jar" -d $B/classes $(find $D/src -name '*.java')
(cd $B/classes && jar cf $B/moj.jar .)
LIBS=""; for j in $(echo "$MOJ:$OTHERS" | tr ':' ' '); do LIBS="$LIBS --lib $j"; done
java -jar $ART --input $B/moj.jar --output $B/srg.jar --map $W/moj2srg.tsrg $LIBS --log $B/art.log > /dev/null
cp -r $D/res/. $B/merge/
(cd $B/merge && unzip -q $B/srg.jar -x 'META-INF/*')
rm -f "$OUT"; (cd $B/merge && zip -qr -X "$OUT" META-INF/MANIFEST.MF .)
echo "built $OUT: $(unzip -l "$OUT" | tail -1)"
