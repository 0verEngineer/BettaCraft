"""M25 part c assets: the dosing pump of a reef tank (a white case with four peristaltic pump heads and a display) and
its canisters (empty, calcium, alkalinity, magnesium, trace elements).

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/reef_dosing.py
"""
from __future__ import annotations

from common import BLOCK_TEX, ITEM_TEX, save_png
from icons import grid
from plumbing import _img, _rgb, _rng, steel

K = "#1c2228"
# the colour of each channel (label of the canister, the tube of the pump head)
CHANNEL = {"calcium": "#e8e8e0", "alkalinity": "#4a8ad8", "magnesium": "#58b868", "trace": "#d84a9a"}


def pump_front():
    """Four round pump heads with their channel colour in a white case, a small blue display above."""
    r = _rng(2560)
    cols = list(CHANNEL.values())

    def px(x, y):
        base = _rgb("#e8ecef") * (0.95 + 0.05 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.72
        if 2 <= y <= 4 and 3 <= x <= 12:  # the display
            return _rgb("#7ad0ff") if y == 3 and x % 2 == 0 else _rgb("#1e3a5a")
        for i, cx in enumerate((3, 6, 9, 12)):
            if cx - 1 <= x <= cx + 1 and 7 <= y <= 11:
                if y in (7, 11) or x in (cx - 1, cx + 1):
                    return _rgb("#2a2e33")
                return _rgb(cols[i]) * (0.9 + 0.1 * r.random())
            if x == cx and 12 <= y <= 14:
                return _rgb(cols[i]) * 0.8
        return base
    return _img(px)


def pump_side():
    img = steel(2561, "#dfe3e7")
    for x in range(3, 13):
        img[8, x, :3] = (60, 120, 190)
    return img


def pump_top():
    return steel(2562, "#e4e8eb")


def canister_rows(label: bool) -> list[str]:
    rows = [
        "................",
        "......kkkk......",
        "......kccK......",
        ".....kkkkkk.....",
        "....kwwwwwwk....",
        "...kwWwwwwwwk...",
        "...kwWwwwwwwk...",
        "...kwWLLLLwwk...",
        "...kwWLLLLwwk...",
        "...kwWLLLLwwk...",
        "...kwWwwwwwwk...",
        "...kwWfffffwk...",
        "...kwffffffwk...",
        "...kffffffffk...",
        "....kkkkkkkk....",
        "................",
    ]
    if not label:
        rows = [r.replace("L", "w").replace("f", "w") for r in rows]
    return rows


def canister(kind: str | None):
    pal = {"k": K, "c": "#3a3e44", "K": "#5a5e64", "w": "#dfe8ee", "W": "#ffffff"}
    if kind is None:
        return grid(canister_rows(False), pal)
    col = CHANNEL[kind]
    pal.update({"L": col, "f": col + "c0"})
    return grid(canister_rows(True), pal)


def write_all():
    out = []
    for name, img in {"dosing_pump_front": pump_front(), "dosing_pump_side": pump_side(), "dosing_pump_top": pump_top()}.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    out.append(save_png(canister(None), ITEM_TEX / "dosing_canister.png"))
    for kind in CHANNEL:
        out.append(save_png(canister(kind), ITEM_TEX / f"{kind}_canister.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
