"""M52b: spawn egg textures (Minecraft 1.21.5+ has no tinted spawn egg template any more; every egg has its own texture).

One 16x16 icon per `*_spawn_egg` item: an egg of our own shape, shaded in the egg's base colour, with spots in its second
colour (the two colours the 1.21.1 eggs were tinted with). The colours are read from the Java sources (ModItems,
TankmateSpecies, ModItems#eggColours for the amphibians and reptiles), the list of eggs from the generated item models.
Deterministic, procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/spawn_eggs.py
"""
from __future__ import annotations

import re
import zlib

import numpy as np

from common import ITEM_TEX, ROOT, rel, save_png

S = 16
JAVA = ROOT / "common" / "src" / "main" / "java" / "org" / "overengineer" / "bettacraft"
MODELS = ROOT / "common" / "src" / "generated" / "resources" / "assets" / "bettacraft" / "models" / "item"
DEFAULT_TANKMATE = (0x808080, 0x404040)


def egg_ids() -> list[str]:
    return sorted(p.stem for p in MODELS.glob("*_spawn_egg.json"))


def colours() -> dict[str, tuple[int, int]]:
    out: dict[str, tuple[int, int]] = {}
    items = (JAVA / "registry" / "ModItems.java").read_text(encoding="utf-8")
    for m in re.finditer(r'register\("(\w+_spawn_egg)",\s*\(\)\s*->[^;]*?spawnEgg\([\w.]+,\s*(0x[0-9A-Fa-f]{6}),\s*(0x[0-9A-Fa-f]{6})', items, re.S):
        out[m.group(1)] = (int(m.group(2), 16), int(m.group(3), 16))
    sw = items[items.index("private static int[] eggColours"):]
    sw = sw[:sw.index("\n    }\n")]
    herp = {m.group(1): (int(m.group(2), 16), int(m.group(3), 16))
            for m in re.finditer(r'case "(\w+)" -> new int\[\]\{(0x[0-9A-Fa-f]{6}), (0x[0-9A-Fa-f]{6})\}', sw)}
    d = re.search(r'default -> new int\[\]\{(0x[0-9A-Fa-f]{6}), (0x[0-9A-Fa-f]{6})\}', sw)
    herp_default = (int(d.group(1), 16), int(d.group(2), 16))
    herp_ids = set(re.findall(r'HerpSpecies [A-Z_]+ = builder\("(\w+)"', (JAVA / "terrarium" / "HerpSpecies.java").read_text(encoding="utf-8")))
    for h in herp_ids:
        out.setdefault(h + "_spawn_egg", herp.get(h, herp_default))
    tm = (JAVA / "tankmates" / "TankmateSpecies.java").read_text(encoding="utf-8")
    starts = [(m.start(), m.group(1)) for m in re.finditer(r'\bd\("(\w+)"', tm)]
    for i, (pos, sid) in enumerate(starts):
        end = starts[i + 1][0] if i + 1 < len(starts) else len(tm)
        e = re.search(r'\.egg\((0x[0-9A-Fa-f]{6}),\s*(0x[0-9A-Fa-f]{6})\)', tm[pos:end])
        out.setdefault(sid + "_spawn_egg", (int(e.group(1), 16), int(e.group(2), 16)) if e else DEFAULT_TANKMATE)
    return out


def _rgb(c: int) -> np.ndarray:
    return np.array([(c >> 16) & 255, (c >> 8) & 255, c & 255], np.float64)


def egg(base: int, spots: int, seed: int) -> np.ndarray:
    """RGBA 16x16: an egg (wider at the bottom) with a dark rim, light from the upper left and a few spots."""
    img = np.zeros((S, S, 4), np.float64)
    b, s = _rgb(base), _rgb(spots)
    rng = np.random.default_rng(seed)
    cx, cy = 7.5, 8.3
    inside = np.zeros((S, S), bool)
    for y in range(S):
        for x in range(S):
            dy = (y + 0.5 - cy) / 6.9
            rx = 5.2 * (1.0 + 0.16 * dy)  # the egg widens downwards
            dx = (x + 0.5 - cx) / rx
            inside[y, x] = dx * dx + dy * dy <= 1.0
    # spots: small blobs of the second colour
    spot = np.zeros((S, S), bool)
    for _ in range(6):
        px, py = rng.uniform(4.0, 11.0), rng.uniform(3.5, 13.0)
        r = rng.uniform(0.9, 1.6)
        for y in range(S):
            for x in range(S):
                if (x + 0.5 - px) ** 2 + (y + 0.5 - py) ** 2 <= r * r:
                    spot[y, x] = True
    for y in range(S):
        for x in range(S):
            if not inside[y, x]:
                continue
            edge = any(not (0 <= x + ox < S and 0 <= y + oy < S and inside[y + oy, x + ox]) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            c = s.copy() if spot[y, x] else b.copy()
            light = 1.0 + 0.22 * ((cx - x) / 6.0) + 0.18 * ((cy - y) / 7.0)
            c = c * light
            if edge:
                c = c * 0.55
            elif (x - 4.8) ** 2 + (y - 4.6) ** 2 < 2.0:
                c = c * 0.5 + 255 * 0.5  # the shine
            img[y, x, :3] = np.clip(c, 0, 255)
            img[y, x, 3] = 255
    return img.astype(np.uint8)


def main() -> None:
    table = colours()
    ids = egg_ids()
    missing = [i for i in ids if i not in table]
    if missing:
        raise SystemExit(f"no colours for {missing}")
    for i in ids:
        base, spots = table[i]
        p = save_png(egg(base, spots, zlib.crc32(i.encode())), ITEM_TEX / f"{i}.png")
    print(f"{len(ids)} spawn egg textures, e.g. {rel(p)}")


if __name__ == "__main__":
    main()
