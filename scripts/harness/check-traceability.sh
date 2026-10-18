#!/usr/bin/env bash
# Sensor S-09: every AC-xx in a spec must be cited by at least one test name.
# Scope: the spec's "Módulo" field (e.g. feature/payment); falls back to the whole repo.
# Usage: check-traceability.sh [docs/specs/NNN-feature ...]   (no args = all specs)
set -uo pipefail
source "$(dirname "$0")/lib.sh"
cd "$HARNESS_ROOT"

if (( $# > 0 )); then specs=("$@"); else
  specs=()
  for d in docs/specs/[0-9]*/; do [[ -f "$d/spec.md" ]] && specs+=("${d%/}"); done
fi

if (( ${#specs[@]} == 0 )); then
  echo "SENSOR S-09 OK: nenhuma spec encontrada ainda"; exit 0
fi

missing=0
for spec_dir in "${specs[@]}"; do
  spec="$spec_dir/spec.md"
  [[ -f "$spec" ]] || { echo "S-09: $spec não encontrado"; missing=$((missing + 1)); continue; }

  status=$(grep -m1 -E '^\| \*\*Status\*\*' "$spec" | sed -E 's/.*\| *([^|]*) *\|$/\1/' | xargs)
  case "$status" in Rascunho*|"Em revisão"*|Obsoleta*) echo "S-09: $spec_dir ignorada (status: $status)"; continue ;; esac
  # Aprovada = em implementação: tests are still being written, so missing ACs are reported as a
  # warning. "Implementada" (set when closing the feature) enforces. Steering loop 2026-10-17.
  enforce=1
  case "$status" in Aprovada*) enforce=0 ;; esac
  warned=0

  module=$(grep -m1 -E '^\| \*\*Módulo\*\*' "$spec" | grep -oE '`[^`<>]+`' | head -1 | tr -d '`')
  search_root="${module:-.}"
  [[ -d "$search_root" ]] || { echo "S-09: módulo '$search_root' de $spec_dir não existe; buscando no repositório todo"; search_root="."; }

  acs=$(grep -oE '^### AC-[0-9]+' "$spec" | sed 's/### //' | sort -u)
  for ac in $acs; do
    if ! grep -rqsE "\`$ac\b[^\`]*\`" --include='*.kt' \
        $(find "$search_root" -type d \( -path '*/src/test' -o -path '*/src/androidTest' -o -path '*/src/testDebug' \) 2>/dev/null) /dev/null; then
      if (( enforce == 0 )); then warned=$((warned + 1)); continue; fi
      sensor_fail "S-09" "AC sem teste" "$spec_dir ($ac)" \
        "$ac não é citado no nome de nenhum teste em '$search_root'." \
        "Crie um teste: fun \`$ac - dado ... quando ... entao ...\`() no módulo da feature." \
        "docs/harness/guides/testing.md"
      missing=$((missing + 1))
    fi
  done
  (( warned > 0 )) && echo "S-09 AVISO: $spec_dir ($status) tem $warned AC(s) ainda sem teste; vira falha quando o status for Implementada."
done

if (( missing > 0 )); then echo "S-09: $missing AC(s) sem teste."; exit 1; fi
echo "SENSOR S-09 OK: nenhum AC sem teste em spec Implementada (avisos de specs em implementação, se houver, acima)"
