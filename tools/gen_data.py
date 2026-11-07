#!/usr/bin/env python3
"""Writes this mod's data and language files, and turns DangerZone's "things to do" into advancements.

usage: gen_data.py <dangerzone root> <OreSpawn ids json>

The ids file lists what the port registers. A task whose item, block or creature isn't there is skipped.
"""
import json
import os
import re
import sys

from PIL import Image

DZ, IDS = sys.argv[1], sys.argv[2]
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = "orespawn_dangerzone"
RES = os.path.join(HERE, "src/main/resources")
DATA = os.path.join(RES, "data", MOD)
ASSETS = os.path.join(RES, "assets", MOD)
ids = json.load(open(IDS, encoding="utf8"))
PORT_ITEMS, PORT_BLOCKS, PORT_ENTITIES = set(ids["items"]), set(ids["blocks"]), set(ids["entities"])

OWN_ENTITIES = {"NastysaurusPrime": "nastysaurus_prime", "Needlenose": "needlenose", "NurseBug": "nurse_bug",
                "AntQueen": "ant_queen", "BloodlustAnt": "bloodlust_ant"}
OWN_ITEMS = {"dimensionstaff": "dimension_staff", "dragonseye": "dragons_eye", "dragonseyesword": "dragons_eye_sword",
             "shrinkray": "shrink_ray"}
ENTITY_ALIAS = {
    "Beetle": "hercules_beetle", "Bird": "cockateil", "Criminal": "band_p", "FFrog": "frog", "Hoverboard": "elevator",
    "JumpyBug": "trooper_bug", "WTF": "gamma_metroid", "Moth": "luna_moth", "Nightmare": "pitch_black", "PowerOrb": "purple_power",
    "Ratt": "rat", "RoboBomb": "robot_1", "RoboPounder": "robot_2", "RoboGunner": "robot_3", "RoboWarrior": "robot_4",
    "RoboSniper": "robot_5", "Squid": "attack_squid", "ThePrinceYoungAdult": "the_prince_adult", "Mobzilla": "godzilla",
    "RobotAnt": "ant_robot", "Bro": "boyfriend", "TRex": "trex", "Rotator": "rotator", "Leon": "leonopteryx", "TheQueen": "the_queen", "TheKing": "the_king",
}
ITEM_ALIAS = {
    "cageempty": "cage_empty", "crabbypatty": "crabby_patty", "wormtooth": "worm_tooth", "trextooth": "trex_tooth", "krakentooth": "kraken_tooth",
    "vortexeye": "vortex_eye", "magicapple": "magic_apple", "ducktape": "duct_tape", "mothscale": "moth_scale", "mantisclaw": "mantis_claw",
    "ultimatesword": "ultimate_sword", "ultimatebow": "ultimate_bow", "bertha": "big_bertha", "raygun": "ray_gun",
    "nuggeturanium": "uranium_nugget", "nuggettitanium": "titanium_nugget",
    "bbblade": "bertha_blade", "bbguard": "bertha_guard", "bbhandle": "bertha_handle",
}
DIMENSIONS = {"utopiadimension": "utopia", "miningdimension": "mining", "chaosdimension": "chaos", "crystaldimension": "crystal", "dangerdimension": "islands"}
# Kept out of the mod: the real-world political jokes.
EXCLUDE = re.compile(r"Putin|Lavrov|Russian|Vaxx|Libtard|Pharma|Red-Pill|Whiner|Liberals|Deep State", re.I)
# The original's text and its check disagree, so there's nothing faithful to carry over.
BROKEN = {"task218"}


def write_json(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf8", newline="\n") as fh:
        json.dump(value, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def norm(text):
    return re.sub(r"[^a-z0-9]", "", text.lower())


PORT_ITEM_NORM = {norm(i): i for i in PORT_ITEMS}
PORT_BLOCK_NORM = {norm(i): i for i in PORT_BLOCKS}


def item_id(field):
    if field in OWN_ITEMS:
        return f"{MOD}:{OWN_ITEMS[field]}"
    if field in ITEM_ALIAS and ITEM_ALIAS[field] in PORT_ITEMS | PORT_BLOCKS:
        return "orespawn:" + ITEM_ALIAS[field]
    key = norm(field)
    for table in (PORT_ITEM_NORM, PORT_BLOCK_NORM):
        if key in table:
            return "orespawn:" + table[key]
    return None


def entity_id(cls):
    if cls in OWN_ENTITIES:
        return f"{MOD}:{OWN_ENTITIES[cls]}"
    name = ENTITY_ALIAS.get(cls)
    if name is None:
        name = re.sub(r"(?<!^)(?=[A-Z])", "_", cls).lower()
    return "orespawn:" + name if name in PORT_ENTITIES else None


def egg_icon(entity):
    ns, path = entity.split(":")
    if ns == MOD:
        return f"{MOD}:{path}_spawn_egg"
    return f"orespawn:{path}_spawn_egg" if f"{path}_spawn_egg" in PORT_ITEMS else "minecraft:bone"


def player_stat_condition(entity, count):
    return [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type_specific": {
        "type": "minecraft:player",
        "stats": [{"type": "minecraft:killed", "stat": entity, "value": {"min": count}}]}}}]


def article(name):
    if name.startswith("The "):
        return ""
    return "an " if name[0].lower() in "aeiou" else "a "


def parse_tasks():
    src = open(os.path.join(DZ, "src/orespawn-dzvr28/OreSpawn/Achievements.java"), encoding="utf8", errors="ignore").read()
    pattern = re.compile(r'new (\w+)\(\s*"OreSpawn:(task\d+)",\s*"((?:[^"\\]|\\.)*)",\s*(null|"(?:[^"\\]|\\.)*"),\s*(\d+),\s*(.*?)\)\s*\);', re.S)
    tasks, skipped = [], []
    for kind, tid, title, desc, level, rest in pattern.findall(src):
        title = title.strip().lstrip("* ").strip()
        desc = None if desc == "null" else desc[1:-1].replace("\\n", " ").strip()
        args = [a.strip() for a in re.split(r",\s*", rest)]
        if tid in BROKEN or EXCLUDE.search(title + " " + (desc or "") + " " + rest):
            skipped.append((tid, title, "excluded"))
            continue
        crit, icon = None, None
        target = args[0].split(".")[-1] if "OreSpawnMain." in args[0] or "Items." in args[0] or "Blocks." in args[0] else args[0].replace(".class", "")
        if kind in ("CompareCrafter", "FoundOnGround", "ArmoredUpItem", "Eaten", "BlockPlaced", "ItemRightClickedBlock"):
            item = item_id(target)
            if item:
                icon = item
                if kind == "Eaten":
                    crit = {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": item}}}
                elif kind == "BlockPlaced" and item.split(":")[1] in PORT_BLOCKS:
                    crit = {"trigger": "minecraft:placed_block", "conditions": {"location": [{"condition": "minecraft:block_state_property", "block": item}]}}
                elif kind == "ItemRightClickedBlock":
                    crit = {"trigger": "minecraft:item_used_on_block", "conditions": {"location": [{"condition": "minecraft:match_tool", "predicate": {"items": item}}]}}
                else:
                    crit = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}
        elif kind in ("MobKiller", "MobTamer", "Rider"):
            entity = entity_id(target)
            if entity:
                icon = egg_icon(entity)
                if kind == "MobKiller":
                    plural, count = args[1].strip('"'), int(args[2])
                    desc = desc or (f"Kill {count} {plural}." if count > 1 else f"Kill {article(plural)}{plural}.")
                    conditions = {"entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]}
                    if count > 1:
                        conditions["player"] = player_stat_condition(entity, count)
                    crit = {"trigger": "minecraft:player_killed_entity", "conditions": conditions}
                elif kind == "MobTamer":
                    crit = {"trigger": "minecraft:tame_animal", "conditions": {"entity": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]}}
                else:
                    crit = {"trigger": "minecraft:started_riding", "conditions": {"player": [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"vehicle": {"type": entity}}}]}}
        elif kind == "DimensionChangeTo":
            dim = DIMENSIONS.get(target)
            if dim:
                icon = "minecraft:ender_eye"
                crit = {"trigger": "minecraft:changed_dimension", "conditions": {"to": "orespawn:" + dim}}
        if crit is None:
            skipped.append((tid, title, f"{kind} {target}"))
            continue
        tasks.append(dict(id=tid, kind=kind, title=title, desc=desc or title, level=int(level), crit=crit, icon=icon))
    kill = lambda entity: {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]}}
    tasks += [
        dict(id="prime", kind="MobKiller", title="Prime Time", desc="Kill a Nastysaurus Prime.", level=8,
             crit=kill(f"{MOD}:nastysaurus_prime"), icon=f"{MOD}:nastysaurus_prime_spawn_egg"),
        dict(id="queen", kind="MobKiller", title="Long Live the Queen", desc="Kill an Ant Queen.", level=8,
             crit=kill(f"{MOD}:ant_queen"), icon=f"{MOD}:ant_queen_spawn_egg"),
        dict(id="tape", kind="CompareCrafter", title="Good as New", desc="Fix something with Duct Tape.", level=2,
             crit={"trigger": "minecraft:recipe_crafted", "conditions": {"recipe_id": f"{MOD}:duct_tape_repair"}}, icon="orespawn:duct_tape"),
    ]
    return tasks, skipped


CATEGORIES = {
    "Dimensions": ("DimensionChangeTo", "minecraft:ender_eye", "Other Places", "Go somewhere you shouldn't."),
    "Slayer": ("MobKiller", "minecraft:iron_sword", "Slayer", "Kill things. Lots of things."),
    "Friends": ("MobTamer Rider", "minecraft:saddle", "Friends", "Tame it. Ride it."),
    "Makers": ("CompareCrafter Eaten", "minecraft:crafting_table", "Maker", "Craft it. Cook it. Eat it."),
    "Finders": ("FoundOnGround ArmoredUpItem", "minecraft:chest", "Finders Keepers", "Find it. Wear it."),
    "Planters": ("BlockPlaced ItemRightClickedBlock", "minecraft:wheat_seeds", "Green Thumb", "Plant it. Build it."),
}


def node(title_key, desc_key, icon, parent, crit, frame="task", toast=True, xp=0, background=None):
    display = {"icon": {"id": icon}, "title": {"translate": title_key}, "description": {"translate": desc_key},
               "frame": frame, "show_toast": toast, "announce_to_chat": False, "hidden": False}
    if background:
        display["background"] = background
    value = {"display": display, "criteria": {"done": crit}, "requirements": [["done"]]}
    if parent:
        value["parent"] = parent
    if xp:
        value["rewards"] = {"experience": xp}
    return value


def advancements(lang):
    tick = {"trigger": "minecraft:tick"}
    root = f"{MOD}:things_to_do/root"
    lang["advancement.orespawn_dangerzone.root"] = "Things To Do"
    lang["advancement.orespawn_dangerzone.root.desc"] = "Hundreds of things to do, from DangerZone."
    base = os.path.join(DATA, "advancement/things_to_do")
    write_json(os.path.join(base, "root.json"), node("advancement.orespawn_dangerzone.root", "advancement.orespawn_dangerzone.root.desc",
                                                     f"{MOD}:dimension_staff", None, tick, toast=False,
                                                     background="minecraft:textures/block/obsidian.png"))
    tasks, skipped = parse_tasks()
    written = 0
    for cat, (kinds, icon, title, desc) in CATEGORIES.items():
        slug = cat.lower()
        cat_id = f"{MOD}:things_to_do/{slug}"
        lang[f"advancement.orespawn_dangerzone.{slug}"] = title
        lang[f"advancement.orespawn_dangerzone.{slug}.desc"] = desc
        write_json(os.path.join(base, f"{slug}.json"), node(f"advancement.orespawn_dangerzone.{slug}", f"advancement.orespawn_dangerzone.{slug}.desc",
                                                            icon, root, tick, toast=False))
        group = [t for t in tasks if t["kind"] in kinds.split()]
        previous = cat_id
        for index, task in enumerate(group):
            if index % 6 == 0:
                previous = cat_id
            key = f"advancement.orespawn_dangerzone.{task['id']}"
            lang[key] = task["title"]
            lang[key + ".desc"] = task["desc"]
            frame = "challenge" if task["level"] >= 8 else "goal" if task["level"] >= 5 else "task"
            write_json(os.path.join(base, slug, task["id"] + ".json"),
                       node(key, key + ".desc", task["icon"], previous, task["crit"], frame=frame, xp=111))
            previous = f"{MOD}:things_to_do/{slug}/{task['id']}"
            written += 1
    print("advancements", written, "skipped", len(skipped))
    report = os.path.join(HERE, "build", "things_to_do_report.txt")
    os.makedirs(os.path.dirname(report), exist_ok=True)
    with open(report, "w", encoding="utf8") as fh:
        fh.write("KEPT\n" + "".join(f"{t['id']} [{t['kind']}] {t['title']} :: {t['desc']}\n" for t in tasks))
        fh.write("SKIPPED\n" + "".join(f"{a} {b} :: {c}\n" for a, b, c in skipped))


def item_model(name, parent="minecraft:item/generated"):
    return {"parent": parent, "textures": {"layer0": f"{MOD}:item/{name}"}}


def loot(*pools):
    return {"type": "minecraft:entity", "pools": list(pools)}


def pool(item, low=1, high=1, chance=None):
    entry = {"type": "minecraft:item", "name": item}
    if (low, high) != (1, 1):
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    result = {"rolls": 1, "entries": [entry]}
    if chance is not None:
        result["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return result


def main():
    lang = {
        "itemGroup.orespawn_dangerzone": "OreSpawn: DangerZone",
        "entity.orespawn_dangerzone.nastysaurus_prime": "Nastysaurus Prime",
        "entity.orespawn_dangerzone.needlenose": "Needlenose",
        "entity.orespawn_dangerzone.nurse_bug": "Nurse Bug",
        "entity.orespawn_dangerzone.ant_queen": "Ant Queen",
        "entity.orespawn_dangerzone.bloodlust_ant": "Bloodlust Ant",
        "entity.orespawn_dangerzone.shrink_bolt": "Shrink Bolt",
        "item.orespawn_dangerzone.dimension_staff": "Dimension Staff",
        "item.orespawn_dangerzone.dimension_staff.tooltip": "Use: next dimension. Sneak and use: the one before.",
        "item.orespawn_dangerzone.dragons_eye": "Dragon's Eye",
        "item.orespawn_dangerzone.dragons_eye_sword": "Dragon's Eye Sword",
        "item.orespawn_dangerzone.shrink_ray": "A Freakin' Shrink Ray!!!",
        "item.orespawn_dangerzone.nastysaurus_prime_spawn_egg": "Nastysaurus Prime Egg",
        "item.orespawn_dangerzone.needlenose_spawn_egg": "Needlenose Egg",
        "item.orespawn_dangerzone.nurse_bug_spawn_egg": "Nurse Bug Egg",
        "item.orespawn_dangerzone.ant_queen_spawn_egg": "Ant Queen Egg",
        "item.orespawn_dangerzone.bloodlust_ant_spawn_egg": "Bloodlust Ant Egg",
        "effect.orespawn_dangerzone.shrunk": "Shrunk",
    }
    models = os.path.join(ASSETS, "models/item")
    write_json(os.path.join(models, "dragons_eye.json"), item_model("dragons_eye"))
    write_json(os.path.join(models, "shrink_ray.json"), item_model("shrink_ray", "minecraft:item/handheld"))
    write_json(os.path.join(models, "dimension_staff.json"), item_model("dimension_staff", "minecraft:item/handheld"))
    write_json(os.path.join(models, "dragons_eye_sword.json"), item_model("dragons_eye_sword", "minecraft:item/handheld"))
    for egg in ("nastysaurus_prime", "needlenose", "ant_queen", "bloodlust_ant"):
        write_json(os.path.join(models, f"{egg}_spawn_egg.json"), item_model(f"{egg}_spawn_egg"))
    write_json(os.path.join(models, "nurse_bug_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})

    # DangerZone has no effect icons: the ray's own picture, doubled onto the 18x18 frame.
    ray = Image.open(os.path.join(ASSETS, "textures/item/shrink_ray.png")).convert("RGBA")
    icon = Image.new("RGBA", (18, 18))
    icon.paste(ray.resize((16, 16), Image.NEAREST), (1, 1))
    os.makedirs(os.path.join(ASSETS, "textures/mob_effect"), exist_ok=True)
    icon.save(os.path.join(ASSETS, "textures/mob_effect/shrunk.png"))

    recipes = os.path.join(DATA, "recipe")
    write_json(os.path.join(recipes, "dimension_staff.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" V ", "PTP", " T "],
        "key": {"V": {"item": "orespawn:vortex_eye"}, "P": {"item": "minecraft:ender_pearl"}, "T": {"item": "orespawn:ingot_titanium"}},
        "result": {"id": f"{MOD}:dimension_staff", "count": 1}})
    write_json(os.path.join(recipes, "dragons_eye_sword.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["E", "E", "S"],
        "key": {"E": {"item": f"{MOD}:dragons_eye"}, "S": {"item": "minecraft:stick"}},
        "result": {"id": f"{MOD}:dragons_eye_sword", "count": 1}})
    write_json(os.path.join(recipes, "shrink_ray.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["AAA", "ARA", "AAA"],
        "key": {"A": {"item": "minecraft:amethyst_shard"}, "R": {"item": "orespawn:ray_gun"}},
        "result": {"id": f"{MOD}:shrink_ray", "count": 1}})
    write_json(os.path.join(recipes, "duct_tape_repair.json"), {"type": f"{MOD}:duct_tape_repair", "category": "misc"})

    tables = os.path.join(DATA, "loot_table/entities")
    write_json(os.path.join(tables, "nastysaurus_prime.json"), loot(
        pool("minecraft:beef", 7, 15), pool("minecraft:bone", 3, 7), pool("orespawn:ruby", 6, 20), pool(f"{MOD}:dragons_eye", chance=0.35)))
    write_json(os.path.join(tables, "needlenose.json"), loot(pool("minecraft:beef", 1, 2), pool("minecraft:bone", 3, 7)))
    write_json(os.path.join(tables, "bloodlust_ant.json"), loot(pool("minecraft:spider_eye", 1, 2), pool("minecraft:bone", 1, 3)))
    write_json(os.path.join(tables, "nurse_bug.json"), loot(pool("minecraft:chicken", 1, 4), pool("minecraft:feather", 1, 4), pool("orespawn:mantis_claw", 2, 2)))
    ants = ["ant", "red_ant", "rainbow_ant", "unstable_ant", "termite", "ant_robot"]
    eggs = [f"orespawn:{a}_spawn_egg" for a in ants] + [f"{MOD}:bloodlust_ant_spawn_egg"]
    queen = [pool("orespawn:raw_crab_meat", 50, 99), pool("minecraft:bone", 25, 74),
             {"rolls": {"type": "minecraft:uniform", "min": 15, "max": 39}, "entries": [{"type": "minecraft:item", "name": e} for e in eggs]}]
    for rare in ("worm_tooth", "kraken_tooth", "vortex_eye", "ultimate_sword", "trex_tooth", "sea_monster_scale", "nightmare_scale", "jumpy_bug_scale"):
        queen.append(pool("orespawn:" + rare, chance=0.25))
    queen.append(pool(f"{MOD}:dimension_staff", chance=0.05))
    write_json(os.path.join(tables, "ant_queen.json"), loot(*queen))

    optional = lambda name: {"id": name, "required": False}
    write_json(os.path.join(DATA, "tags/entity_type/shrink_immune.json"), {"values": [
        f"{MOD}:nastysaurus_prime", f"{MOD}:ant_queen", "minecraft:wither", "minecraft:ender_dragon", {"id": "#c:bosses", "required": False}]
        + [optional("orespawn:" + b) for b in (
            "the_king", "king_head", "the_queen", "queen_head", "godzilla", "godzilla_head", "kraken", "mothra", "basilisk", "alien_boss",
            "the_prince_adult", "giant_robot", "jeffery", "emperor_scorpion", "sea_monster", "water_dragon", "worm_large", "ender_reaper", "nastysaurus")]})
    write_json(os.path.join(DATA, "tags/entity_type/drops_dragons_eye.json"), {"values": [
        optional("orespawn:" + d) for d in ("dragon", "water_dragon", "spyro", "leonopteryx")]})
    write_json(os.path.join(DATA, "tags/item/not_tapeable.json"), {"values": []})

    spawns = os.path.join(DATA, "neoforge/biome_modifier")

    def add_spawns(name, biomes, *spawners):
        write_json(os.path.join(spawns, name + ".json"), {
            "type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": f"{MOD}:{e}", "weight": w, "minCount": a, "maxCount": b} for e, w, a, b in spawners]})

    add_spawns("mining", "orespawn:mining_biome", ("nastysaurus_prime", 3, 1, 2), ("needlenose", 5, 2, 5), ("nurse_bug", 2, 1, 1))
    add_spawns("chaos", "orespawn:chaos_biome", ("nastysaurus_prime", 3, 1, 2), ("ant_queen", 1, 1, 1), ("bloodlust_ant", 3, 1, 3), ("nurse_bug", 3, 1, 1))
    add_spawns("windswept", "#c:is_windswept", ("ant_queen", 1, 1, 1), ("bloodlust_ant", 3, 1, 5))
    add_spawns("nurses", ["orespawn:crystal_plains", "orespawn:island_biome"], ("nurse_bug", 3, 1, 1))
    add_spawns("forests", "#minecraft:is_forest", ("nurse_bug", 1, 1, 1))

    advancements(lang)
    write_json(os.path.join(ASSETS, "lang/en_us.json"), lang)


main()
