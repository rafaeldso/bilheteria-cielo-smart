#!/usr/bin/env bash
# One-time setup of the harness on a developer machine or a freshly created project.
# Idempotent: safe to run again. See docs/harness/bootstrap.md.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
cd "$HARNESS_ROOT"

echo "1/4 Permissões de execução"
chmod +x scripts/harness/*.sh .claude/hooks/*.sh .githooks/* 2>/dev/null || true

echo "2/4 Git hooks"
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git config core.hooksPath .githooks
  echo "    core.hooksPath=.githooks"
else
  echo "    repositório git não inicializado; rode 'git init' e execute este script de novo"
fi

echo "3/4 Ferramentas"
for tool in java jq python3 android; do
  if command -v "$tool" >/dev/null 2>&1; then echo "    ✓ $tool"; else echo "    ✗ $tool (necessário)"; fi
done

echo "4/4 Skills oficiais do Android CLI"
SKILLS=(android-cli testing-setup edge-to-edge adaptive jetpack-compose-performance
        navigation-3 android-intent-security android-permissions-security r8-analyzer agp-9-upgrade)
if command -v android >/dev/null 2>&1; then
  for s in "${SKILLS[@]}"; do
    android skills add "$s" --project="$HARNESS_ROOT" >/dev/null 2>&1 \
      && echo "    ✓ $s" || echo "    ✗ $s (instale manualmente: android skills add $s --project=.)"
  done
else
  echo "    Android CLI ausente: https://developer.android.com/tools/agents"
fi

echo "Pronto. Próximo: scripts/harness/verify.sh fast"
