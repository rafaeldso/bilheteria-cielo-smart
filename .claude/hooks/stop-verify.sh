#!/usr/bin/env bash
# Stop: if source files changed in this session, run `verify.sh fast` before the
# agent finishes. On failure, blocks the stop (exit 2) and returns the summary.
# Gives up after 3 consecutive failed attempts to avoid infinite loops.
# Level via HARNESS_STOP_LEVEL (in .claude/settings.local.json "env"): off | light | fast (default).
root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
level="${HARNESS_STOP_LEVEL:-fast}"
[[ "$level" == "off" ]] && exit 0
state="$root/build/harness"
dirty="$state/.dirty"
attempts_file="$state/.stop-attempts"

[[ -f "$dirty" ]] || exit 0

attempts=$(cat "$attempts_file" 2>/dev/null || echo 0)
if (( attempts >= 3 )); then
  rm -f "$attempts_file"
  echo "Harness: verify.sh $level falhou 3 vezes seguidas; devolvendo controle ao usuário." >&2
  exit 0
fi

if out=$("$root/scripts/harness/verify.sh" "$level" 2>&1); then
  rm -f "$dirty" "$attempts_file"
  exit 0
fi

echo $((attempts + 1)) > "$attempts_file"
{
  echo "Os sensores do harness (nível $level) falharam. Corrija antes de encerrar (tentativa $((attempts + 1))/3):"
  printf '%s\n' "$out" | tail -80
} >&2
exit 2
