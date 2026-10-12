#!/bin/bash
# Every Minecraft/Forge/GeckoLib/PlayerAnimator member the jar references must exist at runtime (SRG names).
# Usage: WORK=... tools/refcheck.sh mod.jar
W=${WORK:?set WORK}; L=$W/forge/mc/libraries
RT="$L/net/minecraftforge/forge/1.20.1-47.4.9/forge-1.20.1-47.4.9-client.jar:$L/net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar:$L/net/minecraftforge/forge/1.20.1-47.4.9/forge-1.20.1-47.4.9-universal.jar:$W/deps/geckolib.jar:$W/deps/playeranim.jar:$(find $L -name '*.jar' ! -path '*forge/1.20.1*' ! -path '*net/minecraft/client*' | tr '\n' ':')"
java -cp $W/tools/cls:$W/tools/asm-9.7.jar RefCheck "$1" "$RT" 2>&1 | grep -v JAVA_TOOL | tail -20
