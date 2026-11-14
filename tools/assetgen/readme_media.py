"""README / mod page media from the README smoke scene.

    gradlew runClient -Psmoketestreadme                        # stills + frame sequences (background window)
    gradlew runClient -Psmoketestreadme -Pshaders -Preadmeroom # optional: the room with Complementary (_shaders)
    py tools/assetgen/readme_media.py [run/screenshots] [docs/images]

Stills become JPEGs (scenes) or a PNG (menus), the frame sequences small looping GIFs (one global palette, unchanged
pixels transparent, so the LZW runs stay long) and animated WebPs (smaller, better colours). Names start with
``bettacraft-`` so the files can be reused for the CurseForge page as they are (docs/description.md links the GIFs:
the CurseForge description takes GIF, PNG and JPG).
"""
import os
import sys
import glob

import numpy as np
from PIL import Image

SRC = sys.argv[1] if len(sys.argv) > 1 else 'run/screenshots'
DST = sys.argv[2] if len(sys.argv) > 2 else 'docs/images'

# still name -> (output name, crop box as fractions (l, t, r, b) of the 1600x900 shot or None, width)
STILLS = {
    'readme_room': ('bettacraft-aquarium-room', (0.02, 0.2, 0.98, 0.8), 1280),
    'readme_room_shaders': ('bettacraft-aquarium-room-shaders', (0.02, 0.2, 0.98, 0.8), 1280),
    'readme_planted': ('bettacraft-planted-tank', None, 1280),
    'readme_reef': ('bettacraft-reef-tank', None, 1280),
    'readme_varieties': ('bettacraft-betta-varieties', (0.04, 0.3, 0.96, 0.7), 1280),
    'readme_inside': ('bettacraft-tank-interior', None, 1280),
    'readme_nano': ('bettacraft-betta-nano-tank', None, 1280),
}
# frame sequence -> (output name, crop fractions or None, width, frames per second, first frame, frame count)
ANIMS = {
    'planted': ('bettacraft-planted-tank-swimming', None, 600, 20, 0, 80),
    'reef': ('bettacraft-reef-tank-swimming', None, 640, 20, 0, 100),
    'inside': ('bettacraft-tank-interior-swaying', None, 440, 20, 0, 50),
}


def crop(img, box):
    if box is None:
        return img
    w, h = img.size
    return img.crop((int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h)))


def scaled(img, width):
    if img.width == width:
        return img
    return img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)


def stills():
    for key, (name, box, width) in STILLS.items():
        path = os.path.join(SRC, key + '.png')
        if not os.path.exists(path):
            continue
        img = scaled(crop(Image.open(path).convert('RGB'), box), width)
        out = os.path.join(DST, name + '.jpg')
        img.save(out, quality=88, optimize=True, progressive=True, subsampling='4:4:4')
        print(f'{out}: {os.path.getsize(out) // 1024} KB')


# the tank window without the player inventory, and the top of the handbook page (fractions of the 1600x900 shot)
MENU_BOX = (0.268, 0.115, 0.733, 0.588)
GUIDE_BOX = (0.0, 0.0, 1.0, 0.425)


def menus():
    """The betta list with its genes and the water tab side by side, the handbook page below."""
    names = ['readme_menu_bettas', 'readme_menu_water', 'readme_guide']
    if not all(os.path.exists(os.path.join(SRC, n + '.png')) for n in names):
        return
    a, b, g = (Image.open(os.path.join(SRC, n + '.png')).convert('RGB') for n in names)
    a, b, g = crop(a, MENU_BOX), crop(b, MENU_BOX), crop(g, GUIDE_BOX)
    gap = 12
    width = a.width + b.width + gap
    g = g.resize((width, round(g.height * width / g.width)), Image.LANCZOS)
    sheet = Image.new('RGB', (width + 2 * gap, a.height + g.height + 3 * gap), (24, 28, 34))
    sheet.paste(a, (gap, gap))
    sheet.paste(b, (2 * gap + a.width, gap))
    sheet.paste(g, (gap, 2 * gap + a.height))
    sheet = scaled(sheet, 1280)
    out = os.path.join(DST, 'bettacraft-menus.png')
    sheet.quantize(256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(out, optimize=True)
    print(f'{out}: {os.path.getsize(out) // 1024} KB')


def frames_of(seq, box, width, first, count):
    files = sorted(glob.glob(os.path.join(SRC, f'readme_anim_{seq}_*.png')))[first:first + count]
    return [scaled(crop(Image.open(f).convert('RGB'), box), width) for f in files]


def gif(frames, out, fps):
    # one palette for all frames from a sample of them; index 255 = transparent (pixel as in the frame before)
    sample = Image.new('RGB', (frames[0].width, frames[0].height * 4))
    for i, k in enumerate(np.linspace(0, len(frames) - 1, 4).astype(int)):
        sample.paste(frames[k], (0, i * frames[0].height))
    pal_img = sample.quantize(255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    idx = [np.asarray(f.quantize(palette=pal_img, dither=Image.Dither.NONE)) for f in frames]
    palette = pal_img.getpalette()[:255 * 3] + [0, 0, 0]
    out_frames = []
    prev = None
    for a in idx:
        b = a.copy()
        if prev is not None:
            b[a == prev] = 255
        prev = a
        im = Image.fromarray(b.astype(np.uint8), 'P')
        im.putpalette(palette)
        out_frames.append(im)
    out_frames[0].save(out, save_all=True, append_images=out_frames[1:], duration=round(1000 / fps), loop=0,
                       transparency=255, disposal=1, optimize=False)
    print(f'{out}: {len(frames)} frames, {os.path.getsize(out) // 1024} KB')


def webp(frames, out, fps):
    frames[0].save(out, save_all=True, append_images=frames[1:], duration=round(1000 / fps), loop=0, quality=80, method=6)
    print(f'{out}: {os.path.getsize(out) // 1024} KB')


def anims():
    for seq, (name, box, width, fps, first, count) in ANIMS.items():
        frames = frames_of(seq, box, width, first, count)
        if len(frames) < 10:
            continue
        loop = frames
        gif(loop, os.path.join(DST, name + '.gif'), fps)
        webp(loop, os.path.join(DST, name + '.webp'), fps)


if __name__ == '__main__':
    os.makedirs(DST, exist_ok=True)
    stills()
    menus()
    anims()
