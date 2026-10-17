#!/usr/bin/env bash
# PostToolUse(Edit|Write): runs sensor S-01 on the edited file and marks the
# session as dirty so the Stop hook runs the fast suite. Exit 2 feeds stderr back.
root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
file=$(jq -r '.tool_input.file_path // ""')
[[ -f "$file" ]] || exit 0

case "$file" in
  *.kt|*.kts|*.xml|*.toml|*.gradle) mkdir -p "$root/build/harness" && touch "$root/build/harness/.dirty" ;;
esac

if ! out=$("$root/scripts/harness/lint-file.sh" "$file" 2>&1); then
  printf '%s\n' "$out" >&2
  exit 2
fi
exit 0
