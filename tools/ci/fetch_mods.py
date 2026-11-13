#!/usr/bin/env python3
"""Downloads the pinned optional dev mods of a group (e.g. tools/ci/power-mods.json) for CI.

    python3 tools/ci/fetch_mods.py tools/ci/power-mods.json

Every jar comes from its official Modrinth CDN url and must match the pinned SHA-512, otherwise the script fails and
deletes the file. The jars land in the group's folder (e.g. run/power-mods), from where build.gradle copies them into
the run with -Ppower. Jars that are already there with the right hash are kept. Nothing of this is committed.
Only the Python standard library is used.
"""
import hashlib
import json
import os
import sys
import urllib.request


def sha512(path):
    h = hashlib.sha512()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def main(spec_path):
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    with open(spec_path, encoding='utf-8') as f:
        spec = json.load(f)
    target = os.path.join(root, spec['target'])
    os.makedirs(target, exist_ok=True)
    for mod in spec['mods']:
        name = os.path.basename(mod['file'])
        if not mod['url'].startswith('https://cdn.modrinth.com/'):
            sys.exit(f'{name}: only Modrinth CDN urls are allowed, got {mod["url"]}')
        dst = os.path.join(target, name)
        if os.path.isfile(dst) and sha512(dst) == mod['sha512']:
            print(f'ok      {name} (cached)')
            continue
        tmp = dst + '.part'
        req = urllib.request.Request(mod['url'], headers={'User-Agent': 'bettacraft-ci (github.com/0verEngineer/BettaCraft)'})
        with urllib.request.urlopen(req, timeout=120) as r, open(tmp, 'wb') as out:
            while True:
                chunk = r.read(1 << 20)
                if not chunk:
                    break
                out.write(chunk)
        got = sha512(tmp)
        if got != mod['sha512']:
            os.remove(tmp)
            sys.exit(f'{name}: SHA-512 mismatch (got {got})')
        os.replace(tmp, dst)
        print(f'fetched {name} ({mod["slug"]} {mod["version_number"]}, {mod["license"]})')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
