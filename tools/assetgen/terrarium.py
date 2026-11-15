"""M31 assets: the terrarium devices.

* heat mat: a black heating foil with the copper heating trace in a meander (``heat_mat``) and the same glowing
  orange while it heats (``heat_mat_on``);
* basking lamp: a dark enamelled reflector shade (``basking_lamp_shade``), the bulb seen from below, warm white when lit
  (``basking_lamp_bulb``) and grey when off (``basking_lamp_bulb_off``);
* mister: a translucent-looking blue water bottle (``mister_tank``), the white pump head (``mister_cap``) and its
  nozzle (``mister_nozzle``);
* thermo-hygrometer: a white case (``thermo_hygrometer_side``) and the grey-green LCD face with its frame
  (``thermo_hygrometer_front``); the digits are drawn live by its block entity renderer.
* M31 part c: the dart frog clutch (``frog_eggs``): pale jelly with dark embryos.
* M31 part b: turtle eggs (``turtle_eggs``): a chalky cream shell with faint grey-brown speckles and a few grains of the
  nest sand stuck to it.

Deterministic pixel grids and procedural faces, no vanilla pixels.

Run from the repo root:  py tools/assetgen/terrarium.py
"""
from __future__ import annotations

import numpy as np

from common import BLOCK_TEX, save_png
from plumbing import _img, _rgb, _rng, steel


def heat_mat(on: bool):
    """A meander of copper trace on black foil with a soft frame; lit: the trace glows orange and the foil warms red."""
    r = _rng(3101)

    def px(x, y):
        foil = _rgb("#2a1a1a" if on else "#1b1d20") * (0.92 + 0.08 * r.random())
        if x in (0, 15) or y in (0, 15):
            return foil * 0.7
        # the meander: horizontal runs every 3 rows, joined at alternating ends
        row = (y - 2) % 3 == 0 and 2 <= y <= 13 and 2 <= x <= 13
        k = (y - 2) // 3
        join = (x == 13 and k % 2 == 0 or x == 2 and k % 2 == 1) and 2 <= y <= 13 and (y - 2) % 3 != 0 and (y - 2) // 3 < 4
        if row or join:
            return _rgb("#ff8a2a") * (0.9 + 0.1 * r.random()) if on else _rgb("#b06a3a") * (0.85 + 0.15 * r.random())
        if on and 1 <= x <= 14 and 1 <= y <= 14:
            return foil * 1.25
        return foil
    return _img(px)


def lamp_shade():
    """Dark green enamel with a highlight band (the dome of the reflector)."""
    r = _rng(3102)

    def px(x, y):
        v = 0.85 + 0.1 * r.random()
        if y in (4, 5):
            v *= 1.35
        if y >= 14:
            v *= 0.7
        return _rgb("#2c4232") * v
    return _img(px)


def lamp_bulb(lit: bool):
    """The round bulb in the silver reflector seen from below: a warm white glow when lit."""
    r = _rng(3103)

    def px(x, y):
        d = np.hypot(x - 7.5, y - 7.5)
        if d <= 3.6:
            if lit:
                return _rgb("#fff4d0") * (1.0 - 0.04 * d) + 0 * r.random()
            return _rgb("#9a9a92") * (0.95 - 0.03 * d)
        if d <= 7.3:
            v = 0.78 + 0.18 * np.cos(d * 1.4) + 0.05 * r.random()
            return _rgb("#e8d8a8" if lit else "#c0c4c8") * v
        return _rgb("#2c4232") * 0.8
    return _img(px)


def mister_tank():
    """Light blue water behind a clear bottle: a lighter edge, a water line near the top, little bubbles."""
    r = _rng(3104)

    def px(x, y):
        if y <= 2:
            return _rgb("#d8e6ec") * (0.95 + 0.05 * r.random())
        base = _rgb("#5aa8d8") * (0.9 + 0.08 * r.random())
        if x in (0, 15):
            base = _rgb("#a8d4ee")
        if x in (2, 3) and 4 <= y <= 13:
            base = _rgb("#cfe9f8")
        if y == 3:
            base = _rgb("#8cc8ea")
        if (x * 7 + y * 3) % 23 == 0:
            base = _rgb("#e0f2ff")
        return base
    return _img(px)


def mister_cap():
    img = steel(3105, "#eceff1")
    img[7:9, 6:10, :3] = (40, 44, 50)  # the switch
    img[7, 11, :3] = (90, 200, 120)  # the power lamp
    return img


def mister_nozzle():
    return steel(3106, "#9aa3ab")


def thermo_side():
    return steel(3107, "#f0f2f2")


def thermo_front():
    """White case, a grey-green LCD (the BER writes on it), two small buttons below."""
    r = _rng(3108)

    def px(x, y):
        if x in (0, 15) or y in (0, 15):
            return _rgb("#c8ccd0")
        if 2 <= x <= 13 and 2 <= y <= 11:
            if x in (2, 13) or y in (2, 11):
                return _rgb("#3a4040")
            return _rgb("#b4c4a8") * (0.96 + 0.04 * r.random())
        if y == 13 and x in (5, 6, 9, 10):
            return _rgb("#7a8288")
        return _rgb("#eef0f0") * (0.96 + 0.04 * r.random())
    return _img(px)


def turtle_eggs():
    """Chalky cream egg shell: soft light-dark mottling, a few speckles, sand grains on the lower rows."""
    r = _rng(3109)

    def px(x, y):
        v = 0.94 + 0.06 * r.random()
        col = _rgb("#f0e8d4") * v
        if r.random() < 0.05:
            col = _rgb("#bfb39a")
        if y >= 11 and r.random() < 0.18:
            col = _rgb("#cdb27a") * (0.9 + 0.1 * r.random())
        if y <= 3:
            col = col * 1.03
        return col
    return _img(px)


def frog_eggs():
    """M31 part c: a dart frog clutch: pale, clear-looking jelly (opaque for the miniature) with round dark embryos."""
    r = _rng(3131)

    def px(x, y):
        col = _rgb("#d6e4dc") * (0.94 + 0.06 * r.random())
        # embryos on a grid of 3 px, a dark core with a lighter rim
        cx, cy = x % 3, y % 3
        if cx == 1 and cy == 1:
            col = _rgb("#2a2a22")
        elif (cx == 1 or cy == 1) and r.random() < 0.3:
            col = _rgb("#9aa89a")
        return col
    return _img(px)


def write_all():
    out = []
    tex = {
        "heat_mat": heat_mat(False), "heat_mat_on": heat_mat(True),
        "basking_lamp_shade": lamp_shade(), "basking_lamp_bulb": lamp_bulb(True), "basking_lamp_bulb_off": lamp_bulb(False),
        "mister_tank": mister_tank(), "mister_cap": mister_cap(), "mister_nozzle": mister_nozzle(),
        "thermo_hygrometer_side": thermo_side(), "thermo_hygrometer_front": thermo_front(),
        "turtle_eggs": turtle_eggs(),
        "frog_eggs": frog_eggs(),
    }
    for name, img in tex.items():
        out.append(save_png(img, BLOCK_TEX / f"{name}.png"))
    return out


if __name__ == "__main__":
    for p in write_all():
        print(p)
