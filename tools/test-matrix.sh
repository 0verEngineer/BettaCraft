#!/usr/bin/env bash
# The test matrix of this branch (M69, after Fast Craft Book's tools/test-matrix.sh): everything that must be green per
# milestone and before every release, both loaders, one log per step and a summary. Run it on every branch.
#
#   tools/test-matrix.sh [full] [--parallel] ["<client task + flags>" ...]
#       runData (common/src/generated must not change), build + :fabric:build (jars, unit tests, jar checks), the
#       NeoForge and the Fabric GameTests, the NeoForge and the Fabric server smoke, then every client smoke of
#       tools/test-matrix.txt and of the arguments, one after another (never two test clients at a time; background
#       windows, muted). --parallel runs the two GameTest suites side by side, and then the two server smokes (no clients).
#   tools/test-matrix.sh quick <gametest-prefixes>
#       compiles both loaders and runs only the GameTests of those prefixes on both loaders side by side (the loop while
#       working). Prefixes, comma separated: NeoForge 1.21.1 matches the lower-case method name or the batch, Fabric and
#       the newer NeoForge the class.method in lower case: give both, e.g. "filterlinesgametests,bettacraft_filter".
#
# Hang guard: every step has a timeout (TEST_MATRIX_TIMEOUT_SCALE=2 doubles them); a client whose log shows a loading
# error or a crash is stopped at once (only the game JVMs started from this checkout, never other ones). A GameTest run
# that ran no test counts as failed. Logs: build/test-matrix/<step>.log, build/test-matrix/summary.txt.
# Exit code 0 only if every step is green.
set -u
cd "$(dirname "$0")/.." || exit 2
WT=$(pwd)
MODE=full
PARALLEL=0
CLIENTS=()
if [ "${1:-}" = quick ]; then MODE=quick; shift; PREFIXES=${1:-}; shift || true; fi
[ "${1:-}" = full ] && shift
for a in "$@"; do
  if [ "$a" = --parallel ]; then PARALLEL=1; else CLIENTS+=("$a"); fi
done
OUT="$WT/build/test-matrix"; rm -rf "$OUT"; mkdir -p "$OUT"
SUM="$OUT/summary.txt"; : > "$SUM"; FAIL=0
G="./gradlew --no-configuration-cache --console=plain"
SCALE=${TEST_MATRIX_TIMEOUT_SCALE:-1}

log() { echo "$(date +%H:%M:%S) $*" | tee -a "$SUM"; }

# stops the game JVMs (NeoForge / Fabric client or server) launched from this checkout; never Gradle daemons or other folders
kill_games() {
  if command -v powershell.exe > /dev/null; then
    local wt; wt=$(cygpath -w "$WT" 2>/dev/null || echo "$WT")
    powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*$(echo "$wt" | sed 's/\\/\\\\/g')*' -and (\$_.CommandLine -like '*fml.*' -or \$_.CommandLine -like '*net.fabricmc*' -or \$_.CommandLine -like '*KnotClient*' -or \$_.CommandLine -like '*KnotServer*') } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" 2>/dev/null
  else
    pkill -f "java.*$WT.*(fml\.|net\.fabricmc|Knot)" 2>/dev/null || true
  fi
}

# step <name> <timeout-min> <client:0|1> <gradle args...>
step() {
  local name=$1 tmin=$(( $2 * SCALE )) client=$3; shift 3
  local lf="$OUT/$name.log" t0; t0=$(date +%s)
  $G "$@" < /dev/null > "$lf" 2>&1 & local pid=$!
  local rc=""
  while kill -0 $pid 2>/dev/null; do
    sleep 10
    if [ $(( $(date +%s) - t0 )) -gt $(( tmin * 60 )) ]; then log "TIMEOUT $name after ${tmin} min"; kill_games; kill $pid 2>/dev/null; rc=124; break; fi
    if [ "$client" = 1 ] && grep -qiE "Mod loading has failed|ModLoadingException|Loading errors encountered|Crash report saved|---- Minecraft Crash Report|Incompatible mods found" "$lf"; then
      log "LOADING ERROR/CRASH $name (client stopped)"; kill_games; kill $pid 2>/dev/null; rc=125; break
    fi
  done
  [ -z "$rc" ] && { wait $pid; rc=$?; }
  # a GameTest run that ran nothing (a wrong prefix) is not green
  if [ "$rc" = 0 ] && grep -qE "All 0 required tests passed|[^0-9]0 GAME TESTS COMPLETE" "$lf"; then log "NO TESTS RAN in $name (check the prefix)"; rc=126; fi
  # a client smoke that ran its scene must not report a failure
  if [ "$rc" = 0 ] && [ "$client" = 1 ] && grep -q "\[smoketest\] FAILED" "$lf"; then log "[smoketest] FAILED in $name"; rc=128; fi
  # a server smoke must say so
  if [ "$rc" = 0 ] && [[ "$name" == server* ]]; then
    local need=1; [ "$name" = servers ] && need=2
    [ "$(grep -c "\[smoketest\] PASSED" "$lf")" -ge $need ] || { log "NO [smoketest] PASSED in $name"; rc=127; }
  fi
  local dt=$(( ($(date +%s) - t0) / 60 ))
  if [ "$rc" = 0 ]; then log "OK   $name (${dt} min)"; else log "FAIL $name rc=$rc (${dt} min) -> $lf"; FAIL=1; fi
  return 0
}

log "test matrix $MODE on $(git rev-parse --abbrev-ref HEAD) ($(git rev-parse --short HEAD)), Minecraft $(grep '^minecraft_version=' gradle.properties | cut -d= -f2 | tr -d '\r')"
if [ "$MODE" = quick ]; then
  step compile 15 0 compileJava :fabric:compileJava
  if [ -n "$PREFIXES" ]; then step gametests 30 0 runGameTestServer runFabricGameTest --parallel "-Pgametests=$PREFIXES"; fi
else
  step rundata 20 0 runData
  if [ -n "$(git status --porcelain -- common/src/generated)" ]; then log "FAIL runData changed common/src/generated"; FAIL=1; else log "OK   runData diff empty"; fi
  step build 30 0 build :fabric:build
  if [ $PARALLEL = 1 ]; then
    step gametests 45 0 runGameTestServer runFabricGameTest --parallel
    step servers 20 0 runServer runFabricServer --parallel -Psmoketest
  else
    step gametests-neoforge 45 0 runGameTestServer
    step gametests-fabric 45 0 runFabricGameTest
    step server-neoforge 20 0 runServer -Psmoketest
    step server-fabric 20 0 runFabricServer -Psmoketest
  fi
  # the client smokes of this branch (tools/test-matrix.txt) and of the arguments, one after another
  mapfile -t listed < <(grep -v '^\s*\(#\|$\)' tools/test-matrix.txt | tr -d '\r')
  i=0
  for c in "${listed[@]}" "${CLIENTS[@]}"; do
    i=$((i + 1))
    # shellcheck disable=SC2086 # c holds the task and its separate Gradle arguments
    step "client$i-$(echo "$c" | awk '{print $1}')" 20 1 $c
    echo "           client$i = $c" >> "$SUM"
  done
fi
[ $FAIL = 0 ] && log "ALL GREEN" || log "NOT GREEN"
exit $FAIL
