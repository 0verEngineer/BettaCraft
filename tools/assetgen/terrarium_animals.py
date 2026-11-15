"""M31 assets: the terrarium animals (entity models, ground at y = 24, head to -Z) and their textures.

* ``musk_turtle``: the common musk turtle (Sternotherus odoratus): a high, keeled, dark olive-brown carapace with
  darker seams, a yellowish plastron, a pointed head with the two pale stripes on each side, short legs, a tiny tail.
* M31 part b ``slider_turtle``: the pond slider (Trachemys scripta): a broad, low, olive-green carapace with yellow
  lines, webbed feet, yellow stripes on the green head; two textures: ``slider_turtle_elegans`` (the red patch behind
  the eye, a plastron with dark spots) and ``slider_turtle_scripta`` (a big yellow patch, a plain yellow plastron).
* ``hermann_tortoise``: Hermann's tortoise (Testudo hermanni): a high domed carapace of ochre scutes with black
  blotches and black seams, a yellow plastron with two dark bands, pillar legs with scales, a short tail.
* ``reeves_turtle``: the Chinese three-keeled pond turtle (Mauremys reevesii): a dark brown carapace with three low
  keels, a yellow plastron with brown blotches, a dark head with yellow-green wavy lines.

One model can carry several textures (``TEXTURES``: the varieties). Each model is a list of named groups (``shell``, ``head``, ``leg_fr`` / ``leg_fl`` / ``leg_br`` / ``leg_bl``, ``tail``)
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


def slider_turtle() -> list[Group]:
    """A broad, low carapace (a basking turtle), long webbed feet, a rounded head on a long neck."""
    shell = Group("shell", pivot=(0, -2.6, 0), cubes=[
        c("rim", (0, -2.2, 0.3), (11.0, 1.2, 14.5), (0, 0, 0), "carapace"),
        c("dome", (0, -3.3, 0.3), (9.6, 1.2, 12.8), (0, 0, 0), "carapace"),
        c("top", (0, -4.2, 0.5), (7.0, 0.8, 9.6), (0, 0, 0), "carapace"),
        c("plastron", (0, -1.2, 0.3), (9.0, 0.8, 12.6), (0, 0, 0), "plastron"),
    ])
    head = Group("head", pivot=(0, -2.2, -6.6), cubes=[
        c("neck", (0, -2.1, -7.6), (2.6, 2.0, 2.6), (0, 0, 0), "skin"),
        c("skull", (0, -2.5, -9.6), (3.2, 2.4, 3.0), (0, 0, 0), "head"),
        c("snout", (0, -2.2, -11.4), (2.0, 1.5, 0.8), (0, 0, 0), "head"),
        c("eye_r", (1.65, -3.0, -10.0), (0.3, 0.7, 0.7), (0, 0, 0), "eye"),
        c("eye_l", (-1.65, -3.0, -10.0), (0.3, 0.7, 0.7), (0, 0, 0), "eye"),
    ])
    groups = [shell, head]
    for name, sx, z in (("leg_fr", 1, -4.6), ("leg_fl", -1, -4.6), ("leg_br", 1, 4.8), ("leg_bl", -1, 4.8)):
        out = -1 if z < 0 else 1
        groups.append(Group(name, pivot=(4.0 * sx, -1.6, z), cubes=[
            c("limb", (5.2 * sx, -1.0, z + 0.6 * out), (2.4, 1.4, 2.6), (0, -20 * sx * -out, 0), "skin"),
            c("foot", (6.0 * sx, -0.2, z + 1.3 * out), (3.0, 0.4, 2.8), (0, -20 * sx * -out, 0), "skin")]))
    groups.append(Group("tail", pivot=(0, -1.8, 7.4), cubes=[c("tip", (0, -1.7, 8.7), (1.2, 0.9, 2.6), (-10, 0, 0), "skin")]))
    return groups


def hermann_tortoise() -> list[Group]:
    """A high dome of stacked scute rings, pillar legs, a blunt head; the plastron close to the ground."""
    shell = Group("shell", pivot=(0, -2.6, 0), cubes=[
        c("rim", (0, -2.0, 0.3), (9.8, 1.6, 12.4), (0, 0, 0), "carapace"),
        c("dome", (0, -3.6, 0.3), (9.0, 1.6, 11.4), (0, 0, 0), "carapace"),
        c("dome2", (0, -5.0, 0.3), (7.6, 1.4, 9.4), (0, 0, 0), "carapace"),
        c("top", (0, -6.1, 0.4), (5.2, 1.0, 6.6), (0, 0, 0), "carapace"),
        c("plastron", (0, -1.0, 0.3), (8.6, 0.8, 11.0), (0, 0, 0), "plastron"),
    ])
    head = Group("head", pivot=(0, -2.2, -6.0), cubes=[
        c("neck", (0, -2.2, -6.8), (2.4, 2.0, 2.2), (0, 0, 0), "skin"),
        c("skull", (0, -2.6, -8.5), (2.8, 2.6, 2.6), (0, 0, 0), "head"),
        c("snout", (0, -2.3, -10.0), (1.8, 1.6, 0.6), (0, 0, 0), "head"),
        c("eye_r", (1.45, -3.1, -8.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
        c("eye_l", (-1.45, -3.1, -8.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
    ])
    groups = [shell, head]
    for name, sx, z in (("leg_fr", 1, -4.0), ("leg_fl", -1, -4.0), ("leg_br", 1, 4.2), ("leg_bl", -1, 4.2)):
        out = -1 if z < 0 else 1
        groups.append(Group(name, pivot=(3.6 * sx, -1.6, z), cubes=[
            c("limb", (4.3 * sx, -0.9, z + 0.4 * out), (2.4, 1.8, 2.4), (0, 0, 0), "scales"),
            c("foot", (4.5 * sx, -0.2, z + 0.6 * out), (2.6, 0.4, 2.6), (0, 0, 0), "scales")]))
    groups.append(Group("tail", pivot=(0, -1.6, 6.2), cubes=[c("tip", (0, -1.4, 6.9), (1.0, 0.8, 1.6), (-15, 0, 0), "skin")]))
    return groups


def reeves_turtle() -> list[Group]:
    """A medium, flat carapace with the three low keels, a pointed head."""
    shell = Group("shell", pivot=(0, -2.6, 0), cubes=[
        c("rim", (0, -2.1, 0.3), (9.4, 1.3, 12.4), (0, 0, 0), "carapace"),
        c("dome", (0, -3.2, 0.3), (8.2, 1.2, 11.0), (0, 0, 0), "carapace"),
        c("keel", (0, -4.1, 0.4), (1.2, 0.8, 9.0), (0, 0, 0), "keel"),
        c("keel_r", (2.6, -3.9, 0.4), (1.0, 0.6, 7.8), (0, 0, 0), "keel"),
        c("keel_l", (-2.6, -3.9, 0.4), (1.0, 0.6, 7.8), (0, 0, 0), "keel"),
        c("plastron", (0, -1.2, 0.3), (8.0, 0.8, 10.8), (0, 0, 0), "plastron"),
    ])
    head = Group("head", pivot=(0, -2.0, -5.6), cubes=[
        c("neck", (0, -2.0, -6.4), (2.2, 1.9, 2.2), (0, 0, 0), "skin"),
        c("skull", (0, -2.3, -8.1), (2.8, 2.2, 2.6), (0, 0, 0), "head"),
        c("snout", (0, -2.0, -9.7), (1.6, 1.3, 0.8), (0, 0, 0), "head"),
        c("eye_r", (1.45, -2.7, -8.4), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
        c("eye_l", (-1.45, -2.7, -8.4), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
    ])
    groups = [shell, head]
    for name, sx, z in (("leg_fr", 1, -3.8), ("leg_fl", -1, -3.8), ("leg_br", 1, 4.0), ("leg_bl", -1, 4.0)):
        out = -1 if z < 0 else 1
        groups.append(Group(name, pivot=(3.4 * sx, -1.6, z), cubes=[
            c("limb", (4.4 * sx, -1.0, z + 0.5 * out), (2.0, 1.4, 2.2), (0, -20 * sx * -out, 0), "skin"),
            c("foot", (5.0 * sx, -0.2, z + 0.9 * out), (2.2, 0.4, 2.2), (0, -20 * sx * -out, 0), "skin")]))
    groups.append(Group("tail", pivot=(0, -1.8, 6.2), cubes=[c("tip", (0, -1.7, 7.4), (1.0, 0.8, 2.4), (-10, 0, 0), "skin")]))
    return groups


def _shade(face):
    return {"top": 1.0, "bottom": 0.85, "north": 0.9, "south": 0.9, "east": 0.85, "west": 0.85}[face]


SLIDER_SHELL = np.array([0.27, 0.33, 0.16])
SLIDER_LINE = np.array([0.72, 0.70, 0.30])
SLIDER_SKIN = np.array([0.24, 0.32, 0.17])
SLIDER_YELLOW = np.array([0.86, 0.78, 0.30])


def _slider(elegans: bool):
    def colour(cu, face, px, py, pz, rng) -> np.ndarray:
        if cu.pattern == "eye":
            return np.array([0.10, 0.09, 0.05]) if elegans else np.array([0.30, 0.26, 0.08])
        if cu.pattern == "carapace":
            if face == "bottom":
                col = SLIDER_YELLOW * 0.85
            else:
                seam = (abs(math.sin(px * 1.0)) < 0.12) or (abs(math.sin(pz * 0.8)) < 0.1)
                # the yellow lines run across every scute (wider and brighter on the scripta)
                line = abs(math.sin(px * 2.6 + pz * 0.9)) < (0.22 if elegans else 0.34)
                col = SHELL_DARK * 1.1 if seam else SLIDER_LINE if line else SLIDER_SHELL
                if face in ("east", "west", "north", "south") and cu.name == "rim" and (int(px * 2 + pz * 2) % 3 == 0):
                    col = SLIDER_YELLOW  # the yellow marginal scutes' edges
        elif cu.pattern == "plastron":
            col = SLIDER_YELLOW if face != "top" else SLIDER_SHELL
            if elegans and face == "bottom" and math.sin(px * 1.7) * math.sin(pz * 1.3) > 0.55:
                col = np.array([0.22, 0.20, 0.10])  # the dark ocelli of the red-eared slider
        elif cu.pattern == "head":
            col = SLIDER_SKIN
            if face in ("east", "west"):
                if abs(py + 2.0) < 0.2 or abs(py + 3.3) < 0.15:
                    col = SLIDER_YELLOW
                # behind the eye: the red ear stripe or the big yellow cheek patch
                if elegans and -9.6 < pz < -8.2 and -3.0 < py < -2.3:
                    col = np.array([0.80, 0.14, 0.10])
                if not elegans and -9.9 < pz < -8.0 and -3.1 < py < -1.8:
                    col = np.array([0.92, 0.82, 0.30])
            if face == "bottom":
                col = SLIDER_YELLOW * 0.9
        else:
            col = SLIDER_SKIN
            if face in ("east", "west", "north", "south") and abs(math.sin(py * 3.0 + pz * 1.5)) < 0.2:
                col = SLIDER_YELLOW * 0.9  # the stripes on legs and neck
            if face == "bottom":
                col = SLIDER_SKIN * 1.4
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)
    return colour


HERM_SHELL = np.array([0.78, 0.60, 0.26])
HERM_BLACK = np.array([0.10, 0.08, 0.05])
HERM_SKIN = np.array([0.46, 0.43, 0.32])


def _hermann(cu, face, px, py, pz, rng) -> np.ndarray:
    if cu.pattern == "eye":
        return np.array([0.06, 0.05, 0.04])
    if cu.pattern == "carapace":
        if face == "bottom":
            col = HERM_SHELL * 0.9
        else:
            # scutes on a grid: black seams, a black blotch near the front of each scute, ochre growth rings
            u, v = px / 2.4, pz / 2.6
            fu, fv = u - math.floor(u), v - math.floor(v)
            seam = fu < 0.12 or fv < 0.1
            blot = 0.2 < fu < 0.65 and 0.15 < fv < 0.5 and (math.floor(u) + math.floor(v)) % 2 == 0
            ring = abs(math.sin((fu + fv) * 9.0)) < 0.18
            col = HERM_BLACK if seam or blot else HERM_SHELL * (0.85 if ring else 1.0)
    elif cu.pattern == "plastron":
        col = HERM_SHELL * 1.05 if face != "top" else HERM_SHELL * 0.6
        if face == "bottom" and (abs(px - 2.2) < 1.0 or abs(px + 2.2) < 1.0) and -3 < pz < 5:
            col = HERM_BLACK * 1.5  # the two dark bands of the plastron
    elif cu.pattern == "head":
        col = HERM_SKIN * 0.85
        if face == "top" and math.sin(px * 3.0) * math.sin(pz * 3.0) > 0.3:
            col = HERM_SKIN * 0.6
        if face in ("east", "west") and abs(py + 2.3) < 0.25:
            col = np.array([0.80, 0.72, 0.36])  # the yellow cheek spot
    elif cu.pattern == "scales":
        col = HERM_SKIN if (int(px * 2) + int(py * 2) + int(pz * 2)) % 2 else HERM_SKIN * 0.75
    else:
        col = HERM_SKIN if face != "bottom" else HERM_SKIN * 1.2
    return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)


REEV_SHELL = np.array([0.28, 0.20, 0.12])
REEV_YELLOW = np.array([0.80, 0.70, 0.32])
REEV_SKIN = np.array([0.20, 0.20, 0.17])


def _reeves(cu, face, px, py, pz, rng) -> np.ndarray:
    if cu.pattern == "eye":
        return np.array([0.20, 0.18, 0.08])
    if cu.pattern in ("carapace", "keel"):
        if face == "bottom":
            col = REEV_YELLOW * 0.8
        else:
            seam = (abs(math.sin(px * 1.2)) < 0.13) or (abs(math.sin(pz * 0.95)) < 0.12)
            col = REEV_SHELL * (0.7 if seam else 1.0)
            if cu.pattern == "keel":
                col = REEV_SHELL * 1.35
    elif cu.pattern == "plastron":
        col = REEV_YELLOW if face != "top" else REEV_SHELL
        if face == "bottom" and math.sin(px * 1.9 + 0.5) * math.sin(pz * 1.4) > 0.4:
            col = np.array([0.30, 0.20, 0.10])  # the brown blotches of each plastral scute
    elif cu.pattern == "head":
        col = REEV_SKIN
        if face in ("east", "west") and abs(py + 2.4 + 0.35 * math.sin(pz * 2.2)) < 0.2:
            col = np.array([0.66, 0.72, 0.32])  # the wavy yellow-green lines
        if face == "bottom":
            col = REEV_YELLOW * 0.7
    else:
        col = REEV_SKIN if face != "bottom" else REEV_SKIN * 1.5
    return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)


MODELS = {"musk_turtle": musk_turtle, "slider_turtle": slider_turtle, "hermann_tortoise": hermann_tortoise, "reeves_turtle": reeves_turtle}
COLOURS = {"musk_turtle": _musk_colour, "hermann_tortoise": _hermann, "reeves_turtle": _reeves}
# models with several textures (varieties): texture name -> colour
TEXTURES = {"slider_turtle": {"slider_turtle_elegans": _slider(True), "slider_turtle_scripta": _slider(False)}}


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
        for tex, colour in TEXTURES.get(name, {name: COLOURS.get(name)}).items():
            written.append(save_png(texture(groups, colour, 3150 + i, sizes[name]), TERRA_TEX / f"{tex}.png"))
    written.append(write_java(models, sizes))
    return written


if __name__ == "__main__":
    for p in write_all():
        print(p)
