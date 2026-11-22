"""M63b: compares two runs of the real-world tour (gradlew runClient -Psmoketestworldtour, DevClientWorldTour).

    py tools/tour/compare.py <run dir A (e.g. the last release)> <run dir B (this version)> <out dir> [threshold %]

A run dir is run/<mc>/tour/<tag> (pictures in screenshots/<pack>/<view>.png). For every picture in both runs: the share of
pixels whose colour differs clearly (any channel by more than 32). Writes into <out dir>:
  diff.csv                      pack, picture, share in % (sorted, largest first)
  pairs-<pack>-<n>.png          A | B | difference (red) side by side, the pictures above the threshold (default 1 %) first
  sheet-<run>-<pack>-<n>.png    contact sheets of every picture of each run (48 per sheet, named), to look at all of them
Pictures of the miniature guard (*_nomini) are left out of the pairs and sheets.
"""
import csv
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

TILE = (320, 180)
SHEET_TILE = (240, 135)


def load(path):
    return np.asarray(Image.open(path).convert('RGB'), dtype=np.int16)


def pictures(run):
    base = os.path.join(run, 'screenshots')
    out = {}
    if not os.path.isdir(base):
        return out
    for pack in sorted(os.listdir(base)):
        d = os.path.join(base, pack)
        if not os.path.isdir(d):
            continue
        for f in sorted(os.listdir(d)):
            if f.endswith('.png') and not f.endswith('_nomini.png'):
                out[(pack, f[:-4])] = os.path.join(d, f)
    return out


def label(img, text):
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, img.width, 12], fill=(0, 0, 0))
    d.text((2, 0), text, fill=(255, 255, 255))
    return img


def sheets(run_name, pics, out):
    by_pack = {}
    for (pack, name), path in pics.items():
        by_pack.setdefault(pack, []).append((name, path))
    for pack, items in by_pack.items():
        cols, rows = 6, 8
        per = cols * rows
        for n in range(0, len(items), per):
            sheet = Image.new('RGB', (cols * SHEET_TILE[0], rows * SHEET_TILE[1]), (40, 40, 40))
            for i, (name, path) in enumerate(items[n:n + per]):
                t = Image.open(path).convert('RGB').resize(SHEET_TILE)
                sheet.paste(label(t, name), ((i % cols) * SHEET_TILE[0], (i // cols) * SHEET_TILE[1]))
            sheet.save(os.path.join(out, 'sheet-%s-%s-%02d.png' % (run_name, pack, n // per)))


def main():
    a_dir, b_dir, out = sys.argv[1], sys.argv[2], sys.argv[3]
    threshold = float(sys.argv[4]) if len(sys.argv) > 4 else 1.0
    os.makedirs(out, exist_ok=True)
    a, b = pictures(a_dir), pictures(b_dir)
    rows = []
    for key in sorted(set(a) & set(b)):
        ia, ib = load(a[key]), load(b[key])
        if ia.shape != ib.shape:
            rows.append((key[0], key[1], 100.0))
            continue
        changed = np.abs(ia - ib).max(axis=2) > 32
        rows.append((key[0], key[1], float(changed.mean() * 100)))
    rows.sort(key=lambda r: -r[2])
    with open(os.path.join(out, 'diff.csv'), 'w', newline='', encoding='utf-8') as f:
        w = csv.writer(f)
        w.writerow(['pack', 'picture', 'differs %'])
        for r in rows:
            w.writerow([r[0], r[1], '%.2f' % r[2]])
    only_a = sorted(set(a) - set(b))
    only_b = sorted(set(b) - set(a))
    flagged = [r for r in rows if r[2] > threshold]
    per = 8
    for n in range(0, len(flagged), per):
        sheet = Image.new('RGB', (3 * TILE[0], per * TILE[1]), (40, 40, 40))
        for i, (pack, name, share) in enumerate(flagged[n:n + per]):
            ia, ib = Image.open(a[(pack, name)]).convert('RGB'), Image.open(b[(pack, name)]).convert('RGB')
            if ia.size == ib.size:
                d = np.abs(np.asarray(ia, dtype=np.int16) - np.asarray(ib, dtype=np.int16)).max(axis=2) > 32
                diff = np.asarray(ib.convert('L').convert('RGB'), dtype=np.uint8) // 3
                diff[d] = (255, 40, 40)
                di = Image.fromarray(diff)
            else:
                di = Image.new('RGB', ib.size, (255, 0, 0))
            y = i * TILE[1]
            sheet.paste(label(ia.resize(TILE), 'A ' + pack + ' ' + name), (0, y))
            sheet.paste(label(ib.resize(TILE), 'B %.1f%%' % share), (TILE[0], y))
            sheet.paste(label(di.resize(TILE), 'diff'), (2 * TILE[0], y))
        sheet.save(os.path.join(out, 'pairs-%02d.png' % (n // per)))
    sheets('A', a, out)
    sheets('B', b, out)
    print('%d pictures in both, %d above %.1f %%, %d only in A, %d only in B; %s' % (len(rows), len(flagged), threshold, len(only_a), len(only_b), out))
    for r in flagged[:40]:
        print('  %-10s %-40s %6.2f %%' % r)


if __name__ == '__main__':
    main()
