#!/usr/bin/env bash
# Release tag check (M52, .github/workflows/release.yml): a release tag names the mod version, the loader and the Minecraft
# version, v<mod_version>+<loader>-<minecraft>, e.g. v1.42.0-beta.1+neoforge-1.21.1 or v1.42.0-beta.1+fabric-26.1.2. Each
# branch releases its own Minecraft version, one loader per tag.
#
#   IS_TAG=true  GITHUB_REF_NAME=<tag>          tools/ci/release_tag.sh   # a pushed tag: checked against gradle.properties
#   IS_TAG=false INPUT_LOADER=neoforge|fabric   tools/ci/release_tag.sh   # a dry run (workflow_dispatch) of that loader
#
# Fails when the tag does not have that form, its mod version is not mod_version or its Minecraft version is not the
# branch's minecraft_version (gradle.properties of the tagged commit). Writes version, loader, loader_name, mc, type
# (alpha / beta / release), tag and jar to $GITHUB_OUTPUT (stdout without it), and (M69, so that release.yml is the same
# file on every branch) the branch facts of the CurseForge upload:
#   java          java_version of gradle.properties (the Java of the game)
#   game_versions the CurseForge game versions of the loader, one per line: minecraft_version for NeoForge,
#                 fabric_game_versions (comma separated, e.g. 26.1,26.1.1,26.1.2) for Fabric
#   game_label    the Minecraft versions in the file name, e.g. 1.21.1 or 26.1-26.1.2
#   dependencies  the CurseForge dependencies of the loader, one per line (tools/ci/curseforge-<loader>.txt of the branch)
#   latest        true when this release becomes the latest GitHub release (release_make_latest=true in gradle.properties:
#                 only master, the main target ATM10, and only its NeoForge releases that are no pre-release)
set -euo pipefail

prop() { grep "^$1=" gradle.properties | head -1 | cut -d= -f2- | tr -d '\r'; }
fail() { echo "::error::$1"; exit 1; }

version=$(prop mod_version)
mc=$(prop minecraft_version)
java=$(prop java_version)
[ -n "$version" ] && [ -n "$mc" ] || fail "gradle.properties has no mod_version or minecraft_version"
[ -n "$java" ] || fail "gradle.properties has no java_version"

if [ "${IS_TAG:-false}" = true ]; then
  tag=${GITHUB_REF_NAME:?GITHUB_REF_NAME is not set}
  if [[ ! "$tag" =~ ^v([^+]+)\+([a-z]+)-([0-9][0-9A-Za-z.\-]*)$ ]]; then
    fail "tag $tag is not v<mod_version>+<loader>-<minecraft>, e.g. v$version+neoforge-$mc or v$version+fabric-$mc"
  fi
  tag_version=${BASH_REMATCH[1]}
  loader=${BASH_REMATCH[2]}
  tag_mc=${BASH_REMATCH[3]}
  [ "$tag_version" = "$version" ] || fail "tag $tag: mod version $tag_version is not mod_version $version of gradle.properties"
  [ "$tag_mc" = "$mc" ] || fail "tag $tag: Minecraft $tag_mc, but this commit builds Minecraft $mc (tag the branch of Minecraft $tag_mc)"
else
  loader=${INPUT_LOADER:-neoforge}
  tag="v$version+$loader-$mc"
fi

case "$loader" in
  neoforge) loader_name=NeoForge ;;
  fabric) loader_name=Fabric ;;
  *) fail "unknown loader $loader in $tag (neoforge or fabric)" ;;
esac

case "$version" in
  *-alpha*) type=alpha ;;
  *-beta*|*-rc*) type=beta ;;
  *-*) fail "unknown pre-release suffix in $version (use -alpha.n, -beta.n or -rc.n)" ;;
  *) type=release ;;
esac

if [ "$loader" = fabric ]; then
  games=$(prop fabric_game_versions)
  [ -n "$games" ] || fail "gradle.properties has no fabric_game_versions"
else
  games=$mc
fi
games=$(echo "$games" | tr ',' '\n' | sed 's/^ *//; s/ *$//' | grep -v '^$')
first=$(echo "$games" | head -1)
last=$(echo "$games" | tail -1)
if [ "$first" = "$last" ]; then game_label=$first; else game_label="$first-$last"; fi
echo "$games" | grep -qx "$mc" || fail "the CurseForge game versions of $loader ($(echo $games)) do not contain minecraft_version $mc"

deps_file="tools/ci/curseforge-$loader.txt"
[ -f "$deps_file" ] || fail "$deps_file is missing (the CurseForge dependencies of $loader on this branch)"
deps=$(grep -v '^\s*\(#\|$\)' "$deps_file" | tr -d '\r')

latest=false
if [ "$type" = release ] && [ "$loader" = neoforge ] && [ "$(prop release_make_latest)" = true ]; then latest=true; fi

out=${GITHUB_OUTPUT:-/dev/stdout}
{
  echo "version=$version"
  echo "loader=$loader"
  echo "loader_name=$loader_name"
  echo "mc=$mc"
  echo "type=$type"
  echo "tag=$tag"
  echo "jar=build/libs/bettacraft-$loader-$mc-$version.jar"
  echo "java=$java"
  echo "game_label=$game_label"
  echo "latest=$latest"
  echo "game_versions<<EOF"
  echo "$games"
  echo "EOF"
  echo "dependencies<<EOF"
  echo "$deps"
  echo "EOF"
} >> "$out"
echo "Bettacraft $version for $loader_name $game_label (Java $java), channel $type, tag $tag (pushed tag: ${IS_TAG:-false})"
