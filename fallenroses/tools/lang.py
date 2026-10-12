"""Adds/updates the 0.9 keys in en_us.json and ru_ru.json (both kept complete)."""
import json
import os

D = os.path.join(os.path.dirname(__file__), '..', 'res', 'assets', 'fallenroses', 'lang')

# key: (English, Russian)
NEW = {
    "subtitles.fallenroses.aura": ("Cursed energy flares", "Вспышка проклятой энергии"),
    "subtitles.fallenroses.barrier": ("Barrier closes", "Барьер смыкается"),
    "subtitles.fallenroses.shatter": ("Barrier shatters", "Барьер рушится"),
    "subtitles.fallenroses.gunshot": ("Gunshot", "Выстрел"),
    "subtitles.fallenroses.reload": ("Revolver clicks", "Щелчок револьвера"),
    "subtitles.fallenroses.whip": ("Rope lashes", "Свист верёвки"),
    "subtitles.fallenroses.unravel": ("Technique unravels", "Техника распускается"),
    "subtitles.fallenroses.wind": ("Wind rushes", "Свист ветра"),
    "subtitles.fallenroses.metal": ("Metal rings", "Звон металла"),
    "subtitles.fallenroses.electric": ("Electricity crackles", "Треск электричества"),
    "subtitles.fallenroses.roar": ("Monstrous roar", "Чудовищный рёв"),
    "subtitles.fallenroses.swarm": ("Insects swarm", "Гул роя"),
    "subtitles.fallenroses.gavel": ("Gavel strikes", "Удар судейского молотка"),
    "subtitles.fallenroses.jackpot": ("Jackpot!", "Джекпот!"),
    # weapons
    "message.fallenroses.revolver_last": ("Construction! The last bullet is forged from cursed energy", "Конструкция! Последняя пуля выкована из проклятой энергии"),
    "message.fallenroses.revolver_ammo": ("Cylinder: %s", "Барабан: %s"),
    "message.fallenroses.revolver_reload": ("Reloading…", "Перезарядка…"),
    "message.fallenroses.revolver_headshot": ("Headshot!", "В голову!"),
    "item.fallenroses.mai_revolver.desc": (
        "Right-click: fire, hits instantly, headshots deal 1.5x. Six rounds: Mai constructs the sixth from 20 cursed energy, it hits much harder and pierces three targets. Empty cylinder or sneak + right-click: reload.",
        "ПКМ — выстрел, попадание мгновенное, в голову ×1,5. Шесть патронов: шестой Май создаёт из 20 проклятой энергии — он бьёт гораздо сильнее и пробивает трёх целей. Пустой барабан или Shift + ПКМ — перезарядка."),
    "item.fallenroses.black_rope.desc": (
        "Unravels cursed techniques: strips buffs, breaks reinforcement and Infinity. Tap right-click: a 7-block lash that also drains cursed energy, jams techniques for a moment and cuts projectiles. Hold: spin the rope to shred techniques that come close.",
        "Расплетает проклятые техники: снимает усиления, сбивает укрепление и Бесконечность. Короткий ПКМ — хлёст на 7 блоков: ещё и выжигает проклятую энергию, ненадолго сбивает техники и рассекает снаряды. Удержание — верёвка вращается и рвёт техники, что подлетают близко."),
    "item.fallenroses.momo_broom.desc": (
        "Hold right-click to fly where you look: up to ~11 blocks/s, no higher than 22 blocks above the ground, 12 seconds per flight; the broom recharges while you walk. A solid hit knocks you off. Uses cursed energy.",
        "Держи ПКМ — летишь туда, куда смотришь: до ~11 блоков/с, не выше 22 блоков над землёй, 12 секунд за полёт; метла восстанавливается, пока идёшь пешком. Сильный удар сбивает с метлы. Тратит проклятую энергию."),
    "message.fallenroses.broom_tired": ("The broom needs a rest", "Метле нужна передышка"),
    "message.fallenroses.broom_no_energy": ("Not enough cursed energy to fly", "Не хватает проклятой энергии для полёта"),
    "message.fallenroses.broom_charge": ("Broom %s", "Метла %s"),
    "message.fallenroses.broom_knocked": ("You were knocked off the broom!", "Тебя сбили с метлы!"),
}


def main():
    for lang, idx in (('en_us', 0), ('ru_ru', 1)):
        p = os.path.join(D, lang + '.json')
        d = json.load(open(p, encoding='utf-8'))
        for k, v in NEW.items():
            d[k] = v[idx]
        open(p, 'w', encoding='utf-8').write(json.dumps(d, indent=2, ensure_ascii=False) + '\n')
    en = json.load(open(os.path.join(D, 'en_us.json'), encoding='utf-8'))
    ru = json.load(open(os.path.join(D, 'ru_ru.json'), encoding='utf-8'))
    assert set(en) == set(ru), set(en) ^ set(ru)
    print(f"lang: {len(en)} keys in both files")


if __name__ == '__main__':
    main()
