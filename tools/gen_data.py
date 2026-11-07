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
                "AntQueen": "ant_queen", "BloodlustAnt": "bloodlust_ant", "Nope": "nope", "BabyNope": "baby_nope",
                "ThePrincessTeen": "princess_teen", "ThePrincessYoungAdult": "princess_young_adult", "UFO": "ufo",
                "UFOCarrier": "ufo_carrier", "UFOFighter": "ufo_fighter", "Martian": "martian", "MyMartian": "martian",
                "ChaosDemon": "chaos_demon", "BabySitter": "baby_sitter", "KrakenKing": "kraken_king", "SquidGuard": "squid_guard",
                "Dargon": "dargon", "ElementDragon": "element_dragon", "DoomWorm": "doom_worm", "DoomAnt": "doom_ant", "Drant": "drant",
                "Annoyance": "annoyance", "Varmint": "varmint", "Vermin": "vermin", "Flattop": "flattop", "Hyena": "hyena", "LavaLizard": "lava_lizard",
                "LazerLizard": "lazer_lizard", "LightningLizard": "lightning_lizard", "RagingBull": "raging_bull", "Rattlesnake": "rattlesnake",
                "RollyOuchy": "rolly_ouchy", "SkinnyThing": "skinny_thing", "V": "v", "Warthog": "warthog", "WoolyMammoth": "woolly_mammoth"}
OWN_ITEMS = {"dimensionstaff": "dimension_staff", "dragonseye": "dragons_eye", "dragonseyesword": "dragons_eye_sword",
             "shrinkray": "shrink_ray", "wormfood": "worm_food", "extremetorch": "extreme_torch", "firestaff": "fire_staff",
             "earthstaff": "earth_staff", "waterstaff": "water_staff", "windstaff": "wind_staff", "lightningstaff": "lightning_staff",
             "plantstaff": "plant_staff", "dragoneyestone": "dragons_eye_stone", "darkstone": "dark_stone", "tiara": "royal_tiara", "morphstaff": "morph_staff"}
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
DIMENSIONS = {"utopiadimension": "utopia", "miningdimension": "mining", "chaosdimension": "chaos", "crystaldimension": "crystal", "dangerdimension": "islands",
              "doomdimension": "orespawn_dangerzone:doom", "dragonslairdimension": "orespawn_dangerzone:dragons_lair", "krakenkingdimension": "orespawn_dangerzone:kraken_realm"}
# Kept out of the mod: the real-world political jokes.
EXCLUDE = re.compile(r"Putin|Lavrov|Russian|Vaxx|Libtard|Pharma|Red-Pill|Whiner|Liberals|Deep State", re.I)
# The original's text and its check disagree, so there's nothing faithful to carry over.
BROKEN = {"task218"}


def milestone_four(lang, add_spawns):
    """The Lair's little flyers, the rest of the Tower Defense ladder, and what Dargon's captive carries."""
    models = os.path.join(ASSETS, "models/item")
    names = {"annoyance": "Annoyance", "varmint": "Varmint", "vermin": "Vermin", "flattop": "Flattop", "hyena": "Hyena", "lava_lizard": "Lava Lizard",
             "lazer_lizard": "Lazer Lizard", "lightning_lizard": "Lightning Lizard", "raging_bull": "Raging Bull", "rattlesnake": "Rattlesnake",
             "rolly_ouchy": "Rolly Ouchy", "skinny_thing": "Skinny Thing", "v": "V", "warthog": "Warthog", "woolly_mammoth": "Woolly Mammoth"}
    for key, title in names.items():
        lang[f"entity.{MOD}.{key}"] = title
        lang[f"item.{MOD}.{key}_spawn_egg"] = title + " Egg"
        write_json(os.path.join(models, key + "_spawn_egg.json"), item_model(key + "_spawn_egg"))
    write_json(os.path.join(models, "royal_tiara.json"), item_model("royal_tiara"))
    write_json(os.path.join(models, "morph_staff.json"), item_model("morph_staff", "minecraft:item/handheld"))
    lang[f"item.{MOD}.royal_tiara"] = "Royal Tiara"
    lang[f"item.{MOD}.morph_staff"] = "Morph Staff"

    tables = os.path.join(DATA, "loot_table/entities")
    meat = lambda: [pool("minecraft:beef", 3, 6), pool("minecraft:bone", 2, 4)]
    drops = {
        "annoyance": [pool("minecraft:spider_eye", 1, 2)],
        "varmint": [pool("minecraft:spider_eye", 2, 3)],
        "vermin": [pool("minecraft:spider_eye", chance=0.5)],
        "flattop": meat() + [pool("minecraft:crafting_table", chance=0.5)],
        "hyena": [pool("minecraft:beef", 1, 3), pool("minecraft:bone", 1, 2)],
        "lava_lizard": meat() + [pool("minecraft:copper_ingot", 3, 7), pool("minecraft:copper_block", 1, 3), pool("minecraft:chainmail_chestplate", chance=0.25)],
        "lazer_lizard": meat() + [pool("minecraft:iron_ingot", 3, 7), pool("minecraft:iron_block", 1, 3), pool("minecraft:iron_chestplate", chance=0.25)],
        "lightning_lizard": meat() + [pool("minecraft:gold_ingot", 3, 7), pool("minecraft:gold_block", 1, 3), pool("minecraft:golden_chestplate", chance=0.25)],
        "raging_bull": meat(),
        "rattlesnake": [pool("minecraft:string", 1, 2)],
        "rolly_ouchy": [pool("minecraft:iron_nugget", 1, 3)],
        "skinny_thing": [pool("minecraft:stick", 1, 3)],
        "v": [pool("minecraft:spider_eye", 1, 1)],
        "warthog": [pool("minecraft:porkchop", 1, 3), pool("minecraft:bone", 1, 2)],
        "woolly_mammoth": [pool("minecraft:beef", 8, 16), pool("minecraft:bone", 8, 16), pool("minecraft:leather", 4, 8)],
    }
    for key, pools in drops.items():
        write_json(os.path.join(tables, key + ".json"), loot(*pools))

    # DangerZone keeps these in its Misfit dimension and the Zoo, which the port doesn't have: Chaos, the mines and wild biomes instead.
    add_spawns("chaos_m4", "orespawn:chaos_biome", ("flattop", 1, 1, 1), ("raging_bull", 1, 1, 1), ("rolly_ouchy", 3, 1, 3), ("skinny_thing", 3, 1, 3), ("v", 3, 1, 2))
    add_spawns("mining_m4", "orespawn:mining_biome", ("lava_lizard", 1, 1, 1), ("lazer_lizard", 1, 1, 1), ("lightning_lizard", 1, 1, 1))
    add_spawns("savanna_m4", "#c:is_savanna", ("hyena", 3, 2, 4), ("warthog", 3, 1, 3))
    add_spawns("desert_m4", ["minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands"], ("rattlesnake", 3, 1, 2))
    add_spawns("snow_m4", ["minecraft:snowy_plains", "minecraft:snowy_taiga"], ("woolly_mammoth", 1, 1, 1))


def swarm_lines():
    src = open(os.path.join(DZ, "src/orespawn-dzvr28/OreSpawn/OreSpawnServerHooks.java"), encoding="utf8", errors="ignore").read()
    return re.findall(r'toprint = "([^"]+)"', src)


SWARM_LINES = swarm_lines()


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


def tower_defense():
    """DangerZone marks which creatures its flag may call; those that exist here are the ladder."""
    import glob
    found = set()
    missing = []
    for path in sorted(glob.glob(os.path.join(DZ, "src/orespawn-dzvr28/OreSpawn/entities/*.java"))):
        text = open(path, encoding="utf8", errors="ignore").read()
        if "tower_defense_enable = true" in text:
            cls = os.path.basename(path)[:-5]
            entity = entity_id(cls)
            if entity:
                found.add(entity)
            else:
                missing.append(cls)
    print("tower defense", len(found), "not in this mod or the port:", missing)
    return sorted(found)


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
                crit = {"trigger": "minecraft:changed_dimension", "conditions": {"to": dim if ":" in dim else "orespawn:" + dim}}
        if crit is None:
            skipped.append((tid, title, f"{kind} {target}"))
            continue
        tasks.append(dict(id=tid, kind=kind, title=title, desc=desc or title, level=int(level), crit=crit, icon=icon))
    kill = lambda entity: {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]}}
    tasks += [
        dict(id="flag", kind="CompareCrafter", title="Bring It On", desc="Make a Tower Defense Flag.", level=4,
             crit={"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"{MOD}:tower_flag"}]}}, icon=f"{MOD}:tower_flag"),
        dict(id="trophy", kind="FoundOnGround", title="Bragging Rights", desc="Find a Trophy.", level=2,
             crit={"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"{MOD}:trophy"}]}}, icon=f"{MOD}:trophy"),
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


def element_noise(name, default_block, default_fluid, sea_level, surface):
    """Vanilla's overworld terrain with this mod's blocks, sea and surface."""
    base = json.load(open(os.path.join(HERE, "tools/overworld_noise_settings.json"), encoding="utf8"))
    base["default_block"] = {"Name": default_block}
    base["default_fluid"] = {"Name": default_fluid, "Properties": {"level": "0"}}
    base["sea_level"] = sea_level
    base["aquifers_enabled"] = False
    base["ore_veins_enabled"] = False
    base["spawn_target"] = base.get("spawn_target", [])
    if surface:
        base["surface_rule"] = surface
    write_json(os.path.join(DATA, f"worldgen/noise_settings/{name}.json"), base)


def biome(name, spawners, features, sky, fog, water, grass=None, temperature=0.8, downfall=0.4):
    effects = {"sky_color": sky, "fog_color": fog, "water_color": water, "water_fog_color": water, "mood_sound": {
        "sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}}
    if grass is not None:
        effects["grass_color"] = grass
        effects["foliage_color"] = grass
    categories = {"monster": [], "creature": [], "ambient": [], "water_creature": [], "underground_water_creature": [], "water_ambient": [], "misc": [], "axolotls": []}
    for category, entries in spawners.items():
        categories[category] = [{"type": e, "weight": w, "minCount": a, "maxCount": b} for e, w, a, b in entries]
    write_json(os.path.join(DATA, f"worldgen/biome/{name}.json"), {
        "has_precipitation": True, "temperature": temperature, "downfall": downfall, "effects": effects,
        "spawners": categories, "spawn_costs": {}, "carvers": {}, "features": features})


def dimension_type(name, min_y=-64, height=384, logical=384, bed=True, skylight=True):
    write_json(os.path.join(DATA, f"dimension_type/{name}.json"), {
        "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": bed, "has_raids": False,
        "has_skylight": skylight, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.0, "logical_height": logical,
        "effects": "minecraft:overworld", "infiniburn": "#minecraft:infiniburn_overworld", "min_y": min_y, "height": height,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}, "monster_spawn_block_light_limit": 0})


def placed(name, feature, placement):
    write_json(os.path.join(DATA, f"worldgen/placed_feature/{name}.json"), {"feature": feature, "placement": placement})


def surface_block(y_margin, block):
    return {"type": "minecraft:block", "result_state": {"Name": block}}


def milestone_three(lang):
    blocks = {
        "dark_stone": "Dark Stone", "dark_dirt": "Dark Dirt", "fossil_bone": "Fossilized Bone", "dragons_eye_stone": "Dragon's Eye Stone",
        "dargon_stone": "Dargon Stone", "dargon_stone_dark": "Dark Dargon Stone", "kraken_king_spawner": "It Sleeps",
    }
    bs = os.path.join(ASSETS, "blockstates")
    bm = os.path.join(ASSETS, "models/block")
    im = os.path.join(ASSETS, "models/item")
    loot_blocks = os.path.join(DATA, "loot_table/blocks")
    self_drop = lambda name: {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                                                                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]}

    def cube(name, texture=None):
        write_json(os.path.join(bs, name + ".json"), {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        write_json(os.path.join(bm, name + ".json"), {"parent": "minecraft:block/cube_all", "textures": {"all": texture or f"{MOD}:block/{name}"}})
        write_json(os.path.join(im, name + ".json"), {"parent": f"{MOD}:block/{name}"})

    def pillar(name, side, top):
        write_json(os.path.join(bs, name + ".json"), {"variants": {
            "axis=y": {"model": f"{MOD}:block/{name}"},
            "axis=x": {"model": f"{MOD}:block/{name}", "x": 90, "y": 90},
            "axis=z": {"model": f"{MOD}:block/{name}", "x": 90}}})
        write_json(os.path.join(bm, name + ".json"), {"parent": "minecraft:block/cube_column", "textures": {"end": top, "side": side}})
        write_json(os.path.join(im, name + ".json"), {"parent": f"{MOD}:block/{name}"})

    for name, title in blocks.items():
        cube(name, f"{MOD}:block/dargon_stone" if name == "kraken_king_spawner" else None)
        lang[f"block.{MOD}.{name}"] = title
        # The block that wakes the King gives nothing back, or it could wake him for ever.
        write_json(os.path.join(loot_blocks, name + ".json"), {"type": "minecraft:block", "pools": []} if name == "kraken_king_spawner" else self_drop(name))
    cube("doom_sand", "minecraft:block/sand")
    lang[f"block.{MOD}.doom_sand"] = "Doom Sand"
    write_json(os.path.join(loot_blocks, "doom_sand.json"), self_drop("doom_sand"))
    pillar("dark_log", f"{MOD}:block/dark_log", f"{MOD}:block/dark_log_top")
    lang[f"block.{MOD}.dark_log"] = "Dark Log"
    write_json(os.path.join(loot_blocks, "dark_log.json"), self_drop("dark_log"))
    pillar("doom_log", f"{MOD}:block/doom_log", f"{MOD}:block/doom_log")
    lang[f"block.{MOD}.doom_log"] = "Doom Tree Log"
    write_json(os.path.join(loot_blocks, "doom_log.json"), self_drop("doom_log"))
    for name, title in (("dark_leaves", "Dark Leaves"), ("doom_leaves", "Doom Tree Leaves")):
        write_json(os.path.join(bs, name + ".json"), {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        write_json(os.path.join(bm, name + ".json"), {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout", "textures": {"all": f"{MOD}:block/dark_leaves"}})
        write_json(os.path.join(im, name + ".json"), {"parent": f"{MOD}:block/{name}"})
        lang[f"block.{MOD}.{name}"] = title
    write_json(os.path.join(loot_blocks, "dark_leaves.json"), {"type": "minecraft:block", "pools": []})
    # Its leaves are where Worm Food comes from.
    write_json(os.path.join(loot_blocks, "doom_leaves.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:worm_food"}],
                                                                                                      "conditions": [{"condition": "minecraft:random_chance", "chance": 0.5}]}]})
    write_json(os.path.join(bs, "extreme_torch.json"), {"variants": {"": {"model": f"{MOD}:block/extreme_torch"}}})
    write_json(os.path.join(bm, "extreme_torch.json"), {"parent": "minecraft:block/template_torch", "render_type": "minecraft:cutout", "textures": {"torch": f"{MOD}:block/extreme_torch"}})
    write_json(os.path.join(im, "extreme_torch.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:block/extreme_torch"}})
    lang[f"block.{MOD}.extreme_torch"] = "Extreme Torch"
    write_json(os.path.join(loot_blocks, "extreme_torch.json"), self_drop("extreme_torch"))

    tags = os.path.join(DATA, "..", "minecraft", "tags")
    write_json(os.path.join(tags, "block/mineable/pickaxe.json"), {"values": [f"{MOD}:{b}" for b in ("dark_stone", "fossil_bone", "dragons_eye_stone", "dargon_stone", "dargon_stone_dark", "kraken_king_spawner")]})
    write_json(os.path.join(tags, "block/mineable/shovel.json"), {"values": [f"{MOD}:dark_dirt", f"{MOD}:doom_sand"]})
    write_json(os.path.join(tags, "block/mineable/axe.json"), {"values": [f"{MOD}:dark_log", f"{MOD}:doom_log"]})
    write_json(os.path.join(tags, "block/logs.json"), {"values": [f"{MOD}:dark_log", f"{MOD}:doom_log"]})
    write_json(os.path.join(tags, "block/leaves.json"), {"values": [f"{MOD}:dark_leaves", f"{MOD}:doom_leaves"]})

    models = os.path.join(ASSETS, "models/item")
    for item in ("earth_staff", "wind_staff", "fire_staff", "water_staff", "lightning_staff", "plant_staff"):
        write_json(os.path.join(models, item + ".json"), item_model(item, "minecraft:item/handheld"))
        lang[f"item.{MOD}.{item}"] = item.replace("_", " ").title()
    write_json(os.path.join(models, "worm_food.json"), item_model("worm_food"))
    lang[f"item.{MOD}.worm_food"] = "Worm Food"
    for egg in ("chaos_demon", "baby_sitter", "kraken_king", "squid_guard", "dargon", "element_dragon", "doom_worm", "doom_ant", "drant"):
        write_json(os.path.join(models, egg + "_spawn_egg.json"), item_model(egg + "_spawn_egg"))
    names = {"chaos_demon": "Chaos Demon", "baby_sitter": "The Baby Sitter", "kraken_king": "King of the Kraken", "squid_guard": "Squid Guard", "dargon": "Dargon",
             "element_dragon": "Element Dragon", "doom_worm": "Doom Worm", "doom_ant": "Doom Ant", "drant": "Drant"}
    for key, title in names.items():
        lang[f"entity.{MOD}.{key}"] = title
        lang[f"item.{MOD}.{key}_spawn_egg"] = title + " Egg"
    # DangerZone's own name for this egg.
    lang[f"item.{MOD}.kraken_king_spawn_egg"] = "Uh, seriously. No. Don't."
    lang[f"entity.{MOD}.delayed_event"] = "Something's Coming"
    lang[f"entity.{MOD}.element_shot"] = "Element Shot"
    lang[f"entity.{MOD}.purple_fireball"] = "Purple Fireball"

    recipes = os.path.join(DATA, "recipe")
    write_json(os.path.join(recipes, "extreme_torch.json"), {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:torch"}, {"item": f"{MOD}:dragons_eye"}], "result": {"id": f"{MOD}:extreme_torch", "count": 1}})
    write_json(os.path.join(recipes, "dragons_eye.json"), {"type": "minecraft:smelting", "category": "misc", "ingredient": {"item": f"{MOD}:dragons_eye_stone"},
        "result": {"id": f"{MOD}:dragons_eye"}, "experience": 2.0, "cookingtime": 200})

    tables = os.path.join(DATA, "loot_table/entities")
    write_json(os.path.join(tables, "chaos_demon.json"), loot(pool("minecraft:beef", 3, 8), pool("minecraft:bone", 3, 8), pool("orespawn:ruby", 2, 6), pool("minecraft:diamond", 1, 3)))
    foods = ["minecraft:bread", "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:cooked_chicken", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cake", "minecraft:golden_carrot", "orespawn:crabby_patty", "orespawn:cooked_crab_meat"]
    write_json(os.path.join(tables, "baby_sitter.json"), loot({"rolls": {"type": "minecraft:uniform", "min": 15, "max": 30}, "entries": [{"type": "minecraft:item", "name": f} for f in foods if f.split(":")[0] != "orespawn" or f.split(":")[1] in PORT_ITEMS]}))
    write_json(os.path.join(tables, "squid_guard.json"), loot(pool("minecraft:ink_sac", 1, 3), pool("orespawn:raw_crab_meat", 1, 3)))
    rare = [r for r in ("worm_tooth", "kraken_tooth", "vortex_eye", "ultimate_sword", "ultimate_bow", "trex_tooth", "sea_monster_scale", "nightmare_scale", "queen_scale", "godzilla_scale") if r in PORT_ITEMS]
    write_json(os.path.join(tables, "kraken_king.json"), loot(pool(f"{MOD}:dimension_staff"), pool(f"{MOD}:kraken_king_spawn_egg", chance=0.25), pool("minecraft:cod", 60, 99), pool("minecraft:feather", 20, 40), pool("minecraft:bone", 20, 40),
        {"rolls": {"type": "minecraft:uniform", "min": 8, "max": 16}, "entries": [{"type": "minecraft:item", "name": "orespawn:" + r} for r in rare]}))
    write_json(os.path.join(tables, "dargon.json"), loot(pool(f"{MOD}:dragons_eye", 4, 8), pool("orespawn:queen_scale", 40, 55), pool("minecraft:diamond", 16, 32),
        {"rolls": {"type": "minecraft:uniform", "min": 8, "max": 16}, "entries": [{"type": "minecraft:item", "name": "orespawn:" + r} for r in rare]}))
    write_json(os.path.join(tables, "element_dragon.json"), loot(pool("minecraft:beef", 3, 8), pool("minecraft:bone", 2, 6), pool("minecraft:feather", 2, 6)))
    write_json(os.path.join(tables, "doom_worm.json"), loot(pool("minecraft:beef", 40, 79), pool("minecraft:bone", 20, 39), pool(f"{MOD}:doom_worm_spawn_egg", 1, 2), pool("orespawn:worm_tooth", 2, 5), pool("orespawn:ruby", 20, 39), pool("orespawn:amethyst_gem", 20, 39), pool("orespawn:ingot_titanium", 20, 39), pool("orespawn:ingot_uranium", 20, 39)))
    for ant in ("doom_ant", "drant"):
        write_json(os.path.join(tables, ant + ".json"), loot(pool("minecraft:string", chance=0.2)))

    # The castle's chests. DangerZone's Gem list works out at about forty staffs a castle: here, one or two.
    chests = os.path.join(DATA, "loot_table/chests")
    staffs = [f"{MOD}:{s}" for s in ("earth_staff", "wind_staff", "fire_staff", "water_staff", "lightning_staff", "plant_staff")]
    entries = lambda items: [{"type": "minecraft:item", "name": i} for i in items]
    write_json(os.path.join(chests, "gemstuff.json"), {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 7}, "entries": entries(["minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:amethyst_shard", "orespawn:ruby", "orespawn:amethyst_gem", "orespawn:ingot_titanium", "orespawn:ingot_uranium"])},
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.03}], "entries": entries(staffs)}]})
    write_json(os.path.join(chests, "foodstuff.json"), {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 8}, "entries": entries([f for f in foods if f.split(":")[0] != "orespawn" or f.split(":")[1] in PORT_ITEMS])}]})
    write_json(os.path.join(chests, "elementstuff.json"), {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 7}, "entries": entries(["minecraft:diamond", "minecraft:gold_ingot", "minecraft:blaze_rod", "minecraft:ender_pearl", f"{MOD}:dragons_eye"])},
        {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.25}], "entries": entries(staffs)}]})

    # ---- the dimensions
    dimension_type("realm")
    dimension_type("doom", min_y=0, height=256, logical=256)
    bedrock = {"type": "minecraft:condition", "if_true": {"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
               "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}}, "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:bedrock"}}}
    dark_surface = {"type": "minecraft:sequence", "sequence": [bedrock, {"type": "minecraft:condition", "if_true": {"type": "minecraft:above_preliminary_surface"}, "then_run": {
        "type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 3, "surface_type": "floor", "add_surface_depth": True, "secondary_depth_range": 0},
        "then_run": {"type": "minecraft:block", "result_state": {"Name": f"{MOD}:dark_dirt"}}}}]}
    element_noise("dragons_lair", f"{MOD}:dark_stone", "minecraft:water", 60, dark_surface)
    element_noise("kraken_realm", "minecraft:stone", "minecraft:water", 70, None)

    tree = lambda log, leaves, height: {"type": "minecraft:tree", "config": {
        "ignore_vines": True, "force_dirt": False, "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{MOD}:dark_dirt"}},
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{MOD}:{log}", "Properties": {"axis": "y"}}},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{MOD}:{leaves}", "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}},
        "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": height, "height_rand_a": 2, "height_rand_b": 2},
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 3},
        "decorators": []}}
    configured = os.path.join(DATA, "worldgen/configured_feature")
    write_json(os.path.join(configured, "dark_tree.json"), tree("dark_log", "dark_leaves", 7))
    write_json(os.path.join(configured, "doom_tree.json"), tree("doom_log", "doom_leaves", 3))
    write_json(os.path.join(configured, "fossil_cross.json"), {"type": f"{MOD}:fossil_cross", "config": {}})
    write_json(os.path.join(configured, "element_pillar.json"), {"type": f"{MOD}:element_pillar", "config": {}})
    surface = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]
    survives = lambda block: {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": {"Name": block}}}
    placed("dark_tree", f"{MOD}:dark_tree", [{"type": "minecraft:rarity_filter", "chance": 6}] + surface)
    placed("doom_tree", f"{MOD}:doom_tree", [{"type": "minecraft:rarity_filter", "chance": 32}] + surface)
    placed("fossil_cross", f"{MOD}:fossil_cross", [{"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 45}}}, {"type": "minecraft:biome"}])
    placed("element_pillar", f"{MOD}:element_pillar", [{"type": "minecraft:rarity_filter", "chance": 150}] + surface)
    # The generation steps a biome lists are by index; decoration goes where trees and underground ornaments go.
    steps = lambda under, veg: [[], [], [], [], under, [], [], [], [], veg]
    # The Kraken, Attack Squid and Terrible Terror come by way of ExtraSpawns: the port's spawn rules turn these places down.
    # Its monsters come by way of ExtraSpawns too: on a list of its own, a Squid Guard filled the whole mob cap.
    biome("kraken_realm", {"creature": [("orespawn_dangerzone:doom_ant", 1, 1, 1)]},
          [], 0x78A7FF, 0xC0D8FF, 0x3F76E4, grass=0x79C05A)
    # Its monsters are in the air, by way of ExtraSpawns.
    biome("dragons_lair", {"creature": [("orespawn_dangerzone:drant", 3, 1, 2)]},
          steps([f"{MOD}:fossil_cross"], [f"{MOD}:dark_tree", f"{MOD}:element_pillar"]), 0x5959BF, 0x2A2A40, 0x20203F, grass=0x202030)
    # The worm comes by way of ExtraSpawns: as the only monster on a list it filled the whole mob cap, fifteen boss worms at once.
    biome("doom", {"creature": [("orespawn_dangerzone:doom_ant", 4, 1, 2)]},
          steps([], [f"{MOD}:doom_tree"]), 0xC9B27A, 0xD8C690, 0x3F76E4, temperature=2.0, downfall=0.0)
    dim = os.path.join(DATA, "dimension")
    write_json(os.path.join(dim, "kraken_realm.json"), {"type": f"{MOD}:realm", "generator": {"type": "minecraft:noise", "settings": f"{MOD}:kraken_realm", "biome_source": {"type": "minecraft:fixed", "biome": f"{MOD}:kraken_realm"}}})
    write_json(os.path.join(dim, "dragons_lair.json"), {"type": f"{MOD}:realm", "generator": {"type": "minecraft:noise", "settings": f"{MOD}:dragons_lair", "biome_source": {"type": "minecraft:fixed", "biome": f"{MOD}:dragons_lair"}}})
    write_json(os.path.join(dim, "doom.json"), {"type": f"{MOD}:doom", "generator": {"type": "minecraft:flat", "settings": {"biome": f"{MOD}:doom", "lakes": False, "features": True, "structure_overrides": [],
        "layers": [{"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:obsidian", "height": 2}, {"block": f"{MOD}:doom_sand", "height": 47}]}}})

    def spawns(name, biomes, *entries):
        write_json(os.path.join(DATA, f"neoforge/biome_modifier/{name}.json"), {"type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": f"{MOD}:{e}", "weight": w, "minCount": a, "maxCount": b} for e, w, a, b in entries]})

    # Dargon, like the UFO, is too big for the game's spawn loop: see ExtraSpawns.
    spawns("chaos_m3", "orespawn:chaos_biome", ("chaos_demon", 2, 1, 1))
    spawns("windswept_m3", "#c:is_windswept", ("chaos_demon", 1, 1, 1), ("drant", 2, 1, 2))
    spawns("portal_ants", "#minecraft:is_overworld", ("doom_ant", 3, 1, 3))


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
        "entity.orespawn_dangerzone.tower_flag": "Tower Defense Flag",
        "entity.orespawn_dangerzone.tower_flag.wave": "Wave %s of %s: %s",
        "entity.orespawn_dangerzone.trophy": "Trophy",
        "entity.orespawn_dangerzone.nope": "NOPE!!!",
        "entity.orespawn_dangerzone.baby_nope": "Baby Nope",
        "entity.orespawn_dangerzone.princess_teen": "Teen Princess",
        "entity.orespawn_dangerzone.princess_young_adult": "Young Adult Princess",
        "entity.orespawn_dangerzone.volcano": "Volcano",
        "entity.orespawn_dangerzone.ufo": "UFO",
        "entity.orespawn_dangerzone.ufo_carrier": "UFO Carrier",
        "entity.orespawn_dangerzone.ufo_fighter": "UFO Fighter",
        "entity.orespawn_dangerzone.martian": "Martian",
        "item.orespawn_dangerzone.tower_flag": "Tower Defense Flag",
        "item.orespawn_dangerzone.trophy": "Trophy",
        "item.orespawn_dangerzone.trophy.of": "%s Trophy",
        "item.orespawn_dangerzone.nope_spawn_egg": "NOPE!!! Egg",
        "item.orespawn_dangerzone.baby_nope_spawn_egg": "Baby Nope Egg",
        "item.orespawn_dangerzone.princess_teen_spawn_egg": "Teen Princess Egg",
        "item.orespawn_dangerzone.princess_young_adult_spawn_egg": "Young Adult Princess Egg",
        "item.orespawn_dangerzone.volcano_spawn_egg": "Volcano Egg",
        "item.orespawn_dangerzone.ufo_spawn_egg": "UFO Egg",
        "item.orespawn_dangerzone.ufo_carrier_spawn_egg": "UFO Carrier Egg",
        "item.orespawn_dangerzone.ufo_fighter_spawn_egg": "UFO Fighter Egg",
        "item.orespawn_dangerzone.martian_spawn_egg": "Martian Egg",
        "swarm.orespawn_dangerzone.0": "What is that sound.....?",
        "swarm.orespawn_dangerzone.1": SWARM_LINES[1],
        "swarm.orespawn_dangerzone.2": SWARM_LINES[2],
        "swarm.orespawn_dangerzone.3": SWARM_LINES[3],
        "swarm.orespawn_dangerzone.4": SWARM_LINES[4],
    }
    models = os.path.join(ASSETS, "models/item")
    write_json(os.path.join(models, "dragons_eye.json"), item_model("dragons_eye"))
    write_json(os.path.join(models, "shrink_ray.json"), item_model("shrink_ray", "minecraft:item/handheld"))
    write_json(os.path.join(models, "dimension_staff.json"), item_model("dimension_staff", "minecraft:item/handheld"))
    write_json(os.path.join(models, "dragons_eye_sword.json"), item_model("dragons_eye_sword", "minecraft:item/handheld"))
    for egg in ("nastysaurus_prime", "needlenose", "ant_queen", "bloodlust_ant", "nope", "baby_nope", "ufo", "ufo_carrier", "ufo_fighter", "martian"):
        write_json(os.path.join(models, f"{egg}_spawn_egg.json"), item_model(f"{egg}_spawn_egg"))
    for egg in ("nurse_bug", "princess_teen", "princess_young_adult", "volcano"):
        write_json(os.path.join(models, f"{egg}_spawn_egg.json"), {"parent": "minecraft:item/template_spawn_egg"})
    write_json(os.path.join(models, "tower_flag.json"), item_model("tower_flag"))
    write_json(os.path.join(models, "trophy.json"), item_model("trophy"))

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

    write_json(os.path.join(recipes, "tower_flag.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["B", "D", "D"],
        "key": {"B": {"tag": "minecraft:banners"}, "D": {"item": "minecraft:diamond_block"}},
        "result": {"id": f"{MOD}:tower_flag", "count": 1}})
    # DangerZone pays out 80 to 110 of each of its three coins. There are no coins here, so the worth comes as gems and ingots.
    write_json(os.path.join(DATA, "loot_table/gameplay/tower_flag_reward.json"), {"type": "minecraft:empty", "pools": [
        pool("minecraft:emerald", 20, 32), pool("minecraft:diamond", 8, 16), pool("minecraft:gold_ingot", 16, 32),
        pool("orespawn:ruby", 8, 16), pool("orespawn:ingot_titanium", 4, 8), pool("orespawn:ingot_uranium", 4, 8)]})

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

    write_json(os.path.join(tables, "tower_flag.json"), loot(pool(f"{MOD}:tower_flag")))
    # As the port's Teen and Adult Prince leave his egg.
    write_json(os.path.join(tables, "princess_teen.json"), loot(pool("orespawn:the_princess_spawn_egg")))
    write_json(os.path.join(tables, "princess_young_adult.json"), loot(pool("orespawn:the_princess_spawn_egg")))
    write_json(os.path.join(tables, "baby_nope.json"), loot(pool("minecraft:spider_eye", 1, 2)))
    nope_eggs = [f"{MOD}:nope_spawn_egg", f"{MOD}:baby_nope_spawn_egg"]
    nope = [pool("orespawn:raw_crab_meat", 50, 99), pool("minecraft:bone", 37, 62), pool(nope_eggs[0], chance=0.25), pool(nope_eggs[1], 2, 5)]
    for rare in ("worm_tooth", "kraken_tooth", "vortex_eye", "ultimate_sword", "trex_tooth", "sea_monster_scale", "nightmare_scale", "jumpy_bug_scale",
                 "moth_scale", "basilisk_scale", "water_dragon_scale", "emperor_scorpion_scale", "caterkiller_jaw", "big_bertha"):
        if rare in PORT_ITEMS:
            nope.append(pool("orespawn:" + rare, chance=0.5))
    write_json(os.path.join(tables, "nope.json"), loot(*nope))
    write_json(os.path.join(tables, "ufo.json"), loot(pool("orespawn:ray_gun", chance=0.25), pool("minecraft:iron_ingot", 8, 16), pool("minecraft:redstone", 8, 16)))
    write_json(os.path.join(tables, "ufo_fighter.json"), loot(pool("orespawn:ray_gun", chance=0.1), pool("minecraft:iron_ingot", 1, 3)))
    write_json(os.path.join(tables, "ufo_carrier.json"), loot(pool("minecraft:iron_ingot", 3, 6), pool("minecraft:redstone", 2, 5)))
    write_json(os.path.join(tables, "martian.json"), loot(pool("orespawn:ray_gun", chance=0.05), pool("minecraft:arrow", 1, 4)))

    write_json(os.path.join(DATA, "tags/entity_type/tower_defense.json"), {"values": [{"id": e, "required": False} for e in tower_defense()]})
    optional = lambda name: {"id": name, "required": False}
    write_json(os.path.join(DATA, "tags/entity_type/no_trophy.json"), {"values": [optional("orespawn:" + e) for e in (
        "island", "island_too", "elevator", "skate", "coin", "tshirt", "rock_base", "king_head", "queen_head", "godzilla_head", "butterfly", "cage", "shoes")]
        + [f"{MOD}:tower_flag", f"{MOD}:volcano", f"{MOD}:delayed_event"]})
    write_json(os.path.join(DATA, "tags/entity_type/shrink_immune.json"), {"values": [
        f"{MOD}:nastysaurus_prime", f"{MOD}:ant_queen", f"{MOD}:nope", f"{MOD}:ufo", f"{MOD}:tower_flag", f"{MOD}:volcano",
        f"{MOD}:princess_young_adult", f"{MOD}:kraken_king", f"{MOD}:dargon", f"{MOD}:doom_worm", f"{MOD}:baby_sitter", f"{MOD}:chaos_demon", f"{MOD}:element_dragon", "minecraft:wither", "minecraft:ender_dragon", {"id": "#c:bosses", "required": False}]
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
    # DangerZone keeps Nopes and UFOs in its Misfit dimension and volcanoes in its mountains, which the port doesn't have.
    add_spawns("chaos_m2", "orespawn:chaos_biome", ("nope", 1, 1, 1), ("volcano", 1, 1, 1))
    add_spawns("mining_m2", "orespawn:mining_biome", ("volcano", 1, 1, 1))
    add_spawns("forests", "#minecraft:is_forest", ("nurse_bug", 1, 1, 1))

    milestone_three(lang)
    milestone_four(lang, add_spawns)
    advancements(lang)
    write_json(os.path.join(ASSETS, "lang/en_us.json"), lang)


main()
