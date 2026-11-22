#!/usr/bin/env bash
# M21.5 upgrade test: (re)builds the reference world "Bettacraft Referenz 1.13.1" in run/saves with the OLD version.
#
#   bash tools/upgrade/build_reference.sh [commit] [world name]
#
# Default commit 909d12a (1.13.1-beta.1, branch m21.1-plant-exterior, the version the user plays). It makes a
# temporary git worktree of that commit (outside the repo), copies DevUpgradeReference.java into it, hooks it into that
# version's DevShowcase (an extra row after the showcase rows, the manifest after saving), runs its buildShowcase (a
# dedicated server without a window) and moves the world to run/saves/<world name> of this repo. The world folder then
# holds bettacraft_reference.json, which `gradlew smoketestUpgrade` compares against.
set -euo pipefail
COMMIT="${1:-909d12a}"
NAME="${2:-Bettacraft Referenz 1.13.1}"
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
WT="${UPGRADE_WORKTREE:-$REPO/../bettacraft-upgrade-ref}"
if [ -d "$WT" ]; then git -C "$REPO" worktree remove --force "$WT" || rm -rf "$WT"; fi
git -C "$REPO" worktree add --detach "$WT" "$COMMIT"
# the dev code is in src/main up to 1.13.x and in the dev source set src/dev from 1.14.0 (M22) on
CMD="$WT/src/main/java/org/overengineer/bettacraft/command"
if [ -f "$WT/src/dev/java/org/overengineer/bettacraft/command/DevShowcase.java" ]; then CMD="$WT/src/dev/java/org/overengineer/bettacraft/command"; fi
# M52 multiloader layout: the dev source set is neoforge/src/dev; M52b (1.21.11): the runs live in run/1.21.11
if [ -f "$WT/neoforge/src/dev/java/org/overengineer/bettacraft/command/DevShowcase.java" ]; then CMD="$WT/neoforge/src/dev/java/org/overengineer/bettacraft/command"; fi
SAVES_REL="run/saves"
if grep -q "run/1.21.11" "$WT/neoforge/build.gradle" 2>/dev/null; then SAVES_REL="run/1.21.11/saves"; fi
if grep -q "run/26.1.2" "$WT/neoforge/build.gradle" 2>/dev/null; then SAVES_REL="run/26.1.2/saves"; fi
cp "$REPO/tools/upgrade/DevUpgradeReference.java" "$CMD/"
# M62: the old version's server takes this branch's port (never 25565, never the port hard-coded in that commit)
. "$REPO/tools/upgrade/server_port.sh"
pin_server_port "$WT" "${SAVES_REL%/saves}"
SHOW="$CMD/DevShowcase.java"
# extra row at the end of build(ServerLevel) (after the last showcase row); manifest right after the world is saved.
# M40: the end of build() is found by matching its braces (strings, chars and comments skipped): newer DevShowcase
# versions have other methods (terraria, blockOf ...) between build() and startRow(), where the old search put the hook.
python - "$SHOW" <<'PY'
import sys
p = sys.argv[1]
s = open(p, encoding='utf-8').read()
head = 'private static void build(ServerLevel level) {'
j = s.index(head) + len(head)
depth = 1
while depth > 0:
    if s.startswith('//', j):
        j = s.index('\n', j)
    elif s.startswith('/*', j):
        j = s.index('*/', j) + 2
    elif s[j] in '"\'':
        q = s[j]
        j += 1
        while s[j] != q:
            j += 2 if s[j] == '\\' else 1
        j += 1
    else:
        depth += {'{': 1, '}': -1}.get(s[j], 0)
        j += 1
end = s.rindex('\n', 0, j - 1) + 1  # the line with the closing brace of build()
s = s[:end] + '        DevUpgradeReference.extras(level, origin.offset(0, 0, row * ROW));\n' + s[end:]
s = s.replace('server.saveEverything(false, true, true);', 'server.saveEverything(false, true, true);\n                DevUpgradeReference.manifest(server, level);', 1)
open(p, 'w', encoding='utf-8').write(s)
PY
# its own run directory (nothing is shared with this repo's run/)
(cd "$WT" && ./gradlew buildShowcase --no-configuration-cache -q) || { check_server_log "$WT/build/showcase/server.log"; exit 1; }
check_server_log "$WT/build/showcase/server.log"
grep -q "MANIFEST WRITTEN" "$WT/build/showcase/server.log" || { echo "no manifest, see $WT/build/showcase/server.log"; exit 1; }
rm -rf "$REPO/$SAVES_REL/$NAME"
mkdir -p "$REPO/$SAVES_REL"
cp -r "$WT/$SAVES_REL/Bettacraft Schaukasten" "$REPO/$SAVES_REL/$NAME"
rm -f "$REPO/$SAVES_REL/$NAME/session.lock"
echo "commit $COMMIT built $(date)" > "$REPO/$SAVES_REL/$NAME/bettacraft_reference_version.txt"
git -C "$REPO" worktree remove --force "$WT"
echo "reference world: $REPO/$SAVES_REL/$NAME"
