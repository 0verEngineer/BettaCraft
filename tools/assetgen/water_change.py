"""M68 part B assets: the four port faces of the water change controller and the drain.

The controller has a steel stub on each port; the end of the stub shows the port: a pipe socket with the ring colour of its line
(green: fresh water, brown: waste water, the colours of the pipe rings) and a white arrow pointing into the socket (in) or out of
it (out). The stub's sides are plain brushed steel. The drain: a dark grate on top, brushed steel sides with a brown band (the
waste water ring) and a plain steel bottom. Deterministic and procedural, no vanilla pixels.

Run from the repo root:  py tools/assetgen/water_change.py
"""
from __future__ import annotations

import math

from common import BLOCK_TEX, save_png
from filter_lines import FRESH, WASTE
from plumbing import _img, _rgb, steel


def port_face(colour, inward):
    """A port: the dark socket in a steel collar, the ring of its line's colour, an arrow above it pointing in (down) or out (up)."""
    ring = _rgb(colour)
    white = _rgb("#f2f4f6")

    def arrow(x, y):
        # a 5 px wide chevron in rows 1..3 above the socket, centred on columns 7..8
        cx = 7.5
        if inward:  # points down, into the socket
            return 1 <= y <= 3 and abs(x - cx) <= 3.5 - (y - 1) and abs(x - cx) >= 2.0 - (y - 1)
        return 1 <= y <= 3 and abs(x - cx) <= 1.5 + (y - 1) and abs(x - cx) >= (y - 1)

    def px(x, y):
        if arrow(x, y):
            return white
        d = math.hypot(x - 7.5, y - 9.0)
        if d < 2.4:
            return _rgb("#14181c")
        if d < 3.4:
            return _rgb("#c8d0d8")
        if d < 4.6:
            return ring
        if x in (0, 15) or y in (0, 15):
            return ring * 0.8
        return _rgb("#aab2bb") * (0.95 + 0.05 * math.sin(y * 0.9))
    return _img(px)


def drain_top():
    """The grate of the drain: dark slots between steel bars in a steel frame, a brown rim (the waste water ring)."""
    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb(WASTE) * 0.85
        if x in (1, 14) or y in (1, 14):
            return _rgb("#b4bcc5") * 0.9
        if y % 3 == 0:
            return _rgb("#c2cad2")
        return _rgb("#101418") if 3 <= x <= 12 else _rgb("#9aa3ac")
    return _img(px)


def drain_side():
    """Brushed steel with a brown band at the top (the waste water ring)."""
    img = steel(2470, "#b4bcc5")
    band = _rgb(WASTE)
    for x in range(16):
        for y in (2, 3):
            img[y, x, :3] = (band * (0.9 + 0.1 * (x % 4 == 0))).clip(0, 255).astype("uint8")
    return img


def write_all():
    out = []
    blocks = {
        "water_change_port_fresh_in": port_face(FRESH, True), "water_change_port_fresh_out": port_face(FRESH, False),
        "water_change_port_waste_in": port_face(WASTE, True), "water_change_port_waste_out": port_face(WASTE, False),
        "water_change_port_stub": steel(2471, "#a9b2bc"),
        "drain_top": drain_top(), "drain_side": drain_side(), "drain_bottom": steel(2472, "#a0a8b0"),
    }
    for name, img in blocks.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
