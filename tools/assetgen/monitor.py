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
    for name, img in {"monitor_screen": screen(), "monitor_side": side(), "monitor_back": back(), "monitor_bezel": bezel()}.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
