#!/usr/bin/env bash
# PreToolUse(Edit|Write): protects secrets, generated code and sensor baselines.
file=$(jq -r '.tool_input.file_path // ""')

block() {
  printf 'BLOQUEADO PELO HARNESS: %s\nArquivo: %s\nComo proceder: %s\n' "$1" "$file" "$2" >&2
  exit 2
}

case "$file" in
  *local.properties|*.jks|*.keystore|*.p12|*/.env|*/.env.*|*google-services.json|*secrets.*)
    block "arquivo de segredos/credenciais." "Segredos são configurados pelo humano ou pelo CI, nunca pelo agente." ;;
  */build/*|*/generated/*|*/.gradle/*)
    block "arquivo gerado; a alteração seria perdida no próximo build." "Altere a fonte que gera este arquivo." ;;
  *gradle-wrapper.jar)
    block "binário do Gradle Wrapper." "Use './gradlew wrapper --gradle-version X' com aprovação." ;;
  *baseline.xml|*/screenshots/*.png|*/roborazzi/*.png)
    block "baseline/referência de sensor; editar mascara regressões (Constitution Art. VIII.3)." "Corrija o código. Se a referência deve mudar, peça aprovação humana." ;;
esac
exit 0
