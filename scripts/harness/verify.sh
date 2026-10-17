#!/usr/bin/env bash
# Harness verification entry point used by agents, hooks, pre-commit and CI.
# Usage: verify.sh [light|fast|standard|full]   (default: standard)
#
#   light     S-01 + S-02 on changed files only, no Gradle (~1 s)
#   fast      light + Gradle `harnessFast` (spotless, detekt, Konsist)
#   standard  fast + Gradle `harnessCheck` (unit tests, Android Lint) + S-09 traceability
#   full      standard + Gradle `harnessFull` (Roborazzi, Kover, module graph, release build)
#
# Output is condensed for LLM consumption: a summary per sensor plus the first
# relevant error lines; full logs go to build/harness/<level>/.
set -uo pipefail
source "$(dirname "$0")/lib.sh"
cd "$HARNESS_ROOT"

LEVEL="${1:-standard}"
case "$LEVEL" in light|fast|standard|full) ;; *) echo "Uso: $0 [light|fast|standard|full]"; exit 64 ;; esac

LOG_DIR="build/harness/$LEVEL"
mkdir -p "$LOG_DIR"
failed=()
start=$(date +%s)

run_step() { # <name> <command...>
  local name="$1"; shift
  local log="$LOG_DIR/${name// /_}.log"
  printf '▶ %s… ' "$name"
  if "$@" >"$log" 2>&1; then
    echo "ok"
  else
    echo "FALHOU"
    failed+=("$name")
    echo "----- $name (resumo; log completo: $log) -----"
    # Our own sensors already print the contract; Gradle output is filtered.
    if grep -q '^SENSOR ' "$log"; then
      grep -A5 '^SENSOR .* FALHOU' "$log" | head -60
    else
      grep -E '^e: |error:|ERROR|FAILED|Violation|✗|> Task .* FAILED|What went wrong|expected:|but was:|AssertionFailedError|\.kt:[0-9]+' "$log" \
        | grep -v '^\s*at ' | head -40
      echo "Relatórios: build/reports/ (detekt, lint, tests, kover, roborazzi)"
    fi
    echo "-----"
  fi
}

changed_files() {
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    { git diff --name-only --diff-filter=ACMR HEAD 2>/dev/null; git ls-files --others --exclude-standard; } | sort -u
  fi
}

gradle_task() { # <task>
  if has_gradle; then
    ./gradlew "$1" --continue --console=plain
  else
    echo "SENSOR gradle IGNORADO: gradlew ausente (projeto Android ainda não criado; ver docs/harness/bootstrap.md)"
  fi
}

echo "Harness verify — nível: $LEVEL"

files=$(changed_files)
if [[ -n "$files" ]]; then
  # shellcheck disable=SC2086
  run_step "S-01 padroes proibidos" scripts/harness/lint-file.sh $files
fi
# Daily levels scan only what changed; standard/full scan the whole repository.
case "$LEVEL" in
  light|fast) [[ -n "$files" ]] && run_step "S-02 dados sensiveis" scripts/harness/scan-sensitive-data.sh $files ;;
  *)          run_step "S-02 dados sensiveis" scripts/harness/scan-sensitive-data.sh ;;
esac

case "$LEVEL" in
  light)    ;;
  fast)     run_step "gradle harnessFast" gradle_task harnessFast ;;
  standard) run_step "gradle harnessCheck" gradle_task harnessCheck
            run_step "S-09 rastreabilidade" scripts/harness/check-traceability.sh ;;
  full)     run_step "gradle harnessFull" gradle_task harnessFull
            run_step "S-09 rastreabilidade" scripts/harness/check-traceability.sh ;;
esac

elapsed=$(( $(date +%s) - start ))
echo
if (( ${#failed[@]} > 0 )); then
  echo "RESULTADO: FALHOU (${#failed[@]} sensor(es): ${failed[*]}) em ${elapsed}s"
  echo "Próximo passo: corrija a causa de cada falha acima e rode novamente 'scripts/harness/verify.sh $LEVEL'."
  echo "Não desative sensores para passar (Constitution Art. VIII.3)."
  exit 1
fi
[[ "$LEVEL" != light ]] && ! has_gradle && echo "AVISO: sensores Gradle não executados (sem gradlew)."
echo "RESULTADO: OK — nível $LEVEL em ${elapsed}s"
