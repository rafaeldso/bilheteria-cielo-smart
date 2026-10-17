#!/usr/bin/env bash
# PreToolUse(Bash): blocks destructive, outward-facing or sensor-bypassing commands.
# Exit 2 = block; stderr is shown to the agent.
cmd=$(jq -r '.tool_input.command // ""')

block() {
  printf 'BLOQUEADO PELO HARNESS: %s\nComando: %s\nComo proceder: %s\n' "$1" "$cmd" "$2" >&2
  exit 2
}

shopt -s nocasematch
[[ "$cmd" =~ git[[:space:]]+push ]] \
  && block "push para remoto exige pedido explícito do usuário." "Peça ao usuário para fazer o push ou autorizar explicitamente."
[[ "$cmd" =~ git[[:space:]]+(reset[[:space:]]+--hard|clean[[:space:]]+-[a-z]*f|checkout[[:space:]]+--[[:space:]]|restore[[:space:]]+\.) ]] \
  && block "comando git descarta trabalho de forma irreversível." "Confirme com o usuário ou use 'git stash'."
[[ "$cmd" =~ --no-verify ]] \
  && block "pular hooks de verificação viola a Constitution (Art. VIII.3)." "Corrija as falhas apontadas pelos hooks."
[[ "$cmd" =~ gradlew.*[[:space:]]-x[[:space:]]*[a-z:]*(test|detekt|lint|spotless|kover|konsist|roborazzi|harness) ]] \
  && block "excluir tasks de verificação mascara falhas." "Rode a task completa e corrija o que falhar."
[[ "$cmd" =~ (detektBaseline|updateLintBaseline|recordRoborazzi) ]] \
  && block "gerar baseline/regravar screenshots enfraquece sensores." "Peça aprovação humana: é uma mudança de referência."
[[ "$cmd" =~ rm[[:space:]]+-[a-z]*r[a-z]*f?[[:space:]]+(/|~|\.\.|\$HOME|\*)([[:space:]]|$) ]] \
  && block "remoção recursiva fora do escopo do projeto." "Remova arquivos específicos dentro do projeto."
# Shell must be a whole word: "| shasum" is checksum verification, not execution (steering loop 2026-10-17).
[[ "$cmd" =~ (curl|wget)[^|]*\|[[:space:]]*(ba|z)?sh([[:space:]]|$) ]] \
  && block "executar script baixado da internet." "Baixe, revise e peça aprovação antes de executar."
[[ "$cmd" =~ gradlew.*(publish|uploadArchives|appDistributionUpload|publishBundle|publishApk) ]] \
  && block "publicação de artefatos é ação externa." "Somente pelo pipeline de release, com aprovação humana."
# Only the command lines are checked, not heredoc bodies (avoids false positives when
# writing .gitignore/.aiexclude entries). See docs/harness/steering-loop.md (2026-10-16).
cmd_lines=$(printf '%s\n' "$cmd" | awk '/<<-?[[:space:]]*'"'"'?[A-Za-z_]+'"'"'?/{print; skip=1; next} skip&&/^[A-Za-z_]+$/{skip=0; next} !skip')
[[ "$cmd_lines" =~ (cat|less|more|head|tail|grep|sed|awk|cp|base64|xxd|strings)[[:space:]][^\<\>\|]*(local\.properties|\.jks|\.keystore|\.p12|[[:space:]/]\.env|google-services\.json) ]] \
  && block "leitura de arquivo com segredos." "O agente não precisa de segredos; use variáveis de ambiente no CI."
exit 0
