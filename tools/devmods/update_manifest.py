#!/usr/bin/env python3
"""Writes dev-mods.json, the manifest of the optional dev mod groups of this branch (M59).

The jars of the groups (-Ppower, -Paquatic, integrationTests, zooTests, the Fabric groups ...) are never committed:
gradle/dev-mods.gradle downloads them from their official source (Modrinth Maven, CurseMaven, FTB Maven) and checks
their hashes. This tool keeps the manifest in step with the group folders:

  1. put the new jar into its group folder (e.g. run/power-mods/, run-fabric/integration-mods/; ports:
     run/<mc>/<group>, run/<mc>/fabric/<group>)
  2. py tools/devmods/update_manifest.py            (add --dry-run to see the changes only)
  3. run the group once (it is downloaded again from the manifest) and commit dev-mods.json

Every jar is looked up by its SHA-1: Modrinth (POST /v2/version_files), FTB Maven (the ftb-* file names), CurseForge
(CFWidget file lists of the projects in CURSE_PROJECTS, downloaded from CurseMaven and compared byte for byte). A jar
that is found nowhere stops the tool (an unofficial file cannot go into the manifest). Entries whose file and SHA-1 are
unchanged are kept without a lookup. Groups of the manifest without a local folder are kept (a fresh clone has no
folders); --prune drops them. Shader packs (the "shaderpacks" list) and "gaps" are kept as they are (edited by hand).

Usage: py tools/devmods/update_manifest.py [--neo DIR] [--fabric DIR] [--seed OLD.json ...] [--prune] [--dry-run]
"""
import argparse
import hashlib
import json
import os
import re
import sys
import urllib.parse
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
UA = {"User-Agent": "bettacraft-dev-mods-manifest (github.com/0verEngineer/BettaCraft)"}
SKIP_FOLDERS = {"mods", "nested-mods", "local-mods", "patchouli-libraries"}

# file name prefix -> CurseForge project id, for the mods that are only on CurseForge (longest prefix wins)
CURSE_PROJECTS = {
    "iceandfire": 1040076, "IceAndFireCE": 1040076, "shiny": 552172, "uranus": 1010827, "jupiter": 1072905,
    "twilightforest": 227639, "commoncapabilities": 247007, "cyclopscore": 232758, "integrateddynamics": 236307,
    "integratedtunnels": 251389, "laserio": 626839, "kubejs": 238086, "FluxNetworks": 248020, "refinedstorage": 243076,
    "aether_ii": 223796, "aether": 255308, "ars_elemental": 561470, "evilcraft": 74610, "mahoutsukai": 342543,
    "productivebees": 377897, "thaumaturge": 1628024, "Psychadelic-Chemistry": 623095, "The_Undergarden": 379849,
    "mcjtylib": 233105, "rftoolsbase": 326041,
}
FTB = re.compile(r"(ftb-(?:library|quests|teams|chunks)-(?:neoforge|fabric))-(.+)\.jar$")


def get(url, data=None, headers=None):
    req = urllib.request.Request(url, data=data, headers={**UA, **(headers or {})})
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.read()


def digests(path):
    h1, h256 = hashlib.sha1(), hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h1.update(chunk)
            h256.update(chunk)
    return h1.hexdigest(), h256.hexdigest()


def prop(name):
    for line in open(os.path.join(ROOT, "gradle.properties"), encoding="utf-8"):
        if line.startswith(name + "="):
            return line.split("=", 1)[1].strip()
    raise SystemExit(f"{name} not in gradle.properties")


def scan(folder):
    """{group: [jar paths]} of the *-mods folders below folder."""
    out = {}
    if not os.path.isdir(folder):
        return out
    for d in sorted(os.listdir(folder)):
        p = os.path.join(folder, d)
        if d.endswith("-mods") and d not in SKIP_FOLDERS and os.path.isdir(p):
            out[d] = sorted(os.path.join(p, f) for f in os.listdir(p) if f.endswith(".jar"))
    return out


def modrinth_lookup(sha1s):
    found = {}
    sha1s = sorted(set(sha1s))
    for i in range(0, len(sha1s), 200):
        body = json.dumps({"hashes": sha1s[i:i + 200], "algorithm": "sha1"}).encode()
        found.update(json.loads(get("https://api.modrinth.com/v2/version_files", body, {"Content-Type": "application/json"})))
    pids = sorted({v["project_id"] for v in found.values()})
    slugs = {}
    for i in range(0, len(pids), 100):
        q = urllib.parse.quote(json.dumps(pids[i:i + 100]))
        for pr in json.loads(get(f"https://api.modrinth.com/v2/projects?ids={q}")):
            slugs[pr["id"]] = pr["slug"]
    return {h: f"maven.modrinth:{slugs[v['project_id']]}:{v['id']}" for h, v in found.items()}


_cf = {}


def curse_lookup(name, size, sha1):
    pid = next((p for k, p in sorted(CURSE_PROJECTS.items(), key=lambda kv: -len(kv[0])) if name.startswith(k)), None)
    if pid is None:
        return None
    if pid not in _cf:
        _cf[pid] = json.loads(get(f"https://api.cfwidget.com/{pid}")).get("files", [])
    cands = [x for x in _cf[pid] if x.get("name") == name] or [x for x in _cf[pid] if x.get("filesize") == size]
    for c in cands:
        url = f"https://cursemaven.com/curse/maven/cf-{pid}/{c['id']}/cf-{pid}-{c['id']}.jar"
        try:
            if hashlib.sha1(get(url)).hexdigest() == sha1:
                return f"curse.maven:cf-{pid}:{c['id']}"
        except OSError:
            pass
    return None


def ftb_lookup(name, sha1):
    m = FTB.match(name)
    if not m:
        return None
    url = f"https://maven.ftb.dev/releases/dev/ftb/mods/{m.group(1)}/{m.group(2)}/{m.group(1)}-{m.group(2)}.jar"
    try:
        return f"dev.ftb.mods:{m.group(1)}:{m.group(2)}" if hashlib.sha1(get(url)).hexdigest() == sha1 else None
    except OSError:
        return None


def main():
    mc = prop("minecraft_version")
    ap = argparse.ArgumentParser()
    ap.add_argument("--neo", default=os.path.join(ROOT, "run" if mc == "1.21.1" else f"run/{mc}"))
    ap.add_argument("--fabric", default=os.path.join(ROOT, "run-fabric" if mc == "1.21.1" else f"run/{mc}/fabric"))
    ap.add_argument("--manifest", default=os.path.join(ROOT, "dev-mods.json"))
    ap.add_argument("--seed", nargs="*", default=[], help="older manifests whose maven coordinates are reused by SHA-1")
    ap.add_argument("--prune", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    old = json.load(open(a.manifest, encoding="utf-8")) if os.path.exists(a.manifest) else {}
    known = {}  # sha1 -> maven
    for src in [old] + [json.load(open(s, encoding="utf-8")) for s in a.seed]:
        for loader in ("neoforge", "fabric"):
            for entries in (src.get(loader) or {}).values():
                for e in entries:
                    known.setdefault(e["sha1"], e["maven"])

    local = {"neoforge": scan(a.neo), "fabric": scan(a.fabric)}
    jars = []  # (loader, group, path, size, sha1, sha256)
    for loader, groups in local.items():
        for group, paths in groups.items():
            for p in paths:
                s1, s256 = digests(p)
                jars.append((loader, group, p, os.path.getsize(p), s1, s256))
    todo = sorted({j[4] for j in jars if j[4] not in known})
    if todo:
        print(f"looking up {len(todo)} jar(s) on Modrinth ...")
        known.update(modrinth_lookup(todo))
    missing = []
    for loader, group, p, size, s1, _ in jars:
        if s1 in known:
            continue
        name = os.path.basename(p)
        coord = ftb_lookup(name, s1) or curse_lookup(name, size, s1)
        if coord:
            known[s1] = coord
        else:
            missing.append(f"{loader}/{group}/{name}")
    if missing:
        raise SystemExit("not found on Modrinth, the FTB Maven or CurseForge (add its project to CURSE_PROJECTS, or it is not an "
                         "official file):\n  " + "\n  ".join(missing))

    new = {"comment": old.get("comment") or (
        f"Optional dev mods of Minecraft {mc} (gradle/dev-mods.gradle): per loader and group folder the official file "
        "(Modrinth Maven, CurseMaven, FTB Maven) with its SHA-1 and SHA-256. Written by tools/devmods/update_manifest.py; "
        "the jars are downloaded by Gradle and never committed."),
        "minecraft": mc}
    for loader in ("neoforge", "fabric"):
        groups = {}
        for g, entries in (old.get(loader) or {}).items():
            if g not in local[loader] and not a.prune:
                groups[g] = entries
        for lo, group, p, size, s1, s256 in jars:
            if lo == loader:
                groups.setdefault(group, []).append({"file": os.path.basename(p), "maven": known[s1], "size": size, "sha1": s1, "sha256": s256})
        new[loader] = {g: sorted(v, key=lambda e: e["file"].lower()) for g, v in sorted(groups.items())}
    for key in ("shaderpacks", "gaps"):
        if key in old:
            new[key] = old[key]

    def flat(m):
        return {(lo, g, e["file"], e["sha1"]) for lo in ("neoforge", "fabric") for g, v in (m.get(lo) or {}).items() for e in v}
    added, removed = flat(new) - flat(old), flat(old) - flat(new)
    for x in sorted(added):
        print("  +", "/".join(x[:3]))
    for x in sorted(removed):
        print("  -", "/".join(x[:3]))
    print(f"{len(added)} added, {len(removed)} removed, {len(flat(new))} jars in {a.manifest}")
    if not a.dry_run:
        with open(a.manifest, "w", encoding="utf-8", newline="\n") as f:
            json.dump(new, f, indent=1, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    sys.exit(main())
