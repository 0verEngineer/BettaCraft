"""M27 assets: the monitor block (dark glass, the case, the back with vents, the frame).

Deterministic procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/monitor.py
"""
from __future__ import annotations

from common import BLOCK_TEX, save_png
from plumbing import _img, _rgb, _rng

S = 16


def screen():
    """Dark blue-black glass with faint scan lines and a soft sheen in one corner (mostly covered by the screen image)."""
    r = _rng(2701)

    def px(x, y):
        base = _rgb("#0b151a") * (0.92 + 0.08 * r.random())
        if y % 2 == 0:
            base = base * 0.9
        sheen = max(0.0, 1.0 - (x + y) / 9.0) * 18
        return base + sheen
    return _img(px)


def screen_item():
    """M29.1: the item icon's screen: what the monitor shows when it runs (a temperature curve, a header line and
    three value bars), so the item reads as a monitor and not as a black box."""
    import math
    r = _rng(2705)
    curve = {x: round(9.5 - 2.6 * math.sin(x * 0.55) - 0.8 * math.sin(x * 1.7)) for x in range(16)}

    def px(x, y):
        base = _rgb("#0b151a") * (0.92 + 0.08 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.7
        if y == 2 and 2 <= x <= 9:
            return _rgb("#5fd0c8")
        if y == 2 and 11 <= x <= 13:
            return _rgb("#e0c060")
        if 1 <= x <= 14 and curve[x] == y:
            return _rgb("#68e070")
        if 1 <= x <= 14 and abs(curve[x] - y) == 1 and (curve.get(x - 1) == y or curve.get(x + 1) == y):
            return _rgb("#3c9a48")
        if y == 13 and 2 <= x <= 13:
            bar = 2 + (x - 2) // 4
            fill = {2: 7, 3: 10, 4: 13}[bar]
            if (x - 2) % 4 == 3:
                return base
            return _rgb("#4a90e0") if x <= fill else _rgb("#1c3040")
        return base
    return _img(px)


def side():
    """Charcoal case with a seam at the front and two screws."""
    r = _rng(2702)

    def px(x, y):
        base = _rgb("#33383d") * (0.94 + 0.06 * r.random())
        if x == 1:
            return base * 0.7
        if y in (0, 15):
            return base * 0.82
        if x == 12 and y in (3, 12):
            return _rgb("#7d858d")
        return base
    return _img(px)


def back():
    """The back: case grey with a grid of vent slots and a small label."""
    r = _rng(2703)

    def px(x, y):
        base = _rgb("#2c3035") * (0.94 + 0.06 * r.random())
        if x in (0, 15) or y in (0, 15):
            return base * 0.78
        if 3 <= x <= 12 and 3 <= y <= 9 and y % 2 == 1:
            return _rgb("#121518")
        if 5 <= x <= 10 and 12 <= y <= 13:
            return _rgb("#8a9298") * (0.9 + 0.1 * r.random())
        return base
    return _img(px)


def bezel():
    """The frame: a slightly lighter charcoal with a highlight line."""
    r = _rng(2704)

    def px(x, y):
        base = _rgb("#262a2e") * (0.95 + 0.05 * r.random())
        if y == 0 or x == 0:
            return base * 1.35
        if y == 15 or x == 15:
            return base * 0.75
        return base
    return _img(px)


def write_all():
    out = []
    for name, img in {"monitor_screen": screen(), "monitor_screen_item": screen_item(), "monitor_side": side(), "monitor_back": back(),
                      "monitor_bezel": bezel()}.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
