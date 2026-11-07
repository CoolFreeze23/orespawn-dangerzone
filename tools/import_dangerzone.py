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
}
ENGINE_MODEL_CLASSES = {
    "ModelFlag": "DzTowerFlagModel",
    "ModelMartian": "DzMartianModel",
}
# Models whose render method calls helper methods of their own, which come along.
KEEP_HELPERS = {"ModelThePrincessAdult"}
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
        for zf, prefix, names in ((mod, "orespawnres/sounds/", MOD_SOUNDS), (engine, "res/sounds/", GAME_SOUNDS)):
            for name in names:
                wav = os.path.join(tmp, name + ".wav")
                # the archive's own capitalisation varies from the names the code uses
                entry = next(n for n in zf.namelist() if n.lower() == (prefix + name + ".wav").lower())
                write_file(wav, zf.read(entry))
                ogg = os.path.join(ASSETS, "sounds", name + ".ogg")
                subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", ogg], check=True)
    print("sounds", len(MOD_SOUNDS) + len(GAME_SOUNDS))
    events = {k: {"sounds": [f"{MOD}:{s}" for s in v]} for k, v in SOUND_EVENTS.items()}
    write_file(os.path.join(ASSETS, "sounds.json"), json.dumps(events, indent=2).encode())

    for source, target in MODEL_CLASSES.items():
        convert_model(source, target)
    for source, target in ENGINE_MODEL_CLASSES.items():
        convert_model(source, target, os.path.join(DZ, "x/dz21-buildkit/src/dangerzone/entities"))


main()
