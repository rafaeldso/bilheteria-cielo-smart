#!/usr/bin/env bash
# Local security scan: the same computational scanners as .github/workflows/security.yml.
# Tools that are not installed are skipped with a warning (CI still runs them).
# Usage: security-scan.sh [--strict]   (--strict: a missing tool counts as failure)
#
#   S-14 gitleaks     secrets in the working tree (and history, inside git)
#   S-15 osv-scanner  CVEs in resolved dependencies (gradle.lockfile)
#   S-21 zizmor       GitHub Actions workflows
#   S-22 mobsfscan    mobile SAST on Kotlin/Java/manifest
#
# Full logs go to build/harness/security/. Process: docs/security/README.md.
set -uo pipefail
source "$(dirname "$0")/lib.sh"
cd "$HARNESS_ROOT"

STRICT=false
[[ "${1:-}" == "--strict" ]] && STRICT=true

LOG_DIR="build/harness/security"
mkdir -p "$LOG_DIR"
failed=(); skipped=()

run_scanner() { # <id> <tool> <install-hint> <command...>
  local id="$1" tool="$2" hint="$3"; shift 3
  local log="$LOG_DIR/$tool.log"
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "SENSOR $id IGNORADO: $tool não instalado (instale com: $hint)"
    skipped+=("$id")
    return
  fi
  printf '▶ %s %s… ' "$id" "$tool"
  if "$@" >"$log" 2>&1; then
    echo "ok"
  else
    echo "FALHOU"
    failed+=("$id")
    sensor_fail "$id" "$tool encontrou problemas" "$log" \
      "$(grep -vE '^\s*$' "$log" | head -15 | sed 's/^/    /')" \
      "Corrija cada achado; falso positivo segue docs/security/triage.md#falso-positivo (nunca suprima a regra inteira)" \
      "docs/security/README.md"
  fi
}

gitleaks_cmd() {
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    gitleaks git --redact --exit-code 1 . && gitleaks dir --redact --exit-code 1 .
  else
    gitleaks dir --redact --exit-code 1 .
  fi
}

osv_cmd() {
  osv-scanner scan source --recursive .
  local code=$?
  # 128 = no lockfile found: acceptable only before the Android project exists.
  if (( code == 128 )) && ! has_gradle; then echo "Sem lockfiles (projeto Android ainda não criado)"; return 0; fi
  return "$code"
}

echo "Security scan local"
run_scanner S-14 gitleaks "brew install gitleaks" gitleaks_cmd
run_scanner S-15 osv-scanner "brew install osv-scanner" osv_cmd
if [[ -d .github/workflows ]]; then
  run_scanner S-21 zizmor "brew install zizmor  (ou pipx install zizmor)" zizmor .github/workflows
fi
run_scanner S-22 mobsfscan "pipx install mobsfscan" mobsfscan .

echo
if (( ${#failed[@]} > 0 )); then
  echo "RESULTADO: FALHOU (${failed[*]}). Logs: $LOG_DIR/"
  exit 1
fi
if (( ${#skipped[@]} > 0 )); then
  if $STRICT; then echo "RESULTADO: FALHOU (--strict; não executados: ${skipped[*]})"; exit 1; fi
  echo "AVISO: não executados localmente: ${skipped[*]} (o CI executa todos)."
fi
echo "RESULTADO: OK"
