---
paths:
  - "**/data/src/**/*.kt"
  - "**/AndroidManifest.xml"
  - "**/res/xml/network_security_config.xml"
---
# Camada data, rede e manifest
Antes de editar, leia [architecture.md](../../docs/harness/guides/architecture.md),
[error-handling.md](../../docs/harness/guides/error-handling.md) e [security.md](../../docs/harness/guides/security.md).
Padrão: `docs/harness/templates/kotlin/FeatureData.kt`. Mudança aqui fecha com o revisor `security-reviewer`.
Sensores: `lint-file.sh` (dispatcher fixo, `Log` direto, segredo), `scan-sensitive-data.sh` (PAN/CPF).
