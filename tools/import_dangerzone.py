#!/usr/bin/env python3
"""Imports the original DangerZone assets into this mod.

usage: import_dangerzone.py <dangerzone root>

The root holds the DZVR 2.8 download (dl/dzvr-2.8/DZVR2.8.zip) and the decompiled mod
(src/orespawn-dzvr28/OreSpawn). Textures are copied unchanged, sounds are converted from WAV to
Ogg Vorbis with ffmpeg, and each DangerZone model becomes a Java model class that keeps the
original geometry and animation code.
"""
import io
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

DZ = sys.argv[1]
HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = "orespawn_dangerzone"
ASSETS = os.path.join(HERE, "src/main/resources/assets", MOD)
MODELS = os.path.join(HERE, "src/main/java/danger/orespawn/dangerzone/client/model")
ENTITIES = os.path.join(DZ, "src/orespawn-dzvr28/OreSpawn/entities")

# original texture -> this mod's texture
TEXTURES = {
    "orespawnres/skins/NastysaurusPrimetexture.png": "entity/nastysaurus_prime.png",
    "orespawnres/skins/Pointytexture.png": "entity/needlenose.png",
    "orespawnres/skins/Mantistexture2.png": "entity/nurse_bug.png",
    "orespawnres/skins/AntQueentexture.png": "entity/ant_queen.png",
    "orespawnres/skins/BloodlustAnttexture.png": "entity/bloodlust_ant.png",
    "orespawnres/items/dimensionstaff.png": "item/dimension_staff.png",
    "orespawnres/items/dragonseyesword.png": "item/dragons_eye_sword.png",
    "orespawnres/items/itemdragoneye.png": "item/dragons_eye.png",
    "orespawnres/items/ShrinkRay.png": "item/shrink_ray.png",
    "orespawnres/items/eggnastysaurusprime.png": "item/nastysaurus_prime_spawn_egg.png",
    "orespawnres/items/eggneedlenose.png": "item/needlenose_spawn_egg.png",
    "orespawnres/items/eggantqueen.png": "item/ant_queen_spawn_egg.png",
    "orespawnres/items/eggbloodlustant.png": "item/bloodlust_ant_spawn_egg.png",
    "orespawnres/skins/Nopetexture.png": "entity/nope.png",
    "orespawnres/skins/BabyNopetexture.png": "entity/baby_nope.png",
    "orespawnres/skins/ThePrincessTeentexture.png": "entity/princess_teen.png",
    "orespawnres/skins/ThePrincessTeentexture2.png": "entity/princess_teen_angry.png",
    "orespawnres/skins/ThePrincessAdulttexture.png": "entity/princess_young_adult.png",
    "orespawnres/skins/ThePrincessAdulttexture2.png": "entity/princess_young_adult_angry.png",
    "orespawnres/skins/UFOtexture.png": "entity/ufo.png",
    "orespawnres/skins/UFO_Carriertexture.png": "entity/ufo_carrier.png",
    "orespawnres/skins/UFO_Fightertexture.png": "entity/ufo_fighter.png",
    "orespawnres/items/eggnope.png": "item/nope_spawn_egg.png",
    "orespawnres/items/eggbabynope.png": "item/baby_nope_spawn_egg.png",
    "orespawnres/items/eggufo.png": "item/ufo_spawn_egg.png",
    "orespawnres/items/eggufocarrier.png": "item/ufo_carrier_spawn_egg.png",
    "orespawnres/items/eggufofighter.png": "item/ufo_fighter_spawn_egg.png",
    "orespawnres/skins/ChaosDemontexture.png": "entity/chaos_demon.png",
    "orespawnres/skins/BabySittertexture.png": "entity/baby_sitter.png",
    "orespawnres/skins/KrakenKingtexture.png": "entity/kraken_king.png",
    "orespawnres/skins/SquidGuardtexture.png": "entity/squid_guard.png",
    "orespawnres/skins/Dargontexture.png": "entity/dargon.png",
    "orespawnres/skins/DoomWormtexture.png": "entity/doom_worm.png",
    "orespawnres/skins/doomant.png": "entity/doom_ant.png",
    "orespawnres/skins/ElementDragontexture.png": "entity/element_dragon.png",
    "orespawnres/skins/EarthElementDragontexture.png": "entity/element_dragon_earth.png",
    "orespawnres/skins/WindElementDragonTexture.png": "entity/element_dragon_wind.png",
    "orespawnres/skins/FireElementDragontexture.png": "entity/element_dragon_fire.png",
    "orespawnres/skins/WaterElementDragontexture.png": "entity/element_dragon_water.png",
    "orespawnres/skins/LightningElementDragontexture.png": "entity/element_dragon_lightning.png",
    "orespawnres/skins/PlantElementDragontexture.png": "entity/element_dragon_plant.png",
    "orespawnres/skins/%s" % "Drant.png": "entity/drant.png",
    "orespawnres/items/earthstaff.png": "item/earth_staff.png",
    "orespawnres/items/firestaff.png": "item/fire_staff.png",
    "orespawnres/items/lightningstaff.png": "item/lightning_staff.png",
    "orespawnres/items/plantstaff.png": "item/plant_staff.png",
    "orespawnres/items/waterstaff.png": "item/water_staff.png",
    "orespawnres/items/windstaff.png": "item/wind_staff.png",
    "orespawnres/items/wormfood.png": "item/worm_food.png",
    "orespawnres/items/eggbabysitter.png": "item/baby_sitter_spawn_egg.png",
    "orespawnres/items/eggchaosdemon.png": "item/chaos_demon_spawn_egg.png",
    "orespawnres/items/eggdargon.png": "item/dargon_spawn_egg.png",
    "orespawnres/items/eggdoomant.png": "item/doom_ant_spawn_egg.png",
    "orespawnres/items/eggdoomworm.png": "item/doom_worm_spawn_egg.png",
    "orespawnres/items/eggdrant.png": "item/drant_spawn_egg.png",
    "orespawnres/items/eggelementdragon.png": "item/element_dragon_spawn_egg.png",
    "orespawnres/items/eggkrakenking.png": "item/kraken_king_spawn_egg.png",
    "orespawnres/items/eggsquidguard.png": "item/squid_guard_spawn_egg.png",
    "orespawnres/blocks/darkstone.png": "block/dark_stone.png",
    "orespawnres/blocks/darkdirt.png": "block/dark_dirt.png",
    "orespawnres/blocks/darkwood.png": "block/dark_log.png",
    "orespawnres/blocks/darkwood_top.png": "block/dark_log_top.png",
    "orespawnres/blocks/darkleaves.png": "block/dark_leaves.png",
    "orespawnres/blocks/fossilbone.png": "block/fossil_bone.png",
    "orespawnres/blocks/dragoneye.png": "block/dragons_eye_stone.png",
    "orespawnres/blocks/extremetorch.png": "block/extreme_torch.png",
    "orespawnres/blocks/duplicatortreelog.png": "block/doom_log.png",
    "orespawnres/blocks/dargonstone1.png": "block/dargon_stone.png",
    "orespawnres/blocks/dargonstone2.png": "block/dargon_stone_dark.png",
}
# from the game itself rather than its OreSpawn mod
ENGINE_TEXTURES = {
    "res/skins/Flagtexture.png": "entity/tower_flag.png",
    "res/skins/Martiantexture.png": "entity/martian.png",
    "res/items/flag.png": "item/tower_flag.png",
    "res/items/trophy.png": "item/trophy.png",
    "res/items/eggmartian.png": "item/martian_spawn_egg.png",
}
MOD_SOUNDS = [
    "warthog_living1", "warthog_living2", "warthog_living3", "warthog_hit1", "warthog_hit2", "warthog_hit3",
    "lurkingterror_dead", "dragonfly_hurt", "beebuzz", "nope_death", "nope_hit", "scorpion_attack",
    "clatter1", "clatter2", "crunch", "emperorscorpion_death",
    "laser1", "laser2", "laser3", "laser4", "laser5", "laser6", "nope_living",
    "roar1", "roar2", "roar3", "roar4", "roar5", "roar6", "mothrawings1", "mothrawings2", "mothrawings3",
]
GAME_SOUNDS = ["teleport1", "teleport2", "teleport3", "goose_living", "martian_hit", "martian_death"]
LATE_MOD_SOUNDS = [
    "babysitter_death1", "babysitter_death2", "babysitter_hit1", "babysitter_hit2", "babysitter_hit3", "babysitter_hit4", "babysitter_hit5",
    "babysitter_hit6", "babysitter_hit7", "babysitter_hit8", "babysitter_hit9", "babysitter_living1", "babysitter_living2",
    "elementdragon_death", "elementdragon_hit", "elementdragon_living",
    "caterkiller_living1", "caterkiller_living2", "caterkiller_living3", "caterkiller_living4",
]
# sound event -> its variants
SOUND_EVENTS = {
    "needlenose.ambient": ["warthog_living1", "warthog_living2", "warthog_living3"],
    "needlenose.hurt": ["warthog_hit1", "warthog_hit2", "warthog_hit3"],
    "needlenose.death": ["lurkingterror_dead"],
    "nurse_bug.ambient": ["beebuzz"],
    "nurse_bug.hurt": ["dragonfly_hurt"],
    "ant_queen.hurt": ["nope_hit"],
    "ant_queen.death": ["nope_death"],
    "ant_queen.attack": ["scorpion_attack"],
    "bloodlust_ant.ambient": ["clatter1", "clatter2"],
    "bloodlust_ant.attack": ["crunch"],
    "bloodlust_ant.death": ["emperorscorpion_death"],
    "shrink_ray.shoot": ["laser1", "laser2", "laser3", "laser4", "laser5", "laser6"],
    "item.teleport": ["teleport1", "teleport2", "teleport3"],
    "duct_tape.use": ["goose_living"],
    "nope.ambient": ["nope_living"],
    "martian.hurt": ["martian_hit"],
    "martian.death": ["martian_death"],
    "baby_sitter.ambient": ["babysitter_living1", "babysitter_living2"],
    "baby_sitter.hurt": ["babysitter_hit1", "babysitter_hit2", "babysitter_hit3", "babysitter_hit4", "babysitter_hit5", "babysitter_hit6", "babysitter_hit7", "babysitter_hit8", "babysitter_hit9"],
    "baby_sitter.death": ["babysitter_death1", "babysitter_death2"],
    "element_dragon.ambient": ["elementdragon_living"],
    "element_dragon.hurt": ["elementdragon_hit"],
    "element_dragon.death": ["elementdragon_death"],
    "doom_worm.ambient": ["caterkiller_living1", "caterkiller_living2", "caterkiller_living3", "caterkiller_living4"],
    "princess.roar": ["roar1", "roar2", "roar3", "roar4", "roar5", "roar6"],
    "princess.wings": ["mothrawings1", "mothrawings2", "mothrawings3"],
}
MODEL_CLASSES = {
    "ModelNastysaurusPrime": "DzNastysaurusPrimeModel",
    "ModelNeedlenose": "DzNeedlenoseModel",
    "ModelMantis": "DzNurseBugModel",
    "ModelAntQueen": "DzAntQueenModel",
    "ModelBloodlustAnt": "DzBloodlustAntModel",
    "ModelNope": "DzNopeModel",
    "ModelBabyNope": "DzBabyNopeModel",
    "ModelUFO": "DzUfoModel",
    "ModelUFOCarrier": "DzUfoCarrierModel",
    "ModelUFOFighter": "DzUfoFighterModel",
    "ModelThePrincessTeen": "DzPrincessTeenModel",
    "ModelThePrincessAdult": "DzPrincessYoungAdultModel",
    "ModelChaosDemon": "DzChaosDemonModel",
    "ModelBabySitter": "DzBabySitterModel",
    "ModelKrakenKing": "DzKrakenKingModel",
    "ModelSquidGuard": "DzSquidGuardModel",
    "ModelDargon": "DzDargonModel",
    "ModelElementDragon": "DzElementDragonModel",
    "ModelDoomWorm": "DzDoomWormModel",
    "ModelDrant": "DzDrantModel",
    "ModelAnt": "DzAntModel",
}
ENGINE_MODEL_CLASSES = {
    "ModelFlag": "DzTowerFlagModel",
    "ModelMartian": "DzMartianModel",
}
# Models whose render method calls helper methods of their own, which come along.
KEEP_HELPERS = {"ModelThePrincessAdult", "ModelKrakenKing", "ModelSquidGuard", "ModelDargon", "ModelChaosDemon", "ModelBabySitter", "ModelElementDragon"}
# The real classes behind a model's cast of its entity.
ENTITY_CASTS = {"NastysaurusPrime": "NastysaurusPrime", "ChaosDemon": "ChaosDemon", "KrakenKing": "KrakenKing", "SquidGuard": "SquidGuard", "Dargon": "Dargon", "ElementDragon": "ElementDragon", "DoomWorm": "DoomWorm"}
# DangerZone applies this model's extra scale only to other creatures that borrow it.
SCALE_OVERRIDE = {"ModelThePrincessAdult": 1.0}


def strip_method(text, start):
    at = text.find(start)
    if at < 0:
        return text
    depth, i = 0, text.index("{", at)
    for j in range(i, len(text)):
        depth += (text[j] == "{") - (text[j] == "}")
        if depth == 0:
            return text[:at] + text[j + 1 :]
    return text


def strip_block(text, start):
    """Removes a whole `if (...) { ... }` block whose opening starts with the given text."""
    while True:
        at = text.find(start)
        if at < 0:
            return text
        depth, i = 0, text.index("{", at)
        for j in range(i, len(text)):
            depth += (text[j] == "{") - (text[j] == "}")
            if depth == 0:
                text = text[:at] + text[j + 1:]
                break


def number(text):
    """Evaluates a decompiled constant such as (float) (-Math.PI * 2.0 / 9.0)."""
    text = text.replace("(float)", "").replace("(double)", "").replace("Math.PI", repr(math.pi))
    text = re.sub(r"(?<=[0-9.])[Ff]\b", "", text)
    return float(eval(text, {"__builtins__": {}}))  # the text is this project's own decompiled constants


def matmul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(3)) for c in range(3)] for r in range(3)]


def rot_mc(x, y, z):
    """A part's rotation as ModelPart applies it: R = Rz * Ry * Rx. DangerZone's renderer comes out the same."""
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return matmul(rz, matmul(ry, rx))


def to_zyx(m):
    """Euler angles for ModelPart's own order, R = Rz * Ry * Rx."""
    y = math.asin(max(-1.0, min(1.0, -m[2][0])))
    if abs(math.cos(y)) > 1e-6:
        x = math.atan2(m[2][1], m[2][2])
        z = math.atan2(m[1][0], m[0][0])
    else:
        x = math.atan2(-m[1][2], m[1][1])
        z = 0.0
    return x, y, z


def convert_model(source_name, class_name, folder=ENTITIES):
    src = open(os.path.join(folder, source_name + ".java"), encoding="utf8", errors="ignore").read()
    ctor = re.search(r"public " + source_name + r"\(\s*\)", src)
    ctor = src[ctor.start() :]
    ctor = ctor[: ctor.index("public void render(")]
    # the game's own models leave off "this." and write the angles in their own helper
    ctor = re.sub(r"(?m)^(\s*)(\w+)(\s*=\s*new ModelRenderer|\.addBox|\.setRotationPoint|\.setTextureSize|\.mirror|\.connectPart|\.finishConnect)", r"\1this.\2\3", ctor)
    ctor = re.sub(r"setRotation\((\w+),", r"setRotation(this.\1,", ctor).replace("this.this.", "this.")
    ctor = re.sub(r"(?<![\w.])this\.setRotation\(", "setRotation(", ctor).replace("setRotation(", "this.setRotation(")
    ctor = re.sub(r"connectPart\((\w+)\)", r"connectPart(this.\1)", ctor).replace("this.this.", "this.")
    parts = {}

    def part(name):
        return parts.setdefault(name, {"name": name, "children": []})

    fields = re.findall(r"ModelRenderer (\w+) = new ModelRenderer\(this, (-?\d+), (-?\d+)\)", src)
    fields += re.findall(r"this\.(\w+)\s*=\s*new ModelRenderer\(this, (-?\d+), (-?\d+)\)", ctor)
    for name, u, v in fields:
        part(name).update(u=int(u), v=int(v), pos=(0.0, 0.0, 0.0), rot=(0.0, 0.0, 0.0))
    for name, args in re.findall(r"this\.(\w+)\.addBox\(([^;]+)\);", ctor):
        part(name)["box"] = [number(a) for a in args.split(",")]
    for name, args in re.findall(r"this\.(\w+)\.setRotationPoint\(([^;]+)\);", ctor):
        part(name)["pos"] = tuple(number(a) for a in args.split(","))
    for name, args in re.findall(r"this\.setRotation\(this\.(\w+), ([^;]+)\);", ctor):
        part(name)["rot"] = tuple(number(a) for a in args.split(","))
    missing = [n for n, p in parts.items() if "u" not in p or "box" not in p]
    if missing:
        raise SystemExit(f"{source_name}: unparsed parts {missing}")
    size = re.search(r"setTextureSize\((\d+), (\d+)\)", ctor).groups()
    connected = {}
    for parent, child in re.findall(r"this\.(\w+)\.connectPart\(this\.(\w+)\)", ctor):
        parts[parent]["children"].append(child)
        connected[child] = parent

    scale_m = re.search(r"MatrixStack\.scale\(([\d.]+)F", src)
    scale = SCALE_OVERRIDE.get(source_name, float(scale_m.group(1)) if scale_m else 1.0)

    def box_code(p):
        b = p["box"]
        return "CubeListBuilder.create().texOffs(%d, %d).addBox(%sF, %sF, %sF, %sF, %sF, %sF)" % (
            p["u"], p["v"], *[repr(x) for x in b])

    layer = []
    fields_decl, ctor_code = [], []
    for name, p in parts.items():
        fields_decl.append("    private final ModelPart %s;" % name)
        if name in connected:
            # part of its parent's rigid group, so its own pose never moves anything
            ctor_code.append("        this.%s = detached(%sF, %sF, %sF, %sF, %sF, %sF);" % (
                name, *[repr(x) for x in p["pos"]], *[repr(x) for x in p["rot"]]))
            continue
        ctor_code.append('        this.%s = root.getChild("%s");' % (name, name))
        layer.append(
            '        PartDefinition %s_def = root.addOrReplaceChild("%s", %s, PartPose.offsetAndRotation(%sF, %sF, %sF, %sF, %sF, %sF));'
            % (name, name, box_code(p), *[repr(x) for x in p["pos"]], *[repr(x) for x in p["rot"]])
        )
        for child in p["children"]:
            c = parts[child]
            inv = [list(r) for r in zip(*rot_mc(*p["rot"]))]
            d = [c["pos"][i] - p["pos"][i] for i in range(3)]
            off = [sum(inv[r][k] * d[k] for k in range(3)) for r in range(3)]
            rel = matmul(inv, rot_mc(*c["rot"]))
            ex, ey, ez = to_zyx(rel)
            layer.append(
                '        %s_def.addOrReplaceChild("%s", %s, PartPose.offsetAndRotation(%sF, %sF, %sF, %sF, %sF, %sF));'
                % (name, child, box_code(c), *[repr(round(x, 5)) for x in off], *[repr(round(x, 5)) for x in (ex, ey, ez)])
            )

    body = re.search(r"public void render\(\s*Entity entity", src)
    body = src[body.start() :]
    signature = body[: body.index("{")]
    last_arg = re.findall(r"float (\w+)\s*\)", signature)[0]
    args = re.findall(r"float (\w+)", signature)
    body = body[body.index("{") + 1 :]
    depth, end = 1, 0
    for i, ch in enumerate(body):
        depth += (ch == "{") - (ch == "}")
        if depth == 0:
            end = i
            break
    helpers = body[end + 1 :]
    helpers = helpers[: helpers.rindex("}")]
    helpers = strip_method(helpers, "public void doScale(")
    body = body[:end] + ("\n//@@HELPERS\n" + helpers if source_name in KEEP_HELPERS else "")
    for old, new in (("rotateAngleX", "xRot"), ("rotateAngleY", "yRot"), ("rotateAngleZ", "zRot"),
                     ("rotationPointX", "x"), ("rotationPointY", "y"), ("rotationPointZ", "z")):
        body = body.replace("." + old, "." + new)
    # the game's own models leave off "this." too
    for name in sorted(parts, key=len, reverse=True):
        body = re.sub(r"(?<![\w.])" + name + r"\.", "this." + name + ".", body)
    body = re.sub(r"this\.(\w+)\.render\(\w+\);",
                  lambda m: "" if m.group(1) in connected else "draw(this.%s);" % m.group(1), body)
    # a box's y offset can't change once baked: move the part by the same amount instead, as these parts have no rotation
    for name, value in re.findall(r"this\.(\w+)\.offsetY = (\w+);", body):
        rest = parts[name]["pos"][1] - parts[name]["box"][1]
        body = body.replace(f"this.{name}.offsetY = {value};", f"this.{name}.y = {rest}F + {value};")
    # held items are drawn by the engine's player code; there's no such thing to call here
    body = strip_block(body, "if(entity != null && (entity != DangerZone.player")
    body = strip_block(body, "if (entity != null && (entity != DangerZone.player")
    blended = "GL_BLEND" in body
    body = re.sub(r"GL11\.gl\w+\([^;]*\);", "", body)
    body = body.replace("MatrixStack.pushMatrix()", "dzPush()").replace("MatrixStack.popMatrix()", "dzPop()")
    body = body.replace("MatrixStack.sendCurrentStack();", "")
    body = body.replace("MatrixStack.translate(", "dzTranslate(").replace("MatrixStack.rotate(", "dzRotate(").replace("MatrixStack.scale(", "dzScale(")
    # the worm's hundred segments are drawn by the code in DzModel, against the body it tracks
    if "double curx = dw.posx;" in body:
        at = body.index("double curx = dw.posx;")
        close = body.index("dzPop();\n         }", at)
        close = body.index("}", close)
        rest = body.index("dzPop();", close)
        body = body[:at] + "drawSegments(dw, this.body1);" + body[rest + len("dzPop();"):]
    body = body.replace("entity instanceof ThePrinceYoungAdult", "false")
    body = re.sub(r"ThePrinceYoungAdult (\w+) = \(ThePrinceYoungAdult\)entity;", r"danger.orespawn.dangerzone.entity.DzAnimated \1 = (danger.orespawn.dangerzone.entity.DzAnimated) entity;", body)
    for cls, real in ENTITY_CASTS.items():
        body = re.sub(cls + r" (\w+) = \(" + cls + r"\)entity;", "danger.orespawn.dangerzone.entity." + real + r" \1 = (danger.orespawn.dangerzone.entity." + real + r") entity;", body)
        body = body.replace("entity instanceof " + cls, "entity instanceof danger.orespawn.dangerzone.entity." + real)
    body = re.sub(r"RenderInfo r = (\w+)\.renderdata;", r"danger.orespawn.entity.client.RenderInfo r = \1.getRenderInfo();", body)
    body = re.sub(r"\b(\w+)\.world\.rand\.", r"\1.level().random.", body)
    body = re.sub(r"\b(e|k|dw|cd)\.getAttacking\(\)", r"\1.dzAttacking()", body)
    body = re.sub(r"\b(e|k)\.isFlying\(\)", r"\1.dzFlying()", body)
    body = re.sub(r"\b(e|k)\.getStaying\(\)", r"\1.dzStaying()", body)
    body = body.replace("entity.getStaying()", "staying()").replace("entity.getArmsUp()", "armsUp()")
    # how far the engine's own swing has got; these creatures don't swing that way
    body = body.replace("entity.getRightArmAngle()", "0.0F").replace("entity.getLeftArmAngle()", "0.0F")
    body = re.sub(r"\bModelRenderer\b", "ModelPart", body)
    # the model's own state calls go through the interface its creature implements
    body = re.sub(r"\b(c|k|ro)\.getAttacking\(\)", r"\1.dzAttacking()", body)
    body = re.sub(r"\b(c|k|ro)\.isFlying\(\)", r"\1.dzFlying()", body)
    body = re.sub(r"\b(c|k|ro)\.getStaying\(\)", r"\1.dzStaying()", body)
    body = re.sub(r'GL20\.glUniform1f\(GL20\.glGetUniformLocation\(DangerZone\.model_shader, "alpha_adjust"\), ([\d.]+)F\);', r"alpha(\1F);", body)
    body = re.sub(r"DangerZone\.wr\.setBrightness\([\d.]+F\);", "fullBright(true);", body)
    body = body.replace("DangerZone.wr.setBrightness();", "fullBright(false);")
    body = body.replace("entity.isBaby()", "((net.minecraft.world.entity.LivingEntity) entity).isBaby()")
    body = body.replace("entity.getHealth()", "((net.minecraft.world.entity.LivingEntity) entity).getHealth()")
    body = body.replace("entity.getMaxHealth()", "((net.minecraft.world.entity.LivingEntity) entity).getMaxHealth()")
    body = body.replace(".scaleX", ".xScale").replace(".scaleY", ".yScale").replace(".scaleZ", ".zScale")
    entity_pkg = "danger.orespawn.dangerzone.entity."
    body = re.sub(r"(\w+) (\w+) = \((UFO|UFOFighter|ThePrincessTeen|ThePrincessYoungAdult)\)entity;",
                  lambda m: f"{entity_pkg}DzAnimated {m.group(2)} = ({entity_pkg}DzAnimated) entity;", body)
    body = body.replace("entity.getAttacking()", "attacking()")
    body = body.replace("entity.world.rand.", "entity.level().random.")
    body = re.sub(r"NastysaurusPrime o = \(NastysaurusPrime\)entity;\s*RenderInfo r = o\.renderdata;",
                  "danger.orespawn.entity.client.RenderInfo r = ((danger.orespawn.entity.Nastysaurus) entity).getRenderInfo();", body)

    translucent = "alpha(" in body or blended
    extra = ""
    if "//@@HELPERS" in body:
        body, extra = body.split("//@@HELPERS")
    ctor_super = "        super(net.minecraft.client.renderer.RenderType::entityTranslucent);\n" if translucent else ""
    out = f"""package danger.orespawn.dangerzone.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * DangerZone's {source_name}, by TheyCallMeDanger. Generated by tools/import_dangerzone.py; do not edit.
 */
public class {class_name}<T extends Entity> extends DzModel<T> {{
    public static final float SCALE = {scale}F;

{os.linesep.join(fields_decl)}

    public {class_name}(ModelPart root) {{
{ctor_super}{os.linesep.join(ctor_code)}
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{os.linesep.join(layer)}
        return LayerDefinition.create(mesh, {size[0]}, {size[1]});
    }}

    @Override
    protected void animateAndDraw(T entity, {", ".join("float " + a for a in args[:5])}) {{
        float {last_arg} = 1.0F;
{body}
    }}
{extra}
}}
"""
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, class_name + ".java"), "w", encoding="utf8", newline="\n") as fh:
        fh.write(out)
    print(class_name, "parts", len(parts), "connected", len(connected), "texture", size, "scale", scale)


# DangerZone's blocks, as the castles' palette. Its own materials are this mod's; the rest are their nearest vanilla blocks.
PALETTE = {
    "stone": "minecraft:stone", "stone2": "minecraft:cobblestone", "greystone": "minecraft:andesite", "greystone2": "minecraft:polished_andesite",
    "greystone4": "minecraft:polished_andesite", "hardrock": "minecraft:obsidian", "lightstick": "minecraft:shroomlight", "glass": "minecraft:glass",
    "grassblock": "minecraft:grass_block", "blockemerald": "minecraft:emerald_block", "blockdark": "minecraft:black_concrete",
    "oredark": "minecraft:blackstone", "darkstick": "minecraft:blackstone_wall", "blockflower_black": "minecraft:wither_rose",
    "blockflower_scary": "minecraft:wither_rose",
    "dargonstone1": "orespawn_dangerzone:dargon_stone", "dargonstone2": "orespawn_dangerzone:dargon_stone_dark",
    "dragoneyestone": "orespawn_dangerzone:dragons_eye_stone", "extremetorch": "orespawn_dangerzone:extreme_torch",
    "krakenkingspawner": "orespawn_dangerzone:kraken_king_spawner",
    # spawners: these three names only ever go through spawner()
    "varmintspawner": "orespawn:terrible_terror", "verminspawner": "orespawn:terrible_terror", "annoyancespawner": "orespawn:terrible_terror",
}
DUNGEON_METHODS = ["public static void makeElementDungeon(", "public void makeDargonsLair(", "public void buildDargonDungeon(",
                   "public void makeKrakenKingCastle(", "private void makekrakentreasures(", "private void makekrakentreasure("]


def translate_dungeons():
    src = open(os.path.join(DZ, "src/orespawn-dzvr28/OreSpawn/OreSpawnDungeons.java"), encoding="utf8", errors="ignore").read()
    out = []
    for start in DUNGEON_METHODS:
        at = src.index(start)
        i = src.index("{", at)
        depth = 0
        for j in range(i, len(src)):
            depth += (src[j] == "{") - (src[j] == "}")
            if depth == 0:
                out.append(src[at : j + 1])
                break
    code = "\n\n".join(out)
    code = re.sub(r"\b(public|private)( static)? void ", "public static void ", code)
    code = re.sub(r"\bWorld (world|w)\b, int (dim|d)\b", r"DzBuilder \1", code)
    code = re.sub(r"\b(?:OreSpawnMain|Blocks)\.(\w+)\.blockID", lambda m: f'block("{m.group(1)}")', code)
    code = code.replace("this.", "")
    code = re.sub(r"\bFastSetBlock\((world|w), (dim|d), ", r"\1.set(", code)
    code = re.sub(r"\bSetSpawner\((world|w), (dim|d), ", r"\1.spawner(", code)
    code = re.sub(r"\b(world|w)\.setblocknonotify\((dim|d), ", r"\1.set(", code)
    code = re.sub(r"\b(world|w)\.setblock\((dim|d), ", r"\1.set(", code)
    code = re.sub(r"\b(world|w)\.getblock\((dim|d), ", r"\1.get(", code)
    code = re.sub(r"\bUtils\.add_chest\((world|w), (dim|d), ([^;]*), (\w+)\);", r'\1.chest(\3, "\4");', code)
    code = re.sub(r"\bspawnCreature\(\s*(world|w),\s*(dim|d),\s*", r"\1.creature(", code)
    code = re.sub(r"\b(world|w)\.createEntityByName\(\s*", r"\1.create(", code)
    code = re.sub(r'(\.create\("[^"]+"), (dim|d), ', r"\1, ", code)
    code = re.sub(r"\bFastmath\.nextInt\(", "world.rand(", code)
    code = re.sub(r"\b(world|w)\.rand\.nextInt\(", r"\1.rand(", code)
    code = re.sub(r"\b(world|w)\.spawnEntityInWorld\(", r"\1.add(", code)
    code = re.sub(r"\b(makekrakentreasures?|buildDargonDungeon|makeDargonsLair|makeKrakenKingCastle|makeElementDungeon)\((world|w), (dim|d), ", r"\1(\2, ", code)
    code = re.sub(r"\bEntity\b", "DzBuilder.Creature", code)
    code = re.sub(r"\bnew InventoryContainer\(", "new DzBuilder.Held(", code)
    used = sorted(set(re.findall(r'block\("(\w+)"\)', code)))
    missing = [b for b in used if b not in PALETTE]
    if missing:
        raise SystemExit(f"blocks with no palette entry: {missing}")
    target = os.path.join(HERE, "src/main/java/danger/orespawn/dangerzone/world/DzDungeons.java")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    table = ",\n".join(f'            "{k}", "{v}"' for k, v in PALETTE.items() if k in used)
    # Map.of tops out at ten pairs, and this table is longer.
    pairs = ",\n".join(f'            Map.entry("{k}", "{v}")' for k, v in PALETTE.items() if k in used)
    text = f"""package danger.orespawn.dangerzone.world;

import java.util.Map;

/**
 * DangerZone's castle builders, by TheyCallMeDanger. Generated by tools/import_dangerzone.py; do not edit.
 * They run against {{@link DzBuilder}}, which queues what they ask for and places it over many ticks.
 */
public final class DzDungeons {{
    static final Map<String, String> PALETTE = Map.ofEntries(
{pairs});

    private DzDungeons() {{
    }}

    private static int block(String name) {{
        return DzBuilder.id(name);
    }}

{code}
}}
"""
    with open(target, "w", encoding="utf8", newline="\n") as fh:
        fh.write(text)
    print("dungeons", len(code.splitlines()), "lines, blocks", used)


def terrain_base():
    """Vanilla's overworld noise settings, the starting point for this mod's dimensions."""
    import glob
    jars = glob.glob(os.path.join(HERE, ".gradle/repositories/ng_dummy_ng/net/minecraft/client/1.21.1/*client-extra.jar"))
    with zipfile.ZipFile(jars[0]) as jar:
        target = os.path.join(HERE, "tools/overworld_noise_settings.json")
        with open(target, "wb") as fh:
            fh.write(jar.read("data/minecraft/worldgen/noise_settings/overworld.json"))


def write_file(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(data)


def main():
    game = zipfile.ZipFile(os.path.join(DZ, "dl/dzvr-2.8/DZVR2.8.zip"))
    mod = zipfile.ZipFile(io.BytesIO(game.read("mods/OreSpawn_for_dzvr2.8.jar")))
    engine = zipfile.ZipFile(io.BytesIO(game.read("DangerZone.jar")))
    names = set(mod.namelist())
    for source, target in TEXTURES.items():
        if source in names:
            write_file(os.path.join(ASSETS, "textures", target), mod.read(source))
        else:
            print("no texture", source)
    for source, target in ENGINE_TEXTURES.items():
        write_file(os.path.join(ASSETS, "textures", target), engine.read(source))
    print("textures", len(TEXTURES) + len(ENGINE_TEXTURES))

    ffmpeg = shutil.which("ffmpeg")
    if ffmpeg is None:
        raise SystemExit("ffmpeg is not on the path")
    os.makedirs(os.path.join(ASSETS, "sounds"), exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for zf, prefix, names in ((mod, "orespawnres/sounds/", MOD_SOUNDS + LATE_MOD_SOUNDS), (engine, "res/sounds/", GAME_SOUNDS)):
            for name in names:
                wav = os.path.join(tmp, name + ".wav")
                # the archive's own capitalisation varies from the names the code uses
                entry = next(n for n in zf.namelist() if n.lower() == (prefix + name + ".wav").lower())
                write_file(wav, zf.read(entry))
                ogg = os.path.join(ASSETS, "sounds", name + ".ogg")
                subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", ogg], check=True)
    print("sounds", len(MOD_SOUNDS) + len(LATE_MOD_SOUNDS) + len(GAME_SOUNDS))
    terrain_base()
    events = {k: {"sounds": [f"{MOD}:{s}" for s in v]} for k, v in SOUND_EVENTS.items()}
    write_file(os.path.join(ASSETS, "sounds.json"), json.dumps(events, indent=2).encode())

    for source, target in MODEL_CLASSES.items():
        convert_model(source, target)
    for source, target in ENGINE_MODEL_CLASSES.items():
        convert_model(source, target, os.path.join(DZ, "x/dz21-buildkit/src/dangerzone/entities"))
    translate_dungeons()


main()
