"""Shared paths and small image helpers for the Bettacraft asset pipeline."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ASSETGEN = Path(__file__).resolve().parent
ROOT = ASSETGEN.parents[1]
BUILD = ASSETGEN / "build"
GEN = BUILD / "generated"
PREVIEW = BUILD / "preview"

RES = ROOT / "src" / "main" / "resources"
TEX = RES / "assets" / "bettacraft" / "textures"
ENTITY_TEX = TEX / "entity" / "betta"
ITEM_TEX = TEX / "item"
BLOCK_TEX = TEX / "block"
LOGO = RES / "bettacraft.png"
GUI_TEX = TEX / "gui"
JAVA_SRC = ROOT / "src" / "main" / "java" / "org" / "overengineer" / "bettacraft"
REVIEW_DIR = ROOT / "builds"


def ensure_dirs() -> None:
    for d in (GEN, PREVIEW, ENTITY_TEX, ITEM_TEX, BLOCK_TEX, GUI_TEX, REVIEW_DIR):
        d.mkdir(parents=True, exist_ok=True)


def hex_rgb(h: str) -> tuple[int, int, int]:
    h = h.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def hex_f(h: str) -> np.ndarray:
    """Hex colour -> float RGB array in 0..1."""
    return np.array(hex_rgb(h), dtype=np.float64) / 255.0


def to_image(arr: np.ndarray) -> Image.Image:
    """float RGBA (0..1) or uint8 array -> PIL RGBA image."""
    if arr.dtype != np.uint8:
        arr = np.clip(np.rint(arr * 255.0), 0, 255).astype(np.uint8)
    if arr.ndim == 2:
        return Image.fromarray(arr, "L")
    if arr.shape[2] == 3:
        return Image.fromarray(arr, "RGB")
    return Image.fromarray(arr, "RGBA")


def save_png(img, path: Path) -> Path:
    if isinstance(img, np.ndarray):
        img = to_image(img)
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG", optimize=True)
    return path


def rel(path: Path) -> str:
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)


def over(dst: np.ndarray, rgb: np.ndarray, a: np.ndarray) -> np.ndarray:
    """Straight-alpha 'source over' of (rgb, a) onto float RGBA dst (in place)."""
    a = a[..., None]
    out_a = a + dst[..., 3:4] * (1 - a)
    safe = np.where(out_a > 0, out_a, 1)
    dst[..., :3] = (rgb * a + dst[..., :3] * dst[..., 3:4] * (1 - a)) / safe
    dst[..., 3:4] = out_a
    return dst
