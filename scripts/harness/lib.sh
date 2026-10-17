#!/usr/bin/env bash
# Shared helpers for harness sensors. Source it; do not execute directly.
# Every failure follows the sensor message contract (docs/harness/sensors/README.md).

HARNESS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export HARNESS_ROOT

# sensor_fail <sensor-id> <rule> <location> <problem> <fix> <reference>
sensor_fail() {
  printf 'SENSOR %s FALHOU: %s\n  Onde: %s\n  Problema: %s\n  Como corrigir: %s\n  Referência: %s\n\n' \
    "$1" "$2" "$3" "$4" "$5" "$6"
}

sensor_ok() {
  printf 'SENSOR %s OK: %s\n' "$1" "$2"
}

is_test_path() {
  [[ "$1" =~ /src/(test|androidTest|testFixtures|testDebug|testRelease)/ ]] || [[ "$1" =~ /core/testing/ ]]
}

is_generated_path() {
  [[ "$1" =~ (^|/)build/ ]] || [[ "$1" =~ /generated/ ]]
}

has_gradle() {
  [[ -x "$HARNESS_ROOT/gradlew" ]]
}

# Lists tracked files when inside git, otherwise every file outside build dirs.
list_project_files() {
  if git -C "$HARNESS_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git -C "$HARNESS_ROOT" ls-files --cached --others --exclude-standard
  else
    (cd "$HARNESS_ROOT" && find . -type f \
      -not -path './.git/*' -not -path '*/build/*' -not -path './.gradle/*' \
      -not -path './.idea/*' -not -path '*/node_modules/*' | sed 's|^\./||')
  fi
}
