#!/usr/bin/env bash
# Test of the curseforgeDescription cache (M65): the task must run again and pin the links to the new commit when only the
# commit behind the same ref name changes (a release tag set again on another commit). It used to be UP-TO-DATE then, with
# the old commit in every link.
#
#   bash tools/ci/test_curseforge_description.sh
#
# Makes a throwaway commit object (same tree as HEAD, no branch moves, works in a shallow clone) and a temporary local tag,
# builds the description with the tag on HEAD, moves the tag to the other commit, builds again; deletes the tag at the end.
set -euo pipefail
cd "$(dirname "$0")/../.."

tag="cfdesc-cache-test-$$"
gradle=(./gradlew --console=plain --quiet curseforgeDescription "-PdescriptionRef=$tag")
out=build/curseforge/description.md
cleanup() { git tag -d "$tag" >/dev/null 2>&1 || true; }
trap cleanup EXIT

first=$(git rev-parse HEAD)
second=$(GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@localhost GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@localhost \
  git commit-tree "HEAD^{tree}" -p HEAD -m "curseforgeDescription cache test")

check() {
  local sha=$1 other=$2
  grep -q "raw.githubusercontent.com/[^/]*/[^/]*/$sha/" "$out" || { echo "FAIL: $out has no link pinned to $sha"; exit 1; }
  if grep -q "/$other/" "$out"; then echo "FAIL: $out still links $other"; exit 1; fi
}

git tag -f "$tag" "$first" >/dev/null
"${gradle[@]}"
check "$first" "$second"

git tag -f "$tag" "$second" >/dev/null
"${gradle[@]}"
check "$second" "$first"

echo "PASS: curseforgeDescription follows the commit behind the ref ($first -> $second)"
