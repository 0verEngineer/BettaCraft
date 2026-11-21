"""M55: an own icon for every animal of the mod in its container (bucket, fish bag, transport tub).

Before M55 every tankmate bucket showed the betta's silhouette in two tints and every small animal (shrimp, crayfish, snails,
hermit crabs) the same shrimp. Now every animal has its own sprite:

* the fish (tankmates, the big animals in the tub) are rendered from their own models and textures (``tankmates.py``: the
  same geometry and box-UV paint the game uses) from the side (the rays from above), reduced to the icon size with a
  coverage vote, given a darker rim and an eye;
* the small animals are drawn as pixel sprites per species (shell shapes and patterns of the snails, the shrimp and
  crayfish in their colours); the species whose colour comes from their genes (Neocaridina, Caridina, apple snail,
  ramshorn, the crayfish) get a grey body layer that the game tints like before plus an untinted detail layer.

Writes ``textures/item/animal/<id>.png`` (the sprite in the bucket's water window), ``<id>_body.png`` (the tinted body of
the gene-coloured small animals), ``textures/item/animal/tub_<id>.png`` (a big animal sticking out of the tub) and the
empty containers ``animal_bucket``, ``animal_bucket_glass``, ``animal_bag`` and ``animal_tub``. The item models are made by
the datagen (``DataGenM55``); ``build/preview/animal_icons.png`` shows them all.

Run from the repo root:  py tools/assetgen/animal_icons.py
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image

import tankmates as tm
from common import ITEM_TEX, PREVIEW, hex_f, save_png

S = 16
ANIMAL_TEX = ITEM_TEX / "animal"


# ---------------------------------------------------------------------------------------------------------- rendering

def _splat_render(groups, tex, view, scale, hide=()):
    """RGBA render (transparent background) of a tankmate model: side (from +x, head left) or top (head left)."""
    pts = [p for p in tm.splats(groups, tex, sub=4) if p[2] not in hide]
    xs, ys = [], []
    for p, _, _ in pts:
        x, y, z = p
        sx, sy = (z, y) if view == "side" else (z, x)
        xs.append(sx)
        ys.append(sy)
    x0, y0 = min(xs) - 1, min(ys) - 1
    W = int((max(xs) - x0 + 1) * scale) + 2
    H = int((max(ys) - y0 + 1) * scale) + 2
    img = np.zeros((H, W, 4))
    depth = np.full((H, W), -1e9)
    for (p, col, _), sx, sy in zip(pts, xs, ys):
        d = p[0] if view == "side" else -p[1]
        px, py = int((sx - x0) * scale), int((sy - y0) * scale)
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                qx, qy = px + dx, py + dy
                if 0 <= qx < W and 0 <= qy < H and d > depth[qy, qx]:
                    depth[qy, qx] = d
                    img[qy, qx, :3] = col
                    img[qy, qx, 3] = 1
    return img, (x0, y0)


def _crop(img):
    a = img[..., 3] > 0
    ys, xs = np.where(a)
    return img[ys.min():ys.max() + 1, xs.min():xs.max() + 1], (xs.min(), ys.min())


def _reduce(img, w, h, vote=0.45):
    """Area reduction to w x h: a pixel is covered when ``vote`` of its area is; its colour is the mean of the covered part."""
    H, W = img.shape[:2]
    out = np.zeros((h, w, 4))
    for j in range(h):
        for i in range(w):
            ya, yb = int(j * H / h), max(int(j * H / h) + 1, int((j + 1) * H / h))
            xa, xb = int(i * W / w), max(int(i * W / w) + 1, int((i + 1) * W / w))
            cell = img[ya:yb, xa:xb]
            a = cell[..., 3]
            if a.mean() >= vote:
                out[j, i, :3] = (cell[..., :3] * a[..., None]).sum(axis=(0, 1)) / a.sum()
                out[j, i, 3] = 1
    return out


def _rim(sprite, dark=0.7):
    """Darkens the outermost pixels of the silhouette (reads better on the water) and saturates a little."""
    a = sprite[..., 3] > 0
    out = sprite.copy()
    h, w = a.shape
    for y in range(h):
        for x in range(w):
            if not a[y, x]:
                continue
            edge = any(not (0 <= y + dy < h and 0 <= x + dx < w) or not a[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            rgb = out[y, x, :3]
            m = rgb.mean()
            rgb = np.clip(m + (rgb - m) * 1.25, 0, 1)
            out[y, x, :3] = rgb * dark if edge else rgb
    return out


def fish_sprite(sp, model, varieties, dimorphic, index, box=(12, 6), view="side", vote=0.45):
    """The sprite of a tankmate (wild variety, male): rendered, cropped, reduced to fit ``box`` keeping the proportions."""
    plan, groups = PLANS_BUILT[model]
    var = varieties[0]
    tex = tm.paint(plan, groups, tm.looks(sp, var, False), 1900 + index)
    hide = ("female",) if dimorphic else ()
    img, origin = _splat_render(groups, tex, view, 6, hide)
    img, off = _crop(img)
    H, W = img.shape[:2]
    bw, bh = box
    k = min(bw / W, bh / H)
    w, h = max(3, round(W * k)), max(2, round(H * k))
    small = _reduce(img, w, h, vote)
    # the eye: the right eye box of the model, projected
    eye = None
    for g in groups:
        for b in g.boxes:
            if b.name == "eye_r" and view == "side":
                cz, cy = b.center[2], b.center[1]
                ex = ((cz - origin[0]) * 6 - off[0]) / W * w
                ey = ((cy - origin[1]) * 6 - off[1]) / H * h
                eye = (int(ex), int(ey))
    small = _rim(small)
    if eye and 0 <= eye[1] < h and 0 <= eye[0] < w and small[eye[1], eye[0], 3] > 0:
        small[eye[1], eye[0], :3] = 0.06
    return small


PLANS_BUILT = {}


def _build_plans():
    for name, fn in tm.PLANS.items():
        plan, groups = fn()
        tm.pack(groups)
        PLANS_BUILT[name] = (plan, groups)


def preview_fish(path):
    _build_plans()
    tiles = []
    names = []
    for i, (sp, model, varieties, dimorphic) in enumerate(tm.SPECIES):
        rays = model in ("stingray", "eagle_ray", "cownose_ray")
        s = fish_sprite(sp, model, varieties, dimorphic, i, box=(12, 7) if not rays else (12, 9), view="top" if rays else "side")
        tile = np.zeros((S, S, 4))
        tile[..., :3] = hex_f("#3a6bc4")
        tile[..., 3] = 1
        h, w = s.shape[:2]
        y0, x0 = 8 - h // 2, 8 - w // 2
        m = s[..., 3] > 0
        tile[y0:y0 + h, x0:x0 + w][m] = s[m]
        tiles.append(tile)
        names.append(sp)
    cols = 10
    rows = (len(tiles) + cols - 1) // cols
    sheet = np.zeros((rows * S, cols * S, 4))
    for k, t in enumerate(tiles):
        r, c = divmod(k, cols)
        sheet[r * S:(r + 1) * S, c * S:(c + 1) * S] = t
    img = Image.fromarray((np.clip(sheet, 0, 1) * 255).astype(np.uint8), "RGBA").resize((cols * S * 8, rows * S * 8), Image.NEAREST)
    img.save(path)
    return names



# ---------------------------------------------------------------------------------------------------------- small animals

# The sprites are 12 x 7 (columns 2..13, rows 7..13 of the icon: the water of the bucket). Head (or opening) to the left.
# Body letters (the layer the game tints for the gene-coloured species, else in the species' colours): B base, b dark,
# h light; shell letters S, s, z (base, dark, light) are the body too. The rest are fixed colours (PAL).
_SHRIMP = ["............",
           ".a..........",
           "a.a.hhhh....",
           "...ehBBBBh..",
           "...bBBBBBBbt",
           "....l.l.lbtt",
           "..........t."]
_CRAYFISH = ["............",
             "cc..........",
             "ccc.hhhh....",
             ".cehBBBBBh..",
             "cc.bBBBBBBbt",
             "c...l.l.l.tt",
             "...........t"]
# the dwarf crayfish (Cambarellus, 3 to 4 cm) is smaller than the Florida and marbled crayfish (about 10 cm)
_DWARF_CRAYFISH = ["............",
                   "............",
                   "..c.........",
                   "..cchhhh....",
                   "...ebBBBBbt.",
                   "..c..l.l.ltt",
                   "............"]
_NERITE = ["............",
           "............",
           "....zSSS....",
           "...zSSSSS...",
           "..SSSSSSSs..",
           ".tFFFFFFFF..",
           "............"]
_APPLE = ["....zzSS....",
          "..zSSSSSSS..",
          ".zSSsSSSSSs.",
          ".SSssSSSSSs.",
          ".SSSSSSSSss.",
          "t.sSSSSSss..",
          "tFFFFFFFFF.."]
_RAMSHORN = ["............",
             "...zSSSs....",
             "..zSsssSs...",
             "..SsSzSsS...",
             "..SsSssSs...",
             "..sSSSSs....",
             "tFFFFFFF...."]
_BLADDER = ["............",
            "............",
            "......zS....",
            ".....zSSs...",
            "....SSSSs...",
            "..tFFSSs....",
            "............"]
_TRUMPET = ["............",
            ".........S..",
            "........SSs.",
            "......zSSs..",
            "....zSSSs...",
            "..zSSSSs....",
            ".tFFFFF....."]
_ASSASSIN = ["............",
             "........SS..",
             "......zSSs..",
             ".....zSSSs..",
             "...zSSSSs...",
             ".tFFFFFFF...",
             "............"]
_TYLOMELANIA = ["...........S",
                ".........zSs",
                ".......zSSs.",
                ".....zSSSSs.",
                "...zSSSSSs..",
                "ttFFFFFFF...",
                "............"]
_TURBO = ["............",
          "......zS....",
          ".....zSSs...",
          "....zSSSSs..",
          "...SSSSSSSs.",
          "..tFFFFFFF..",
          "............"]
_HERMIT = ["............",
           ".....zSSs...",
           "....zSSSSs..",
           "...zSSSSSSs.",
           ".cceSSSSSSs.",
           "cc.l.l.l....",
           "............"]

PAL = {
    "k": "#141414", "e": "#0c0c0c", "w": "#f4f2ea", "W": "#c8c8c4", "r": "#d02a22", "R": "#7a1410", "o": "#e07a24",
    "y": "#f0cc30", "Y": "#a8841c", "n": "#7a5634", "N": "#3e2a1a", "g": "#9a9a92", "G": "#5a5a54", "u": "#3a74e0",
    "U": "#1c3c8a", "c": "#c46a3a", "p": "#e8a0a8", "v": "#6e7a36", "V": "#3e4a1e", "F": "#a89a86", "t": "#c8bca8",
    "l": "#8a6a52", "a": "#c8b8a0",
}
_E = "............"

# per species: (sprite, body colour or None for the gene-tinted ones, overlay rows ('.' = keep; only on the animal),
# overrides of the fixed letters: a colour, a PAL letter, or None = the letter is part of the body)
CRITTER_SPRITES = {
    "neocaridina": (_SHRIMP, None, None, {"l": "#9a8a7a", "a": "#b0a090", "t": None}),
    "caridina": (_SHRIMP, None, [_E, _E, _E, "......w..w..", ".....w..w...", _E, _E], {"t": "w", "a": "#e8e4dc"}),
    "sulawesi_shrimp": (_SHRIMP, "#c8261e", [_E, _E, ".....w......", "......w..w..", "....w...w...", _E, _E], {"t": None, "a": "#e8d8d0"}),
    "cleaner_shrimp": (_SHRIMP, "#e8a030", [_E, _E, "....rwwr....", "....rrwwrr..", _E, _E, _E], {"a": "#f4f4f0", "l": "#f4e8d8", "t": "#d82a24"}),
    "fire_shrimp": (_SHRIMP, "#b01418", [_E, _E, "......w.....", ".........w..", ".....w......", _E, _E], {"a": "#f4f4f0", "l": "#f4f4f0", "t": None}),
    "amano_shrimp": (_SHRIMP, "#8c9078", [_E, _E, _E, ".....G.G.G..", "....G.G.G...", _E, _E], {"t": None, "a": "#a8a894", "l": "#a8a894"}),
    "dwarf_crayfish": (_DWARF_CRAYFISH, None, None, {"c": None, "t": None, "l": None}),
    "florida_crayfish": (_CRAYFISH, None, None, {"c": None, "t": None, "l": None}),
    "marbled_crayfish": (_CRAYFISH, "#6a6248", [_E, _E, ".....N..N...", "....N.N...N.", "...N...N.N..", _E, _E], {"c": None, "t": None, "l": None}),
    "zebra_nerite": (_NERITE, "#e2c23a", [_E, _E, "....k.k.....", "...k.k.k....", "..k.k.k.k...", _E, _E], {}),
    "horned_nerite": (_NERITE, "#d8b248", [_E, "....k..k....", "....N.N.....", "...N.N.N....", _E, _E, _E], {}),
    "olive_nerite": (_NERITE, "#5e6a2e", [_E, _E, _E, "......V.....", "....V...V...", _E, _E], {}),
    "red_nerite": (_NERITE, "#c4421e", [_E, _E, ".....k.k....", "...k..k..k..", "....k...k...", _E, _E], {}),
    "yellow_nerite": (_NERITE, "#f0d030", [_E, _E, _E, "....k...k...", "......k.....", _E, _E], {}),
    "apple_snail": (_APPLE, None, None, {"F": "#5a4a3a", "t": "#4a3a2a"}),
    "ramshorn": (_RAMSHORN, None, None, {"F": "#6a4a3a", "t": "#5a3a2a"}),
    "bladder_snail": (_BLADDER, "#a08058", [_E, _E, _E, "......N.....", ".....N......", _E, _E], {"F": "#7a6a5a"}),
    "trumpet_snail": (_TRUMPET, "#6e5a40", [_E, _E, ".........r..", ".......r....", ".....r..r...", "...r..r.....", _E], {"F": "#8a7a6a"}),
    "assassin_snail": (_ASSASSIN, "#d0a030", [_E, "........NN..", "......NN....", ".....N...N..", "...N...N....", _E, _E], {"F": "#c0b090"}),
    "tylomelania": (_TYLOMELANIA, "#3a2c22", [_E, "..........k.", "........k...", "......k...k.", "....k...k...", _E, _E], {"F": "#e88a2a", "t": "#f0a040"}),
    "turbo_snail": (_TURBO, "#6a6a3e", [_E, _E, "......V.....", ".....V.V....", "....V...V...", _E, _E], {"F": "#9a8a70"}),
    "hermit_crab": (_HERMIT, "#c8b08a", [_E, _E, ".....n......", "....n...n...", "......n.....", _E, _E], {"c": "#3a6ad0", "l": "#3a6ad0"}),
}
# the gene-coloured species: their body layer is grey (tinted by the game with the colour of the animal's genes)
TINTED_CRITTERS = [k for k, v in CRITTER_SPRITES.items() if v[1] is None]
# the colours of the preview (a typical animal of each gene-coloured species)
PREVIEW_TINT = {"neocaridina": "#d02a22", "caridina": "#c8282a", "dwarf_crayfish": "#e07a24", "florida_crayfish": "#3a74e0",
                "apple_snail": "#e8b830", "ramshorn": "#c84a3a"}


def _shade(rgb, f):
    return np.clip(np.asarray(rgb) * f, 0, 1)


def critter_layers(sid):
    """(body, detail) 12 x 7 RGBA arrays of a small animal; the body is grey for the gene-tinted species."""
    grid, base, overlay, over = CRITTER_SPRITES[sid]
    pal = dict(PAL)
    for k, v in over.items():
        pal[k] = None if v is None else PAL[v] if v in PAL else v
    tinted = base is None
    body_rgb = np.array([0.82, 0.82, 0.82]) if tinted else np.array(hex_f(base))
    shades = {"B": 1.0, "S": 1.0, "b": 0.72, "s": 0.72, "h": 1.18, "z": 1.22}
    h, w = len(grid), len(grid[0])
    body = np.zeros((h, w, 4))
    detail = np.zeros((h, w, 4))
    for y in range(h):
        for x in range(w):
            ch = grid[y][x]
            if ch == ".":
                continue
            if ch in shades or pal.get(ch, 0) is None:
                f = shades.get(ch, {"l": 0.62, "c": 0.8, "t": 0.9, "a": 0.95}.get(ch, 0.8))
                body[y, x, :3] = _shade(body_rgb, f)
                body[y, x, 3] = 1
            else:
                detail[y, x, :3] = hex_f(pal[ch])
                detail[y, x, 3] = 1
    if overlay:
        for y, row in enumerate(overlay):
            for x, ch in enumerate(row):
                if ch == "." or (body[y, x, 3] == 0 and detail[y, x, 3] == 0):
                    continue
                detail[y, x, :3] = hex_f(PAL[ch])
                detail[y, x, 3] = 1
                body[y, x, 3] = 0
    return body, detail


def preview_critters(path):
    tiles = []
    for sid in CRITTER_SPRITES:
        body, detail = critter_layers(sid)
        if sid in TINTED_CRITTERS:
            body[..., :3] = np.clip(body[..., :3] * np.array(hex_f(PREVIEW_TINT[sid])) / 0.82, 0, 1)
        tile = np.zeros((S, S, 4))
        tile[..., :3] = hex_f("#3a6bc4")
        tile[..., 3] = 1
        for layer in (body, detail):
            m = layer[..., 3] > 0
            tile[7:14, 2:14][m] = layer[m]
        tiles.append(tile)
    cols = 11
    rows = (len(tiles) + cols - 1) // cols
    sheet = np.zeros((rows * S, cols * S, 4))
    for k, t in enumerate(tiles):
        r, c = divmod(k, cols)
        sheet[r * S:(r + 1) * S, c * S:(c + 1) * S] = np.clip(t, 0, 1)
    Image.fromarray((sheet * 255).astype(np.uint8), "RGBA").resize((cols * S * 8, rows * S * 8), Image.NEAREST).save(path)



# ---------------------------------------------------------------------------------------------------------- the icons

# the water window of the bucket (and the fish bag): columns 2..13, rows 7..13; a fish is centred on row 10.5
WIN_X, WIN_Y, WIN_W, WIN_H = 2, 7, 12, 7
# tall fish get more height (up to the window), the rays are seen from above
TALL = {"angel": (10, 7), "discus": (10, 7), "seahorse": (6, 7), "tang": (11, 7), "puffer": (11, 6), "clown": (12, 6),
        "cichlid": (12, 6), "apisto": (12, 6), "damsel": (12, 6)}
RAYS = ("stingray", "eagle_ray", "cownose_ray")
TUB_BOX = (14, 7)
# thin models keep their thin parts (a lower coverage vote)
THIN = {"seahorse": 0.22, "kuhli": 0.3, "angel": 0.3}


def _place(sprite, cx=None, cy=10.5):
    """The sprite on a 16 x 16 canvas, centred in the water window."""
    img = np.zeros((S, S, 4))
    h, w = sprite.shape[:2]
    cx = WIN_X + WIN_W / 2 if cx is None else cx
    x0 = int(round(cx - w / 2))
    y0 = int(round(cy - h / 2))
    y0 = max(WIN_Y, min(y0, WIN_Y + WIN_H - h))
    x0 = max(1, min(x0, S - 1 - w))
    m = sprite[..., 3] > 0
    img[y0:y0 + h, x0:x0 + w][m] = sprite[m]
    return img


def _tub_place(sprite):
    """A big animal lying in the tub: its belly behind the lower front wall (drawn over it), the rest over the rim."""
    img = np.zeros((S, S, 4))
    h, w = sprite.shape[:2]
    x0 = (S - w) // 2
    y0 = 9 - h  # lying in the tub: its belly behind the front wall (row 9 down), head and tail over the rim
    for y in range(h):
        for x in range(w):
            if sprite[y, x, 3] > 0 and 0 <= y0 + y < S:
                img[y0 + y, x0 + x] = sprite[y, x]
    return img


def tub_front():
    """The lower front wall of the transport tub (rows 9 down), drawn over the animal's belly."""
    tub = np.asarray(Image.open(ITEM_TEX / "transport_tub.png").convert("RGBA")).astype(np.float64) / 255
    out = tub.copy()
    out[:9] = 0
    return out


def fish_icons():
    """{name: 16 x 16 RGBA} of every tankmate: ``<id>`` in the bucket window, ``tub_<id>`` for the big ones."""
    _build_plans()
    out = {}
    for i, (sp, model, varieties, dimorphic) in enumerate(tm.SPECIES):
        big = model in ("pangasius", "redtail", "wels", "arapaima", "bamboo_shark", "reef_shark", "moray") or model in RAYS
        if big:
            view = "top" if model in RAYS else "side"
            s = fish_sprite(sp, model, varieties, dimorphic, i, box=TUB_BOX, view=view)
            s = _rim(s, 0.6)  # a second, darker rim: the grey and silver ones stand out from the grey tub
            out["tub_" + sp] = _tub_place(s)
        else:
            s = fish_sprite(sp, model, varieties, dimorphic, i, box=TALL.get(model, (WIN_W, 5)), vote=THIN.get(model, 0.45))
            out[sp] = _place(s)
    return out


def critter_icons():
    """{name: 16 x 16 RGBA}: ``<id>`` (the detail layer) and ``<id>_body`` (the body, grey for the gene-coloured species)."""
    out = {}
    for sid in CRITTER_SPRITES:
        body, detail = critter_layers(sid)
        for name, layer in ((sid + "_body", body), (sid, detail)):
            img = np.zeros((S, S, 4))
            img[WIN_Y:WIN_Y + WIN_H, WIN_X:WIN_X + WIN_W] = layer
            out[name] = img
    return out


def _over(dst, src):
    a = src[..., 3:4]
    dst[..., :3] = dst[..., :3] * (1 - a) + src[..., :3] * a
    dst[..., 3:4] = np.maximum(dst[..., 3:4], a)
    return dst


def _load(name):
    return np.asarray(Image.open(ITEM_TEX / f"{name}.png").convert("RGBA")).astype(np.float64) / 255


def composites(fish, crit):
    """Every icon as the game composes it (bucket, bag, tub; the gene-coloured ones in a typical colour): [(name, image)]."""
    bucket, glass, bag, tub, front = _load("betta_bucket"), _load("betta_bucket_glass"), opaque_bag(), _load("transport_tub"), tub_front()
    out = []
    for name, img in fish.items():
        if name.startswith("tub_"):
            out.append((name, _over(_over(tub.copy(), img), front)))
        else:
            out.append((name + "_bucket", _over(_over(bucket.copy(), img), glass)))
            out.append((name + "_bag", _over(bag.copy(), img)))
    for sid in CRITTER_SPRITES:
        body = crit[sid + "_body"].copy()
        if sid in TINTED_CRITTERS:
            body[..., :3] = np.clip(body[..., :3] * np.array(hex_f(PREVIEW_TINT[sid])), 0, 1)
        out.append((sid + "_bucket", _over(_over(_over(bucket.copy(), body), crit[sid]), glass)))
        out.append((sid + "_bag", _over(_over(bag.copy(), body), crit[sid])))
    return out


def preview(items, path, cols=12, scale=6):
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * S * scale, rows * (S * scale + 12)), (48, 48, 56, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(sheet)
    for k, (name, img) in enumerate(items):
        r, c = divmod(k, cols)
        tile = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA").resize((S * scale, S * scale), Image.NEAREST)
        sheet.alpha_composite(tile, (c * S * scale, r * (S * scale + 12)))
        d.text((c * S * scale + 2, r * (S * scale + 12) + S * scale), name[:16], fill=(230, 230, 230, 255))
    PREVIEW.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    return path


def opaque_bag():
    """The fish bag with its see-through water made opaque (laid over a pale water colour): the layers of a bag with an
    animal draw in a fixed order then (1.21.4+ sorts the quads of a translucent item; the animal went behind the water)."""
    bag = _load("fish_bag")
    under = np.array(hex_f("#cfe3ee"))
    out = bag.copy()
    a = bag[..., 3:4]
    m = (bag[..., 3] > 0)
    out[..., :3] = np.where(m[..., None], bag[..., :3] * a + under * (1 - a), 0)
    out[..., 3] = np.where(m, 1.0, 0.0)
    return out


def write_all() -> list:
    out = []
    out.append(save_png(opaque_bag(), ANIMAL_TEX / "bag.png"))
    fish = fish_icons()
    crit = critter_icons()
    for name, img in list(fish.items()) + list(crit.items()):
        out.append(save_png(img, ANIMAL_TEX / f"{name}.png"))
    out.append(save_png(tub_front(), ANIMAL_TEX / "tub_front.png"))
    out.append(preview(composites(fish, crit), PREVIEW / "animal_icons.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print("wrote", p)
