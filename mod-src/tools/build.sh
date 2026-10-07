#!/bin/bash
# Compile edited/new sources (Mojang names) -> remap to SRG -> merge into original jar.
set -e
S=${WORK:?set WORK}; L=$S/forge/mc/libraries
ART=$L/net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar
OTHERS=$(find $L -name '*.jar' ! -path '*forge/1.20.1*' ! -path '*net/minecraft/client*' ! -path '*ForgeAutoRenaming*' | tr '\n' ':')
CP="$S/moj/forge-client.jar:$S/moj/mc.jar:$S/moj/forge-universal.jar:${OTHERS}$S/tools/annotations-24.0.1.jar:$S/moj/mod.jar"
rm -rf $S/work/classes $S/work/out.jar $S/work/srg.jar; mkdir -p $S/work/classes
cd $S/work/src
javac -nowarn -proc:none -encoding UTF-8 --release 17 -g -cp "$CP" -d $S/work/classes $(find . -name '*.java')
cd $S/work/classes && jar cf $S/work/out.jar .
LIBS=""; for j in $S/moj/mc.jar $S/moj/forge-client.jar $S/moj/forge-universal.jar $S/moj/mod.jar $(echo $OTHERS | tr ':' ' '); do LIBS="$LIBS --lib $j"; done
java -jar $ART --input $S/work/out.jar --output $S/work/srg.jar --map $S/moj2srg.tsrg $LIBS --log $S/work/art.log >/dev/null
rm -rf $S/work/merge && mkdir $S/work/merge && cd $S/work/merge && unzip -q $S/jar.orig.jar && unzip -oq $S/work/srg.jar -x 'META-INF/MANIFEST.MF'
[ -d $S/work/res ] && cp -r $S/work/res/. $S/work/merge/
rm -f "$1"; cd $S/work/merge && zip -qr -X "$1" META-INF/MANIFEST.MF . 
echo "built $1: $(unzip -l "$1" | tail -1)"
