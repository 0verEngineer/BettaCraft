"""M31 assets: the terrarium animals (entity models, ground at y = 24, head to -Z) and their textures.

* ``musk_turtle``: the common musk turtle (Sternotherus odoratus): a high, keeled, dark olive-brown carapace with
  darker seams, a yellowish plastron, a pointed head with the two pale stripes on each side, short legs, a tiny tail.

Each model is a list of named groups (``shell``, ``head``, ``leg_fr`` / ``leg_fl`` / ``leg_br`` / ``leg_bl``, ``tail``)
of cuboids; one box-UV texture per model (packed like the critters). The Java layers go to
``client/model/TerrariumModelDefinitions.java`` (generated). Parts b-d add their turtles, frogs, geckos and snakes
here: a model function in ``MODELS``, a colour function in ``COLOURS``, then run this script.

Run from the repo root:  py tools/assetgen/terrarium_animals.py
"""
from __future__ import annotations

import math

import numpy as np

from common import JAVA_SRC, TEX, save_png
from critters import Group, c, java_layer, pack

TERRA_TEX = TEX / "entity" / "terrarium"


def musk_turtle() -> list[Group]:
    shell = Group("shell", pivot=(0, -2.6, 0), cubes=[
        c("rim", (0, -2.3, 0.3), (8.6, 1.4, 11.4), (0, 0, 0), "carapace"),
        c("dome", (0, -3.6, 0.3), (7.4, 1.4, 10.0), (0, 0, 0), "carapace"),
        c("keel", (0, -4.6, 0.5), (5.0, 1.0, 7.4), (0, 0, 0), "carapace"),
        c("crest", (0, -5.2, 0.6), (2.0, 0.6, 5.0), (0, 0, 0), "carapace"),
        c("plastron", (0, -1.3, 0.3), (6.8, 0.8, 9.6), (0, 0, 0), "plastron"),
    ])
    head = Group("head", pivot=(0, -2.0, -5.0), cubes=[
        c("neck", (0, -2.0, -5.9), (2.2, 1.8, 2.2), (0, 0, 0), "skin"),
        c("skull", (0, -2.3, -7.6), (2.8, 2.2, 2.6), (0, 0, 0), "head"),
        c("snout", (0, -2.0, -9.2), (1.6, 1.3, 0.8), (0, 0, 0), "head"),
        c("eye_r", (1.45, -2.7, -7.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
        c("eye_l", (-1.45, -2.7, -7.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
    ])
    groups = [shell, head]
    for name, sx, z in (("leg_fr", 1, -3.4), ("leg_fl", -1, -3.4), ("leg_br", 1, 3.6), ("leg_bl", -1, 3.6)):
        groups.append(Group(name, pivot=(3.0 * sx, -1.6, z), cubes=[
            c("limb", (3.9 * sx, -1.0, z + (-0.5 if z < 0 else 0.5)), (1.8, 1.4, 2.0), (0, -20 * sx if z < 0 else 20 * sx, 0), "skin"),
            c("foot", (4.4 * sx, -0.2, z + (-0.9 if z < 0 else 0.9)), (2.0, 0.4, 2.0), (0, -20 * sx if z < 0 else 20 * sx, 0), "skin")]))
    groups.append(Group("tail", pivot=(0, -1.8, 5.6), cubes=[c("tip", (0, -1.7, 6.6), (0.9, 0.8, 2.0), (-10, 0, 0), "skin")]))
    return groups


SHELL = np.array([0.29, 0.24, 0.16])
SHELL_DARK = np.array([0.15, 0.12, 0.08])
PLASTRON = np.array([0.70, 0.60, 0.36])
SKIN = np.array([0.24, 0.24, 0.20])
STRIPE = np.array([0.82, 0.78, 0.58])


def _musk_colour(cu, face, px, py, pz, rng) -> np.ndarray:
    if cu.pattern == "eye":
        return np.array([0.06, 0.06, 0.05])
    if cu.pattern == "carapace":
        # the scutes: darker seams on a grid, a few dark blotches
        seam = (abs(math.sin(px * 1.15)) < 0.16) or (abs(math.sin(pz * 0.9)) < 0.14)
        blot = math.sin(px * 2.3 + pz * 1.1) * math.sin(pz * 1.9 - px * 0.7) > 0.55
        col = SHELL_DARK if seam or blot else SHELL
        if face == "bottom":
            col = PLASTRON * 0.9
    elif cu.pattern == "plastron":
        col = PLASTRON if face != "top" else SHELL_DARK
    elif cu.pattern == "head":
        col = SKIN
        # two pale stripes along each side of the head (above and below the eye)
        if face in ("east", "west") and (abs(py + 2.9) < 0.22 or abs(py + 1.7) < 0.22):
            col = STRIPE
        if face == "bottom":
            col = SKIN * 1.5
    else:
        col = SKIN if face != "bottom" else SKIN * 1.4
    shade = {"top": 1.0, "bottom": 0.85, "north": 0.9, "south": 0.9, "east": 0.85, "west": 0.85}[face]
    return np.clip(col * shade + (rng.random() - 0.5) * 0.05, 0, 1)


MODELS = {"musk_turtle": musk_turtle}
COLOURS = {"musk_turtle": _musk_colour}


def texture(groups: list[Group], colour, seed: int, size: int) -> np.ndarray:
    """Box-UV texture like critters.texture, coloured per face pixel by ``colour``."""
    rng = np.random.default_rng(seed)
    img = np.zeros((size, size, 4))
    for cu in (cu for g in groups for cu in g.cubes):
        w, h, d = cu.size
        cx, cy, cz = cu.center
        x0, y0, z0 = cx - w / 2, cy - h / 2, cz - d / 2
        W, D, H = int(math.ceil(w)), int(math.ceil(d)), int(math.ceil(h))
        rects = [
            ("top", cu.u + D, cu.v, W, D, lambda a, b: (x0 + a, y0, z0 + b)),
            ("bottom", cu.u + D + W, cu.v, W, D, lambda a, b: (x0 + a, y0 + h, z0 + b)),
            ("east", cu.u, cu.v + D, D, H, lambda a, b: (x0 + w, y0 + b, z0 + a)),
            ("north", cu.u + D, cu.v + D, W, H, lambda a, b: (x0 + a, y0 + b, z0)),
            ("west", cu.u + D + W, cu.v + D, D, H, lambda a, b: (x0, y0 + b, z0 + d - a)),
            ("south", cu.u + 2 * D + W, cu.v + D, W, H, lambda a, b: (x0 + w - a, y0 + b, z0 + d)),
        ]
        for face, u0, v0, fw, fh, fn in rects:
            for j in range(max(1, fh)):
                for i in range(max(1, fw)):
                    uu, vv = u0 + i, v0 + j
                    if not (0 <= uu < size and 0 <= vv < size):
                        continue
                    a = (i + 0.5) * (w if face in ("top", "bottom", "north", "south") else d) / max(1, fw)
                    b = (j + 0.5) * (d if face in ("top", "bottom") else h) / max(1, fh)
                    px, py, pz = fn(a, b)
                    img[vv, uu, :3] = colour(cu, face, px, py, pz, rng)
                    img[vv, uu, 3] = 1.0
    return img


def write_java(models: dict[str, list[Group]], sizes: dict[str, int]):
    body = ["// GENERATED by tools/assetgen/terrarium_animals.py - do not edit by hand, edit terrarium_animals.py and rerun it",
            "package org.overengineer.bettacraft.client.model;",
            "",
            "import net.minecraft.client.model.geom.PartPose;",
            "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
            "import net.minecraft.client.model.geom.builders.LayerDefinition;",
            "import net.minecraft.client.model.geom.builders.MeshDefinition;",
            "import net.minecraft.client.model.geom.builders.PartDefinition;",
            "",
            "/**",
            " * M31: entity models of the terrarium animals (ground y = 24 under the root part \"animal\", head to -Z). Groups:",
            " * shell, head, leg_fr / leg_fl / leg_br / leg_bl, tail. Textures: textures/entity/terrarium/<model>.png (box UV).",
            " */",
            "public final class TerrariumModelDefinitions {",
            "    public static final String[] MODELS = {" + ", ".join(f'"{m}"' for m in models) + "};",
            "",
            "    private TerrariumModelDefinitions() {}",
            "",
            "    public static LayerDefinition create(String model) {",
            "        return switch (model) {"]
    for m in models:
        body.append(f"            case \"{m}\" -> create{m.replace('_', '').capitalize()}Layer();")
    body += ["            default -> throw new IllegalArgumentException(model);",
             "        };",
             "    }",
             ""]
    for m, groups in models.items():
        layer = java_layer(m.replace("_", ""), groups, True, sizes[m]).replace("addOrReplaceChild(\"frog\"", "addOrReplaceChild(\"animal\"")
        body.append(layer)
        body.append("")
    body.append("}")
    path = JAVA_SRC / "client" / "model" / "TerrariumModelDefinitions.java"
    path.write_text("\n".join(body) + "\n", encoding="utf-8", newline="\n")
    return path


def write_all() -> list:
    written = []
    models, sizes = {}, {}
    for i, (name, fn) in enumerate(MODELS.items()):
        groups = fn()
        sizes[name] = pack(groups)
        models[name] = groups
        written.append(save_png(texture(groups, COLOURS[name], 3150 + i, sizes[name]), TERRA_TEX / f"{name}.png"))
    written.append(write_java(models, sizes))
    return written


if __name__ == "__main__":
    for p in write_all():
        print(p)
