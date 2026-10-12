# Fallen Roses — исходники

Мод для сборки «JJK! Fallen Roses» (Minecraft 1.20.1, Forge 47+, GeckoLib 4.4+, PlayerAnimator 1.0+).

Исходников оригинала нет: `src/` — полностью декомпилированный мод (Vineflower) в именах Mojang,
исправленный так, что собирается целиком. `res/` — все ресурсы JAR (assets, data, mods.toml).

## Сборка

```sh
export WORK=/путь/к/рабочей/папке
tools/setup.sh                      # один раз: Forge 1.20.1-47.4.9, библиотеки, маппинги, зависимости
tools/build.sh ../fallenroses.jar   # javac → ForgeAutoRenamingTool (Mojang → SRG) → JAR
tools/refcheck.sh ../fallenroses.jar
```

`tools/MemberDiff.java` сравнивает члены классов двух JAR — так проверено, что пересборка
неизменённых исходников даёт те же методы и поля, что и оригинал 0.8.0.
