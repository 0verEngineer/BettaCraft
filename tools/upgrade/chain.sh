#!/usr/bin/env bash
# M42 part b: upgrade chain. A reference world of an old version (run/saves/<from>, made by build_reference.sh, with its
# manifest bettacraft_reference.json) is opened and saved by an intermediate version first, then `gradlew smoketestUpgrade`
# loads that world with this version and compares it with the OLD manifest: nothing may be lost on the way over two upgrades.
#
#   bash tools/upgrade/chain.sh [intermediate commit] [from world] [chain world name]
#
# Default: the 1.13.1 reference through 1.31.0-beta.1 (commit 6800aec, M39) into "Bettacraft Kette 1.13.1-1.31.0". The
# intermediate version runs in a temporary git worktree outside the repo (its own run directory) with its
# runServer -PsmoketestOldWorld (a dedicated server without a window: loads the world, checks every tank, saves, stops).
set -euo pipefail
COMMIT="${1:-6800aec}"
FROM="${2:-Bettacraft Referenz 1.13.1}"
NAME="${3:-Bettacraft Kette 1.13.1-1.31.0}"
REPO="$(cd "$(dirname "$0")/../.." && pwd)"
WT="${UPGRADE_WORKTREE:-$REPO/../bettacraft-upgrade-chain}"
SRC="$REPO/run/saves/$FROM"
[ -f "$SRC/bettacraft_reference.json" ] || { echo "no reference world $SRC (build it with build_reference.sh)"; exit 1; }
if [ -d "$WT" ]; then git -C "$REPO" worktree remove --force "$WT" || rm -rf "$WT"; fi
git -C "$REPO" worktree add --detach "$WT" "$COMMIT"
mkdir -p "$WT/run/saves"
cp -r "$SRC" "$WT/run/saves/chain_in"
rm -f "$WT/run/saves/chain_in/session.lock"
printf 'eula=true\n' > "$WT/run/eula.txt"
printf 'online-mode=false\nserver-port=25597\nspawn-protection=0\nmax-players=1\n' > "$WT/run/server.properties"
(cd "$WT" && ./gradlew runServer -PsmoketestOldWorld -PoldWorld=chain_in -PkeepWorld --no-configuration-cache -q > "$WT/chain-server.log" 2>&1) || true
grep -q "OLD WORLD PASSED" "$WT/chain-server.log" || { echo "the intermediate version did not pass the old world, see $WT/chain-server.log"; exit 1; }
rm -rf "$REPO/run/saves/$NAME"
cp -r "$WT/run/oldworld_check" "$REPO/run/saves/$NAME"
rm -f "$REPO/run/saves/$NAME/session.lock"
cp "$SRC/bettacraft_reference.json" "$REPO/run/saves/$NAME/bettacraft_reference.json"
echo "reference '$FROM' saved by commit $COMMIT on $(date)" > "$REPO/run/saves/$NAME/bettacraft_reference_version.txt"
git -C "$REPO" worktree remove --force "$WT"
echo "chain world ready: run/saves/$NAME -> gradlew smoketestUpgrade -PupgradeWorld=\"$NAME\" --no-configuration-cache"
