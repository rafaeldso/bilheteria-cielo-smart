#!/usr/bin/env bash
# Sensor S-01: fast, file-level checks for forbidden patterns (AP-xx).
# Runs on every agent edit (PostToolUse hook) and in pre-commit.
# Usage: lint-file.sh <file> [<file> ...]   Exit: 0 = clean, 1 = violations.
set -uo pipefail
source "$(dirname "$0")/lib.sh"

violations=0

report() { # <file> <line> <rule> <problem> <fix> <ref>
  sensor_fail "S-01" "$3" "$1:$2" "$4" "$5" "$6"
  violations=$((violations + 1))
}

# check <file> <regex> <rule> <problem> <fix> <ref>   (GREP_FLAGS=-i for case-insensitive)
check() {
  local file="$1" regex="$2"
  while IFS=: read -r line _; do
    [[ -n "$line" ]] && report "$file" "$line" "$3" "$4" "$5" "$6"
  done < <(grep -nE ${GREP_FLAGS:-} -- "$regex" "$file" 2>/dev/null | grep -vE '^[0-9]+:\s*(//|\*|/\*)' | grep -v 'harness:allow')
}

lint_kotlin() {
  local f="$1"
  local test=false; is_test_path "$f" && test=true

  if ! $test; then
    [[ "$f" =~ /di/ ]] || check "$f" 'Dispatchers\.(IO|Default|Main|Unconfined)\b' \
      "AP-01 dispatcher fixo" "Dispatcher hardcoded fora de módulo de DI." \
      "Injete via construtor: '@IoDispatcher private val ioDispatcher: CoroutineDispatcher'." \
      "docs/harness/guides/kotlin-style.md#coroutines-e-flow"

    check "$f" '\bGlobalScope\b|\brunBlocking\b|Thread\.sleep\(' \
      "AP-02 escopo/bloqueio proibido" "GlobalScope, runBlocking ou Thread.sleep em código de produção." \
      "Use viewModelScope, um CoroutineScope injetado ou torne a função 'suspend'." \
      "docs/harness/guides/kotlin-style.md#coroutines-e-flow"

    check "$f" '\bLog\.(v|d|i|w|e|wtf)\(|\bprintln\(|\.printStackTrace\(\)' \
      "AP-07 log direto" "Log do Android/println usado diretamente; risco de vazar PII/cartão em release." \
      "Use o logger do projeto (core:common) e nunca inclua dados sensíveis na mensagem." \
      "docs/harness/guides/security.md#regras-para-o-agente"

    check "$f" '(Timber|[Ll]ogger|[Ll]og)\.[a-z]+\(.*\$\{?[a-zA-Z.]*([Pp]an|[Cc]vv|[Cc]pf|[Cc]ardNumber|[Pp]assword|[Tt]oken|[Ee]mail|[Pp]hone)' \
      "AP-07 dado sensível em log" "Interpolação de dado sensível (PAN/CVV/CPF/token/e-mail) em log." \
      "Remova o dado da mensagem; registre apenas tipo do evento ou ID de correlação." \
      "docs/harness/guides/security.md#classificação-de-dados"

    check "$f" '([Aa]mount|[Pp]rice|[Vv]alor|[Bb]alance|[Ss]aldo|[Tt]otal|[Ff]ee|[Tt]axa)[A-Za-z]*\s*:\s*(Double|Float)\b' \
      "AP-08 dinheiro em ponto flutuante" "Valor monetário tipado como Double/Float." \
      "Use BigDecimal ou centavos em Long (ou a value class de dinheiro do projeto)." \
      "docs/harness/guides/kotlin-style.md#idiomático"
  fi

  if [[ "$f" =~ /domain/ ]] && ! $test; then
    check "$f" '^import (android|androidx|retrofit2|okhttp3|com\.squareup|dagger\.hilt\.android)\.' \
      "AP-03 domain impuro" "Camada domain importando Android/framework de infraestrutura." \
      "Mova o código para data/ui ou dependa de uma interface definida no domain." \
      "docs/harness/guides/architecture.md#camadas"
  fi

  check "$f" '@(Suppress|SuppressLint|Ignore|Disabled)\b' \
    "AP-10 sensor silenciado" "Supressão/desativação de verificação ou teste." \
    "Corrija a causa. Se for realmente necessário, peça aprovação humana e adicione 'harness:allow <motivo>' na mesma linha." \
    "docs/constitution.md (Art. VIII.3)"
}

lint_any() {
  local f="$1"
  GREP_FLAGS=-i check "$f" '(api[_-]?key|secret|passw(or)?d|client[_-]?secret|token)\s*[:=]\s*"[^"$]{12,}"' \
    "Segredo hardcoded" "Possível credencial literal no código." \
    "Remova o valor; leia de variável de ambiente/BuildConfig injetado pelo CI ou do Keystore." \
    "docs/harness/guides/security.md"
  check "$f" '-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}' \
    "Chave privada/credencial" "Material criptográfico ou chave de API no arquivo." \
    "Remova o arquivo/valor do repositório e revogue a credencial." \
    "docs/harness/guides/security.md"
}

for file in "$@"; do
  [[ -f "$file" ]] || continue
  is_generated_path "$file" && continue
  case "$file" in
    *.kt|*.kts) lint_kotlin "$file"; lint_any "$file" ;;
    *.xml|*.json|*.properties|*.yml|*.yaml|*.gradle|*.toml) lint_any "$file" ;;
  esac
done

if (( violations > 0 )); then
  echo "S-01: $violations violação(ões). Corrija antes de seguir."
  exit 1
fi
exit 0
