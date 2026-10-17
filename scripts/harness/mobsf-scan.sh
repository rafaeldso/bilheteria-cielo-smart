#!/usr/bin/env bash
# Sensor S-23: static analysis of a release APK/AAB on a running MobSF instance.
# Fails when MobSF reports any `high` finding. Accepted risks (docs/security/triage.md) are
# fixed at the source or justified in the release PR; this script has no allowlist on purpose.
# Usage: MOBSF_URL=http://localhost:8000 MOBSF_API_KEY=... mobsf-scan.sh <app.apk|app.aab>
# Local MobSF: docker run --rm -p 8000:8000 -e MOBSF_API_KEY=<key> opensecurity/mobile-security-framework-mobsf:v4.5.3
set -uo pipefail
source "$(dirname "$0")/lib.sh"

artifact="${1:-}"
: "${MOBSF_URL:=http://localhost:8000}"
if [[ -z "$artifact" || ! -f "$artifact" ]]; then
  sensor_fail S-23 "artefato ausente" "${artifact:-<vazio>}" "APK/AAB não encontrado" \
    "Gere com ./gradlew :app:assembleRelease e passe o caminho do .apk" "docs/security/README.md"
  exit 64
fi
if [[ -z "${MOBSF_API_KEY:-}" ]]; then
  sensor_fail S-23 "MOBSF_API_KEY ausente" "ambiente" "Sem chave da API do MobSF" \
    "Defina MOBSF_API_KEY (secret do CI ou a mesma chave passada ao container)" "docs/security/README.md"
  exit 64
fi

out="$HARNESS_ROOT/build/harness/mobsf"
mkdir -p "$out"
auth=(-H "Authorization: $MOBSF_API_KEY")

for _ in $(seq 1 30); do curl -sf "${auth[@]}" "$MOBSF_URL/api/v1/scans" >/dev/null && break; sleep 5; done

hash=$(curl -sSf "${auth[@]}" -F "file=@$artifact" "$MOBSF_URL/api/v1/upload" | jq -r '.hash')
[[ -z "$hash" || "$hash" == null ]] && { echo "SENSOR S-23 FALHOU: upload ao MobSF falhou"; exit 1; }

curl -sSf "${auth[@]}" --data "hash=$hash" "$MOBSF_URL/api/v1/scan" >/dev/null
curl -sSf "${auth[@]}" --data "hash=$hash" "$MOBSF_URL/api/v1/report_json" > "$out/report.json"
curl -sSf "${auth[@]}" --data "hash=$hash" "$MOBSF_URL/api/v1/download_pdf" -o "$out/report.pdf" || true

high=$(jq '[.appsec.high // [] | .[]] | length' "$out/report.json")
score=$(jq -r '.appsec.security_score // "?"' "$out/report.json")

if (( high > 0 )); then
  jq -r '.appsec.high[] | "  - \(.title // .section): \(.description // "" | .[0:160])"' "$out/report.json"
  sensor_fail S-23 "MobSF: $high achado(s) high" "$artifact" \
    "Problemas de severidade alta no binário de release (score $score)" \
    "Corrija ou registre risco aceito com prazo em docs/security/triage.md; relatório em build/harness/mobsf/" \
    "docs/security/masvs-matrix.md"
  exit 1
fi
sensor_ok S-23 "MobSF sem achados high (score $score). Relatório: build/harness/mobsf/"
