#!/usr/bin/env bash
# TokiDiscover versions:
#   X.Y.Z      release
#   X.Y.Z-bN   prerelease
# Order: 0.1.0 < 0.1.1-b1 < 0.1.1-b2 < 0.1.1
set -euo pipefail

toki_parse() {
  local v="${1#v}"
  if [[ "$v" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    printf '%s %s %s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" "${BASH_REMATCH[3]}"
    return 0
  fi
  if [[ "$v" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)-b([0-9]+)$ ]]; then
    printf '%s %s %s %s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" "${BASH_REMATCH[3]}" "${BASH_REMATCH[4]}"
    return 0
  fi
  return 1
}

num_cmp() {
  local x=$((10#$1)) y=$((10#$2))
  if (( x < y )); then
    echo -1
  elif (( x > y )); then
    echo 1
  else
    echo 0
  fi
}

# Print -1 when a < b, 0 when equal, 1 when a > b. Exit 2 when either side is not a TokiDiscover version.
toki_cmp() {
  local a b a1 a2 a3 ap b1 b2 b3 bp r
  a="$(toki_parse "$1")" || return 2
  b="$(toki_parse "$2")" || return 2
  read -r a1 a2 a3 ap <<<"$a"
  read -r b1 b2 b3 bp <<<"$b"
  r="$(num_cmp "$a1" "$b1")"
  [[ "$r" == "0" ]] || { echo "$r"; return 0; }
  r="$(num_cmp "$a2" "$b2")"
  [[ "$r" == "0" ]] || { echo "$r"; return 0; }
  r="$(num_cmp "$a3" "$b3")"
  [[ "$r" == "0" ]] || { echo "$r"; return 0; }
  if [[ -z "${ap:-}" && -z "${bp:-}" ]]; then
    echo 0
  elif [[ -z "${ap:-}" ]]; then
    echo 1
  elif [[ -z "${bp:-}" ]]; then
    echo -1
  else
    num_cmp "$ap" "$bp"
  fi
}

# prerelease for X.Y.Z-bN (optional v prefix). Every other tag is a release.
toki_kind() {
  local v="${1#v}"
  if [[ "$v" =~ ^[0-9]+\.[0-9]+\.[0-9]+-b[0-9]+$ ]]; then
    echo prerelease
  else
    echo release
  fi
}

self_test() {
  expect_cmp() {
    local got
    got="$(toki_cmp "$2" "$3")" || { echo "cmp failed: $2 $3" >&2; exit 1; }
    [[ "$got" == "$1" ]] || { echo "cmp $2 $3: expected $1, got $got" >&2; exit 1; }
  }
  expect_kind() {
    local got
    got="$(toki_kind "$2")"
    [[ "$got" == "$1" ]] || { echo "kind $2: expected $1, got $got" >&2; exit 1; }
  }

  expect_cmp -1 0.1.0 0.1.1-b1
  expect_cmp -1 0.1.1-b1 0.1.1-b2
  expect_cmp -1 0.1.1-b2 0.1.1
  expect_cmp -1 0.1.1-b1 0.1.1
  expect_cmp -1 v0.1.1-b1 v0.1.1
  expect_cmp 0 0.1.0 0.1.0
  expect_cmp 0 v0.1.1-b2 0.1.1-b2
  expect_cmp 1 0.1.1 0.1.1-b1
  expect_cmp -1 0.1.0 0.2.0-b1
  expect_cmp 1 0.1.1-b10 0.1.1-b2
  expect_cmp -1 0.1.1-b08 0.1.1-b10
  expect_cmp 1 0.1.10 0.1.9
  expect_cmp -1 0.9.0 0.10.0
  if toki_cmp 0.1.1-beta.1 0.1.1 >/dev/null; then
    echo "expected 0.1.1-beta.1 to be rejected" >&2
    exit 1
  fi
  expect_kind prerelease v0.1.1-b1
  expect_kind prerelease 0.1.1-b2
  expect_kind release v0.1.0
  expect_kind release v0.1.1
  expect_kind release 0.1.0-beta.1
  echo ok
}

case "${1:-}" in
  --self-test)
    self_test
    ;;
  --kind)
    [[ $# -eq 2 ]] || { echo "usage: version.sh --kind <version>" >&2; exit 2; }
    toki_kind "$2"
    ;;
  --cmp)
    [[ $# -eq 3 ]] || { echo "usage: version.sh --cmp <a> <b>" >&2; exit 2; }
    toki_cmp "$2" "$3"
    ;;
  *)
    echo "usage: version.sh --self-test | --kind <version> | --cmp <a> <b>" >&2
    exit 2
    ;;
esac
