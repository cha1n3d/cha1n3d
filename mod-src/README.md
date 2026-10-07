# VAZ-2109 1.3.0 — изменённые исходники

Готовый мод: `../vaz2109-1.3.0.jar` (Minecraft 1.20.1, Forge 47+). Он включает все правки,
в том числе полоски боссов только после удара.

Исходников оригинала нет, поэтому здесь только **изменённые и новые** классы, декомпилированные
в именах Mojang. Остальные классы в JAR — оригинальные.

- `src/` — Java-исходники (Mojang names).
- `res/` — новые/изменённые ресурсы (lang, текстуры, модели, worldgen, теги, mods.toml).
- `tools/` — сборка и проверки.

## Как пересобрать

1. Установить Forge 1.20.1-47.4.9 (`--installClient`) в `$WORK/forge/mc` и скачать ванильные
   библиотеки по `versions/1.20.1/1.20.1.json`.
2. `tools/mkmap.py` + `tools/mktsrg.py` — таблица Mojang→SRG из маппингов установщика (`$WORK/moj2srg.tsrg`).
3. ForgeAutoRenamingTool `--reverse`: перевести в Mojang-имена `client-srg.jar`, `forge-*-client.jar`,
   `forge-*-universal.jar` и исходный JAR мода → `$WORK/moj/{mc,forge-client,forge-universal,mod}.jar`.
4. Положить исходный JAR в `$WORK/jar.orig.jar`, `src/` → `$WORK/work/src`, `res/` → `$WORK/work/res`.
5. `WORK=... tools/build.sh out.jar` — javac → обратное переименование в SRG → слияние с оригиналом.
6. `tools/RefCheck.java` (ASM) — проверить, что все ссылки на Minecraft/Forge существуют в рантайме.

## Тестовые команды (нужны права оператора)

- `/vazclass set <vessel|mechanic|frog|brewer> [игроки]` — сменить класс, `/vazclass reset [игроки]` — сбросить.
- `/vazclass unlock [игроки]` — открыть все способности класса без условий и сбросить перезарядки; `/vazclass lock` — вернуть обычные условия.
- `/vazclass cooldowns [игроки]` — сбросить перезарядки.
- `/vazclass fingers <0-20> [игроки]` — задать число пальцев Сукуны.
- `/vazclass trials [игроки]` — выполнить все испытания (Картина смерти, тюнинг, прыжки, пиво).
- `/vazclass bosses [игроки]` — отметить всех боссов побеждёнными.
