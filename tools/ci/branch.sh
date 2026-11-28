#!/usr/bin/env bash
# The branch facts of the workflows (M69): build.yml, integration.yml and release.yml are the same file on every branch
# (master = Minecraft 1.21.1, one branch per other Minecraft version); what differs comes from gradle.properties and
# gradle/dev-mods.json of the checked-out commit through this script.
#
#   tools/ci/branch.sh        # writes to $GITHUB_OUTPUT (stdout without it)
#
# Outputs:
#   mc              minecraft_version
#   java            java_version: the Java the game of this branch runs on (21 or 25)
#   java_versions   the JDKs to install, the last one is the default: 25 for the Gradle daemon (Fabric Loom,
#                   gradle/gradle-daemon-jvm.properties) and java_version (one line when they are the same)
#   run_dir         the NeoForge run directory (run on 1.21.1, run/<mc> on the ports; as gradle/dev-mods.gradle)
#   fabric_run_dir  the Fabric run directory (run-fabric on 1.21.1, run/<mc>/fabric on the ports)
#   power           true when gradle/dev-mods.json has NeoForge power mods for this version (the -Ppower GameTests)
set -euo pipefail

prop() { grep "^$1=" gradle.properties | head -1 | cut -d= -f2- | tr -d '\r'; }
fail() { echo "::error::$1"; exit 1; }

mc=$(prop minecraft_version)
java=$(prop java_version)
[ -n "$mc" ] || fail "gradle.properties has no minecraft_version"
[ -n "$java" ] || fail "gradle.properties has no java_version"

if [ "$mc" = 1.21.1 ]; then
  run_dir=run
  fabric_run_dir=run-fabric
else
  run_dir="run/$mc"
  fabric_run_dir="run/$mc/fabric"
fi

power=false
if [ -f gradle/dev-mods.json ] && python3 -c 'import json,sys; sys.exit(0 if json.load(open("gradle/dev-mods.json")).get("neoforge", {}).get("power-mods") else 1)'; then
  power=true
fi

out=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "mc=$mc"
  echo "java=$java"
  echo "java_versions<<EOF"
  [ "$java" = 25 ] || echo 25
  echo "$java"
  echo "EOF"
  echo "run_dir=$run_dir"
  echo "fabric_run_dir=$fabric_run_dir"
  echo "power=$power"
} >> "$out"
echo "Minecraft $mc, Java $java, run dirs $run_dir and $fabric_run_dir, power GameTests: $power"
