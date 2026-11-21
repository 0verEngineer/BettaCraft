"""M31 part c assets: the amphibians and reptiles of the terrarium (entity models and textures; written together with the
turtles by ``terrarium_animals.py``, which imports ``MODELS``, ``COLOURS`` and ``TEXTURES`` from here).

Ground at y = 0 of the model (y negative is up), head to -Z, the same conventions as the turtles. Each body plan has its
own groups (and its own Java model class in ``client/model``):

* frogs (``FrogModel``): ``body``, ``head``, ``leg_fr`` / ``leg_fl`` (arms), ``leg_br`` / ``leg_bl`` (the long folded hind legs);
* the tadpole (``TadpoleModel``): ``body``, ``tail``;
* geckos (``GeckoModel``): ``body``, ``head``, four legs, ``tail`` and ``tail_tip``;
* the snake (``SnakeModel``): ``head`` and ``seg0`` .. ``seg9`` (neck to tail tip), bent by the model.

Species:

* ``dart_frog`` (Dendrobates): a small, round frog with long thin legs and big black eyes. Three textures, one per species:
  ``dart_frog_tinctorius`` (the blue "azureus" form: sky-blue back with black spots, darker blue legs),
  ``dart_frog_auratus`` (mint green with black blotches), ``dart_frog_leucomelas`` (yolk yellow with black bands).
  ``dart_frog_tadpole``: a grey-brown tadpole with a clear fin.
* ``horned_frog`` (Ceratophrys cranwelli): the "pacman" frog: almost round, a huge wide head and mouth, little horns over the
  eyes, short legs. ``horned_frog_green`` (green with dark brown, red-edged blotches) and ``horned_frog_brown``.
* ``leopard_gecko`` (Eublepharis macularius): a stocky body on splayed legs, a big head with lidded eyes, a fat banded tail.
  ``leopard_gecko_normal`` (yellow with black spots), ``_tangerine`` (orange, few spots, a carrot tail), ``_albino``
  (pale yellow and pink with light brown spots, red eyes).
* ``crested_gecko`` (Correlophus ciliatus): a slender body, a big triangular head with the spiny crests over the eyes and
  down the back, wide toe pads, a thin tail. ``crested_gecko_tiger`` (tan with dark bands), ``_flame`` (a cream back on
  red-brown), ``_harlequin`` (cream sides and legs on orange-red).
* ``corn_snake`` (Pantherophis guttatus): ten body segments tapering to the tail. ``corn_snake_wild`` (red saddles bordered
  black on orange-tan, a black and white checkered belly), ``_amelanistic`` (no black: red on orange, white borders, red
  eyes), ``_anerythristic`` (no red: black and grey on silver).
"""
from __future__ import annotations

import math

import numpy as np

from critters import Group, c


def _shade(face):
    return {"top": 1.0, "bottom": 0.8, "north": 0.9, "south": 0.9, "east": 0.85, "west": 0.85}[face]


def _legs(names_z, build):
    out = []
    for name, sx, z in names_z:
        out.append(Group(name, pivot=build[0](sx, z), cubes=build[1](sx, z)))
    return out


# ---------------------------------------------------------------------------------------------- frogs

def dart_frog() -> list[Group]:
    body = Group("body", pivot=(0, -2.8, 0.8), cubes=[
        c("back", (0, -2.9, 0.6), (5.6, 3.4, 6.4), (0, 0, 0), "skin"),
        c("rump", (0, -2.6, 3.6), (4.6, 2.8, 2.2), (0, 0, 0), "skin"),
    ])
    head = Group("head", pivot=(0, -3.2, -2.4), cubes=[
        c("skull", (0, -3.4, -4.0), (5.4, 2.6, 3.2), (0, 0, 0), "skin"),
        c("snout", (0, -3.0, -6.0), (3.8, 1.8, 0.9), (0, 0, 0), "skin"),
        c("eye_r", (2.3, -4.5, -4.2), (1.5, 1.5, 1.6), (0, 0, 0), "eye"),
        c("eye_l", (-2.3, -4.5, -4.2), (1.5, 1.5, 1.6), (0, 0, 0), "eye"),
    ])
    groups = [body, head]
    for name, sx in (("leg_fr", 1), ("leg_fl", -1)):
        groups.append(Group(name, pivot=(2.4 * sx, -2.2, -2.0), cubes=[
            c("arm", (3.3 * sx, -1.3, -2.4), (1.0, 2.2, 1.0), (0, 0, 0), "limb"),
            c("hand", (3.6 * sx, -0.2, -3.0), (1.8, 0.4, 1.6), (0, 0, 0), "toe")]))
    for name, sx in (("leg_br", 1), ("leg_bl", -1)):
        groups.append(Group(name, pivot=(2.6 * sx, -2.4, 3.0), cubes=[
            c("thigh", (3.6 * sx, -2.1, 2.6), (1.6, 1.6, 3.4), (0, -25 * sx, 0), "limb"),
            c("shin", (4.2 * sx, -0.9, 3.6), (1.2, 1.1, 3.0), (0, 15 * sx, 0), "limb"),
            c("foot", (4.3 * sx, -0.2, 1.6), (1.8, 0.4, 2.6), (0, 0, 0), "toe")]))
    return groups


def tadpole() -> list[Group]:
    body = Group("body", pivot=(0, -1.4, 0), cubes=[
        c("head", (0, -1.4, -0.6), (2.6, 2.0, 3.0), (0, 0, 0), "skin"),
        c("eye_r", (1.25, -1.9, -1.4), (0.4, 0.6, 0.6), (0, 0, 0), "eye"),
        c("eye_l", (-1.25, -1.9, -1.4), (0.4, 0.6, 0.6), (0, 0, 0), "eye"),
    ])
    tail = Group("tail", pivot=(0, -1.4, 0.9), cubes=[
        c("muscle", (0, -1.4, 2.0), (0.8, 0.9, 2.4), (0, 0, 0), "skin"),
        c("fin", (0, -1.4, 3.3), (0.3, 1.8, 3.6), (0, 0, 0), "fin"),
    ])
    return [body, tail]


def horned_frog() -> list[Group]:
    """Wide and low: a broad body under a low dome, a head as wide as the body with the mouth line across the front, the
    eyes with their little horns standing up from the flat top."""
    body = Group("body", pivot=(0, -3.0, 1.0), cubes=[
        c("back", (0, -2.2, 1.0), (11.0, 3.2, 10.0), (0, 0, 0), "skin"),
        c("dome", (0, -4.6, 1.0), (9.0, 1.8, 8.5), (0, 0, 0), "skin"),
        c("top", (0, -5.9, 1.2), (6.5, 1.0, 6.0), (0, 0, 0), "skin"),
        c("belly", (0, -0.35, 1.0), (10.0, 0.7, 9.0), (0, 0, 0), "belly"),
    ])
    head = Group("head", pivot=(0, -3.0, -3.5), cubes=[
        c("skull", (0, -3.6, -5.3), (11.4, 3.4, 3.6), (0, 0, 0), "head"),
        c("jaw", (0, -1.2, -5.0), (11.0, 1.6, 3.4), (0, 0, 0), "jaw"),
        c("eye_r", (3.2, -5.9, -4.6), (2.2, 1.6, 2.2), (0, 0, 0), "eye"),
        c("eye_l", (-3.2, -5.9, -4.6), (2.2, 1.6, 2.2), (0, 0, 0), "eye"),
        c("horn_r", (3.2, -7.0, -4.2), (1.4, 0.7, 1.4), (0, 0, 0), "horn"),
        c("horn_l", (-3.2, -7.0, -4.2), (1.4, 0.7, 1.4), (0, 0, 0), "horn"),
    ])
    groups = [body, head]
    for name, sx in (("leg_fr", 1), ("leg_fl", -1)):
        groups.append(Group(name, pivot=(5.0 * sx, -1.6, -2.4), cubes=[
            c("arm", (5.7 * sx, -0.9, -3.0), (1.8, 1.8, 1.8), (0, 0, 0), "limb"),
            c("hand", (6.1 * sx, -0.15, -3.7), (2.4, 0.3, 2.0), (0, 0, 0), "toe")]))
    for name, sx in (("leg_br", 1), ("leg_bl", -1)):
        groups.append(Group(name, pivot=(5.0 * sx, -1.8, 3.8), cubes=[
            c("thigh", (5.5 * sx, -1.3, 3.8), (2.6, 2.2, 4.2), (0, -20 * sx, 0), "limb"),
            c("foot", (6.0 * sx, -0.15, 2.0), (2.6, 0.3, 3.0), (0, 0, 0), "toe")]))
    return groups


# ---------------------------------------------------------------------------------------------- geckos

def _gecko_legs(z_front, z_back, spread, pad):
    groups = []
    for name, sx, z in (("leg_fr", 1, z_front), ("leg_fl", -1, z_front), ("leg_br", 1, z_back), ("leg_bl", -1, z_back)):
        groups.append(Group(name, pivot=(spread * sx, -1.6, z), cubes=[
            c("upper", ((spread + 1.0) * sx, -1.6, z), (2.0, 0.9, 0.9), (0, 0, 0), "limb"),
            c("lower", ((spread + 1.9) * sx, -0.8, z - 0.2), (0.9, 1.4, 0.9), (0, 0, 0), "limb"),
            c("foot", ((spread + 2.1) * sx, -0.15, z - 0.6), (pad, 0.3, pad), (0, 0, 0), "toe")]))
    return groups


def leopard_gecko() -> list[Group]:
    body = Group("body", pivot=(0, -1.9, 0), cubes=[c("trunk", (0, -1.9, 0.4), (4.2, 2.4, 8.6), (0, 0, 0), "skin")])
    head = Group("head", pivot=(0, -2.0, -3.8), cubes=[
        c("skull", (0, -2.3, -5.6), (4.0, 2.4, 3.8), (0, 0, 0), "head"),
        c("snout", (0, -2.0, -8.1), (2.8, 1.7, 1.4), (0, 0, 0), "head"),
        c("eye_r", (1.95, -2.9, -6.0), (0.5, 1.0, 1.1), (0, 0, 0), "eye"),
        c("eye_l", (-1.95, -2.9, -6.0), (0.5, 1.0, 1.1), (0, 0, 0), "eye"),
    ])
    tail = Group("tail", pivot=(0, -1.8, 4.6), cubes=[
        c("base", (0, -1.8, 6.2), (3.4, 2.1, 3.2), (0, 0, 0), "tail"),
        c("mid", (0, -1.7, 8.8), (3.2, 2.0, 2.4), (0, 0, 0), "tail"),
    ])
    tip = Group("tail_tip", pivot=(0, -1.6, 10.0), cubes=[
        c("tip", (0, -1.5, 11.2), (2.2, 1.5, 2.6), (0, 0, 0), "tail"),
        c("end", (0, -1.3, 13.3), (1.1, 1.0, 1.8), (0, 0, 0), "tail"),
    ])
    return [body, head, tail, tip] + _gecko_legs(-2.4, 3.4, 2.0, 1.5)


def crested_gecko() -> list[Group]:
    body = Group("body", pivot=(0, -1.9, 0), cubes=[
        c("trunk", (0, -1.9, 0.2), (3.6, 2.2, 7.6), (0, 0, 0), "skin"),
        c("crest_back_r", (1.2, -3.15, 0.2), (0.4, 0.5, 7.0), (0, 0, 0), "crest"),
        c("crest_back_l", (-1.2, -3.15, 0.2), (0.4, 0.5, 7.0), (0, 0, 0), "crest"),
    ])
    head = Group("head", pivot=(0, -2.0, -3.6), cubes=[
        c("skull", (0, -2.4, -5.4), (4.6, 2.8, 3.6), (0, 0, 0), "head"),
        c("snout", (0, -2.1, -7.8), (3.0, 1.8, 1.4), (0, 0, 0), "head"),
        c("crest_r", (2.2, -3.75, -5.2), (0.5, 0.7, 3.6), (0, 0, 0), "crest"),
        c("crest_l", (-2.2, -3.75, -5.2), (0.5, 0.7, 3.6), (0, 0, 0), "crest"),
        c("eye_r", (2.25, -2.8, -6.1), (0.6, 1.3, 1.4), (0, 0, 0), "eye"),
        c("eye_l", (-2.25, -2.8, -6.1), (0.6, 1.3, 1.4), (0, 0, 0), "eye"),
    ])
    tail = Group("tail", pivot=(0, -1.7, 4.0), cubes=[
        c("base", (0, -1.7, 5.4), (2.2, 1.6, 3.0), (0, 0, 0), "tail"),
        c("mid", (0, -1.6, 7.8), (1.7, 1.3, 2.4), (0, 0, 0), "tail"),
    ])
    tip = Group("tail_tip", pivot=(0, -1.5, 9.0), cubes=[
        c("tip", (0, -1.5, 10.0), (1.3, 1.1, 2.4), (0, 0, 0), "tail"),
        c("end", (0, -1.7, 12.0), (0.9, 0.9, 1.8), (0, 0, 0), "tail"),
    ])
    return [body, head, tail, tip] + _gecko_legs(-2.2, 3.0, 1.8, 1.8)


# ---------------------------------------------------------------------------------------------- M49: the mudskipper

def mudskipper() -> list[Group]:
    """A goby on land: a round, tapering body, a blunt head with the bulging eyes on top, two dorsal fins (the first a high
    sail), the pectoral fins as front legs (it skips on them), small pelvic fins under the belly, a rounded tail fin. The parts
    carry the gecko's names, so it moves with the gecko's model."""
    body = Group("body", pivot=(0, -1.8, 0), cubes=[
        c("trunk", (0, -1.8, 0.0), (2.8, 2.4, 5.6), (0, 0, 0), "skin"),
        c("dorsal1", (0, -3.7, -0.4), (0.3, 1.6, 2.6), (0, 0, 0), "fin"),
    ])
    head = Group("head", pivot=(0, -1.9, -2.8), cubes=[
        c("skull", (0, -2.0, -4.2), (3.0, 2.6, 2.8), (0, 0, 0), "head"),
        c("snout", (0, -1.7, -6.0), (2.4, 1.6, 0.9), (0, 0, 0), "head"),
        c("eye_r", (0.75, -3.65, -4.6), (0.9, 0.9, 0.9), (0, 0, 0), "eye"),
        c("eye_l", (-0.75, -3.65, -4.6), (0.9, 0.9, 0.9), (0, 0, 0), "eye"),
    ])
    tail = Group("tail", pivot=(0, -1.7, 2.8), cubes=[
        c("base", (0, -1.7, 4.2), (2.1, 1.9, 2.8), (0, 0, 0), "skin"),
        c("dorsal2", (0, -3.1, 4.0), (0.3, 1.0, 2.4), (0, 0, 0), "fin"),
    ])
    tip = Group("tail_tip", pivot=(0, -1.6, 5.6), cubes=[
        c("tip", (0, -1.6, 6.7), (1.3, 1.4, 2.2), (0, 0, 0), "skin"),
        c("end", (0, -1.7, 8.6), (0.3, 2.4, 1.8), (0, 0, 0), "fin"),
    ])
    legs = []
    for name, sx, z, big in (("leg_fr", 1, -1.6, True), ("leg_fl", -1, -1.6, True), ("leg_br", 1, 1.0, False), ("leg_bl", -1, 1.0, False)):
        if big:  # the pectoral fins: a muscular base and a broad fan on the ground
            cubes = [c("upper", (1.9 * sx, -1.3, z), (1.2, 0.7, 0.9), (0, 0, 0), "limb"),
                     c("lower", (2.4 * sx, -0.6, z - 0.1), (0.7, 0.9, 0.8), (0, 0, 0), "limb"),
                     c("foot", (2.7 * sx, -0.15, z - 0.4), (1.6, 0.3, 1.6), (0, 0, 0), "finfoot")]
        else:  # the small pelvic fins under the belly
            cubes = [c("upper", (0.9 * sx, -0.7, z), (0.6, 0.5, 0.6), (0, 0, 0), "limb"),
                     c("lower", (1.0 * sx, -0.4, z + 0.2), (0.5, 0.4, 0.5), (0, 0, 0), "limb"),
                     c("foot", (1.1 * sx, -0.15, z + 0.4), (0.9, 0.3, 0.9), (0, 0, 0), "finfoot")]
        legs.append(Group(name, pivot=(1.4 * sx if big else 0.7 * sx, -1.3 if big else -0.7, z), cubes=cubes))
    return [body, head, tail, tip] + legs


def _mudskipper(cu, face, px, py, pz, rng):
    """Grey-olive with darker blotches and tiny blue spots, a pale belly; the dorsal fins dusky with a blue and orange edge;
    dark bulging eyes."""
    if cu.pattern == "eye":
        return np.array([0.10, 0.10, 0.09]) if face != "top" else np.array([0.55, 0.50, 0.32])
    if cu.pattern == "fin":
        edge = py < -4.2 or (cu.name == "end" and abs(py + 1.7) > 0.9)
        if edge:
            return np.array([0.25, 0.55, 0.85]) if rng.random() < 0.6 else np.array([0.92, 0.55, 0.22])
        return np.clip(np.array([0.36, 0.34, 0.28]) * (0.9 + 0.2 * rng.random()), 0, 1)
    if cu.pattern == "finfoot":
        return np.clip(np.array([0.48, 0.45, 0.36]) * _shade(face), 0, 1)
    if face == "bottom":
        return np.clip(np.array([0.84, 0.80, 0.70]) * _shade(face) + (rng.random() - 0.5) * 0.03, 0, 1)
    col = np.array([0.46, 0.44, 0.34])
    cell = (math.floor(px * 1.3), math.floor(pz * 1.3), math.floor(py * 1.3))
    h = ((cell[0] * 73856093) ^ (cell[1] * 19349663) ^ (cell[2] * 83492791)) % 100
    if h < 28:
        col = np.array([0.30, 0.29, 0.22])
    elif h > 93:
        col = np.array([0.35, 0.62, 0.92])  # the little blue spots
    if face in ("east", "west") and py > -1.2:
        col = np.array([0.74, 0.70, 0.60])
    return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)


# ---------------------------------------------------------------------------------------------- the snake

SNAKE_SEGMENTS = 10
SEG_LEN = 3.8


def _seg_width(i):
    return [2.0, 2.3, 2.5, 2.6, 2.6, 2.5, 2.3, 2.0, 1.5, 0.9][i]


def corn_snake() -> list[Group]:
    groups = [Group("head", pivot=(0, -0.9, -13.8), cubes=[
        c("skull", (0, -1.0, -15.5), (2.8, 1.6, 3.4), (0, 0, 0), "head"),
        c("snout", (0, -0.85, -17.7), (2.0, 1.2, 1.2), (0, 0, 0), "head"),
        c("eye_r", (1.35, -1.45, -15.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
        c("eye_l", (-1.35, -1.45, -15.9), (0.3, 0.6, 0.6), (0, 0, 0), "eye"),
        # M54: the forked tongue, out of the mouth in short flicks (SnakeModel scales it along z and hides it in between)
        c("tongue", (0, -0.55, -19.3), (0.5, 0.15, 2.0), (0, 0, 0), "tongue"),
    ])]
    for i in range(SNAKE_SEGMENTS):
        w = _seg_width(i)
        h = round(w * 0.78, 2)
        z = -12.0 + i * (SEG_LEN - 0.2)
        groups.append(Group(f"seg{i}", pivot=(0, -h / 2, z), cubes=[c("scales", (0, -h / 2, z), (w, h, SEG_LEN), (0, 0, 0), "skin")]))
    return groups


# ---------------------------------------------------------------------------------------------- colours

BLACK = np.array([0.05, 0.05, 0.06])


def _dart(kind: str):
    base = {"tinctorius": np.array([0.32, 0.62, 0.95]), "auratus": np.array([0.30, 0.82, 0.52]),
            "leucomelas": np.array([0.98, 0.80, 0.10])}[kind]
    limb = {"tinctorius": np.array([0.10, 0.28, 0.72]), "auratus": np.array([0.24, 0.70, 0.45]),
            "leucomelas": np.array([0.10, 0.10, 0.10])}[kind]

    def colour(cu, face, px, py, pz, rng):
        if cu.pattern == "eye":
            return np.array([0.02, 0.02, 0.03]) if face != "top" else np.array([0.12, 0.12, 0.14])
        if kind == "tinctorius":
            col = limb if cu.pattern in ("limb", "toe") else base
            spot = math.sin(px * 2.6 + 1.3) * math.sin(pz * 2.2 + py * 1.7) > 0.62
            if spot and face != "bottom":
                col = BLACK
            if face == "bottom":
                col = limb * 0.9
        elif kind == "auratus":
            blot = math.sin(px * 1.4 + pz * 1.1) * math.cos(pz * 1.6 - px * 0.8) > 0.25
            col = BLACK * 1.4 if blot else base
            if face == "bottom":
                col = base * 0.8
        else:
            band = abs(math.sin(pz * 1.1 + 0.4 * math.sin(px * 1.5))) < 0.42
            col = BLACK * 1.3 if band else base
            if cu.pattern in ("limb", "toe"):
                col = base if (int(pz * 1.3) % 2) else BLACK * 1.3
            if face == "bottom":
                col = BLACK * 1.6
        if cu.pattern == "toe":
            col = col * 1.1
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)
    return colour


def _tadpole(cu, face, px, py, pz, rng):
    if cu.pattern == "eye":
        return np.array([0.03, 0.03, 0.03])
    if cu.pattern == "fin":
        col = np.array([0.60, 0.56, 0.48]) if abs(py + 1.4) < 0.3 else np.array([0.80, 0.78, 0.70])
    else:
        col = np.array([0.26, 0.23, 0.18]) if face != "bottom" else np.array([0.45, 0.42, 0.36])
        if face != "bottom" and math.sin(px * 4.1) * math.sin(pz * 3.7) > 0.6:
            col = col * 0.7
    return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)


def _horned(green: bool):
    ground = np.array([0.46, 0.70, 0.22]) if green else np.array([0.62, 0.50, 0.32])
    blotch = np.array([0.28, 0.20, 0.10]) if green else np.array([0.30, 0.20, 0.12])
    rim = np.array([0.72, 0.30, 0.14]) if green else np.array([0.50, 0.30, 0.16])
    belly = np.array([0.90, 0.86, 0.70])

    def colour(cu, face, px, py, pz, rng):
        if cu.pattern == "eye":
            return np.array([0.04, 0.03, 0.02]) if face in ("north", "east", "west") and abs(py + 5.9) < 0.3 else np.array([0.80, 0.66, 0.22])
        if cu.pattern in ("belly", "jaw") or face == "bottom":
            col = belly
            if cu.pattern == "jaw" and face in ("north", "east", "west") and py < -1.6:
                col = np.array([0.10, 0.08, 0.06])  # the line of the huge mouth
            return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)
        # rounded blotches with a reddish rim on the light ground
        v = math.sin(px * 0.9 + 0.7) * math.sin(pz * 0.85 - 0.3) + 0.35 * math.sin(px * 2.1 - pz * 1.7)
        col = ground
        if v > 0.55:
            col = blotch
        elif v > 0.4:
            col = rim
        if cu.pattern == "horn":
            col = ground * 0.85
        if cu.pattern == "head" and face in ("north", "east", "west") and py > -2.5:
            col = np.array([0.12, 0.09, 0.06])  # the wide mouth line all round the front
        if cu.pattern in ("limb", "toe") and int(px * 2 + pz) % 3 == 0:
            col = blotch
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)
    return colour


def _leopard(morph: str):
    ground = {"normal": np.array([0.92, 0.80, 0.30]), "tangerine": np.array([0.96, 0.52, 0.12]),
              "albino": np.array([0.96, 0.86, 0.62])}[morph]
    spot = {"normal": np.array([0.10, 0.09, 0.08]), "tangerine": np.array([0.35, 0.18, 0.08]),
            "albino": np.array([0.70, 0.52, 0.36])}[morph]
    band = {"normal": np.array([0.16, 0.14, 0.12]), "tangerine": np.array([0.98, 0.45, 0.08]),
            "albino": np.array([0.76, 0.60, 0.46])}[morph]
    belly = np.array([0.95, 0.92, 0.86])
    eye = {"normal": np.array([0.12, 0.12, 0.10]), "tangerine": np.array([0.15, 0.13, 0.10]), "albino": np.array([0.75, 0.25, 0.25])}[morph]

    def colour(cu, face, px, py, pz, rng):
        if cu.pattern == "eye":
            return eye
        if face == "bottom":
            return np.clip(belly * _shade(face) + (rng.random() - 0.5) * 0.03, 0, 1)
        col = ground
        if cu.pattern == "tail":
            # the banded tail: light and dark rings (the tangerine's carrot tail stays orange)
            col = band if math.sin(pz * 1.5) > 0.2 else (ground * 1.05 if morph != "tangerine" else ground)
            if morph == "tangerine":
                col = band
        cell = (math.floor(px * 1.6), math.floor(pz * 1.6), math.floor(py * 1.6))
        h = ((cell[0] * 73856093) ^ (cell[1] * 19349663) ^ (cell[2] * 83492791)) % 100
        dots = h < (30 if morph != "tangerine" else 8)
        if dots and cu.pattern != "toe":
            col = spot
        if face in ("east", "west") and py > -1.0:
            col = belly * 0.95
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)
    return colour


def _crested(morph: str):
    ground = {"tiger": np.array([0.66, 0.46, 0.26]), "flame": np.array([0.55, 0.24, 0.14]),
              "harlequin": np.array([0.82, 0.36, 0.14])}[morph]
    pattern = {"tiger": np.array([0.30, 0.18, 0.10]), "flame": np.array([0.94, 0.86, 0.66]),
               "harlequin": np.array([0.95, 0.88, 0.70])}[morph]

    def colour(cu, face, px, py, pz, rng):
        if cu.pattern == "eye":
            return np.array([0.70, 0.55, 0.25]) if abs(pz + 6.1) > 0.3 else np.array([0.05, 0.04, 0.03])
        col = ground
        if morph == "tiger":
            if face != "bottom" and abs(math.sin(pz * 1.25 + px * 0.25)) < 0.33:
                col = pattern
        elif morph == "flame":
            if face == "top" and abs(px) < 1.3:
                col = pattern  # the cream dorsal stripe
        else:
            if face in ("east", "west") and math.sin(pz * 1.6) * math.sin(py * 2.2) > 0.1 or cu.pattern in ("limb", "toe"):
                col = pattern  # cream on the sides and legs
            if face == "top" and abs(px) < 1.0:
                col = pattern
        if cu.pattern == "crest":
            col = pattern * 0.95 if morph != "tiger" else ground * 1.25
        if face == "bottom":
            col = ground * 1.2
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.05, 0, 1)
    return colour


def _corn(morph: str):
    ground = {"wild": np.array([0.85, 0.52, 0.30]), "amelanistic": np.array([0.98, 0.62, 0.36]),
              "anerythristic": np.array([0.62, 0.62, 0.64])}[morph]
    saddle = {"wild": np.array([0.80, 0.18, 0.10]), "amelanistic": np.array([0.92, 0.22, 0.14]),
              "anerythristic": np.array([0.22, 0.22, 0.24])}[morph]
    border = {"wild": np.array([0.06, 0.05, 0.05]), "amelanistic": np.array([0.98, 0.95, 0.90]),
              "anerythristic": np.array([0.05, 0.05, 0.06])}[morph]
    eye = np.array([0.80, 0.20, 0.18]) if morph == "amelanistic" else np.array([0.18, 0.10, 0.06])

    def colour(cu, face, px, py, pz, rng):
        if cu.pattern == "eye":
            return eye
        if cu.pattern == "tongue":  # M54: dark red, black at the fork
            return np.array([0.10, 0.04, 0.05]) if pz < -19.9 else np.array([0.42, 0.08, 0.10])
        if face == "bottom":
            # the checkered belly (no black in the amelanistic one): one row per texel row of the segments (their 3.8 px
            # are 4 texels of 0.95 px), the dark checks on alternate sides of the pale middle (M53: the old 1.2 x 1 px
            # grid beat against the texels, rows and columns doubled at random and the belly looked broken)
            row = math.floor(pz / 0.95)
            check = (px < -0.3 and row % 2 == 0) or (px > 0.3 and row % 2 == 1)
            dark = np.array([0.08, 0.08, 0.08]) if morph != "amelanistic" else np.array([0.95, 0.70, 0.55])
            col = dark if check else np.array([0.95, 0.94, 0.90])
            return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.03, 0, 1)
        if cu.pattern == "head":
            col = ground
            if face == "top" and abs(px) < 0.5 + 0.6 * math.sin(pz * 1.3):
                col = saddle  # the spear-point mark on the head
            if face in ("east", "west") and abs(py + 1.1) < 0.18:
                col = saddle * 0.9  # the stripe through the eye
            return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)
        # saddles along the back, smaller blotches low on the sides
        u = (pz + 20.0) % 3.4
        col = ground
        if face == "top" or py < -0.9:
            if 0.5 < u < 2.5:
                col = saddle
            elif 0.25 < u < 2.75:
                col = border
        elif (u + 1.7) % 3.4 < 0.9:
            col = saddle * 0.9
        return np.clip(col * _shade(face) + (rng.random() - 0.5) * 0.04, 0, 1)
    return colour


MODELS = {"dart_frog": dart_frog, "dart_frog_tadpole": tadpole, "horned_frog": horned_frog, "leopard_gecko": leopard_gecko,
          "crested_gecko": crested_gecko, "corn_snake": corn_snake, "mudskipper": mudskipper}
COLOURS = {"dart_frog_tadpole": _tadpole, "mudskipper": _mudskipper}
TEXTURES = {
    "dart_frog": {f"dart_frog_{k}": _dart(k) for k in ("tinctorius", "auratus", "leucomelas")},
    "horned_frog": {"horned_frog_green": _horned(True), "horned_frog_brown": _horned(False)},
    "leopard_gecko": {f"leopard_gecko_{k}": _leopard(k) for k in ("normal", "tangerine", "albino")},
    "crested_gecko": {f"crested_gecko_{k}": _crested(k) for k in ("tiger", "flame", "harlequin")},
    "corn_snake": {f"corn_snake_{k}": _corn(k) for k in ("wild", "amelanistic", "anerythristic")},
}
